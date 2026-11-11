package model;

/**
 * Utilitário de projeção isométrica 2:1 e sua transformação inversa.
 *
 * Convenção matemática documentada:
 * - O plano do chão (mundo) é representado em coordenadas cartesianas contínuas (worldX, worldY).
 * - A escala é definida de modo que 1 tile lógico tenha TILE_SIZE_WORLD = 32 pixels no mundo.
 * - Na tela, 1 tile isométrico possui TILE_W = 64 e TILE_H = 32 pixels (relação exata 2:1, TILE_W = 2 * TILE_H).
 *
 * Equações de projeção direta (Mundo -> Tela):
 *   tileX = worldX / 32.0
 *   tileY = worldY / 32.0
 *   screenX = (tileX - tileY) * (TILE_W / 2.0) + originX
 *           = (worldX - worldY) * (64 / 64) + originX
 *           = (worldX - worldY) + originX
 *
 *   screenY = (tileX + tileY) * (TILE_H / 2.0) + originY
 *           = (worldX + worldY) * (32 / 64) + originY
 *           = (worldX + worldY) * 0.5 + originY
 *
 * Equações de projeção inversa (Tela -> Mundo):
 *   deltaScreenX = screenX - originX = worldX - worldY
 *   deltaScreenY = screenY - originY = (worldX + worldY) * 0.5
 *
 *   worldX = deltaScreenY + (deltaScreenX * 0.5)
 *   worldY = deltaScreenY - (deltaScreenX * 0.5)
 */
public final class IsometricHelper {

    public static final int TILE_W = 64;
    public static final int TILE_H = 32;
    public static final double TILE_SIZE_WORLD = 32.0;

    private IsometricHelper() {
        // Construtor privado para classe utilitária
    }

    /**
     * Converte coordenada X de mundo para X de tela.
     */
    public static double toScreenX(double worldX, double worldY, double originX) {
        return (worldX - worldY) + originX;
    }

    /**
     * Converte coordenada Y de mundo para Y de tela.
     */
    public static double toScreenY(double worldX, double worldY, double originY) {
        return (worldX + worldY) * 0.5 + originY;
    }

    /**
     * Converte deslocamento de tela (deltaScreenX, deltaScreenY) para deslocamento X no mundo.
     */
    public static double toWorldX(double deltaScreenX, double deltaScreenY) {
        return deltaScreenY + (deltaScreenX * 0.5);
    }

    /**
     * Converte deslocamento de tela (deltaScreenX, deltaScreenY) para deslocamento Y no mundo.
     */
    public static double toWorldY(double deltaScreenX, double deltaScreenY) {
        return deltaScreenY - (deltaScreenX * 0.5);
    }

    /**
     * Calcula o valor de profundidade Z de uma base no chão para o Z-Sorting.
     * Utiliza o ponto de apoio (ancoragem) no solo (centro da base do footprint).
     *
     * @param worldX  coordenada X da base
     * @param worldY  coordenada Y da base
     * @param width   largura da base no chão
     * @param height  profundidade da base no chão
     * @param originY deslocamento vertical da origem da tela
     * @return profundidade Z em pixels de tela
     */
    public static double getDepthZ(double worldX, double worldY, double width, double height, double originY) {
        double anchorX = worldX + width * 0.5;
        double anchorY = worldY + height * 0.5;
        return toScreenY(anchorX, anchorY, originY);
    }
}
