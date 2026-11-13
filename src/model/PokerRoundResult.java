package model;

/**
 * Resultados possiveis para o desfecho de uma mao de Poker Texas Hold'em.
 */
public enum PokerRoundResult {
    AGUARDANDO_INICIO("Aguardando início da mão"),
    EM_ANDAMENTO("Mão em andamento"),
    JOGADOR_VENCEU_DESISTENCIA("Jogador venceu (Bot desistiu)"),
    BOT_VENCEU_DESISTENCIA("Bot venceu (Jogador desistiu)"),
    JOGADOR_VENCEU_SHOWDOWN("Jogador venceu no Showdown!"),
    BOT_VENCEU_SHOWDOWN("Bot venceu no Showdown."),
    EMPATE_DIVISAO_POTE("Empate no Showdown! Pote dividido (Split Pot).");

    private final String descricaoPadrao;

    PokerRoundResult(String descricaoPadrao) {
        this.descricaoPadrao = descricaoPadrao;
    }

    public String getDescricaoPadrao() {
        return descricaoPadrao;
    }

    public boolean isFinalizado() {
        return this != AGUARDANDO_INICIO && this != EM_ANDAMENTO;
    }

    public boolean isShowdown() {
        return this == JOGADOR_VENCEU_SHOWDOWN
                || this == BOT_VENCEU_SHOWDOWN
                || this == EMPATE_DIVISAO_POTE;
    }
}
