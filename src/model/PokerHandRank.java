package model;

/**
 * Categorias oficiais de maos de 5 cartas do Poker Texas Hold'em, ordenadas da menor
 * forca ({@link #CARTA_ALTA}) para a maior ({@link #STRAIGHT_FLUSH}).
 */
public enum PokerHandRank {
    CARTA_ALTA(1, "Carta Alta"),
    UM_PAR(2, "Um Par"),
    DOIS_PARES(3, "Dois Pares"),
    TRINCA(4, "Trinca"),
    SEQUENCIA(5, "Sequência (Straight)"),
    FLUSH(6, "Flush"),
    FULL_HOUSE(7, "Full House"),
    QUADRA(8, "Quadra (Four of a Kind)"),
    STRAIGHT_FLUSH(9, "Straight Flush");

    private final int forcaCategoria;
    private final String nomeEmPortugues;

    PokerHandRank(int forcaCategoria, String nomeEmPortugues) {
        this.forcaCategoria = forcaCategoria;
        this.nomeEmPortugues = nomeEmPortugues;
    }

    public int getForcaCategoria() {
        return forcaCategoria;
    }

    public String getNomeEmPortugues() {
        return nomeEmPortugues;
    }

    @Override
    public String toString() {
        return nomeEmPortugues;
    }
}
