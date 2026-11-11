package tests;

import model.RoundState;
import view.BlackjackMockSimulator;
import view.BlackjackTableView;
import view.ScreenSaver;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Teste automatizado de renderização visual completa off-screen.
 *
 * Valida a renderização sem abrir janelas (headless-friendly):
 * 1. Salão isométrico com entidades polimórficas (List<MyShape>).
 * 2. Mesa de Blackjack em AGUARDANDO_APOSTA (feltro, saldo, slots e botões de fichas).
 * 3. Mesa de Blackjack em TURNO_JOGADOR (cartas abertas, verso da carta oculta do dealer).
 * 4. Mesa de Blackjack em FINALIZADA (revelação da carta oculta e banner de resultado em português).
 * 5. Interação de cliques e atalhos de teclado no simulador visual.
 */
public class TesteRenderizacao {

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTE DE RENDERIZACAO OFF-SCREEN (SALAO E MESA)");
        System.out.println("====================================================\n");

        int largura = 1280;
        int altura = 720;

        ScreenSaver painel = new ScreenSaver();
        painel.setSize(new Dimension(largura, altura));

        BufferedImage buffer = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = buffer.createGraphics();

        // 1. Testa Renderização do Salão Isométrico
        painel.paint(g2d);
        int pixelPiso = buffer.getRGB(640, 360);
        int pixelHud = buffer.getRGB(50, 40);
        afirme(pixelPiso != 0, "Salao isometrico renderizou piso com sucesso (Pixel: 0x" + Integer.toHexString(pixelPiso) + ")");
        afirme(pixelHud != 0, "HUD do salao renderizou com sucesso (Pixel: 0x" + Integer.toHexString(pixelHud) + ")");

        // 2. Testa Renderização da Mesa de Blackjack (Estado: AGUARDANDO_APOSTA)
        BlackjackMockSimulator simulador = (BlackjackMockSimulator) painel.getBlackjackEngine();
        BlackjackTableView mesaView = painel.getBlackjackTableView();

        BufferedImage bufferMesa = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gMesa = bufferMesa.createGraphics();

        afirme(simulador.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA,
                "Mesa inicia no estado formal AGUARDANDO_APOSTA");
        mesaView.render(gMesa, largura, altura);

        int pixelFeltroVerde = bufferMesa.getRGB(largura / 2, altura / 2);
        afirme(pixelFeltroVerde != 0, "Feltro verde da mesa de Blackjack renderizado com sucesso");

        // 3. Testa Transição para TURNO_JOGADOR e Carta Oculta
        boolean iniciou = simulador.iniciarPartida(100L);
        afirme(iniciou, "Simulador iniciou partida com aposta par de 100 fichas");
        afirme(simulador.getEstadoRodada() == RoundState.TURNO_JOGADOR, "Estado da rodada transitou para TURNO_JOGADOR");
        afirme(simulador.hasCartaOcultaDealer(), "Dealer possui carta oculta virada para baixo no turno do jogador");
        afirme(simulador.getCartasVisiveisDealer().size() == 1,
                "Apenas 1 carta do dealer e visivel para a interface durante o turno do jogador (sem vazamento)");

        // Renderiza com cartas abertas e verso da carta oculta
        mesaView.render(gMesa, largura, altura);
        afirme(simulador.getMaoJogador().size() == 2, "Jogador possui 2 cartas na mao");

        // 4. Testa Ação de Pedir Carta (Hit)
        boolean pediu = simulador.pedirCarta();
        afirme(pediu, "Acao de pedir carta (Hit) executada com sucesso");
        afirme(simulador.getMaoJogador().size() == 3, "Jogador agora possui 3 cartas na mao");

        // 5. Testa Ação de Parar (Stand) e Transição para FINALIZADA
        simulador.parar();
        afirme(simulador.getEstadoRodada() == RoundState.FINALIZADA,
                "Apos parar, a rodada transitou para FINALIZADA");
        afirme(!simulador.hasCartaOcultaDealer(), "Carta oculta do dealer foi revelada apos a finalizacao");
        afirme(simulador.getCartasVisiveisDealer().size() == 2, "Todas as cartas do dealer estao visiveis agora");
        afirme(simulador.getResultadoRodada() != null,
                "Resultado da rodada definido: " + simulador.getResultadoRodada().getDescricaoPadrao());

