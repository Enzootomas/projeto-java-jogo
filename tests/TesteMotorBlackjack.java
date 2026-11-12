package tests;

import model.BlackjackEngine;
import model.BlackjackGame;
import model.Card;
import model.Deck;
import model.Rank;
import model.RoundResult;
import model.RoundState;
import model.Suit;

import java.util.List;

/**
 * Bateria de testes deterministicos do motor completo de regras {@link BlackjackGame},
 * validando a implementacao oficial do contrato {@link BlackjackEngine}.
 *
 * Cobre com sequencias de cartas 100% controladas:
 * 1. Estado inicial, validacao de apostas pares, saldo insuficiente, valores invalidos e protecao de overflow.
 * 2. Ocultacao estrita da 2a carta do dealer durante TURNO_JOGADOR e revelacao ao encerrar.
 * 3. Blackjack Natural do jogador (pagamento 3:2 exato), Blackjack Natural do dealer e duplo Blackjack (Push).
 * 4. Diferenca entre Blackjack Natural (2 cartas -> 3:2) e 21 com 3 cartas (vitoria simples -> 1:1).
 * 5. Comportamento do dealer parando em qualquer 17 (tanto hard 17 quanto soft 17) e comprando com 16.
 * 6. Estouro (Bust) do jogador e estouro do dealer.
 * 7. Vitoria simples, derrota por pontos e empate (Push) na comparacao final.
 * 8. Matriz de acoes bloqueadas fora do estado permitido, ausencia de pagamento duplicado,
 *    imutabilidade das listas retornadas e multiplas rodadas consecutivas.
 */
public class TesteMotorBlackjack {

    private static int totalTestes = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DETERMINISTICOS DO MOTOR BLACKJACKGAME");
        System.out.println("====================================================\n");

        testarEstadoInicialEApostasValidasEInvalidas();
        testarProtecaoContraOverflowDeLong();
        testarOcultacaoDaCartaDoDealerEImutabilidade();
        testarBlackjackNaturalJogadorDealerEEmpate();
        testar21ComTresCartasPaga1Para1ENao3Para2();
        testarDealerParandoEmHard17ESoft17();
        testarEstouroDoJogadorEDoDealer();
        testarVitoriaDerrotaEmpateEPagamentoUnicoSemDuplicidade();
        testarMultiplasRodadasConsecutivasEModoProducao();
        testarBaralhoInjetadoDeterministicoSemReposicaoAleatoria();

