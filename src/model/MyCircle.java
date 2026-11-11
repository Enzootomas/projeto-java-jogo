package model;

import java.awt.Color;

/**
 * Representa uma forma geométrica circular.
 *
 * Pilares de POO demonstrados:
 * - Herança: estende MyEllipse, reaproveitando a lógica de desenho de elipses (draw),
 *   bounding box (intersects) e movimento (move).
 * - Encapsulamento & Invariante de Classe: sobrescreve setWidth e setHeight
 *   garantindo que qualquer alteração de dimensão mantenha os eixos horizontal e
 *   vertical rigorosamente iguais ao diâmetro, preservando o invariante geométrico do círculo.
 */
public class MyCircle extends MyEllipse {

    /**
     * Construtor aleatório dentro dos limites da tela informada.
     * Harmoniza largura e altura para o mesmo diâmetro após instanciação da superclasse.
     *
     * @param screenWidth  largura da tela
     * @param screenHeight altura da tela
     */
    public MyCircle(int screenWidth, int screenHeight) {
        super(screenWidth, screenHeight);
        int diameter = Math.min(this.width, this.height);
        this.width = diameter;
        this.height = diameter;
    }

    /**
     * Construtor com posição e diâmetro.
     *
     * @param x        coordenada X inicial
     * @param y        coordenada Y inicial
     * @param diameter diâmetro do círculo (deve ser positivo)
     */
    public MyCircle(int x, int y, int diameter) {
        super(x, y, diameter, diameter);
    }

    /**
     * Construtor completo com posição, diâmetro e cor.
     *
     * @param x        coordenada X inicial
     * @param y        coordenada Y inicial
     * @param diameter diâmetro do círculo (deve ser positivo)
     * @param color    cor de preenchimento
     */
    public MyCircle(int x, int y, int diameter, Color color) {
        super(x, y, diameter, diameter, color);
    }

    /**
     * Ajusta o diâmetro horizontal e sincroniza o diâmetro vertical.
     *
     * @param width novo diâmetro (deve ser positivo)
     */
    @Override
    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("O diâmetro do círculo deve ser positivo (valor: " + width + ").");
        }
        this.width = width;
        this.height = width;
    }

    /**
     * Ajusta o diâmetro vertical e sincroniza o diâmetro horizontal.
     *
     * @param height novo diâmetro (deve ser positivo)
     */
    @Override
    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("O diâmetro do círculo deve ser positivo (valor: " + height + ").");
        }
        this.width = height;
        this.height = height;
    }
}
