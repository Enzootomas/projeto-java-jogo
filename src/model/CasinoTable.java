package model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;

/**
 * Representa uma mesa interativa no salão do cassino.
 *
 * <p>Pilares de POO demonstrados:</p>
 * <ul>
 *   <li><strong>Herança:</strong> estende {@link MyRectangle} (e {@link MyShape}), usando posição e
 *       dimensão como o FOOTPRINT sólido no solo.</li>
 *   <li><strong>Polimorfismo:</strong> sobrescreve {@link #draw(Graphics2D)} para renderizar o sprite
 *       isométrico da mesa, sombra no solo e plaqueta temática de identificação.</li>
 * </ul>
 */
public class CasinoTable extends MyRectangle {

    private Image sprite;
    private double originX;
    private double originY;
    private String gameName;

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

    public double getDepthZ() {
        return IsometricHelper.getDepthZ(this.x, this.y, this.width, this.height, originY);
    }

    public boolean isPlayerNear(MyCharacter player, int radius) {
        if (player == null) {
            return false;
        }
        MyRectangle interactionZone = new MyRectangle(
                this.x - radius,
                this.y - radius,
                this.width + (radius * 2),
                this.height + (radius * 2)
        );
        return interactionZone.intersects(player);
    }

    public double getDistanceSqToPlayer(MyCharacter player) {
        if (player == null) {
            return Double.MAX_VALUE;
        }
        double mesaCenterX = this.x + (this.width * 0.5);
        double mesaCenterY = this.y + (this.height * 0.5);
        double playerCenterX = player.getX() + (player.getWidth() * 0.5);
        double playerCenterY = player.getY() + (player.getHeight() * 0.5);

        double dx = mesaCenterX - playerCenterX;
        double dy = mesaCenterY - playerCenterY;
        return (dx * dx) + (dy * dy);
    }

    @Override
    public void draw(Graphics2D g) {
        double footX = this.x + this.width * 0.5;
        double footY = this.y + this.height * 0.5;

        int screenFootX = (int) Math.round(IsometricHelper.toScreenX(footX, footY, originX));
        int screenFootY = (int) Math.round(IsometricHelper.toScreenY(footX, footY, originY));

        // 1. Sombra suave isométrica sob a mesa
        g.setColor(new Color(0, 0, 0, 72));
        g.fillOval(screenFootX - 54, screenFootY - 22, 108, 46);

        // 2. Sprite ou fallback geométrico da mesa
        if (this.sprite != null) {
            int spriteW = sprite.getWidth(null);
            int spriteH = sprite.getHeight(null);
            int drawX = screenFootX - (spriteW / 2);
            int drawY = screenFootY - spriteH + 18;
            g.drawImage(this.sprite, drawX, drawY, null);
        } else {
            g.setColor(this.color);
            g.fillOval(screenFootX - 40, screenFootY - 20, 80, 40);
        }

        // 3. Plaqueta elegante com cor de destaque por jogo
        if (this.gameName != null && !this.gameName.isEmpty()) {
            Color corAcento = escolherCorAcentoMesa();
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();
            String rotulo = this.gameName.toUpperCase();
            int strW = fm.stringWidth(rotulo);
            int padX = 8;
            int boxW = strW + (padX * 2);
            int boxH = fm.getHeight() + 4;
            int boxX = screenFootX - (boxW / 2);
            int boxY = screenFootY - 64;

            // Sombra da plaqueta
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(boxX + 2, boxY + 2, boxW, boxH, 7, 7);

            // Fundo escuro aquecido
            g.setColor(new Color(22, 14, 18, 225));
            g.fillRoundRect(boxX, boxY, boxW, boxH, 7, 7);

            // Faixa de acento inferior
            g.setColor(corAcento);
            g.fillRoundRect(boxX + 3, boxY + boxH - 3, boxW - 6, 2, 2, 2);

            // Borda dourada
            g.setColor(new Color(235, 198, 86));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(boxX, boxY, boxW, boxH, 7, 7);
            g.setStroke(new BasicStroke(1.0f));

            g.setColor(new Color(255, 240, 180));
            g.drawString(rotulo, boxX + padX, boxY + fm.getAscent() + 2);
        }
    }

    private Color escolherCorAcentoMesa() {
        if ("Poker".equalsIgnoreCase(gameName)) {
            return new Color(196, 48, 68);
        }
        if ("Truco".equalsIgnoreCase(gameName)) {
            return new Color(62, 138, 214);
        }
        return new Color(46, 178, 104); // Blackjack
    }

    public String getGameName() {
        return gameName;
    }

    public Image getSprite() {
        return sprite;
    }
}
