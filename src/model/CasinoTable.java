package model;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

/**
 * Representa uma mesa interativa no salão do cassino.
 *
 * Pilares de POO demonstrados:
 * - Herança: estende MyRectangle (que estende MyShape), herdando atributos de
 *   posição e dimensão que definem o FOOTPRINT (base física sólida no chão).
 * - Polimorfismo: sobrescreve draw(Graphics2D g) para renderizar o sprite da mesa
 *   alinhado isometricamente ao ponto de apoio da base no chão.
 */
public class CasinoTable extends MyRectangle {

    private Image sprite;
    private double originX;
    private double originY;
    private String gameName;

    /**
     * Construtor da mesa do cassino.
     *
     * @param x              coordenada X da base no mundo (em pixels de mundo)
     * @param y              coordenada Y da base no mundo (em pixels de mundo)
     * @param footprintWidth largura da base sólida no chão
     * @param footprintDepth profundidade da base sólida no chão
     * @param imagePath      caminho para o sprite PNG da mesa
     * @param gameName       nome do jogo (ex.: "Blackjack")
     * @param originX        origem horizontal da projeção isométrica
     * @param originY        origem vertical da projeção isométrica
     */
    public CasinoTable(int x, int y, int footprintWidth, int footprintDepth,
                       String imagePath, String gameName, double originX, double originY) {
        super(x, y, footprintWidth, footprintDepth, new Color(20, 115, 60));
        this.gameName = gameName;
        this.originX = originX;
        this.originY = originY;

        this.sprite = ResourceLoader.carregarImagem(imagePath);
    }

    public void updateOrigins(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
    }

    /**
     * Calcula o valor de profundidade Z para ordenação visual correta no chão.
     */
    public double getDepthZ() {
        return IsometricHelper.getDepthZ(this.x, this.y, this.width, this.height, originY);
    }

    /**
     * Verifica se o avatar está a uma distância mínima de interação da mesa.
     */
    public boolean isPlayerNear(MyCharacter player, int radius) {
        if (player == null) {
            return false;
        }
        // Caixa de interação expandida
        MyRectangle interactionZone = new MyRectangle(
                this.x - radius,
                this.y - radius,
                this.width + (radius * 2),
                this.height + (radius * 2)
        );
        return interactionZone.intersects(player);
    }

    /**
     * Renderiza a mesa projetada na tela.
     */
    @Override
    public void draw(Graphics2D g) {
        // Ponto de apoio no chão (centro da base do footprint)
        double footX = this.x + this.width * 0.5;
        double footY = this.y + this.height * 0.5;

        int screenFootX = (int) Math.round(IsometricHelper.toScreenX(footX, footY, originX));
        int screenFootY = (int) Math.round(IsometricHelper.toScreenY(footX, footY, originY));

        if (this.sprite != null) {
            // Posiciona o sprite de modo que a base da mesa coincida com a projeção no chão
            int spriteW = sprite.getWidth(null);
            int spriteH = sprite.getHeight(null);
            int drawX = screenFootX - (spriteW / 2);
            int drawY = screenFootY - spriteH + 18; // offset dos pés no sprite
            g.drawImage(this.sprite, drawX, drawY, null);
        } else {
            // Fallback geométrico desenhando a base isométrica
            g.setColor(this.color);
            g.fillOval(screenFootX - 40, screenFootY - 20, 80, 40);
        }
    }

    public String getGameName() {
        return gameName;
    }

    public Image getSprite() {
        return sprite;
    }
}
