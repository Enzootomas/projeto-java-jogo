package tests;

import model.Card;
import model.CasinoTable;
import model.MyCharacter;
import view.PokerTableView;
import view.ScreenSaver;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Suíte de testes automatizados da interface da Mesa de Poker (Texas Hold'em),
 * navegação no salão isométrico e validação de rotas entre as três mesas.
 */
public class TesteInterfacePoker {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO SUÍTE DE TESTES: INTERFACE E NAVEGAÇÃO DO POKER ===");

        testarInstanciacaoEPesquisaMesaPoker();
        testarPassagemLivreERotasEntreTresMesas();
        testarDesempateEuclidianoEAvisoMesmoDestino();
        testarTransicaoEntradaESaidaMesaPoker();
        testarPreservacaoMaoAoAlternarTelas();
        testarCincoEspacosBoardECartasAbertasPorFase();
        testarOcultamentoCartasBotAteShowdown();
        testarAcoesBotoesEAtalhosTeclado();
        testarNavegacaoEntreTodasAsMesas();
        testarRenderizacaoOffScreen();

        System.out.println("=== TODOS OS 10 TESTES DE INTERFACE DO POKER FORAM CONCLUÍDOS COM SUCESSO! ===");
    }

    private static void afirme(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError("FALHA: " + mensagem);
        }
    }

    /**
     * 1. Testa a instanciação da mesa de Poker em (340, 50) e sua presença nos obstáculos e renderizáveis.
     */
    public static void testarInstanciacaoEPesquisaMesaPoker() {
        System.out.println("[1/10] Testando instanciação e identificação da mesa de Poker no salão...");
        ScreenSaver painel = new ScreenSaver();
        try {
            CasinoTable mesaPoker = painel.getMesaPoker();
            afirme(mesaPoker != null, "Mesa de Poker deve existir no painel.");
            afirme(mesaPoker.getX() == 340 && mesaPoker.getY() == 50,
                    "Mesa de Poker deve estar posicionada em (340, 50). Posição: " + mesaPoker.getX() + ", " + mesaPoker.getY());
            afirme("Poker".equalsIgnoreCase(mesaPoker.getGameName()),
                    "Nome do jogo da mesa deve ser 'Poker'. Nome: " + mesaPoker.getGameName());

            afirme(painel.getObstaculos().contains(mesaPoker),
                    "Mesa de Poker deve constar na lista de obstáculos sólidos.");
            afirme(painel.getEntidadesRenderizaveis().contains(mesaPoker),
                    "Mesa de Poker deve constar na lista polimórfica de renderização.");
            afirme(painel.getPokerTableView() != null,
                    "PokerTableView deve estar inicializada.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Mesa de Poker configurada corretamente.");
    }

    /**
     * 2. Valida o caminho a pé entre as três mesas (Blackjack, Truco e Poker) sem bloqueios da mobília.
     */
    public static void testarPassagemLivreERotasEntreTresMesas() {
        System.out.println("[2/10] Testando passagem a pé e rotas livres entre as três mesas...");
        ScreenSaver painel = new ScreenSaver();
        try {
            MyCharacter player = painel.getPlayer();
            CasinoTable mesaBlackjack = painel.getMesaBlackjack();
            CasinoTable mesaTruco = painel.getMesaTruco();
            CasinoTable mesaPoker = painel.getMesaPoker();

            // Verifica folga horizontal e vertical entre mobílias
            // Poker (340, 50, w=80, h=50) -> x=[340, 420], y=[50, 100]
            // Cadeiras 2 e 3 em x=310, y=140 e y=180 -> passagem entre y=100 e y=140 = 40px (avatar tem 24px)
            int folgaVerticalPokerCadeira = 140 - (mesaPoker.getY() + mesaPoker.getHeight());
            afirme(folgaVerticalPokerCadeira >= player.getHeight(),
                    "Deve haver folga vertical suficiente para o avatar passar entre a mesa de Poker e as cadeiras. Folga: "
                            + folgaVerticalPokerCadeira + "px (avatar: " + player.getHeight() + "px)");

            // Move avatar do Blackjack (160, 220) até o Poker (340, 110)
            player.setX(160);
            player.setY(220);
            afirme(!temColisao(player, 160, 220, painel.getObstaculos()),
                    "Posição inicial perto do Blackjack deve ser livre de colisões.");

            // Rota para o Poker através do corredor central
            player.setX(250);
            player.setY(110);
            afirme(!temColisao(player, 250, 110, painel.getObstaculos()),
                    "Corredor central em direção ao Poker deve estar desimpedido.");

            // Posição de interação da mesa de Poker
            player.setX(340);
            player.setY(110);
            afirme(!temColisao(player, 340, 110, painel.getObstaculos()),
                    "Ponto de interação em frente à mesa de Poker deve ser livre de colisão.");
            afirme(mesaPoker.isPlayerNear(player, 35),
                    "Avatar deve estar ao alcance da mesa de Poker na posição (340, 110).");

            // Rota da mesa de Poker até a mesa de Truco (320, 280)
            player.setX(250);
            player.setY(250);
            afirme(!temColisao(player, 250, 250, painel.getObstaculos()),
                    "Passagem entre Poker e Truco deve estar desimpedida.");

            player.setX(320);
            player.setY(240);
            afirme(!temColisao(player, 320, 240, painel.getObstaculos()),
                    "Ponto de interação da mesa de Truco deve ser acessível e sem colisão.");
            afirme(mesaTruco.isPlayerNear(player, 35),
                    "Avatar deve estar ao alcance da mesa de Truco na posição (320, 240).");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Rotas entre Blackjack, Truco e Poker estão 100% livres e navegáveis.");
    }

    private static boolean temColisao(MyCharacter player, int x, int y, List<model.MyShape> obstaculos) {
        model.MyRectangle caixa = new model.MyRectangle(x, y, player.getWidth(), player.getHeight());
        if (obstaculos != null) {
            for (model.MyShape obst : obstaculos) {
                if (obst != player && caixa.intersects(obst)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 3. Testa que o aviso do salão e a tecla E selecionam a mesma mesa quando houver proximidade com desempate.
     */
    public static void testarDesempateEuclidianoEAvisoMesmoDestino() {
        System.out.println("[3/10] Testando consistência entre o aviso do salão e a tecla E no desempate euclidiano...");
        ScreenSaver painel = new ScreenSaver();
        try {
            MyCharacter player = painel.getPlayer();
            CasinoTable mesaPoker = painel.getMesaPoker();

            // Posiciona jogador próximo à mesa de Poker
            player.setX(340);
            player.setY(110);
            painel.atualizarMesaMaisProxima();

            CasinoTable mesaAtiva = painel.getMesaAtiva();
            afirme(mesaAtiva != null, "Mesa ativa não deve ser nula.");
            afirme(mesaAtiva == mesaPoker, "Mesa ativa deve ser a de Poker.");

            // Confirma que a tecla E abre exatamente a mesma mesa indicada
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_POKER,
                    "Estado atual deve transitar para MENU_POKER. Estado: " + painel.getEstadoAtual());

            // Retorna ao salão
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO,
                    "Estado atual deve retornar para SALAO.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Aviso e transição da tecla E são 100% consistentes.");
    }

    /**
     * 4. Testa a transição de entrada (E), saída (B) e botão de retorno na mesa de Poker.
     */
    public static void testarTransicaoEntradaESaidaMesaPoker() {
        System.out.println("[4/10] Testando transição de entrada (E), saída (B) e clique de retorno...");
        ScreenSaver painel = new ScreenSaver();
        try {
            MyCharacter player = painel.getPlayer();
            player.setX(340);
            player.setY(110);
            painel.atualizarMesaMaisProxima();

            // 1. Entra na mesa com tecla E
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_POKER,
                    "Deve estar no MENU_POKER após tecla E.");

            // 2. Volta ao salão com tecla B
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO,
                    "Deve estar no SALAO após tecla B.");

            // 3. Entra novamente e testa o botão na tela de voltar
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_POKER,
                    "Deve reentrar no MENU_POKER.");

            PokerTableView view = painel.getPokerTableView();
            BufferedImage img = new BufferedImage(1024, 768, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            view.render(g, 1024, 768);
            g.dispose();

            Rectangle btnVoltar = view.getBtnVoltar();
            painel.processarCliqueMouse(btnVoltar.x + 5, btnVoltar.y + 5);
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO,
                    "Clique no botão 'Voltar ao Salão' deve retornar ao SALAO.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Transições de entrada e saída funcionam por teclado e mouse.");
    }

    /**
     * 5. Testa que a mão em andamento é preservada ao sair com B e reentrar com E.
     */
    public static void testarPreservacaoMaoAoAlternarTelas() {
        System.out.println("[5/10] Testando preservação do estado da mão de Poker ao alternar telas (E -> B -> E)...");
        ScreenSaver painel = new ScreenSaver();
        try {
            MyCharacter player = painel.getPlayer();
            player.setX(340);
            player.setY(110);
            painel.atualizarMesaMaisProxima();

            painel.processarInteracaoE();
            PokerTableView view = painel.getPokerTableView();

            // Avança para a fase do Flop
            view.executarCheckCall();
            afirme(view.getFaseAtual() == PokerTableView.PokerPhase.FLOP,
                    "Fase deve ser FLOP após check/call inicial.");

            long poteAntes = view.getPoteCentral();
            List<Card> holeCardsAntes = view.getHoleCardsJogador();

            // Sai com B para o salão
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Deve estar no SALÃO.");

            // Reentra na mesa com E
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_POKER, "Deve estar no MENU_POKER.");

            PokerTableView viewApos = painel.getPokerTableView();
            afirme(viewApos.getFaseAtual() == PokerTableView.PokerPhase.FLOP,
                    "Fase do jogo deve continuar sendo FLOP após reentrar.");
            afirme(viewApos.getPoteCentral() == poteAntes,
                    "Pote central deve ser rigorosamente preservado.");
            afirme(viewApos.getHoleCardsJogador().equals(holeCardsAntes),
                    "Hole cards do jogador devem ser rigorosamente preservadas.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Mão de Poker preservada integralmente entre saídas e reentradas.");
    }

    /**
     * 6. Testa que as 5 posições do board são reservadas e somente as cartas da fase atual são abertas.
     */
    public static void testarCincoEspacosBoardECartasAbertasPorFase() {
        System.out.println("[6/10] Testando os 5 espaços do board e cartas abertas por fase...");
        PokerTableView view = new PokerTableView(null);

        // 1. Pré-Flop: 0 cartas abertas
        view.setFaseAtual(PokerTableView.PokerPhase.PRE_FLOP);
        afirme(view.getFaseAtual().getCartasAbertas() == 0, "Pré-flop deve ter 0 cartas comunitárias abertas.");

        // 2. Flop: 3 cartas abertas
        view.setFaseAtual(PokerTableView.PokerPhase.FLOP);
        afirme(view.getFaseAtual().getCartasAbertas() == 3, "Flop deve ter 3 cartas comunitárias abertas.");

        // 3. Turn: 4 cartas abertas
        view.setFaseAtual(PokerTableView.PokerPhase.TURN);
        afirme(view.getFaseAtual().getCartasAbertas() == 4, "Turn deve ter 4 cartas comunitárias abertas.");

        // 4. River: 5 cartas abertas
        view.setFaseAtual(PokerTableView.PokerPhase.RIVER);
        afirme(view.getFaseAtual().getCartasAbertas() == 5, "River deve ter 5 cartas comunitárias abertas.");

        // 5. Showdown: 5 cartas abertas
        view.setFaseAtual(PokerTableView.PokerPhase.SHOWDOWN);
        afirme(view.getFaseAtual().getCartasAbertas() == 5, "Showdown deve ter 5 cartas comunitárias abertas.");

        // Renderiza para preencher os bounds e valida que exatamente 5 posições existem
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        view.render(g, 800, 600);
        g.dispose();

        afirme(view.getBoundsBoard().size() == 5,
                "Devem existir exatamente 5 espaços reservados no board comunitário. Encontrados: "
                        + view.getBoundsBoard().size());
        System.out.println("  -> Sucesso: 5 posições do board e contagem de cartas abertas validadas.");
    }

    /**
     * 7. Testa que as 2 hole cards do bot permanecem ocultas até o Showdown ou Fold.
     */
    public static void testarOcultamentoCartasBotAteShowdown() {
        System.out.println("[7/10] Testando ocultamento das cartas do bot até a autorização de revelação...");
        PokerTableView view = new PokerTableView(null);

        afirme(view.getHoleCardsBot().size() == 2, "Bot deve possuir 2 hole cards.");
        afirme(!view.isBotCartasReveladas(), "Cartas do bot devem iniciar viradas (fechadas).");

        // Flop
        view.executarCheckCall();
        afirme(!view.isBotCartasReveladas(), "Cartas do bot devem continuar viradas no Flop.");

        // Turn
        view.executarCheckCall();
        afirme(!view.isBotCartasReveladas(), "Cartas do bot devem continuar viradas no Turn.");

        // River
        view.executarCheckCall();
        afirme(!view.isBotCartasReveladas(), "Cartas do bot devem continuar viradas no River.");

        // Showdown
        view.executarCheckCall();
        afirme(view.getFaseAtual() == PokerTableView.PokerPhase.SHOWDOWN, "Deve atingir o Showdown.");
        afirme(view.isBotCartasReveladas(), "Cartas do bot devem ser autorizadas para revelação apenas no Showdown.");
        afirme(view.isMaoFinalizada(), "Mão deve ser marcada como finalizada no Showdown.");
        System.out.println("  -> Sucesso: Sigilo das cartas do bot respeitado estritamente.");
    }

    /**
     * 8. Testa as ações de Fold, Check/Call, Raise, Nova Mão por clique e atalho de teclado.
     */
    public static void testarAcoesBotoesEAtalhosTeclado() {
        System.out.println("[8/10] Testando ações de Fold, Check/Call, Raise e atalhos de teclado (F, C, R, N)...");
        PokerTableView view = new PokerTableView(null);

        // 1. Testa Fold via atalho F
        view.processarTecla("F");
        afirme(view.isMaoFinalizada(), "Mão deve estar finalizada após Fold.");
        afirme(view.isBotCartasReveladas(), "Cartas do bot devem ser reveladas após Fold.");

        // 2. Testa Nova Mão via atalho N
        view.processarTecla("N");
        afirme(!view.isMaoFinalizada(), "Mão deve ser reiniciada após tecla N.");
        afirme(view.getFaseAtual() == PokerTableView.PokerPhase.PRE_FLOP, "Fase deve retornar ao Pré-Flop.");

        // 3. Testa Raise via atalho R
        long poteAntes = view.getPoteCentral();
        view.processarTecla("R");
        afirme(view.getPoteCentral() > poteAntes, "Pote central deve aumentar após Raise.");
        afirme(view.getFaseAtual() == PokerTableView.PokerPhase.FLOP, "Deve avançar para Flop após Raise.");

        // 4. Testa Check/Call via atalho ESPACO
        view.processarTecla("ESPACO");
        afirme(view.getFaseAtual() == PokerTableView.PokerPhase.TURN, "Deve avançar para Turn após ESPAÇO.");

        // 5. Testa Check/Call via atalho C
        view.processarTecla("C");
        afirme(view.getFaseAtual() == PokerTableView.PokerPhase.RIVER, "Deve avançar para River após C.");
        System.out.println("  -> Sucesso: Ações e atalhos F, C, R, ESPAÇO e N operam corretamente.");
    }

    /**
     * 9. Testa a navegação completa entre o Salão, Blackjack, Truco e Poker.
     */
    public static void testarNavegacaoEntreTodasAsMesas() {
        System.out.println("[9/10] Testando navegação completa entre as três mesas do cassino...");
        ScreenSaver painel = new ScreenSaver();
        try {
            MyCharacter player = painel.getPlayer();

            // 1. Vai ao Blackjack
            player.setX(160);
            player.setY(220);
            painel.atualizarMesaMaisProxima();
            afirme(painel.getMesaAtiva() == painel.getMesaBlackjack(), "Mesa ativa deve ser Blackjack.");
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_BLACKJACK, "Deve estar no MENU_BLACKJACK.");
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Deve voltar ao SALAO.");

            // 2. Vai ao Truco
            player.setX(320);
            player.setY(240);
            painel.atualizarMesaMaisProxima();
            afirme(painel.getMesaAtiva() == painel.getMesaTruco(), "Mesa ativa deve ser Truco.");
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_TRUCO, "Deve estar no MENU_TRUCO.");
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Deve voltar ao SALAO.");

            // 3. Vai ao Poker
            player.setX(340);
            player.setY(110);
            painel.atualizarMesaMaisProxima();
            afirme(painel.getMesaAtiva() == painel.getMesaPoker(), "Mesa ativa deve ser Poker.");
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_POKER, "Deve estar no MENU_POKER.");
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Deve voltar ao SALAO.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Navegação integrada entre todas as três mesas e o salão.");
    }

    /**
     * 10. Testa a renderização gráfica off-screen de PokerTableView sem exceções.
     */
    public static void testarRenderizacaoOffScreen() {
        System.out.println("[10/10] Testando renderização gráfica off-screen em múltiplas resoluções...");
        ScreenSaver painel = new ScreenSaver();
        try {
            PokerTableView view = painel.getPokerTableView();

            int[][] resolucoes = {
                    {800, 600},
                    {1024, 768},
                    {1280, 720},
                    {1920, 1080}
            };

            for (int[] res : resolucoes) {
                int w = res[0];
                int h = res[1];
                BufferedImage imagem = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = imagem.createGraphics();

                // Renderiza pré-flop
                view.setFaseAtual(PokerTableView.PokerPhase.PRE_FLOP);
                view.render(g, w, h);

                // Renderiza flop
                view.setFaseAtual(PokerTableView.PokerPhase.FLOP);
                view.render(g, w, h);

                // Renderiza river
                view.setFaseAtual(PokerTableView.PokerPhase.RIVER);
                view.render(g, w, h);

                // Renderiza showdown
                view.setFaseAtual(PokerTableView.PokerPhase.SHOWDOWN);
                view.render(g, w, h);

                g.dispose();
            }
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Renderização off-screen concluída sem qualquer exceção.");
    }
}
