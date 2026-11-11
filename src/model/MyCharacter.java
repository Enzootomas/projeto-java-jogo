package model;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.util.List;

/**
 * Representa o avatar do jogador que caminha pelo salão do cassino.
 *
 * Pilares de POO demonstrados:
 * - Herança: estende MyShape, reaproveitando coordenadas x, y e dimensões width, height.
 * - Encapsulamento & Distinção Semântica:
 *   As dimensões width e height herdadas de MyShape definem exclusivamente o FOOTPRINT
 *   (base física de colisão nos pés no plano do chão do mundo), enquanto a imagem (sprite)
 *   possui dimensões visuais próprias (mais alta que o footprint).
 * - Polimorfismo: sobrescreve draw(Graphics2D g) para renderizar o sprite alinhado aos pés
 *   na projeção isométrica.
 */
public class MyCharacter extends MyShape {

    private Image sprite;
    private Clip collisionClip;
    private double exactX;
    private double exactY;
    private double originX;
    private double originY;
    private boolean walking;
    private String currentDirection = "DOWN";

    /**
     * Construtor completo com dimensões de footprint explícitas.
     *
     * @param imagePath caminho para o arquivo de imagem PNG do sprite
     * @param x         posição X inicial no plano do mundo (pixels de mundo)
     * @param y         posição Y inicial no plano do mundo (pixels de mundo)
     * @param width     largura da base de colisão nos pés (footprint)
     * @param height    profundidade da base de colisão nos pés (footprint)
     * @param soundPath caminho para o arquivo de áudio WAV
     */
    public MyCharacter(String imagePath, int x, int y, int width, int height, String soundPath) {
        super(x, y, width, height, new Color(50, 100, 220));
        this.exactX = x;
        this.exactY = y;
        carregarSprite(imagePath);
        carregarAudio(soundPath);
    }

    /**
     * Construtor de conveniência com footprint padrão de 24x24 pixels de mundo.
     *
     * @param imagePath caminho para o arquivo de imagem PNG
     * @param x         posição X inicial
     * @param y         posição Y inicial
     * @param soundPath caminho para o arquivo de áudio WAV
     */
    public MyCharacter(String imagePath, int x, int y, String soundPath) {
        this(imagePath, x, y, 24, 24, soundPath);
    }

