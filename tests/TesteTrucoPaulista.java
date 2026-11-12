package tests;

import model.Card;
import model.Rank;
import model.Suit;
import model.TrucoCardComparator;
import model.TrucoDeck;
import model.TrucoHandResult;
import model.TrucoPlayer;
import model.TrucoRound;
import model.TrucoRules;
import model.TrucoSimpleAI;
import model.TrucoTrick;
import model.TrucoTrickResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bateria de testes deterministicos para o motor do Truco Paulista.
 *
 * Valida:
 * 1. Baralho limpo de 40 cartas (ausencia de 8, 9, 10).
 * 2. Sequencia ciclica da vira para a manilha (inclusive a virada de 3 para 4).
 * 3. Ordem de forca dos naipes das manilhas (Paus > Copas > Espadas > Ouros).
 * 4. Empate entre cartas comuns de mesmo valor (naipes ignorados).
 * 5. Comparacao de cartas via TrucoCardComparator e TrucoRules.
 * 6. Tabela de resolucao de vazas:
 *    - Encerramento antecipado por 2x0
 *    - Vitoria 2x1 na terceira vaza
 *    - Empate na 1a vaza e vitoria na 2a vaza
 *    - Empate na 2a vaza com vitoria de quem levou a 1a
 *    - Empate na 3a vaza com vitoria de quem levou a 1a
 *    - Empate triplice (3 cangas seguidas)
 * 7. Alternancia de iniciativa (quem comeca cada vaza, inclusive apos empate).
 * 8. Validacao de turnos e bloqueio contra jogar cartas repetidas ou fora da vez.
 * 9. Isolamento do Bot e tomada de decisao estrategica.
 */
public class TesteTrucoPaulista {

    private static int totalVerificacoes = 0;
    private static int falhas = 0;

    private static void afirme(boolean condicao, String mensagem) {
        totalVerificacoes++;
        if (condicao) {
            System.out.println("  [OK] " + mensagem);
        } else {
            System.err.println("  [FALHA] " + mensagem);
            falhas++;
        }
    }

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DETERMINISTICOS DO MOTOR TRUCO PAULISTA   ");
        System.out.println("====================================================");

        testarBaralhoLimpo();
        testarSequenciaCiclicaManilhas();
        testarHierarquiaNaipesManilha();
        testarComparacaoCartasComunsEEmpate();
        testarManilhaVsCartaComum();
        testarTabelaResolucaoVazas();
        testarControleDeTurnosEValidacoes();
        testarIsolamentoEDecisaoDoBot();
        testarImutabilidadeDeConsultaVazas();

