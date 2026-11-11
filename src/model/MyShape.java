package model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * Classe abstrata base para todas as formas geométricas do jogo.
 *
 * Pilares de POO demonstrados:
 * - Abstração: define o modelo conceitual e o contrato draw(Graphics2D g) que
 *   toda forma visual concreta deve implementar. Não pode ser instanciada diretamente.
 * - Encapsulamento: atributos protegidos com validação de dimensões positivas
 *   em getters/setters e inicialização segura no construtor sem invocar métodos
 *   sobrescrevíveis.
 */
public abstract class MyShape {
    protected int x;
    protected int y;
    protected int speedX;
    protected int speedY;
    protected int width;
    protected int height;
    protected Color color;
    protected static Random random = new Random();

    /**
     * Construtor aleatório.
     * Gera dimensões e posições válidas dentro dos limites da tela informados,
     * velocidade não nula e cor aleatória.
     *
     * @param screenWidth  largura da tela
     * @param screenHeight altura da tela
     */
    public MyShape(int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("As dimensões da tela devem ser positivas.");
        }

        // Define dimensões aleatórias razoáveis (entre 20 e 80 pixels, ou fração da tela)
        int maxDim = Math.max(20, Math.min(screenWidth, screenHeight) / 5);
        int minDim = Math.min(20, maxDim);
        this.width = minDim + random.nextInt(Math.max(1, maxDim - minDim + 1));
        this.height = minDim + random.nextInt(Math.max(1, maxDim - minDim + 1));

        // Posição inicial contida na tela
        int maxX = Math.max(0, screenWidth - this.width);
        int maxY = Math.max(0, screenHeight - this.height);
        this.x = maxX > 0 ? random.nextInt(maxX) : 0;
        this.y = maxY > 0 ? random.nextInt(maxY) : 0;

        // Velocidades aleatórias entre -4 e 4 (evitando repouso em ambos os eixos)
        this.speedX = random.nextInt(9) - 4;
        this.speedY = random.nextInt(9) - 4;
        if (this.speedX == 0 && this.speedY == 0) {
            this.speedX = 2;
            this.speedY = 2;
        }

        // Cor aleatória opaca
        this.color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    /**
     * Construtor com coordenadas e dimensões explícitas.
     * Utiliza cor preta e velocidade zero como padrão.
     *
     * @param x      coordenada X inicial
     * @param y      coordenada Y inicial
     * @param width  largura (deve ser positiva)
     * @param height altura (deve ser positiva)
     */
    public MyShape(int x, int y, int width, int height) {
        this(x, y, width, height, Color.BLACK);
    }

    /**
     * Construtor completo com coordenadas, dimensões e cor.
     *
     * Decisão didática de projeto: para evitar chamadas a métodos sobrescrevíveis
     * durante a instanciação (o que violaria boas práticas e poderia causar
     * inconsistência em subclasses como MySquare/MyCircle), a validação e atribuição
     * direta são realizadas aqui sem chamar setters.
     *
     * @param x      coordenada X inicial
     * @param y      coordenada Y inicial
     * @param width  largura (deve ser positiva)
     * @param height altura (deve ser positiva)
     * @param color  cor da forma (padrão Color.BLACK se nulo)
     */
    public MyShape(int x, int y, int width, int height, Color color) {
        validateDimension(width, "largura");
        validateDimension(height, "altura");

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = (color != null) ? color : Color.BLACK;
        this.speedX = 0;
        this.speedY = 0;
    }

    /**
     * Valida se uma dimensão é estritamente positiva.
     */
    private static void validateDimension(int value, String dimensionName) {
        if (value <= 0) {
            throw new IllegalArgumentException("A dimensão '" + dimensionName + "' deve ser positiva (valor recebido: " + value + ").");
        }
    }

    /**
     * Contrato abstrato de renderização.
     * Cada subclasse concreta é obrigada a implementar a sua própria forma de desenho.
     *
     * @param g contexto gráfico 2D para renderização
     */
    public abstract void draw(Graphics2D g);

    /**
     * Atualiza a posição da forma baseando-se em suas velocidades speedX e speedY,
     * invertendo a direção (quique) ao atingir as bordas da tela.
     *
     * @param screenWidth  largura atual da área visível
     * @param screenHeight altura atual da área visível
     */
    public void move(int screenWidth, int screenHeight) {
        this.x += this.speedX;
        this.y += this.speedY;

        // Colisão com borda esquerda
        if (this.x < 0) {
            this.x = 0;
            this.speedX = -this.speedX;
        }
        // Colisão com borda direita
        else if (screenWidth > this.width && this.x + this.width > screenWidth) {
            this.x = screenWidth - this.width;
            this.speedX = -this.speedX;
        }

        // Colisão com borda superior
        if (this.y < 0) {
            this.y = 0;
            this.speedY = -this.speedY;
        }
        // Colisão com borda inferior
        else if (screenHeight > this.height && this.y + this.height > screenHeight) {
            this.y = screenHeight - this.height;
            this.speedY = -this.speedY;
        }
    }

    /**
     * Verifica colisão entre duas formas utilizando o algoritmo de Bounding Box (AABB).
     *
     * Decisão de implementação para bordas encostadas:
     * Considera-se colisão SOMENTE quando há sobreposição efetiva de área (área de interseção > 0).
     * Portanto, se duas formas estiverem apenas se tocando pelas bordas externas (tangentes,
     * onde this.x + this.width == other.x), o método retorna FALSE.
     *
     * @param other outra forma a ser comparada
     * @return true se houver sobreposição de área; false caso contrário ou se other for nulo
     */
    public boolean intersects(MyShape other) {
        if (other == null) {
            return false;
        }
        return this.x < other.x + other.width &&
               this.x + this.width > other.x &&
               this.y < other.y + other.height &&
               this.y + this.height > other.y;
    }

    // --- Getters e Setters com encapsulamento ---

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        validateDimension(width, "largura");
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        validateDimension(height, "altura");
        this.height = height;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = (color != null) ? color : Color.BLACK;
    }

    public int getSpeedX() {
        return speedX;
    }

    public void setSpeedX(int speedX) {
        this.speedX = speedX;
    }

    public int getSpeedY() {
        return speedY;
    }

    public void setSpeedY(int speedY) {
        this.speedY = speedY;
    }
}
