package model;

/**
 * Acoes de aposta disponiveis durante os turnos de uma mao de Poker Texas Hold'em.
 */
public enum PokerAction {
    /** Desiste da mao atual entregando o pote ao adversario sem revelar as cartas fechadas. */
    DESISTIR("Desistir (Fold)"),

    /** Passa a vez sem colocar fichas adicionais (permitido apenas quando nao ha aposta pendente). */
    PASSAR("Passar (Check)"),

    /** Iguala a aposta atual da rua (ou entra em all-in caso o saldo seja menor que a diferenca). */
    PAGAR("Pagar (Call)"),

    /** Abre ou eleva o valor da aposta na rua atual (Bet / Raise). */
    AUMENTAR("Aumentar (Bet/Raise)");

    private final String descricao;

    PokerAction(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
