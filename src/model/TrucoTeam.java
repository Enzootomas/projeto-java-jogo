package model;

/**
 * Equipes de uma partida de Truco Paulista (Individual ou Duplas).
 */
public enum TrucoTeam {
    EQUIPE_JOGADOR("Sua Equipe"),
    EQUIPE_ADVERSARIA("Adversários");

    private final String nome;

    TrucoTeam(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public TrucoTeam oponente() {
        return this == EQUIPE_JOGADOR ? EQUIPE_ADVERSARIA : EQUIPE_JOGADOR;
    }
}
