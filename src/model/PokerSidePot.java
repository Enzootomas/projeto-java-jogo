package model;

import java.util.List;
import java.util.Objects;

/**
 * Representa de forma imutavel um pote (Pote Principal ou Pote Paralelo / Side Pot)
 * em uma partida de Poker Texas Hold'em com multiplos participantes.
 */
public final class PokerSidePot {

    private final String rotulo;
    private final long valor;
    private final List<Integer> seatsElegiveis;

    public PokerSidePot(String rotulo, long valor, List<Integer> seatsElegiveis) {
        if (valor < 0L) {
            throw new IllegalArgumentException("Valor do pote nao pode ser negativo: " + valor);
        }
        this.rotulo = Objects.requireNonNull(rotulo, "Rotulo do pote nao pode ser nulo.");
        this.valor = valor;
        this.seatsElegiveis = List.copyOf(Objects.requireNonNull(seatsElegiveis));
    }

    public String getRotulo() {
        return rotulo;
    }

    public long getValor() {
        return valor;
    }

    public List<Integer> getSeatsElegiveis() {
        return seatsElegiveis;
    }

    @Override
    public String toString() {
        return rotulo + ": " + valor + " (elegiveis: " + seatsElegiveis + ")";
    }
}
