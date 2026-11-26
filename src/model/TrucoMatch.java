package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Motor oficial de partida de Truco Paulista suportando:
 * <ul>
 *   <li><strong>Modos:</strong> {@link TrucoMode#INDIVIDUAL} (1 vs 1) e {@link TrucoMode#DUPLAS}
 *       (2 vs 2 com 4 lugares alternando turnos entre as equipes).</li>
 *   <li><strong>Apostas e Escalada Paulista:</strong> maos valendo 1 ponto com aumentos para
 *       3 (Truco), 6 (Seis), 9 (Nove) e 12 (Doze); partida encerrada aos 12 pontos.</li>
 *   <li><strong>Mao de 11 e Mao de Ferro:</strong> quando apenas uma equipe atinge 11 pontos,
 *       a equipe decide se aceita jogar valendo 3 pontos ou corre entregando 1 ponto (sem pedidos de Truco);
 *       em 11 x 11 (Mao de Ferro), a mao vale 1 ponto sem aumentos.</li>
 *   <li><strong>Ritmo Controlado:</strong> avanca passo a passo via {@link #executarPassoBot()} e
 *       {@link #avancarAposPausaVaza()}, permitindo pausas visiveis (~1s por Bot e ~2s por vaza resolvida)
 *       sem bloquear a EDT do Swing e preservando a ultima vaza resolvida para consulta.</li>
 * </ul>
 */
public class TrucoMatch {

    public static final int PONTOS_VITORIA_PARTIDA = 12;

    private final TrucoMode modo;
    private final List<TrucoSeat> seats;
    private final TrucoMatchAI botAI;

    private int pontosEquipeJogador;
    private int pontosEquipeAdversaria;
    private boolean partidaFinalizada;
    private TrucoTeam equipeVencedoraPartida;

    private int proximoPrimeiroSeatDaMao;
    private int primeiroSeatDaMaoAtual;
    private int seatVezAtual;

    private Card vira;
    private Rank manilhaRank;

    private int valorMaoAtual;
    private boolean maoDeOnze;
    private TrucoTeam equipeMaoDeOnze;
    private boolean maoDeFerro;

    private boolean pedidoTrucoPendente;
    private int valorPropostoTruco;
    private TrucoTeam equipeQuePediuTruco;
    private String nomeQuemPediuTruco;
    private TrucoTeam equipeQueDeveResponderTruco;
    private TrucoTeam ultimaEquipeQueAumentou;
    private boolean botJaAvaliouPedirTrucoNesteTurno;

    private final List<TrucoTableTrick> vazasConcluidas;
    private TrucoTableTrick vazaAtual;
    private TrucoTableTrick ultimaVazaResolvida;
    private boolean aguardandoTransicaoVaza;

    private boolean maoFinalizada;
    private TrucoTeam equipeVencedoraMao;
    private String ultimaAcaoDescricao;

    /**
     * Inicia uma nova partida de Truco Paulista no modo escolhido com baralho embaralhado.
     */
    public TrucoMatch(TrucoMode modo) {
        this(modo, new TrucoMatchAI.DefaultAI(), 0, 0, 0);
        iniciarNovaMao(new TrucoDeck());
    }

    /**
     * Construtor deterministico para testes automatizados (sem iniciar a mao automaticamente).
     */
    public TrucoMatch(TrucoMode modo,
                      TrucoMatchAI botAI,
                      int pontosIniciaisJogador,
                      int pontosIniciaisAdversario,
                      int primeiroSeatInicial) {
        this.modo = Objects.requireNonNull(modo, "Modo de Truco nao pode ser nulo.");
        this.botAI = Objects.requireNonNull(botAI, "IA de Truco nao pode ser nula.");
        this.pontosEquipeJogador = Math.max(0, pontosIniciaisJogador);
        this.pontosEquipeAdversaria = Math.max(0, pontosIniciaisAdversario);
        this.seats = criarAssentos(modo);
        this.proximoPrimeiroSeatDaMao = Math.floorMod(primeiroSeatInicial, seats.size());
        this.vazasConcluidas = new ArrayList<>(3);
        this.valorMaoAtual = 1;
        this.ultimaAcaoDescricao = "Partida de Truco (" + modo.getNomeExibicao() + ") criada.";
    }

    private static List<TrucoSeat> criarAssentos(TrucoMode modo) {
        List<TrucoSeat> lista = new ArrayList<>(modo.getQuantidadeParticipantes());
        if (modo == TrucoMode.INDIVIDUAL) {
            lista.add(new TrucoSeat(0, "Você", TrucoTeam.EQUIPE_JOGADOR, true));
            lista.add(new TrucoSeat(1, "Bot", TrucoTeam.EQUIPE_ADVERSARIA, false));
        } else {
            // Ordem horaria alternando equipes: 0(Você) -> 1(Bot 1) -> 2(Parceiro) -> 3(Bot 2)
            lista.add(new TrucoSeat(0, "Você", TrucoTeam.EQUIPE_JOGADOR, true));
            lista.add(new TrucoSeat(1, "Bot 1", TrucoTeam.EQUIPE_ADVERSARIA, false));
            lista.add(new TrucoSeat(2, "Parceiro", TrucoTeam.EQUIPE_JOGADOR, false));
            lista.add(new TrucoSeat(3, "Bot 2", TrucoTeam.EQUIPE_ADVERSARIA, false));
        }
        return lista;
    }

    /**
     * Inicia uma nova mao usando um baralho limpo de 40 cartas recem-embaralhado.
     */
    public boolean iniciarNovaMao() {
        return iniciarNovaMao(new TrucoDeck());
    }

    /**
     * Inicia uma nova mao usando o baralho fornecido (deterministico ou embaralhado).
     */
    public boolean iniciarNovaMao(TrucoDeck deck) {
        if (partidaFinalizada) {
            return false;
        }
        int cartasNecessarias = (seats.size() * 3) + 1;
        if (deck == null || deck.getCartasRestantes() < cartasNecessarias) {
            return false;
        }

        for (TrucoSeat seat : seats) {
            seat.definirMao(deck.distribuirMao(3));
        }
        this.vira = deck.comprarCarta();
        this.manilhaRank = TrucoRules.determinarManilha(this.vira.getRank());

        for (int i = vazasConcluidas.size() - 1; i >= 0; i--) {
            vazasConcluidas.remove(i);
        }
        this.ultimaVazaResolvida = null;
        this.aguardandoTransicaoVaza = false;
        this.maoFinalizada = false;
        this.equipeVencedoraMao = null;

        this.primeiroSeatDaMaoAtual = this.proximoPrimeiroSeatDaMao;
        this.proximoPrimeiroSeatDaMao = (this.proximoPrimeiroSeatDaMao + 1) % seats.size();
        this.seatVezAtual = this.primeiroSeatDaMaoAtual;

        this.valorMaoAtual = 1;
        this.pedidoTrucoPendente = false;
        this.valorPropostoTruco = 0;
        this.equipeQuePediuTruco = null;
        this.nomeQuemPediuTruco = null;
        this.equipeQueDeveResponderTruco = null;
        this.ultimaEquipeQueAumentou = null;
        this.botJaAvaliouPedirTrucoNesteTurno = false;

        // Verifica Mao de 11 e Mao de Ferro (Truco Paulista)
        this.maoDeFerro = (pontosEquipeJogador == 11 && pontosEquipeAdversaria == 11);
        this.maoDeOnze = (!maoDeFerro && (pontosEquipeJogador == 11 || pontosEquipeAdversaria == 11));
        this.equipeMaoDeOnze = maoDeOnze
                ? (pontosEquipeJogador == 11 ? TrucoTeam.EQUIPE_JOGADOR : TrucoTeam.EQUIPE_ADVERSARIA)
                : null;

        this.vazaAtual = new TrucoTableTrick(1, seats.size(), primeiroSeatDaMaoAtual, vira);

        if (maoDeFerro) {
            this.ultimaAcaoDescricao = "Mão de Ferro (11 × 11)! Vale 1 ponto — sem pedidos de Truco.";
        } else if (maoDeOnze) {
            this.pedidoTrucoPendente = true;
            this.valorPropostoTruco = 3;
            this.equipeQuePediuTruco = equipeMaoDeOnze.oponente();
            this.nomeQuemPediuTruco = "Mão de 11";
            this.equipeQueDeveResponderTruco = equipeMaoDeOnze;
            this.ultimaAcaoDescricao = "Mão de 11 (" + equipeMaoDeOnze.getNome()
                    + "): aceitar jogar valendo 3 pontos ou correr (1 ponto ao adversário).";
        } else {
            this.ultimaAcaoDescricao = "Nova mão iniciada! Vira: " + vira.getSiglaVisual()
                    + " (Manilha: " + manilhaRank.getSimbolo() + "). Começa: "
                    + seats.get(seatVezAtual).getNome() + ".";
        }

        return true;
    }

    // =========================================================================
    // ACOES DO JOGADOR HUMANO
    // =========================================================================

    /**
     * Joga uma carta da mao do jogador humano quando for seu turno e nao houver
     * pedido de Truco nem transicao de vaza pendente.
     */
    public boolean jogarCartaJogador(Card carta) {
        if (!podeJogadorJogarCarta() || carta == null) {
            return false;
        }
        TrucoSeat humano = seats.get(0);
        if (!humano.contemCarta(carta)) {
            return false;
        }
        executarJogadaCarta(humano, carta);
        return true;
    }

    /**
     * Joga a carta no indice informado da mao do jogador humano (0..size-1).
     */
    public boolean jogarCartaJogadorPorIndice(int indice) {
        List<Card> mao = getMaoJogador();
        if (indice < 0 || indice >= mao.size()) {
            return false;
        }
        return jogarCartaJogador(mao.get(indice));
    }

    /**
     * Jogador solicita Truco / aumento de aposta (1->3, 3->6, 6->9, 9->12).
     */
    public boolean pedirTrucoJogador() {
        if (!getAcoesApostaValidasJogador().contains(TrucoBetAction.PEDIR_TRUCO)) {
            return false;
        }
        iniciarPedidoTruco(TrucoTeam.EQUIPE_JOGADOR, seats.get(0).getNome());
        return true;
    }

    /**
     * Jogador responde a um pedido pendente de Truco (ou Mao de 11): ACEITAR, CORRER ou AUMENTAR.
     */
    public boolean responderTrucoJogador(TrucoBetAction acao) {
        if (acao == null || !getAcoesApostaValidasJogador().contains(acao)) {
            return false;
        }
        if (acao == TrucoBetAction.PEDIR_TRUCO) {
            return pedirTrucoJogador();
        }
        aplicarRespostaPedidoTruco(TrucoTeam.EQUIPE_JOGADOR, seats.get(0).getNome(), acao);
        return true;
    }

    // =========================================================================
    // CONTROLE DE PASSOS DOS BOTS E TRANSICAO DE VAZA (RITMO VISIVEL)
    // =========================================================================

    /**
     * Indica se o motor aguarda a pausa de exibicao (~2s) da vaza recem-resolvida
     * antes de abrir a proxima vaza ou encerrar a mao.
     */
    public boolean isAguardandoTransicaoVaza() {
        return aguardandoTransicaoVaza;
    }

    /**
     * Indica se existe uma acao pendente de algum Bot (responder pedido de Truco ou jogar carta).
     */
    public boolean isAguardandoAcaoBot() {
        if (maoFinalizada || partidaFinalizada || aguardandoTransicaoVaza) {
            return false;
        }
        if (pedidoTrucoPendente) {
            return equipeQueDeveResponderTruco == TrucoTeam.EQUIPE_ADVERSARIA;
        }
        return !seats.get(seatVezAtual).isHumano();
    }

    /**
     * Executa exatamente UM passo do Bot atual (responder pedido de Truco, pedir Truco ou jogar 1 carta).
     * Deve ser chamado pela interface apos a pausa visivel (~1s).
     */
    public boolean executarPassoBot() {
        if (!isAguardandoAcaoBot()) {
            return false;
        }

        // 1. Bot responde a um pedido pendente de Truco ou Mao de 11
        if (pedidoTrucoPendente && equipeQueDeveResponderTruco == TrucoTeam.EQUIPE_ADVERSARIA) {
            TrucoSeat representanteBot = encontrarBotDaEquipe(TrucoTeam.EQUIPE_ADVERSARIA);
            boolean podeAumentar = !maoDeOnze && !maoDeFerro && valorPropostoTruco < 12;
            TrucoBetAction resposta = botAI.responderPedidoTruco(
                    representanteBot.getMaoInternaSnapshot(),
                    vira,
                    valorPropostoTruco,
                    podeAumentar,
                    getVazasVencidasEquipe(TrucoTeam.EQUIPE_ADVERSARIA),
                    getVazasVencidasEquipe(TrucoTeam.EQUIPE_JOGADOR)
            );
            if (resposta == TrucoBetAction.AUMENTAR && !podeAumentar) {
                resposta = TrucoBetAction.ACEITAR;
            }
            if (resposta == null || resposta == TrucoBetAction.PEDIR_TRUCO) {
                resposta = TrucoBetAction.ACEITAR;
            }
            aplicarRespostaPedidoTruco(TrucoTeam.EQUIPE_ADVERSARIA, representanteBot.getNome(), resposta);
            return true;
        }

        // 2. Turno de um Bot jogar (pode decidir pedir Truco antes da carta se permitido)
        TrucoSeat botAtual = seats.get(seatVezAtual);
        if (!botJaAvaliouPedirTrucoNesteTurno && podeEquipePedirTruco(botAtual.getEquipe())) {
            botJaAvaliouPedirTrucoNesteTurno = true;
            int proximo = proximoValorAposta(valorMaoAtual);
            boolean querPedir = botAI.decidirPedirTruco(
                    botAtual.getMaoInternaSnapshot(),
                    vira,
                    botAtual.getEquipe(),
                    valorMaoAtual,
                    proximo,
                    getVazasVencidasEquipe(botAtual.getEquipe()),
                    getVazasVencidasEquipe(botAtual.getEquipe().oponente())
            );
            if (querPedir) {
                iniciarPedidoTruco(botAtual.getEquipe(), botAtual.getNome());
                return true;
            }
        }

        // 3. Bot escolhe e joga 1 carta
        Card escolhida = botAI.escolherCarta(
                botAtual.getMaoInternaSnapshot(),
                vira,
                vazaAtual.getJogadas(),
                botAtual.getEquipe()
        );
        executarJogadaCarta(botAtual, escolhida);
        return true;
    }

    /**
     * Conclui a pausa visual da vaza resolvida (~2s): verifica se a mao terminou ou abre a proxima vaza.
     */
    public boolean avancarAposPausaVaza() {
        if (!aguardandoTransicaoVaza) {
            return false;
        }
        this.aguardandoTransicaoVaza = false;
        avaliarDesfechoOuProximaVaza();
        return true;
    }

    // =========================================================================
    // LOGICA INTERNA DE JOGADAS, APOSTAS E DESFECHO
    // =========================================================================

    private void iniciarPedidoTruco(TrucoTeam equipePedinte, String nomePedinte) {
        int proximo = proximoValorAposta(valorMaoAtual);
        this.pedidoTrucoPendente = true;
        this.valorPropostoTruco = proximo;
        this.equipeQuePediuTruco = equipePedinte;
        this.nomeQuemPediuTruco = nomePedinte;
        this.equipeQueDeveResponderTruco = equipePedinte.oponente();
        this.ultimaAcaoDescricao = nomePedinte + " pediu " + nomeDoValorAposta(proximo)
                + " (" + proximo + " pts)! Aguardando " + equipeQueDeveResponderTruco.getNome() + ".";
    }

    private void aplicarRespostaPedidoTruco(TrucoTeam equipeRespondente, String nomeRespondente, TrucoBetAction acao) {
        if (acao == TrucoBetAction.CORRER) {
            TrucoTeam vencedora = equipeRespondente.oponente();
            int pontosGanhos = this.valorMaoAtual; // Valor aceito anteriormente (1, 3, 6 ou 9)
            this.pedidoTrucoPendente = false;
            this.ultimaAcaoDescricao = nomeRespondente + " correu! " + vencedora.getNome()
                    + " ganha " + pontosGanhos + " ponto(s).";
            encerrarMao(vencedora, pontosGanhos);
            return;
        }

        if (acao == TrucoBetAction.ACEITAR) {
            this.valorMaoAtual = this.valorPropostoTruco;
            this.ultimaEquipeQueAumentou = this.equipeQuePediuTruco;
            this.pedidoTrucoPendente = false;
            this.ultimaAcaoDescricao = nomeRespondente + " aceitou! Mão agora vale "
                    + this.valorMaoAtual + " pontos.";
            return;
        }

        if (acao == TrucoBetAction.AUMENTAR && !maoDeOnze && !maoDeFerro && valorPropostoTruco < 12) {
            this.valorMaoAtual = this.valorPropostoTruco;
            int novoProposto = proximoValorAposta(this.valorPropostoTruco);
            this.valorPropostoTruco = novoProposto;
            this.equipeQuePediuTruco = equipeRespondente;
            this.nomeQuemPediuTruco = nomeRespondente;
            this.equipeQueDeveResponderTruco = equipeRespondente.oponente();
            this.ultimaEquipeQueAumentou = equipeRespondente;
            this.ultimaAcaoDescricao = nomeRespondente + " aumentou para " + nomeDoValorAposta(novoProposto)
                    + " (" + novoProposto + " pts)! Aguardando " + equipeQueDeveResponderTruco.getNome() + ".";
        }
    }

    private void executarJogadaCarta(TrucoSeat seat, Card carta) {
        seat.removerCarta(carta);
        this.botJaAvaliouPedirTrucoNesteTurno = false;
        TrucoPlayedCard jogada = new TrucoPlayedCard(seat.getSeatIndex(), seat.getNome(), seat.getEquipe(), carta);
        this.vazaAtual = this.vazaAtual.comJogada(jogada);

        if (vazaAtual.isCompleta()) {
            this.vazasConcluidas.add(vazaAtual);
            this.ultimaVazaResolvida = vazaAtual;
            this.aguardandoTransicaoVaza = true;
            this.ultimaAcaoDescricao = seat.getNome() + " jogou " + carta.getSiglaVisual()
                    + " — " + vazaAtual.getResumoResultado();
        } else {
            this.seatVezAtual = (seat.getSeatIndex() + 1) % seats.size();
            this.ultimaAcaoDescricao = seat.getNome() + " jogou " + carta.getSiglaVisual()
                    + ". Vez de " + seats.get(seatVezAtual).getNome() + ".";
        }
    }

    private void avaliarDesfechoOuProximaVaza() {
        int total = vazasConcluidas.size();
        if (total == 1) {
            TrucoTableTrick v1 = vazasConcluidas.get(0);
            int proxStarter = v1.isEmpate() ? v1.getPrimeiroSeatIndex() : v1.getSeatVencedor();
            abrirProximaVaza(2, proxStarter);
            return;
        }

        if (total == 2) {
            TrucoTableTrick v1 = vazasConcluidas.get(0);
            TrucoTableTrick v2 = vazasConcluidas.get(1);

            if (!v1.isEmpate() && !v2.isEmpate()) {
                if (v1.getEquipeVencedora() == v2.getEquipeVencedora()) {
                    encerrarMao(v1.getEquipeVencedora(), valorMaoAtual);
                    return;
                }
                abrirProximaVaza(3, v2.getSeatVencedor());
                return;
            }
            if (v1.isEmpate() && !v2.isEmpate()) {
                encerrarMao(v2.getEquipeVencedora(), valorMaoAtual);
                return;
            }
            if (!v1.isEmpate() && v2.isEmpate()) {
                encerrarMao(v1.getEquipeVencedora(), valorMaoAtual);
                return;
            }
            // Ambas empataram -> 3a vaza decide
            abrirProximaVaza(3, v2.getPrimeiroSeatIndex());
            return;
        }

        // 3a vaza concluida
        TrucoTableTrick v1 = vazasConcluidas.get(0);
        TrucoTableTrick v3 = vazasConcluidas.get(2);
        if (!v3.isEmpate()) {
            encerrarMao(v3.getEquipeVencedora(), valorMaoAtual);
        } else if (!v1.isEmpate()) {
            encerrarMao(v1.getEquipeVencedora(), valorMaoAtual);
        } else {
            // Tres empates seguidos: ninguem pontua
            encerrarMao(null, 0);
        }
    }

    private void abrirProximaVaza(int numero, int starterSeat) {
        this.vazaAtual = new TrucoTableTrick(numero, seats.size(), starterSeat, vira);
        this.seatVezAtual = starterSeat;
        this.botJaAvaliouPedirTrucoNesteTurno = false;
        this.ultimaAcaoDescricao = "Início da Vaza " + numero + ". Começa: "
                + seats.get(starterSeat).getNome() + ".";
    }

    private void encerrarMao(TrucoTeam vencedora, int pontosGanhos) {
        this.maoFinalizada = true;
        this.aguardandoTransicaoVaza = false;
        this.pedidoTrucoPendente = false;
        this.equipeVencedoraMao = vencedora;

        if (vencedora == TrucoTeam.EQUIPE_JOGADOR) {
            this.pontosEquipeJogador += pontosGanhos;
            this.ultimaAcaoDescricao = "Sua Equipe venceu a mão (+" + pontosGanhos + " pts)! Placar: "
                    + pontosEquipeJogador + " × " + pontosEquipeAdversaria + ".";
        } else if (vencedora == TrucoTeam.EQUIPE_ADVERSARIA) {
            this.pontosEquipeAdversaria += pontosGanhos;
            this.ultimaAcaoDescricao = "Adversários venceram a mão (+" + pontosGanhos + " pts). Placar: "
                    + pontosEquipeJogador + " × " + pontosEquipeAdversaria + ".";
        } else {
            this.ultimaAcaoDescricao = "Mão empatada por três cangas — nenhum ponto marcado.";
        }

        if (pontosEquipeJogador >= PONTOS_VITORIA_PARTIDA) {
            this.partidaFinalizada = true;
            this.equipeVencedoraPartida = TrucoTeam.EQUIPE_JOGADOR;
            this.ultimaAcaoDescricao = "VITÓRIA NA PARTIDA! Sua Equipe atingiu " + pontosEquipeJogador + " pontos!";
        } else if (pontosEquipeAdversaria >= PONTOS_VITORIA_PARTIDA) {
            this.partidaFinalizada = true;
            this.equipeVencedoraPartida = TrucoTeam.EQUIPE_ADVERSARIA;
            this.ultimaAcaoDescricao = "FIM DE PARTIDA! Adversários atingiram " + pontosEquipeAdversaria + " pontos.";
        }
    }

    private boolean podeEquipePedirTruco(TrucoTeam equipe) {
        if (maoFinalizada || partidaFinalizada || aguardandoTransicaoVaza || pedidoTrucoPendente) {
            return false;
        }
        if (maoDeOnze || maoDeFerro || valorMaoAtual >= 12) {
            return false;
        }
        return ultimaEquipeQueAumentou != equipe;
    }

    private TrucoSeat encontrarBotDaEquipe(TrucoTeam equipe) {
        for (TrucoSeat s : seats) {
            if (!s.isHumano() && s.getEquipe() == equipe) {
                return s;
            }
        }
        return seats.get(1);
    }

    public static int proximoValorAposta(int valorAtual) {
        switch (valorAtual) {
            case 1:
                return 3;
            case 3:
                return 6;
            case 6:
                return 9;
            case 9:
                return 12;
            default:
                return 12;
        }
    }

    public static String nomeDoValorAposta(int valor) {
        switch (valor) {
            case 3:
                return "TRUCO";
            case 6:
                return "SEIS";
            case 9:
                return "NOVE";
            case 12:
                return "DOZE";
            default:
                return valor + " pontos";
        }
    }

    // =========================================================================
    // CONSULTAS PUBLICAS PARA VIEW E HUD (SEM VAZAR CARTAS DOS BOTS)
    // =========================================================================

    public boolean podeJogadorJogarCarta() {
        return !maoFinalizada && !partidaFinalizada && !aguardandoTransicaoVaza
                && !pedidoTrucoPendente && seatVezAtual == 0;
    }

    public Set<TrucoBetAction> getAcoesApostaValidasJogador() {
        if (maoFinalizada || partidaFinalizada || aguardandoTransicaoVaza) {
            return Collections.emptySet();
        }
        EnumSet<TrucoBetAction> acoes = EnumSet.noneOf(TrucoBetAction.class);
        if (pedidoTrucoPendente) {
            if (equipeQueDeveResponderTruco == TrucoTeam.EQUIPE_JOGADOR) {
                acoes.add(TrucoBetAction.ACEITAR);
                acoes.add(TrucoBetAction.CORRER);
                if (!maoDeOnze && !maoDeFerro && valorPropostoTruco < 12) {
                    acoes.add(TrucoBetAction.AUMENTAR);
                }
            }
            return Collections.unmodifiableSet(acoes);
        }
        if (seatVezAtual == 0 && podeEquipePedirTruco(TrucoTeam.EQUIPE_JOGADOR)) {
            acoes.add(TrucoBetAction.PEDIR_TRUCO);
        }
        return Collections.unmodifiableSet(acoes);
    }

    public TrucoMode getModo() {
        return modo;
    }

    public List<TrucoSeat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    public List<Card> getMaoJogador() {
        return seats.get(0).getMaoInternaSnapshot();
    }

    public Card getVira() {
        return vira;
    }

    public Rank getManilhaRank() {
        return manilhaRank;
    }

    public int getPontosEquipeJogador() {
        return pontosEquipeJogador;
    }

    public int getPontosEquipeAdversaria() {
        return pontosEquipeAdversaria;
    }

    public int getValorMaoAtual() {
        return valorMaoAtual;
    }

    public boolean isMaoDeOnze() {
        return maoDeOnze;
    }

    public boolean isMaoDeFerro() {
        return maoDeFerro;
    }

    public boolean isPedidoTrucoPendente() {
        return pedidoTrucoPendente;
    }

    public int getValorPropostoTruco() {
        return valorPropostoTruco;
    }

    public TrucoTeam getEquipeQuePediuTruco() {
        return equipeQuePediuTruco;
    }

    public String getNomeQuemPediuTruco() {
        return nomeQuemPediuTruco;
    }

    public TrucoTeam getEquipeQueDeveResponderTruco() {
        return equipeQueDeveResponderTruco;
    }

    public int getSeatVezAtual() {
        return seatVezAtual;
    }

    public TrucoTableTrick getVazaAtual() {
        return vazaAtual;
    }

    public TrucoTableTrick getUltimaVazaResolvida() {
        return ultimaVazaResolvida;
    }

    public List<TrucoTableTrick> getVazasConcluidas() {
        return List.copyOf(vazasConcluidas);
    }

    public int getVazasVencidasEquipe(TrucoTeam equipe) {
        int count = 0;
        for (TrucoTableTrick t : vazasConcluidas) {
            if (t.isCompleta() && t.getEquipeVencedora() == equipe) {
                count++;
            }
        }
        return count;
    }

    public boolean isMaoFinalizada() {
        return maoFinalizada;
    }

    public TrucoTeam getEquipeVencedoraMao() {
        return equipeVencedoraMao;
    }

    public boolean isPartidaFinalizada() {
        return partidaFinalizada;
    }

    public TrucoTeam getEquipeVencedoraPartida() {
        return equipeVencedoraPartida;
    }

    public String getUltimaAcaoDescricao() {
        return ultimaAcaoDescricao;
    }

    public boolean hasProgressoNaPartida() {
        return pontosEquipeJogador > 0 || pontosEquipeAdversaria > 0
                || !vazasConcluidas.isEmpty()
                || (vazaAtual != null && !vazaAtual.getJogadas().isEmpty())
                || pedidoTrucoPendente;
    }
}
