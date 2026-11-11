package model;

/**
 * Estados do ciclo de vida de uma rodada de Blackjack.
 *
 * Transicoes validas:
 * AGUARDANDO_APOSTA -> TURNO_JOGADOR (ao chamar iniciarPartida com aposta valida)
 * AGUARDANDO_APOSTA -> FINALIZADA     (caso haja Blackjack natural imediato na distribuicao)
 * TURNO_JOGADOR     -> TURNO_JOGADOR (ao pedir carta sem estourar e sem atingir 21)
 * TURNO_JOGADOR     -> FINALIZADA     (ao pedir carta e estourar, ou ao parar e o dealer concluir seu turno)
 * FINALIZADA        -> TURNO_JOGADOR (ao chamar iniciarPartida para iniciar nova rodada)
 */
public enum RoundState {
    /**
     * Nenhuma partida em andamento. Aguardando definicao da aposta pelo jogador.
     * Acoes permitidas: iniciarPartida(valorAposta).
     */
    AGUARDANDO_APOSTA,

    /**
     * Partida em andamento sob o controle do jogador.
     * O dealer possui 1 carta visivel e 1 carta oculta virada para baixo.
     * Acoes permitidas: pedirCarta() [Hit] ou parar() [Stand].
     */
    TURNO_JOGADOR,

    /**
     * A rodada foi concluida. A carta oculta do dealer foi revelada, a pontuacao final
     * foi apurada e os pagamentos/deducoes foram liquidados EXATAMENTE UMA VEZ.
     * Acoes permitidas: iniciarPartida(valorAposta) para iniciar a proxima rodada.
     */
    FINALIZADA
}
