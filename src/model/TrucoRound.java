package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Motor de uma mão completa de Truco Paulista (1 contra 1).
 *
 * Uma mão consiste na distribuição de 3 cartas para cada jogador,
 * uma carta vira que define as manilhas, e a disputa em melhor de até 3 vazas.
 *
 * TABELA OFICIAL DE RESOLUÇÃO DE VAZAS NO TRUCO PAULISTA:
 * +--------+--------+--------+-----------------+----------------------------------------------+
 * | Vaza 1 | Vaza 2 | Vaza 3 | Vencedor da Mão | Regra Aplicada                               |
 * +--------+--------+--------+-----------------+----------------------------------------------+
 * |   J    |   J    |   --   | JOGADOR         | Encerramento antecipado por 2x0              |
 * |   B    |   B    |   --   | BOT             | Encerramento antecipado por 0x2              |
 * |   J    |   B    |   J    | JOGADOR         | Vitória 2x1 na terceira vaza                 |
 * |   J    |   B    |   B    | BOT             | Vitória 1x2 na terceira vaza                 |
 * |   B    |   J    |   J    | JOGADOR         | Vitória 2x1 na terceira vaza                 |
 * |   B    |   J    |   B    | BOT             | Vitória 1x2 na terceira vaza                 |
 * |   E    |   J    |   --   | JOGADOR         | Empate na 1ª: quem vencer a 2ª leva a mão    |
 * |   E    |   B    |   --   | BOT             | Empate na 1ª: quem vencer a 2ª leva a mão    |
 * |   J    |   E    |   --   | JOGADOR         | Empate na 2ª: quem venceu a 1ª leva a mão    |
 * |   B    |   E    |   --   | BOT             | Empate na 2ª: quem venceu a 1ª leva a mão    |
 * |   J    |   B    |   E    | JOGADOR         | Empate na 3ª: quem venceu a 1ª leva a mão    |
 * |   B    |   J    |   E    | BOT             | Empate na 3ª: quem venceu a 1ª leva a mão    |
 * |   E    |   E    |   J    | JOGADOR         | Duas primeiras empatadas: 3ª decide a mão    |
 * |   E    |   E    |   B    | BOT             | Duas primeiras empatadas: 3ª decide a mão    |
 * |   E    |   E    |   E    | EMPATE          | Três empates seguidos: mão sem vencedor      |
 * +--------+--------+--------+-----------------+----------------------------------------------+
 *
 * QUEM COMEÇA CADA VAZA:
 * - Vaza 1: começa o jogador definido como 'primeiroDaMao'.
 * - Vazas 2 e 3:
 *   - Se a vaza anterior teve vencedor: o VENCEDOR da vaza anterior começa a vaza seguinte.
 *   - Se a vaza anterior empatou (canga): QUEM COMEÇOU a vaza empatada continua começando.
 */
public class TrucoRound {

    private final Card vira;
    private final Rank manilhaRank;
    private final TrucoPlayer primeiroDaMao;
    private final TrucoSimpleAI botAI;

    private final List<Card> maoJogador;
    private final List<Card> maoBot;
    private final List<TrucoTrick> vazas;

    private TrucoPlayer vezAtual;
    private TrucoHandResult resultadoMao;

