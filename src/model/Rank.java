package model;

/**
 * Representa os treze valores possiveis das cartas no baralho.
 * No Blackjack, as cartas numericas valem seu numero de face,
 * as figuras (Valete, Dama e Rei) valem 10, e o As vale inicialmente
 * 11 ou 1 (conforme a conveniencia da mao).
 */
public enum Rank {
    AS("Ás", "A", 11),
    DOIS("2", "2", 2),
    TRES("3", "3", 3),
    QUATRO("4", "4", 4),
    CINCO("5", "5", 5),
    SEIS("6", "6", 6),
    SETE("7", "7", 7),
    OITO("8", "8", 8),
    NOVE("9", "9", 9),
    DEZ("10", "10", 10),
    VALETE("Valete", "J", 10),
    DAMA("Dama", "Q", 10),
    REI("Rei", "K", 10);

    private final String nomeEmPortugues;
    private final String simbolo;
    private final int valorBaseBlackjack;

    Rank(String nomeEmPortugues, String simbolo, int valorBaseBlackjack) {
        this.nomeEmPortugues = nomeEmPortugues;
        this.simbolo = simbolo;
        this.valorBaseBlackjack = valorBaseBlackjack;
    }

    public String getNomeEmPortugues() {
        return nomeEmPortugues;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public int getValorBaseBlackjack() {
        return valorBaseBlackjack;
    }

    public boolean isAs() {
        return this == AS;
    }

    @Override
    public String toString() {
        return nomeEmPortugues;
    }
}
