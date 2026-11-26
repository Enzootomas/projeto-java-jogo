package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa o estado encapsulado de um assento na mesa de Poker Texas Hold'em (ate 6 participantes).
 * As cartas privadas dos Bots jamais sao expostas fora do Showdown.
 */
public class PokerSeatState {

    private final int seatIndex;
    private final String nome;
    private final boolean humano;
    private final List<Card> maoPrivada;

    private long saldo;
    private long apostaNaRua;
    private long totalContribuidoNaMao;
    private boolean desistiu;
    private boolean allIn;
    private boolean eliminado;
    private boolean jaAgiuNaRua;
    private String ultimaAcao;
    private EvaluatedPokerHand maoShowdown;

    public PokerSeatState(int seatIndex, String nome, boolean humano, long saldoInicial) {
        if (saldoInicial < 0L) {
            throw new IllegalArgumentException("Saldo inicial nao pode ser negativo: " + saldoInicial);
        }
        this.seatIndex = seatIndex;
        this.nome = Objects.requireNonNull(nome, "Nome do assento nao pode ser nulo.");
        this.humano = humano;
        this.saldo = saldoInicial;
        this.maoPrivada = new ArrayList<>(2);
        this.ultimaAcao = "";
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public String getNome() {
        return nome;
    }

    public boolean isHumano() {
        return humano;
    }

    public long getSaldo() {
        return saldo;
    }

    public long getApostaNaRua() {
        return apostaNaRua;
    }

    public long getTotalContribuidoNaMao() {
        return totalContribuidoNaMao;
    }

    public boolean isDesistiu() {
        return desistiu;
    }

    public boolean isAllIn() {
        return allIn;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public boolean isAtivoNaMao() {
        return !eliminado && !desistiu;
    }

    public boolean podeAgirNaRua() {
        return isAtivoNaMao() && !allIn && saldo > 0L;
    }

    public boolean isJaAgiuNaRua() {
        return jaAgiuNaRua;
    }

    public String getUltimaAcao() {
        return ultimaAcao;
    }

    public int getQuantidadeCartas() {
        return maoPrivada.size();
    }

    /**
     * Retorna as cartas visiveis deste participante para a interface grafica:
     * - Jogador humano sempre enxerga suas proprias 2 cartas.
     * - Bots so tem suas cartas reveladas no {@link PokerStage#SHOWDOWN} quando nao desistiram.
     */
    public List<Card> getCartasVisiveis(PokerStage estagio) {
        if (humano) {
            return List.copyOf(maoPrivada);
        }
        if (estagio == PokerStage.SHOWDOWN && !desistiu && !eliminado) {
            return List.copyOf(maoPrivada);
        }
        return List.of();
    }

    public boolean hasCartasOcultas(PokerStage estagio) {
        if (humano || maoPrivada.isEmpty()) {
            return false;
        }
        return !(estagio == PokerStage.SHOWDOWN && !desistiu && !eliminado);
    }

    public EvaluatedPokerHand getMaoShowdown(PokerStage estagio) {
        if (estagio != PokerStage.SHOWDOWN || desistiu || eliminado) {
            return null;
        }
        return maoShowdown;
    }

    // Metodos package-private exclusivos do motor TexasHoldemTableGame
    void prepararParaNovaMao() {
        limparMaoComLacoReverso();
        this.apostaNaRua = 0L;
        this.totalContribuidoNaMao = 0L;
        this.desistiu = false;
        this.allIn = false;
        this.eliminado = (this.saldo <= 0L);
        this.jaAgiuNaRua = false;
        this.ultimaAcao = this.eliminado ? "Eliminado" : "";
        this.maoShowdown = null;
    }

    void prepararParaNovaRua() {
        this.apostaNaRua = 0L;
        this.jaAgiuNaRua = false;
        if (isAtivoNaMao() && !allIn) {
            this.ultimaAcao = "";
        }
    }

    void adicionarCarta(Card carta) {
        this.maoPrivada.add(Objects.requireNonNull(carta));
    }

    long colocarFichas(long solicitado) {
        if (solicitado <= 0L || saldo <= 0L) {
            return 0L;
        }
        long efetivo = Math.min(solicitado, saldo);
        this.saldo = Math.subtractExact(this.saldo, efetivo);
        this.apostaNaRua = Math.addExact(this.apostaNaRua, efetivo);
        this.totalContribuidoNaMao = Math.addExact(this.totalContribuidoNaMao, efetivo);
        if (this.saldo == 0L) {
            this.allIn = true;
        }
        return efetivo;
    }

    void devolverExcesso(long excesso) {
        if (excesso <= 0L) {
            return;
        }
        this.saldo = Math.addExact(this.saldo, excesso);
        this.apostaNaRua = Math.max(0L, Math.subtractExact(this.apostaNaRua, excesso));
        this.totalContribuidoNaMao = Math.max(0L, Math.subtractExact(this.totalContribuidoNaMao, excesso));
        if (this.saldo > 0L) {
            this.allIn = false;
        }
    }

    void creditarPremio(long premio) {
        if (premio > 0L) {
            this.saldo = Math.addExact(this.saldo, premio);
        }
    }

    void marcarDesistencia() {
        this.desistiu = true;
        this.jaAgiuNaRua = true;
        this.ultimaAcao = "Desistiu";
    }

    void setJaAgiuNaRua(boolean jaAgiu) {
        this.jaAgiuNaRua = jaAgiu;
    }

    void setUltimaAcao(String texto) {
        this.ultimaAcao = (texto != null) ? texto : "";
    }

    void setMaoShowdown(EvaluatedPokerHand mao) {
        this.maoShowdown = mao;
    }

    List<Card> getMaoInternaSnapshot() {
        return List.copyOf(maoPrivada);
    }

    void limparMaoComLacoReverso() {
        for (int i = maoPrivada.size() - 1; i >= 0; i--) {
            maoPrivada.remove(i);
        }
    }
}
