package model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Regras e constantes do Truco Paulista para baralho limpo (40 cartas).
 *
 * Desacopla a logica do Truco de qualquer regra de pontuacao de Blackjack.
 *
 * Hierarquia tradicional das cartas comuns (nao-manilhas):
 * 4 < 5 < 6 < 7 < Q (Dama) < J (Valete) < K (Rei) < A (As) < 2 < 3
 *
 * Sequencia ciclica da vira para determinar a manilha:
 * 4 -> 5 -> 6 -> 7 -> Q -> J -> K -> A -> 2 -> 3 -> 4
 *
 * Desempate das manilhas por naipe (os quatro tombos):
 * Paus (Zap) > Copas (Copeta) > Espadas (Espadilha) > Ouros (Picafumo)
 */
public final class TrucoRules {

    /**
     * Os 10 valores presentes no baralho limpo de Truco (exclui 8, 9 e 10).
     * Indexados do menor valor comum ao maior valor comum.
     */
    public static final List<Rank> RANKS_TRUCO = Collections.unmodifiableList(Arrays.asList(
            Rank.QUATRO,  // Forca 0
            Rank.CINCO,   // Forca 1
            Rank.SEIS,    // Forca 2
            Rank.SETE,    // Forca 3
            Rank.DAMA,    // Forca 4 (No Truco tradicional, Dama e inferior ao Valete)
            Rank.VALETE,  // Forca 5
            Rank.REI,     // Forca 6
            Rank.AS,      // Forca 7
            Rank.DOIS,    // Forca 8
            Rank.TRES     // Forca 9
    ));

    private TrucoRules() {
        // Construtor privado para classe puramente utilitaria
    }

    /**
     * Verifica se um Rank pertence ao baralho limpo de Truco.
     */
    public static boolean isRankValidoNoTruco(Rank rank) {
        return rank != null && RANKS_TRUCO.contains(rank);
    }

    /**
     * Retorna a forca ordinal da carta comum no Truco (0 a 9).
     * Cartas com forcas iguais resultam em empate (canga).
     */
    public static int getForcaCartaComum(Rank rank) {
        int index = RANKS_TRUCO.indexOf(rank);
        if (index < 0) {
            throw new IllegalArgumentException("Rank " + rank + " nao faz parte do baralho limpo de Truco.");
        }
        return index;
    }

    /**
     * Determina o rank da manilha a partir do rank da carta vira.
     * Segue a sequencia ciclica: 4 -> 5 -> 6 -> 7 -> Q -> J -> K -> A -> 2 -> 3 -> 4.
     */
    public static Rank determinarManilha(Rank rankVira) {
        int index = RANKS_TRUCO.indexOf(rankVira);
        if (index < 0) {
            throw new IllegalArgumentException("Carta vira com rank invalido para Truco: " + rankVira);
        }
        int indexManilha = (index + 1) % RANKS_TRUCO.size();
        return RANKS_TRUCO.get(indexManilha);
    }

    /**
     * Retorna o peso do naipe para desempate exclusivo de manilhas.
     * Hierarquia tradicional dos 'quatro tombos':
     * Paus (4 - Zap) > Copas (3 - Copeta) > Espadas (2 - Espadilha) > Ouros (1 - Picafumo).
     */
    public static int getForcaNaipeManilha(Suit suit) {
        if (suit == null) {
            throw new IllegalArgumentException("Naipe nao pode ser nulo.");
        }
        switch (suit) {
            case PAUS:
                return 4; // Zap / Gato (Mais forte)
            case COPAS:
                return 3; // Copeta
            case ESPADAS:
                return 2; // Espadilha
            case OUROS:
                return 1; // Picafumo (Mais fraco entre as manilhas)
            default:
                throw new IllegalArgumentException("Naipe desconhecido: " + suit);
        }
    }

    /**
     * Verifica se a carta informada e uma manilha para a vira atual.
     */
    public static boolean isManilha(Card card, Card vira) {
        if (card == null || vira == null) {
            return false;
        }
        Rank manilhaRank = determinarManilha(vira.getRank());
        return card.getRank() == manilhaRank;
    }

    /**
     * Compara duas cartas no contexto da carta vira fornecida.
     *
     * @param c1 Primeira carta.
     * @param c2 Segunda carta.
     * @param vira Carta vira que define a manilha.
     * @return Valor positivo se c1 vence c2; negativo se c2 vence c1; 0 em caso de empate (canga).
     */
    public static int compararCartas(Card c1, Card c2, Card vira) {
        if (c1 == null || c2 == null || vira == null) {
            throw new IllegalArgumentException("Cartas e vira nao podem ser nulas.");
        }

        boolean c1Manilha = isManilha(c1, vira);
        boolean c2Manilha = isManilha(c2, vira);

        // 1. Ambas sao manilhas: desempate estrito por naipe (nunca empata)
        if (c1Manilha && c2Manilha) {
            return Integer.compare(getForcaNaipeManilha(c1.getSuit()), getForcaNaipeManilha(c2.getSuit()));
        }

        // 2. Apenas uma e manilha: a manilha sempre vence
        if (c1Manilha) {
            return 1;
        }
        if (c2Manilha) {
            return -1;
        }

        // 3. Nenhuma e manilha: compara forca dos ranks comuns
        int forca1 = getForcaCartaComum(c1.getRank());
        int forca2 = getForcaCartaComum(c2.getRank());

        // Ranks iguais em cartas comuns geram empate (naipes sao ignorados em cartas comuns)
        return Integer.compare(forca1, forca2);
    }
}
