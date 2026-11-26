package model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Representa uma cadeira de cassino estofada no salão com orientação explícita.
 *
 * <p>Pilares de POO aplicados:</p>
 * <ul>
 *   <li><strong>Herança:</strong> estende {@link MyRectangle} (e {@link MyShape}), encapsulando o
 *       footprint sólido no solo ({@code x, y, size, size}) para colisão e deslizamento.</li>
 *   <li><strong>Encapsulamento:</strong> mantém a orientação ({@link Orientation}) para desenhar
 *       o encosto no lado oposto ao tampo da mesa ou balcão associado.</li>
 *   <li><strong>Polimorfismo:</strong> sobrescreve {@link #draw(Graphics2D)} em projeção isométrica 2:1.</li>
 * </ul>
 */
public class CasinoChair extends MyRectangle {

    /**
     * Direção para a qual a pessoa sentada está voltada (em coordenadas do mundo isométrico).
     * O encosto é sempre desenhado na borda oposta à direção para a qual a cadeira aponta.
     */
    public enum Orientation {
        /** Voltada para -Y (mesa/balcão ao Norte; encosto na borda Sul). */
        NORTE,
        /** Voltada para +Y (mesa ao Sul; encosto na borda Norte). */
        SUL,
        /** Voltada para +X (mesa a Leste; encosto na borda Oeste). */
        LESTE,
        /** Voltada para -X (mesa a Oeste; encosto na borda Leste). */
        OESTE
    }

    private double originX;
    private double originY;
    private final Orientation orientation;

    /**
     * Construtor padrão (orientação {@link Orientation#SUL}), preservando compatibilidade com testes existentes.
     */
    public CasinoChair(int x, int y, int size, double originX, double originY) {
        this(x, y, size, Orientation.SUL, originX, originY);
    }

    /**
     * Construtor completo com orientação explícita voltada para a mesa ou balcão correspondente.
     *
     * @param x           coordenada X do footprint no solo
     * @param y           coordenada Y do footprint no solo
     * @param size        dimensão lateral da base sólida no solo (largura = profundidade)
     * @param orientation direção para a qual a cadeira está voltada (nunca nula)
     * @param originX     origem horizontal da projeção isométrica
     * @param originY     origem vertical da projeção isométrica
     */
    public CasinoChair(int x, int y, int size, Orientation orientation, double originX, double originY) {
        super(x, y, size, size, new Color(139, 0, 0));
        this.orientation = (orientation != null) ? orientation : Orientation.SUL;
        this.originX = originX;
        this.originY = originY;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void updateOrigins(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
    }

    /**
     * Retorna a profundidade visual Z calculada pelo ponto de apoio no solo.
     */
    public double getDepthZ() {
        return IsometricHelper.getDepthZ(this.x, this.y, this.width, this.height, originY);
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

        int alturaAssento = 13;
        int alturaEncosto = 27;

        // 1. Sombra suave no piso
        g.setColor(new Color(0, 0, 0, 68));
        int[] sombraX = {sxNorte, sxLeste + 2, sxSul + 2, sxOeste};
        int[] sombraY = {syNorte + 2, syLeste + 3, sySul + 3, syOeste + 2};
        g.fillPolygon(sombraX, sombraY, 4);

        // 2. Encosto traseiro (quando está atrás do assento na projeção: SUL ou LESTE)
        boolean encostoAtras = (orientation == Orientation.SUL || orientation == Orientation.LESTE);

        // Pernas traseiras
        g.setColor(new Color(165, 128, 42));
        g.setStroke(new BasicStroke(1.8f));
        g.drawLine(sxNorte, syNorte, sxNorte, syNorte - alturaAssento);
        g.drawLine(sxOeste, syOeste, sxOeste, syOeste - alturaAssento);
        g.drawLine(sxLeste, syLeste, sxLeste, syLeste - alturaAssento);

        if (encostoAtras) {
            desenharEncosto(g, sxNorte, syNorte, sxLeste, syLeste, sxSul, sySul, sxOeste, syOeste,
                    alturaAssento, alturaEncosto);
        }

        // 3. Perna frontal (Sul)
        g.setColor(new Color(205, 165, 58));
        g.setStroke(new BasicStroke(1.8f));
        g.drawLine(sxSul, sySul, sxSul, sySul - alturaAssento);

        // 4. Assento estofado em veludo bordô com filete dourado
        int[] assentoX = {sxNorte, sxLeste, sxSul, sxOeste};
        int[] assentoY = {
                syNorte - alturaAssento,
                syLeste - alturaAssento,
                sySul - alturaAssento,
                syOeste - alturaAssento
        };
        g.setColor(new Color(142, 24, 34));
        g.fillPolygon(assentoX, assentoY, 4);

        // Reflexo suave no centro do estofado
        int midX = (sxNorte + sxSul) / 2;
        int midY = (syNorte + sySul) / 2 - alturaAssento;
        g.setColor(new Color(178, 42, 54, 130));
        g.fillOval(midX - 5, midY - 3, 10, 6);

        g.setColor(new Color(216, 178, 68));
        g.setStroke(new BasicStroke(1.2f));
        g.drawPolygon(assentoX, assentoY, 4);

        // 5. Encosto frontal (quando a cadeira olha para cima/esquerda: NORTE ou OESTE)
        if (!encostoAtras) {
            desenharEncosto(g, sxNorte, syNorte, sxLeste, syLeste, sxSul, sySul, sxOeste, syOeste,
                    alturaAssento, alturaEncosto);
        }

        g.setStroke(new BasicStroke(1.0f));
    }

    private void desenharEncosto(Graphics2D g,
                                 int sxNorte, int syNorte,
                                 int sxLeste, int syLeste,
                                 int sxSul, int sySul,
                                 int sxOeste, int syOeste,
                                 int alturaAssento, int alturaEncosto) {
        int x1;
        int y1;
        int x2;
        int y2;
        Color corEncosto;

        switch (orientation) {
            case SUL:
                // Olha para +Y -> encosto na borda Norte-Leste (-Y)
                x1 = sxNorte;
                y1 = syNorte;
                x2 = sxLeste;
                y2 = syLeste;
                corEncosto = new Color(108, 16, 24);
                break;
            case LESTE:
                // Olha para +X -> encosto na borda Norte-Oeste (-X)
                x1 = sxNorte;
                y1 = syNorte;
                x2 = sxOeste;
                y2 = syOeste;
                corEncosto = new Color(96, 14, 22);
                break;
            case NORTE:
                // Olha para -Y -> encosto na borda Oeste-Sul (+Y)
                x1 = sxOeste;
                y1 = syOeste;
                x2 = sxSul;
                y2 = sySul;
                corEncosto = new Color(122, 20, 30);
                break;
            case OESTE:
            default:
                // Olha para -X -> encosto na borda Leste-Sul (+X)
                x1 = sxLeste;
                y1 = syLeste;
                x2 = sxSul;
                y2 = sySul;
                corEncosto = new Color(115, 18, 28);
                break;
        }

        int[] encostoX = {x1, x2, x2, x1};
        int[] encostoY = {
                y1 - alturaAssento,
                y2 - alturaAssento,
                y2 - alturaEncosto,
                y1 - alturaEncosto
        };

        g.setColor(corEncosto);
        g.fillPolygon(encostoX, encostoY, 4);

        // Detalhe vertical acolchoado central
        g.setColor(new Color(168, 38, 48, 170));
        int midX = (x1 + x2) / 2;
        int midYBase = (y1 + y2) / 2 - alturaAssento - 1;
        int midYTopo = (y1 + y2) / 2 - alturaEncosto + 3;
        g.drawLine(midX, midYBase, midX, midYTopo);

        // Moldura dourada do encosto
        g.setColor(new Color(228, 190, 82));
        g.setStroke(new BasicStroke(1.2f));
        g.drawPolygon(encostoX, encostoY, 4);
    }
}
