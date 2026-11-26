package model;

/**
 * Modos de jogo disponiveis na mesa de Truco Paulista.
 */
public enum TrucoMode {
    INDIVIDUAL("Individual (1 vs 1)", 2),
    DUPLAS("Duplas (2 vs 2)", 4);

    private final String nomeExibicao;
    private final int quantidadeParticipantes;

    TrucoMode(String nomeExibicao, int quantidadeParticipantes) {
        this.nomeExibicao = nomeExibicao;
        this.quantidadeParticipantes = quantidadeParticipantes;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public int getQuantidadeParticipantes() {
        return quantidadeParticipantes;
    }
}
