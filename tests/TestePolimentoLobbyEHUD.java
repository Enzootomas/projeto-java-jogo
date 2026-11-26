package tests;

import model.BlackjackGame;
import model.Card;
import model.CasinoChair;
import model.CasinoCounter;
import model.CasinoTable;
import model.Deck;
import model.MyCharacter;
import model.MyRectangle;
import model.MyShape;
import model.PokerBotStrategy;
import model.PokerParticipant;
import model.Rank;
import model.Suit;
import model.TexasHoldemGame;
import model.TexasHoldemTableGame;
import model.TrucoPlayer;
import model.TrucoRound;
import view.ScreenSaver;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Suíte de testes automatizados para validar o polimento do lobby isométrico,
 * orientação das cadeiras, bar decorativo sólido, HUD de apostas/ganhos possíveis
 * (Blackjack, Poker e Truco) e ausência de sobreposição em 1280x720 e 1920x1080.
 */
public class TestePolimentoLobbyEHUD {

    private static int totalVerificacoes = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DE POLIMENTO DO LOBBY, BAR E HUD DE APOSTAS");
        System.out.println("====================================================");

        testarCadeirasOrientadasESemSobreposicao();
        testarBarSolidoEPercursoCompletoComZSorting();
        testarHUDMultiplasMesasEGanhosPossiveis();
        testarHUDSemRodadaAtivaEEvolucaoPokerUncalledBet();
        testarLayoutHUDVSControlesEm720pE1080p();

