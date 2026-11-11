package model;

/**
 * Representa os quatro naipes tradicionais do baralho frances.
 */
public enum Suit {
    COPAS("Copas", "♥", "Vermelho"),
    ESPADAS("Espadas", "♠", "Preto"),
    OUROS("Ouros", "♦", "Vermelho"),
    PAUS("Paus", "♣", "Preto");

    private final String nomeEmPortugues;
    private final String simbolo;
    private final String cor;

    Suit(String nomeEmPortugues, String simbolo, String cor) {
        this.nomeEmPortugues = nomeEmPortugues;
        this.simbolo = simbolo;
        this.cor = cor;
    }

    public String getNomeEmPortugues() {
        return nomeEmPortugues;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public String getCor() {
        return cor;
    }

    @Override
    public String toString() {
        return nomeEmPortugues + " (" + simbolo + ")";
    }
}