        System.out.println("\n----------------------------------------------------");
        System.out.println("Total de verificacoes do Truco: " + totalVerificacoes);
        System.out.println("Falhas: " + falhas);
        if (falhas == 0) {
            System.out.println("RESULTADO: TODOS OS TESTES DO TRUCO PASSARAM COM SUCESSO! [OK]");
        } else {
            System.err.println("RESULTADO: EXISTEM FALHAS NOS TESTES DO TRUCO!");
            System.exit(1);
        }
        System.out.println("----------------------------------------------------");
    }

    private static void testarBaralhoLimpo() {
        System.out.println("\n1. Testando Baralho Limpo de 40 Cartas:");
        TrucoDeck deck = new TrucoDeck();
        afirme(deck.getCartasRestantes() == 40, "Baralho limpo possui exatamente 40 cartas");

        List<Card> cartas = deck.getCartas();
        boolean temOito = cartas.stream().anyMatch(c -> c.getRank() == Rank.OITO);
        boolean temNove = cartas.stream().anyMatch(c -> c.getRank() == Rank.NOVE);
        boolean temDez = cartas.stream().anyMatch(c -> c.getRank() == Rank.DEZ);

        afirme(!temOito, "Baralho limpo nao contem cartas de valor 8");
        afirme(!temNove, "Baralho limpo nao contem cartas de valor 9");
        afirme(!temDez, "Baralho limpo nao contem cartas de valor 10");

        afirme(TrucoRules.RANKS_TRUCO.size() == 10, "Existem exatamente 10 ranks validos no Truco");
        afirme(!TrucoRules.isRankValidoNoTruco(Rank.OITO), "Rank.OITO e invalido no Truco");
        afirme(TrucoRules.isRankValidoNoTruco(Rank.SETE), "Rank.SETE e valido no Truco");
    }

    private static void testarSequenciaCiclicaManilhas() {
        System.out.println("\n2. Testando Sequencia Ciclica da Vira para a Manilha:");
        // 4 -> 5 -> 6 -> 7 -> Q -> J -> K -> A -> 2 -> 3 -> 4
        afirme(TrucoRules.determinarManilha(Rank.QUATRO) == Rank.CINCO, "Vira 4 define manilha 5");
        afirme(TrucoRules.determinarManilha(Rank.CINCO) == Rank.SEIS, "Vira 5 define manilha 6");
        afirme(TrucoRules.determinarManilha(Rank.SEIS) == Rank.SETE, "Vira 6 define manilha 7");
        afirme(TrucoRules.determinarManilha(Rank.SETE) == Rank.DAMA, "Vira 7 define manilha Dama (Q)");
        afirme(TrucoRules.determinarManilha(Rank.DAMA) == Rank.VALETE, "Vira Dama define manilha Valete (J)");
        afirme(TrucoRules.determinarManilha(Rank.VALETE) == Rank.REI, "Vira Valete define manilha Rei (K)");
        afirme(TrucoRules.determinarManilha(Rank.REI) == Rank.AS, "Vira Rei define manilha As (A)");
        afirme(TrucoRules.determinarManilha(Rank.AS) == Rank.DOIS, "Vira As define manilha 2");
        afirme(TrucoRules.determinarManilha(Rank.DOIS) == Rank.TRES, "Vira 2 define manilha 3");
        afirme(TrucoRules.determinarManilha(Rank.TRES) == Rank.QUATRO, "Virada ciclica: Vira 3 define manilha 4!");
    }

    private static void testarHierarquiaNaipesManilha() {
        System.out.println("\n3. Testando Ordem dos Naipes entre Manilhas (Os Quatro Tombos):");
        // Paus (4 - Zap) > Copas (3 - Copeta) > Espadas (2 - Espadilha) > Ouros (1 - Picafumo)
        afirme(TrucoRules.getForcaNaipeManilha(Suit.PAUS) > TrucoRules.getForcaNaipeManilha(Suit.COPAS),
                "Zap (Paus) e mais forte que Copeta (Copas)");
        afirme(TrucoRules.getForcaNaipeManilha(Suit.COPAS) > TrucoRules.getForcaNaipeManilha(Suit.ESPADAS),
                "Copeta (Copas) e mais forte que Espadilha (Espadas)");
        afirme(TrucoRules.getForcaNaipeManilha(Suit.ESPADAS) > TrucoRules.getForcaNaipeManilha(Suit.OUROS),
                "Espadilha (Espadas) e mais forte que Picafumo (Ouros)");

        // Comparando manilhas de mesmo rank diretamente
        Card vira = new Card(Suit.COPAS, Rank.SEIS); // Manilha e 7
        Card zap = new Card(Suit.PAUS, Rank.SETE);
        Card copeta = new Card(Suit.COPAS, Rank.SETE);
        Card espadilha = new Card(Suit.ESPADAS, Rank.SETE);
        Card picafumo = new Card(Suit.OUROS, Rank.SETE);

        afirme(TrucoRules.compararCartas(zap, copeta, vira) > 0, "Zap (7 de Paus) vence Copeta (7 de Copas)");
        afirme(TrucoRules.compararCartas(copeta, espadilha, vira) > 0, "Copeta (7 de Copas) vence Espadilha (7 de Espadas)");
        afirme(TrucoRules.compararCartas(espadilha, picafumo, vira) > 0, "Espadilha (7 de Espadas) vence Picafumo (7 de Ouros)");
        afirme(TrucoRules.compararCartas(picafumo, zap, vira) < 0, "Picafumo (7 de Ouros) perde para Zap (7 de Paus)");
    }

    private static void testarComparacaoCartasComunsEEmpate() {
        System.out.println("\n4. Testando Forca das Cartas Comuns e Empate (Canga):");
        // Hierarquia comum: 4 < 5 < 6 < 7 < Q < J < K < A < 2 < 3
        Card vira = new Card(Suit.ESPADAS, Rank.QUATRO); // Manilha e 5, logo 4, 7, Q, J, K, A, 2, 3 sao comuns

        Card tresCopas = new Card(Suit.COPAS, Rank.TRES);
        Card tresOuros = new Card(Suit.OUROS, Rank.TRES);
        Card doisPaus = new Card(Suit.PAUS, Rank.DOIS);
        Card asEspadas = new Card(Suit.ESPADAS, Rank.AS);
        Card reiOuros = new Card(Suit.OUROS, Rank.REI);
        Card valeteCopas = new Card(Suit.COPAS, Rank.VALETE);
        Card damaPaus = new Card(Suit.PAUS, Rank.DAMA);
        Card seteOuros = new Card(Suit.OUROS, Rank.SETE);

        afirme(TrucoRules.compararCartas(tresCopas, doisPaus, vira) > 0, "3 comum vence 2 comum");
        afirme(TrucoRules.compararCartas(doisPaus, asEspadas, vira) > 0, "2 comum vence As comum");
        afirme(TrucoRules.compararCartas(asEspadas, reiOuros, vira) > 0, "As comum vence Rei comum");
        afirme(TrucoRules.compararCartas(reiOuros, valeteCopas, vira) > 0, "Rei comum vence Valete comum");
        afirme(TrucoRules.compararCartas(valeteCopas, damaPaus, vira) > 0, "Valete comum vence Dama comum");
        afirme(TrucoRules.compararCartas(damaPaus, seteOuros, vira) > 0, "Dama comum vence 7 comum");

        // Empate de cartas comuns de naipes diferentes:
        afirme(TrucoRules.compararCartas(tresCopas, tresOuros, vira) == 0,
                "Cartas comuns de mesmo valor (3 de Copas vs 3 de Ouros) EMPATAM perfeitamente (canga)");

        Card seteCopas = new Card(Suit.COPAS, Rank.SETE);
        afirme(TrucoRules.compararCartas(seteOuros, seteCopas, vira) == 0,
                "Dois 7 comuns de naipes diferentes resultam em empate (canga)");

        TrucoCardComparator comparador = new TrucoCardComparator(vira);
        afirme(comparador.compare(tresCopas, tresOuros) == 0, "TrucoCardComparator reflete o empate em cartas comuns");
    }

    private static void testarManilhaVsCartaComum() {
        System.out.println("\n5. Testando Manilha contra Carta Comum:");
        // Vira: 3 -> Manilha: 4 (a carta ordinalmente mais baixa comum se torna a mais forte!)
        Card vira = new Card(Suit.OUROS, Rank.TRES);
        Card manilhaQuatroOuros = new Card(Suit.OUROS, Rank.QUATRO); // Picafumo (manilha mais fraca)
        Card tresDePaus = new Card(Suit.PAUS, Rank.TRES);             // Maior carta comum

        afirme(TrucoRules.isManilha(manilhaQuatroOuros, vira), "4 de Ouros e manilha com vira 3");
        afirme(!TrucoRules.isManilha(tresDePaus, vira), "3 de Paus nao e manilha com vira 3");
        afirme(TrucoRules.compararCartas(manilhaQuatroOuros, tresDePaus, vira) > 0,
                "A manilha mais fraca (4 de Ouros) VENCE a maior carta comum (3 de Paus)!");
    }

    private static void testarTabelaResolucaoVazas() {
        System.out.println("\n6. Testando Tabela Oficial de Resolucao de Vazas e Alternancia de Vez:");

        Card vira = new Card(Suit.OUROS, Rank.VALETE); // Manilha e Rei (K)

        // Cenário 6.1: Vitória antecipada por 2x0
        {
            List<Card> maoJ = Arrays.asList(new Card(Suit.PAUS, Rank.TRES), new Card(Suit.COPAS, Rank.TRES), new Card(Suit.OUROS, Rank.TRES));
            List<Card> maoB = Arrays.asList(new Card(Suit.ESPADAS, Rank.QUATRO), new Card(Suit.COPAS, Rank.QUATRO), new Card(Suit.OUROS, Rank.QUATRO));
            TrucoRound rodada = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);

            rodada.jogarCartaJogador(maoJ.get(0)); // Joga 3 (Bot responde com menor: 4)
            afirme(rodada.getVazas().get(0).getResultado() == TrucoTrickResult.VITORIA_JOGADOR, "Vaza 1: Jogador venceu");
            afirme(!rodada.isFinalizada(), "Mao nao finaliza apos vaza 1");
            afirme(rodada.getVezAtual() == TrucoPlayer.JOGADOR, "Vencedor da vaza 1 joga primeiro na vaza 2");

            rodada.jogarCartaJogador(maoJ.get(1)); // Joga 3 (Bot responde com 4)
            afirme(rodada.getVazas().size() == 2, "Encerramento antecipado: apenas 2 vazas foram disputadas");
            afirme(rodada.isFinalizada(), "Mao foi finalizada apos 2x0");
            afirme(rodada.getResultadoMao() == TrucoHandResult.VITORIA_JOGADOR, "Resultado final: Vitoria do Jogador por 2x0");
        }

        // Cenário 6.2: Vitória 2x1 na terceira vaza
        {
            // V1: Jogador vence, V2: Bot vence, V3: Jogador vence
            List<Card> maoJ = Arrays.asList(new Card(Suit.PAUS, Rank.TRES), new Card(Suit.COPAS, Rank.QUATRO), new Card(Suit.OUROS, Rank.TRES));
            List<Card> maoB = Arrays.asList(new Card(Suit.ESPADAS, Rank.CINCO), new Card(Suit.COPAS, Rank.DOIS), new Card(Suit.OUROS, Rank.DOIS));
            TrucoRound rodada = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);

            rodada.jogarCartaJogador(maoJ.get(0)); // Joga 3, Bot joga menor
            afirme(rodada.getVazas().get(0).getResultado() == TrucoTrickResult.VITORIA_JOGADOR, "Vaza 1: Jogador venceu");
            afirme(rodada.getVezAtual() == TrucoPlayer.JOGADOR, "Jogador abre a Vaza 2");

            rodada.jogarCartaJogador(maoJ.get(1)); // Jogador joga 4, Bot supera e vence
            afirme(rodada.getVazas().get(1).getResultado() == TrucoTrickResult.VITORIA_BOT, "Vaza 2: Bot venceu");

            // Como o Bot venceu a V2, o Bot abre a V3. Como a IA e automatica, o Bot ja jogou na abertura da V3!
            afirme(rodada.getVazaAtual().getPrimeiroAJogar() == TrucoPlayer.BOT, "Vencedor da vaza 2 (Bot) abriu a Vaza 3");
            afirme(rodada.getVazaAtual().getCartaBot() != null, "Bot realizou a jogada de abertura da Vaza 3");
            afirme(rodada.getVezAtual() == TrucoPlayer.JOGADOR, "Jogador responde na Vaza 3");

            rodada.jogarCartaJogador(maoJ.get(2)); // Jogador joga 3 e supera a carta do Bot

            afirme(rodada.getVazas().size() == 3, "Três vazas disputadas em 2x1");
            afirme(rodada.isFinalizada(), "Mao finalizada");
            afirme(rodada.getResultadoMao() == TrucoHandResult.VITORIA_JOGADOR, "Resultado final: Vitoria do Jogador por 2x1");
        }

        // Cenário 6.3: Empate na 1ª vaza -> Quem vencer a 2ª leva a mão
        {
            List<Card> maoJ = Arrays.asList(new Card(Suit.COPAS, Rank.SETE), new Card(Suit.PAUS, Rank.TRES), new Card(Suit.OUROS, Rank.QUATRO));
            List<Card> maoB = Arrays.asList(new Card(Suit.OUROS, Rank.SETE), new Card(Suit.ESPADAS, Rank.QUATRO), new Card(Suit.COPAS, Rank.QUATRO));
            TrucoRound rodada = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);

            rodada.jogarCartaJogador(maoJ.get(0)); // Joga 7 de Copas. Bot tem 7 de Ouros e canga a vaza
            afirme(rodada.getVazas().get(0).getResultado() == TrucoTrickResult.EMPATE, "Vaza 1 empatou (canga)");
            afirme(rodada.getVezAtual() == TrucoPlayer.JOGADOR, "Apos empate na Vaza 1, quem comecou continua abrindo a Vaza 2");

            rodada.jogarCartaJogador(maoJ.get(1)); // Joga 3 e vence a Vaza 2
            afirme(rodada.getVazas().get(1).getResultado() == TrucoTrickResult.VITORIA_JOGADOR, "Vaza 2: Jogador venceu");
            afirme(rodada.isFinalizada(), "Mao finalizou antecipadamente apos empate na 1a e vitoria na 2a");
            afirme(rodada.getResultadoMao() == TrucoHandResult.VITORIA_JOGADOR, "Resultado: Jogador levou a mao por vencer a 2a vaza apos canga na 1a");
        }

        // Cenário 6.4: Empate na 2ª vaza -> Quem venceu a 1ª leva a mão antecipadamente
        {
            List<Card> maoJ = Arrays.asList(new Card(Suit.PAUS, Rank.TRES), new Card(Suit.COPAS, Rank.SETE), new Card(Suit.OUROS, Rank.QUATRO));
            List<Card> maoB = Arrays.asList(new Card(Suit.ESPADAS, Rank.QUATRO), new Card(Suit.OUROS, Rank.SETE), new Card(Suit.ESPADAS, Rank.CINCO));
            TrucoRound rodada = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);

            rodada.jogarCartaJogador(maoJ.get(0)); // Joga 3 e vence a Vaza 1
            afirme(rodada.getVazas().get(0).getResultado() == TrucoTrickResult.VITORIA_JOGADOR, "Vaza 1: Jogador venceu");

            rodada.jogarCartaJogador(maoJ.get(1)); // Joga 7 de Copas, Bot responde com 7 de Ouros (empate)
            afirme(rodada.getVazas().get(1).getResultado() == TrucoTrickResult.EMPATE, "Vaza 2: Empatou (canga)");
            afirme(rodada.isFinalizada(), "Mao finaliza antecipadamente por empate na 2a vaza");
            afirme(rodada.getResultadoMao() == TrucoHandResult.VITORIA_JOGADOR, "Vencedor da 1a vaza ganha a mao apos empate na 2a");
        }

        // Cenário 6.5: Empate na 3ª vaza -> Quem venceu a 1ª leva a mão
        {
            // V1: Bot vence, V2: Jogador vence, V3: Empate
            List<Card> maoJ = Arrays.asList(new Card(Suit.COPAS, Rank.QUATRO), new Card(Suit.PAUS, Rank.TRES), new Card(Suit.ESPADAS, Rank.DOIS));
            List<Card> maoB = Arrays.asList(new Card(Suit.ESPADAS, Rank.CINCO), new Card(Suit.OUROS, Rank.QUATRO), new Card(Suit.COPAS, Rank.DOIS));
            TrucoRound rodada = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);

            rodada.jogarCartaJogador(maoJ.get(0)); // Joga 4, Bot joga 5 e vence V1
            afirme(rodada.getVazas().get(0).getResultado() == TrucoTrickResult.VITORIA_BOT, "Vaza 1: Bot venceu");

            // Bot abriu Vaza 2 com sua menor carta (4 de Ouros). Jogador responde com 3 e vence V2
            rodada.jogarCartaJogador(maoJ.get(1));
            afirme(rodada.getVazas().get(1).getResultado() == TrucoTrickResult.VITORIA_JOGADOR, "Vaza 2: Jogador venceu");

            // Jogador abre Vaza 3 com 2 de Espadas. Bot responde com 2 de Copas (empate na V3)
            rodada.jogarCartaJogador(maoJ.get(2));
            afirme(rodada.getVazas().get(2).getResultado() == TrucoTrickResult.EMPATE, "Vaza 3: Empatou");
            afirme(rodada.isFinalizada(), "Mao finalizada");
            afirme(rodada.getResultadoMao() == TrucoHandResult.VITORIA_BOT, "Empate na 3a vaza: Bot leva a mao por ter vencido a 1a vaza!");
        }

        // Cenário 6.6: Três empates seguidos -> Mão sem vencedor (EMPATE)
        {
            // Para forcar 3 empates, cada carta jogada pelo jogador e a carta mais alta comum (ou o bot so tem a que empata):
            // V1: 3 de Copas vs 3 de Ouros (nenhuma carta supera o 3)
            // V2: 2 de Copas vs 2 de Ouros (nenhuma carta restante do bot supera o 2)
            // V3: As de Copas vs As de Ouros
            List<Card> maoJ = Arrays.asList(new Card(Suit.COPAS, Rank.TRES), new Card(Suit.COPAS, Rank.DOIS), new Card(Suit.COPAS, Rank.AS));
            List<Card> maoB = Arrays.asList(new Card(Suit.OUROS, Rank.TRES), new Card(Suit.OUROS, Rank.DOIS), new Card(Suit.OUROS, Rank.AS));
            TrucoRound rodada = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);

            rodada.jogarCartaJogador(maoJ.get(0)); // 3 vs 3
            afirme(rodada.getVazas().get(0).getResultado() == TrucoTrickResult.EMPATE, "Vaza 1: Empate");

            rodada.jogarCartaJogador(maoJ.get(1)); // 2 vs 2
            afirme(rodada.getVazas().get(1).getResultado() == TrucoTrickResult.EMPATE, "Vaza 2: Empate");

            rodada.jogarCartaJogador(maoJ.get(2)); // As vs As
            afirme(rodada.getVazas().get(2).getResultado() == TrucoTrickResult.EMPATE, "Vaza 3: Empate");

            afirme(rodada.isFinalizada(), "Mao finalizada apos 3 empates");
            afirme(rodada.getResultadoMao() == TrucoHandResult.EMPATE, "Tres empates seguidos: mao anulada/sem vencedor (EMPATE)");
        }
    }

    private static void testarControleDeTurnosEValidacoes() {
        System.out.println("\n7. Testando Controle de Turno e Protecao contra Jogadas Invalidas:");
        Card vira = new Card(Suit.COPAS, Rank.SETE);
        Card c1 = new Card(Suit.PAUS, Rank.QUATRO);
        Card c2 = new Card(Suit.COPAS, Rank.CINCO);
        Card c3 = new Card(Suit.ESPADAS, Rank.SEIS);
        List<Card> maoJ = Arrays.asList(c1, c2, c3);

        List<Card> maoB = Arrays.asList(
                new Card(Suit.OUROS, Rank.QUATRO),
                new Card(Suit.OUROS, Rank.CINCO),
                new Card(Suit.OUROS, Rank.SEIS)
        );

        // Turno inicial do Jogador
        TrucoRound rodada = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);
        afirme(rodada.isTurnoJogador(), "E o turno do jogador");
        afirme(!rodada.isTurnoBot(), "Nao e o turno do Bot");

        // Tentativa de jogar carta nula
        boolean pegouNulo = false;
        try {
            rodada.jogarCartaJogador(null);
        } catch (IllegalArgumentException e) {
            pegouNulo = true;
        }
        afirme(pegouNulo, "Jogar carta nula lanca IllegalArgumentException");

        // Tentativa de jogar carta que nao pertence a mao do jogador
        Card cartaInexistente = new Card(Suit.PAUS, Rank.REI);
        boolean pegouInexistente = false;
        try {
            rodada.jogarCartaJogador(cartaInexistente);
        } catch (IllegalArgumentException e) {
            pegouInexistente = true;
        }
        afirme(pegouInexistente, "Jogar carta inexistente na mao lanca IllegalArgumentException");

        // Jogador joga c1 com sucesso
        rodada.jogarCartaJogador(c1);
        afirme(!rodada.getMaoJogador().contains(c1), "Carta jogada c1 foi removida da mao do jogador");

        // Tentativa de jogar a mesma carta c1 duas vezes
        boolean pegouRepetida = false;
        try {
            rodada.jogarCartaJogador(c1);
        } catch (IllegalArgumentException e) {
            pegouRepetida = true;
        }
        afirme(pegouRepetida, "Jogar a mesma carta duas vezes e bloqueado com IllegalArgumentException");

        // Tentativa de chamar jogarTurnoBot quando nao e a vez dele
        boolean pegouTurnoInvalido = false;
        try {
            rodada.jogarTurnoBot();
        } catch (IllegalStateException e) {
            pegouTurnoInvalido = true;
        }
        afirme(pegouTurnoInvalido, "Chamar jogada do Bot fora do turno lanca IllegalStateException");
    }

    private static void testarIsolamentoEDecisaoDoBot() {
        System.out.println("\n8. Testando Isolamento e Inteligencia do Bot Simples:");
        TrucoSimpleAI bot = new TrucoSimpleAI();
        Card vira = new Card(Suit.COPAS, Rank.SETE); // Manilha: Dama (Q)

        Card zap = new Card(Suit.PAUS, Rank.DAMA);      // Manilha forte
        Card tres = new Card(Suit.ESPADAS, Rank.TRES);   // Carta comum forte
        Card quatro = new Card(Suit.OUROS, Rank.QUATRO); // Carta comum fraca

        List<Card> maoBot = new ArrayList<>(Arrays.asList(zap, tres, quatro));

        // Cenário A: Bot abre a vaza -> Deve escolher a menor carta (4 de Ouros) para economizar recursos
        Card escolhaAbertura = bot.escolherCarta(maoBot, vira, null);
        afirme(escolhaAbertura.equals(quatro), "Quando abre a vaza, o Bot escolhe sua menor carta (4) para sondar");

        // Cenário B: Adversário jogou um 2. Bot tem 4, 3 e Zap.
        // O 3 supera o 2, e o Zap tambem. O Bot deve escolher o 3 (menor vencedora) para poupar o Zap!
        Card adversarioDois = new Card(Suit.COPAS, Rank.DOIS);
        Card escolhaResposta = bot.escolherCarta(maoBot, vira, adversarioDois);
        afirme(escolhaResposta.equals(tres), "Respondendo ao 2, Bot usa a menor vencedora (3), poupando a manilha (Zap)");

        // Cenário C: Adversário jogou uma manilha mais forte (Copeta). Bot tem 4 e 3 comuns.
        // Nenhuma vence a Copeta. O Bot deve descartar a menor carta (4) como lixo, guardando o 3.
        List<Card> maoSemZap = Arrays.asList(tres, quatro);
        Card copetaAdversario = new Card(Suit.COPAS, Rank.DAMA);
        Card escolhaDescarte = bot.escolherCarta(maoSemZap, vira, copetaAdversario);
        afirme(escolhaDescarte.equals(quatro), "Quando nao pode vencer, Bot descarta a menor carta (4 de lixo)");
    }

    private static void testarImutabilidadeDeConsultaVazas() {
        System.out.println("\n9. Testando Imutabilidade de Consulta de getVazaAtual() e getVazas():");

        Card vira = new Card(Suit.COPAS, Rank.SETE);
        List<Card> maoJ = Arrays.asList(
                new Card(Suit.PAUS, Rank.QUATRO),
                new Card(Suit.COPAS, Rank.CINCO),
                new Card(Suit.ESPADAS, Rank.SEIS)
        );
        List<Card> maoB = Arrays.asList(
                new Card(Suit.OUROS, Rank.QUATRO),
                new Card(Suit.OUROS, Rank.CINCO),
                new Card(Suit.OUROS, Rank.SEIS)
        );

        TrucoRound rodada = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);

        // 1. Coleção getVazas() é imutável
        List<TrucoTrick> listaVazas = rodada.getVazas();
        boolean pegouAdd = false;
        try {
            listaVazas.add(new TrucoTrick(1, TrucoPlayer.JOGADOR, vira));
        } catch (UnsupportedOperationException e) {
            pegouAdd = true;
        }
        afirme(pegouAdd, "getVazas() lanca UnsupportedOperationException ao tentar adicionar elemento");

        boolean pegouClear = false;
        try {
            listaVazas.clear();
        } catch (UnsupportedOperationException e) {
            pegouClear = true;
        }
        afirme(pegouClear, "getVazas() lanca UnsupportedOperationException ao tentar limpar a colecao");

        // 2. Todos os campos declarados de TrucoTrick são final (imutabilidade estrutural)
        java.lang.reflect.Field[] campos = TrucoTrick.class.getDeclaredFields();
        boolean todosCamposFinal = true;
        for (java.lang.reflect.Field campo : campos) {
            if (!java.lang.reflect.Modifier.isFinal(campo.getModifiers())) {
                todosCamposFinal = false;
                break;
            }
        }
        afirme(todosCamposFinal, "Todos os campos de TrucoTrick sao estritamente 'final' (imutaveis)");

        // 3. Nenhum método mutador público exposto em TrucoTrick
        java.lang.reflect.Method[] metodos = TrucoTrick.class.getMethods();
        boolean temMutadorPublico = false;
        for (java.lang.reflect.Method m : metodos) {
            if (m.getName().startsWith("set") || m.getName().equals("registrarJogada")) {
                temMutadorPublico = true;
                break;
            }
        }
        afirme(!temMutadorPublico, "TrucoTrick nao expoem metodos mutadores publicos (setters ou registrarJogada)");

        // 4. Salvar estado original do TrucoRound
        List<Card> cartasJogadorOriginal = new ArrayList<>(rodada.getMaoJogador());
        TrucoPlayer vezOriginal = rodada.getVezAtual();
        TrucoHandResult resultadoOriginal = rodada.getResultadoMao();
        int qtdCartasBotOriginal = rodada.getQuantidadeCartasBot();

        // 5. Consultar a vaza atual e seus dados diversas vezes
        TrucoTrick vazaConsultada = rodada.getVazaAtual();
        afirme(vazaConsultada != null, "getVazaAtual() retorna dados da vaza ativa");
        afirme(vazaConsultada.getNumeroVaza() == 1, "Vaza consultada e a vaza 1");
        afirme(vazaConsultada.getCartaJogador() == null, "Carta do jogador na vaza consultada e inicialmente null");
        afirme(vazaConsultada.getCartaBot() == null, "Carta do bot na vaza consultada e inicialmente null");
        afirme(vazaConsultada.getResultado() == null, "Resultado da vaza consultada e inicialmente null");

        // 6. Confirmar que consultas NUNCA alteram as cartas, o turno ou o resultado
        afirme(rodada.getMaoJogador().equals(cartasJogadorOriginal),
                "Consultar a vaza nao altera as cartas da mao do jogador");
        afirme(rodada.getQuantidadeCartasBot() == qtdCartasBotOriginal,
                "Consultar a vaza nao altera a quantidade de cartas do bot");
        afirme(rodada.getVezAtual() == vezOriginal,
                "Consultar a vaza nao altera o turno da rodada (continua sendo " + vezOriginal + ")");
        afirme(rodada.getResultadoMao() == resultadoOriginal,
                "Consultar a vaza nao altera o resultado da mao (continua sendo " + resultadoOriginal + ")");

        // 7. Confirmar que apenas TrucoRound pode registrar jogadas e transitar estado
        rodada.jogarCartaJogador(maoJ.get(0));
        afirme(rodada.getMaoJogador().size() == 2,
                "Apenas o ponto de entrada TrucoRound.jogarCartaJogador altera o estado da rodada");
        afirme(rodada.getVazas().get(0).getCartaJogador().equals(maoJ.get(0)),
                "Apos jogada oficial via TrucoRound, novo snapshot da vaza 1 reflete a carta jogada");
        afirme(vazaConsultada.getCartaJogador() == null,
                "Snapshot obtido anteriormente permanece intacto e imutavel (valor original null)");
        afirme(rodada.getVazas().size() == 2,
                "Apos resolucao da vaza 1, TrucoRound iniciou a vaza 2 com novo snapshot imutavel");
    }
}
