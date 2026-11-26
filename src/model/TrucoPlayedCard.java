package model;

import java.util.Objects;

/**
 * Representa de forma imutavel uma carta jogada por um participante em uma vaza de Truco.
 */
public final class TrucoPlayedCard {

    private final int seatIndex;
    private final String nomeParticipante;
    private final TrucoTeam equipe;
    private final Card carta;

    public TrucoPlayedCard(int seatIndex, String nomeParticipante, TrucoTeam equipe, Card carta) {
        this.seatIndex = seatIndex;
        this.nomeParticipante = Objects.requireNonNull(nomeParticipante, "Nome do participante nao pode ser nulo.");
        this.equipe = Objects.requireNonNull(equipe, "Equipe nao pode ser nula.");
        this.carta = Objects.requireNonNull(carta, "Carta jogada nao pode ser nula.");
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public String getNomeParticipante() {
        return nomeParticipante;
    }

    public TrucoTeam getEquipe() {
        return equipe;
    }

    public Card getCarta() {
        return carta;
    }
}
