package model;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Representa uma forma geométrica retangular.
 *
 * Pilares de POO demonstrados:
 * - Herança: estende MyShape, reutilizando estado (x, y, width, height, velocidades, cor)
 *   e comportamentos concretos (move, intersects).
 * - Polimorfismo: implementa a operação abstrata draw(Graphics2D g) preenchendo
 *   um retângulo com a cor configurada.
 */
public class MyRectangle extends MyShape {

    /**
     * Construtor aleatório dentro dos limites da tela informada.
     *
     * @param screenWidth  largura da tela
     * @param screenHeight altura da tela
     */
    public MyRectangle(int screenWidth, int screenHeight) {
        super(screenWidth, screenHeight);
    }

    /**
     * Construtor com coordenadas e dimensões explícitas.
     *
     * @param x      coordenada X inicial
     * @param y      coordenada Y inicial
     * @param width  largura do retângulo
     * @param height altura do retângulo
     */
    public MyRectangle(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Construtor completo com coordenadas, dimensões e cor.
     *
     * @param x      coordenada X inicial
     * @param y      coordenada Y inicial
     * @param width  largura do retângulo
     * @param height altura do retângulo
     * @param color  cor de preenchimento
     */
    public MyRectangle(int x, int y, int width, int height, Color color) {
        super(x, y, width, height, color);
    }

    /**
     * Renderiza o retângulo no contexto gráfico fornecido.
     * Sobrescrita polimórfica de MyShape.draw.
     *
     * @param g contexto gráfico 2D
     */
    @Override
    public void draw(Graphics2D g) {
        g.setColor(this.color);
        g.fillRect(this.x, this.y, this.width, this.height);
    }
}
