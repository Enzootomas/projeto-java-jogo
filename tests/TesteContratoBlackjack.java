package tests;

import model.Card;
import model.Rank;
import model.RoundResult;
import model.RoundState;
import model.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bateria de testes automatizados do Contrato de Blackjack (Etapa 3 - Preparacao).
 * Valida a imutabilidade e regras de dominino de Card, Suit, Rank, RoundState e RoundResult.
 */
public class TesteContratoBlackjack {

    private static int totalTestes = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DO CONTRATO DO MOTOR DE BLACKJACK");
        System.out.println("====================================================\n");

        testarCartasENaipes();
        testarEstadosEResultados();
        testarCalculoPagamentos3Para2();
        testarImutabilidadeDeListas();

        System.out.println("\n----------------------------------------------------");
        System.out.println("Total de verificacoes do contrato: " + totalTestes);
        System.out.println("Falhas: " + falhas);
        if (falhas == 0) {
            System.out.println("RESULTADO: TODOS OS TESTES DO CONTRATO PASSARAM! [OK]");
            System.out.println("----------------------------------------------------");
        } else {
            System.err.println("RESULTADO: FALHA EM TESTES DO CONTRATO! [FALHA]");
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

    private static void testarCartasENaipes() {
        System.out.println("1. Testando Card, Suit e Rank:");

        Card asEspadas = new Card(Suit.ESPADAS, Rank.AS);
        Card reiCopas = new Card(Suit.COPAS, Rank.REI);
        Card seteOuros = new Card(Suit.OUROS, Rank.SETE);

        afirme(asEspadas.isAs(), "Card identifica que e um As");
        afirme(asEspadas.getValorBlackjack() == 11, "As tem valor base 11");
        afirme(reiCopas.getValorBlackjack() == 10, "Rei (figura) tem valor 10");
        afirme(seteOuros.getValorBlackjack() == 7, "Carta numerica (7) tem valor de face 7");

        Card outroAsEspadas = new Card(Suit.ESPADAS, Rank.AS);
        afirme(asEspadas.equals(outroAsEspadas), "Duas cartas com mesmo naipe e rank sao iguais (equals)");
        afirme(asEspadas.hashCode() == outroAsEspadas.hashCode(), "Cartas iguais possuem o mesmo hashCode");

        boolean capturouNulo = false;
        try {
            new Card(null, Rank.DEZ);
        } catch (IllegalArgumentException e) {
            capturouNulo = true;
        }
        afirme(capturouNulo, "Card rejeita naipe nulo com IllegalArgumentException");
        System.out.println();
    }

    private static void testarEstadosEResultados() {
        System.out.println("2. Testando Enums RoundState e RoundResult:");

        afirme(RoundState.values().length == 3, "RoundState possui exatamente 3 estados formais");
        afirme(RoundState.AGUARDANDO_APOSTA != null &&
               RoundState.TURNO_JOGADOR != null &&
               RoundState.FINALIZADA != null, "Estados AGUARDANDO_APOSTA, TURNO_JOGADOR e FINALIZADA presentes");

        afirme(RoundResult.values().length == 7, "RoundResult possui os 7 desfechos esperados");
        afirme(RoundResult.JOGADOR_BLACKJACK.isVitoriaJogador(), "JOGADOR_BLACKJACK e vitoria do jogador");
        afirme(RoundResult.JOGADOR_VITORIA.isVitoriaJogador(), "JOGADOR_VITORIA e vitoria do jogador");
        afirme(!RoundResult.JOGADOR_ESTOUROU.isVitoriaJogador(), "JOGADOR_ESTOUROU nao e vitoria do jogador");
        afirme(!RoundResult.DEALER_VITORIA.isVitoriaJogador(), "DEALER_VITORIA nao e vitoria do jogador");
        afirme(RoundResult.EMPATE_PUSH.isEmpate(), "EMPATE_PUSH e classificado como empate");
        System.out.println();
    }

    private static void testarCalculoPagamentos3Para2() {
        System.out.println("3. Testando Matematica de Pagamentos e Aposta Par (3:2 sem arredondamento):");

        long aposta100 = 100L;
        long retornoBJ100 = RoundResult.JOGADOR_BLACKJACK.calcularRetornoTotal(aposta100);
        afirme(retornoBJ100 == 250L,
                "Aposta de 100 com Blackjack (3:2) retorna 250 fichas (100 aposta + 150 lucro exato)");

        long aposta20 = 20L;
        long retornoBJ20 = RoundResult.JOGADOR_BLACKJACK.calcularRetornoTotal(aposta20);
        afirme(retornoBJ20 == 50L,
                "Aposta de 20 com Blackjack (3:2) retorna 50 fichas (20 aposta + 30 lucro exato)");

        long retornoVitoria100 = RoundResult.JOGADOR_VITORIA.calcularRetornoTotal(aposta100);
        afirme(retornoVitoria100 == 200L,
                "Aposta de 100 com Vitoria simples (1:1) retorna 200 fichas (100 aposta + 100 lucro)");

        long retornoEmpate100 = RoundResult.EMPATE_PUSH.calcularRetornoTotal(aposta100);
        afirme(retornoEmpate100 == 100L,
                "Aposta de 100 com Empate (Push) devolve integralmente as 100 fichas originais");

        long retornoEstouro100 = RoundResult.JOGADOR_ESTOUROU.calcularRetornoTotal(aposta100);
        afirme(retornoEstouro100 == 0L, "Jogador que estourou nao recebe retorno de fichas (0)");

        long retornoDealerVenceu = RoundResult.DEALER_VITORIA.calcularRetornoTotal(aposta100);
        afirme(retornoDealerVenceu == 0L, "Vitoria do dealer nao gera retorno para o jogador (0)");
        System.out.println();
    }

    private static void testarImutabilidadeDeListas() {
        System.out.println("4. Testando Seguranca e Imutabilidade de Colecoes:");

        List<Card> cartasOriginais = new ArrayList<>();
        cartasOriginais.add(new Card(Suit.PAUS, Rank.DEZ));
        cartasOriginais.add(new Card(Suit.COPAS, Rank.AS));

        List<Card> copiaImutavel = Collections.unmodifiableList(cartasOriginais);

        boolean capturouModificacao = false;
        try {
            copiaImutavel.add(new Card(Suit.OUROS, Rank.CINCO));
        } catch (UnsupportedOperationException e) {
            capturouModificacao = true;
        }

        afirme(capturouModificacao,
                "Colecao imutavel de cartas lanca UnsupportedOperationException ao tentar adicionar carta externamente");
        System.out.println();
    }
}
