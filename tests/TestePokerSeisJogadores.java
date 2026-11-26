package tests;

import model.Card;
import model.Deck;
import model.PokerAction;
import model.PokerBotStrategy;
import model.PokerParticipant;
import model.PokerSeatState;
import model.PokerSidePot;
import model.PokerStage;
import model.Rank;
import model.Suit;
import model.TexasHoldemTableGame;
import view.PokerTableView;
import view.ScreenSaver;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Suíte determinística de testes para o Poker Texas Hold'em com 6 participantes:
 * - Ordem de turnos (Pré-Flop UTG e Pós-Flop pós-Dealer) e ocultação das cartas dos 5 Bots
 * - Desistências múltiplas sem vazamento de cartas
 * - All-ins com stacks diferentes, Pote Principal + Side Pots e conservação exata de fichas
 * - Empate com divisão de pote e ficha ímpar (odd chip)
 * - Transição automática para regra de Heads-Up quando restam apenas 2 participantes com fichas
 * - Pausa visível entre turnos dos Bots (~0,85s), pausa ao sair com B e retomada exata com E
 */
public class TestePokerSeisJogadores {

    private static int verificacoes = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DE POKER TEXAS HOLD'EM (6 PARTICIPANTES)");
        System.out.println("====================================================");

        testarSeisLugaresOrdemEOcultacaoBots();
        testarDesistenciasMultiplasSemVazamento();
        testarAllInStacksDiferentesESidePots();
        testarEmpateDivisaoPoteEFichaImpar();
        testarTransicaoAutomaticaHeadsUp();
        testarPausaBotsRetomadaBERenderizacao();

