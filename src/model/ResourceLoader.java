package model;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.Image;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário responsável pelo carregamento seguro de recursos visuais (PNG) e sonoros (WAV).
 *
 * <p>Diretrizes arquiteturais aplicadas:</p>
 * <ul>
 *   <li>Prioriza o carregamento pelo Classpath via {@code getResourceAsStream} / {@code getResource},
 *       garantindo funcionamento tanto no ambiente de desenvolvimento quanto empacotado em arquivo JAR.</li>
 *   <li>Oferece fallback transparente para caminhos do sistema de arquivos local.</li>
 *   <li>Fecha streams de áudio {@link AudioInputStream} estritamente com bloco {@code try-with-resources}.</li>
 *   <li>Permite pré-carregamento assíncrono ou fora da Event Dispatch Thread (EDT) do Swing.</li>
 *   <li>Mantém cache em memória para evitar releituras redundantes de disco.</li>
 * </ul>
 */
public final class ResourceLoader {

    private static final Map<String, Image> cacheImagens = new HashMap<>();

    private ResourceLoader() {
        // Classe utilitária estática
    }

    /**
     * Executa o pré-carregamento dos assets principais fora da EDT.
     */
    public static void precarregar() {
        carregarImagem("src/image/avatar.png");
        carregarImagem("src/image/table.png");
        // Valida e pré-aquece o subsistema de áudio
        Clip clipTeste = carregarAudio("src/sound/interact.wav");
        if (clipTeste != null) {
            clipTeste.close();
        }
    }

    /**
     * Carrega uma imagem PNG pelo Classpath ou pelo sistema de arquivos, utilizando cache.
     *
     * @param path caminho relativo ou absoluto para o arquivo de imagem.
     * @return {@link Image} carregada ou {@code null} se o arquivo não for encontrado.
     */
    public static synchronized Image carregarImagem(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        if (cacheImagens.containsKey(path)) {
            return cacheImagens.get(path);
        }

        Image img = null;

        // 1. Tenta carregar via Classpath (várias convenções de caminho para compatibilidade)
        String[] variantesClasspath = {
                path.startsWith("/") ? path : "/" + path,
                path.startsWith("src/") ? "/" + path.substring(4) : "/" + path,
                path.replace("src/", "")
        };

        for (String cpPath : variantesClasspath) {
            try (InputStream is = ResourceLoader.class.getResourceAsStream(cpPath)) {
                if (is != null) {
                    img = ImageIO.read(is);
                    if (img != null) {
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Fallback para sistema de arquivos local
        if (img == null) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    img = ImageIO.read(file);
                }
            } catch (Exception e) {
                System.err.println("Aviso: Falha ao ler imagem em " + path + ": " + e.getMessage());
            }
        }

        if (img != null) {
            cacheImagens.put(path, img);
        }

        return img;
    }

    /**
     * Carrega um arquivo de áudio WAV em um {@link Clip}, fechando o stream com try-with-resources.
     *
     * @param path caminho para o arquivo WAV.
     * @return {@link Clip} aberto e pronto para reprodução, ou {@code null} se indisponível.
     */
    public static Clip carregarAudio(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        // 1. Tenta carregar via Classpath URL
        String[] variantesClasspath = {
                path.startsWith("/") ? path : "/" + path,
                path.startsWith("src/") ? "/" + path.substring(4) : "/" + path
        };

        for (String cpPath : variantesClasspath) {
            URL url = ResourceLoader.class.getResource(cpPath);
            if (url != null) {
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(url)) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais);
                    return clip;
                } catch (Exception ignored) {
                }
            }
        }

        // 2. Fallback para arquivo local no sistema de arquivos
        File file = new File(path);
        if (file.exists()) {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                return clip;
            } catch (Exception e) {
                System.err.println("Aviso: Falha ao carregar audio em " + path + ": " + e.getMessage());
            }
        }

        return null;
    }
}