        // Renderiza banner final e desfecho
        mesaView.render(gMesa, largura, altura);

        // 6. Testa atalhos de clique e teclas
        mesaView.processarTecla("FICHA_1");
        mesaView.processarTecla("ESPACO");

        // 7. Testa Ambientacao do Salao e Percurso Completo do Avatar
        afirme(painel.getBalcao() != null, "Balcao do bar instanciado no salao");
        afirme(painel.getCadeiras() != null && painel.getCadeiras().size() == 3,
                "Tres cadeiras de cassino instanciadas no salao");
        afirme(painel.getObstaculos().contains(painel.getBalcao()),
                "Balcao registrado na lista de obstaculos fisicos");

        // Avatar inicia na entrada do salao (x=60, y=320)
        afirme(painel.getPlayer().getX() == 60 && painel.getPlayer().getY() == 320,
                "Avatar inicia posicionado na entrada do salao sobre o tapete");

        // Percurso: avatar caminha em direcao a mesa de Blackjack
        painel.getPlayer().setExactPosition(160, 125); // Posicao adjacente a mesa
        afirme(painel.getMesaBlackjack().isPlayerNear(painel.getPlayer(), 35),
                "Avatar detectado proximo a mesa de Blackjack");

        // Abre a mesa com a tecla E
        painel.setEstadoAtual(view.ScreenSaver.GameState.MENU_BLACKJACK);
        afirme(painel.getEstadoAtual() == view.ScreenSaver.GameState.MENU_BLACKJACK,
                "Transitou do salao para a interface da mesa de Blackjack");

        // Retorna ao salao com a tecla B
        painel.setEstadoAtual(view.ScreenSaver.GameState.SALAO);
        afirme(painel.getEstadoAtual() == view.ScreenSaver.GameState.SALAO,
                "Retornou com sucesso ao salao principal");

        // Contorno dos novos moveis: avatar circula ao redor do balcao sem sobreposicao
        painel.getPlayer().setExactPosition(110, 15); // Norte do balcao
        afirme(!painel.getPlayer().intersects(painel.getBalcao()),
                "Avatar navega ao Norte do balcao sem colisao ilegal");

        painel.getPlayer().setExactPosition(110, 80); // Sul do balcao
        afirme(!painel.getPlayer().intersects(painel.getBalcao()),
                "Avatar navega ao Sul do balcao sem colisao ilegal");

        // 8. Testes da Mesa de Truco, Trajeto Desimpedido e Selecao Contextual
        afirme(painel.getMesaTruco() != null, "Mesa de Truco instanciada no salao");
        afirme("Truco".equals(painel.getMesaTruco().getGameName()), "Mesa de Truco configurada com gameName 'Truco'");
        afirme(painel.getObstaculos().contains(painel.getMesaTruco()),
                "Mesa de Truco registrada na lista de obstaculos fisicos");
        afirme(painel.getEntidadesRenderizaveis().contains(painel.getMesaTruco()),
                "Mesa de Truco registrada na lista heterogenea de renderizacao");
        afirme(!painel.getMesaBlackjack().intersects(painel.getMesaTruco()),
                "Mesas de Blackjack e Truco possuem footprints fisicos independentes e sem sobreposicao");

        // Trajeto livre entre Blackjack e Truco (posicoes intermediarias)
        int[][] rotaLivre = {
                {200, 210},
                {240, 230},
                {270, 250}
        };
        for (int[] ponto : rotaLivre) {
            painel.getPlayer().setExactPosition(ponto[0], ponto[1]);
            boolean colidiu = false;
            for (model.MyShape obstaculo : painel.getObstaculos()) {
                if (painel.getPlayer().intersects(obstaculo)) {
                    colidiu = true;
                    break;
                }
            }
            afirme(!colidiu, "Ponto de passagem livre (" + ponto[0] + ", " + ponto[1] + ") sem bloqueio fisico");
        }

