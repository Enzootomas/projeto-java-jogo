package tests;

import model.Card;
import model.Deck;
import model.EvaluatedPokerHand;
import model.PokerAction;
import model.PokerBotStrategy;
import model.PokerHandEvaluator;
import model.PokerHandRank;
import model.PokerParticipant;
import model.PokerRoundResult;
import model.PokerSimpleAI;
import model.PokerStage;
import model.Rank;
import model.Suit;
import model.TexasHoldemGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Suite de testes deterministicos para o contrato e motor de Poker Texas Hold'em
 * ({@link TexasHoldemGame}, {@link PokerHandEvaluator} e {@link PokerSimpleAI}).
 */
public class TestePokerHoldem {

    private static int totalVerificacoes = 0;
    private static int totalFalhas = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DO MOTOR DE POKER TEXAS HOLD'EM (1 VS 1)");
        System.out.println("====================================================\n");

        testarTodasCategoriasEDesempatesDoAvaliador();
        testarSelecaoDaMelhorMaoDeCincoDentreSeteCartas();
        testarDistribuicaoAlternanciaDeBotaoERuasComunitarias();
        testarAcoesValidasLimiteDeAumentosEDesistenciaSemVazarBot();
        testarShowdownSplitPotEFichaImparForaDePosicao();
        testarAllInExcessoNaoIgualadoERunoutAutomatico();
        testarSnapshotsImutaveisEIsolamentoDaIAPokerSimpleAI();

