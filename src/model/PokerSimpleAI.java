package model;

import java.util.ArrayList;
import java.util.List;

/**
 * IA tatica deterministica e honesta para o Bot de Poker Texas Hold'em Heads-Up.
 *
 * <p>Avalia a forca da mao no Pre-Flop (pares de mao, figuras conectadas/suited, Ases fortes)
 * e no Pos-Flop (usando {@link PokerHandEvaluator#avaliarMelhorMao(List)} sobre as cartas do Bot
 * combinadas estritamente com as comunitarias ja reveladas), tomando decisoes proporcionais ao
 * custo da aposta em relacao ao pote e ao Big Blind, sem jamais enxergar a mao do jogador.</p>
 */
public class PokerSimpleAI implements PokerBotStrategy {

    @Override
    public Decision decidirAcao(List<Card> maoPrivadaBot,
                                List<Card> cartasComunitarias,
                                PokerStage estagio,
                                long poteTotal,
                                long valorParaPagar,
                                long saldoDisponivelBot,
                                long bigBlind,
                                boolean podeAumentar) {
        if (maoPrivadaBot == null || maoPrivadaBot.size() != 2) {
            return valorParaPagar == 0L ? Decision.passar() : Decision.desistir();
        }

        if (estagio == PokerStage.PRE_FLOP || cartasComunitarias == null || cartasComunitarias.size() < 3) {
            return decidirPreFlop(maoPrivadaBot, valorParaPagar, saldoDisponivelBot, bigBlind, podeAumentar);
        }

        return decidirPosFlop(
                maoPrivadaBot,
                cartasComunitarias,
                poteTotal,
                valorParaPagar,
                saldoDisponivelBot,
                bigBlind,
                podeAumentar
        );
    }

    private Decision decidirPreFlop(List<Card> maoBot,
                                    long valorParaPagar,
                                    long saldoBot,
                                    long bigBlind,
                                    boolean podeAumentar) {
        int forca = calcularPontuacaoPreFlop(maoBot.get(0), maoBot.get(1));

        if (valorParaPagar == 0L) {
            if (podeAumentar && forca >= 75 && saldoBot >= bigBlind) {
                return Decision.aumentar(bigBlind);
            }
            return Decision.passar();
        }

        // Mao premium (par alto JJ+, AK, AQ suited): aumenta se possivel ou paga qualquer valor
        if (forca >= 85) {
            long aumento = calcularValorAumentoPadrao(valorParaPagar, saldoBot, bigBlind);
            if (podeAumentar && aumento > 0L) {
                return Decision.aumentar(aumento);
            }
            return Decision.pagar();
        }

        // Mao boa/jogavel (pares baixos/medios, figuras, Ases medios): paga apostas moderadas (ate 4 BBs)
        if (forca >= 50) {
            if (valorParaPagar <= bigBlind * 4L) {
                return Decision.pagar();
            }
            return forca >= 70 ? Decision.pagar() : Decision.desistir();
        }

        // Completar Small Blind barato no Heads-Up com mao especulativa
        if (valorParaPagar <= bigBlind / 2L && forca >= 32) {
            return Decision.pagar();
        }

        return Decision.desistir();
    }

