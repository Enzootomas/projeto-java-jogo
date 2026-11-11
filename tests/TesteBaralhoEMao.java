package tests;

import model.Card;
import model.Deck;
import model.Hand;
import model.Rank;
import model.Suit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

/**
 * Bateria de testes deterministicos para Deck e Hand (Motor de Regras - Tarefa 1).
 *
 * Verifica:
 * - Integridade do baralho padrao de 52 cartas (sem duplicatas, todos os naipes e valores).
 * - Preservacao das 52 cartas apos embaralhamento com semente controlada (Random(seed)) e reinicio.
 * - Esgotamento do baralho e excecao controlada ao tentar comprar com monte vazio.
 * - Baralho deterministico com sequencia predefinida para testes controlados.
 * - Pontuacao de Hand com 0, 1, 2, 3 e 4 Ases (conversao automatica 11 -> 1).
 * - Diferenca estrita entre Blackjack Natural (21 com 2 cartas) e 21 comum (3+ cartas).
 * - Deteccao de estouro (Bust > 21) e identificacao de mao soft vs hard.
 * - Imutabilidade real dos snapshots via List.copyOf (desacoplados de mutacoes futuras e bloqueados contra edicao).
 */
public class TesteBaralhoEMao {

    private static int totalTestes = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DETERMINISTICOS DE DECK E HAND (BLACKJACK)");
        System.out.println("====================================================\n");

        testarIntegridadeBaralhoPadrao();
        testarEmbaralhamentoEReinicioDeterministico();
        testarEsgotamentoESequenciaControlada();
        testarSnapshotsImutaveisDeckEHand();
        testarPontuacaoComMultiplosAses();
        testarBlackjackNaturalVersus21Comum();
        testarEstouroELimpezaDaMao();