    /**
     * Construtor para inicialização controlada da mão (determinística ou via baralho).
     *
     * @param maoJogador 3 cartas iniciais do jogador.
     * @param maoBot 3 cartas iniciais do bot.
     * @param vira Carta vira que define a manilha.
     * @param primeiroDaMao Quem joga primeiro na primeira vaza.
     */
    public TrucoRound(List<Card> maoJogador, List<Card> maoBot, Card vira, TrucoPlayer primeiroDaMao) {
        if (maoJogador == null || maoJogador.size() != 3) {
            throw new IllegalArgumentException("A mão do jogador deve conter exatamente 3 cartas.");
        }
        if (maoBot == null || maoBot.size() != 3) {
            throw new IllegalArgumentException("A mão do bot deve conter exatamente 3 cartas.");
        }
        if (vira == null) {
            throw new IllegalArgumentException("A carta vira não pode ser nula.");
        }
        if (primeiroDaMao == null) {
            throw new IllegalArgumentException("O jogador inicial não pode ser nulo.");
        }

        this.maoJogador = new ArrayList<>(maoJogador);
        this.maoBot = new ArrayList<>(maoBot);
        this.vira = vira;
        this.manilhaRank = TrucoRules.determinarManilha(vira.getRank());
        this.primeiroDaMao = primeiroDaMao;
        this.botAI = new TrucoSimpleAI();
        this.vazas = new ArrayList<>(3);
        this.resultadoMao = TrucoHandResult.EM_ANDAMENTO;

        // Inicia a primeira vaza
        iniciarNovaVaza(1, primeiroDaMao);
    }

    /**
     * Fábrica conveniente para iniciar uma mão a partir de um baralho de Truco.
     */
    public static TrucoRound iniciarComBaralho(TrucoDeck deck, TrucoPlayer primeiroDaMao) {
        if (deck == null || deck.getCartasRestantes() < 7) {
            throw new IllegalArgumentException("O baralho deve conter pelo menos 7 cartas para iniciar uma mão.");
        }
        List<Card> cartasJogador = deck.distribuirMao(3);
        List<Card> cartasBot = deck.distribuirMao(3);
        Card cartaVira = deck.comprarCarta();
        return new TrucoRound(cartasJogador, cartasBot, cartaVira, primeiroDaMao);
    }

    private void iniciarNovaVaza(int numeroVaza, TrucoPlayer primeiroAJogar) {
        TrucoTrick trick = new TrucoTrick(numeroVaza, primeiroAJogar, vira);
        this.vazas.add(trick);
        this.vezAtual = primeiroAJogar;

        // Se for a vez do Bot abrir a vaza, ele já pode fazer sua jogada se a IA estiver ativa
        if (vezAtual == TrucoPlayer.BOT && resultadoMao == TrucoHandResult.EM_ANDAMENTO) {
            executarJogadaBotInterna();
        }
    }

    public Card getVira() {
        return vira;
    }

    public Rank getManilhaRank() {
        return manilhaRank;
    }

    public TrucoPlayer getPrimeiroDaMao() {
        return primeiroDaMao;
    }

    public TrucoPlayer getVezAtual() {
        return vezAtual;
    }

    public boolean isTurnoJogador() {
        return resultadoMao == TrucoHandResult.EM_ANDAMENTO && vezAtual == TrucoPlayer.JOGADOR;
    }

    public boolean isTurnoBot() {
        return resultadoMao == TrucoHandResult.EM_ANDAMENTO && vezAtual == TrucoPlayer.BOT;
    }

    public boolean isFinalizada() {
        return resultadoMao.isFinalizada();
    }

    public TrucoHandResult getResultadoMao() {
        return resultadoMao;
    }

    public List<Card> getMaoJogador() {
        return Collections.unmodifiableList(maoJogador);
    }

    /**
     * Retorna a quantidade de cartas restantes na mão do Bot sem expor quais são.
     */
    public int getQuantidadeCartasBot() {
        return maoBot.size();
    }

    public List<TrucoTrick> getVazas() {
        return List.copyOf(vazas);
    }

    public TrucoTrick getVazaAtual() {
        if (vazas.isEmpty()) {
            return null;
        }
        return vazas.get(vazas.size() - 1);
    }

    public int getNumeroVazaAtual() {
        TrucoTrick atual = getVazaAtual();
        return atual != null ? atual.getNumeroVaza() : 0;
    }