    private Decision decidirPosFlop(List<Card> maoBot,
                                    List<Card> comunitarias,
                                    long poteTotal,
                                    long valorParaPagar,
                                    long saldoBot,
                                    long bigBlind,
                                    boolean podeAumentar) {
        List<Card> conhecidas = new ArrayList<>(2 + comunitarias.size());
        conhecidas.addAll(maoBot);
        conhecidas.addAll(comunitarias);

        EvaluatedPokerHand melhorAtual = PokerHandEvaluator.avaliarMelhorMao(conhecidas);
        PokerHandRank cat = melhorAtual.getCategoria();

        // Verifica tambem se a categoria vem apenas da mesa (ex.: par na mesa sem melhorar a mao do Bot)
        boolean melhoraAlemDaMesa = true;
        if (comunitarias.size() == 5) {
            EvaluatedPokerHand apenasMesa = PokerHandEvaluator.avaliarExatamenteCinco(comunitarias);
            melhoraAlemDaMesa = melhorAtual.compareTo(apenasMesa) > 0;
        }

        if (valorParaPagar == 0L) {
            if (podeAumentar && melhoraAlemDaMesa
                    && cat.getForcaCategoria() >= PokerHandRank.DOIS_PARES.getForcaCategoria()
                    && saldoBot >= bigBlind) {
                return Decision.aumentar(bigBlind);
            }
            return Decision.passar();
        }

        // Maos muito fortes (Trinca ou superior contribuindo alem da mesa): aumenta ou paga tudo
        if (melhoraAlemDaMesa && cat.getForcaCategoria() >= PokerHandRank.TRINCA.getForcaCategoria()) {
            long aumento = calcularValorAumentoPadrao(valorParaPagar, saldoBot, bigBlind);
            if (podeAumentar && aumento > 0L) {
                return Decision.aumentar(aumento);
            }
            return Decision.pagar();
        }

        // Dois pares: paga apostas razoaveis (ate o tamanho do pote ou 6 BBs)
        if (melhoraAlemDaMesa && cat == PokerHandRank.DOIS_PARES) {
            if (valorParaPagar <= Math.max(poteTotal, bigBlind * 6L)) {
                return Decision.pagar();
            }
            return Decision.desistir();
        }

        // Um par real: paga apostas pequenas/medias (ate metade do pote ou 3 BBs)
        if (melhoraAlemDaMesa && cat == PokerHandRank.UM_PAR) {
            int valorPar = melhorAtual.getCriteriosDesempate().get(0);
            long limiteCall = valorPar >= 10 ? Math.max(poteTotal / 2L, bigBlind * 3L) : bigBlind * 2L;
            if (valorParaPagar <= limiteCall) {
                return Decision.pagar();
            }
            return Decision.desistir();
        }

        // Carta alta com As e aposta simbolica
        int maiorCartaBot = Math.max(
                PokerHandEvaluator.valorPoker(maoBot.get(0).getRank()),
                PokerHandEvaluator.valorPoker(maoBot.get(1).getRank())
        );
        if (maiorCartaBot == 14 && valorParaPagar <= bigBlind) {
            return Decision.pagar();
        }

        return Decision.desistir();
    }

    private long calcularValorAumentoPadrao(long valorParaPagar, long saldoBot, long bigBlind) {
        long saldoAposIgualar = saldoBot - valorParaPagar;
        if (saldoAposIgualar < bigBlind) {
            return 0L;
        }
        return bigBlind;
    }

    /**
     * Heuristica deterministica de 0 a 100 para classificar as 2 cartas iniciais do Bot.
     */
    static int calcularPontuacaoPreFlop(Card c1, Card c2) {
        int v1 = PokerHandEvaluator.valorPoker(c1.getRank());
        int v2 = PokerHandEvaluator.valorPoker(c2.getRank());
        int maior = Math.max(v1, v2);
        int menor = Math.min(v1, v2);
        boolean mesmoNaipe = c1.getSuit() == c2.getSuit();

        if (maior == menor) {
            // Par de mao: 22 vale 58, TT vale 82, AA vale 98
            return Math.min(98, 52 + maior * 3 + (maior >= 11 ? 4 : 0));
        }

        int pontos = maior * 3 + menor * 2;
        if (mesmoNaipe) {
            pontos += 6;
        }
        int distancia = maior - menor;
        if (distancia == 1) {
            pontos += 5;
        } else if (distancia == 2) {
            pontos += 2;
        }
        if (maior == 14 && menor >= 12) {
            pontos += 22; // AK / AQ
        } else if (maior == 14 && menor >= 10) {
            pontos += 12; // AJ / AT
        }
        return Math.min(95, pontos);
    }
}