    private void carregarSprite(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                this.sprite = ImageIO.read(new File(imagePath));
            } catch (Exception e) {
                System.err.println("Aviso: Nao foi possivel carregar o sprite do avatar em " + imagePath + " (" + e.getMessage() + ")");
                this.sprite = null;
            }
        }
    }

    private void carregarAudio(String soundPath) {
        if (soundPath != null && !soundPath.isEmpty()) {
            try {
                File audioFile = new File(soundPath);
                if (audioFile.exists()) {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
                    this.collisionClip = AudioSystem.getClip();
                    this.collisionClip.open(ais);
                }
            } catch (Exception e) {
                System.err.println("Aviso: Nao foi possivel carregar o audio em " + soundPath + " (" + e.getMessage() + ")");
                this.collisionClip = null;
            }
        }
    }

    /**
     * Atualiza as origens de projeção isométrica para renderização e cálculos de tela.
     */
    public void updateOrigins(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
    }

    /**
     * Retorna a imagem do sprite carregada.
     */
    public Image getSprite() {
        return this.sprite;
    }

    /**
     * Reproduz o efeito sonoro pré-carregado, rebobinando o Clip antes de tocar.
     */
    public void playSound() {
        if (this.collisionClip != null) {
            if (this.collisionClip.isRunning()) {
                this.collisionClip.stop();
            }
            this.collisionClip.setFramePosition(0);
            this.collisionClip.start();
        }
    }

    /**
     * Libera os recursos do Clip de áudio no encerramento da aplicação.
     */
    public void dispose() {
        if (this.collisionClip != null) {
            try {
                this.collisionClip.stop();
                this.collisionClip.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Movimentação oficial exigida pelo documento do professor.
     * Move nas direções clássicas no plano com velocidade discreta e verifica bordas da tela.
     *
     * @param speed        velocidade de deslocamento em pixels
     * @param direction    direção textual ("UP", "DOWN", "LEFT", "RIGHT", etc.)
     * @param screenWidth  largura máxima da área
     * @param screenHeight altura máxima da área
     */
    public void move(int speed, String direction, int screenWidth, int screenHeight) {
        this.currentDirection = direction;
        if ("UP".equalsIgnoreCase(direction)) {
            this.y -= speed;
        } else if ("DOWN".equalsIgnoreCase(direction)) {
            this.y += speed;
        } else if ("LEFT".equalsIgnoreCase(direction)) {
            this.x -= speed;
        } else if ("RIGHT".equalsIgnoreCase(direction)) {
            this.x += speed;
        }

        // Validação de bordas usando o footprint completo
        if (this.x < 0) this.x = 0;
        if (screenWidth > this.width && this.x + this.width > screenWidth) {
            this.x = screenWidth - this.width;
        }
        if (this.y < 0) this.y = 0;
        if (screenHeight > this.height && this.y + this.height > screenHeight) {
            this.y = screenHeight - this.height;
        }

        this.exactX = this.x;
        this.exactY = this.y;
    }

    /**
     * Movimentação isométrica contínua com:
     * 1. Normalização do vetor de entrada em coordenadas visuais de tela.
     * 2. Transformação inversa para o plano cartesiano do chão.
     * 3. Resolução de colisão por eixos independentes (permitindo deslizar junto a paredes/mesas).
     * 4. Validação de limites do salão usando o footprint completo.
     * 5. Acumulação fracionária em double, sincronizando this.x e this.y inteiros sem perda de precisão.
     *
     * @param dirSx        direção X desejada na tela (-1, 0 ou +1)
     * @param dirSy        direção Y desejada na tela (-1, 0 ou +1)
     * @param speedScreen  velocidade escalar desejada na tela em pixels por segundo
     * @param dt           tempo decorrido desde o último quadro em segundos
     * @param worldLimitW  largura total do chão do salão
     * @param worldLimitH  profundidade total do chão do salão
     * @param obstacles    lista de obstáculos sólidos do salão
     */
    public void moveIsometric(double dirSx, double dirSy, double speedScreen, double dt,
                               int worldLimitW, int worldLimitH, List<MyShape> obstacles) {
        double len = Math.hypot(dirSx, dirSy);
        if (len < 0.0001) {
            this.walking = false;
            return;
        }
        this.walking = true;

        // 1. Normaliza o vetor de direção na tela
        double normSx = dirSx / len;
        double normSy = dirSy / len;

        // 2. Converte a direção unitária de tela para o mundo via transformação inversa
        double dirWx = IsometricHelper.toWorldX(normSx, normSy);
        double dirWy = IsometricHelper.toWorldY(normSx, normSy);

        // Deslocamento desejado no mundo para este quadro (dt segundos)
        double deltaWx = dirWx * speedScreen * dt;
        double deltaWy = dirWy * speedScreen * dt;

        // 3. Resolução de colisão por eixos no plano do mundo (deslizamento)
        // Tentativa no Eixo X
        double tentX = this.exactX + deltaWx;
        // Limites com o footprint completo no chão
        if (tentX < 0) tentX = 0;
        if (worldLimitW > this.width && tentX + this.width > worldLimitW) {
            tentX = worldLimitW - this.width;
        }

        MyRectangle testeCaixaX = new MyRectangle((int) Math.round(tentX), this.y, this.width, this.height);
        boolean colidiuX = false;
        if (obstacles != null) {
            for (MyShape obst : obstacles) {
                if (obst != this && testeCaixaX.intersects(obst)) {
                    colidiuX = true;
                    break;
                }
            }
        }
        if (!colidiuX) {
            this.exactX = tentX;
            this.x = (int) Math.round(this.exactX);
        }

        // Tentativa no Eixo Y
        double tentY = this.exactY + deltaWy;
        // Limites com o footprint completo no chão
        if (tentY < 0) tentY = 0;
        if (worldLimitH > this.height && tentY + this.height > worldLimitH) {
            tentY = worldLimitH - this.height;
        }

        MyRectangle testeCaixaY = new MyRectangle(this.x, (int) Math.round(tentY), this.width, this.height);
        boolean colidiuY = false;
        if (obstacles != null) {
            for (MyShape obst : obstacles) {
                if (obst != this && testeCaixaY.intersects(obst)) {
                    colidiuY = true;
                    break;
                }
            }
        }
        if (!colidiuY) {
            this.exactY = tentY;
            this.y = (int) Math.round(this.exactY);
        }
    }

    /**
     * Retorna a profundidade Z para o Z-Sorting no ponto de apoio dos pés no solo.
     */
    public double getDepthZ() {
        return IsometricHelper.getDepthZ(this.exactX, this.exactY, this.width, this.height, originY);
    }

    /**
     * Renderiza o avatar alinhado com seu ponto de apoio no chão.
     */
    @Override
    public void draw(Graphics2D g) {
        // Centro da base dos pés no mundo
        double footX = this.exactX + this.width * 0.5;
        double footY = this.exactY + this.height * 0.5;

        int screenFootX = (int) Math.round(IsometricHelper.toScreenX(footX, footY, originX));
        int screenFootY = (int) Math.round(IsometricHelper.toScreenY(footX, footY, originY));

        if (this.sprite != null) {
            int spriteW = sprite.getWidth(null);
            int spriteH = sprite.getHeight(null);
            // Ancoragem: centraliza horizontalmente e alinha a base dos pés do sprite com os pés no chão
            int drawX = screenFootX - (spriteW / 2);
            int drawY = screenFootY - spriteH + 6; // pequeno offset para apoiar a sola no solo
            g.drawImage(this.sprite, drawX, drawY, null);
        } else {
            // Fallback geométrico do avatar caso imagem falhe
            g.setColor(new Color(0, 0, 0, 70));
            g.fillOval(screenFootX - 12, screenFootY - 6, 24, 12); // sombra
            g.setColor(this.color);
            g.fillOval(screenFootX - 10, screenFootY - 45, 20, 20); // cabeça
            g.fillRect(screenFootX - 8, screenFootY - 25, 16, 25);  // corpo
        }
    }

    public double getExactX() {
        return exactX;
    }

    public double getExactY() {
        return exactY;
    }

    public boolean isWalking() {
        return walking;
    }
}