    /**
     * Jogador humano realiza a jogada de uma carta de sua mão.
     *
     * @param carta Carta presente na mão do jogador.
     */
    public void jogarCartaJogador(Card carta) {
        if (isFinalizada()) {
            throw new IllegalStateException("A mão de Truco já foi finalizada com o resultado: " + resultadoMao);
        }
        if (!isTurnoJogador()) {
            throw new IllegalStateException("Não é o turno do jogador. Vez atual: " + vezAtual);
        }
        if (carta == null) {
            throw new IllegalArgumentException("A carta a ser jogada não pode ser nula.");
        }
        if (!maoJogador.contains(carta)) {
            throw new IllegalArgumentException("A carta " + carta + " não está na mão do jogador ou já foi jogada.");
        }

        // Remove da mão e registra na vaza atual através de nova instância imutável
        maoJogador.remove(carta);
        TrucoTrick vaza = getVazaAtual();
        TrucoTrick vazaAtualizada = vaza.comJogada(TrucoPlayer.JOGADOR, carta);
        vazas.set(vazas.size() - 1, vazaAtualizada);

        processarAposJogada();
    }

    /**
     * Executa a jogada do Bot caso seja o turno dele.
     */
    public void jogarTurnoBot() {
        if (isFinalizada()) {
            throw new IllegalStateException("A mão de Truco já foi finalizada.");
        }
        if (!isTurnoBot()) {
            throw new IllegalStateException("Não é o turno do Bot. Vez atual: " + vezAtual);
        }
        executarJogadaBotInterna();
    }

    private void executarJogadaBotInterna() {
        TrucoTrick vaza = getVazaAtual();
        Card cartaPublicaAdversario = vaza.getCartaJogador(); // Pode ser null se o bot abre

        // O Bot consulta exclusivamente sua própria mão, a vira pública e a carta aberta do adversário
        Card cartaEscolhida = botAI.escolherCarta(maoBot, vira, cartaPublicaAdversario);

        maoBot.remove(cartaEscolhida);
        TrucoTrick vazaAtualizada = vaza.comJogada(TrucoPlayer.BOT, cartaEscolhida);
        vazas.set(vazas.size() - 1, vazaAtualizada);

        processarAposJogada();
    }

    private void processarAposJogada() {
        TrucoTrick vaza = getVazaAtual();

        // Se a vaza foi concluída por ambos os jogadores
        if (vaza.isCompleta()) {
            avaliarDesfechoMao();
        } else {
            // A vaza ainda aguarda a jogada do segundo jogador
            this.vezAtual = vaza.getProximoAJogar();
            if (vezAtual == TrucoPlayer.BOT && resultadoMao == TrucoHandResult.EM_ANDAMENTO) {
                executarJogadaBotInterna();
            }
        }
    }

