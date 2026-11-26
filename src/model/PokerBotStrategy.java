package model;

import java.util.Objects;

/**
 * Estrategia de decisao para o Bot no Poker Texas Hold'em Heads-Up.
 *
 * <p><strong>Isolamento de Estado (Jogo Limpo):</strong> a interface recebe exclusivamente
 * as 2 cartas privadas do proprio Bot, as cartas comunitarias ja abertas e os dados publicos
 * de apostas da mesa. A IA <strong>nunca</strong> recebe nem consulta as cartas ocultas do jogador.</p>
 */
public interface PokerBotStrategy {

    /**
     * Representa a decisao imutavel tomada pelo Bot em seu turno.
     */
    final class Decision {
        private final PokerAction acao;
        private final long valorAcimaDaApostaAtual;

        private Decision(PokerAction acao, long valorAcimaDaApostaAtual) {
            this.acao = Objects.requireNonNull(acao, "A acao da decisao do Bot nao pode ser nula.");
            this.valorAcimaDaApostaAtual = valorAcimaDaApostaAtual;
        }

        public static Decision desistir() {
            return new Decision(PokerAction.DESISTIR, 0L);
        }

        public static Decision passar() {
            return new Decision(PokerAction.PASSAR, 0L);
        }

        public static Decision pagar() {
            return new Decision(PokerAction.PAGAR, 0L);
        }

        public static Decision aumentar(long valorAcimaDaApostaAtual) {
            if (valorAcimaDaApostaAtual <= 0L) {
                throw new IllegalArgumentException("O valor de aumento deve ser positivo: " + valorAcimaDaApostaAtual);
            }
            return new Decision(PokerAction.AUMENTAR, valorAcimaDaApostaAtual);
        }

        public PokerAction getAcao() {
            return acao;
        }

        public long getValorAcimaDaApostaAtual() {
            return valorAcimaDaApostaAtual;
        }
    }

    /**
     * Escolhe a acao do Bot com base apenas nas informacoes visiveis ao proprio Bot.
     *
     * @param maoPrivadaBot       snapshot imutavel das 2 cartas do Bot.
     * @param cartasComunitarias  snapshot imutavel das cartas comunitarias abertas (0, 3, 4 ou 5).
     * @param estagio             rua atual ({@code PRE_FLOP}, {@code FLOP}, {@code TURN} ou {@code RIVER}).
     * @param poteTotal           total acumulado no pote.
     * @param valorParaPagar      fichas necessarias para o Bot igualar a aposta atual ({@code >= 0}).
     * @param saldoDisponivelBot  fichas restantes do Bot.
     * @param bigBlind            valor do Big Blind da mesa.
     * @param podeAumentar        {@code true} se o limite de aumentos e os saldos permitirem {@link PokerAction#AUMENTAR}.
     * @return {@link Decision} escolhida pelo Bot.
     */
    Decision decidirAcao(java.util.List<Card> maoPrivadaBot,
                         java.util.List<Card> cartasComunitarias,
                         PokerStage estagio,
                         long poteTotal,
                         long valorParaPagar,
                         long saldoDisponivelBot,
                         long bigBlind,
                         boolean podeAumentar);
}
