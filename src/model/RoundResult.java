package model;

/**
 * Resultados possiveis de uma rodada de Blackjack.
 *
 * A logica de pagamento e definida por:
 * - JOGADOR_BLACKJACK: Paga 3:2 sobre a aposta (retorna a aposta + (aposta * 3 / 2)).
 *   Exige que a aposta seja par para divisao inteira exata sem truncamento.
 * - JOGADOR_VITORIA: Paga 1:1 sobre a aposta (retorna a aposta + aposta).
 * - EMPATE_PUSH: Devolve a aposta original integralmente (retorna a aposta).
 * - JOGADOR_ESTOUROU / DEALER_VITORIA / DEALER_BLACKJACK: O jogador perde a aposta (retorno 0).
 */
public enum RoundResult {
    EM_ANDAMENTO("Partida em andamento", false),
    JOGADOR_BLACKJACK("Blackjack natural do jogador!", true),
    JOGADOR_VITORIA("Jogador venceu a rodada!", true),
    EMPATE_PUSH("Empate (Push)! Fichas devolvidas.", false),
    JOGADOR_ESTOUROU("Jogador estourou (Bust)! Mais de 21 pontos.", false),
    DEALER_VITORIA("Dealer venceu a rodada.", false),
    DEALER_BLACKJACK("Dealer possui Blackjack natural.", false);

    private final String descricaoPadrao;
    private final boolean vitoriaJogador;

    RoundResult(String descricaoPadrao, boolean vitoriaJogador) {
        this.descricaoPadrao = descricaoPadrao;
        this.vitoriaJogador = vitoriaJogador;
    }

    public String getDescricaoPadrao() {
        return descricaoPadrao;
    }

    public boolean isVitoriaJogador() {
        return vitoriaJogador;
    }

    public boolean isEmpate() {
        return this == EMPATE_PUSH;
    }

    /**
     * Calcula o valor total a ser creditado ao saldo do jogador ao final da rodada
     * (devolucao da aposta inicial + lucro da vitoria, se houver).
     *
     * @param aposta valor da aposta (deve ser par).
     * @return montante total de fichas creditadas (0 em caso de derrota).
     */
    public long calcularRetornoTotal(long aposta) {
        if (aposta <= 0) {
            return 0L;
        }
        switch (this) {
            case JOGADOR_BLACKJACK:
                // Pagamento 3:2: devolve a aposta + lucro de (aposta * 3 / 2).
                // Como a aposta e garantidamente par, (aposta * 3) e par e a divisao por 2 e exata.
                return aposta + ((aposta * 3L) / 2L);
            case JOGADOR_VITORIA:
                // Pagamento 1:1: devolve a aposta + lucro igual a aposta.
                return aposta + aposta;
            case EMPATE_PUSH:
                // Devolve a aposta original.
                return aposta;
            case JOGADOR_ESTOUROU:
            case DEALER_VITORIA:
            case DEALER_BLACKJACK:
            case EM_ANDAMENTO:
            default:
                // Perda da aposta ou rodada ainda nao concluida.
                return 0L;
        }
    }
}
