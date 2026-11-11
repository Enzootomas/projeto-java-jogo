package model;

import java.awt.Color;

/**
 * Representa uma forma geométrica quadrada.
 *
 * Pilares de POO demonstrados:
 * - Herança: estende MyRectangle, aproveitando a lógica de desenho (draw),
 *   bounding box (intersects) e movimento (move).
 * - Encapsulamento & Invariante de Classe: sobrescreve setWidth e setHeight
 *   garantindo que qualquer alteração de dimensão mantenha os quatro lados iguais,
 *   preservando o invariante matemático do quadrado.
 */
public class MySquare extends MyRectangle {

    /**
     * Construtor aleatório dentro dos limites da tela informada.
     * Após a geração inicial pela superclasse, harmoniza largura e altura
     * para o mesmo valor, garantindo o formato estritamente quadrado.
     *
     * @param screenWidth  largura da tela
     * @param screenHeight altura da tela
     */
    public MySquare(int screenWidth, int screenHeight) {
        super(screenWidth, screenHeight);
        int size = Math.min(this.width, this.height);
        this.width = size;
        this.height = size;
    }

    /**
     * Construtor com posição e tamanho do lado.
     *
     * @param x    coordenada X inicial
     * @param y    coordenada Y inicial
     * @param size tamanho do lado (deve ser positivo)
     */
    public MySquare(int x, int y, int size) {
        super(x, y, size, size);
    }

    /**
     * Construtor completo com posição, tamanho do lado e cor.
     *
     * @param x     coordenada X inicial
     * @param y     coordenada Y inicial
     * @param size  tamanho do lado (deve ser positivo)
     * @param color cor de preenchimento
     */
    public MySquare(int x, int y, int size, Color color) {
        super(x, y, size, size, color);
    }

    /**
     * Ajusta a largura e sincroniza automaticamente a altura para manter lados iguais.
     *
     * @param width novo valor do lado (deve ser positivo)
     */
    @Override
    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("A dimensão do quadrado deve ser positiva (valor: " + width + ").");
        }
        this.width = width;
        this.height = width;
    }

    /**
     * Ajusta a altura e sincroniza automaticamente a largura para manter lados iguais.
     *
     * @param height novo valor do lado (deve ser positivo)
     */
    @Override
    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("A dimensão do quadrado deve ser positiva (valor: " + height + ").");
        }
        this.width = height;
        this.height = height;
    }
}
