package model;

import java.util.Objects;

/**
 * Representa os dados imutáveis de consulta de uma vaza individual de Truco.
 *
 * É uma classe de valor imutável para leitura segura do estado da vaza,
 * impedindo que agentes externos alterem as cartas, o turno ou o resultado da mão.
 * O registro e processamento de jogadas é centralizado exclusivamente em TrucoRound.
 */
public final class TrucoTrick {

    private final int numeroVaza;
    private final TrucoPlayer primeiroAJogar;
    private final Card vira;
    private final Card cartaJogador;
    private final Card cartaBot;
    private final TrucoPlayer proximoAJogar;
    private final TrucoTrickResult resultado;

    /**
     * Cria uma nova vaza no estado inicial (sem cartas jogadas).
     */
    public TrucoTrick(int numeroVaza, TrucoPlayer primeiroAJogar, Card vira) {
        this(numeroVaza, primeiroAJogar, vira, null, null, primeiroAJogar, null);
    }

    /**
     * Construtor completo para snapshot imutável da vaza.
     */
    public TrucoTrick(int numeroVaza, TrucoPlayer primeiroAJogar, Card vira,
                      Card cartaJogador, Card cartaBot,
                      TrucoPlayer proximoAJogar, TrucoTrickResult resultado) {
        if (numeroVaza < 1 || numeroVaza > 3) {
            throw new IllegalArgumentException("Numero da vaza invalido (deve ser 1, 2 ou 3): " + numeroVaza);
        }
        if (primeiroAJogar == null) {
            throw new IllegalArgumentException("Primeiro a jogar nao pode ser nulo.");
        }
        if (vira == null) {
            throw new IllegalArgumentException("A carta vira nao pode ser nula.");
        }
        this.numeroVaza = numeroVaza;
        this.primeiroAJogar = primeiroAJogar;
        this.vira = vira;
        this.cartaJogador = cartaJogador;
        this.cartaBot = cartaBot;
        this.proximoAJogar = proximoAJogar;
        this.resultado = resultado;
    }

    public int getNumeroVaza() {
        return numeroVaza;
    }

    public TrucoPlayer getPrimeiroAJogar() {
        return primeiroAJogar;
    }

    public TrucoPlayer getSegundoAJogar() {
        return primeiroAJogar.getAdversario();
    }

    public TrucoPlayer getProximoAJogar() {
        return proximoAJogar;
    }

    public Card getVira() {
        return vira;
    }

    public Card getCartaJogador() {
        return cartaJogador;
    }

    public Card getCartaBot() {
        return cartaBot;
    }

    public Card getCarta(TrucoPlayer player) {
        if (player == TrucoPlayer.JOGADOR) {
            return cartaJogador;
        } else if (player == TrucoPlayer.BOT) {
            return cartaBot;
        }
        return null;
    }

    public boolean isCompleta() {
        return cartaJogador != null && cartaBot != null;
    }

    public TrucoTrickResult getResultado() {
        return resultado;
    }

    /**
     * Retorna o jogador vencedor da vaza, ou null caso tenha ocorrido empate (canga)
     * ou a vaza ainda nao esteja completa.
     */
    public TrucoPlayer getVencedor() {
        if (resultado == TrucoTrickResult.VITORIA_JOGADOR) {
            return TrucoPlayer.JOGADOR;
        }
        if (resultado == TrucoTrickResult.VITORIA_BOT) {
            return TrucoPlayer.BOT;
        }
        return null;
    }

    /**
     * Produz uma nova instância imutável da vaza aplicando a jogada informada.
     * Visibilidade de pacote (package-private) para assegurar que apenas o motor
     * TrucoRound gerencie o fluxo de transições.
     */
    TrucoTrick comJogada(TrucoPlayer player, Card carta) {
        if (isCompleta()) {
            throw new IllegalStateException("A vaza " + numeroVaza + " ja foi finalizada.");
        }
        if (player != proximoAJogar) {
            throw new IllegalStateException("Nao e a vez de " + player + " jogar na vaza " + numeroVaza + ". Vez atual: " + proximoAJogar);
        }
        if (carta == null) {
            throw new IllegalArgumentException("A carta jogada nao pode ser nula.");
        }

        Card novaCartaJogador = (player == TrucoPlayer.JOGADOR) ? carta : this.cartaJogador;
        Card novaCartaBot = (player == TrucoPlayer.BOT) ? carta : this.cartaBot;

        TrucoPlayer novoProximo;
        TrucoTrickResult novoResultado = null;

        // Se ambos jogaram, resolve o resultado da vaza
        if (novaCartaJogador != null && novaCartaBot != null) {
            novoProximo = null;
            int comparacao = TrucoRules.compararCartas(novaCartaJogador, novaCartaBot, vira);
            if (comparacao > 0) {
                novoResultado = TrucoTrickResult.VITORIA_JOGADOR;
            } else if (comparacao < 0) {
                novoResultado = TrucoTrickResult.VITORIA_BOT;
            } else {
                novoResultado = TrucoTrickResult.EMPATE;
            }
        } else {
            novoProximo = player.getAdversario();
        }

        return new TrucoTrick(numeroVaza, primeiroAJogar, vira, novaCartaJogador, novaCartaBot, novoProximo, novoResultado);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrucoTrick that = (TrucoTrick) o;
        return numeroVaza == that.numeroVaza &&
                primeiroAJogar == that.primeiroAJogar &&
                Objects.equals(vira, that.vira) &&
                Objects.equals(cartaJogador, that.cartaJogador) &&
                Objects.equals(cartaBot, that.cartaBot) &&
                proximoAJogar == that.proximoAJogar &&
                resultado == that.resultado;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroVaza, primeiroAJogar, vira, cartaJogador, cartaBot, proximoAJogar, resultado);
    }

    @Override
    public String toString() {
        return "TrucoTrick{" +
                "numeroVaza=" + numeroVaza +
                ", primeiro=" + primeiroAJogar +
                ", cartaJ=" + (cartaJogador != null ? cartaJogador.getSiglaVisual() : "-") +
                ", cartaB=" + (cartaBot != null ? cartaBot.getSiglaVisual() : "-") +
                ", resultado=" + resultado +
                '}';
    }
}
