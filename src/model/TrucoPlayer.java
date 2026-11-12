package model;

/**
 * Identifica os participantes da partida de Truco 1 contra 1.
 */
public enum TrucoPlayer {
    JOGADOR("Jogador"),
    BOT("Adversário (Bot)");

    private final String nomeEmPortugues;

    TrucoPlayer(String nomeEmPortugues) {
        this.nomeEmPortugues = nomeEmPortugues;
    }

    public String getNomeEmPortugues() {
        return nomeEmPortugues;
    }

    public TrucoPlayer getAdversario() {
        return this == JOGADOR ? BOT : JOGADOR;
    }

    @Override
    public String toString() {
        return nomeEmPortugues;
    }
}
