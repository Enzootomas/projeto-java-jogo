package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Motor completo de Poker Texas Hold'em offline para mesa com ate 6 participantes
 * (1 Jogador humano + 5 Bots), implementando {@link PokerEngine} e suportando:
 * <ul>
 *   <li>6 lugares com rotacao de botao Dealer, Small Blind e Big Blind.</li>
 *   <li>Transicao automatica para a regra oficial de Heads-Up quando restarem apenas 2 participantes com fichas.</li>
 *   <li>Desistencias (Fold), All-In com valores diferentes, devolucao de excesso nao coberto (Uncalled Bet),
 *       calculo exato de Pote Principal e Potes Paralelos (Side Pots), empates (Split Pot) e conservacao absoluta das fichas.</li>
 *   <li>Execucao passo a passo dos Bots via {@link #executarPassoBot()} para pausas visiveis entre turnos
 *       sem bloquear a EDT do Swing, mantendo ocultacao estrita das cartas privadas dos Bots ate o Showdown.</li>
 * </ul>
 */
public class TexasHoldemTableGame implements PokerEngine {

    public static final int TOTAL_LUGARES_PADRAO = 6;
    public static final long SALDO_INICIAL_PADRAO = 50_000L;
    public static final long SMALL_BLIND_PADRAO = 50L;
    public static final long BIG_BLIND_PADRAO = 100L;
    public static final int MAX_AUMENTOS_POR_RUA = 4;

    private final List<PokerSeatState> seats;
    private final Deck baralho;
    private final boolean reembaralharAutomaticamente;
    private final long smallBlind;
    private final long bigBlind;
    private final PokerBotStrategy botStrategy;

    private final List<Card> cartasComunitarias;

    private PokerStage estagioAtual;
    private PokerRoundResult resultadoMao;

    private int seatDealer;
    private int proximoSeatDealer;
    private int seatSmallBlind;
    private int seatBigBlind;
    private int seatTurnoAtual;

    private long poteTotal;
    private long maiorApostaNaRua;
    private long ultimoAumentoNaRua;
    private int aumentosNaRua;
    private boolean poteLiquidado;

    private List<PokerSidePot> potesCalculados;
    private String ultimaAcaoDescricao;

    /**
     * Cria uma mesa padrao de 6 participantes (Jogador + 5 Bots) para producao.
     */
    public TexasHoldemTableGame() {
        this(
                criarSaldosPadraoSeisLugares(SALDO_INICIAL_PADRAO),
                SMALL_BLIND_PADRAO,
                BIG_BLIND_PADRAO,
                new Deck(),
                true,
                new PokerSimpleAI(),
                0
        );
    }

    /**
     * Construtor deterministico para testes automatizados com saldos individuais por assento,
     * baralho controlado, estrategia de Bot injetada e posicao inicial do botao Dealer.
     */
    public TexasHoldemTableGame(long[] saldosPorAssento,
                                long smallBlind,
                                long bigBlind,
                                Deck baralhoDeterministico,
                                PokerBotStrategy botStrategy,
                                int seatDealerInicial) {
        this(saldosPorAssento, smallBlind, bigBlind, baralhoDeterministico, false, botStrategy, seatDealerInicial);
    }

    public TexasHoldemTableGame(long[] saldosPorAssento,
                                long smallBlind,
                                long bigBlind,
                                Deck baralho,
                                boolean reembaralharAutomaticamente,
                                PokerBotStrategy botStrategy,
                                int seatDealerInicial) {
        if (saldosPorAssento == null || saldosPorAssento.length < 2 || saldosPorAssento.length > 6) {
            throw new IllegalArgumentException("A mesa de Poker exige entre 2 e 6 participantes.");
        }
        if (smallBlind <= 0L || smallBlind % 2L != 0L) {
            throw new IllegalArgumentException("Small Blind deve ser positivo e par.");
        }
        if (bigBlind < smallBlind || bigBlind % 2L != 0L) {
            throw new IllegalArgumentException("Big Blind deve ser par e >= Small Blind.");
        }
        this.baralho = Objects.requireNonNull(baralho, "Deck nao pode ser nulo.");
        this.botStrategy = Objects.requireNonNull(botStrategy, "Estrategia do Bot nao pode ser nula.");
        this.reembaralharAutomaticamente = reembaralharAutomaticamente;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;

        this.seats = new ArrayList<>(saldosPorAssento.length);
        for (int i = 0; i < saldosPorAssento.length; i++) {
            String nome = (i == 0) ? "Você" : ("Bot " + i);
            this.seats.add(new PokerSeatState(i, nome, i == 0, saldosPorAssento[i]));
        }

        this.cartasComunitarias = new ArrayList<>(5);
        this.potesCalculados = List.of();
        this.estagioAtual = PokerStage.AGUARDANDO_MAO;
        this.resultadoMao = PokerRoundResult.AGUARDANDO_INICIO;
        this.seatDealer = Math.floorMod(seatDealerInicial, seats.size());
        this.proximoSeatDealer = this.seatDealer;
        this.seatSmallBlind = -1;
        this.seatBigBlind = -1;
        this.seatTurnoAtual = -1;
        this.ultimaAcaoDescricao = "Mesa de Poker (" + seats.size() + " lugares) pronta.";
    }

    private static long[] criarSaldosPadraoSeisLugares(long saldo) {
        long[] arr = new long[TOTAL_LUGARES_PADRAO];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = saldo;
        }
        return arr;
    }

    // =========================================================================
    // INICIO DE MAO E TRANSICAO HEADS-UP / MULTI-JOGADOR
    // =========================================================================

    @Override
    public boolean iniciarNovaMao() {
        if (estagioAtual.isMaoEmAndamento()) {
            return false;
        }

        int participantesComFichas = contarParticipantesComFichas();
        if (participantesComFichas < 2 || seats.get(0).getSaldo() <= 0L) {
            return false;
        }

        int cartasNecessarias = (participantesComFichas * 2) + 5;
        if (reembaralharAutomaticamente) {
            baralho.reiniciarBaralhoPadrao();
            baralho.embaralhar();
        } else if (baralho.cartasRestantes() < cartasNecessarias) {
            return false;
        }

        for (int i = cartasComunitarias.size() - 1; i >= 0; i--) {
            cartasComunitarias.remove(i);
        }
        for (PokerSeatState s : seats) {
            s.prepararParaNovaMao();
        }

        // Seleciona o botao Dealer no proximo assento elegivel com fichas
        this.seatDealer = encontrarProximoSeatNaoEliminadoApartirDe(this.proximoSeatDealer);
        this.proximoSeatDealer = (this.seatDealer + 1) % seats.size();

        if (participantesComFichas == 2) {
            // Regra oficial Heads-Up: Dealer posta Small Blind e age primeiro no Pre-Flop
            this.seatSmallBlind = this.seatDealer;
            this.seatBigBlind = encontrarProximoSeatNaoEliminadoApos(this.seatDealer);
        } else {
            // Regra oficial 3..6 jogadores: SB a esquerda do Dealer, BB a esquerda do SB
            this.seatSmallBlind = encontrarProximoSeatNaoEliminadoApos(this.seatDealer);
            this.seatBigBlind = encontrarProximoSeatNaoEliminadoApos(this.seatSmallBlind);
        }

        this.poteTotal = 0L;
        this.maiorApostaNaRua = 0L;
        this.ultimoAumentoNaRua = this.bigBlind;
        this.aumentosNaRua = 0;
        this.poteLiquidado = false;
        this.estagioAtual = PokerStage.PRE_FLOP;
        this.resultadoMao = PokerRoundResult.EM_ANDAMENTO;

        // Cobranca de blinds
        long sbPago = seats.get(seatSmallBlind).colocarFichas(smallBlind);
        seats.get(seatSmallBlind).setUltimaAcao("SB " + sbPago);
        long bbPago = seats.get(seatBigBlind).colocarFichas(bigBlind);
        seats.get(seatBigBlind).setUltimaAcao("BB " + bbPago);

        this.poteTotal = Math.addExact(sbPago, bbPago);
        this.maiorApostaNaRua = Math.max(sbPago, bbPago);

        // Distribuicao de 2 cartas fechadas por participante nao eliminado (2 rodadas a partir do SB)
        for (int rodada = 0; rodada < 2; rodada++) {
            int idx = seatSmallBlind;
            for (int k = 0; k < seats.size(); k++) {
                PokerSeatState s = seats.get(idx);
                if (!s.isEliminado()) {
                    s.adicionarCarta(baralho.comprarCarta());
                }
                idx = (idx + 1) % seats.size();
            }
        }

        recalcularPotes();

        // Define primeiro a agir no Pre-Flop
        int primeiroPreFlop = (participantesComFichas == 2)
                ? seatSmallBlind
                : encontrarProximoSeatQuePodeAgirApos(seatBigBlind);

        if (primeiroPreFlop < 0 || contarParticipantesQuePodemAgir() <= 1 && todosAtivosIgualaramAposta()) {
            executarRunoutAteShowdown();
            return true;
        }

        if (!seats.get(primeiroPreFlop).podeAgirNaRua()) {
            primeiroPreFlop = encontrarProximoSeatQuePodeAgirApos(primeiroPreFlop);
        }

        this.seatTurnoAtual = primeiroPreFlop;
        this.ultimaAcaoDescricao = "Nova mão (" + participantesComFichas + " ativos | Blinds "
                + smallBlind + "/" + bigBlind + "). Turno: "
                + (seatTurnoAtual >= 0 ? seats.get(seatTurnoAtual).getNome() : "Showdown") + ".";

        if (seatTurnoAtual < 0) {
            executarRunoutAteShowdown();
        }
        return true;
    }

    // =========================================================================
    // ACOES DO JOGADOR HUMANO (SEAT 0)
    // =========================================================================

    @Override
    public boolean desistir() {
        if (!isTurnoDoJogador()) {
            return false;
        }
        aplicarDesistencia(seats.get(0));
        return true;
    }

    @Override
    public boolean passar() {
        if (!isTurnoDoJogador() || getValorParaPagar(seats.get(0)) > 0L) {
            return false;
        }
        aplicarPassar(seats.get(0));
        return true;
    }

    @Override
    public boolean pagar() {
        if (!isTurnoDoJogador()) {
            return false;
        }
        long paraPagar = getValorParaPagar(seats.get(0));
        if (paraPagar <= 0L) {
            return false;
        }
        aplicarPagar(seats.get(0), paraPagar);
        return true;
    }

    @Override
    public boolean aumentar(long valorAcimaDaApostaAtual) {
        if (!isTurnoDoJogador() || !podeSeatAumentar(seats.get(0)) || valorAcimaDaApostaAtual <= 0L) {
            return false;
        }
        PokerSeatState jogador = seats.get(0);
        long novaApostaTotal;
        try {
            novaApostaTotal = Math.addExact(maiorApostaNaRua, valorAcimaDaApostaAtual);
        } catch (ArithmeticException e) {
            return false;
        }
        long fichasNecessarias = Math.subtractExact(novaApostaTotal, jogador.getApostaNaRua());
        if (fichasNecessarias > jogador.getSaldo()) {
            return false;
        }

        boolean allInExato = (fichasNecessarias == jogador.getSaldo());
        long aumentoMinimo = Math.max(bigBlind, ultimoAumentoNaRua);
        if (!allInExato && (valorAcimaDaApostaAtual < aumentoMinimo || valorAcimaDaApostaAtual % 2L != 0L)) {
            return false;
        }

        aplicarAumentar(jogador, fichasNecessarias, valorAcimaDaApostaAtual);
        return true;
    }

    // =========================================================================
    // EXECUCAO PASSO A PASSO DOS BOTS (UM TURNO POR CHAMADA PARA RITMO VISIVEL)
    // =========================================================================

    public boolean isAguardandoAcaoBot() {
        return estagioAtual.isMaoEmAndamento() && seatTurnoAtual > 0 && seatTurnoAtual < seats.size();
    }

    /**
     * Executa exatamente o turno do Bot atual ({@link #getSeatTurnoAtual()}) usando apenas
     * as cartas privadas daquele Bot e informacoes publicas da mesa.
     */
    public boolean executarPassoBot() {
        if (!isAguardandoAcaoBot()) {
            return false;
        }
        PokerSeatState bot = seats.get(seatTurnoAtual);
        long paraPagar = getValorParaPagar(bot);
        boolean podeAumentar = podeSeatAumentar(bot);

        PokerBotStrategy.Decision dec = botStrategy.decidirAcao(
                bot.getMaoInternaSnapshot(),
                List.copyOf(cartasComunitarias),
                estagioAtual,
                poteTotal,
                paraPagar,
                bot.getSaldo(),
                bigBlind,
                podeAumentar
        );

        PokerAction acao = (dec != null) ? dec.getAcao() : PokerAction.PASSAR;

        if (acao == PokerAction.AUMENTAR && podeAumentar) {
            long inc = Math.max(dec.getValorAcimaDaApostaAtual(), Math.max(bigBlind, ultimoAumentoNaRua));
            long saldoAposPagar = Math.max(0L, bot.getSaldo() - paraPagar);
            if (saldoAposPagar > 0L) {
                long incEfetivo = Math.min(inc, saldoAposPagar);
                long fichasColocar = Math.addExact(paraPagar, incEfetivo);
                aplicarAumentar(bot, fichasColocar, incEfetivo);
                return true;
            }
        }

        if (acao == PokerAction.PAGAR && paraPagar > 0L) {
            aplicarPagar(bot, paraPagar);
            return true;
        }

        if (acao == PokerAction.PASSAR && paraPagar == 0L) {
            aplicarPassar(bot);
            return true;
        }

        if (paraPagar == 0L) {
            aplicarPassar(bot);
        } else {
            aplicarDesistencia(bot);
        }
        return true;
    }

    // =========================================================================
    // MAQUINA DE ESTADOS MULTI-JOGADOR, SIDE POTS E SHOWDOWN
    // =========================================================================

    private void aplicarDesistencia(PokerSeatState seat) {
        seat.marcarDesistencia();
        this.ultimaAcaoDescricao = seat.getNome() + " desistiu (Fold).";
        avancarAposAcao(seat.getSeatIndex());
    }

    private void aplicarPassar(PokerSeatState seat) {
        seat.setJaAgiuNaRua(true);
        seat.setUltimaAcao("Passou");
        this.ultimaAcaoDescricao = seat.getNome() + " passou (Check).";
        avancarAposAcao(seat.getSeatIndex());
    }

    private void aplicarPagar(PokerSeatState seat, long paraPagar) {
        long efetivo = seat.colocarFichas(paraPagar);
        this.poteTotal = Math.addExact(this.poteTotal, efetivo);
        seat.setJaAgiuNaRua(true);
        seat.setUltimaAcao(seat.isAllIn() ? "All-In " + efetivo : "Pagou " + efetivo);
        this.ultimaAcaoDescricao = seat.getNome() + (seat.isAllIn() ? " deu All-In (" : " pagou (")
                + efetivo + " fichas).";
        recalcularPotes();
        avancarAposAcao(seat.getSeatIndex());
    }

    private void aplicarAumentar(PokerSeatState seat, long fichasColocar, long incrementoAcima) {
        long efetivo = seat.colocarFichas(fichasColocar);
        this.poteTotal = Math.addExact(this.poteTotal, efetivo);
        if (seat.getApostaNaRua() > this.maiorApostaNaRua) {
            long ganhoReal = seat.getApostaNaRua() - this.maiorApostaNaRua;
            this.maiorApostaNaRua = seat.getApostaNaRua();
            if (ganhoReal >= this.ultimoAumentoNaRua) {
                this.ultimoAumentoNaRua = ganhoReal;
            }
            this.aumentosNaRua++;
            // Reabre a rodada de acao para todos os outros jogadores ativos nao all-in
            for (PokerSeatState outro : seats) {
                if (outro.getSeatIndex() != seat.getSeatIndex() && outro.podeAgirNaRua()) {
                    outro.setJaAgiuNaRua(false);
                }
            }
        }
        seat.setJaAgiuNaRua(true);
        seat.setUltimaAcao(seat.isAllIn() ? "All-In +" + incrementoAcima : "Aumentou +" + incrementoAcima);
        this.ultimaAcaoDescricao = seat.getNome()
                + (seat.isAllIn() ? " foi de All-In (+" : " aumentou (+")
                + incrementoAcima + " fichas).";
        recalcularPotes();
        avancarAposAcao(seat.getSeatIndex());
    }

    private void avancarAposAcao(int ultimoSeatIndex) {
        // 1. Verifica vitoria imediata por desistencia (apenas 1 participante nao desistiu)
        List<PokerSeatState> ativos = listarSeatsAtivosNaMao();
        if (ativos.size() == 1) {
            encerrarPorDesistencia(ativos.get(0));
            return;
        }

        // 2. Verifica se a rua atual terminou
        if (ruaEstaConcluida()) {
            devolverExcessoNaoCobertoNaRua();
            recalcularPotes();
            if (contarParticipantesQuePodemAgir() <= 1) {
                executarRunoutAteShowdown();
            } else {
                avancarParaProximaRua();
            }
            return;
        }

        // 3. Passa o turno para o proximo participante ativo que ainda pode agir
        int proximo = encontrarProximoSeatQuePodeAgirApos(ultimoSeatIndex);
        if (proximo < 0) {
            devolverExcessoNaoCobertoNaRua();
            recalcularPotes();
            executarRunoutAteShowdown();
        } else {
            this.seatTurnoAtual = proximo;
        }
    }

    private boolean ruaEstaConcluida() {
        for (PokerSeatState s : seats) {
            if (s.podeAgirNaRua()) {
                if (!s.isJaAgiuNaRua() || s.getApostaNaRua() < maiorApostaNaRua) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean todosAtivosIgualaramAposta() {
        for (PokerSeatState s : seats) {
            if (s.podeAgirNaRua() && s.getApostaNaRua() < maiorApostaNaRua) {
                return false;
            }
        }
        return true;
    }

    /**
     * Se exatamente um participante nao desistente apostou na rua acima da segunda maior aposta
     * (por exemplo, um All-In nao igualado por nenhum outro), devolve o excesso imediatamente.
     */
    private void devolverExcessoNaoCobertoNaRua() {
        long maior = 0L;
        long segundaMaior = 0L;
        PokerSeatState lider = null;
        int qtdNoTopo = 0;

        for (PokerSeatState s : seats) {
            long ap = s.getApostaNaRua();
            if (ap > maior) {
                segundaMaior = maior;
                maior = ap;
                lider = s;
                qtdNoTopo = 1;
            } else if (ap == maior && ap > 0L) {
                qtdNoTopo++;
            } else if (ap > segundaMaior) {
                segundaMaior = ap;
            }
        }

        if (lider != null && qtdNoTopo == 1 && maior > segundaMaior) {
            long excesso = Math.subtractExact(maior, segundaMaior);
            lider.devolverExcesso(excesso);
            this.poteTotal = Math.subtractExact(this.poteTotal, excesso);
            this.maiorApostaNaRua = segundaMaior;
        }
    }

    private void avancarParaProximaRua() {
        for (PokerSeatState s : seats) {
            s.prepararParaNovaRua();
        }
        this.maiorApostaNaRua = 0L;
        this.ultimoAumentoNaRua = this.bigBlind;
        this.aumentosNaRua = 0;

        switch (estagioAtual) {
            case PRE_FLOP:
                this.estagioAtual = PokerStage.FLOP;
                cartasComunitarias.add(baralho.comprarCarta());
                cartasComunitarias.add(baralho.comprarCarta());
                cartasComunitarias.add(baralho.comprarCarta());
                break;
            case FLOP:
                this.estagioAtual = PokerStage.TURN;
                cartasComunitarias.add(baralho.comprarCarta());
                break;
            case TURN:
                this.estagioAtual = PokerStage.RIVER;
                cartasComunitarias.add(baralho.comprarCarta());
                break;
            case RIVER:
                resolverShowdown();
                return;
            default:
                return;
        }

        // Pos-Flop: primeiro participante ativo a esquerda do botao Dealer age primeiro
        int primeiroPosFlop = encontrarProximoSeatQuePodeAgirApos(seatDealer);
        if (primeiroPosFlop < 0) {
            executarRunoutAteShowdown();
        } else {
            this.seatTurnoAtual = primeiroPosFlop;
            this.ultimaAcaoDescricao = estagioAtual.getNomeExibicao() + " aberto! Turno: "
                    + seats.get(seatTurnoAtual).getNome() + ".";
        }
    }

    private void executarRunoutAteShowdown() {
        devolverExcessoNaoCobertoNaRua();
        while (cartasComunitarias.size() < 5) {
            cartasComunitarias.add(baralho.comprarCarta());
        }
        resolverShowdown();
    }

    private void encerrarPorDesistencia(PokerSeatState vencedor) {
        this.estagioAtual = PokerStage.ENCERRADA_POR_DESISTENCIA;
        this.seatTurnoAtual = -1;
        this.resultadoMao = vencedor.isHumano()
                ? PokerRoundResult.JOGADOR_VENCEU_DESISTENCIA
                : PokerRoundResult.BOT_VENCEU_DESISTENCIA;
        if (!poteLiquidado) {
            this.poteLiquidado = true;
            vencedor.creditarPremio(poteTotal);
        }
        this.ultimaAcaoDescricao = vencedor.getNome() + " venceu por desistência dos demais (Pote: "
                + poteTotal + " fichas)!";
    }

    private void resolverShowdown() {
        this.estagioAtual = PokerStage.SHOWDOWN;
        this.seatTurnoAtual = -1;

        List<PokerSeatState> ativos = listarSeatsAtivosNaMao();
        for (PokerSeatState s : ativos) {
            List<Card> sete = new ArrayList<>(7);
            sete.addAll(s.getMaoInternaSnapshot());
            sete.addAll(cartasComunitarias);
            s.setMaoShowdown(PokerHandEvaluator.avaliarMelhorMao(sete));
        }

        recalcularPotes();

        if (!poteLiquidado) {
            this.poteLiquidado = true;
            boolean jogadorGanhouAlgum = false;
            boolean botGanhouAlgum = false;
            boolean houveEmpateNoPrincipal = false;

            for (int pIdx = 0; pIdx < potesCalculados.size(); pIdx++) {
                PokerSidePot pot = potesCalculados.get(pIdx);
                List<Integer> vencedores = encontrarVencedoresDoPote(pot);
                if (vencedores.isEmpty()) {
                    continue;
                }
                long base = pot.getValor() / vencedores.size();
                long resto = pot.getValor() % vencedores.size();

                // Ordena vencedores em sentido horario a partir da esquerda do Dealer (regra da ficha impar)
                List<Integer> ordenados = ordenarClockwiseAposDealer(vencedores);
                for (int i = 0; i < ordenados.size(); i++) {
                    int seatIdx = ordenados.get(i);
                    long premio = Math.addExact(base, (i < resto) ? 1L : 0L);
                    seats.get(seatIdx).creditarPremio(premio);
                    if (seatIdx == 0) {
                        jogadorGanhouAlgum = true;
                    } else {
                        botGanhouAlgum = true;
                    }
                }
                if (pIdx == 0 && vencedores.size() > 1 && vencedores.contains(0)) {
                    houveEmpateNoPrincipal = true;
                }
            }

            if (houveEmpateNoPrincipal) {
                this.resultadoMao = PokerRoundResult.EMPATE_DIVISAO_POTE;
                this.ultimaAcaoDescricao = "Showdown empatado! Pote dividido (Split Pot).";
            } else if (jogadorGanhouAlgum && !botGanhouAlgum) {
                this.resultadoMao = PokerRoundResult.JOGADOR_VENCEU_SHOWDOWN;
                this.ultimaAcaoDescricao = "Showdown: Você venceu com "
                        + seats.get(0).getMaoShowdown(estagioAtual).getDescricaoCurta() + "!";
            } else if (jogadorGanhouAlgum) {
                this.resultadoMao = PokerRoundResult.JOGADOR_VENCEU_SHOWDOWN;
                this.ultimaAcaoDescricao = "Showdown (múltiplos potes): Você venceu parte dos potes!";
            } else {
                this.resultadoMao = PokerRoundResult.BOT_VENCEU_SHOWDOWN;
                this.ultimaAcaoDescricao = "Showdown: Bot venceu o pote de " + poteTotal + " fichas.";
            }
        }
    }

    private List<Integer> encontrarVencedoresDoPote(PokerSidePot pot) {
        EvaluatedPokerHand melhor = null;
        List<Integer> vencedores = new ArrayList<>();
        for (int seatIdx : pot.getSeatsElegiveis()) {
            EvaluatedPokerHand mao = seats.get(seatIdx).getMaoShowdown(PokerStage.SHOWDOWN);
            if (mao == null) {
                continue;
            }
            if (melhor == null) {
                melhor = mao;
                vencedores.add(seatIdx);
            } else {
                int diff = mao.compareTo(melhor);
                if (diff > 0) {
                    melhor = mao;
                    vencedores.clear();
                    vencedores.add(seatIdx);
                } else if (diff == 0) {
                    vencedores.add(seatIdx);
                }
            }
        }
        return vencedores;
    }

    private List<Integer> ordenarClockwiseAposDealer(List<Integer> indices) {
        List<Integer> resultado = new ArrayList<>(indices.size());
        int n = seats.size();
        for (int offset = 1; offset <= n; offset++) {
            int idx = (seatDealer + offset) % n;
            if (indices.contains(idx)) {
                resultado.add(idx);
            }
        }
        return resultado;
    }

    /**
     * Calcula Pote Principal e Potes Paralelos (Side Pots) a partir das contribuicoes
     * acumuladas de todos os participantes na mao atual.
     */
    private void recalcularPotes() {
        TreeSet<Long> niveis = new TreeSet<>();
        for (PokerSeatState s : seats) {
            if (s.getTotalContribuidoNaMao() > 0L) {
                niveis.add(s.getTotalContribuidoNaMao());
            }
        }
        if (niveis.isEmpty()) {
            this.potesCalculados = List.of();
            return;
        }

        List<PokerSidePot> potes = new ArrayList<>();
        long nivelAnterior = 0L;

        for (long nivel : niveis) {
            long delta = Math.subtractExact(nivel, nivelAnterior);
            long valorCamada = 0L;
            List<Integer> elegiveis = new ArrayList<>();

            for (PokerSeatState s : seats) {
                if (s.getTotalContribuidoNaMao() >= nivel) {
                    valorCamada = Math.addExact(valorCamada, delta);
                    if (s.isAtivoNaMao()) {
                        elegiveis.add(s.getSeatIndex());
                    }
                }
            }

            if (valorCamada > 0L) {
                if (!potes.isEmpty() && potes.get(potes.size() - 1).getSeatsElegiveis().equals(elegiveis)) {
                    PokerSidePot anterior = potes.remove(potes.size() - 1);
                    potes.add(new PokerSidePot(anterior.getRotulo(),
                            Math.addExact(anterior.getValor(), valorCamada), elegiveis));
                } else {
                    String rotulo = potes.isEmpty() ? "Pote Principal" : ("Pote Paralelo " + potes.size());
                    potes.add(new PokerSidePot(rotulo, valorCamada, elegiveis));
                }
            }
            nivelAnterior = nivel;
        }

        this.potesCalculados = List.copyOf(potes);
    }

    // =========================================================================
    // AUXILIARES DE NAVEGACAO CIRCULAR DE ASSENTOS
    // =========================================================================

    private int contarParticipantesComFichas() {
        int c = 0;
        for (PokerSeatState s : seats) {
            if (s.getSaldo() > 0L) {
                c++;
            }
        }
        return c;
    }

    private int contarParticipantesQuePodemAgir() {
        int c = 0;
        for (PokerSeatState s : seats) {
            if (s.podeAgirNaRua()) {
                c++;
            }
        }
        return c;
    }

    private List<PokerSeatState> listarSeatsAtivosNaMao() {
        List<PokerSeatState> ativos = new ArrayList<>();
        for (PokerSeatState s : seats) {
            if (s.isAtivoNaMao()) {
                ativos.add(s);
            }
        }
        return ativos;
    }

    private int encontrarProximoSeatNaoEliminadoApartirDe(int inicio) {
        int n = seats.size();
        for (int i = 0; i < n; i++) {
            int idx = (inicio + i) % n;
            if (!seats.get(idx).isEliminado()) {
                return idx;
            }
        }
        return -1;
    }

    private int encontrarProximoSeatNaoEliminadoApos(int atual) {
        int n = seats.size();
        for (int i = 1; i <= n; i++) {
            int idx = (atual + i) % n;
            if (!seats.get(idx).isEliminado()) {
                return idx;
            }
        }
        return -1;
    }

    private int encontrarProximoSeatQuePodeAgirApos(int atual) {
        int n = seats.size();
        for (int i = 1; i <= n; i++) {
            int idx = (atual + i) % n;
            if (seats.get(idx).podeAgirNaRua()) {
                return idx;
            }
        }
        return -1;
    }

    private boolean isTurnoDoJogador() {
        return estagioAtual.isMaoEmAndamento() && seatTurnoAtual == 0;
    }

    private long getValorParaPagar(PokerSeatState seat) {
        if (!estagioAtual.isMaoEmAndamento() || !seat.isAtivoNaMao()) {
            return 0L;
        }
        long dif = Math.max(0L, Math.subtractExact(maiorApostaNaRua, seat.getApostaNaRua()));
        return Math.min(dif, seat.getSaldo());
    }

    private boolean podeSeatAumentar(PokerSeatState seat) {
        if (!estagioAtual.isMaoEmAndamento() || !seat.podeAgirNaRua() || aumentosNaRua >= MAX_AUMENTOS_POR_RUA) {
            return false;
        }
        long paraPagar = getValorParaPagar(seat);
        if (seat.getSaldo() <= paraPagar) {
            return false;
        }
        // Precisa existir ao menos outro jogador ativo que ainda possa responder
        for (PokerSeatState outro : seats) {
            if (outro.getSeatIndex() != seat.getSeatIndex() && outro.podeAgirNaRua()) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // CONSULTAS MULTI-JOGADOR + IMPLEMENTACAO COMPLETA DE PokerEngine
    // =========================================================================

    public List<PokerSeatState> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    public List<PokerSidePot> getPotes() {
        return potesCalculados;
    }

    public int getSeatDealer() {
        return seatDealer;
    }

    public int getSeatSmallBlind() {
        return seatSmallBlind;
    }

    public int getSeatBigBlind() {
        return seatBigBlind;
    }

    public int getSeatTurnoAtual() {
        return seatTurnoAtual;
    }

    public long getMaiorApostaNaRua() {
        return maiorApostaNaRua;
    }

    public long getSomaTotalFichasNaMesa() {
        long soma = poteLiquidado ? 0L : poteTotal;
        for (PokerSeatState s : seats) {
            soma = Math.addExact(soma, s.getSaldo());
        }
        return soma;
    }

    @Override
    public PokerStage getEstagioAtual() {
        return estagioAtual;
    }

    @Override
    public PokerRoundResult getResultadoMao() {
        return resultadoMao;
    }

    @Override
    public PokerParticipant getTurnoAtual() {
        if (!estagioAtual.isMaoEmAndamento() || seatTurnoAtual < 0) {
            return null;
        }
        return seatTurnoAtual == 0 ? PokerParticipant.JOGADOR : PokerParticipant.BOT;
    }

    @Override
    public PokerParticipant getBotaoDealer() {
        return seatDealer == 0 ? PokerParticipant.JOGADOR : PokerParticipant.BOT;
    }

    @Override
    public long getSaldoJogador() {
        return seats.get(0).getSaldo();
    }

    @Override
    public long getSaldoBot() {
        return seats.get(1).getSaldo();
    }

    @Override
    public long getPoteTotal() {
        return poteTotal;
    }

    @Override
    public long getApostaJogadorNaRua() {
        return seats.get(0).getApostaNaRua();
    }

    @Override
    public long getApostaBotNaRua() {
        return maiorApostaNaRua;
    }

    @Override
    public long getTotalComprometidoJogadorNaMao() {
        return seats.get(0).getTotalContribuidoNaMao();
    }

    @Override
    public long getRetornoTotalPotencialVitoriaAtual() {
        if (!estagioAtual.isMaoEmAndamento() || !seats.get(0).isAtivoNaMao()) {
            return 0L;
        }
        long elegivel = 0L;
        for (PokerSidePot pot : potesCalculados) {
            if (pot.getSeatsElegiveis().contains(0)) {
                elegivel = Math.addExact(elegivel, pot.getValor());
            }
        }
        return elegivel;
    }

    @Override
    public long getLucroLiquidoPotencialVitoriaAtual() {
        long ret = getRetornoTotalPotencialVitoriaAtual();
        return Math.max(0L, ret - getTotalComprometidoJogadorNaMao());
    }

    @Override
    public long getRetornoPotencialEmpateAtual() {
        if (!estagioAtual.isMaoEmAndamento() || !seats.get(0).isAtivoNaMao()) {
            return 0L;
        }
        long totalEmpate = 0L;
        for (PokerSidePot pot : potesCalculados) {
            if (pot.getSeatsElegiveis().contains(0)) {
                int qtd = Math.max(1, pot.getSeatsElegiveis().size());
                totalEmpate = Math.addExact(totalEmpate, pot.getValor() / qtd);
            }
        }
        return totalEmpate;
    }

    @Override
    public long getValorParaPagarJogador() {
        return getValorParaPagar(seats.get(0));
    }

    @Override
    public long getSmallBlind() {
        return smallBlind;
    }

    @Override
    public long getBigBlind() {
        return bigBlind;
    }

    @Override
    public long getAumentoMinimoJogador() {
        if (!isTurnoDoJogador() || !podeSeatAumentar(seats.get(0))) {
            return 0L;
        }
        long max = getAumentoMaximoJogador();
        return Math.min(Math.max(bigBlind, ultimoAumentoNaRua), max);
    }

    @Override
    public long getAumentoMaximoJogador() {
        if (!isTurnoDoJogador() || !podeSeatAumentar(seats.get(0))) {
            return 0L;
        }
        PokerSeatState j = seats.get(0);
        return Math.max(0L, Math.subtractExact(j.getSaldo(), getValorParaPagar(j)));
    }

    @Override
    public Set<PokerAction> getAcoesValidasJogador() {
        if (!isTurnoDoJogador()) {
            return Collections.emptySet();
        }
        EnumSet<PokerAction> acoes = EnumSet.of(PokerAction.DESISTIR);
        if (getValorParaPagar(seats.get(0)) == 0L) {
            acoes.add(PokerAction.PASSAR);
        } else {
            acoes.add(PokerAction.PAGAR);
        }
        if (podeSeatAumentar(seats.get(0))) {
            acoes.add(PokerAction.AUMENTAR);
        }
        return Collections.unmodifiableSet(acoes);
    }

    @Override
    public List<Card> getMaoJogador() {
        return seats.get(0).getCartasVisiveis(estagioAtual);
    }

    @Override
    public List<Card> getCartasComunitarias() {
        return List.copyOf(cartasComunitarias);
    }

    @Override
    public List<Card> getCartasVisiveisBot() {
        if (estagioAtual != PokerStage.SHOWDOWN) {
            return List.of();
        }
        // Retorna as cartas do primeiro Bot ativo no Showdown para compatibilidade com PokerEngine
        for (int i = 1; i < seats.size(); i++) {
            List<Card> vis = seats.get(i).getCartasVisiveis(estagioAtual);
            if (!vis.isEmpty()) {
                return vis;
            }
        }
        return List.of();
    }

    @Override
    public boolean hasCartasOcultasBot() {
        for (int i = 1; i < seats.size(); i++) {
            if (seats.get(i).hasCartasOcultas(estagioAtual)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getQuantidadeCartasBot() {
        return seats.get(1).getQuantidadeCartas();
    }

    @Override
    public EvaluatedPokerHand getMelhorMaoAtualJogador() {
        PokerSeatState j = seats.get(0);
        if (estagioAtual == PokerStage.SHOWDOWN) {
            return j.getMaoShowdown(estagioAtual);
        }
        if (j.getQuantidadeCartas() != 2 || cartasComunitarias.size() < 3) {
            return null;
        }
        List<Card> conhecidas = new ArrayList<>(2 + cartasComunitarias.size());
        conhecidas.addAll(j.getMaoInternaSnapshot());
        conhecidas.addAll(cartasComunitarias);
        return PokerHandEvaluator.avaliarMelhorMao(conhecidas);
    }

    @Override
    public EvaluatedPokerHand getMelhorMaoJogadorShowdown() {
        return seats.get(0).getMaoShowdown(estagioAtual);
    }

    @Override
    public EvaluatedPokerHand getMelhorMaoBotShowdown() {
        if (estagioAtual != PokerStage.SHOWDOWN) {
            return null;
        }
        for (int i = 1; i < seats.size(); i++) {
            EvaluatedPokerHand m = seats.get(i).getMaoShowdown(estagioAtual);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    @Override
    public String getUltimaAcaoDescricao() {
        return ultimaAcaoDescricao;
    }
}
