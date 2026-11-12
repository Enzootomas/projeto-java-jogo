package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Representa uma mao de cartas no Blackjack (seja do jogador ou do dealer) e encapsula
 * todas as regras de calculo de pontuacao, tratamento flexivel de Ases (11 ou 1),
 * identificacao de Blackjack Natural e verificacao de estouro (Bust).
 *
 * <p><strong>Conceitos de POO aplicados:</strong></p>
 * <ul>
 *   <li><strong>Encapsulamento e Defensividade:</strong> a colecao interna {@code cartas} e privada;
 *       consultas retornam snapshots imutaveis via {@link List#copyOf(Collection)} (seguro porque
 *       {@link Card} tambem e imutavel). Nenhuma referencia mutavel escapa para fora da classe.</li>
 *   <li><strong>Abstracao e Coesao:</strong> concentra em um unico lugar a regra matematica dos Ases
 *       (inclusive para subconjuntos visiveis da mao do dealer via {@link #calcularPontuacao(List)}),
 *       evitando duplicacao de logica entre {@code Hand} e o futuro motor {@code BlackjackGame}.</li>
 * </ul>
 */
public class Hand {

    /** Pontuacao maxima valida no Blackjack antes de configurar estouro (Bust). */
    public static final int PONTUACAO_MAXIMA = 21;

    /** Quantidade exata de cartas exigida para caracterizar um Blackjack Natural. */
    public static final int CARTAS_BLACKJACK_NATURAL = 2;

    /** Diferenca subtraida quando um As passa a valer 1 em vez de 11 (11 - 1 = 10). */
    private static final int AJUSTE_AS_DE_11_PARA_1 = 10;

    private final List<Card> cartas;

    /**
     * Cria uma mao inicialmente vazia.
     */
    public Hand() {
        this.cartas = new ArrayList<>();
    }

    /**
     * Adiciona uma nova carta ao final da mao.
     *
     * @param carta carta a ser recebida (nao pode ser nula).
     * @throws IllegalArgumentException se {@code carta} for nula.
     */
    public void adicionarCarta(Card carta) {
        if (carta == null) {
            throw new IllegalArgumentException("Nao e permitido adicionar uma carta nula na mao.");
        }
        this.cartas.add(carta);
    }

    /**
     * Remove todas as cartas da mao para preparacao de uma nova rodada,
     * utilizando laco reverso conforme diretriz didatica do projeto.
     */
    public void limpar() {
        for (int i = this.cartas.size() - 1; i >= 0; i--) {
            this.cartas.remove(i);
        }
    }

    /**
     * Retorna um snapshot imutavel de todas as cartas presentes na mao.
     *
     * <p>Como {@link List#copyOf(Collection)} cria uma copia nao-modificavel independente
     * e {@link Card} e uma classe {@code final} imutavel, nem a lista nem seus elementos
     * podem ser alterados externamente, e mutacoes futuras na {@code Hand} nao alteram
     * snapshots capturados anteriormente.</p>
     *
     * @return snapshot imutavel das cartas da mao.
     */
    public List<Card> getCartas() {
        return List.copyOf(this.cartas);
    }

    /**
     * Retorna um snapshot imutavel contendo apenas as primeiras {@code quantidadeVisivel}
     * cartas da mao (util para expor somente a carta aberta do dealer sem vazar a carta oculta).
     *
     * @param quantidadeVisivel numero de cartas iniciais visiveis ({@code 0 <= quantidadeVisivel <= tamanho()}).
     * @return snapshot imutavel das cartas visiveis solicitadas.
     * @throws IllegalArgumentException se {@code quantidadeVisivel} estiver fora dos limites validos.
     */
    public List<Card> getPrimeirasCartas(int quantidadeVisivel) {
        if (quantidadeVisivel < 0 || quantidadeVisivel > this.cartas.size()) {
            throw new IllegalArgumentException(
                    "Quantidade visivel invalida: " + quantidadeVisivel + " (tamanho atual: " + this.cartas.size() + ")."
            );
        }
        return List.copyOf(this.cartas.subList(0, quantidadeVisivel));
    }

    /**
     * Retorna a quantidade atual de cartas na mao.
     *
     * @return numero de cartas ({@code >= 0}).
     */
    public int tamanho() {
        return this.cartas.size();
    }

    /**
     * Informa se a mao nao possui nenhuma carta.
     *
     * @return {@code true} se vazia; {@code false} caso contrario.
     */
    public boolean estaVazia() {
        return this.cartas.isEmpty();
    }

    /**
     * Calcula a melhor pontuacao possivel da mao no Blackjack tratando cada As como 11
     * sempre que isso nao causar estouro ({@code > 21}), ou convertendo quantos Ases forem
     * necessarios de 11 para 1.
     *
     * @return pontuacao inteira otimizada da mao.
     */
    public int calcularPontuacao() {
        return calcularPontuacao(this.cartas);
    }

    /**
     * Calcula a pontuacao apenas das primeiras {@code quantidadeVisivel} cartas da mao
     * (por exemplo, para consultar os pontos visiveis do dealer durante o turno do jogador).
     *
     * @param quantidadeVisivel numero de cartas iniciais a considerar.
     * @return pontuacao correspondente apenas as cartas visiveis.
     */
    public int calcularPontuacaoPrimeirasCartas(int quantidadeVisivel) {
        return calcularPontuacao(getPrimeirasCartas(quantidadeVisivel));
    }

    /**
     * Metodo utilitario puro para calcular a pontuacao de qualquer sequencia de cartas
     * segundo as regras do Blackjack.
     *
     * @param cartasParaAvaliar lista de cartas (nao nula e sem elementos nulos).
     * @return soma otimizada considerando Ases como 11 ou 1.
     */
    public static int calcularPontuacao(List<Card> cartasParaAvaliar) {
        if (cartasParaAvaliar == null) {
            throw new IllegalArgumentException("A lista de cartas para calculo nao pode ser nula.");
        }

        int soma = 0;
        int asesContadosComoOnze = 0;

        for (Card carta : cartasParaAvaliar) {
            if (carta == null) {
                throw new IllegalArgumentException("A lista de cartas nao pode conter elementos nulos.");
            }
            soma = Math.addExact(soma, carta.getValorBlackjack());
            if (carta.isAs()) {
                asesContadosComoOnze++;
            }
        }

        // Enquanto a soma ultrapassar 21 e ainda houver Ases valendo 11,
        // convertemos um As por vez para valer 1 (subtraindo 10).
        while (soma > PONTUACAO_MAXIMA && asesContadosComoOnze > 0) {
            soma -= AJUSTE_AS_DE_11_PARA_1;
            asesContadosComoOnze--;
        }

        return soma;
    }

    /**
     * Indica se a mao atual e "soft" (possui pelo menos um As ainda contabilizado como 11
     * sem estourar 21). Util para regras de dealer (ex.: soft 17 vs hard 17) e depuracao.
     *
     * @return {@code true} se houver ao menos um As valendo 11 na pontuacao atual ({@code <= 21}).
     */
    public boolean isMaoSoft() {
        int soma = 0;
        int asesContadosComoOnze = 0;

        for (Card carta : this.cartas) {
            soma = Math.addExact(soma, carta.getValorBlackjack());
            if (carta.isAs()) {
                asesContadosComoOnze++;
            }
        }

        while (soma > PONTUACAO_MAXIMA && asesContadosComoOnze > 0) {
            soma -= AJUSTE_AS_DE_11_PARA_1;
            asesContadosComoOnze--;
        }

        return soma <= PONTUACAO_MAXIMA && asesContadosComoOnze > 0;
    }

    /**
     * Verifica se a mao configura um <strong>Blackjack Natural</strong>:
     * pontuacao igual a 21 obtida com <strong>exatamente duas cartas</strong>
     * (um As + uma carta de valor 10).
     *
     * <p>Maos com 3 ou mais cartas que somem 21 contam como 21 comum,
     * retornando {@code false} neste metodo.</p>
     *
     * @return {@code true} exclusivamente quando {@code tamanho() == 2 && calcularPontuacao() == 21}.
     */
    public boolean isBlackjackNatural() {
        return this.cartas.size() == CARTAS_BLACKJACK_NATURAL
                && calcularPontuacao() == PONTUACAO_MAXIMA;
    }

    /**
     * Verifica se a mao estourou o limite de 21 pontos (Bust), mesmo apos converter
     * todos os Ases existentes para o valor minimo 1.
     *
     * @return {@code true} se {@code calcularPontuacao() > 21}; {@code false} caso contrario.
     */
    public boolean isEstourada() {
        return calcularPontuacao() > PONTUACAO_MAXIMA;
    }

    @Override
    public String toString() {
        return "Hand{cartas=" + cartas + ", pontos=" + calcularPontuacao() + "}";
    }
}
