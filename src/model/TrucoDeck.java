package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa um baralho limpo de 40 cartas para Truco Paulista.
 *
 * Exclui 8, 9 e 10 de todos os naipes.
 * Suporta instanciacao aleatoria (para partidas normais) e
 * instanciacao deterministica (para testes unitarios reproduziveis).
 */
public class TrucoDeck {

    private final List<Card> cartas;

    /**
     * Cria e embaralha um novo baralho limpo tradicional de 40 cartas.
     */
    public TrucoDeck() {
        this.cartas = criarBaralhoLimpoPadrao();
        Collections.shuffle(this.cartas);
    }

    /**
     * Cria um baralho com cartas predefinidas para testes deterministicos.
     *
     * @param cartasPredefinidas Lista de cartas em ordem de compra (a primeira sera comprada primeiro).
     */
    public TrucoDeck(List<Card> cartasPredefinidas) {
        if (cartasPredefinidas == null) {
            throw new IllegalArgumentException("A lista de cartas predefinidas nao pode ser nula.");
        }
        this.cartas = new ArrayList<>(cartasPredefinidas);
    }

    /**
     * Gera as 40 cartas do baralho limpo (4 naipes x 10 ranks validos no Truco).
     */
    public static List<Card> criarBaralhoLimpoPadrao() {
        List<Card> lista = new ArrayList<>(40);
        for (Suit suit : Suit.values()) {
            for (Rank rank : TrucoRules.RANKS_TRUCO) {
                lista.add(new Card(suit, rank));
            }
        }
        return lista;
    }

    /**
     * Retira e retorna a proxima carta do topo do baralho.
     */
    public Card comprarCarta() {
        if (cartas.isEmpty()) {
            throw new IllegalStateException("Nao ha cartas restantes no baralho de Truco.");
        }
        return cartas.remove(0);
    }

    /**
     * Distribui uma mao contendo a quantidade de cartas solicitada.
     */
    public List<Card> distribuirMao(int quantidade) {
        if (quantidade < 0 || quantidade > cartas.size()) {
            throw new IllegalArgumentException("Quantidade de cartas invalida para distribuicao: " + quantidade);
        }
        List<Card> mao = new ArrayList<>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            mao.add(comprarCarta());
        }
        return mao;
    }

    /**
     * Retorna a quantidade de cartas restantes no baralho.
     */
    public int getCartasRestantes() {
        return cartas.size();
    }

    /**
     * Verifica se o baralho esta vazio.
     */
    public boolean isVazio() {
        return cartas.isEmpty();
    }

    /**
     * Retorna uma visao imutavel das cartas atualmente no baralho.
     */
    public List<Card> getCartas() {
        return Collections.unmodifiableList(cartas);
    }
}
