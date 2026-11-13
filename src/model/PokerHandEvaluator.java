package model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Avaliador deterministico da melhor mao de 5 cartas do Poker Texas Hold'em.
 *
 * <p>Recebe de 5 a 7 cartas (tipicamente as 2 cartas fechadas do jogador somadas as 5
 * cartas comunitarias) e examina todas as combinacoes de 5 cartas ({@code C(n, 5)}, ate 21
 * combinacoes para 7 cartas), retornando um {@link EvaluatedPokerHand} imutavel com a maior
 * categoria ({@link PokerHandRank}) e os criterios exatos de desempate (kickers).</p>
 */
public final class PokerHandEvaluator {

    private PokerHandEvaluator() {
        // Classe utilitaria estatica
    }

    /**
     * Converte um {@link Rank} do baralho para seu valor numerico oficial no Poker
     * (2 = 2, ..., 10 = 10, Valete = 11, Dama = 12, Rei = 13, As = 14).
     *
     * @param rank valor da carta (nao nulo).
     * @return inteiro de 2 a 14.
     */
    public static int valorPoker(Rank rank) {
        if (rank == null) {
            throw new IllegalArgumentException("O Rank nao pode ser nulo.");
        }
        switch (rank) {
            case DOIS:
                return 2;
            case TRES:
                return 3;
            case QUATRO:
                return 4;
            case CINCO:
                return 5;
            case SEIS:
                return 6;
            case SETE:
                return 7;
            case OITO:
                return 8;
            case NOVE:
                return 9;
            case DEZ:
                return 10;
            case VALETE:
                return 11;
            case DAMA:
                return 12;
            case REI:
                return 13;
            case AS:
                return 14;
            default:
                throw new IllegalStateException("Rank desconhecido: " + rank);
        }
    }

    /**
     * Avalia a melhor mao de 5 cartas dentre 5, 6 ou 7 cartas distintas disponiveis.
     *
     * @param cartasDisponiveis lista contendo de 5 a 7 cartas distintas sem elementos nulos.
     * @return {@link EvaluatedPokerHand} correspondente a melhor combinacao de 5 cartas.
     */
    public static EvaluatedPokerHand avaliarMelhorMao(List<Card> cartasDisponiveis) {
        if (cartasDisponiveis == null) {
            throw new IllegalArgumentException("A lista de cartas para avaliacao nao pode ser nula.");
        }
        int n = cartasDisponiveis.size();
        if (n < 5 || n > 7) {
            throw new IllegalArgumentException(
                    "O avaliador de Texas Hold'em exige entre 5 e 7 cartas (recebido: " + n + ")."
            );
        }

        Set<Card> unicas = new HashSet<>(n);
        for (Card c : cartasDisponiveis) {
            if (c == null) {
                throw new IllegalArgumentException("A lista de cartas nao pode conter elementos nulos.");
            }
            if (!unicas.add(c)) {
                throw new IllegalArgumentException("Cartas duplicadas nao sao permitidas em uma mesma mao: " + c);
            }
        }

        EvaluatedPokerHand melhor = null;

        for (int a = 0; a < n - 4; a++) {
            for (int b = a + 1; b < n - 3; b++) {
                for (int c = b + 1; c < n - 2; c++) {
                    for (int d = c + 1; d < n - 1; d++) {
                        for (int e = d + 1; e < n; e++) {
                            List<Card> cinco = List.of(
                                    cartasDisponiveis.get(a),
                                    cartasDisponiveis.get(b),
                                    cartasDisponiveis.get(c),
                                    cartasDisponiveis.get(d),
                                    cartasDisponiveis.get(e)
                            );
                            EvaluatedPokerHand candidata = avaliarExatamenteCinco(cinco);
                            if (melhor == null || candidata.compareTo(melhor) > 0) {
                                melhor = candidata;
                            }
                        }
                    }
                }
            }
        }

        return melhor;
    }

