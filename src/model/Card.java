package model;

import java.util.Objects;

/**
 * Representa uma carta de baralho imutavel.
 * Cada carta e definida por seu naipe (Suit) e valor (Rank).
 */
public final class Card {

    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        if (suit == null) {
            throw new IllegalArgumentException("O naipe (Suit) da carta nao pode ser nulo.");
        }
        if (rank == null) {
            throw new IllegalArgumentException("O valor (Rank) da carta nao pode ser nulo.");
        }
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    public int getValorBlackjack() {
        return rank.getValorBaseBlackjack();
    }

    public boolean isAs() {
        return rank.isAs();
    }

    public String getNomeFormatado() {
        return rank.getNomeEmPortugues() + " de " + suit.getNomeEmPortugues();
    }

    public String getSiglaVisual() {
        return rank.getSimbolo() + suit.getSimbolo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return suit == card.suit && rank == card.rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, rank);
    }

    @Override
    public String toString() {
        return getNomeFormatado() + " [" + getSiglaVisual() + "]";
    }
}
