package model;

import java.util.List;

/**
 * Motor oficial de regras do Blackjack Solo contra o crupie (dealer), implementando
 * integralmente o contrato {@link BlackjackEngine} sem qualquer dependencia de interface
 * grafica (Swing/Graphics2D), audio ou entrada de teclado.
 *
 * <p><strong>Regras oficiais adotadas nesta versao (v1):</strong></p>
 * <ul>
 *   <li><strong>Baralho:</strong> 1 baralho frances padrao de 52 cartas ({@link Deck}). Em modo de
 *       producao, o baralho e reiniciado e embaralhado com {@code SecureRandom} a cada nova rodada;
 *       em modo deterministico (injetando um {@link Deck} customizado), a sequencia exata fornecida
 *       e consumida sem embaralhamento automatico para permitir testes controlados.</li>
 *   <li><strong>Ordem de distribuicao inicial (4 cartas):</strong>
 *       1a carta -> Jogador (aberta);
 *       2a carta -> Dealer (aberta / visivel);
 *       3a carta -> Jogador (aberta);
 *       4a carta -> Dealer (oculta / virada para baixo durante {@link RoundState#TURNO_JOGADOR}).</li>
 *   <li><strong>Blackjack Natural (21 com exatamente 2 cartas):</strong> verificado imediatamente
 *       apos a distribuicao inicial:
 *       <ul>
 *         <li>Se ambos (jogador e dealer) tiverem Blackjack Natural -> {@link RoundResult#EMPATE_PUSH}.</li>
 *         <li>Se apenas o jogador tiver Blackjack Natural -> {@link RoundResult#JOGADOR_BLACKJACK} (paga 3:2).</li>
 *         <li>Se apenas o dealer tiver Blackjack Natural -> {@link RoundResult#DEALER_BLACKJACK} (jogador perde a aposta).</li>
 *       </ul>
 *       Em qualquer um desses tres casos, a carta oculta e revelada e a rodada encerra imediatamente
 *       em {@link RoundState#FINALIZADA}.</li>
 *   <li><strong>Comportamento do Dealer no 17:</strong> ao encerrar o turno do jogador ({@link #parar()}),
 *       o dealer revela sua carta oculta, compra cartas enquanto sua pontuacao for {@code < 17}
 *       e <strong>para em qualquer 17 ou mais</strong> (tanto <em>hard 17</em> quanto <em>soft 17</em>,
 *       ex.: As + 6 = 17 -> para).</li>
 *   <li><strong>Semantica financeira e protecao contra overflow:</strong>
 *       <ul>
 *         <li>Saldo inicial padrao: {@value #SALDO_INICIAL_PADRAO} fichas virtuais.</li>
 *         <li>Apostas devem ser positivas ({@code > 0}), pares ({@code valorAposta % 2 == 0}) e {@code <= saldoVirtual},
 *             alem de respeitar limites seguros de {@code long} verificados com {@link Math#multiplyExact(long, long)}
 *             e {@link Math#addExact(long, long)}.</li>
 *         <li>No inicio da rodada, {@code valorAposta} e debitado imediatamente do saldo.</li>
 *         <li>Na liquidacao (executada <strong>exatamente uma unica vez</strong> por rodada controlada por flag interna),
 *             credita-se o <strong>retorno total</strong> (aposta devolvida + lucro liquido) ao saldo:
 *             {@code 0} na derrota (lucro {@code -aposta}), {@code aposta} no empate (lucro {@code 0}),
 *             {@code 2 * aposta} na vitoria comum 1:1 (lucro {@code +aposta}) e
 *             {@code aposta + (aposta * 3) / 2} no Blackjack Natural 3:2 (lucro {@code +(aposta * 3) / 2}).</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><strong>Conceitos de POO demonstrados:</strong></p>
 * <ul>
 *   <li><strong>Abstracao e Polimorfismo por Interface:</strong> implementa {@link BlackjackEngine},
 *       permitindo que a View substitua mocks por este motor real sem alterar o codigo visual.</li>
 *   <li><strong>Composicao:</strong> delega responsabilidades coesas para {@link Deck} e duas instancias
 *       de {@link Hand} ({@code maoJogador} e {@code maoDealer}).</li>
 *   <li><strong>Encapsulamento Forte:</strong> oculta a segunda carta do dealer durante
 *       {@link RoundState#TURNO_JOGADOR}, retorna snapshots imutaveis ({@code List.copyOf}) e blinda
 *       transicoes de estado e liquidacao contra chamadas repetidas ou fora de ordem.</li>
 * </ul>
 */
public class BlackjackGame implements BlackjackEngine {