        System.out.println("\n----------------------------------------------------");
        System.out.println("Total de verificacoes (BlackjackGame): " + totalTestes);
        System.out.println("Falhas: " + falhas);
        if (falhas == 0) {
            System.out.println("RESULTADO: TODOS OS TESTES DO MOTOR BLACKJACK PASSARAM! [OK]");
            System.out.println("----------------------------------------------------");
        } else {
            System.err.println("RESULTADO: FALHA NOS TESTES DO MOTOR BLACKJACK! [FALHA]");
            System.out.println("----------------------------------------------------");
            System.exit(1);
        }
    }

    private static void afirme(boolean condicao, String descricao) {
        totalTestes++;
        if (condicao) {
            System.out.println("  [OK] " + descricao);
        } else {
            falhas++;
            System.err.println("  [FALHA] " + descricao);
        }
    }

    private static Card c(Suit suit, Rank rank) {
        return new Card(suit, rank);
    }

    private static void testarEstadoInicialEApostasValidasEInvalidas() {
        System.out.println("1. Testando Estado Inicial e Validacao de Apostas (Pares, Positivas e <= Saldo):");

        Deck deck = new Deck(List.of(
                c(Suit.COPAS, Rank.OITO),     // Jogador 1
                c(Suit.ESPADAS, Rank.SETE),   // Dealer aberta
                c(Suit.OUROS, Rank.NOVE),     // Jogador 2 (17)
                c(Suit.PAUS, Rank.DEZ)        // Dealer oculta (17)
        ));
        BlackjackEngine engine = new BlackjackGame(1000L, deck);

        afirme(engine.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA, "Motor inicia em AGUARDANDO_APOSTA");
        afirme(engine.getResultadoRodada() == RoundResult.EM_ANDAMENTO, "Resultado inicial e EM_ANDAMENTO");
        afirme(engine.getSaldoVirtual() == 1000L, "Saldo inicial configurado corretamente (1000)");
        afirme(engine.getApostaAtual() == 0L, "Aposta inicial e 0");
        afirme(engine.getMaoJogador().isEmpty() && engine.getCartasVisiveisDealer().isEmpty(),
                "Maos iniciam vazias antes da aposta");
        afirme(!engine.hasCartaOcultaDealer(), "Nao ha carta oculta antes de iniciar partida");

        // Tentativas de acoes proibidas em AGUARDANDO_APOSTA
        afirme(!engine.pedirCarta(), "pedirCarta() retorna false em AGUARDANDO_APOSTA");
        engine.parar();
        afirme(engine.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA,
                "parar() nao tem efeito em AGUARDANDO_APOSTA");

        // Apostas invalidas
        afirme(!engine.iniciarPartida(0L), "Rejeita aposta igual a zero");
        afirme(!engine.iniciarPartida(-50L), "Rejeita aposta negativa");
        afirme(!engine.iniciarPartida(101L), "Rejeita aposta impar (101) para preservar 3:2 exato");
        afirme(!engine.iniciarPartida(1L), "Rejeita aposta impar minima (1)");
        afirme(!engine.iniciarPartida(1002L), "Rejeita aposta par acima do saldo disponivel (1002 > 1000)");
        afirme(engine.getSaldoVirtual() == 1000L && engine.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA,
                "Apostas rejeitadas nao alteram o saldo nem o estado da rodada");

        // Aposta valida
        afirme(engine.iniciarPartida(200L), "Aceita aposta par valida dentro do saldo (200)");
        afirme(engine.getSaldoVirtual() == 800L, "Debita imediatamente as 200 fichas no inicio da partida (saldo = 800)");
        afirme(engine.getApostaAtual() == 200L, "Registra aposta atual = 200");
        afirme(engine.getEstadoRodada() == RoundState.TURNO_JOGADOR, "Transita para TURNO_JOGADOR");
        afirme(!engine.iniciarPartida(100L), "Bloqueia nova chamada a iniciarPartida() durante TURNO_JOGADOR");
        System.out.println();
    }

    private static void testarProtecaoContraOverflowDeLong() {
        System.out.println("2. Testando Protecao contra Overflow Aritmetico em long:");

        Deck deck = new Deck(List.of(
                c(Suit.ESPADAS, Rank.AS),
                c(Suit.COPAS, Rank.SEIS),
                c(Suit.OUROS, Rank.REI),
                c(Suit.PAUS, Rank.SETE)
        ));
        long saldoGigante = Long.MAX_VALUE - 1L; // valor par proximo ao limite de 64 bits
        BlackjackGame game = new BlackjackGame(saldoGigante, deck);

        // Apostar todo o saldoGigante geraria retorno 3:2 de ~2.5 * Long.MAX_VALUE -> overflow!
        afirme(!game.iniciarPartida(saldoGigante),
                "Rejeita preventivamente aposta cujo pagamento 3:2 ultrapassaria Long.MAX_VALUE");
        afirme(game.getSaldoVirtual() == saldoGigante && game.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA,
                "Saldo permanece intacto quando aposta e bloqueada por risco de overflow");
        System.out.println();
    }

    private static void testarOcultacaoDaCartaDoDealerEImutabilidade() {
        System.out.println("3. Testando Ocultacao da Carta do Dealer e Imutabilidade das Consultas:");

        Card j1 = c(Suit.COPAS, Rank.DEZ);
        Card dAberta = c(Suit.ESPADAS, Rank.SEIS);
        Card j2 = c(Suit.OUROS, Rank.OITO);
        Card dOculta = c(Suit.PAUS, Rank.REI); // se vazasse somaria 16, mas visivel deve ser apenas 6!

        Deck deck = new Deck(List.of(
                j1, dAberta, j2, dOculta,
                c(Suit.COPAS, Rank.DOIS) // compra do dealer (6 + 10 + 2 = 18)
        ));
        BlackjackGame game = new BlackjackGame(500L, deck);
        game.iniciarPartida(100L);

        List<Card> visiveisTurno = game.getCartasVisiveisDealer();
        afirme(game.hasCartaOcultaDealer(), "hasCartaOcultaDealer() retorna true durante TURNO_JOGADOR");
        afirme(visiveisTurno.size() == 1 && visiveisTurno.get(0).equals(dAberta),
                "getCartasVisiveisDealer() retorna APENAS a 1a carta aberta (6 de Espadas), sem vazar o Rei oculto");
        afirme(game.getPontosDealerVisiveis() == 6,
                "getPontosDealerVisiveis() retorna apenas 6 pontos durante TURNO_JOGADOR");

        // Tentativa de adulterar listas retornadas pela API publica
        boolean bloqueouMutacaoJogador = false;
        try {
            game.getMaoJogador().add(c(Suit.ESPADAS, Rank.TRES));
        } catch (UnsupportedOperationException e) {
            bloqueouMutacaoJogador = true;
        }
        afirme(bloqueouMutacaoJogador, "getMaoJogador() retorna lista imutavel (bloqueia add externo)");

        boolean bloqueouMutacaoDealer = false;
        try {
            visiveisTurno.clear();
        } catch (UnsupportedOperationException e) {
            bloqueouMutacaoDealer = true;
        }
        afirme(bloqueouMutacaoDealer, "getCartasVisiveisDealer() retorna lista imutavel (bloqueia clear externo)");

        // Jogador para -> revela carta oculta e dealer compra o 2 (total 18)
        game.parar();
        List<Card> visiveisFinal = game.getCartasVisiveisDealer();
        afirme(!game.hasCartaOcultaDealer(), "hasCartaOcultaDealer() passa a false em FINALIZADA");
        afirme(visiveisFinal.size() == 3 && visiveisFinal.get(1).equals(dOculta),
                "Apos finalizar, getCartasVisiveisDealer() revela a carta antes oculta e as compras da banca");
        afirme(visiveisTurno.size() == 1,
                "Snapshot antigo capturado durante o turno continua intacto com 1 carta");
        afirme(game.getPontosDealerVisiveis() == 18, "getPontosDealerVisiveis() passa a informar o total real (18)");
        System.out.println();
    }

    private static void testarBlackjackNaturalJogadorDealerEEmpate() {
        System.out.println("4. Testando Blackjack Natural (Jogador 3:2, Dealer e Duplo Blackjack Push):");

        // Cenario A: Blackjack Natural exclusivo do Jogador -> Paga 3:2 exato
        Deck deckJogadorBJ = new Deck(List.of(
                c(Suit.ESPADAS, Rank.AS),     // Jogador 1
                c(Suit.COPAS, Rank.NOVE),     // Dealer aberta
                c(Suit.OUROS, Rank.REI),      // Jogador 2 -> 21 natural!
                c(Suit.PAUS, Rank.OITO)       // Dealer oculta -> 17
        ));
        BlackjackGame gameBJ = new BlackjackGame(1000L, deckJogadorBJ);
        afirme(gameBJ.iniciarPartida(100L), "Partida com aposta 100 iniciada");
        afirme(gameBJ.getEstadoRodada() == RoundState.FINALIZADA,
                "Blackjack Natural encerra a rodada imediatamente na distribuicao");
        afirme(gameBJ.getResultadoRodada() == RoundResult.JOGADOR_BLACKJACK,
                "Resultado e JOGADOR_BLACKJACK");
        afirme(!gameBJ.hasCartaOcultaDealer() && gameBJ.getCartasVisiveisDealer().size() == 2,
                "Carta do dealer ja fica revelada ao encerrar por Blackjack Natural");
        // Saldo: 1000 - 100 + 250 = 1150 (retorno total 250, lucro liquido +150)
        afirme(gameBJ.getSaldoVirtual() == 1150L,
                "Pagamento 3:2 exato creditado: saldo foi de 1000 para 1150 (aposta 100 -> retorno 250)");
        afirme(gameBJ.getUltimoRetornoTotalCreditado() == 250L && gameBJ.getLucroLiquidoUltimaRodada() == 150L,
                "Retorno total (250) e lucro liquido (+150) documentados corretamente");

        // Cenario B: Blackjack Natural exclusivo do Dealer -> Jogador perde aposta
        Deck deckDealerBJ = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // Jogador 1
                c(Suit.ESPADAS, Rank.AS),     // Dealer aberta
                c(Suit.OUROS, Rank.NOVE),     // Jogador 2 -> 19
                c(Suit.PAUS, Rank.DAMA)       // Dealer oculta -> 21 natural!
        ));
        BlackjackGame gameDealerBJ = new BlackjackGame(1000L, deckDealerBJ);
        gameDealerBJ.iniciarPartida(100L);
        afirme(gameDealerBJ.getEstadoRodada() == RoundState.FINALIZADA,
                "Blackjack Natural do dealer encerra imediatamente a rodada");
        afirme(gameDealerBJ.getResultadoRodada() == RoundResult.DEALER_BLACKJACK,
                "Resultado e DEALER_BLACKJACK");
        afirme(gameDealerBJ.getSaldoVirtual() == 900L && gameDealerBJ.getLucroLiquidoUltimaRodada() == -100L,
                "Jogador perde as 100 fichas apostadas contra Blackjack Natural do dealer");

        // Cenario C: Ambos com Blackjack Natural -> Empate (Push)
        Deck deckAmbosBJ = new Deck(List.of(
                c(Suit.ESPADAS, Rank.AS),     // Jogador 1
                c(Suit.COPAS, Rank.AS),       // Dealer aberta
                c(Suit.OUROS, Rank.VALETE),   // Jogador 2 -> 21 natural
                c(Suit.PAUS, Rank.REI)        // Dealer oculta -> 21 natural
        ));
        BlackjackGame gameAmbosBJ = new BlackjackGame(1000L, deckAmbosBJ);
        gameAmbosBJ.iniciarPartida(100L);
        afirme(gameAmbosBJ.getEstadoRodada() == RoundState.FINALIZADA
                        && gameAmbosBJ.getResultadoRodada() == RoundResult.EMPATE_PUSH,
                "Blackjack Natural simultaneo de jogador e dealer resulta em EMPATE_PUSH");
        afirme(gameAmbosBJ.getSaldoVirtual() == 1000L && gameAmbosBJ.getLucroLiquidoUltimaRodada() == 0L,
                "Empate de Blackjack Natural devolve integralmente as 100 fichas (saldo = 1000)");
        System.out.println();
    }

    private static void testar21ComTresCartasPaga1Para1ENao3Para2() {
        System.out.println("5. Testando 21 com 3 Cartas (Vitoria 1:1) vs Blackjack Natural:");

        Deck deck21TresCartas = new Deck(List.of(
                c(Suit.COPAS, Rank.SETE),     // Jogador 1
                c(Suit.ESPADAS, Rank.DEZ),    // Dealer aberta
                c(Suit.OUROS, Rank.SETE),     // Jogador 2 (14)
                c(Suit.PAUS, Rank.OITO),      // Dealer oculta (18)
                c(Suit.ESPADAS, Rank.SETE)    // Hit do jogador -> 7 + 7 + 7 = 21 em 3 cartas!
        ));
        BlackjackGame game = new BlackjackGame(1000L, deck21TresCartas);
        game.iniciarPartida(100L);
        afirme(game.pedirCarta(), "Jogador pede 3a carta e atinge 21 pontos");
        afirme(game.getPontosJogador() == 21, "Pontuacao do jogador e 21 com 3 cartas");
        game.parar();

        afirme(game.getResultadoRodada() == RoundResult.JOGADOR_VITORIA,
                "21 com 3 cartas gera JOGADOR_VITORIA (e NAO JOGADOR_BLACKJACK)");
        afirme(game.getSaldoVirtual() == 1100L && game.getLucroLiquidoUltimaRodada() == 100L,
                "21 com 3 cartas paga 1:1 (lucro +100, saldo 1100) e nao 3:2");
        System.out.println();
    }

    private static void testarDealerParandoEmHard17ESoft17() {
        System.out.println("6. Testando Regra do Dealer: Compra ate 16 e Para em Qualquer 17 (Hard 17 e Soft 17):");

        // Cenario A: Dealer tem Soft 17 inicial (As + 6 = 17) -> Deve PARAR sem comprar a 5a carta!
        Deck deckSoft17 = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // Jogador 1
                c(Suit.ESPADAS, Rank.AS),     // Dealer aberta (As)
                c(Suit.OUROS, Rank.OITO),     // Jogador 2 (18)
                c(Suit.PAUS, Rank.SEIS),      // Dealer oculta (6) -> Soft 17!
                c(Suit.COPAS, Rank.QUATRO)    // Carta armadilha: se comprasse faria 21!
        ));
        BlackjackGame gameSoft17 = new BlackjackGame(1000L, deckSoft17);
        gameSoft17.iniciarPartida(100L);
        gameSoft17.parar();

        afirme(gameSoft17.getCartasVisiveisDealer().size() == 2 && gameSoft17.getPontosDealerVisiveis() == 17,
                "Dealer com Soft 17 (As + 6) PARA imediatamente com 2 cartas (nao compra a carta seguinte)");
        afirme(gameSoft17.getResultadoRodada() == RoundResult.JOGADOR_VITORIA && gameSoft17.getSaldoVirtual() == 1100L,
                "Jogador com 18 vence o Soft 17 do dealer");

        // Cenario B: Dealer tem 16 (10 + 6), compra As -> vai a Hard 17 (10 + 6 + 1 = 17) e PARA!
        Deck deck16ParaHard17 = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // Jogador 1
                c(Suit.ESPADAS, Rank.DEZ),    // Dealer aberta (10)
                c(Suit.OUROS, Rank.NOVE),     // Jogador 2 (19)
                c(Suit.PAUS, Rank.SEIS),      // Dealer oculta (6) -> 16 (obrigado a comprar)
                c(Suit.COPAS, Rank.AS),       // Compra do dealer -> 10 + 6 + 1 = Hard 17 (deve parar)
                c(Suit.OUROS, Rank.TRES)      // Carta armadilha: nao deve ser comprada
        ));
        BlackjackGame gameHard17 = new BlackjackGame(1000L, deck16ParaHard17);
        gameHard17.iniciarPartida(100L);
        gameHard17.parar();

        afirme(gameHard17.getCartasVisiveisDealer().size() == 3 && gameHard17.getPontosDealerVisiveis() == 17,
                "Dealer com 16 compra uma carta, atinge Hard 17 (10+6+A) e PARA");
        afirme(gameHard17.getResultadoRodada() == RoundResult.JOGADOR_VITORIA,
                "Jogador com 19 vence o Hard 17 do dealer");
        System.out.println();
    }

    private static void testarEstouroDoJogadorEDoDealer() {
        System.out.println("7. Testando Estouro (Bust) do Jogador e Estouro do Dealer:");

        // Estouro do Jogador: 10 + 6 + 8 = 24 -> encerra imediatamente sem turno do dealer
        Deck deckJugadorEstoura = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // Jogador 1
                c(Suit.ESPADAS, Rank.CINCO),  // Dealer aberta (5)
                c(Suit.OUROS, Rank.SEIS),     // Jogador 2 (16)
                c(Suit.PAUS, Rank.SETE),      // Dealer oculta (12)
                c(Suit.COPAS, Rank.OITO),     // Hit do jogador -> 24 (Bust!)
                c(Suit.OUROS, Rank.NOVE)      // Dealer NAO deve comprar esta carta pois jogador ja estourou
        ));
        BlackjackGame gameBustJ = new BlackjackGame(1000L, deckJugadorEstoura);
        gameBustJ.iniciarPartida(200L);
        afirme(gameBustJ.pedirCarta(), "Jogador pede carta e recebe o 8");
        afirme(gameBustJ.getPontosJogador() == 24, "Jogador fica com 24 pontos");
        afirme(gameBustJ.getEstadoRodada() == RoundState.FINALIZADA,
                "Estouro do jogador finaliza a rodada imediatamente");
        afirme(gameBustJ.getResultadoRodada() == RoundResult.JOGADOR_ESTOUROU,
                "Resultado e JOGADOR_ESTOUROU");
        afirme(gameBustJ.getCartasVisiveisDealer().size() == 2,
                "Dealer revela suas 2 cartas mas nao executa compras desnecessarias apos estouro do jogador");
        afirme(gameBustJ.getSaldoVirtual() == 800L, "Jogador perde as 200 fichas no estouro (saldo = 800)");

        // Estouro do Dealer: Dealer tem 10 + 6 = 16, compra Valete (10) -> 26 (Bust!)
        Deck deckDealerEstoura = new Deck(List.of(
                c(Suit.COPAS, Rank.OITO),     // Jogador 1
                c(Suit.ESPADAS, Rank.DEZ),    // Dealer aberta
                c(Suit.OUROS, Rank.CINCO),    // Jogador 2 (13)
                c(Suit.PAUS, Rank.SEIS),      // Dealer oculta (16)
                c(Suit.COPAS, Rank.VALETE)    // Compra do dealer -> 26 (Bust!)
        ));
        BlackjackGame gameBustD = new BlackjackGame(1000L, deckDealerEstoura);
        gameBustD.iniciarPartida(200L);
        gameBustD.parar();

        afirme(gameBustD.getPontosDealerVisiveis() == 26, "Dealer estoura com 26 pontos");
        afirme(gameBustD.getResultadoRodada() == RoundResult.JOGADOR_VITORIA,
                "Estouro do dealer concede JOGADOR_VITORIA mesmo com jogador tendo 13 pontos");
        afirme(gameBustD.getSaldoVirtual() == 1200L,
                "Jogador recebe pagamento 1:1 (saldo 1000 -> 1200)");
        System.out.println();
    }

    private static void testarVitoriaDerrotaEmpateEPagamentoUnicoSemDuplicidade() {
        System.out.println("8. Testando Derrota por Pontos, Empate (Push) e Blindagem contra Pagamento Duplicado:");

        // Cenario A: Dealer vence por pontos (Dealer 20 vs Jogador 18)
        Deck deckDealerVence = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // Jogador 1
                c(Suit.ESPADAS, Rank.REI),    // Dealer aberta
                c(Suit.OUROS, Rank.OITO),     // Jogador 2 (18)
                c(Suit.PAUS, Rank.DAMA)       // Dealer oculta (20)
        ));
        BlackjackGame gameDerrota = new BlackjackGame(1000L, deckDealerVence);
        gameDerrota.iniciarPartida(150L);
        gameDerrota.parar();
        afirme(gameDerrota.getResultadoRodada() == RoundResult.DEALER_VITORIA && gameDerrota.getSaldoVirtual() == 850L,
                "Dealer (20) vence Jogador (18) -> DEALER_VITORIA e saldo = 850");

        // Cenario B: Empate simples em 19 vs 19 (Push)
        Deck deckEmpate = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // Jogador 1
                c(Suit.ESPADAS, Rank.DEZ),    // Dealer aberta
                c(Suit.OUROS, Rank.NOVE),     // Jogador 2 (19)
                c(Suit.PAUS, Rank.NOVE)       // Dealer oculta (19)
        ));
        BlackjackGame gameEmpate = new BlackjackGame(1000L, deckEmpate);
        gameEmpate.iniciarPartida(150L);
        gameEmpate.parar();
        afirme(gameEmpate.getResultadoRodada() == RoundResult.EMPATE_PUSH && gameEmpate.getSaldoVirtual() == 1000L,
                "Empate 19 x 19 -> EMPATE_PUSH devolve as 150 fichas (saldo = 1000)");

        // Cenario C: Blindagem contra acoes repetidas e pagamento duplicado apos FINALIZADA
        Deck deckVitoria = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // Jogador 1
                c(Suit.ESPADAS, Rank.DEZ),    // Dealer aberta
                c(Suit.OUROS, Rank.DAMA),     // Jogador 2 (20)
                c(Suit.PAUS, Rank.OITO)       // Dealer oculta (18)
        ));
        BlackjackGame gameUnico = new BlackjackGame(1000L, deckVitoria);
        gameUnico.iniciarPartida(100L);
        gameUnico.parar(); // Saldo deve ir para 1100 uma unica vez!
        long saldoAposPrimeiraLiquidacao = gameUnico.getSaldoVirtual();

        // Repete chamadas indevidas apos a rodada estar FINALIZADA
        gameUnico.parar();
        gameUnico.parar();
        boolean aceitouHitFinalizada = gameUnico.pedirCarta();

        afirme(!aceitouHitFinalizada, "pedirCarta() retorna false quando a rodada ja esta FINALIZADA");
        afirme(saldoAposPrimeiraLiquidacao == 1100L && gameUnico.getSaldoVirtual() == 1100L,
                "Multiplas chamadas a parar() apos o fim NAO duplicam o pagamento (saldo cravado em 1100)");
        System.out.println();
    }

    private static void testarMultiplasRodadasConsecutivasEModoProducao() {
        System.out.println("9. Testando Multiplas Rodadas Consecutivas e Construtor Padrao de Producao:");

        Deck deckDuasRodadas = new Deck(List.of(
                // Rodada 1: Jogador Blackjack (A + K = 21) vs Dealer (9 + 8 = 17) -> Aposta 100, ganha +150 (saldo 1150)
                c(Suit.ESPADAS, Rank.AS),
                c(Suit.COPAS, Rank.NOVE),
                c(Suit.OUROS, Rank.REI),
                c(Suit.PAUS, Rank.OITO),
                // Rodada 2: Jogador 18 (10 + 8) vs Dealer 19 (10 + 9) -> Aposta 50, perde -50 (saldo 1100)
                c(Suit.COPAS, Rank.DEZ),
                c(Suit.ESPADAS, Rank.DEZ),
                c(Suit.OUROS, Rank.OITO),
                c(Suit.PAUS, Rank.NOVE)
        ));

        BlackjackGame game = new BlackjackGame(1000L, deckDuasRodadas);
        afirme(game.iniciarPartida(100L), "Rodada 1 iniciada com sucesso");
        afirme(game.getSaldoVirtual() == 1150L && game.getEstadoRodada() == RoundState.FINALIZADA,
                "Rodada 1 concluida com saldo 1150");

        afirme(game.iniciarPartida(50L), "Rodada 2 iniciada a partir do estado FINALIZADA");
        afirme(game.getEstadoRodada() == RoundState.TURNO_JOGADOR && game.hasCartaOcultaDealer(),
                "Rodada 2 restaura estado TURNO_JOGADOR, limpa maos anteriores e oculta 2a carta do dealer");
        afirme(game.getMaoJogador().size() == 2 && game.getCartasVisiveisDealer().size() == 1,
                "Maos da Rodada 2 contem apenas as cartas recem-distribuidas");
        game.parar();
        afirme(game.getResultadoRodada() == RoundResult.DEALER_VITORIA && game.getSaldoVirtual() == 1100L,
                "Rodada 2 concluida acumulando corretamente o historico de saldo (1100)");

        // Construtor padrao de producao (50.000 fichas + SecureRandom)
        BlackjackGame gameProducao = new BlackjackGame();
        afirme(gameProducao.getSaldoVirtual() == BlackjackGame.SALDO_INICIAL_PADRAO,
                "Construtor padrao inicializa com 50.000 fichas virtuais");
        afirme(gameProducao.iniciarPartida(200L),
                "Construtor padrao de producao inicia partida real embaralhada com sucesso");
        System.out.println();
    }

    private static void testarBaralhoInjetadoDeterministicoSemReposicaoAleatoria() {
        System.out.println("10. Testando Determinismo Estrito do Baralho Injetado (< 4 Cartas e Esgotamento na Rodada):");

        // Caso 1: Menos de 4 cartas antes de iniciarPartida -> deve retornar false sem debitar saldo nem repor cartas
        Deck deckComTresCartas = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),
                c(Suit.ESPADAS, Rank.SETE),
                c(Suit.OUROS, Rank.CINCO)
        ));
        BlackjackGame gamePoucasCartas = new BlackjackGame(1000L, deckComTresCartas);
        afirme(!gamePoucasCartas.iniciarPartida(100L),
                "iniciarPartida() retorna false quando baralho injetado possui menos de 4 cartas (3)");
        afirme(gamePoucasCartas.getSaldoVirtual() == 1000L
                        && gamePoucasCartas.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA,
                "Saldo (1000) e estado (AGUARDANDO_APOSTA) permanecem intactos quando faltam cartas para distribuir");
        afirme(deckComTresCartas.cartasRestantes() == 3,
                "Baralho injetado nao foi reiniciado nem consumido ao recusar inicio com menos de 4 cartas");

        // Subcaso 1b: Apos uma 1a rodada consumir 4 de 6 cartas, restam 2 -> 2a rodada deve ser recusada com false
        Deck deckSeisCartas = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // R1 Jogador 1
                c(Suit.ESPADAS, Rank.DEZ),    // R1 Dealer aberta
                c(Suit.OUROS, Rank.NOVE),     // R1 Jogador 2 (19)
                c(Suit.PAUS, Rank.OITO),      // R1 Dealer oculta (18)
                c(Suit.COPAS, Rank.DOIS),     // Sobra 1
                c(Suit.OUROS, Rank.TRES)      // Sobra 2
        ));
        BlackjackGame gameSegundaSemCartas = new BlackjackGame(1000L, deckSeisCartas);
        afirme(gameSegundaSemCartas.iniciarPartida(100L), "1a rodada inicia normalmente com 6 cartas no deck");
        gameSegundaSemCartas.parar(); // Saldo = 1100, restam apenas 2 cartas no deck injetado
        afirme(!gameSegundaSemCartas.iniciarPartida(100L),
                "2a rodada retorna false quando restam apenas 2 cartas no baralho injetado (sem reembaralhar)");
        afirme(gameSegundaSemCartas.getSaldoVirtual() == 1100L && deckSeisCartas.cartasRestantes() == 2,
                "Saldo apos R1 (1100) e as 2 cartas restantes permanecem intactos");

        // Caso 2a: Esgotamento durante pedirCarta() do jogador -> sinaliza IllegalStateException sem repor cartas
        Deck deckQuatroCartasParaHit = new Deck(List.of(
                c(Suit.COPAS, Rank.CINCO),    // Jogador 1
                c(Suit.ESPADAS, Rank.SETE),   // Dealer aberta
                c(Suit.OUROS, Rank.SEIS),     // Jogador 2 (11)
                c(Suit.PAUS, Rank.DEZ)        // Dealer oculta (17) -> 0 cartas restantes!
        ));
        BlackjackGame gameEsgotaNoHit = new BlackjackGame(1000L, deckQuatroCartasParaHit);
        afirme(gameEsgotaNoHit.iniciarPartida(100L), "Inicia partida com exatas 4 cartas");
        boolean sinalizouEsgotamentoNoHit = false;
        try {
            gameEsgotaNoHit.pedirCarta();
        } catch (IllegalStateException e) {
            sinalizouEsgotamentoNoHit = true;
        }
        afirme(sinalizouEsgotamentoNoHit,
                "pedirCarta() com baralho injetado esgotado lanca IllegalStateException explicita (sem repor cartas aleatorias)");
        afirme(deckQuatroCartasParaHit.estaVazio(),
                "Baralho injetado permanece vazio (0 cartas), comprovando ausencia de reposicao aleatoria");

        // Caso 2b: Esgotamento imediato em parar() (0 cartas extras) -> preserva estado anterior sem alterar carta oculta/saldo
        Deck deckQuatroCartasDealerPrecisaComprar = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // Jogador 1
                c(Suit.ESPADAS, Rank.SEIS),   // Dealer aberta (6)
                c(Suit.OUROS, Rank.OITO),     // Jogador 2 (18)
                c(Suit.PAUS, Rank.NOVE)       // Dealer oculta (9 -> soma 15 < 17, precisara comprar mas deck acabou!)
        ));
        BlackjackGame gameEsgotaNoDealer = new BlackjackGame(1000L, deckQuatroCartasDealerPrecisaComprar);
        afirme(gameEsgotaNoDealer.iniciarPartida(100L), "Inicia partida em que dealer precisara comprar alem da 4a carta");
        boolean sinalizouEsgotamentoNoTurnoDealer = false;
        try {
            gameEsgotaNoDealer.parar();
        } catch (IllegalStateException e) {
            sinalizouEsgotamentoNoTurnoDealer = true;
        }
        afirme(sinalizouEsgotamentoNoTurnoDealer,
                "parar() com dealer < 17 e baralho injetado esgotado lanca IllegalStateException explicita");
        afirme(gameEsgotaNoDealer.getEstadoRodada() == RoundState.TURNO_JOGADOR
                        && gameEsgotaNoDealer.getResultadoRodada() == RoundResult.EM_ANDAMENTO,
                "Falha em parar() preserva estado anterior (TURNO_JOGADOR / EM_ANDAMENTO)");
        afirme(gameEsgotaNoDealer.hasCartaOcultaDealer()
                        && gameEsgotaNoDealer.getCartasVisiveisDealer().size() == 1
                        && gameEsgotaNoDealer.getPontosDealerVisiveis() == 6,
                "Falha em parar() preserva a carta oculta do dealer sem vaza-la");
        afirme(gameEsgotaNoDealer.getSaldoVirtual() == 900L,
                "Falha em parar() preserva o saldo anterior (900) sem liquidacao prematura");
        afirme(deckQuatroCartasDealerPrecisaComprar.estaVazio(),
                "Baralho injetado permanece vazio apos falha explicita no turno do dealer");

        // Caso 2c: Banca consegue comprar 1 carta (ex.: 5 + 6 + 3 = 14 < 17), mas o baralho acaba antes da 2a compra!
        // Deve reverter integralmente a compra parcial da banca, devolvendo a carta ao topo do Deck e preservando tudo!
        Card quintaCartaUnica = c(Suit.COPAS, Rank.TRES);
        Deck deckCincoCartasDealerCompraUmaEFalha = new Deck(List.of(
                c(Suit.COPAS, Rank.DEZ),      // Jogador 1
                c(Suit.ESPADAS, Rank.CINCO),  // Dealer aberta (5)
                c(Suit.OUROS, Rank.OITO),     // Jogador 2 (18)
                c(Suit.PAUS, Rank.SEIS),      // Dealer oculta (6 -> soma inicial 11)
                quintaCartaUnica              // 1a compra do dealer (11 + 3 = 14 < 17 -> tenta 2a compra e esgota!)
        ));
        BlackjackGame gameDealerCompraUmaEFalha = new BlackjackGame(1000L, deckCincoCartasDealerCompraUmaEFalha);
        afirme(gameDealerCompraUmaEFalha.iniciarPartida(100L),
                "Inicia partida com 5 cartas (sobra exatamente 1 carta no Deck apos distribuicao)");
        boolean sinalizouAposPrimeiraCompraDealer = false;
        try {
            gameDealerCompraUmaEFalha.parar();
        } catch (IllegalStateException e) {
            sinalizouAposPrimeiraCompraDealer = true;
        }
        afirme(sinalizouAposPrimeiraCompraDealer,
                "parar() lanca IllegalStateException quando a banca compra 1 carta (vai a 14) e o baralho acaba na compra seguinte");
        afirme(gameDealerCompraUmaEFalha.getEstadoRodada() == RoundState.TURNO_JOGADOR
                        && gameDealerCompraUmaEFalha.getResultadoRodada() == RoundResult.EM_ANDAMENTO,
                "Apos falha parcial na 2a compra da banca, estado permanece TURNO_JOGADOR / EM_ANDAMENTO");
        afirme(gameDealerCompraUmaEFalha.hasCartaOcultaDealer()
                        && gameDealerCompraUmaEFalha.getCartasVisiveisDealer().size() == 1
                        && gameDealerCompraUmaEFalha.getPontosDealerVisiveis() == 5,
                "Apos falha parcial da banca, carta oculta permanece protegida e mao da banca nao fica parcialmente alterada");
        afirme(gameDealerCompraUmaEFalha.getSaldoVirtual() == 900L,
                "Apos falha parcial da banca, saldo permanece rigorosamente inalterado (900)");
        afirme(deckCincoCartasDealerCompraUmaEFalha.cartasRestantes() == 1
                        && deckCincoCartasDealerCompraUmaEFalha.getCartasRestantesSnapshot().get(0).equals(quintaCartaUnica),
                "Carta comprada na tentativa abortada da banca foi restaurada ao topo do Deck (3 de Copas)");
        System.out.println();
    }
}
