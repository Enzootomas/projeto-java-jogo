package model;

import java.util.List;
import java.util.Set;

/**
 * Contrato oficial entre a Interface Grafica (View) e o Motor de Regras do Poker Texas Hold'em (Model)
 * no formato Heads-Up (1 Jogador vs. 1 Bot) com fichas 100% virtuais.
 *
 * <p><strong>Regras de Apostas e Estrutura da Versao 1 (Heads-Up Texas Hold'em):</strong></p>
 * <ul>
 *   <li><strong>Saldos e Fichas ({@code long}):</strong> valores representados em inteiros longos com
 *       aritmetica verificada ({@code Math.addExact} / {@code Math.subtractExact}).</li>
 *   <li><strong>Blinds Pares e Posicao Heads-Up:</strong> {@code smallBlind} e {@code bigBlind} sao
 *       obrigatoriamente positivos e pares (ex.: 50 e 100 fichas). Seguindo a regra oficial de Heads-Up,
 *       o <strong>Botao (Dealer)</strong> paga o <strong>Small Blind</strong>, recebe a primeira carta e
 *       <strong>age primeiro no {@link PokerStage#PRE_FLOP}</strong>, passando a <strong>agir por ultimo</strong>
 *       nas ruas pos-flop ({@link PokerStage#FLOP}, {@link PokerStage#TURN} e {@link PokerStage#RIVER}).
 *       O botao alterna entre {@link PokerParticipant#JOGADOR} e {@link PokerParticipant#BOT} a cada nova mao.</li>
 *   <li><strong>Acoes Validas ({@link PokerAction}):</strong>
 *       <ul>
 *         <li>{@link PokerAction#DESISTIR} (Fold): encerra a mao imediatamente em
 *             {@link PokerStage#ENCERRADA_POR_DESISTENCIA}, entregando o pote ao oponente sem revelar cartas ocultas.</li>
 *         <li>{@link PokerAction#PASSAR} (Check): permitido somente quando nao ha diferenca a pagar na rua atual
 *             ({@code getValorParaPagarJogador() == 0}).</li>
 *         <li>{@link PokerAction#PAGAR} (Call): iguala a aposta do oponente na rua (ou coloca todas as fichas restantes
 *             em <em>all-in</em> caso o saldo disponivel seja menor que a diferenca).</li>
 *         <li>{@link PokerAction#AUMENTAR} (Bet/Raise): permitido ate o limite de aumentos por rua (3 aumentos por street)
 *             quando ambos ainda possuem fichas para disputar. O valor adicional de aumento sobre a aposta atual da mesa
 *             deve ser par e {@code >= bigBlind} (ou exatamente todo o saldo restante em <em>all-in</em>).</li>
 *       </ul>
 *   </li>
 *   <li><strong>All-In e Excesso Nao Igualado (Uncalled Bet):</strong> se uma aposta ou aumento exceder o total
 *       que o adversario consegue cobrir, o excesso nao igualado e devolvido imediatamente ao apostador, e se nao
 *       restarem mais acoes possiveis, as cartas comunitarias faltantes sao abertas automaticamente ate o
 *       {@link PokerStage#SHOWDOWN}.</li>
 *   <li><strong>Divisao do Pote (Split Pot):</strong> em caso de empate exato na melhor mao de 5 cartas no Showdown,
 *       o pote e dividido igualmente ({@code pote / 2}). Caso exista 1 ficha impar residual decorrente de <em>all-in</em>
 *       quebrado, essa ficha extra (<em>odd chip</em>) e entregue ao participante fora de posicao pos-flop
 *       (o oponente do botao Dealer, ou seja, o Big Blind).</li>
 *   <li><strong>Ocultacao Estrita das Cartas do Bot:</strong> {@link #getCartasVisiveisBot()} retorna lista vazia
 *       enquanto a mao estiver em andamento ou caso termine por desistencia; as 2 cartas do Bot sao reveladas
 *       <strong>exclusivamente</strong> quando o estagio atinge {@link PokerStage#SHOWDOWN}.</li>
 * </ul>
 */
public interface PokerEngine {

    // =========================================================================
    // ACOES DO JOGADOR HUMANO
    // =========================================================================

    /**
     * Inicia uma nova mao de Texas Hold'em (alternando o botao Dealer a partir da segunda mao),
     * cobra os blinds automaticamente, distribui 2 cartas fechadas para cada lado e, caso seja
     * a vez inicial do Bot no Pre-Flop, processa a acao inicial da IA.
     *
     * <p>Permitido quando o estagio atual for {@link PokerStage#AGUARDANDO_MAO},
     * {@link PokerStage#SHOWDOWN} ou {@link PokerStage#ENCERRADA_POR_DESISTENCIA}, desde que
     * o jogador possua fichas suficientes ao menos para cobrir o blind obrigatorio da sua posicao.</p>
     *
     * @return {@code true} se a nova mao foi iniciada com sucesso; {@code false} caso contrario.
     */
    boolean iniciarNovaMao();

    /**
     * Executa a acao {@link PokerAction#DESISTIR} (Fold) no turno do jogador.
     *
     * @return {@code true} se a acao foi aceita; {@code false} se nao for o turno do jogador.
     */
    boolean desistir();

    /**
     * Executa a acao {@link PokerAction#PASSAR} (Check) no turno do jogador.
     *
     * @return {@code true} se a acao foi aceita; {@code false} se houver aposta pendente ou fora de turno.
     */
    boolean passar();