        System.out.println("----------------------------------------------------");
        System.out.println("Total de verificacoes (Lobby e HUD): " + totalVerificacoes);
        System.out.println("RESULTADO: TODOS OS TESTES DE POLIMENTO DO LOBBY E HUD PASSARAM! [OK]");
        System.out.println("----------------------------------------------------");
    }

    private static void afirme(boolean condicao, String descricao) {
        totalVerificacoes++;
        if (!condicao) {
            throw new AssertionError("FALHA: " + descricao);
        }
    }

    private static Card c(Rank rank, Suit suit) {
        return new Card(suit, rank);
    }

    private static PokerBotStrategy botPassivo() {
        return (maoBot, com, estagio, pote, valorPagar, saldoBot, bb, podeAumentar) ->
                valorPagar > 0L ? PokerBotStrategy.Decision.pagar() : PokerBotStrategy.Decision.passar();
    }

    private static void testarCadeirasOrientadasESemSobreposicao() {
        System.out.println("[1/5] Testando cadeiras orientadas, associadas às mesas/bar e sem sobreposição...");
        ScreenSaver painel = new ScreenSaver();
        try {
            List<CasinoChair> cadeiras = painel.getCadeiras();
            afirme(cadeiras.size() == 8, "Salão deve conter 8 cadeiras associadas ao bar e às 3 mesas.");

            // Verifica que não há cadeiras soltas na antiga área de circulação central (310, 140 / 310, 180)
            MyRectangle antigaCirculacao1 = new MyRectangle(305, 135, 25, 25);
            MyRectangle antigaCirculacao2 = new MyRectangle(305, 175, 25, 25);
            for (CasinoChair ch : cadeiras) {
                afirme(!ch.intersects(antigaCirculacao1) && !ch.intersects(antigaCirculacao2),
                        "Nenhuma cadeira deve ficar solta na circulação central.");
                afirme(ch.getOrientation() != null, "Toda cadeira deve possuir orientação explícita.");
                afirme(!ch.intersects(painel.getBalcao()), "Cadeira não deve sobrepor o footprint do bar.");
                afirme(!ch.intersects(painel.getMesaBlackjack()), "Cadeira não deve sobrepor o Blackjack.");
                afirme(!ch.intersects(painel.getMesaPoker()), "Cadeira não deve sobrepor o Poker.");
                afirme(!ch.intersects(painel.getMesaTruco()), "Cadeira não deve sobrepor o Truco.");
            }

            // Verifica que cadeiras não colidem entre si
            for (int i = 0; i < cadeiras.size(); i++) {
                for (int j = i + 1; j < cadeiras.size(); j++) {
                    afirme(!cadeiras.get(i).intersects(cadeiras.get(j)),
                            "Cadeiras " + i + " e " + j + " não devem se sobrepor.");
                }
            }

            // Verifica que as 2 cadeiras do bar estão voltadas para o NORTE (para o balcão)
            afirme(cadeiras.get(0).getOrientation() == CasinoChair.Orientation.NORTE
                            && cadeiras.get(1).getOrientation() == CasinoChair.Orientation.NORTE,
                    "As duas cadeiras do bar devem estar voltadas para o balcão (NORTE).");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: 8 cadeiras orientadas e livres de sobreposições.");
    }

    private static void testarBarSolidoEPercursoCompletoComZSorting() {
        System.out.println("[2/5] Testando bar sólido, percurso completo entre as 3 mesas e Z-Sorting...");
        ScreenSaver painel = new ScreenSaver();
        try {
            CasinoCounter bar = painel.getBalcao();
            MyCharacter player = painel.getPlayer();

            // Bar continua sendo obstáculo físico sólido
            MyRectangle caixaDentroBar = new MyRectangle(bar.getX() + 10, bar.getY() + 5, 24, 24);
            afirme(caixaDentroBar.intersects(bar), "Bar decorativo continua sendo obstáculo sólido no solo.");

            // Z-Sorting: ao norte do bar tem depthZ menor (desenhado atrás); ao sul tem depthZ maior (à frente)
            player.setExactPosition(bar.getX() + 20, bar.getY() - 26);
            double zNorte = player.getDepthZ();
            double zBar = bar.getDepthZ();
            player.setExactPosition(bar.getX() + 20, bar.getY() + bar.getHeight() + 30);
            double zSul = player.getDepthZ();
            afirme(zNorte < zBar && zBar < zSul,
                    "Z-Sorting ordena corretamente avatar atrás (Norte) e à frente (Sul) do bar.");

            // Percurso completo: Entrada -> Blackjack -> Bar -> Poker -> Truco
            int[][] pontosPercurso = {
                    {60, 320},  // Entrada
                    {160, 220}, // Frente do Blackjack
                    {110, 80},  // Frente do Bar
                    {250, 110}, // Corredor central norte
                    {340, 110}, // Frente do Poker
                    {250, 250}, // Corredor central sul
                    {320, 240}  // Frente do Truco
            };
            for (int[] pt : pontosPercurso) {
                MyRectangle caixa = new MyRectangle(pt[0], pt[1], player.getWidth(), player.getHeight());
                for (MyShape obst : painel.getObstaculos()) {
                    afirme(!caixa.intersects(obst),
                            "Ponto do percurso (" + pt[0] + ", " + pt[1] + ") deve estar livre de colisão.");
                }
            }
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Bar sólido, Z-Sorting e percurso completo validados.");
    }

    private static void testarHUDMultiplasMesasEGanhosPossiveis() {
        System.out.println("[3/5] Testando HUD com rodadas ativas simultâneas em Blackjack, Poker e Truco...");
        ScreenSaver painel = new ScreenSaver();
        try {
            // 1. Configura Blackjack determinístico sem Blackjack Natural (aposta 100 -> lucro 100 / retorno 200; BJ 150 / 250)
            Deck deckBJ = new Deck(List.of(
                    c(Rank.DEZ, Suit.COPAS), c(Rank.OITO, Suit.ESPADAS),
                    c(Rank.SETE, Suit.OUROS), c(Rank.NOVE, Suit.PAUS),
                    c(Rank.CINCO, Suit.COPAS)
            ));
            BlackjackGame bj = new BlackjackGame(1000L, deckBJ, false);
            painel.setBlackjackEngine(bj);
            afirme(bj.iniciarPartida(100L), "Blackjack iniciado com aposta de 100.");
            afirme(bj.getLucroPossivelVitoriaComum() == 100L, "Lucro possível vitória comum BJ = 100.");
            afirme(bj.getRetornoTotalPossivelVitoriaComum() == 200L, "Retorno total possível vitória comum BJ = 200.");
            afirme(bj.getLucroPossivelBlackjackNatural() == 150L, "Lucro possível BJ Natural = 150.");
            afirme(bj.getRetornoTotalPossivelBlackjackNatural() == 250L, "Retorno total possível BJ Natural = 250.");

            // 2. Configura Poker determinístico (Blinds 50/100: comprometido 50, pote 150, lucro +100, retorno 150, empate 75)
            Deck deckPoker = new Deck(List.of(
                    c(Rank.AS, Suit.ESPADAS), c(Rank.DOIS, Suit.PAUS),
                    c(Rank.REI, Suit.ESPADAS), c(Rank.SETE, Suit.OUROS),
                    c(Rank.AS, Suit.COPAS), c(Rank.REI, Suit.COPAS), c(Rank.QUATRO, Suit.PAUS),
                    c(Rank.NOVE, Suit.OUROS), c(Rank.TRES, Suit.ESPADAS)
            ));
            TexasHoldemGame poker = new TexasHoldemGame(
                    1000L, 1000L, 50L, 100L, deckPoker, botPassivo(), PokerParticipant.JOGADOR);
            poker.iniciarNovaMao();
            painel.setPokerEngine(poker);
            afirme(poker.getTotalComprometidoJogadorNaMao() == 50L, "Poker Pré-Flop: comprometido = 50.");
            afirme(poker.getPoteTotal() == 150L, "Poker Pré-Flop: pote = 150.");
            afirme(poker.getLucroLiquidoPotencialVitoriaAtual() == 100L, "Poker Pré-Flop: lucro potencial = 100.");
            afirme(poker.getRetornoTotalPotencialVitoriaAtual() == 150L, "Poker Pré-Flop: retorno potencial = 150.");
            afirme(poker.getRetornoPotencialEmpateAtual() == 75L, "Poker Pré-Flop: empate = 75.");

            // 3. Configura Truco determinístico (1 ponto em disputa, sem fichas)
            TrucoRound truco = new TrucoRound(
                    Arrays.asList(c(Rank.TRES, Suit.PAUS), c(Rank.DOIS, Suit.COPAS), c(Rank.AS, Suit.OUROS)),
                    Arrays.asList(c(Rank.QUATRO, Suit.ESPADAS), c(Rank.CINCO, Suit.COPAS), c(Rank.SEIS, Suit.OUROS)),
                    c(Rank.VALETE, Suit.OUROS),
                    TrucoPlayer.JOGADOR
            );
            painel.setTrucoRound(truco);

            List<String> linhas = painel.getLinhasResumoApostasHUD();
            afirme(linhas.size() == 3, "HUD deve listar as 3 mesas ativas simultaneamente.");
            afirme(linhas.get(0).contains("Blackjack") && linhas.get(0).contains("100") && linhas.get(0).contains("200")
                            && linhas.get(0).contains("150") && linhas.get(0).contains("250"),
                    "Linha do Blackjack diferencia lucro e retorno total para vitória comum e BJ Natural.");
            afirme(linhas.get(1).contains("Poker") && linhas.get(1).contains("50") && linhas.get(1).contains("150")
                            && linhas.get(1).contains("75"),
                    "Linha do Poker exibe comprometido, pote, vitória atual e empate.");
            afirme(linhas.get(2).contains("Truco") && linhas.get(2).contains("1 ponto") && linhas.get(2).contains("Sem aposta em fichas"),
                    "Linha do Truco indica claramente pontuação (1 ponto) sem aposta em fichas.");

            // Entra no Poker (E), paga 50 -> pote 200, comprometido 100, e volta ao salão (B)
            painel.setEstadoAtual(ScreenSaver.GameState.MENU_POKER);
            painel.getPokerTableView().executarCheckCall();
            painel.processarRetornoB();
            afirme(poker.getTotalComprometidoJogadorNaMao() == 100L && poker.getPoteTotal() == 200L,
                    "Após Call no Poker e retorno com B, comprometido (100) e pote (200) atualizam no HUD.");

            // Entra no Truco (E), joga 1 carta vencendo a vaza 1 e volta ao salão (B)
            painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);
            painel.processarComandoTruco("1");
            painel.processarRetornoB();
            List<String> linhasApos = painel.getLinhasResumoApostasHUD();
            afirme(linhasApos.get(2).contains("1 × 0"),
                    "Placar de vazas do Truco no HUD atualiza após retorno com B (1 × 0).");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: HUD multi-mesas e atualizações E/B validados.");
    }

    private static void testarHUDSemRodadaAtivaEEvolucaoPokerUncalledBet() {
        System.out.println("[4/5] Testando estado discreto sem rodadas ativas e devolução de aposta não coberta...");
        ScreenSaver painel = new ScreenSaver();
        try {
            // Finaliza Poker por Fold (aguardando Bots UTG na mesa de 6 lugares) e Truco por 2x0
            if (painel.getPokerEngine() instanceof TexasHoldemTableGame) {
                TexasHoldemTableGame mesaPoker = (TexasHoldemTableGame) painel.getPokerEngine();
                int limite = 10;
                while (mesaPoker.isAguardandoAcaoBot() && limite-- > 0) {
                    mesaPoker.executarPassoBot();
                }
            }
            painel.getPokerEngine().desistir();
            TrucoRound truco = new TrucoRound(
                    Arrays.asList(c(Rank.TRES, Suit.PAUS), c(Rank.TRES, Suit.COPAS), c(Rank.TRES, Suit.OUROS)),
                    Arrays.asList(c(Rank.QUATRO, Suit.ESPADAS), c(Rank.QUATRO, Suit.COPAS), c(Rank.QUATRO, Suit.OUROS)),
                    c(Rank.VALETE, Suit.OUROS),
                    TrucoPlayer.JOGADOR
            );
            painel.setTrucoRound(truco);
            painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);
            painel.processarComandoTruco("1");
            painel.processarComandoTruco("1");
            painel.processarRetornoB();

            afirme(painel.getLinhasResumoApostasHUD().isEmpty(),
                    "Quando nenhuma mesa tem rodada ativa, lista de resumo do HUD fica vazia (estado discreto).");

            // Testa comprometido exato no Poker após devolução de aposta não coberta (Bot com 160 fichas)
            Deck deckShort = new Deck(List.of(
                    c(Rank.AS, Suit.ESPADAS), c(Rank.REI, Suit.PAUS),
                    c(Rank.AS, Suit.OUROS), c(Rank.DAMA, Suit.PAUS),
                    c(Rank.DOIS, Suit.COPAS), c(Rank.SETE, Suit.ESPADAS), c(Rank.NOVE, Suit.OUROS),
                    c(Rank.QUATRO, Suit.PAUS), c(Rank.SEIS, Suit.COPAS)
            ));
            TexasHoldemGame pokerShort = new TexasHoldemGame(
                    1000L, 160L, 50L, 100L, deckShort, botPassivo(), PokerParticipant.JOGADOR);
            pokerShort.iniciarNovaMao();
            pokerShort.aumentar(500L); // Excesso de 440 devolvido -> comprometido efetivo = 160
            afirme(pokerShort.getTotalComprometidoJogadorNaMao() == 160L,
                    "Total comprometido no Poker desconta corretamente o excesso não coberto devolvido (160).");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Estado ocioso discreto e uncalled bet validados.");
    }

    private static void testarLayoutHUDVSControlesEm720pE1080p() {
        System.out.println("[5/5] Testando layout, limites e ausência de sobreposição em 1280x720 e 1920x1080...");
        ScreenSaver painel = new ScreenSaver();
        try {
            // Garante todas as 3 mesas ativas + prompt de interação visível (pior caso de altura do HUD)
            Deck deckBJ = new Deck(List.of(
                    c(Rank.DEZ, Suit.COPAS), c(Rank.OITO, Suit.ESPADAS),
                    c(Rank.SETE, Suit.OUROS), c(Rank.NOVE, Suit.PAUS)
            ));
            BlackjackGame bj = new BlackjackGame(1000L, deckBJ, false);
            bj.iniciarPartida(100L);
            painel.setBlackjackEngine(bj);

            painel.getPlayer().setExactPosition(340, 110);
            painel.atualizarMesaMaisProxima();
            afirme(painel.getMesaAtiva() == painel.getMesaPoker(), "Prompt da mesa de Poker ativo para teste de sobreposição.");

            int[][] resolucoes = {{1280, 720}, {1920, 1080}};
            for (int[] res : resolucoes) {
                int w = res[0];
                int h = res[1];
                painel.setSize(new Dimension(w, h));
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                painel.paint(g);
                g.dispose();

                Rectangle hud = painel.getHudBounds();
                Rectangle prompt = painel.getPromptBounds();
                afirme(hud.width > 0 && hud.height > 0, "HUD renderizado com dimensões válidas em " + w + "x" + h);
                afirme(prompt.width > 0 && prompt.height > 0, "Prompt renderizado com dimensões válidas em " + w + "x" + h);
                afirme(hud.x >= 0 && hud.y >= 0 && hud.x + hud.width <= w && hud.y + hud.height <= h,
                        "HUD respeita as bordas da tela em " + w + "x" + h);
                afirme(!hud.intersects(prompt),
                        "HUD e prompt de interação NÃO se sobrepõem em " + w + "x" + h);
            }
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Layout sem sobreposições em 1280x720 e 1920x1080.");
    }
}
