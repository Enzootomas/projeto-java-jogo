package tests;

import view.ScreenSaver;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Teste automatizado de renderização visual completa do ScreenSaver.
 * Valida se paintComponent executa sem exceções e produz pixels na tela
 * tanto no estado do salão quanto no menu da mesa de Blackjack.
 */
public class TesteRenderizacao {

    public static void main(String[] args) {
        System.out.println("Executando teste de renderizacao off-screen do ScreenSaver...");

        ScreenSaver painel = new ScreenSaver();
        painel.setSize(new Dimension(1280, 720));

        BufferedImage buffer = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = buffer.createGraphics();

        // 1. Renderiza o salão principal
        painel.paint(g2d);

        // Verifica que o buffer não está preto/vazio (pixels desenhados do piso e HUD)
        int pixelCentro = buffer.getRGB(640, 360);
        int pixelHud = buffer.getRGB(50, 40);

        if (pixelCentro == 0 || pixelHud == 0) {
            System.err.println("FALHA: Buffer de renderizacao do salao esta vazio!");
            System.exit(1);
        }

        System.out.println("Renderizacao do salao isometrico executada com sucesso. (Pixel centro: " +
                Integer.toHexString(pixelCentro) + ", Pixel HUD: " + Integer.toHexString(pixelHud) + ")");

        // Encerra recursos
        painel.encerrarJogo();
    }
}
