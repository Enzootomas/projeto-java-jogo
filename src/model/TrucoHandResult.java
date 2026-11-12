package model;

/**
 * Resultado final de uma mão de Truco (disputada em melhor de até 3 vazas).
 */
public enum TrucoHandResult {
    EM_ANDAMENTO("Mão em andamento"),
    VITORIA_JOGADOR("Jogador venceu a mão"),
    VITORIA_BOT("Bot venceu a mão"),
    EMPATE("Mão empatada (três cangas)");

    private final String descricao;

    TrucoHandResult(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isFinalizada() {
        return this != EM_ANDAMENTO;
    }

    public boolean isVitoriaJogador() {
        return this == VITORIA_JOGADOR;
    }

    public boolean isVitoriaBot() {
        return this == VITORIA_BOT;
    }

    public boolean isEmpate() {
        return this == EMPATE;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