    /** Saldo inicial padrao do cassino social (50.000 fichas virtuais). */
    public static final long SALDO_INICIAL_PADRAO = 50_000L;

    /** Pontuacao minima em que o dealer deve parar de comprar cartas (inclusive soft 17). */
    public static final int LIMITE_PARADA_DEALER = 17;

    /** Quantidade minima de cartas exigida no baralho para iniciar uma rodada segura. */
    private static final int MINIMO_CARTAS_PARA_DISTRIBUICAO_INICIAL = 4;

    private final Deck baralho;
    private final boolean reembaralharAutomaticamentePorRodada;
    private final Hand maoJogador;
    private final Hand maoDealer;

    private long saldoVirtual;
    private long apostaAtual;
    private long ultimoRetornoTotalCreditado;
    private RoundState estadoRodada;
    private RoundResult resultadoRodada;
    private boolean cartaOcultaDealer;
    private boolean rodadaLiquidada;

    /**
     * Cria um motor de Blackjack com o saldo inicial padrao ({@value #SALDO_INICIAL_PADRAO} fichas)
     * e um baralho padrao de 52 cartas embaralhado automaticamente com {@code SecureRandom} a cada rodada.
     */
    public BlackjackGame() {
        this(SALDO_INICIAL_PADRAO);
    }

    /**
     * Cria um motor de Blackjack com saldo inicial customizado e baralho padrao de producao
     * (reembaralhado automaticamente a cada nova partida).
     *
     * @param saldoInicial saldo inicial em fichas virtuais ({@code >= 0}).
     * @throws IllegalArgumentException se {@code saldoInicial < 0}.
     */
    public BlackjackGame(long saldoInicial) {
        this(saldoInicial, new Deck(), true);
    }

    /**
     * Cria um motor de Blackjack recebendo um {@link Deck} injetado (por exemplo, com sequencia
     * deterministica de cartas para testes automatizados).
     *
     * <p>Neste construtor, o baralho injetado <strong>nao e reiniciado nem reembaralhado automaticamente</strong>
     * ao iniciar a partida, preservando rigorosamente a ordem das cartas configurada pelo teste
     * (a menos que o baralho possua menos de 4 cartas restantes, caso em que e reiniciado e embaralhado
     * por seguranca).</p>
     *
     * @param saldoInicial saldo inicial em fichas virtuais ({@code >= 0}).
     * @param baralhoInjetado instancia de {@link Deck} a utilizar (nao pode ser nula).
     */
    public BlackjackGame(long saldoInicial, Deck baralhoInjetado) {
        this(saldoInicial, baralhoInjetado, false);
    }

    /**
     * Construtor completo com controle explicito sobre saldo, instancia de baralho e politica de reembaralhamento.
     *
     * @param saldoInicial                         saldo inicial em fichas virtuais ({@code >= 0}).
     * @param baralho                              baralho de cartas (nao pode ser nulo).
     * @param reembaralharAutomaticamentePorRodada {@code true} para reiniciar as 52 cartas e embaralhar a cada
     *                                             nova partida; {@code false} para consumir a sequencia atual do {@code Deck}.
     */
    public BlackjackGame(long saldoInicial, Deck baralho, boolean reembaralharAutomaticamentePorRodada) {
        if (saldoInicial < 0L) {
            throw new IllegalArgumentException("O saldo inicial nao pode ser negativo: " + saldoInicial);
        }
        if (baralho == null) {
            throw new IllegalArgumentException("O baralho (Deck) nao pode ser nulo.");
        }
        this.saldoVirtual = saldoInicial;
        this.baralho = baralho;
        this.reembaralharAutomaticamentePorRodada = reembaralharAutomaticamentePorRodada;
        this.maoJogador = new Hand();
        this.maoDealer = new Hand();
        this.apostaAtual = 0L;
        this.ultimoRetornoTotalCreditado = 0L;
        this.estadoRodada = RoundState.AGUARDANDO_APOSTA;
        this.resultadoRodada = RoundResult.EM_ANDAMENTO;
        this.cartaOcultaDealer = false;
        this.rodadaLiquidada = false;
    }

