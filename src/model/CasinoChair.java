package model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Representa uma cadeira de cassino estofada no salão.
 *
 * Pilares de POO aplicados:
 * - Herança: estende MyRectangle (que estende MyShape), herdando e encapsulando o
 *   FOOTPRINT sólido no solo (x, y, size, size) utilizado para colisão e deslizamento.
 * - Reúso de código: reaproveita a lógica de colisão geométrica da superclasse com intersects().
 * - Polimorfismo: sobrescreve draw(Graphics2D g) para renderizar a geometria tridimensional
 *   isométrica: pernas em metal dourado, assento estofado em veludo vermelho e encosto alto.
 */
public class CasinoChair extends MyRectangle {

    private double originX;
    private double originY;

    /**
     * Construtor da cadeira de cassino.
     *
     * @param x       coordenada X do footprint no solo
     * @param y       coordenada Y do footprint no solo
     * @param size    dimensão lateral da base sólida no solo (largura = profundidade)
     * @param originX origem horizontal da projeção isométrica
     * @param originY origem vertical da projeção isométrica
     */
    public CasinoChair(int x, int y, int size, double originX, double originY) {
        super(x, y, size, size, new Color(139, 0, 0)); // Vermelho bordô clássico
        this.originX = originX;
        this.originY = originY;
    }

    public void updateOrigins(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
    }

    /**
     * Retorna a profundidade visual Z calculada pelo ponto de apoio no solo.
     * Permite ao Z-sorting garantir que o avatar seja desenhado na frente ou atrás da cadeira.
     */
    public double getDepthZ() {
        return IsometricHelper.getDepthZ(this.x, this.y, this.width, this.height, originY);
    }

    @Override
    public void draw(Graphics2D g) {
        // Vértices do footprint no solo
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

        int alturaAssento = 14; // Elevação do assento
        int alturaEncosto = 28; // Elevação do topo do encosto

        // 1. Pernas da cadeira (linhas verticais douradas)
        g.setColor(new Color(180, 140, 40));
        g.setStroke(new BasicStroke(1.8f));
        g.drawLine(sxNorte, syNorte, sxNorte, syNorte - alturaAssento);
        g.drawLine(sxOeste, syOeste, sxOeste, syOeste - alturaAssento);
        g.drawLine(sxLeste, syLeste, sxLeste, syLeste - alturaAssento);
        g.drawLine(sxSul, sySul, sxSul, sySul - alturaAssento);

        // 2. Assento estofado (losango em veludo vermelho bordô)
        int[] assentoX = {sxNorte, sxLeste, sxSul, sxOeste};
        int[] assentoY = {
                syNorte - alturaAssento,
                syLeste - alturaAssento,
                sySul - alturaAssento,
                syOeste - alturaAssento
        };

        g.setColor(new Color(140, 20, 20));
        g.fillPolygon(assentoX, assentoY, 4);

        g.setColor(new Color(212, 175, 55));
        g.setStroke(new BasicStroke(1.2f));
        g.drawPolygon(assentoX, assentoY, 4);

        // 3. Encosto da cadeira (subindo na borda Noroeste/Nordeste)
        int[] encostoX = {
                sxNorte,
                sxLeste,
                sxLeste,
                sxNorte
        };
        int[] encostoY = {
                syNorte - alturaAssento,
                syLeste - alturaAssento,
                syLeste - alturaEncosto,
                syNorte - alturaEncosto
        };

        g.setColor(new Color(110, 15, 15));
        g.fillPolygon(encostoX, encostoY, 4);

        g.setColor(new Color(230, 190, 70));
        g.drawPolygon(encostoX, encostoY, 4);

        // Almofadado central do encosto
        g.setColor(new Color(160, 30, 30));
        int midEncX1 = (sxNorte + sxLeste) / 2;
        int midEncY1 = (syNorte + syLeste) / 2 - alturaAssento;
        int midEncY2 = (syNorte + syLeste) / 2 - alturaEncosto + 3;
        g.drawLine(midEncX1, midEncY1, midEncX1, midEncY2);

        g.setStroke(new BasicStroke(1.0f));
    }
}
