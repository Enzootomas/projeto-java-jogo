package model;

/**
 * Estagios (ruas / streets) do ciclo de vida de uma mao de Poker Texas Hold'em.
 */
public enum PokerStage {
    /** Estado inicial antes da primeira distribuicao de cartas. */
    AGUARDANDO_MAO("Aguardando nova mão", false),

    /** Rodada inicial de apostas com 2 cartas fechadas para cada jogador e 0 comunitarias. */
    PRE_FLOP("Pré-Flop", true),

    /** Segunda rodada de apostas apos a abertura das 3 primeiras cartas comunitarias. */
    FLOP("Flop", true),

    /** Terceira rodada de apostas apos a abertura da 4a carta comunitaria. */
    TURN("Turn", true),

    /** Quarta e ultima rodada de apostas apos a abertura da 5a carta comunitaria. */
    RIVER("River", true),

    /** Mao encerrada com revelacao de cartas (Showdown) e liquidacao do pote. */
    SHOWDOWN("Showdown", false),

    /** Mao encerrada antes do Showdown por desistencia (Fold) de um dos participantes. */
    ENCERRADA_POR_DESISTENCIA("Encerrada por desistência", false);

    private final String nomeExibicao;
    private final boolean emAndamento;

    PokerStage(String nomeExibicao, boolean emAndamento) {
        this.nomeExibicao = nomeExibicao;
        this.emAndamento = emAndamento;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    /**
     * Indica se a mao ainda esta ativa aguardando acoes de aposta.
     *
     * @return {@code true} para {@code PRE_FLOP}, {@code FLOP}, {@code TURN} e {@code RIVER}.
     */
    public boolean isMaoEmAndamento() {
        return emAndamento;
    }

    /**
     * Indica se a mao ja foi encerrada e liquidada ({@link #SHOWDOWN} ou {@link #ENCERRADA_POR_DESISTENCIA}).
     *
     * @return {@code true} se a mao terminou.
     */
    public boolean isMaoFinalizada() {
        return this == SHOWDOWN || this == ENCERRADA_POR_DESISTENCIA;
    }
}