    @Override
    public boolean iniciarPartida(long valorAposta) {
        if (!podeIniciarNovaPartida()) {
            return false;
        }
        if (!isApostaValidaESeguraContraOverflow(valorAposta)) {
            return false;
        }

        prepararBaralhoParaNovaRodada();
        if (this.baralho.cartasRestantes() < MINIMO_CARTAS_PARA_DISTRIBUICAO_INICIAL) {
            return false;
        }

        // Debita a aposta imediatamente e prepara o estado limpo da nova rodada
        this.saldoVirtual = Math.subtractExact(this.saldoVirtual, valorAposta);
        this.apostaAtual = valorAposta;
        this.ultimoRetornoTotalCreditado = 0L;
        this.rodadaLiquidada = false;
        this.maoJogador.limpar();
        this.maoDealer.limpar();

        // Ordem oficial de distribuicao: Jogador (1a), Dealer aberta (2a), Jogador (3a), Dealer oculta (4a)
        this.maoJogador.adicionarCarta(comprarCartaSegura());
        this.maoDealer.adicionarCarta(comprarCartaSegura());
        this.maoJogador.adicionarCarta(comprarCartaSegura());
        this.maoDealer.adicionarCarta(comprarCartaSegura());

        this.estadoRodada = RoundState.TURNO_JOGADOR;
        this.resultadoRodada = RoundResult.EM_ANDAMENTO;
        this.cartaOcultaDealer = true;

        // Verifica Blackjack Natural imediato (21 com exatamente 2 cartas)
        verificarDesfechoInicialBlackjackNatural();
        return true;
    }

    @Override
    public boolean pedirCarta() {
        if (this.estadoRodada != RoundState.TURNO_JOGADOR) {
            return false;
        }

        this.maoJogador.adicionarCarta(comprarCartaSegura());

        if (this.maoJogador.isEstourada()) {
            finalizarELiquidarRodadaUnicaVez(RoundResult.JOGADOR_ESTOUROU);
        }

        return true;
    }

    @Override
    public void parar() {
        if (this.estadoRodada != RoundState.TURNO_JOGADOR) {
            return;
        }

        // Revela a carta oculta do dealer e executa o turno automatico da banca (para em todo 17)
        this.cartaOcultaDealer = false;
        while (this.maoDealer.calcularPontuacao() < LIMITE_PARADA_DEALER) {
            this.maoDealer.adicionarCarta(comprarCartaSegura());
        }

        RoundResult desfecho = apurarResultadoAposTurnoDoDealer();
        finalizarELiquidarRodadaUnicaVez(desfecho);
    }

    @Override
    public long getSaldoVirtual() {
        return this.saldoVirtual;
    }

    @Override
    public long getApostaAtual() {
        return this.apostaAtual;
    }

    @Override
    public RoundState getEstadoRodada() {
        return this.estadoRodada;
    }

    @Override
    public RoundResult getResultadoRodada() {
        return this.resultadoRodada;
    }

    @Override
    public List<Card> getMaoJogador() {
        return this.maoJogador.getCartas();
    }

    @Override
    public List<Card> getCartasVisiveisDealer() {
        if (this.maoDealer.estaVazia()) {
            return List.of();
        }
        if (this.cartaOcultaDealer) {
            // Nunca vaza a 2a carta oculta enquanto o turno do jogador estiver ativo
            return this.maoDealer.getPrimeirasCartas(1);
        }
        return this.maoDealer.getCartas();
    }

    @Override
    public boolean hasCartaOcultaDealer() {
        return this.cartaOcultaDealer;
    }

    @Override
    public int getPontosJogador() {
        return this.maoJogador.calcularPontuacao();
    }

    @Override
    public int getPontosDealerVisiveis() {
        if (this.maoDealer.estaVazia()) {
            return 0;
        }
        if (this.cartaOcultaDealer) {
            return this.maoDealer.calcularPontuacaoPrimeirasCartas(1);
        }
        return this.maoDealer.calcularPontuacao();
    }

    /**
     * Retorna o <strong>retorno total bruto</strong> creditado ao saldo na liquidacao da rodada
     * (incluindo a devolucao da aposta original + eventual lucro; {@code 0} enquanto em andamento ou em derrota).
     *
     * @return montante total creditado ao encerrar a rodada.
     */
    public long getUltimoRetornoTotalCreditado() {
        return this.ultimoRetornoTotalCreditado;
    }

    /**
     * Retorna a <strong>variacao liquida (lucro ou prejuizo)</strong> da rodada finalizada em relacao ao saldo
     * anterior a aposta:
     * <ul>
     *   <li>Derrota: {@code -apostaAtual}</li>
     *   <li>Empate (Push): {@code 0}</li>
     *   <li>Vitoria simples (1:1): {@code +apostaAtual}</li>
     *   <li>Blackjack Natural (3:2): {@code +(apostaAtual * 3 / 2)}</li>
     *   <li>Rodada nao finalizada: {@code 0}</li>
     * </ul>
     *
     * @return lucro liquido positivo, zero ou prejuizo negativo da rodada concluida.
     */
    public long getLucroLiquidoUltimaRodada() {
        if (this.estadoRodada != RoundState.FINALIZADA) {
            return 0L;
        }
        return Math.subtractExact(this.ultimoRetornoTotalCreditado, this.apostaAtual);
    }

