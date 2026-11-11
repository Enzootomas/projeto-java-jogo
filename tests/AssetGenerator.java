package tests;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * Utilitário para gerar os assets iniciais (PNGs e WAV) pré-carregados pelo jogo.
 */
public class AssetGenerator {

    public static void main(String[] args) throws Exception {
        gerarAvatarPNG(new File("src/image/avatar.png"));
        gerarMesaPNG(new File("src/image/table.png"));
        gerarAudioInteracaoWAV(new File("src/sound/interact.wav"));
        System.out.println("Assets gerados com sucesso em src/image/ e src/sound/");
    }

    private static void gerarAvatarPNG(File destino) throws IOException {
        int w = 48;
        int h = 72;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Sombra suave sob os pés
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(10, 62, 28, 9);

        // Pernas e Calça social preta
        g.setColor(new Color(25, 25, 30));
        g.fillRect(16, 42, 6, 22);
        g.fillRect(26, 42, 6, 22);

        // Sapatos sociais
        g.setColor(new Color(15, 15, 18));
        g.fillRect(14, 62, 9, 5);
        g.fillRect(25, 62, 9, 5);

        // Tronco: Blazer / Smoking elegante
        g.setColor(new Color(30, 35, 50));
        g.fillRect(13, 24, 22, 20);

        // Camisa social branca e gravata borboleta vermelha
        g.setColor(Color.WHITE);
        g.fillRect(20, 24, 8, 14);
        g.setColor(new Color(180, 20, 20)); // Gravata
        g.fillRect(22, 26, 4, 4);

        // Cabeça / Rosto estilo Habbo
        Color pele = new Color(250, 210, 175);
        g.setColor(pele);
        g.fillRect(16, 10, 16, 15);

        // Olhos pixel art
        g.setColor(new Color(40, 30, 20));
        g.fillRect(19, 16, 2, 3);
        g.fillRect(27, 16, 2, 3);

        // Cabelo castanho com topete
        g.setColor(new Color(85, 45, 20));
        g.fillRect(15, 6, 18, 7);
        g.fillRect(13, 9, 4, 6);
        g.fillRect(31, 9, 4, 6);

        // Contorno fino sutil
        g.setColor(new Color(10, 10, 15, 140));
        g.drawRect(13, 24, 22, 20);
        g.drawRect(16, 10, 16, 15);

        g.dispose();
        ImageIO.write(img, "png", destino);
    }

    private static void gerarMesaPNG(File destino) throws IOException {
        int w = 120;
        int h = 90;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Sombra no chão
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(10, 50, 100, 35);

        // Pés/Base de madeira maciça
        g.setColor(new Color(80, 45, 20));
        g.fillRect(25, 40, 12, 30);
        g.fillRect(83, 40, 12, 30);
        g.fillRect(54, 45, 12, 28);

        // Borda acolchoada de couro da mesa de cassino
        g.setColor(new Color(120, 60, 25));
        g.fillOval(10, 15, 100, 50);

        // Feltro verde clássico de Blackjack
        g.setColor(new Color(20, 115, 60));
        g.fillOval(16, 19, 88, 42);

        // Linhas de demarcação de apostas no feltro
        g.setColor(new Color(230, 210, 110, 180));
        g.drawArc(26, 25, 68, 30, 0, -180);

        // Plaquinha "BLACKJACK"
        g.setColor(new Color(255, 215, 0));
        g.fillRect(52, 28, 16, 5);

        g.dispose();
        ImageIO.write(img, "png", destino);
    }

    private static void gerarAudioInteracaoWAV(File destino) throws IOException {
        // Gera um áudio curto de 180 ms simulando o tilintar de duas fichas de cassino
        float sampleRate = 44100f;
        int durationMs = 200;
        int totalSamples = (int) (sampleRate * durationMs / 1000);
        byte[] audioBytes = new byte[totalSamples * 2]; // 16-bit mono

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            // Combinação de frequências metálicas harmônicas (fichas acrílicas/argila)
            double freq1 = 2800.0;
            double freq2 = 4200.0;
            double decay = Math.exp(-t * 22.0); // decaimento rápido
            double sample = (Math.sin(2 * Math.PI * freq1 * t) * 0.6 + Math.sin(2 * Math.PI * freq2 * t) * 0.4) * decay;

            short s = (short) (sample * 30000);
            // Little-endian
            audioBytes[2 * i] = (byte) (s & 0xFF);
            audioBytes[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        AudioInputStream ais = new AudioInputStream(bais, format, totalSamples);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, destino);
    }
}
