package model;

import java.util.List;

/**
 * Contrato oficial entre a Interface Grafica (View) e o Motor de Regras do Blackjack (Model).
 *
 * <p><strong>Regras Financeiras e Apostas:</strong></p>
 * <ul>
 *   <li>Saldo e apostas utilizam o tipo {@code long} para suportar grandes volumes de fichas virtuais.</li>
 *   <li>Para viabilizar o pagamento padrao de Blackjack 3:2 sem arredondamento fracionario em numeros
 *       inteiros, {@link #iniciarPartida(long)} <strong>aceita exclusivamente apostas de valor par</strong>
 *       ({@code valorAposta % 2 == 0}). Apostas impares ou menores/iguais a zero devem ser rejeitadas.</li>
 *   <li>Garantia de liquidacao unica: O saldo do jogador deve ser debitado no inicio da rodada e
 *       o retorno financeiro (aposta + lucro) deve ser creditado <strong>exatamente uma unica vez</strong>
 *       quando o estado transitar para {@link RoundState#FINALIZADA}.</li>
 * </ul>
 *
 * <p><strong>Regras de Visibilidade e Ocultacao de Cartas:</strong></p>
 * <ul>
 *   <li>Durante o {@link RoundState#TURNO_JOGADOR}, a segunda carta do dealer permanece virada para baixo.</li>
 *   <li>O metodo {@link #getCartasVisiveisDealer()} <strong>nunca vaza</strong> a carta oculta do dealer
 *       enquanto o turno do jogador estiver ativo, retornando uma lista com apenas a primeira carta visivel.</li>
 *   <li>A presenca da carta virada para baixo e sinalizada por {@link #hasCartaOcultaDealer()}.</li>
 *   <li>Quando o estado passar para {@link RoundState#FINALIZADA}, {@link #getCartasVisiveisDealer()}
 *       retorna todas as cartas do dealer e {@link #hasCartaOcultaDealer()} retorna {@code false}.</li>
 *   <li>Todas as listas de cartas retornadas devem ser <strong>copias imutaveis</strong>
 *       ({@link java.util.Collections#unmodifiableList(List)}), impedindo modificacoes externas na colecao interna.</li>
 * </ul>
 *
 * <p><strong>Matriz de Acoes Permitidas por Estado:</strong></p>
 * <table border="1">
 *   <tr><th>Estado</th><th>iniciarPartida(aposta)</th><th>pedirCarta() [Hit]</th><th>parar() [Stand]</th></tr>
 *   <tr><td>{@link RoundState#AGUARDANDO_APOSTA}</td><td>Permitido</td><td>Proibido (retorna false)</td><td>Proibido (sem efeito)</td></tr>
 *   <tr><td>{@link RoundState#TURNO_JOGADOR}</td><td>Proibido (retorna false)</td><td>Permitido</td><td>Permitido</td></tr>
 *   <tr><td>{@link RoundState#FINALIZADA}</td><td>Permitido (inicia nova)</td><td>Proibido (retorna false)</td><td>Proibido (sem efeito)</td></tr>
 * </table>
 */
public interface BlackjackEngine {

    // =========================================================================
    // ACOES DO JOGADOR
    // =========================================================================

    /**
     * Inicia uma nova rodada de Blackjack debitando a aposta do saldo do jogador.
     *
     * <p>Pre-requisitos obrigatorios para sucesso:</p>
     * <ul>
     *   <li>O estado atual deve ser {@link RoundState#AGUARDANDO_APOSTA} ou {@link RoundState#FINALIZADA}.</li>
     *   <li>{@code valorAposta > 0}.</li>
     *   <li>{@code valorAposta <= getSaldoVirtual()}.</li>
     *   <li>{@code valorAposta % 2 == 0} (valor obrigatoriamente par para viabilizar pagamento 3:2 inteiro).</li>
     * </ul>
     *
     * @param valorAposta valor em fichas virtuais (deve ser positivo, par e <= saldo).
     * @return {@code true} se a rodada foi iniciada com sucesso; {@code false} caso contrario.
     */
    boolean iniciarPartida(long valorAposta);