    private boolean podeIniciarNovaPartida() {
        return this.estadoRodada == RoundState.AGUARDANDO_APOSTA
                || this.estadoRodada == RoundState.FINALIZADA;
    }

    private boolean isApostaValidaESeguraContraOverflow(long valorAposta) {
        if (valorAposta <= 0L || valorAposta > this.saldoVirtual) {
            return false;
        }
        if (valorAposta % 2L != 0L) {
            return false;
        }

        // Verifica previamente se o maior pagamento possivel (Blackjack 3:2 -> retorno total = 2.5 * aposta)
        // somado ao saldo remanescente cabera sem overflow em um long de 64 bits.
        try {
            long saldoAposDebito = Math.subtractExact(this.saldoVirtual, valorAposta);
            long metadeAposta = valorAposta / 2L;
            long lucroMaximoBlackjack = Math.multiplyExact(metadeAposta, 3L);
            long retornoMaximoBlackjack = Math.addExact(valorAposta, lucroMaximoBlackjack);
            Math.addExact(saldoAposDebito, retornoMaximoBlackjack);
            return true;
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    private void prepararBaralhoParaNovaRodada() {
        if (this.reembaralharAutomaticamentePorRodada) {
            this.baralho.reiniciarBaralhoPadrao();
            this.baralho.embaralhar();
        }
    }

    private Card comprarCartaSegura() {
        if (this.baralho.estaVazio()) {
            // Caso de contingencia se uma partida longa esgotar o baralho injetado
            this.baralho.reiniciarBaralhoPadrao();
            this.baralho.embaralhar();
        }
        return this.baralho.comprarCarta();
    }

    private void verificarDesfechoInicialBlackjackNatural() {
        boolean jogadorBJ = this.maoJogador.isBlackjackNatural();
        boolean dealerBJ = this.maoDealer.isBlackjackNatural();

        if (jogadorBJ && dealerBJ) {
            finalizarELiquidarRodadaUnicaVez(RoundResult.EMPATE_PUSH);
        } else if (jogadorBJ) {
            finalizarELiquidarRodadaUnicaVez(RoundResult.JOGADOR_BLACKJACK);
        } else if (dealerBJ) {
            finalizarELiquidarRodadaUnicaVez(RoundResult.DEALER_BLACKJACK);
        }
    }

    private RoundResult apurarResultadoAposTurnoDoDealer() {
        if (this.maoDealer.isEstourada()) {
            return RoundResult.JOGADOR_VITORIA;
        }

        int pontosJogador = this.maoJogador.calcularPontuacao();
        int pontosDealer = this.maoDealer.calcularPontuacao();

        if (pontosJogador > pontosDealer) {
            return RoundResult.JOGADOR_VITORIA;
        }
        if (pontosDealer > pontosJogador) {
            return RoundResult.DEALER_VITORIA;
        }
        return RoundResult.EMPATE_PUSH;
    }

    private void finalizarELiquidarRodadaUnicaVez(RoundResult resultadoFinal) {
        if (this.rodadaLiquidada || this.estadoRodada == RoundState.FINALIZADA) {
            return;
        }

        this.cartaOcultaDealer = false;
        this.resultadoRodada = resultadoFinal;
        this.estadoRodada = RoundState.FINALIZADA;
        this.rodadaLiquidada = true;

        long retornoTotal = calcularRetornoTotalVerificado(resultadoFinal, this.apostaAtual);
        this.ultimoRetornoTotalCreditado = retornoTotal;
        this.saldoVirtual = Math.addExact(this.saldoVirtual, retornoTotal);
    }

    /**
     * Calcula o retorno total bruto com aritmetica exata verificada contra overflow ({@code Math.*Exact}),
     * mantendo compatibilidade matematica identica a {@link RoundResult#calcularRetornoTotal(long)}.
     */
    private static long calcularRetornoTotalVerificado(RoundResult resultado, long aposta) {
        if (aposta <= 0L) {
            return 0L;
        }
        switch (resultado) {
            case JOGADOR_BLACKJACK:
                // Dividimos por 2 antes de multiplicar por 3 (exato pois aposta e par) para ampliar a faixa segura
                long lucroBlackjack = Math.multiplyExact(aposta / 2L, 3L);
                return Math.addExact(aposta, lucroBlackjack);
            case JOGADOR_VITORIA:
                return Math.multiplyExact(aposta, 2L);
            case EMPATE_PUSH:
                return aposta;
            case JOGADOR_ESTOUROU:
            case DEALER_VITORIA:
            case DEALER_BLACKJACK:
            case EM_ANDAMENTO:
            default:
                return 0L;
        }
    }
}