        System.out.println("----------------------------------------------------");
        System.out.println("Total de verificacoes (Poker 6 Participantes): " + verificacoes);
        System.out.println("RESULTADO: TODOS OS 6 TESTES DE POKER 6-MAX PASSARAM! [OK]");
        System.out.println("----------------------------------------------------");
    }

    private static void afirme(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) {
            throw new AssertionError("FALHA: " + mensagem);
        }
    }

    private static Card c(Rank r, Suit s) {
        return new Card(s, r);
    }

    private static Deck criarBaralhoDeterministico(Card... cartasTopo) {
        List<Card> lista = new ArrayList<>();
        for (Card card : cartasTopo) {
            lista.add(card);
        }
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                Card cand = new Card(s, r);
                boolean jaExiste = false;
                for (Card existente : lista) {
                    if (existente.getRank() == cand.getRank() && existente.getSuit() == cand.getSuit()) {
                        jaExiste = true;
                        break;
                    }
                }
                if (!jaExiste) {
                    lista.add(cand);
                }
            }
        }
        return new Deck(lista);
    }

    private static PokerBotStrategy botSempreCallOuCheck() {
        return (mao, board, estagio, pote, paraPagar, saldo, bigBlind, podeAumentar) ->
                paraPagar == 0L ? PokerBotStrategy.Decision.passar() : PokerBotStrategy.Decision.pagar();
    }

    private static PokerBotStrategy botSempreDesistir() {
        return (mao, board, estagio, pote, paraPagar, saldo, bigBlind, podeAumentar) ->
                PokerBotStrategy.Decision.desistir();
    }

    private static PokerBotStrategy botSempreAllInOuCall() {
        return (mao, board, estagio, pote, paraPagar, saldo, bigBlind, podeAumentar) -> {
            long aumento = saldo - paraPagar;
            if (podeAumentar && aumento > 0L) {
                return PokerBotStrategy.Decision.aumentar(aumento);
            }
            return paraPagar == 0L ? PokerBotStrategy.Decision.passar() : PokerBotStrategy.Decision.pagar();
        };
    }

    private static void testarSeisLugaresOrdemEOcultacaoBots() {
        System.out.println("[1/6] Testando 6 assentos, blinds, ordem UTG e ocultação dos 5 Bots...");
        Deck baralho = criarBaralhoDeterministico();
        long[] stacks = {1000L, 1000L, 1000L, 1000L, 1000L, 1000L};
        TexasHoldemTableGame mesa = new TexasHoldemTableGame(
                stacks, 10L, 20L, baralho, botSempreCallOuCheck(), 0);
        afirme(mesa.iniciarNovaMao(), "Mão de 6 lugares deve iniciar corretamente.");

        afirme(mesa.getSeats().size() == 6, "Mesa deve ter exatamente 6 assentos.");
        afirme(mesa.getSeatDealer() == 0, "Dealer inicial deve ser Seat 0.");
        afirme(mesa.getSeatSmallBlind() == 1, "Small Blind deve ser Seat 1.");
        afirme(mesa.getSeatBigBlind() == 2, "Big Blind deve ser Seat 2.");
        afirme(mesa.getSeatTurnoAtual() == 3, "UTG (Seat 3) deve agir primeiro no Pré-Flop.");
        afirme(mesa.getPoteTotal() == 30L, "Pote inicial deve ser SB + BB = 30.");
        afirme(mesa.getSomaTotalFichasNaMesa() == 6000L, "Conservação de fichas inicial deve somar 6000.");

        // Enquanto é turno dos Bots, ações do jogador humano ficam vazias
        afirme(mesa.isAguardandoAcaoBot(), "Mesa deve aguardar ação dos Bots antes do humano.");
        afirme(mesa.getAcoesValidasJogador().isEmpty(), "Ações do jogador devem estar desabilitadas fora da sua vez.");

        // Ocultação rigorosa de todos os 5 Bots
        for (int i = 1; i < 6; i++) {
            PokerSeatState bot = mesa.getSeats().get(i);
            afirme(bot.getQuantidadeCartas() == 2, "Bot " + i + " deve ter 2 cartas.");
            afirme(bot.hasCartasOcultas(mesa.getEstagioAtual()), "Bot " + i + " deve manter cartas ocultas no Pré-Flop.");
            afirme(bot.getCartasVisiveis(mesa.getEstagioAtual()).isEmpty(),
                    "getCartasVisiveis do Bot " + i + " deve ser vazia fora do Showdown.");
        }

        // Executa UTG (3), Hijack (4), Cutoff (5) um a um -> turno chega ao Dealer (Seat 0 = Você)
        afirme(mesa.executarPassoBot(), "Bot 3 (UTG) deve executar ação.");
        afirme(mesa.getSeatTurnoAtual() == 4, "Turno deve avançar para Seat 4.");
        afirme(mesa.executarPassoBot(), "Bot 4 deve executar ação.");
        afirme(mesa.getSeatTurnoAtual() == 5, "Turno deve avançar para Seat 5.");
        afirme(mesa.executarPassoBot(), "Bot 5 deve executar ação.");
        afirme(mesa.getSeatTurnoAtual() == 0, "Turno deve chegar ao jogador humano (Seat 0).");
        afirme(!mesa.isAguardandoAcaoBot(), "Não deve aguardar Bot na vez do humano.");

        Set<PokerAction> acoes = mesa.getAcoesValidasJogador();
        afirme(acoes.contains(PokerAction.DESISTIR) && acoes.contains(PokerAction.PAGAR)
                && acoes.contains(PokerAction.AUMENTAR), "Jogador deve poder Desistir, Pagar ou Aumentar.");

        afirme(mesa.pagar(), "Jogador paga 20 no Pré-Flop.");
        // SB (1) e BB (2) fecham o Pré-Flop -> Flop abre e primeiro pós-Dealer (Seat 1) age primeiro!
        afirme(mesa.executarPassoBot(), "SB (Seat 1) completa 20.");
        afirme(mesa.executarPassoBot(), "BB (Seat 2) dá Check.");
        afirme(mesa.getEstagioAtual() == PokerStage.FLOP, "Mesa deve avançar para o Flop.");
        afirme(mesa.getCartasComunitarias().size() == 3, "Flop deve exibir 3 cartas comunitárias.");
        afirme(mesa.getSeatTurnoAtual() == 1, "Pós-Flop deve começar à esquerda do Dealer (Seat 1).");
        afirme(mesa.getSomaTotalFichasNaMesa() == 6000L, "Conservação de fichas mantida no Flop.");
        System.out.println("  -> Sucesso: 6 assentos, blinds, UTG, pós-Dealer e sigilo dos 5 Bots validados.");
    }

    private static void testarDesistenciasMultiplasSemVazamento() {
        System.out.println("[2/6] Testando desistências múltiplas sem vazamento de cartas...");
        Deck baralho = criarBaralhoDeterministico();
        long[] stacks = {1000L, 1000L, 1000L, 1000L, 1000L, 1000L};
        TexasHoldemTableGame mesa = new TexasHoldemTableGame(
                stacks, 10L, 20L, baralho, botSempreDesistir(), 0);
        afirme(mesa.iniciarNovaMao(), "Mão deve iniciar.");

        // Bots 3, 4, 5 desistem
        afirme(mesa.executarPassoBot(), "Bot 3 desiste.");
        afirme(mesa.executarPassoBot(), "Bot 4 desiste.");
        afirme(mesa.executarPassoBot(), "Bot 5 desiste.");
        afirme(mesa.getSeatTurnoAtual() == 0, "Vez do jogador.");

        // Jogador paga 20; Bots 1 (SB) e 2 (BB) desistem -> Jogador vence sem Showdown
        afirme(mesa.aumentar(20L), "Jogador aumenta para 40.");
        afirme(mesa.executarPassoBot(), "Bot 1 desiste.");
        afirme(mesa.executarPassoBot(), "Bot 2 desiste.");

        afirme(mesa.getEstagioAtual() == PokerStage.ENCERRADA_POR_DESISTENCIA,
                "Mão deve encerrar por desistência quando 5 participantes correm.");
        afirme(mesa.getSaldoJogador() == 1030L, "Jogador deve receber os blinds (1000 + 30 = 1030).");
        afirme(mesa.getSomaTotalFichasNaMesa() == 6000L, "Conservação exata de 6000 fichas.");
        for (int i = 1; i < 6; i++) {
            afirme(mesa.getSeats().get(i).getCartasVisiveis(mesa.getEstagioAtual()).isEmpty(),
                    "Cartas do Bot " + i + " jamais devem vazar após desistência.");
        }
        System.out.println("  -> Sucesso: 5 desistências encerram mão mantendo sigilo e conservação.");
    }

    private static void testarAllInStacksDiferentesESidePots() {
        System.out.println("[3/6] Testando all-in com stacks diferentes, Main Pot e Side Pots...");
        // 4 participantes com fichas: Seat 0 (1000), Seat 1 (200), Seat 2 (500), Seat 3 (1000); Seats 4 e 5 eliminados (0)
        // Ordem de distribuição: 2 cartas para cada não-eliminado começando em SB (Seat 1):
        // Volta 1: Seat 1 (As), Seat 2 (Ks), Seat 3 (Js), Seat 0 (Qs)
        // Volta 2: Seat 1 (Ac), Seat 2 (Kc), Seat 3 (Jc), Seat 0 (Qc)
        // Board: 2d, 5h, 7c, 9d, 4s (sem straight/flush -> AA > KK > QQ > JJ)
        Deck baralho = criarBaralhoDeterministico(
                c(Rank.AS, Suit.ESPADAS), c(Rank.REI, Suit.ESPADAS), c(Rank.VALETE, Suit.ESPADAS), c(Rank.DAMA, Suit.ESPADAS),
                c(Rank.AS, Suit.PAUS), c(Rank.REI, Suit.PAUS), c(Rank.VALETE, Suit.PAUS), c(Rank.DAMA, Suit.PAUS),
                c(Rank.DOIS, Suit.OUROS), c(Rank.CINCO, Suit.COPAS), c(Rank.SETE, Suit.PAUS),
                c(Rank.NOVE, Suit.OUROS), c(Rank.QUATRO, Suit.ESPADAS)
        );
        long[] stacks = {1000L, 200L, 500L, 1000L, 0L, 0L};
        long somaInicial = 2700L;
        TexasHoldemTableGame mesa = new TexasHoldemTableGame(
                stacks, 10L, 20L, baralho, botSempreAllInOuCall(), 0);
        afirme(mesa.iniciarNovaMao(), "Mão multi-all-in deve iniciar.");

        // Seat 3 (UTG) vai all-in de 1000
        afirme(mesa.getSeatTurnoAtual() == 3, "Seat 3 (UTG) age primeiro.");
        afirme(mesa.executarPassoBot(), "Seat 3 vai all-in de 1000.");

        // Seat 0 (Você) paga all-in de 1000
        afirme(mesa.getSeatTurnoAtual() == 0, "Seat 0 (Você) responde ao all-in.");
        afirme(mesa.pagar(), "Você paga 1000 (all-in).");

        // Seat 1 (200) paga all-in; Seat 2 (500) paga all-in -> Showdown imediato com 3 potes!
        afirme(mesa.executarPassoBot(), "Seat 1 vai all-in com 200.");
        afirme(mesa.executarPassoBot(), "Seat 2 vai all-in com 500.");

        afirme(mesa.getEstagioAtual() == PokerStage.SHOWDOWN,
                "Todos all-in devem ir direto ao Showdown com 5 cartas comunitárias.");
        afirme(mesa.getCartasComunitarias().size() == 5, "Showdown deve exibir 5 cartas no board.");

        List<PokerSidePot> potes = mesa.getPotes();
        afirme(potes.size() == 3, "Devem existir exatamente 3 potes (Main Pot + 2 Side Pots), obtido: " + potes.size());
        afirme(potes.get(0).getValor() == 800L, "Main Pot deve valer 4x200 = 800.");
        afirme(potes.get(1).getValor() == 900L, "Side Pot 1 deve valer 3x300 = 900.");
        afirme(potes.get(2).getValor() == 1000L, "Side Pot 2 deve valer 2x500 = 1000.");

        // Seat 1 (AA) leva Main Pot (800); Seat 2 (KK) leva Side Pot 1 (900); Seat 0 (QQ) leva Side Pot 2 (1000); Seat 3 (JJ) fica com 0
        afirme(mesa.getSeats().get(1).getSaldo() == 800L, "Seat 1 (AA) deve vencer Main Pot de 800.");
        afirme(mesa.getSeats().get(2).getSaldo() == 900L, "Seat 2 (KK) deve vencer Side Pot 1 de 900.");
        afirme(mesa.getSeats().get(0).getSaldo() == 1000L, "Seat 0 (QQ) deve vencer Side Pot 2 de 1000.");
        afirme(mesa.getSeats().get(3).getSaldo() == 0L, "Seat 3 (JJ) perde todos os potes e fica com 0.");
        afirme(mesa.getSomaTotalFichasNaMesa() == somaInicial,
                "Conservação exata de fichas após Main Pot + 2 Side Pots (2700).");

        // No Showdown, cartas dos Bots ativos são reveladas pelo motor
        afirme(!mesa.getSeats().get(1).hasCartasOcultas(PokerStage.SHOWDOWN)
                && mesa.getSeats().get(1).getCartasVisiveis(PokerStage.SHOWDOWN).size() == 2,
                "Cartas de Bot ativo no Showdown devem ser reveladas.");
        System.out.println("  -> Sucesso: Main Pot (800), Side Pot 1 (900), Side Pot 2 (1000) e conservação validados.");
    }

    private static void testarEmpateDivisaoPoteEFichaImpar() {
        System.out.println("[4/6] Testando empate no Showdown com divisão de pote e ficha ímpar...");
        // Blinds pares de 16/30; SB tem apenas 15 e entra all-in ao postar.
        // Seat 0 e Seat 2 fazem sequencia ate 6; SB perde com 7/8.
        // Main pot 45: 23/22; side pot 30: 15/15. Retornos 38/37.
        Deck baralho = criarBaralhoDeterministico(
                c(Rank.SETE, Suit.PAUS), c(Rank.SEIS, Suit.PAUS), c(Rank.SEIS, Suit.OUROS),
                c(Rank.OITO, Suit.OUROS), c(Rank.REI, Suit.COPAS), c(Rank.DAMA, Suit.COPAS),
                c(Rank.DOIS, Suit.ESPADAS), c(Rank.TRES, Suit.OUROS), c(Rank.QUATRO, Suit.PAUS),
                c(Rank.CINCO, Suit.COPAS), c(Rank.NOVE, Suit.ESPADAS)
        );
        long[] stacks = {1000L, 15L, 1000L, 0L, 0L, 0L};
        TexasHoldemTableGame mesa = new TexasHoldemTableGame(
                stacks, 16L, 30L, baralho, botSempreCallOuCheck(), 0);
        afirme(mesa.iniciarNovaMao(), "Mao de empate deve iniciar.");
        afirme(mesa.getSeatTurnoAtual() == 0, "Dealer age primeiro no Pre-Flop com 3 participantes.");
        afirme(mesa.pagar(), "Seat 0 paga 30.");
        afirme(mesa.executarPassoBot(), "Seat 2 da check; SB ja esta all-in.");

        // Flop, Turn, River: ambos dão check até Showdown
        for (int rua = 0; rua < 3; rua++) {
            afirme(mesa.executarPassoBot(), "Seat 2 dá check.");
            afirme(mesa.passar(), "Seat 0 dá check.");
        }

        afirme(mesa.getEstagioAtual() == PokerStage.SHOWDOWN, "Deve chegar ao Showdown.");
        afirme(mesa.getPoteTotal() == 75L, "Pote total deve ser 75 (ímpar).");
        // SB perde as 15 fichas; os dois vencedores recebem 37 e 38.
        afirme(mesa.getSeats().get(1).getSaldo() == 0L, "Seat 1 perdeu seu all-in de 15 fichas.");
        afirme(mesa.getSeats().get(0).getSaldo() == 1007L, "Seat 0 deve receber 37 (saldo 1007).");
        afirme(mesa.getSeats().get(2).getSaldo() == 1008L, "Seat 2 (mais próximo após Dealer) deve receber ficha ímpar (saldo 1008).");
        afirme(mesa.getSomaTotalFichasNaMesa() == 2015L, "Conservacao exata de 2015 fichas na divisao de pote.");
        System.out.println("  -> Sucesso: Empate com pote ímpar (75 -> 38/37) e conservação validados.");
    }

    private static void testarTransicaoAutomaticaHeadsUp() {
        System.out.println("[5/6] Testando transição automática para regra de Heads-Up com 2 sobreviventes...");
        Deck baralho = criarBaralhoDeterministico();
        long[] stacks = {1000L, 0L, 0L, 1000L, 0L, 0L};
        TexasHoldemTableGame mesa = new TexasHoldemTableGame(
                stacks, 10L, 20L, baralho, botSempreCallOuCheck(), 0);
        afirme(mesa.iniciarNovaMao(), "Mão em Heads-Up automático deve iniciar.");

        // Regra oficial Heads-Up: Dealer É o Small Blind e age PRIMEIRO no Pré-Flop!
        afirme(mesa.getSeatDealer() == 0, "Dealer deve ser Seat 0.");
        afirme(mesa.getSeatSmallBlind() == 0, "Em Heads-Up, Dealer (Seat 0) deve ser o Small Blind.");
        afirme(mesa.getSeatBigBlind() == 3, "Em Heads-Up, o outro sobrevivente (Seat 3) deve ser o Big Blind.");
        afirme(mesa.getSeatTurnoAtual() == 0, "Em Heads-Up, Dealer/SB (Seat 0) age primeiro no Pré-Flop.");
        afirme(mesa.getTurnoAtual() == PokerParticipant.JOGADOR, "Turno deve ser do jogador.");

        // Jogador paga 10 para completar 20, BB dá check -> Pós-Flop BB (Seat 3) age primeiro!
        afirme(mesa.pagar(), "Dealer/SB completa o Big Blind.");
        afirme(mesa.executarPassoBot(), "BB dá check -> abre Flop.");
        afirme(mesa.getEstagioAtual() == PokerStage.FLOP, "Deve abrir o Flop.");
        afirme(mesa.getSeatTurnoAtual() == 3, "Pós-Flop em Heads-Up: BB (Seat 3) age primeiro.");
        afirme(mesa.getSomaTotalFichasNaMesa() == 2000L, "Conservação de 2000 fichas em Heads-Up.");
        System.out.println("  -> Sucesso: Regra de Heads-Up (Dealer=SB, age primeiro Pré-Flop, BB primeiro Pós-Flop) validada.");
    }

    private static void testarPausaBotsRetomadaBERenderizacao() {
        System.out.println("[6/6] Testando pausa visível (~0,85s), pausa B / retomada E e renderização 6-max...");
        ScreenSaver painel = new ScreenSaver();
        painel.pararLoop(); // O teste avanca o tempo manualmente.
        try {
            afirme(painel.getPokerEngine() instanceof TexasHoldemTableGame,
                    "ScreenSaver deve iniciar com TexasHoldemTableGame (6 lugares).");
            TexasHoldemTableGame mesa = (TexasHoldemTableGame) painel.getPokerEngine();
            PokerTableView view = painel.getPokerTableView();

            painel.setEstadoAtual(ScreenSaver.GameState.MENU_POKER);
            int turnoInicial = mesa.getSeatTurnoAtual();
            afirme(turnoInicial == 3 && mesa.isAguardandoAcaoBot(), "UTG (Bot 3) deve aguardar pausa inicial.");

            // Antes de completar 0,85s, o Bot NÃO pode ter agido ainda
            painel.avancarTempoPoker(0.40);
            afirme(mesa.getSeatTurnoAtual() == turnoInicial,
                    "Bot não deve agir antes de concluir a pausa de ~0,85s.");
            double restanteAntesSair = view.getCronometroBotSegundos();
            afirme(restanteAntesSair > 0.30 && restanteAntesSair < 0.60,
                    "Cronômetro da pausa do Bot deve estar em contagem decrescente.");

            // Sai com B para o salão -> cronômetro congela completamente
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Tecla B retorna ao salão.");
            painel.avancarTempoPoker(5.0);
            afirme(mesa.getSeatTurnoAtual() == turnoInicial, "Enquanto fora da mesa, Bot permanece pausado.");
            afirme(Math.abs(view.getCronometroBotSegundos() - restanteAntesSair) < 1e-6,
                    "Cronômetro do Poker deve congelar exatamente ao sair com B.");

            // Volta com E e completa o tempo restante -> Bot executa exatamente 1 jogada
            painel.setEstadoAtual(ScreenSaver.GameState.MENU_POKER);
            painel.avancarTempoPoker(0.50);
            afirme(mesa.getSeatTurnoAtual() != turnoInicial,
                    "Ao retomar com E e completar a pausa, o Bot executa sua jogada.");

            // Renderização off-screen em 1280x720 e 1920x1080 sem exceções
            int[][] resolucoes = {{1280, 720}, {1920, 1080}};
            for (int[] res : resolucoes) {
                BufferedImage img = new BufferedImage(res[0], res[1], BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();
                try {
                    view.render(g2d, res[0], res[1]);
                } finally {
                    g2d.dispose();
                }
            }
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Pausa de ~0,85s, congelamento em B, retomada em E e renderização 6-max validados.");
    }
}