    /**
     * Solicita uma carta adicional para a mao do jogador (acao "Hit").
     *
     * <p>Permitido exclusivamente quando o estado for {@link RoundState#TURNO_JOGADOR}.
     * Se apos receber a carta a pontuacao do jogador exceder 21, o jogador "estoura" (Bust),
     * a rodada encerra imediatamente e o estado transita para {@link RoundState#FINALIZADA}.</p>
     *
     * @return {@code true} se a carta foi entregue; {@code false} se a acao nao for permitida no momento.
     */
    boolean pedirCarta();

    /**
     * Encerra o turno do jogador com as cartas atuais (acao "Stand").
     *
     * <p>Permitido exclusivamente quando o estado for {@link RoundState#TURNO_JOGADOR}.
     * Dispara a revelacao da carta oculta do dealer e o turno automatico da banca
     * (o dealer compra cartas ate atingir pontuacao >= 17). Ao termino, o resultado
     * e apurado e o estado transita para {@link RoundState#FINALIZADA}.</p>
     */
    void parar();

    // =========================================================================
    // CONSULTAS DE ESTADO E CARTEIRA (VIEW)
    // =========================================================================

    /**
     * Retorna o saldo atual de fichas virtuais do jogador.
     *
     * @return saldo disponivel em fichas.
     */
    long getSaldoVirtual();

    /**
     * Retorna o valor apostado na rodada em andamento ou na rodada recem-finalizada.
     *
     * @return valor da aposta atual em fichas virtuais.
     */
    long getApostaAtual();

    /**
     * Retorna o estado atual do ciclo de vida da rodada.
     *
     * @return {@link RoundState#AGUARDANDO_APOSTA}, {@link RoundState#TURNO_JOGADOR} ou {@link RoundState#FINALIZADA}.
     */
    RoundState getEstadoRodada();

    /**
     * Retorna o resultado da rodada.
     *
     * @return {@link RoundResult#EM_ANDAMENTO} se a rodada nao terminou, ou o desfecho especifico.
     */
    RoundResult getResultadoRodada();

    // =========================================================================
    // CONSULTAS DE CARTAS E PONTUACAO (VIEW)
    // =========================================================================

    /**
     * Retorna uma lista imutavel com todas as cartas atualmente na mao do jogador.
     *
     * @return lista imutavel de cartas do jogador (nunca nula).
     */
    List<Card> getMaoJogador();

    /**
     * Retorna uma lista imutavel contendo exclusivamente as cartas do dealer que sao
     * visiveis para o jogador no momento.
     *
     * <p>Enquanto o estado for {@link RoundState#TURNO_JOGADOR}, esta lista contera
     * apenas 1 carta (a carta aberta). Apos a finalizacao da rodada, contera todas as cartas
     * do dealer (incluindo a carta que estava oculta e as eventuais cartas compradas).</p>
     *
     * @return lista imutavel das cartas visiveis da banca (nunca nula).
     */
    List<Card> getCartasVisiveisDealer();

    /**
     * Informa se o dealer ainda possui uma carta virada para baixo na mesa.
     *
     * @return {@code true} se ha carta oculta na mesa (durante {@link RoundState#TURNO_JOGADOR});
     *         {@code false} caso contrario.
     */
    boolean hasCartaOcultaDealer();

    /**
     * Retorna a pontuacao atual calculada da mao do jogador.
     * As cartas de figura valem 10, numericas valem seu numero e o As vale 11 ou 1.
     *
     * @return pontuacao inteira da mao do jogador.
     */
    int getPontosJogador();

    /**
     * Retorna a soma de pontos apenas das cartas visiveis do dealer.
     * Durante o turno do jogador, reflete apenas o valor da primeira carta.
     * Apos a finalizacao, reflete a pontuacao total do dealer.
     *
     * @return pontuacao visivel do dealer.
     */
    int getPontosDealerVisiveis();
}