        System.out.println("\n----------------------------------------------------");
        System.out.println("Total de verificacoes (Deck e Hand): " + totalTestes);
        System.out.println("Falhas: " + falhas);
        if (falhas == 0) {
            System.out.println("RESULTADO: TODOS OS TESTES DE DECK E HAND PASSARAM! [OK]");
            System.out.println("----------------------------------------------------");
        } else {
            System.err.println("RESULTADO: FALHA NOS TESTES DE DECK E HAND! [FALHA]");
            System.out.println("----------------------------------------------------");
            System.exit(1);
        }
    }

    private static void afirme(boolean condicao, String descricao) {
        totalTestes++;
        if (condicao) {
            System.out.println("  [OK] " + descricao);
        } else {
            falhas++;
            System.err.println("  [FALHA] " + descricao);
        }
    }

    private static void testarIntegridadeBaralhoPadrao() {
        System.out.println("1. Testando Integridade do Baralho Padrao de Producao (SecureRandom, 52 Cartas Unicas):");

        Deck deckProducao = new Deck();
        afirme(deckProducao.cartasRestantes() == 52, "Deck() de producao (SecureRandom) inicia com 52 cartas");
        deckProducao.embaralhar();
        afirme(new HashSet<>(deckProducao.getCartasRestantesSnapshot()).size() == 52,
                "Deck() de producao preserva 52 cartas distintas apos embaralhar com SecureRandom");

        Deck deck = new Deck(new Random(42L));
        afirme(deck.cartasRestantes() == 52, "Deck padrao inicia com exatamente 52 cartas");
        afirme(!deck.estaVazio(), "Deck recem-criado nao esta vazio");

        List<Card> snapshotInicial = deck.getCartasRestantesSnapshot();
        Set<Card> cartasUnicas = new HashSet<>(snapshotInicial);
        afirme(snapshotInicial.size() == 52, "Snapshot inicial contem 52 cartas");
        afirme(cartasUnicas.size() == 52, "Todas as 52 cartas sao estritamente distintas (zero duplicatas)");

        boolean contemTodasCombinacoes = true;
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                if (!cartasUnicas.contains(new Card(suit, rank))) {
                    contemTodasCombinacoes = false;
                }
            }
        }
        afirme(contemTodasCombinacoes, "Deck contem todas as combinacoes de 4 naipes x 13 valores");
        System.out.println();
    }

    private static void testarEmbaralhamentoEReinicioDeterministico() {
        System.out.println("2. Testando Embaralhamento Deterministico e Reinicio:");

        Deck deckA = new Deck(new Random(123456L));
        Deck deckB = new Deck(new Random(123456L));
        List<Card> ordemAntes = deckA.getCartasRestantesSnapshot();

        deckA.embaralhar();
        deckB.embaralhar();

        List<Card> ordemDepoisA = deckA.getCartasRestantesSnapshot();
        List<Card> ordemDepoisB = deckB.getCartasRestantesSnapshot();

        afirme(ordemDepoisA.size() == 52, "Embaralhamento preserva a contagem de 52 cartas");
        afirme(new HashSet<>(ordemDepoisA).size() == 52, "Embaralhamento nao introduz cartas duplicadas nem perde cartas");
        afirme(!ordemAntes.equals(ordemDepoisA), "Embaralhamento altera efetivamente a ordem original das cartas");
        afirme(ordemDepoisA.equals(ordemDepoisB), "Mesma semente de Random produz embaralhamento deterministico identico");

        // Compra algumas cartas e reinicia
        deckA.comprarCarta();
        deckA.comprarCarta();
        deckA.comprarCarta();
        afirme(deckA.cartasRestantes() == 49, "Apos comprar 3 cartas restam 49 no baralho");

        deckA.reiniciarBaralhoPadrao();
        afirme(deckA.cartasRestantes() == 52, "reiniciarBaralhoPadrao() restaura as 52 cartas");
        afirme(new HashSet<>(deckA.getCartasRestantesSnapshot()).size() == 52,
                "Baralho reiniciado possui novamente as 52 cartas unicas");
        System.out.println();
    }

    private static void testarEsgotamentoESequenciaControlada() {
        System.out.println("3. Testando Sequencia Deterministica e Esgotamento do Baralho:");

        Card c1 = new Card(Suit.ESPADAS, Rank.AS);
        Card c2 = new Card(Suit.COPAS, Rank.REI);
        Card c3 = new Card(Suit.OUROS, Rank.SETE);

        Deck deckControlado = new Deck(List.of(c1, c2, c3));
        afirme(deckControlado.cartasRestantes() == 3, "Deck deterministico inicia com as 3 cartas fornecidas");
        afirme(deckControlado.comprarCarta().equals(c1), "1a compra retorna exatamente o 1o elemento da lista (As de Espadas)");
        afirme(deckControlado.comprarCarta().equals(c2), "2a compra retorna exatamente o 2o elemento da lista (Rei de Copas)");
        afirme(deckControlado.comprarCarta().equals(c3), "3a compra retorna exatamente o 3o elemento da lista (7 de Ouros)");
        afirme(deckControlado.estaVazio() && deckControlado.cartasRestantes() == 0, "Deck fica vazio apos comprar todas as cartas");

        boolean lancouExcecaoVazio = false;
        try {
            deckControlado.comprarCarta();
        } catch (NoSuchElementException e) {
            lancouExcecaoVazio = true;
        }
        afirme(lancouExcecaoVazio, "Comprar de um Deck vazio lanca NoSuchElementException");

        boolean rejeitouCartaNula = false;
        try {
            List<Card> invalida = new ArrayList<>();
            invalida.add(c1);
            invalida.add(null);
            new Deck(invalida);
        } catch (IllegalArgumentException e) {
            rejeitouCartaNula = true;
        }
        afirme(rejeitouCartaNula, "Construtor de Deck rejeita lista contendo carta nula");
        System.out.println();
    }

    private static void testarSnapshotsImutaveisDeckEHand() {
        System.out.println("4. Testando Snapshots Imutaveis (List.copyOf) em Deck e Hand:");

        List<Card> entradaMutavel = new ArrayList<>();
        entradaMutavel.add(new Card(Suit.PAUS, Rank.DEZ));
        entradaMutavel.add(new Card(Suit.COPAS, Rank.CINCO));

        Deck deck = new Deck(entradaMutavel);
        // Modificar a lista externa original NAO pode afetar o Deck
        entradaMutavel.clear();
        afirme(deck.cartasRestantes() == 2, "Mutacao na lista de entrada nao afeta o estado interno do Deck");

        List<Card> snapDeckAntes = deck.getCartasRestantesSnapshot();
        deck.comprarCarta();
        afirme(snapDeckAntes.size() == 2 && deck.cartasRestantes() == 1,
                "Snapshot de Deck (List.copyOf) permanece intacto mesmo apos novas compras no Deck");

        boolean bloqueouAddSnapDeck = false;
        try {
            snapDeckAntes.add(new Card(Suit.OUROS, Rank.DOIS));
        } catch (UnsupportedOperationException e) {
            bloqueouAddSnapDeck = true;
        }
        afirme(bloqueouAddSnapDeck, "Snapshot de Deck bloqueia tentativa de add() externo");

        Hand mao = new Hand();
        mao.adicionarCarta(new Card(Suit.ESPADAS, Rank.AS));
        mao.adicionarCarta(new Card(Suit.OUROS, Rank.OITO));

        List<Card> snapMaoAntes = mao.getCartas();
        List<Card> snapVisivelDealer = mao.getPrimeirasCartas(1);

        // Nova carta adicionada na mao NAO pode alterar snapshots ja devolvidos
        mao.adicionarCarta(new Card(Suit.PAUS, Rank.DOIS));

        afirme(snapMaoAntes.size() == 2 && mao.tamanho() == 3,
                "Snapshot de Hand (List.copyOf) nao muda quando a Hand recebe novas cartas");
        afirme(snapVisivelDealer.size() == 1 && snapVisivelDealer.get(0).equals(new Card(Suit.ESPADAS, Rank.AS)),
                "getPrimeirasCartas(1) expoe apenas a 1a carta sem vazar as demais");

        boolean bloqueouClearSnapMao = false;
        try {
            snapMaoAntes.clear();
        } catch (UnsupportedOperationException e) {
            bloqueouClearSnapMao = true;
        }
        afirme(bloqueouClearSnapMao, "Snapshot de Hand bloqueia tentativa de clear() externo");
        System.out.println();
    }

    private static void testarPontuacaoComMultiplosAses() {
        System.out.println("5. Testando Calculo de Pontuacao com Um e Multiplos Ases (11 ou 1):");

        // Cenario A: 1 As + 6 = 17 (Soft 17)
        Hand soft17 = new Hand();
        soft17.adicionarCarta(new Card(Suit.COPAS, Rank.AS));
        soft17.adicionarCarta(new Card(Suit.PAUS, Rank.SEIS));
        afirme(soft17.calcularPontuacao() == 17, "As + 6 resulta em 17 pontos");
        afirme(soft17.isMaoSoft(), "As + 6 e identificado como mao soft (As contado como 11)");

        // Cenario B: As + 6 + 10 = 17 (Hard 17 - As rebaixado de 11 para 1)
        soft17.adicionarCarta(new Card(Suit.ESPADAS, Rank.DEZ));
        afirme(soft17.calcularPontuacao() == 17, "As + 6 + 10 ajusta o As para 1 e resulta em 17 pontos (sem estourar)");
        afirme(!soft17.isMaoSoft(), "As + 6 + 10 passa a ser mao hard (As contado como 1)");

        // Cenario C: Dois Ases (A + A = 12: um vale 11 e o outro vale 1)
        Hand doisAses = new Hand();
        doisAses.adicionarCarta(new Card(Suit.ESPADAS, Rank.AS));
        doisAses.adicionarCarta(new Card(Suit.COPAS, Rank.AS));
        afirme(doisAses.calcularPontuacao() == 12, "Dois Ases (A + A) somam 12 (11 + 1)");
        afirme(doisAses.isMaoSoft(), "A + A permanece mao soft (um As ainda vale 11)");

        // Cenario D: Dois Ases + 9 = 21 (11 + 1 + 9 = 21)
        doisAses.adicionarCarta(new Card(Suit.OUROS, Rank.NOVE));
        afirme(doisAses.calcularPontuacao() == 21, "A + A + 9 soma exatamente 21 (11 + 1 + 9)");

        // Cenario E: Tres Ases + 8 = 21 (11 + 1 + 1 + 8 = 21)
        Hand tresAses = new Hand();
        tresAses.adicionarCarta(new Card(Suit.ESPADAS, Rank.AS));
        tresAses.adicionarCarta(new Card(Suit.COPAS, Rank.AS));
        tresAses.adicionarCarta(new Card(Suit.OUROS, Rank.AS));
        tresAses.adicionarCarta(new Card(Suit.PAUS, Rank.OITO));
        afirme(tresAses.calcularPontuacao() == 21, "Tres Ases + 8 soma 21 (11 + 1 + 1 + 8)");

        // Cenario F: Quatro Ases = 14 (11 + 1 + 1 + 1) e depois + Rei (10) = 14 (1 + 1 + 1 + 1 + 10)
        Hand quatroAses = new Hand();
        quatroAses.adicionarCarta(new Card(Suit.ESPADAS, Rank.AS));
        quatroAses.adicionarCarta(new Card(Suit.COPAS, Rank.AS));
        quatroAses.adicionarCarta(new Card(Suit.OUROS, Rank.AS));
        quatroAses.adicionarCarta(new Card(Suit.PAUS, Rank.AS));
        afirme(quatroAses.calcularPontuacao() == 14, "Quatro Ases somam 14 (11 + 1 + 1 + 1)");

        quatroAses.adicionarCarta(new Card(Suit.COPAS, Rank.REI));
        afirme(quatroAses.calcularPontuacao() == 14,
                "Quatro Ases + Rei converte todos os 4 Ases para 1 e soma 14 (1+1+1+1+10)");
        afirme(!quatroAses.isEstourada(), "Quatro Ases + Rei nao estoura");
        System.out.println();
    }

    private static void testarBlackjackNaturalVersus21Comum() {
        System.out.println("6. Testando Blackjack Natural (2 Cartas) vs 21 Comum (3+ Cartas):");

        Hand bjAsRei = new Hand();
        bjAsRei.adicionarCarta(new Card(Suit.ESPADAS, Rank.AS));
        bjAsRei.adicionarCarta(new Card(Suit.ESPADAS, Rank.REI));
        afirme(bjAsRei.calcularPontuacao() == 21 && bjAsRei.isBlackjackNatural(),
                "As + Rei (2 cartas) = 21 e E Blackjack Natural");

        Hand bjDezAs = new Hand();
        bjDezAs.adicionarCarta(new Card(Suit.OUROS, Rank.DEZ));
        bjDezAs.adicionarCarta(new Card(Suit.COPAS, Rank.AS));
        afirme(bjDezAs.calcularPontuacao() == 21 && bjDezAs.isBlackjackNatural(),
                "10 + As (2 cartas) = 21 e E Blackjack Natural");

        Hand vinteEUmTresCartas = new Hand();
        vinteEUmTresCartas.adicionarCarta(new Card(Suit.PAUS, Rank.SETE));
        vinteEUmTresCartas.adicionarCarta(new Card(Suit.COPAS, Rank.SETE));
        vinteEUmTresCartas.adicionarCarta(new Card(Suit.OUROS, Rank.SETE));
        afirme(vinteEUmTresCartas.calcularPontuacao() == 21 && !vinteEUmTresCartas.isBlackjackNatural(),
                "7 + 7 + 7 = 21 pontos, mas NAO e Blackjack Natural (possui 3 cartas)");

        Hand vinteEUmComAsTresCartas = new Hand();
        vinteEUmComAsTresCartas.adicionarCarta(new Card(Suit.ESPADAS, Rank.AS));
        vinteEUmComAsTresCartas.adicionarCarta(new Card(Suit.COPAS, Rank.AS));
        vinteEUmComAsTresCartas.adicionarCarta(new Card(Suit.OUROS, Rank.NOVE));
        afirme(vinteEUmComAsTresCartas.calcularPontuacao() == 21 && !vinteEUmComAsTresCartas.isBlackjackNatural(),
                "A + A + 9 = 21 pontos, mas NAO e Blackjack Natural (possui 3 cartas)");

        Hand duasCartas20 = new Hand();
        duasCartas20.adicionarCarta(new Card(Suit.COPAS, Rank.DAMA));
        duasCartas20.adicionarCarta(new Card(Suit.PAUS, Rank.VALETE));
        afirme(duasCartas20.calcularPontuacao() == 20 && !duasCartas20.isBlackjackNatural(),
                "Dama + Valete (2 cartas = 20) NAO e Blackjack Natural");
        System.out.println();
    }

    private static void testarEstouroELimpezaDaMao() {
        System.out.println("7. Testando Estouro (Bust), Pontos Visiveis e Limpeza de Mao:");

        Hand maoEstourada = new Hand();
        maoEstourada.adicionarCarta(new Card(Suit.PAUS, Rank.REI));
        maoEstourada.adicionarCarta(new Card(Suit.OUROS, Rank.DAMA));
        afirme(!maoEstourada.isEstourada() && maoEstourada.calcularPontuacao() == 20,
                "Rei + Dama = 20 (ainda nao estourou)");
        afirme(maoEstourada.calcularPontuacaoPrimeirasCartas(1) == 10,
                "calcularPontuacaoPrimeirasCartas(1) informa apenas os 10 pontos da primeira carta visivel");

        maoEstourada.adicionarCarta(new Card(Suit.COPAS, Rank.DOIS));
        afirme(maoEstourada.calcularPontuacao() == 22 && maoEstourada.isEstourada(),
                "Rei + Dama + 2 = 22 -> mao marcada como estourada (Bust)");

        // Estouro mesmo possuindo As (As ja rebaixado para 1)
        Hand estouroComAs = new Hand();
        estouroComAs.adicionarCarta(new Card(Suit.ESPADAS, Rank.AS));
        estouroComAs.adicionarCarta(new Card(Suit.COPAS, Rank.NOVE));
        estouroComAs.adicionarCarta(new Card(Suit.OUROS, Rank.CINCO)); // 1 + 9 + 5 = 15
        estouroComAs.adicionarCarta(new Card(Suit.PAUS, Rank.OITO));   // 1 + 9 + 5 + 8 = 23
        afirme(estouroComAs.calcularPontuacao() == 23 && estouroComAs.isEstourada(),
                "As + 9 + 5 + 8 = 23 -> estoura mesmo com As convertido para 1");

        estouroComAs.limpar();
        afirme(estouroComAs.estaVazia() && estouroComAs.tamanho() == 0 && estouroComAs.calcularPontuacao() == 0,
                "limpar() esvazia a mao por laco reverso e zera a pontuacao");

        boolean rejeitouCartaNula = false;
        try {
            estouroComAs.adicionarCarta(null);
        } catch (IllegalArgumentException e) {
            rejeitouCartaNula = true;
        }
        afirme(rejeitouCartaNula, "Hand rejeita adicao de carta nula com IllegalArgumentException");
        System.out.println();
    }
}
