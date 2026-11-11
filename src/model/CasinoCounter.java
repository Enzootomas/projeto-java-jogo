package model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Representa o balcão de atendimento e bar no salão do cassino.
 *
 * Pilares de POO aplicados:
 * - Herança: estende MyRectangle (que estende MyShape), reaproveitando os atributos
 *   de posição (x, y) e dimensão (width, height) como o FOOTPRINT físico sólido no chão.
 * - Reúso de código: os métodos intersects(MyShape), move e getters/setters da superclasse
 *   fornecem todo o comportamento de colisão física e detecção geométrica sem reescrita.
 * - Polimorfismo: sobrescreve draw(Graphics2D g) para renderizar a estrutura volumétrica
 *   do balcão em projeção isométrica 2:1, com tampo chanfrado e painéis com frisos dourados.
 */
public class CasinoCounter extends MyRectangle {

    private double originX;
    private double originY;
    private final String rotulo;

    /**
     * Construtor do balcão do cassino.
     *
     * @param x       coordenada X do footprint no mundo
     * @param y       coordenada Y do footprint no mundo
     * @param width   largura física da base sólida no chão
     * @param height  profundidade física da base sólida no chão
     * @param rotulo  identificação textual do balcão (ex: "BAR", "RECEPCAO")
     * @param originX origem horizontal da projeção isométrica
     * @param originY origem vertical da projeção isométrica
     */
    public CasinoCounter(int x, int y, int width, int height, String rotulo, double originX, double originY) {
        super(x, y, width, height, new Color(70, 30, 15)); // Cor base de mogno clássico
        this.rotulo = rotulo;
        this.originX = originX;
        this.originY = originY;
    }

    public void updateOrigins(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
    }

    /**
     * Retorna a profundidade visual Z calculada pelo ponto de apoio no solo.
     * Permite que o Z-sorting posicione o avatar à frente ou atrás do balcão conforme a posição.
     */
    public double getDepthZ() {
        return IsometricHelper.getDepthZ(this.x, this.y, this.width, this.height, originY);
    }

    public String getRotulo() {
        return rotulo;
    }

    @Override
    public void draw(Graphics2D g) {
        // Vértices do footprint no solo (projeção isométrica)
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

        int alturaBalcao = 36; // Elevação vertical na tela

        // 1. Sombra projetada no solo
        g.setColor(new Color(0, 0, 0, 75));
        int[] sombraX = {sxNorte, sxLeste + 6, sxSul + 6, sxOeste};
        int[] sombraY = {syNorte + 4, syLeste + 6, sySul + 6, syOeste + 4};
        g.fillPolygon(sombraX, sombraY, 4);

        // 2. Face frontal esquerda (Sudoeste: de Oeste para Sul)
        int[] faceOesteX = {sxOeste, sxSul, sxSul, sxOeste};
        int[] faceOesteY = {syOeste, sySul, sySul - alturaBalcao, syOeste - alturaBalcao};
        g.setColor(new Color(64, 28, 14));
        g.fillPolygon(faceOesteX, faceOesteY, 4);
        g.setColor(new Color(40, 15, 8));
        g.drawPolygon(faceOesteX, faceOesteY, 4);

        // Friso dourado na face esquerda
        g.setColor(new Color(212, 175, 55));
        g.drawLine(sxOeste, syOeste - 10, sxSul, sySul - 10);
        g.drawLine(sxOeste, syOeste - 26, sxSul, sySul - 26);

        // 3. Face frontal direita (Sudeste: de Sul para Leste)
        int[] faceLesteX = {sxSul, sxLeste, sxLeste, sxSul};
        int[] faceLesteY = {sySul, syLeste, syLeste - alturaBalcao, sySul - alturaBalcao};
        g.setColor(new Color(88, 38, 20));
        g.fillPolygon(faceLesteX, faceLesteY, 4);
        g.setColor(new Color(40, 15, 8));
        g.drawPolygon(faceLesteX, faceLesteY, 4);

        // Friso dourado na face direita
        g.setColor(new Color(212, 175, 55));
        g.drawLine(sxSul, sySul - 10, sxLeste, syLeste - 10);
        g.drawLine(sxSul, sySul - 26, sxLeste, syLeste - 26);

        // 4. Tampo superior (elevado)
        int[] topoX = {sxNorte, sxLeste, sxSul, sxOeste};
        int[] topoY = {syNorte - alturaBalcao, syLeste - alturaBalcao, sySul - alturaBalcao, syOeste - alturaBalcao};
        g.setColor(new Color(130, 60, 30));
        g.fillPolygon(topoX, topoY, 4);

        // Borda dourada do tampo superior
        g.setColor(new Color(230, 190, 80));
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(topoX, topoY, 4);
        g.setStroke(new BasicStroke(1.0f));

        // 5. Letreiro/Rótulo na face frontal
        if (rotulo != null && !rotulo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(255, 230, 140));
            int midX = (sxSul + sxLeste) / 2 - 14;
            int midY = (sySul + syLeste) / 2 - (alturaBalcao / 2) + 4;
            g.drawString(rotulo, midX, midY);
        }
    }
}
