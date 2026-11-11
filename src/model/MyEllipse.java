package model;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Representa uma forma geométrica elíptica.
 *
 * Pilares de POO demonstrados:
 * - Herança: estende MyShape, reutilizando atributos e métodos comuns.
 * - Polimorfismo: implementa a operação abstrata draw(Graphics2D g) desenhando
 *   uma elipse/óvalo preenchido através de g.fillOval.
 */
public class MyEllipse extends MyShape {

    /**
     * Construtor aleatório dentro dos limites da tela informada.
     *
     * @param screenWidth  largura da tela
     * @param screenHeight altura da tela
     */
    public MyEllipse(int screenWidth, int screenHeight) {
        super(screenWidth, screenHeight);
    }

    /**
     * Construtor com coordenadas e dimensões explícitas.
     *
     * @param x      coordenada X inicial
     * @param y      coordenada Y inicial
     * @param width  largura da elipse (eixo horizontal)
     * @param height altura da elipse (eixo vertical)
     */
    public MyEllipse(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Construtor completo com coordenadas, dimensões e cor.
     *
     * @param x      coordenada X inicial
     * @param y      coordenada Y inicial
     * @param width  largura da elipse
     * @param height altura da elipse
     * @param color  cor de preenchimento
     */
    public MyEllipse(int x, int y, int width, int height, Color color) {
        super(x, y, width, height, color);
    }

    /**
     * Renderiza a elipse no contexto gráfico fornecido.
     * Sobrescrita polimórfica de MyShape.draw.
     *
     * @param g contexto gráfico 2D
     */
    @Override
    public void draw(Graphics2D g) {
        g.setColor(this.color);
        g.fillOval(this.x, this.y, this.width, this.height);
    }
}