    /**
     * Avalia exatamente 5 cartas distintas e produz sua classificacao e criterios de desempate.
     *
     * @param cincoCartas lista com exatamente 5 cartas distintas.
     * @return {@link EvaluatedPokerHand} avaliada.
     */
    public static EvaluatedPokerHand avaliarExatamenteCinco(List<Card> cincoCartas) {
        if (cincoCartas == null || cincoCartas.size() != 5) {
            throw new IllegalArgumentException("Esperadas exatamente 5 cartas para avaliacao direta.");
        }

        List<Card> ordenadas = new ArrayList<>(cincoCartas);
        ordenadas.sort(Comparator.comparingInt((Card c) -> valorPoker(c.getRank())).reversed());

        boolean flush = verificarFlush(ordenadas);
        int topoSequencia = detectarTopoSequencia(ordenadas);
        boolean sequencia = topoSequencia > 0;

        if (flush && sequencia) {
            return new EvaluatedPokerHand(
                    PokerHandRank.STRAIGHT_FLUSH,
                    List.of(topoSequencia),
                    ordenadas
            );
        }

        // Contagem de frequencias por valor numerico (2..14)
        int[] contagem = new int[15];
        for (Card c : ordenadas) {
            contagem[valorPoker(c.getRank())]++;
        }

        // Agrupa valores por frequencia (4, 3, 2, 1) do maior valor para o menor
        int quadra = 0;
        int trinca = 0;
        List<Integer> pares = new ArrayList<>(2);
        List<Integer> isoladas = new ArrayList<>(5);

        for (int v = 14; v >= 2; v--) {
            if (contagem[v] == 4) {
                quadra = v;
            } else if (contagem[v] == 3) {
                trinca = v;
            } else if (contagem[v] == 2) {
                pares.add(v);
            } else if (contagem[v] == 1) {
                isoladas.add(v);
            }
        }

        if (quadra > 0) {
            return new EvaluatedPokerHand(
                    PokerHandRank.QUADRA,
                    List.of(quadra, isoladas.get(0)),
                    ordenadas
            );
        }

        if (trinca > 0 && !pares.isEmpty()) {
            return new EvaluatedPokerHand(
                    PokerHandRank.FULL_HOUSE,
                    List.of(trinca, pares.get(0)),
                    ordenadas
            );
        }

        if (flush) {
            return new EvaluatedPokerHand(
                    PokerHandRank.FLUSH,
                    isoladas,
                    ordenadas
            );
        }

        if (sequencia) {
            return new EvaluatedPokerHand(
                    PokerHandRank.SEQUENCIA,
                    List.of(topoSequencia),
                    ordenadas
            );
        }

        if (trinca > 0) {
            return new EvaluatedPokerHand(
                    PokerHandRank.TRINCA,
                    List.of(trinca, isoladas.get(0), isoladas.get(1)),
                    ordenadas
            );
        }

        if (pares.size() == 2) {
            return new EvaluatedPokerHand(
                    PokerHandRank.DOIS_PARES,
                    List.of(pares.get(0), pares.get(1), isoladas.get(0)),
                    ordenadas
            );
        }

        if (pares.size() == 1) {
            return new EvaluatedPokerHand(
                    PokerHandRank.UM_PAR,
                    List.of(pares.get(0), isoladas.get(0), isoladas.get(1), isoladas.get(2)),
                    ordenadas
            );
        }

        return new EvaluatedPokerHand(
                PokerHandRank.CARTA_ALTA,
                isoladas,
                ordenadas
        );
    }

    private static boolean verificarFlush(List<Card> cincoCartas) {
        Suit primeiroNaipe = cincoCartas.get(0).getSuit();
        for (int i = 1; i < cincoCartas.size(); i++) {
            if (cincoCartas.get(i).getSuit() != primeiroNaipe) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retorna a maior carta da sequencia (de 5 ate 14), ou {@code 0} se as 5 cartas nao formarem sequencia.
     * Trata especificamente a sequencia baixa com As (Wheel: A-5-4-3-2 -> topo 5).
     */
    private static int detectarTopoSequencia(List<Card> cincoOrdenadasDecrescente) {
        int v0 = valorPoker(cincoOrdenadasDecrescente.get(0).getRank());
        int v1 = valorPoker(cincoOrdenadasDecrescente.get(1).getRank());
        int v2 = valorPoker(cincoOrdenadasDecrescente.get(2).getRank());
        int v3 = valorPoker(cincoOrdenadasDecrescente.get(3).getRank());
        int v4 = valorPoker(cincoOrdenadasDecrescente.get(4).getRank());

        // Sequencia normal decrescente sem repeticao (ex.: 14-13-12-11-10 ou 9-8-7-6-5)
        if (v0 - v1 == 1 && v1 - v2 == 1 && v2 - v3 == 1 && v3 - v4 == 1) {
            return v0;
        }

        // Sequencia baixa (Wheel / Steel Wheel): A (14), 5, 4, 3, 2 -> carta mais alta da sequencia e 5
        if (v0 == 14 && v1 == 5 && v2 == 4 && v3 == 3 && v4 == 2) {
            return 5;
        }

        return 0;
    }
}
