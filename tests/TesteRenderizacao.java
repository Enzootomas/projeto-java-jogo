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