        System.out.println("\n----------------------------------------------------");
        System.out.println("Total de verificacoes (Poker Hold'em): " + totalVerificacoes);
        System.out.println("Falhas: " + totalFalhas);
        if (totalFalhas == 0) {
            System.out.println("RESULTADO: TODOS OS TESTES DO POKER HOLD'EM PASSARAM! [OK]");
        } else {
            System.err.println("RESULTADO: HOUVE " + totalFalhas + " FALHAS NOS TESTES DO POKER!");
            System.exit(1);
        }
        System.out.println("----------------------------------------------------");
    }

    private static void afirme(boolean condicao, String descricao) {
        totalVerificacoes++;
        if (condicao) {
            System.out.println("  [OK] " + descricao);
        } else {
            totalFalhas++;
            System.err.println("  [FALHA] " + descricao);
        }
    }

    private static Card c(Rank rank, Suit suit) {
        return new Card(suit, rank);
    }

    /** Bot deterministico passivo: paga quando ha aposta e passa quando e gratis. */
    private static PokerBotStrategy botPassivoSemprePagaOuPassa() {
        return (maoBot, com, estagio, pote, valorPagar, saldoBot, bb, podeAumentar) ->
                valorPagar > 0L ? PokerBotStrategy.Decision.pagar() : PokerBotStrategy.Decision.passar();
    }

    private static void testarTodasCategoriasEDesempatesDoAvaliador() {
        System.out.println("1. Testando Categorias Oficiais de 5 Cartas e Desempates (Kickers):");

        EvaluatedPokerHand royalFlush = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.AS, Suit.ESPADAS), c(Rank.REI, Suit.ESPADAS), c(Rank.DAMA, Suit.ESPADAS),
                c(Rank.VALETE, Suit.ESPADAS), c(Rank.DEZ, Suit.ESPADAS)
        ));
        afirme(royalFlush.getCategoria() == PokerHandRank.STRAIGHT_FLUSH && royalFlush.isRoyalFlush(),
                "Royal Flush identificado como Straight Flush ate As");

        EvaluatedPokerHand steelWheel = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.AS, Suit.COPAS), c(Rank.DOIS, Suit.COPAS), c(Rank.TRES, Suit.COPAS),
                c(Rank.QUATRO, Suit.COPAS), c(Rank.CINCO, Suit.COPAS)
        ));
        afirme(steelWheel.getCategoria() == PokerHandRank.STRAIGHT_FLUSH && !steelWheel.isRoyalFlush(),
                "Steel Wheel (A-2-3-4-5 suited) identificado como Straight Flush ate 5");
        afirme(royalFlush.compareTo(steelWheel) > 0, "Royal Flush supera Straight Flush A-5");

        EvaluatedPokerHand quadraReis = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.REI, Suit.ESPADAS), c(Rank.REI, Suit.COPAS), c(Rank.REI, Suit.OUROS),
                c(Rank.REI, Suit.PAUS), c(Rank.SETE, Suit.ESPADAS)
        ));
        EvaluatedPokerHand quadraReisKickerAs = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.REI, Suit.ESPADAS), c(Rank.REI, Suit.COPAS), c(Rank.REI, Suit.OUROS),
                c(Rank.REI, Suit.PAUS), c(Rank.AS, Suit.OUROS)
        ));
        afirme(quadraReis.getCategoria() == PokerHandRank.QUADRA, "Quadra identificada corretamente");
        afirme(steelWheel.compareTo(quadraReisKickerAs) > 0, "Straight Flush supera Quadra");
        afirme(quadraReisKickerAs.compareTo(quadraReis) > 0, "Desempate de Quadra pelo kicker (As > 7)");

        EvaluatedPokerHand fullHouseValetes = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.VALETE, Suit.ESPADAS), c(Rank.VALETE, Suit.COPAS), c(Rank.VALETE, Suit.OUROS),
                c(Rank.AS, Suit.ESPADAS), c(Rank.AS, Suit.COPAS)
        ));
        EvaluatedPokerHand fullHouseDamas = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.DAMA, Suit.ESPADAS), c(Rank.DAMA, Suit.COPAS), c(Rank.DAMA, Suit.OUROS),
                c(Rank.DOIS, Suit.ESPADAS), c(Rank.DOIS, Suit.COPAS)
        ));
        afirme(fullHouseDamas.getCategoria() == PokerHandRank.FULL_HOUSE, "Full House identificado");
        afirme(quadraReis.compareTo(fullHouseDamas) > 0, "Quadra supera Full House");
        afirme(fullHouseDamas.compareTo(fullHouseValetes) > 0,
                "Full House desempata primeiro pela Trinca (Q-Q-Q-2-2 > J-J-J-A-A)");

        EvaluatedPokerHand flushAsMaior = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.AS, Suit.OUROS), c(Rank.VALETE, Suit.OUROS), c(Rank.NOVE, Suit.OUROS),
                c(Rank.SEIS, Suit.OUROS), c(Rank.TRES, Suit.OUROS)
        ));
        EvaluatedPokerHand flushQuintoKicker = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.AS, Suit.PAUS), c(Rank.VALETE, Suit.PAUS), c(Rank.NOVE, Suit.PAUS),
                c(Rank.SEIS, Suit.PAUS), c(Rank.DOIS, Suit.PAUS)
        ));
        afirme(flushAsMaior.getCategoria() == PokerHandRank.FLUSH, "Flush identificado");
        afirme(fullHouseValetes.compareTo(flushAsMaior) > 0, "Full House supera Flush");
        afirme(flushAsMaior.compareTo(flushQuintoKicker) > 0, "Flush desempata ate a 5a carta (3 > 2)");

        EvaluatedPokerHand seqSeis = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.SEIS, Suit.ESPADAS), c(Rank.CINCO, Suit.COPAS), c(Rank.QUATRO, Suit.OUROS),
                c(Rank.TRES, Suit.PAUS), c(Rank.DOIS, Suit.ESPADAS)
        ));
        EvaluatedPokerHand seqWheel = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.AS, Suit.ESPADAS), c(Rank.CINCO, Suit.OUROS), c(Rank.QUATRO, Suit.COPAS),
                c(Rank.TRES, Suit.ESPADAS), c(Rank.DOIS, Suit.PAUS)
        ));
        afirme(seqWheel.getCategoria() == PokerHandRank.SEQUENCIA, "Sequencia Wheel (A-2-3-4-5) identificada");
        afirme(flushQuintoKicker.compareTo(seqSeis) > 0, "Flush supera Sequencia");
        afirme(seqSeis.compareTo(seqWheel) > 0, "Sequencia 6-5-4-3-2 supera Wheel A-2-3-4-5 (As vale 1 no Wheel)");

        EvaluatedPokerHand trincaOito = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.OITO, Suit.ESPADAS), c(Rank.OITO, Suit.COPAS), c(Rank.OITO, Suit.OUROS),
                c(Rank.REI, Suit.PAUS), c(Rank.QUATRO, Suit.ESPADAS)
        ));
        EvaluatedPokerHand doisParesAsDama = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.AS, Suit.ESPADAS), c(Rank.AS, Suit.COPAS), c(Rank.DAMA, Suit.OUROS),
                c(Rank.DAMA, Suit.PAUS), c(Rank.NOVE, Suit.ESPADAS)
        ));
        EvaluatedPokerHand doisParesAsDamaKickerDez = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.AS, Suit.OUROS), c(Rank.AS, Suit.PAUS), c(Rank.DAMA, Suit.ESPADAS),
                c(Rank.DAMA, Suit.COPAS), c(Rank.DEZ, Suit.COPAS)
        ));
        EvaluatedPokerHand parDez = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.DEZ, Suit.ESPADAS), c(Rank.DEZ, Suit.OUROS), c(Rank.AS, Suit.COPAS),
                c(Rank.REI, Suit.OUROS), c(Rank.CINCO, Suit.PAUS)
        ));
        EvaluatedPokerHand cartaAltaAs = PokerHandEvaluator.avaliarExatamenteCinco(List.of(
                c(Rank.AS, Suit.ESPADAS), c(Rank.REI, Suit.COPAS), c(Rank.VALETE, Suit.OUROS),
                c(Rank.OITO, Suit.PAUS), c(Rank.QUATRO, Suit.COPAS)
        ));

        afirme(seqWheel.compareTo(trincaOito) > 0, "Sequencia supera Trinca");
        afirme(trincaOito.compareTo(doisParesAsDamaKickerDez) > 0, "Trinca supera Dois Pares");
        afirme(doisParesAsDamaKickerDez.compareTo(doisParesAsDama) > 0, "Dois Pares desempatam no 5o kicker (10 > 9)");
        afirme(doisParesAsDama.compareTo(parDez) > 0, "Dois Pares superam Um Par");
        afirme(parDez.compareTo(cartaAltaAs) > 0, "Um Par supera Carta Alta");
        System.out.println();
    }

    private static void testarSelecaoDaMelhorMaoDeCincoDentreSeteCartas() {
        System.out.println("2. Testando Selecao Combinatoria C(7,5) = 21 Combinacoes:");

        // 7 cartas contendo ao mesmo tempo Trinca de Ases, Full House (Ases com Reis) e Flush de Copas menor:
        // Deve escolher obrigatoriamente o Full House de Ases com Reis!
        List<Card> seteCartas = List.of(
                c(Rank.AS, Suit.COPAS),
                c(Rank.AS, Suit.ESPADAS),
                c(Rank.AS, Suit.OUROS),
                c(Rank.REI, Suit.COPAS),
                c(Rank.REI, Suit.PAUS),
                c(Rank.NOVE, Suit.COPAS),
                c(Rank.QUATRO, Suit.COPAS)
        );
        EvaluatedPokerHand avaliada = PokerHandEvaluator.avaliarMelhorMao(seteCartas);
        afirme(avaliada.getCategoria() == PokerHandRank.FULL_HOUSE,
                "Entre 7 cartas com Flush e Full House, escolhe Full House");
        afirme(avaliada.getCriteriosDesempate().equals(List.of(14, 13)),
                "Full House avaliado exatamente como Ases cheios de Reis [14, 13]");
        afirme(avaliada.getCincoCartasEscolhidas().size() == 5, "Retorna exatamente as 5 cartas vencedoras");
        System.out.println();
    }

    private static void testarDistribuicaoAlternanciaDeBotaoERuasComunitarias() {
        System.out.println("3. Testando Blinds Heads-Up, Alternancia de Botao e Progressao Pre-Flop -> Showdown:");

        // Duas maos completas (9 cartas cada = 18 cartas no baralho deterministico)
        List<Card> cartas = new ArrayList<>();
        // Mao 1 (Botao = JOGADOR / SB = Jogador / BB = Bot):
        // Ordem de distribuicao: J1, B1, J2, B2, Flop(3), Turn(1), River(1)
        cartas.add(c(Rank.AS, Suit.ESPADAS));    // J1
        cartas.add(c(Rank.DOIS, Suit.PAUS));     // B1
        cartas.add(c(Rank.REI, Suit.ESPADAS));   // J2 (Jogador: AKs)
        cartas.add(c(Rank.SETE, Suit.OUROS));    // B2 (Bot: 72o)
        cartas.add(c(Rank.AS, Suit.COPAS));      // Flop 1
        cartas.add(c(Rank.REI, Suit.COPAS));     // Flop 2
        cartas.add(c(Rank.QUATRO, Suit.PAUS));   // Flop 3
        cartas.add(c(Rank.NOVE, Suit.OUROS));    // Turn
        cartas.add(c(Rank.TRES, Suit.ESPADAS));  // River

        // Mao 2 (Botao alterna para BOT / SB = Bot / BB = Jogador):
        cartas.add(c(Rank.TRES, Suit.PAUS));     // B1
        cartas.add(c(Rank.DAMA, Suit.COPAS));    // J1
        cartas.add(c(Rank.OITO, Suit.OUROS));    // B2
        cartas.add(c(Rank.DAMA, Suit.ESPADAS));  // J2 (Jogador: QQ)
        cartas.add(c(Rank.DOIS, Suit.COPAS));    // Flop 1
        cartas.add(c(Rank.CINCO, Suit.ESPADAS));  // Flop 2
        cartas.add(c(Rank.NOVE, Suit.PAUS));     // Flop 3
        cartas.add(c(Rank.VALETE, Suit.OUROS));  // Turn
        cartas.add(c(Rank.QUATRO, Suit.COPAS));  // River

        TexasHoldemGame jogo = new TexasHoldemGame(
                1000L, 1000L, 50L, 100L,
                new Deck(cartas),
                botPassivoSemprePagaOuPassa(),
                PokerParticipant.JOGADOR
        );

        afirme(jogo.getEstagioAtual() == PokerStage.AGUARDANDO_MAO, "Inicia em AGUARDANDO_MAO");
        afirme(jogo.iniciarNovaMao(), "Mao 1 iniciada com sucesso");
        afirme(jogo.getBotaoDealer() == PokerParticipant.JOGADOR, "Mao 1: Jogador ocupa o Botao (Small Blind)");
        afirme(jogo.getSaldoJogador() == 950L && jogo.getSaldoBot() == 900L && jogo.getPoteTotal() == 150L,
                "Blinds 50/100 debitados corretamente no inicio da Mao 1 (Pote = 150)");
        afirme(jogo.getEstagioAtual() == PokerStage.PRE_FLOP, "Estagio atual e PRE_FLOP");
        afirme(jogo.getTurnoAtual() == PokerParticipant.JOGADOR,
                "No Heads-Up Pre-Flop, o Botao (Small Blind) age primeiro");
        afirme(jogo.getValorParaPagarJogador() == 50L, "Jogador precisa de 50 fichas para completar o Big Blind");
        afirme(jogo.getCartasComunitarias().isEmpty(), "Zero cartas comunitarias no Pre-Flop");
        afirme(jogo.hasCartasOcultasBot() && jogo.getCartasVisiveisBot().isEmpty(),
                "Cartas do Bot ocultas no Pre-Flop");

        // Pre-Flop: Jogador paga 50 -> Bot (BB) passa automaticamente -> abre o FLOP e o Bot (fora de posicao) passa -> vez do Jogador!
        afirme(jogo.pagar(), "Jogador pagou os 50 no Pre-Flop");
        afirme(jogo.getEstagioAtual() == PokerStage.FLOP, "Transitou para o FLOP apos check do Big Blind");
        afirme(jogo.getCartasComunitarias().size() == 3, "FLOP abriu exatamente 3 cartas comunitarias");
        afirme(jogo.getTurnoAtual() == PokerParticipant.JOGADOR,
                "Pos-Flop: Bot (fora de posicao) agiu primeiro dando check e passou a vez ao Jogador (Botao)");

        // Flop: Jogador aposta +100 -> Bot paga 100 -> avanca para TURN -> Bot da check -> vez do Jogador
        afirme(jogo.aumentar(100L), "Jogador apostou 100 no Flop");
        afirme(jogo.getEstagioAtual() == PokerStage.TURN, "Apos call do Bot no Flop, transitou para TURN");
        afirme(jogo.getCartasComunitarias().size() == 4, "TURN exibe 4 cartas comunitarias");
        afirme(jogo.getPoteTotal() == 400L, "Pote acumulado em 400 fichas");

        // Turn: Jogador passa -> avanca para RIVER -> Bot da check -> vez do Jogador
        afirme(jogo.passar(), "Jogador passou no Turn");
        afirme(jogo.getEstagioAtual() == PokerStage.RIVER, "Transitou para RIVER");
        afirme(jogo.getCartasComunitarias().size() == 5, "RIVER exibe 5 cartas comunitarias");
        afirme(jogo.hasCartasOcultasBot() && jogo.getCartasVisiveisBot().isEmpty(),
                "Cartas do Bot continuam estritamente ocultas no River");

        // River: Jogador passa -> resolve SHOWDOWN!
        afirme(jogo.passar(), "Jogador passou no River encerrando as apostas");
        afirme(jogo.getEstagioAtual() == PokerStage.SHOWDOWN, "Mao chegou ao SHOWDOWN");
        afirme(!jogo.hasCartasOcultasBot() && jogo.getCartasVisiveisBot().size() == 2,
                "No SHOWDOWN as 2 cartas do Bot sao reveladas");
        afirme(jogo.getResultadoMao() == PokerRoundResult.JOGADOR_VENCEU_SHOWDOWN,
                "Jogador venceu no Showdown com Dois Pares (Ases e Reis)");
        afirme(jogo.getSaldoJogador() == 1200L && jogo.getSaldoBot() == 800L,
                "Pote de 400 liquidado uma unica vez: Jogador = 1200, Bot = 800");

        // Inicia Mao 2: Botao deve alternar para o BOT (Bot posta SB=50 e age primeiro no Pre-Flop pagando 50!)
        afirme(jogo.iniciarNovaMao(), "Mao 2 iniciada com sucesso");
        afirme(jogo.getBotaoDealer() == PokerParticipant.BOT, "Mao 2: Botao alternou corretamente para o BOT");
        afirme(jogo.getEstagioAtual() == PokerStage.PRE_FLOP && jogo.getTurnoAtual() == PokerParticipant.JOGADOR,
                "Bot (SB) completou 50 no Pre-Flop e passou a opcao ao Jogador (BB)");
        afirme(jogo.getPoteTotal() == 200L && jogo.getValorParaPagarJogador() == 0L,
                "No BB apos limp do Bot, Jogador tem opcao gratuita de check (valorParaPagar = 0)");
        System.out.println();
    }

    private static void testarAcoesValidasLimiteDeAumentosEDesistenciaSemVazarBot() {
        System.out.println("4. Testando Validacao de Acoes, Cap de 3 Aumentos por Rua e Desistencia sem Vazamento:");

        // Bot agressivo que sempre aumenta +100 quando permitido, senao paga
        PokerBotStrategy botAgressivo = (maoBot, com, estagio, pote, valorPagar, saldoBot, bb, podeAumentar) ->
                podeAumentar ? PokerBotStrategy.Decision.aumentar(bb) : PokerBotStrategy.Decision.pagar();

        List<Card> cartas = List.of(
                c(Rank.AS, Suit.ESPADAS), c(Rank.REI, Suit.COPAS),
                c(Rank.AS, Suit.COPAS), c(Rank.REI, Suit.OUROS),
                c(Rank.DOIS, Suit.PAUS), c(Rank.SETE, Suit.ESPADAS), c(Rank.NOVE, Suit.COPAS),
                c(Rank.QUATRO, Suit.OUROS), c(Rank.VALETE, Suit.PAUS)
        );

        TexasHoldemGame jogo = new TexasHoldemGame(
                5000L, 5000L, 50L, 100L,
                new Deck(cartas),
                botAgressivo,
                PokerParticipant.JOGADOR
        );
        jogo.iniciarNovaMao();

        // No Pre-Flop com 50 para pagar, PASSAR deve ser proibido!
        Set<PokerAction> acoesIniciais = jogo.getAcoesValidasJogador();
        afirme(!acoesIniciais.contains(PokerAction.PASSAR) && acoesIniciais.contains(PokerAction.PAGAR),
                "Quando ha valor para pagar, PASSAR nao consta nas acoes validas");
        afirme(!jogo.passar(), "Chamar passar() com aposta pendente retorna false sem alterar turno");
        afirme(!jogo.aumentar(50L), "Aumento menor que o Big Blind (50 < 100) e rejeitado");
        afirme(!jogo.aumentar(101L), "Aumento impar (101) fora de all-in e rejeitado");

        // Aumento 1 do Jogador (+100 -> total 200) -> Bot da Raise 2 (+100 -> total 300) -> vez do Jogador
        afirme(jogo.aumentar(100L), "1o aumento da rua aceito (+100)");
        afirme(jogo.getApostaBotNaRua() == 300L && jogo.getApostaJogadorNaRua() == 200L,
                "Bot respondeu com o 2o aumento da rua (300 x 200)");

        // Jogador aplica o 3o e ultimo aumento permitido da rua (+100 -> total 400); Bot (sem poder dar 4o raise) apenas paga!
        afirme(jogo.aumentar(100L), "3o aumento da rua aceito (atingiu MAX_AUMENTOS_POR_RUA = 3)");
        afirme(jogo.getEstagioAtual() == PokerStage.FLOP,
                "Ao atingir cap de 3 aumentos, Bot apenas pagou e a mao avancou ao FLOP");

        // No Flop, Bot (fora de posicao e agressivo) ja abriu apostando 100; Jogador decide DESISTIR (Fold)
        afirme(jogo.getApostaBotNaRua() == 100L, "Bot abriu aposta de 100 no Flop");
        afirme(jogo.desistir(), "Jogador desistiu no Flop");
        afirme(jogo.getEstagioAtual() == PokerStage.ENCERRADA_POR_DESISTENCIA,
                "Estagio transitou para ENCERRADA_POR_DESISTENCIA");
        afirme(jogo.getResultadoMao() == PokerRoundResult.BOT_VENCEU_DESISTENCIA,
                "Resultado registrado como BOT_VENCEU_DESISTENCIA");
        afirme(jogo.getCartasVisiveisBot().isEmpty() && jogo.hasCartasOcultasBot(),
                "Mesmo apos fim por desistencia, as cartas do Bot NAO sao reveladas!");
        afirme(jogo.getMelhorMaoBotShowdown() == null, "Showdown hand do Bot permanece nula em desistencia");
        afirme(jogo.getSaldoJogador() == 4600L && jogo.getSaldoBot() == 5400L,
                "Bot recebeu integralmente o pote (Jogador -400, Bot +400)");
        System.out.println();
    }

    private static void testarShowdownSplitPotEFichaImparForaDePosicao() {
        System.out.println("5. Testando Empate no Showdown (Split Pot Par e Regra da Ficha Impar em All-In):");

        // Cenario A: Mesa forma Royal Flush comunitario (ambos empatam exatamente na mesa -> Split Pot par)
        List<Card> cartasEmpatePar = List.of(
                c(Rank.DOIS, Suit.COPAS), c(Rank.TRES, Suit.PAUS),
                c(Rank.QUATRO, Suit.COPAS), c(Rank.CINCO, Suit.PAUS),
                c(Rank.DEZ, Suit.ESPADAS), c(Rank.VALETE, Suit.ESPADAS), c(Rank.DAMA, Suit.ESPADAS),
                c(Rank.REI, Suit.ESPADAS), c(Rank.AS, Suit.ESPADAS)
        );
        TexasHoldemGame jogoPar = new TexasHoldemGame(
                1000L, 1000L, 50L, 100L,
                new Deck(cartasEmpatePar),
                botPassivoSemprePagaOuPassa(),
                PokerParticipant.JOGADOR
        );
        jogoPar.iniciarNovaMao();
        jogoPar.pagar();  // Pre-flop -> Flop
        jogoPar.passar(); // Flop -> Turn
        jogoPar.passar(); // Turn -> River
        jogoPar.passar(); // River -> Showdown

        afirme(jogoPar.getResultadoMao() == PokerRoundResult.EMPATE_DIVISAO_POTE,
                "Royal Flush na mesa gera EMPATE_DIVISAO_POTE");
        afirme(jogoPar.getSaldoJogador() == 1000L && jogoPar.getSaldoBot() == 1000L,
                "Pote par de 200 dividido igualmente (100 para cada, saldos restaurados em 1000/1000)");

        // Cenario B: All-in impar (saldos 105 e 105 -> pote total 210 e par... entao testemos um caso com ficha impar?
        // Nota: como ambos colocam exatamente o mesmo total em head-to-head com excesso devolvido, 2 * X e sempre par!
        // Validamos que mesmo com saldos impares (ex.: 105 x 105 -> pote 210), cada um recebe exatamente 105 sem perder fichas!)
        TexasHoldemGame jogoAllInImpar = new TexasHoldemGame(
                105L, 105L, 50L, 100L,
                new Deck(cartasEmpatePar),
                botPassivoSemprePagaOuPassa(),
                PokerParticipant.JOGADOR
        );
        jogoAllInImpar.iniciarNovaMao();
        // Jogador (SB=50, restam 55): da all-in aumentando +5 (50 + 55 = 105 -> +5 acima do BB=100)
        afirme(jogoAllInImpar.aumentar(5L), "All-in com saldo quebrado (total 105) e aceito mesmo sendo impar");
        afirme(jogoAllInImpar.getEstagioAtual() == PokerStage.SHOWDOWN,
                "Apos call de all-in do Bot, comunitarias correm direto para o SHOWDOWN");
        afirme(jogoAllInImpar.getSaldoJogador() == 105L && jogoAllInImpar.getSaldoBot() == 105L,
                "Conservacao exata de fichas no split pot em all-in (105 + 105 = 210)");
        System.out.println();
    }

    private static void testarAllInExcessoNaoIgualadoERunoutAutomatico() {
        System.out.println("6. Testando All-In Assimétrico, Devolucao de Excesso Nao Igualado e Runout:");

        // Jogador tem 1000 fichas, Bot tem apenas 160 fichas; Jogador vence no Showdown com Par de Ases
        List<Card> cartas = List.of(
                c(Rank.AS, Suit.ESPADAS), c(Rank.REI, Suit.PAUS),
                c(Rank.AS, Suit.OUROS), c(Rank.DAMA, Suit.PAUS),
                c(Rank.DOIS, Suit.COPAS), c(Rank.SETE, Suit.ESPADAS), c(Rank.NOVE, Suit.OUROS),
                c(Rank.QUATRO, Suit.PAUS), c(Rank.SEIS, Suit.COPAS)
        );
        TexasHoldemGame jogo = new TexasHoldemGame(
                1000L, 160L, 50L, 100L,
                new Deck(cartas),
                botPassivoSemprePagaOuPassa(),
                PokerParticipant.JOGADOR
        );
        jogo.iniciarNovaMao();
        // Jogador (SB=50, saldo=950) aumenta +500 (tentando colocar 600 na rua), mas Bot so tem 160 no total!
        afirme(jogo.aumentar(500L), "Aumento acima do stack do Bot aceito com devolucao automatica do excesso");
        afirme(jogo.getEstagioAtual() == PokerStage.SHOWDOWN,
                "Com o Bot em all-in apos pagar os 60 restantes, as 5 comunitarias abriram ate o SHOWDOWN");
        afirme(jogo.getPoteTotal() == 320L,
                "Pote total limitado exatamente a 2 x 160 = 320 (excesso de 440 devolvido ao Jogador)");
        afirme(jogo.getSaldoJogador() == 1160L && jogo.getSaldoBot() == 0L,
                "Jogador venceu as 160 fichas do Bot (Saldo Jogador = 1160, Bot = 0)");
        afirme(!jogo.iniciarNovaMao(),
                "Em modo deterministico sem recarga automatica, nova mao retorna false quando Bot zera o saldo");
        System.out.println();
    }

    private static void testarSnapshotsImutaveisEIsolamentoDaIAPokerSimpleAI() {
        System.out.println("7. Testando Imutabilidade dos Snapshots e Decisoes Honestas da PokerSimpleAI:");

        List<Card> cartas = List.of(
                c(Rank.VALETE, Suit.ESPADAS), c(Rank.AS, Suit.COPAS),
                c(Rank.DEZ, Suit.ESPADAS), c(Rank.AS, Suit.PAUS),
                c(Rank.AS, Suit.OUROS), c(Rank.REI, Suit.OUROS), c(Rank.DAMA, Suit.ESPADAS),
                c(Rank.DOIS, Suit.PAUS), c(Rank.TRES, Suit.COPAS)
        );
        TexasHoldemGame jogo = new TexasHoldemGame(
                2000L, 2000L, 50L, 100L,
                new Deck(cartas),
                new PokerSimpleAI(),
                PokerParticipant.JOGADOR
        );
        jogo.iniciarNovaMao();

        List<Card> snapshotMao = jogo.getMaoJogador();
        boolean bloqueouMutacaoMao = false;
        try {
            snapshotMao.add(c(Rank.DOIS, Suit.OUROS));
        } catch (UnsupportedOperationException e) {
            bloqueouMutacaoMao = true;
        }
        afirme(bloqueouMutacaoMao, "getMaoJogador() retorna snapshot estritamente imutavel");

        // Jogador paga no Pre-Flop -> Bot tem Par de Ases (AA) no BB, logo PokerSimpleAI deve AUMENTAR!
        afirme(jogo.pagar(), "Jogador completou o Big Blind no Pre-Flop");
        afirme(jogo.getEstagioAtual() == PokerStage.PRE_FLOP && jogo.getValorParaPagarJogador() == 100L,
                "PokerSimpleAI reconheceu Par de Ases (AA) no Pre-Flop e aumentou +100");

        // Jogador paga o raise -> no Flop (A-K-Q), Jogador fez Sequencia (A-K-Q-J-10) e Bot fez Trinca de Ases!
        afirme(jogo.pagar(), "Jogador pagou o aumento pre-flop");
        afirme(jogo.getEstagioAtual() == PokerStage.FLOP, "Avancou ao Flop com A-K-Q na mesa");
        afirme(jogo.getApostaBotNaRua() == 100L, "PokerSimpleAI apostou por valor no Flop com Trinca de Ases");

        jogo.pagar();  // Flop -> Turn
        jogo.pagar();  // Turn (Bot aposta com Trinca -> Jogador paga) -> River
        jogo.pagar();  // River (Bot aposta com Trinca -> Jogador paga) -> Showdown!

        afirme(jogo.getEstagioAtual() == PokerStage.SHOWDOWN, "Partida contra PokerSimpleAI chegou ao SHOWDOWN");
        afirme(jogo.getMelhorMaoJogadorShowdown().getCategoria() == PokerHandRank.SEQUENCIA,
                "Jogador formou Sequencia Broadway (10-J-Q-K-A)");
        afirme(jogo.getMelhorMaoBotShowdown().getCategoria() == PokerHandRank.TRINCA,
                "Bot formou Trinca de Ases");
        afirme(jogo.getResultadoMao() == PokerRoundResult.JOGADOR_VENCEU_SHOWDOWN,
                "Sequencia do Jogador superou a Trinca de Ases do Bot no Showdown");
    }
}
