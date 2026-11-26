package model;

/**
 * Acoes de aposta/pedido disponiveis durante uma mao de Truco Paulista.
 */
public enum TrucoBetAction {
    PEDIR_TRUCO("Pedir Truco"),
    ACEITAR("Aceitar"),
    CORRER("Correr"),
    AUMENTAR("Aumentar");

    private final String rotulo;

    TrucoBetAction(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
