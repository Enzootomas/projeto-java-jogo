package model;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

/**
 * Representa um baralho de cartas para o motor de regras do Blackjack.
 *
 * <p><strong>Conceitos de POO aplicados:</strong></p>
 * <ul>
 *   <li><strong>Abstracao:</strong> expoe operacoes de alto nivel do dominio
 *       ({@link #comprarCarta()}, {@link #embaralhar()}, {@link #reiniciarBaralhoPadrao()})
 *       ocultando a estrutura interna da pilha de cartas.</li>
 *   <li><strong>Encapsulamento:</strong> a lista interna {@code cartas} e privada; consultas
 *       retornam snapshots imutaveis via {@link List#copyOf(Collection)} e a compra de uma
 *       carta remove diretamente do topo (ultimo indice da lista), usando laco reverso quando
 *       precisa remover elementos iterando por indice (como em {@link #limparComLacoReverso()}).</li>
 *   <li><strong>Polimorfismo por Subtipagem ({@link SecureRandom} extends {@link Random}):</strong>
 *       em producao utiliza {@link SecureRandom} para embaralhamento criptograficamente forte,
 *       enquanto aceita qualquer {@link Random} com semente fixa ou sequencia deterministica
 *       nos construtores voltados a testes automatizados.</li>
 * </ul>
 */
public class Deck {

    /** Quantidade oficial de cartas em um baralho frances padrao (4 naipes x 13 valores). */
    public static final int TOTAL_CARTAS_PADRAO = Suit.values().length * Rank.values().length;

    private final List<Card> cartas;
    private final Random random;

    /**
     * Cria um baralho padrao completo com as 52 cartas utilizando {@link SecureRandom}
     * como fonte de aleatoriedade de producao.
     */
    public Deck() {
        this(new SecureRandom());
    }

