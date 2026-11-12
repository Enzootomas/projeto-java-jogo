package model;

/**
 * Representa uma vaza individual em uma mão de Truco.
 * Cada vaza consiste em duas cartas jogadas (uma do Jogador e uma do Bot).
 */
public class TrucoTrick {

    private final int numeroVaza;
    private final TrucoPlayer primeiroAJogar;
    private final Card vira;

    private Card cartaJogador;
    private Card cartaBot;
    private TrucoPlayer proximoAJogar;
    private TrucoTrickResult resultado;

    public TrucoTrick(int numeroVaza, TrucoPlayer primeiroAJogar, Card vira) {
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
        this.proximoAJogar = primeiroAJogar;
        this.vira = vira;
        this.resultado = null;
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
     * Registra a jogada de uma carta por um participante.
     */
    public void registrarJogada(TrucoPlayer player, Card carta) {
        if (isCompleta()) {
            throw new IllegalStateException("A vaza " + numeroVaza + " ja foi finalizada.");
        }
        if (player != proximoAJogar) {
            throw new IllegalStateException("Nao e a vez de " + player + " jogar na vaza " + numeroVaza + ". Vez atual: " + proximoAJogar);
        }
        if (carta == null) {
            throw new IllegalArgumentException("A carta jogada nao pode ser nula.");
        }

        if (player == TrucoPlayer.JOGADOR) {
            this.cartaJogador = carta;
        } else {
            this.cartaBot = carta;
        }

        // Se ambos jogaram, resolve a vaza
        if (cartaJogador != null && cartaBot != null) {
            this.proximoAJogar = null;
            int comparacao = TrucoRules.compararCartas(cartaJogador, cartaBot, vira);
            if (comparacao > 0) {
                this.resultado = TrucoTrickResult.VITORIA_JOGADOR;
            } else if (comparacao < 0) {
                this.resultado = TrucoTrickResult.VITORIA_BOT;
            } else {
                this.resultado = TrucoTrickResult.EMPATE;
            }
        } else {
            this.proximoAJogar = player.getAdversario();
        }
    }
}
