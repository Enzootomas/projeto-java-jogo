package model;

/**
 * Resultado individual de uma vaza (trick) de Truco.
 */
public enum TrucoTrickResult {
    VITORIA_JOGADOR("Vitória do Jogador"),
    VITORIA_BOT("Vitória do Bot"),
    EMPATE("Empate (Canga)");

    private final String descricao;

    TrucoTrickResult(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
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