    /**
     * Executa a acao {@link PokerAction#PAGAR} (Call) no turno do jogador, igualando a aposta do Bot
     * (ou entrando em all-in se o saldo do jogador for inferior ao valor pendente).
     *
     * @return {@code true} se a acao foi aceita; {@code false} se nao houver valor a pagar ou fora de turno.
     */
    boolean pagar();

    /**
     * Executa a acao {@link PokerAction#AUMENTAR} (Bet / Raise) no turno do jogador.
     *
     * <p>O parametro {@code valorAcimaDaApostaAtual} indica quanto a nova aposta total da rua do jogador
     * ficara <strong>acima</strong> da maior aposta ja colocada na rua atual (ex.: se ambos estao em 0 e o
     * jogador chama {@code aumentar(100)}, sua aposta na rua passa a 100; se o Bot apostou 100 e o jogador
     * chama {@code aumentar(200)}, o jogador iguala os 100 e eleva mais 200, totalizando 300 na rua).</p>
     *
     * <p>Regras de validacao: deve ser o turno do jogador, {@link PokerAction#AUMENTAR} deve estar entre
     * {@link #getAcoesValidasJogador()}, e {@code valorAcimaDaApostaAtual} deve ser par e {@code >= getBigBlind()}
     * (exceto quando o jogador coloca exatamente todo o seu saldo restante em <em>all-in</em>, desde que
     * isso supere a aposta atual da rua).</p>
     *
     * @param valorAcimaDaApostaAtual incremento desejado sobre a maior aposta atual da rua.
     * @return {@code true} se o aumento foi realizado; {@code false} se invalido ou fora de turno.
     */
    boolean aumentar(long valorAcimaDaApostaAtual);

    // =========================================================================
    // CONSULTAS DE ESTADO, SALDOS E MESA (VIEW)
    // =========================================================================

    PokerStage getEstagioAtual();

    PokerRoundResult getResultadoMao();

    /**
     * Retorna de quem e o turno de agir no momento, ou {@code null} se a mao nao estiver em andamento.
     *
     * @return {@link PokerParticipant#JOGADOR}, {@link PokerParticipant#BOT} ou {@code null}.
     */
    PokerParticipant getTurnoAtual();

    /**
     * Retorna quem ocupa o Botao (Dealer / Small Blind) na mao atual.
     *
     * @return participante com o botao Dealer.
     */
    PokerParticipant getBotaoDealer();

    long getSaldoJogador();

    long getSaldoBot();

    long getPoteTotal();

    long getApostaJogadorNaRua();

    long getApostaBotNaRua();

    /**
     * Retorna quantas fichas o jogador precisa colocar para igualar a aposta atual do Bot
     * (ja limitado ao saldo disponivel do jogador em caso de all-in).
     *
     * @return valor efetivo para pagar ({@code >= 0}).
     */
    long getValorParaPagarJogador();

    long getSmallBlind();

    long getBigBlind();

    /**
     * Retorna o conjunto imutavel de acoes atualmente permitidas para o jogador humano.
     * Se nao for o turno do jogador, retorna conjunto vazio.
     *
     * @return conjunto imutavel de {@link PokerAction}.
     */
    Set<PokerAction> getAcoesValidasJogador();

    /**
     * Retorna um snapshot imutavel das 2 cartas privativas (hole cards) do jogador.
     *
     * @return lista imutavel de {@link Card}.
     */
    List<Card> getMaoJogador();

    /**
     * Retorna um snapshot imutavel das cartas comunitarias ja abertas na mesa
     * (0 no Pre-Flop, 3 no Flop, 4 no Turn, 5 no River/Showdown).
     *
     * @return lista imutavel de {@link Card}.
     */
    List<Card> getCartasComunitarias();

    /**
     * Retorna as cartas do Bot <strong>exclusivamente no {@link PokerStage#SHOWDOWN}</strong>.
     * Em qualquer outro estagio (inclusive desistencia), retorna lista vazia para impedir vazamento.
     *
     * @return lista imutavel com as 2 cartas do Bot no Showdown, ou lista vazia nos demais estados.
     */
    List<Card> getCartasVisiveisBot();

    /**
     * Indica se o Bot possui cartas fechadas na mesa ainda ocultas para a interface.
     *
     * @return {@code true} quando o Bot recebeu cartas e o estagio nao e {@link PokerStage#SHOWDOWN}.
     */
    boolean hasCartasOcultasBot();

    /**
     * Retorna quantas cartas o Bot possui na mao atual (0 antes da primeira mao, 2 apos distribuicao).
     *
     * @return quantidade de cartas do Bot ({@code 0} ou {@code 2}).
     */
    int getQuantidadeCartasBot();

    /**
     * Retorna a melhor mao avaliada de 5 cartas do jogador quando a mao alcanca {@link PokerStage#SHOWDOWN},
     * ou {@code null} caso a mao ainda nao tenha chegado ao Showdown.
     *
     * @return {@link EvaluatedPokerHand} do jogador no Showdown, ou {@code null}.
     */
    EvaluatedPokerHand getMelhorMaoJogadorShowdown();

    /**
     * Retorna a melhor mao avaliada de 5 cartas do Bot exclusivamente no {@link PokerStage#SHOWDOWN},
     * ou {@code null} em qualquer outro estado (sem vazamento).
     *
     * @return {@link EvaluatedPokerHand} do Bot no Showdown, ou {@code null}.
     */
    EvaluatedPokerHand getMelhorMaoBotShowdown();

    /**
     * Retorna uma mensagem descritiva da ultima acao relevante ocorrida na mesa
     * (util para feedback na barra de status da futura interface).
     *
     * @return texto curto em portugues descrevendo o ultimo evento da mao.
     */
    String getUltimaAcaoDescricao();
}