    /**
     * Aplica a tabela de resolução de vazas e determina se a mão foi concluída
     * ou se uma nova vaza deve ser iniciada.
     */
    private void avaliarDesfechoMao() {
        int totalVazas = vazas.size();

        // Vaza 1 concluída
        if (totalVazas == 1) {
            TrucoTrick v1 = vazas.get(0);
            TrucoPlayer proximo;
            if (v1.getResultado() == TrucoTrickResult.EMPATE) {
                // Em caso de empate na 1ª vaza, quem começou continua jogando primeiro na 2ª
                proximo = v1.getPrimeiroAJogar();
            } else {
                // Quem venceu a 1ª joga primeiro na 2ª
                proximo = v1.getVencedor();
            }
            iniciarNovaVaza(2, proximo);
            return;
        }

        // Vaza 2 concluída
        if (totalVazas == 2) {
            TrucoTrick v1 = vazas.get(0);
            TrucoTrick v2 = vazas.get(1);

            TrucoTrickResult r1 = v1.getResultado();
            TrucoTrickResult r2 = v2.getResultado();

            // Cenário A: Alguém venceu as duas primeiras (2x0) -> Encerramento antecipado
            if (r1 == TrucoTrickResult.VITORIA_JOGADOR && r2 == TrucoTrickResult.VITORIA_JOGADOR) {
                this.resultadoMao = TrucoHandResult.VITORIA_JOGADOR;
                this.vezAtual = null;
                return;
            }
            if (r1 == TrucoTrickResult.VITORIA_BOT && r2 == TrucoTrickResult.VITORIA_BOT) {
                this.resultadoMao = TrucoHandResult.VITORIA_BOT;
                this.vezAtual = null;
                return;
            }

            // Cenário B: Empate na 1ª vaza -> Quem venceu a 2ª leva a mão imediatamente (antecipado)
            if (r1 == TrucoTrickResult.EMPATE) {
                if (r2 == TrucoTrickResult.VITORIA_JOGADOR) {
                    this.resultadoMao = TrucoHandResult.VITORIA_JOGADOR;
                    this.vezAtual = null;
                    return;
                } else if (r2 == TrucoTrickResult.VITORIA_BOT) {
                    this.resultadoMao = TrucoHandResult.VITORIA_BOT;
                    this.vezAtual = null;
                    return;
                }
                // Se r2 também empatou (E + E), teremos que disputar a 3ª vaza
            }

            // Cenário C: Empate na 2ª vaza após vitória na 1ª -> Quem venceu a 1ª leva a mão imediatamente (antecipado)
            if (r2 == TrucoTrickResult.EMPATE) {
                if (r1 == TrucoTrickResult.VITORIA_JOGADOR) {
                    this.resultadoMao = TrucoHandResult.VITORIA_JOGADOR;
                    this.vezAtual = null;
                    return;
                } else if (r1 == TrucoTrickResult.VITORIA_BOT) {
                    this.resultadoMao = TrucoHandResult.VITORIA_BOT;
                    this.vezAtual = null;
                    return;
                }
            }

            // Cenário D: 1x1 (V1 e V2 com vencedores distintos) ou 0x0 (duas cangas E + E) -> Iniciar Vaza 3
            TrucoPlayer proximo;
            if (r2 == TrucoTrickResult.EMPATE) {
                proximo = v2.getPrimeiroAJogar();
            } else {
                proximo = v2.getVencedor();
            }
            iniciarNovaVaza(3, proximo);
            return;
        }

        // Vaza 3 concluída
        if (totalVazas == 3) {
            TrucoTrick v1 = vazas.get(0);
            TrucoTrick v2 = vazas.get(1);
            TrucoTrick v3 = vazas.get(2);

            TrucoTrickResult r1 = v1.getResultado();
            TrucoTrickResult r2 = v2.getResultado();
            TrucoTrickResult r3 = v3.getResultado();

            // Cenário E: A 3ª vaza teve um vencedor claro
            if (r3 == TrucoTrickResult.VITORIA_JOGADOR) {
                // Seja vindo de 1x1 ou de E+E
                this.resultadoMao = TrucoHandResult.VITORIA_JOGADOR;
                this.vezAtual = null;
                return;
            } else if (r3 == TrucoTrickResult.VITORIA_BOT) {
                this.resultadoMao = TrucoHandResult.VITORIA_BOT;
                this.vezAtual = null;
                return;
            }

            // Cenário F: A 3ª vaza empatou (Canga na 3ª)
            if (r3 == TrucoTrickResult.EMPATE) {
                if (r1 == TrucoTrickResult.EMPATE && r2 == TrucoTrickResult.EMPATE) {
                    // Três empates seguidos: mão anulada/sem vencedor
                    this.resultadoMao = TrucoHandResult.EMPATE;
                } else if (r1 == TrucoTrickResult.VITORIA_JOGADOR) {
                    // Empatou na 3ª vindo de 1x1: ganha quem levou a 1ª
                    this.resultadoMao = TrucoHandResult.VITORIA_JOGADOR;
                } else if (r1 == TrucoTrickResult.VITORIA_BOT) {
                    this.resultadoMao = TrucoHandResult.VITORIA_BOT;
                }
                this.vezAtual = null;
            }
        }
    }
}
