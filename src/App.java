import view.ScreenSaver;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

/**
 * Ponto de entrada da aplicação desktop do Cassino Social 2D.
 *
 * Configura o JFrame em tela cheia exclusiva sem bordas (undecorated),
 * inicializa o painel ScreenSaver na Event Dispatch Thread (EDT)
 * e assegura o encerramento limpo ao pressionar ESC.
 */
public class App {

    public static void main(String[] args) {
        // Pré-carregamento de imagens e áudio realizado FORA da EDT do Swing
        model.ResourceLoader.precarregar();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Cassino Social 2D - Habbo Style");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(true); // Janela sem bordas

            ScreenSaver painel = new ScreenSaver();
            frame.setContentPane(painel);

            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (gd.isFullScreenSupported()) {
                try {
                    gd.setFullScreenWindow(frame);
                } catch (Exception e) {
                    System.err.println("Aviso: FullScreen exclusivo falhou, aplicando fallback maximizado.");
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    frame.setSize(screenSize);
                    frame.setLocation(0, 0);
                    frame.setVisible(true);
                }
            } else {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                frame.setSize(screenSize);
                frame.setLocation(0, 0);
                frame.setVisible(true);
            }

            frame.revalidate();
            painel.requestFocusInWindow();
        });
    }
}
