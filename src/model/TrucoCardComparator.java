package model;

import java.util.Comparator;
import java.util.Objects;

/**
 * Comparador de cartas para o Truco Paulista.
 *
 * Utiliza uma carta vira fixa como contexto para determinar a forca relativa
 * de duas cartas, respeitando manilhas, naipes dos quatro tombos e valores comuns.
 */
public class TrucoCardComparator implements Comparator<Card> {

    private final Card vira;

    public TrucoCardComparator(Card vira) {
        if (vira == null) {
            throw new IllegalArgumentException("A carta vira nao pode ser nula para o comparador de Truco.");
        }
        this.vira = vira;
    }

    public Card getVira() {
        return vira;
    }

    @Override
    public int compare(Card c1, Card c2) {
        return TrucoRules.compararCartas(c1, c2, vira);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrucoCardComparator that = (TrucoCardComparator) o;
        return Objects.equals(vira, that.vira);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vira);
    }
}
