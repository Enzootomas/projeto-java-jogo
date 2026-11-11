package view;

import model.BlackjackEngine;
import model.Card;
import model.Rank;
import model.RoundResult;
import model.RoundState;
import model.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * =====================================================================================
 * AVISO DIDATICO IMPORTANTE:
 * Esta classe e EXCLUSIVAMENTE um SIMULADOR/MOCK VISUAL criado para viabilizar o
 * desenvolvimento e teste da Interface Grafica (View da mesa de Blackjack) nesta frente.
 *
 * ELA NAO IMPLEMENTA AS REGRAS OFICIAIS DE BARALHO (Deck), MAO (Hand) OU DEALER.
 * O motor de regras real e oficial sera implementado pelo outro integrante da dupla
 * na classe BlackjackGame (pacote model), respeitando o contrato BlackjackEngine.
 * =====================================================================================
 */
public class BlackjackMockSimulator implements BlackjackEngine {

    private long saldoVirtual = 50000L;
    private long apostaAtual = 0L;
    private RoundState estadoRodada = RoundState.AGUARDANDO_APOSTA;
    private RoundResult resultadoRodada = RoundResult.EM_ANDAMENTO;

    private final List<Card> maoJogador = new ArrayList<>();
    private final List<Card> cartasDealer = new ArrayList<>();
    private boolean cartaOcultaPresente = false;

    // Fila mock de cartas para simular compras consecutivas sem depender de baralho real
    private int indiceMockCompra = 0;
    private final Card[] cartasMockCompra = {
            new Card(Suit.OUROS, Rank.CINCO),
            new Card(Suit.ESPADAS, Rank.TRES),
            new Card(Suit.COPAS, Rank.SETE),
            new Card(Suit.PAUS, Rank.DEZ)
    };

    public BlackjackMockSimulator() {
        // Inicializa com estado limpo aguardando aposta
    }

    @Override
    public boolean iniciarPartida(long valorAposta) {
        // Validacao do contrato: apenas em AGUARDANDO_APOSTA ou FINALIZADA
        if (estadoRodada != RoundState.AGUARDANDO_APOSTA && estadoRodada != RoundState.FINALIZADA) {
            return false;
        }

        // Validacao do contrato: valor positivo, par e menor/igual ao saldo
        if (valorAposta <= 0 || valorAposta % 2 != 0 || valorAposta > saldoVirtual) {
            return false;
        }

        this.apostaAtual = valorAposta;
        this.saldoVirtual -= valorAposta;
        this.maoJogador.clear();
        this.cartasDealer.clear();
        this.indiceMockCompra = 0;

        // Simula distribuicao inicial tipica:
        // Jogador: As de Espadas (11) + Oito de Copas (8) = 19 pontos
        this.maoJogador.add(new Card(Suit.ESPADAS, Rank.AS));
        this.maoJogador.add(new Card(Suit.COPAS, Rank.OITO));

        // Dealer: Rei de Paus (10) aberto + Dama de Ouros (10) oculta
        this.cartasDealer.add(new Card(Suit.PAUS, Rank.REI));
        this.cartasDealer.add(new Card(Suit.OUROS, Rank.DAMA));
        this.cartaOcultaPresente = true;

        this.estadoRodada = RoundState.TURNO_JOGADOR;
        this.resultadoRodada = RoundResult.EM_ANDAMENTO;
        return true;
    }

    @Override
    public boolean pedirCarta() {
        if (estadoRodada != RoundState.TURNO_JOGADOR) {
            return false;
        }

        Card novaCarta = cartasMockCompra[indiceMockCompra % cartasMockCompra.length];
        indiceMockCompra++;
        maoJogador.add(novaCarta);

        // Se passar de 21 no calculo simulado, finaliza com estouro
        if (getPontosJogador() > 21) {
            this.estadoRodada = RoundState.FINALIZADA;
            this.resultadoRodada = RoundResult.JOGADOR_ESTOUROU;
            this.cartaOcultaPresente = false; // revela banca
        }
        return true;
    }

    @Override
    public void parar() {
        if (estadoRodada != RoundState.TURNO_JOGADOR) {
            return;
        }

        // Ao parar, revela a carta oculta e apura resultado simulado
        this.cartaOcultaPresente = false;
        this.estadoRodada = RoundState.FINALIZADA;

        int pontosJ = getPontosJogador();
        int pontosD = getPontosDealerVisiveis();

        if (pontosJ > pontosD || pontosD > 21) {
            this.resultadoRodada = RoundResult.JOGADOR_VITORIA;
            this.saldoVirtual += resultadoRodada.calcularRetornoTotal(apostaAtual);
        } else if (pontosJ == pontosD) {
            this.resultadoRodada = RoundResult.EMPATE_PUSH;
            this.saldoVirtual += resultadoRodada.calcularRetornoTotal(apostaAtual);
        } else {
            this.resultadoRodada = RoundResult.DEALER_VITORIA;
        }
    }

    @Override
    public long getSaldoVirtual() {
        return saldoVirtual;
    }

    @Override
    public long getApostaAtual() {
        return apostaAtual;
    }

    @Override
    public RoundState getEstadoRodada() {
        return estadoRodada;
    }

    @Override
    public RoundResult getResultadoRodada() {
        return resultadoRodada;
    }

    @Override
    public List<Card> getMaoJogador() {
        return Collections.unmodifiableList(new ArrayList<>(maoJogador));
    }

    @Override
    public List<Card> getCartasVisiveisDealer() {
        if (cartasDealer.isEmpty()) {
            return Collections.emptyList();
        }
        // Se a carta oculta estiver presente, retorna exclusivamente a primeira carta aberta
        if (cartaOcultaPresente) {
            return Collections.unmodifiableList(Collections.singletonList(cartasDealer.get(0)));
        }
        return Collections.unmodifiableList(new ArrayList<>(cartasDealer));
    }

    @Override
    public boolean hasCartaOcultaDealer() {
        return cartaOcultaPresente;
    }

    @Override
    public int getPontosJogador() {
        int total = 0;
        int ases = 0;
        for (Card c : maoJogador) {
            total += c.getValorBlackjack();
            if (c.isAs()) ases++;
        }
        while (total > 21 && ases > 0) {
            total -= 10;
            ases--;
        }
        return total;
    }

    @Override
    public int getPontosDealerVisiveis() {
        List<Card> visiveis = getCartasVisiveisDealer();
        int total = 0;
        int ases = 0;
        for (Card c : visiveis) {
            total += c.getValorBlackjack();
            if (c.isAs()) ases++;
        }
        while (total > 21 && ases > 0) {
            total -= 10;
            ases--;
        }
        return total;
    }

    /**
     * Metodo utilitario do simulador para permitir a interface recarregar fichas caso zerem.
     */
    public void adicionarFichas(long quantidade) {
        if (quantidade > 0) {
            this.saldoVirtual += quantidade;
        }
    }
}