    /**
     * Cria um baralho padrao completo de 52 cartas associado a um gerador aleatorio especifico.
     * Util para testes reproduziveis com semente fixa ({@code new Random(seed)}).
     *
     * @param random gerador de numeros aleatorios (nao pode ser nulo).
     */
    public Deck(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("O gerador Random nao pode ser nulo.");
        }
        this.random = random;
        this.cartas = new ArrayList<>(TOTAL_CARTAS_PADRAO);
        preencherBaralhoPadrao();
    }

    /**
     * Cria um baralho deterministico contendo exatamente a sequencia de cartas informada.
     *
     * <p>A <strong>primeira carta</strong> da lista fornecida sera a <strong>primeira carta comprada</strong>
     * em {@link #comprarCarta()}, facilitando a leitura natural de cenarios de teste
     * (ex.: carta 1 -> jogador, carta 2 -> dealer, etc.).</p>
     *
     * @param sequenciaTopoParaBase lista de cartas na ordem de compra (nao pode ser nula nem conter nulos).
     */
    public Deck(List<Card> sequenciaTopoParaBase) {
        this(sequenciaTopoParaBase, new SecureRandom());
    }

    /**
     * Cria um baralho deterministico com sequencia predefinida e gerador aleatorio customizado.
     *
     * @param sequenciaTopoParaBase lista de cartas na ordem em que serao compradas.
     * @param random                gerador de numeros aleatorios para eventual embaralhamento.
     */
    public Deck(List<Card> sequenciaTopoParaBase, Random random) {
        if (sequenciaTopoParaBase == null) {
            throw new IllegalArgumentException("A sequencia inicial de cartas nao pode ser nula.");
        }
        if (random == null) {
            throw new IllegalArgumentException("O gerador Random nao pode ser nulo.");
        }
        for (Card carta : sequenciaTopoParaBase) {
            if (carta == null) {
                throw new IllegalArgumentException("O baralho nao pode conter cartas nulas.");
            }
        }
        this.random = random;
        this.cartas = new ArrayList<>(sequenciaTopoParaBase.size());
        // Armazenamos internamente em ordem reversa para que o "topo" do baralho fique
        // no ultimo indice (size - 1), permitindo remocao O(1) e uso seguro de laco reverso.
        for (int i = sequenciaTopoParaBase.size() - 1; i >= 0; i--) {
            this.cartas.add(sequenciaTopoParaBase.get(i));
        }
    }

    /**
     * Limpa o monte atual (usando laco reverso conforme convencao do projeto)
     * e recria todas as 52 combinacoes unicas de {@link Suit} e {@link Rank}.
     */
    public final void reiniciarBaralhoPadrao() {
        limparComLacoReverso();
        preencherBaralhoPadrao();
    }

    /**
     * Embaralha as cartas atualmente presentes no baralho utilizando Fisher-Yates
     * (via {@link Collections#shuffle(List, Random)}).
     */
    public void embaralhar() {
        Collections.shuffle(this.cartas, this.random);
    }

    /**
     * Remove e retorna a carta que esta no topo do baralho.
     *
     * @return a {@link Card} comprada do topo.
     * @throws NoSuchElementException se o baralho estiver vazio.
     */
    public Card comprarCarta() {
        if (estaVazio()) {
            throw new NoSuchElementException("Nao ha cartas restantes no baralho para comprar.");
        }
        int indiceTopo = this.cartas.size() - 1;
        return this.cartas.remove(indiceTopo);
    }

    /**
     * Devolve uma carta ao topo do baralho (sera a proxima carta retornada em {@link #comprarCarta()}).
     *
     * <p>Metodo com visibilidade de pacote ({@code package-private}) utilizado exclusivamente pelo
     * motor de regras para restaurar atomicamente o estado do baralho caso uma sequencia deterministica
     * de multiplas compras da banca se esgote no meio da operacao.</p>
     *
     * @param carta carta a devolver ao topo (nao pode ser nula).
     */
    void devolverCartaAoTopo(Card carta) {
        if (carta == null) {
            throw new IllegalArgumentException("Nao e permitido devolver uma carta nula ao baralho.");
        }
        this.cartas.add(carta);
    }

    /**
     * Informa quantas cartas ainda restam para compra no baralho.
     *
     * @return quantidade de cartas restantes ({@code >= 0}).
     */
    public int cartasRestantes() {
        return this.cartas.size();
    }

    /**
     * Verifica se o baralho nao possui mais cartas disponiveis.
     *
     * @return {@code true} se {@code cartasRestantes() == 0}; {@code false} caso contrario.
     */
    public boolean estaVazio() {
        return this.cartas.isEmpty();
    }

    /**
     * Retorna um snapshot imutavel das cartas restantes na ordem exata de compra
     * (indice 0 = proxima carta a ser comprada no topo).
     *
     * <p>Usa {@link List#copyOf(Collection)} sobre uma lista temporaria ordenada do topo
     * para a base, garantindo que alteracoes futuras no {@code Deck} nao afetem o snapshot
     * retornado e que o chamador nao consiga modificar a lista.</p>
     *
     * @return lista imutavel de {@link Card} na ordem de compra.
     */
    public List<Card> getCartasRestantesSnapshot() {
        List<Card> ordemDeCompra = new ArrayList<>(this.cartas.size());
        for (int i = this.cartas.size() - 1; i >= 0; i--) {
            ordemDeCompra.add(this.cartas.get(i));
        }
        return List.copyOf(ordemDeCompra);
    }

    private void preencherBaralhoPadrao() {
        Suit[] naipes = Suit.values();
        Rank[] valores = Rank.values();
        // Inserimos de tras para frente para que a primeira combinacao (COPAS, AS)
        // fique no topo caso nao haja embaralhamento.
        for (int i = naipes.length - 1; i >= 0; i--) {
            for (int j = valores.length - 1; j >= 0; j--) {
                this.cartas.add(Objects.requireNonNull(new Card(naipes[i], valores[j])));
            }
        }
    }

    private void limparComLacoReverso() {
        for (int i = this.cartas.size() - 1; i >= 0; i--) {
            this.cartas.remove(i);
        }
    }
}
