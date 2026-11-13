package model;

import java.util.List;
import java.util.Objects;

/**
 * Representa o resultado imutavel e comparavel da melhor mao de 5 cartas de Poker
 * avaliada para um participante.
 *
 * <p>Implementa {@link Comparable} aplicando primeiro a forca da categoria ({@link PokerHandRank})
 * e, em caso de empate na mesma categoria, comparando lexicograficamente a lista ordenada de
 * criterios de desempate ({@code criteriosDesempate} / kickers). Naipes nunca desempatam maos
 * no Texas Hold'em.</p>
 */
public final class EvaluatedPokerHand implements Comparable<EvaluatedPokerHand> {

    private final PokerHandRank categoria;
    private final List<Integer> criteriosDesempate;
    private final List<Card> cincoCartasEscolhidas;

    /**
     * Cria uma representacao imutavel de mao avaliada de 5 cartas.
     *
     * @param categoria             categoria oficial ({@link PokerHandRank}).
     * @param criteriosDesempate    valores inteiros decrescentes para desempate (ex.: valor do par + 3 kickers).
     * @param cincoCartasEscolhidas as 5 cartas que compoem esta melhor mao.
     */
    public EvaluatedPokerHand(PokerHandRank categoria,
                              List<Integer> criteriosDesempate,
                              List<Card> cincoCartasEscolhidas) {
        if (categoria == null) {
            throw new IllegalArgumentException("A categoria da mao de Poker nao pode ser nula.");
        }
        if (criteriosDesempate == null || criteriosDesempate.isEmpty()) {
            throw new IllegalArgumentException("Os criterios de desempate nao podem ser nulos ou vazios.");
        }
        if (cincoCartasEscolhidas == null || cincoCartasEscolhidas.size() != 5) {
            throw new IllegalArgumentException("Uma mao avaliada de Poker deve conter exatamente 5 cartas.");
        }
        for (Integer criterio : criteriosDesempate) {
            if (criterio == null) {
                throw new IllegalArgumentException("Criterios de desempate nao podem conter valores nulos.");
            }
        }
        for (Card carta : cincoCartasEscolhidas) {
            if (carta == null) {
                throw new IllegalArgumentException("As 5 cartas escolhidas nao podem conter elementos nulos.");
            }
        }
        this.categoria = categoria;
        this.criteriosDesempate = List.copyOf(criteriosDesempate);
        this.cincoCartasEscolhidas = List.copyOf(cincoCartasEscolhidas);
    }

    public PokerHandRank getCategoria() {
        return categoria;
    }

    /**
     * Retorna um snapshot imutavel dos criterios numericos de desempate em ordem de prioridade.
     *
     * @return lista imutavel de valores (2 a 14, ou 5 na sequencia baixa A-2-3-4-5).
     */
    public List<Integer> getCriteriosDesempate() {
        return criteriosDesempate;
    }

    /**
     * Retorna um snapshot imutavel das 5 cartas que formam esta melhor combinacao.
     *
     * @return lista imutavel com exatamente 5 {@link Card}.
     */
    public List<Card> getCincoCartasEscolhidas() {
        return cincoCartasEscolhidas;
    }

    /**
     * Indica se a mao corresponde a um Royal Flush (Straight Flush terminado em As: 10-J-Q-K-A).
     *
     * @return {@code true} se for Royal Flush.
     */
    public boolean isRoyalFlush() {
        return categoria == PokerHandRank.STRAIGHT_FLUSH
                && !criteriosDesempate.isEmpty()
                && criteriosDesempate.get(0) == 14;
    }

    /**
     * Retorna uma descricao amigavel em portugues da mao (ex.: "Royal Flush" ou "Full House").
     *
     * @return nome descritivo para exibicao na interface.
     */
    public String getDescricaoCurta() {
        if (isRoyalFlush()) {
            return "Royal Flush";
        }
        return categoria.getNomeEmPortugues();
    }

    @Override
    public int compareTo(EvaluatedPokerHand outra) {
        Objects.requireNonNull(outra, "Nao e possivel comparar com EvaluatedPokerHand nula.");
        int compCategoria = Integer.compare(
                this.categoria.getForcaCategoria(),
                outra.categoria.getForcaCategoria()
        );
        if (compCategoria != 0) {
            return compCategoria;
        }
        int limite = Math.min(this.criteriosDesempate.size(), outra.criteriosDesempate.size());
        for (int i = 0; i < limite; i++) {
            int comp = Integer.compare(this.criteriosDesempate.get(i), outra.criteriosDesempate.get(i));
            if (comp != 0) {
                return comp;
            }
        }
        return Integer.compare(this.criteriosDesempate.size(), outra.criteriosDesempate.size());
    }

    @Override
    public String toString() {
        return getDescricaoCurta() + " " + cincoCartasEscolhidas;
    }
}
