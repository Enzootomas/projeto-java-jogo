package model;

/**
 * Identifica os participantes de uma partida Heads-Up (1 vs 1) de Poker Texas Hold'em.
 */
public enum PokerParticipant {
    JOGADOR("Jogador"),
    BOT("Bot");

    private final String rotulo;

    PokerParticipant(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /**
     * Retorna o participante adversario no duelo 1 vs 1.
     *
     * @return {@link #BOT} se este for {@link #JOGADOR}; {@link #JOGADOR} caso contrario.
     */
    public PokerParticipant oponente() {
        return this == JOGADOR ? BOT : JOGADOR;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
