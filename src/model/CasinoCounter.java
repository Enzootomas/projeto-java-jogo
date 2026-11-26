package model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Representa o balcão decorativo do bar no salão do cassino.
 *
 * <p>Mantém o footprint físico sólido herdado de {@link MyRectangle} para colisão no solo,
 * adicionando acabamento em mogno, apoio de latão dourado e itens decorativos em perspectiva
 * isométrica sobre o tampo (garrafas, copos e bandeja).</p>
 */
public class CasinoCounter extends MyRectangle {

    private double originX;
    private double originY;
    private final String rotulo;

    public CasinoCounter(int x, int y, int width, int height, String rotulo, double originX, double originY) {
        super(x, y, width, height, new Color(70, 30, 15));
        this.rotulo = rotulo;
        this.originX = originX;
        this.originY = originY;
    }

    public void updateOrigins(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
    }

    public double getDepthZ() {
        return IsometricHelper.getDepthZ(this.x, this.y, this.width, this.height, originY);
    }

    public String getRotulo() {
        return rotulo;
    }

    @Override
    public void draw(Graphics2D g) {
        double wx0 = this.x;
        double wy0 = this.y;
        double wx1 = this.x + this.width;
        double wy1 = this.y + this.height;

        int sxNorte = (int) Math.round(IsometricHelper.toScreenX(wx0, wy0, originX));
        int syNorte = (int) Math.round(IsometricHelper.toScreenY(wx0, wy0, originY));

        int sxLeste = (int) Math.round(IsometricHelper.toScreenX(wx1, wy0, originX));
        int syLeste = (int) Math.round(IsometricHelper.toScreenY(wx1, wy0, originY));

        int sxSul = (int) Math.round(IsometricHelper.toScreenX(wx1, wy1, originX));
        int sySul = (int) Math.round(IsometricHelper.toScreenY(wx1, wy1, originY));

        int sxOeste = (int) Math.round(IsometricHelper.toScreenX(wx0, wy1, originX));
        int syOeste = (int) Math.round(IsometricHelper.toScreenY(wx0, wy1, originY));

        int alturaBalcao = 36;

        // 1. Sombra suave projetada no piso
        g.setColor(new Color(0, 0, 0, 82));
        int[] sombraX = {sxNorte, sxLeste + 7, sxSul + 7, sxOeste};
        int[] sombraY = {syNorte + 4, syLeste + 6, sySul + 6, syOeste + 4};
        g.fillPolygon(sombraX, sombraY, 4);

        // 2. Face frontal esquerda (Oeste -> Sul, voltada ao salão)
        int[] faceOesteX = {sxOeste, sxSul, sxSul, sxOeste};
        int[] faceOesteY = {syOeste, sySul, sySul - alturaBalcao, syOeste - alturaBalcao};
        g.setColor(new Color(62, 26, 16));
        g.fillPolygon(faceOesteX, faceOesteY, 4);
        g.setColor(new Color(38, 14, 10));
        g.drawPolygon(faceOesteX, faceOesteY, 4);

        // Painel almofadado e frisos dourados na face frontal esquerda
        g.setColor(new Color(84, 34, 22));
        g.drawLine(sxOeste + 6, syOeste - 6, sxSul - 6, sySul - 6);
        g.setColor(new Color(218, 178, 64));
        g.setStroke(new BasicStroke(1.3f));
        g.drawLine(sxOeste, syOeste - 8, sxSul, sySul - 8);   // Apoio de pés em latão
        g.drawLine(sxOeste, syOeste - 29, sxSul, sySul - 29); // Friso superior

        // 3. Face frontal direita (Sul -> Leste)
        int[] faceLesteX = {sxSul, sxLeste, sxLeste, sxSul};
        int[] faceLesteY = {sySul, syLeste, syLeste - alturaBalcao, sySul - alturaBalcao};
        g.setColor(new Color(86, 36, 22));
        g.fillPolygon(faceLesteX, faceLesteY, 4);
        g.setColor(new Color(38, 14, 10));
        g.drawPolygon(faceLesteX, faceLesteY, 4);

        g.setColor(new Color(218, 178, 64));
        g.drawLine(sxSul, sySul - 8, sxLeste, syLeste - 8);
        g.drawLine(sxSul, sySul - 29, sxLeste, syLeste - 29);

        // 4. Tampo superior em mogno polido com brilho sutil
        int[] topoX = {sxNorte, sxLeste, sxSul, sxOeste};
        int[] topoY = {syNorte - alturaBalcao, syLeste - alturaBalcao, sySul - alturaBalcao, syOeste - alturaBalcao};
        g.setColor(new Color(122, 54, 28));
        g.fillPolygon(topoX, topoY, 4);

        // Faixa interna polida do tampo
        int[] internoX = {
                (sxNorte * 5 + sxSul) / 6,
                (sxLeste * 5 + sxOeste) / 6,
                (sxSul * 5 + sxNorte) / 6,
                (sxOeste * 5 + sxLeste) / 6
        };
        int[] internoY = {
                ((syNorte * 5 + sySul) / 6) - alturaBalcao,
                ((syLeste * 5 + syOeste) / 6) - alturaBalcao,
                ((sySul * 5 + syNorte) / 6) - alturaBalcao,
                ((syOeste * 5 + syLeste) / 6) - alturaBalcao
        };
        g.setColor(new Color(145, 68, 36, 150));
        g.fillPolygon(internoX, internoY, 4);

        // Borda dourada do tampo superior
        g.setColor(new Color(236, 198, 86));
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(topoX, topoY, 4);
        g.setStroke(new BasicStroke(1.0f));

        // 5. Itens decorativos sobre o tampo em perspectiva isométrica 2:1
        desenharDecoracaoBar(g, alturaBalcao);

        // 6. Plaqueta "BAR" discreta na face frontal direita
        if (rotulo != null && !rotulo.isEmpty()) {
            int midX = (sxSul + sxLeste) / 2;
            int midY = (sySul + syLeste) / 2 - (alturaBalcao / 2) + 2;
            g.setColor(new Color(24, 12, 10, 200));
            g.fillRoundRect(midX - 16, midY - 9, 32, 14, 4, 4);
            g.setColor(new Color(225, 185, 70));
            g.drawRoundRect(midX - 16, midY - 9, 32, 14, 4, 4);
            g.setFont(new Font("SansSerif", Font.BOLD, 9));
            g.setColor(new Color(255, 232, 145));
            g.drawString(rotulo, midX - 10, midY + 1);
        }
    }