        // Cenário A: Longe de ambas as mesas -> mesaAtiva e nula e E nao abre nenhuma mesa
        painel.getPlayer().setExactPosition(60, 320); // Entrada
        painel.atualizarMesaMaisProxima();
        afirme(painel.getMesaAtiva() == null, "Nenhuma mesa ativa quando o avatar esta distante de ambas");
        painel.processarInteracaoE();
        afirme(painel.getEstadoAtual() == view.ScreenSaver.GameState.SALAO,
                "Pressionar E longe das mesas nao altera o estado do jogo (permanece SALAO)");

        // Cenário B: Proximo a mesa de Blackjack -> mesaAtiva e Blackjack e E abre Blackjack
        painel.getPlayer().setExactPosition(160, 125);
        painel.atualizarMesaMaisProxima();
        afirme(painel.getMesaAtiva() == painel.getMesaBlackjack(),
                "Mesa ativa selecionada e Blackjack quando proximo a mesa de Blackjack");
        painel.processarInteracaoE();
        afirme(painel.getEstadoAtual() == view.ScreenSaver.GameState.MENU_BLACKJACK,
                "Tecla E abre MENU_BLACKJACK quando mesa ativa e Blackjack");
        painel.processarRetornoB();
        afirme(painel.getEstadoAtual() == view.ScreenSaver.GameState.SALAO,
                "Tecla B retorna com sucesso ao salao");

        // Cenário C: Proximo a mesa de Truco -> mesaAtiva e Truco e E abre tela de Truco
        painel.getPlayer().setExactPosition(320, 245);
        painel.atualizarMesaMaisProxima();
        afirme(painel.getMesaAtiva() == painel.getMesaTruco(),
                "Mesa ativa selecionada e Truco quando proximo a mesa de Truco");
        painel.processarInteracaoE();
        afirme(painel.getEstadoAtual() == view.ScreenSaver.GameState.MENU_TRUCO,
                "Tecla E abre MENU_TRUCO quando mesa ativa e Truco");

        // Renderiza tela provisoria do Truco em buffer grafico
        BufferedImage bufferTruco = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gTruco = bufferTruco.createGraphics();
        painel.paint(gTruco);
        int pixelFeltroTruco = bufferTruco.getRGB(largura / 2, altura / 2);
        afirme(pixelFeltroTruco != 0, "Tela provisoria de Truco renderizou com sucesso off-screen");
        gTruco.dispose();

        // Cenário D: Tecla B na tela de Truco retorna ao salao
        painel.processarRetornoB();
        afirme(painel.getEstadoAtual() == view.ScreenSaver.GameState.SALAO,
                "Tecla B na tela de Truco retorna ao salao principal");

        // Cenário E: Reabrir Truco e clicar no botao 'Voltar ao Salao (B)'
        painel.setEstadoAtual(view.ScreenSaver.GameState.MENU_TRUCO);
        afirme(painel.getEstadoAtual() == view.ScreenSaver.GameState.MENU_TRUCO,
                "Transitou novamente para MENU_TRUCO");
        int pyPainel = (altura - 500) / 2;
        int centroBtnY = pyPainel + 500 - 85 + 24;
        painel.processarCliqueMouse(largura / 2, centroBtnY);
        afirme(painel.getEstadoAtual() == view.ScreenSaver.GameState.SALAO,
                "Clique no botao 'Voltar ao Salao (B)' retorna ao salao com sucesso");

        g2d.dispose();
        gMesa.dispose();
        painel.pararLoop();

        System.out.println("\n----------------------------------------------------");
        System.out.println("RESULTADO: TODOS OS TESTES DE RENDERIZACAO PASSARAM! [OK]");
        System.out.println("----------------------------------------------------");
    }

    private static void afirme(boolean condicao, String descricao) {
        if (condicao) {
            System.out.println("  [OK] " + descricao);
        } else {
            System.err.println("  [FALHA] " + descricao);
            System.exit(1);
        }
    }
}