    /**
     * Desenha garrafas, bandeja dourada e copos posicionados sobre o tampo do balcão
     * usando as coordenadas do mundo projetadas por {@link IsometricHelper}.
     */
    private void desenharDecoracaoBar(Graphics2D g, int alturaBalcao) {
        // Três garrafas alinhadas na borda traseira do tampo
        desenharGarrafa(g, this.x + 16, this.y + 9, alturaBalcao, new Color(175, 92, 24), new Color(240, 215, 160));
        desenharGarrafa(g, this.x + 28, this.y + 8, alturaBalcao, new Color(28, 120, 62), new Color(230, 235, 210));
        desenharGarrafa(g, this.x + 40, this.y + 9, alturaBalcao, new Color(138, 22, 42), new Color(245, 220, 175));

        // Pequena bandeja dourada no lado direito do tampo
        int tx = (int) Math.round(IsometricHelper.toScreenX(this.x + 61, this.y + 16, originX));
        int ty = (int) Math.round(IsometricHelper.toScreenY(this.x + 61, this.y + 16, originY)) - alturaBalcao;
        g.setColor(new Color(0, 0, 0, 55));
        g.fillOval(tx - 15, ty - 5, 30, 13);
        g.setColor(new Color(196, 154, 48));
        g.fillOval(tx - 15, ty - 7, 30, 12);
        g.setColor(new Color(242, 212, 112));
        g.drawOval(tx - 15, ty - 7, 30, 12);

        // Dois copos sobre a bandeja e próximo ao centro do tampo
        desenharCopoBaixo(g, this.x + 56, this.y + 16, alturaBalcao, new Color(220, 145, 45));
        desenharTacaCocktail(g, this.x + 67, this.y + 15, alturaBalcao, new Color(215, 58, 78));
        desenharCopoBaixo(g, this.x + 24, this.y + 21, alturaBalcao, new Color(205, 130, 38));
    }

    private void desenharGarrafa(Graphics2D g, double wx, double wy, int alturaBalcao,
                                 Color corVidro, Color corRotulo) {
        int sx = (int) Math.round(IsometricHelper.toScreenX(wx, wy, originX));
        int sy = (int) Math.round(IsometricHelper.toScreenY(wx, wy, originY)) - alturaBalcao;

        // Sombra pequena na base
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(sx - 4, sy - 2, 9, 4);

        // Corpo da garrafa
        g.setColor(corVidro);
        g.fillRoundRect(sx - 3, sy - 12, 7, 12, 3, 3);

        // Gargalo e tampa dourada
        g.fillRect(sx - 1, sy - 17, 3, 6);
        g.setColor(new Color(230, 192, 75));
        g.fillRect(sx - 1, sy - 18, 3, 2);

        // Rótulo central
        g.setColor(corRotulo);
        g.fillRect(sx - 2, sy - 9, 5, 4);

        // Brilho especular do vidro
        g.setColor(new Color(255, 255, 255, 135));
        g.drawLine(sx - 2, sy - 11, sx - 2, sy - 2);
    }

    private void desenharCopoBaixo(Graphics2D g, double wx, double wy, int alturaBalcao, Color corBebida) {
        int sx = (int) Math.round(IsometricHelper.toScreenX(wx, wy, originX));
        int sy = (int) Math.round(IsometricHelper.toScreenY(wx, wy, originY)) - alturaBalcao;

        g.setColor(new Color(210, 230, 245, 155));
        g.fillRect(sx - 3, sy - 7, 6, 7);
        g.setColor(corBebida);
        g.fillRect(sx - 2, sy - 5, 4, 4);
        g.setColor(new Color(255, 255, 255, 190));
        g.drawRect(sx - 3, sy - 7, 6, 7);
    }

    private void desenharTacaCocktail(Graphics2D g, double wx, double wy, int alturaBalcao, Color corBebida) {
        int sx = (int) Math.round(IsometricHelper.toScreenX(wx, wy, originX));
        int sy = (int) Math.round(IsometricHelper.toScreenY(wx, wy, originY)) - alturaBalcao;

        // Base e haste da taça
        g.setColor(new Color(230, 240, 250, 200));
        g.drawLine(sx - 2, sy, sx + 2, sy);
        g.drawLine(sx, sy, sx, sy - 6);

        // Cálice triangular com bebida
        int[] caliceX = {sx - 4, sx + 4, sx};
        int[] caliceY = {sy - 11, sy - 11, sy - 6};
        g.setColor(corBebida);
        g.fillPolygon(caliceX, caliceY, 3);
        g.setColor(new Color(240, 248, 255, 210));
        g.drawPolygon(caliceX, caliceY, 3);
    }
}
