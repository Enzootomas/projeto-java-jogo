package tests;

import model.Card;
import model.Rank;
import model.Suit;
import model.TrucoBetAction;
import model.TrucoDeck;
import model.TrucoMatch;
import model.TrucoMatchAI;
import model.TrucoMode;
import model.TrucoPlayedCard;
import model.TrucoTeam;
import view.ScreenSaver;
import view.TrucoMatchView;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Suíte determinística para Truco Paulista ampliado:
 * - Modos Individual e Duplas (4 lugares alternando equipes)
 * - Pedidos de Truco (1 -> 3 -> 6 -> 9 -> 12), pontuação ao correr, Mão de 11 e Mão de Ferro
 * - Partida encerrada aos 12 pontos
 * - Temporização visível (~1s por Bot e ~2s por vaza resolvida), última vaza consultável,
 *   bloqueio de jogadas duplicadas, pausa ao sair com B e retomada exata ao voltar com E
 * - Confirmação obrigatória ao trocar de modo com partida em andamento
 */
public class TesteTrucoDuplasEApostas {

    private static int totalVerificacoes = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DE TRUCO PAULISTA (DUPLAS, APOSTAS E RITMO)");
        System.out.println("====================================================");

        testarTurnosDuplasResolucaoPorEquipeECanga();
        testarEscaladaTrucoAte12PontuacaoAoCorrerEFimDePartida();
        testarMaoDeOnzeEMaoDeFerro();
        testarDesaceleracaoPausaBEretomadaEUltimaVaza();
        testarSelecaoModoConfirmacaoTrocaERenderizacao();

        System.out.println("----------------------------------------------------");
        System.out.println("Total de verificacoes (Truco Duplas/Apostas/Ritmo): " + totalVerificacoes);
        System.out.println("RESULTADO: TODOS OS TESTES DE TRUCO AMPLIADO PASSARAM! [OK]");
        System.out.println("----------------------------------------------------");
    }

    private static void afirme(boolean condicao, String mensagem) {
        totalVerificacoes++;
        if (!condicao) {
            throw new AssertionError("FALHA: " + mensagem);
        }
    }

    private static Card c(Rank rank, Suit suit) {
        return new Card(suit, rank);
    }

    /** IA determinística que nunca pede Truco sozinha, joga sempre a 1a carta e responde conforme configurado. */
    private static TrucoMatchAI iaControlada(TrucoBetAction respostaPadrao, boolean pedeTruco) {
        return new TrucoMatchAI() {
            @Override
            public Card escolherCarta(List<Card> maoPropria, Card vira, List<TrucoPlayedCard> jogadas, TrucoTeam equipe) {
                return maoPropria.get(0);
            }

            @Override
            public boolean decidirPedirTruco(List<Card> maoPropria, Card vira, TrucoTeam equipe,
                                             int valorAtual, int valorProposto, int vMinha, int vOp) {
                return pedeTruco;
            }

            @Override
            public TrucoBetAction responderPedidoTruco(List<Card> maoPropria, Card vira, int valorProposto,
                                                       boolean podeAumentar, int vMinha, int vOp) {
                return respostaPadrao;
            }
        };
    }

    private static TrucoDeck criarBaralhoDuplas(Card vira) {
        // 4 jogadores x 3 cartas + 1 vira = 13 cartas
        List<Card> cartas = new ArrayList<>();
        // Seat 0 (Você - Equipe Jogador): 4♠, 3♣, 2♠
        cartas.add(c(Rank.QUATRO, Suit.ESPADAS));
        cartas.add(c(Rank.TRES, Suit.PAUS));
        cartas.add(c(Rank.DOIS, Suit.ESPADAS));
        // Seat 1 (Bot 1 - Adversários): 5♠, 3♥, 4♥
        cartas.add(c(Rank.CINCO, Suit.ESPADAS));
        cartas.add(c(Rank.TRES, Suit.COPAS));
        cartas.add(c(Rank.QUATRO, Suit.COPAS));
        // Seat 2 (Parceiro - Equipe Jogador): K♣ (Zap se vira=J), 6♠, 5♥
        cartas.add(c(Rank.REI, Suit.PAUS));
        cartas.add(c(Rank.SEIS, Suit.ESPADAS));
        cartas.add(c(Rank.CINCO, Suit.COPAS));
        // Seat 3 (Bot 2 - Adversários): 7♠, 6♥, 7♥
        cartas.add(c(Rank.SETE, Suit.ESPADAS));
        cartas.add(c(Rank.SEIS, Suit.COPAS));
        cartas.add(c(Rank.SETE, Suit.COPAS));
        // Vira
        cartas.add(vira);
        return new TrucoDeck(cartas);
    }

    private static void testarTurnosDuplasResolucaoPorEquipeECanga() {
        System.out.println("[1/5] Testando modo Duplas (4 lugares, alternância de equipes, vitória e canga)...");
        // Vira Valete -> Manilha = Rei (K♣ do Parceiro é o Zap!)
        TrucoMatch match = new TrucoMatch(TrucoMode.DUPLAS, iaControlada(TrucoBetAction.ACEITAR, false), 0, 0, 0);
        afirme(match.iniciarNovaMao(criarBaralhoDuplas(c(Rank.VALETE, Suit.OUROS))), "Mão de Duplas iniciada.");
        afirme(match.getSeats().size() == 4, "Modo Duplas possui exatamente 4 lugares.");
        afirme(match.getSeats().get(0).getEquipe() == TrucoTeam.EQUIPE_JOGADOR
                        && match.getSeats().get(1).getEquipe() == TrucoTeam.EQUIPE_ADVERSARIA
                        && match.getSeats().get(2).getEquipe() == TrucoTeam.EQUIPE_JOGADOR
                        && match.getSeats().get(3).getEquipe() == TrucoTeam.EQUIPE_ADVERSARIA,
                "Assentos alternam corretamente entre Equipe Jogador e Adversários.");

        // Vaza 1: Seat 0 (4♠) -> Seat 1 (5♠) -> Seat 2 (K♣ Zap!) -> Seat 3 (7♠) => Equipe Jogador vence Vaza 1!
        afirme(match.getSeatVezAtual() == 0, "Turno inicial é do Seat 0 (Você).");
        afirme(match.jogarCartaJogadorPorIndice(0), "Você jogou 4♠.");
        afirme(match.getSeatVezAtual() == 1, "Turno passou para Seat 1 (Bot 1).");
        afirme(match.executarPassoBot(), "Bot 1 jogou 5♠.");
        afirme(match.getSeatVezAtual() == 2, "Turno passou para Seat 2 (Parceiro).");
        afirme(match.executarPassoBot(), "Parceiro jogou K♣ (Zap).");
        afirme(match.getSeatVezAtual() == 3, "Turno passou para Seat 3 (Bot 2).");
        afirme(match.executarPassoBot(), "Bot 2 jogou 7♠.");

        afirme(match.isAguardandoTransicaoVaza(), "Após 4 jogadas, vaza aguarda pausa visual antes de avançar.");
        afirme(match.getVazaAtual().isCompleta()
                        && match.getVazaAtual().getEquipeVencedora() == TrucoTeam.EQUIPE_JOGADOR,
                "Parceiro garantiu vitória da Vaza 1 para a Equipe Jogador.");
        afirme(match.getUltimaVazaResolvida() == match.getVazaAtual(),
                "Última vaza resolvida fica registrada para consulta.");

        afirme(match.avancarAposPausaVaza(), "Avançou para Vaza 2 após pausa.");
        afirme(match.getSeatVezAtual() == 2, "Quem venceu a Vaza 1 (Seat 2 - Parceiro) abre a Vaza 2.");

        // Vaza 2: Seat 2 (6♠) -> Seat 3 (6♥) -> Seat 0 (3♣) -> Seat 1 (3♥) => Empate (Canga) entre 3♣ e 3♥!
        afirme(match.executarPassoBot(), "Parceiro jogou 6♠.");
        afirme(match.executarPassoBot(), "Bot 2 jogou 6♥.");
        afirme(match.jogarCartaJogadorPorIndice(0), "Você jogou 3♣.");
        afirme(match.executarPassoBot(), "Bot 1 jogou 3♥.");

        afirme(match.getVazaAtual().isEmpate(), "3♣ x 3♥ resultou em Empate (Canga) na Vaza 2.");
        afirme(match.avancarAposPausaVaza(), "Empate na 2a vaza encerra a mão a favor de quem venceu a 1a!");
        afirme(match.isMaoFinalizada() && match.getEquipeVencedoraMao() == TrucoTeam.EQUIPE_JOGADOR,
                "Equipe Jogador venceu a mão (Vaza 1 + Canga na Vaza 2).");
        afirme(match.getPontosEquipeJogador() == 1 && match.getPontosEquipeAdversaria() == 0,
                "Placar atualizado para 1 x 0.");
        System.out.println("  -> Sucesso: Turnos em Duplas, vitória por equipe e canga validados.");
    }

    private static void testarEscaladaTrucoAte12PontuacaoAoCorrerEFimDePartida() {
        System.out.println("[2/5] Testando pedidos 1->3->6->9->12, bloqueio de jogadas, correr e fim aos 12 pts...");

        // 2.1 Jogador pede Truco (3) e Bot corre -> Jogador ganha 1 ponto (valor aceito anteriormente)
        TrucoMatch matchCorrer = new TrucoMatch(TrucoMode.INDIVIDUAL, iaControlada(TrucoBetAction.CORRER, false), 0, 0, 0);
        matchCorrer.iniciarNovaMao(new TrucoDeck());
        afirme(matchCorrer.pedirTrucoJogador(), "Jogador pediu Truco (3 pts).");
        afirme(matchCorrer.isPedidoTrucoPendente() && matchCorrer.getValorPropostoTruco() == 3,
                "Pedido pendente de 3 pts registrado.");
        afirme(!matchCorrer.jogarCartaJogadorPorIndice(0),
                "Jogada de carta é bloqueada enquanto houver pedido de Truco pendente.");
        afirme(matchCorrer.executarPassoBot(), "Bot respondeu ao pedido de Truco correndo.");
        afirme(matchCorrer.isMaoFinalizada() && matchCorrer.getPontosEquipeJogador() == 1,
                "Quando Bot corre do Truco (3), Equipe Jogador recebe o valor aceito anterior (1 ponto).");

        // 2.2 Escalada completa 1 -> 3 -> 6 -> 9 -> 12 e fim de partida aos 12 pontos!
        TrucoMatch matchDoze = new TrucoMatch(TrucoMode.INDIVIDUAL, iaControlada(TrucoBetAction.AUMENTAR, false), 0, 0, 0);
        List<Card> baralhoVitoria = List.of(
                c(Rank.TRES, Suit.PAUS), c(Rank.TRES, Suit.COPAS), c(Rank.TRES, Suit.OUROS),
                c(Rank.QUATRO, Suit.ESPADAS), c(Rank.QUATRO, Suit.COPAS), c(Rank.QUATRO, Suit.OUROS),
                c(Rank.SETE, Suit.OUROS)
        );
        matchDoze.iniciarNovaMao(new TrucoDeck(baralhoVitoria));

        afirme(matchDoze.pedirTrucoJogador(), "Jogador pediu Truco (3).");
        afirme(matchDoze.executarPassoBot(), "Bot aumentou para Seis (6).");
        afirme(matchDoze.getValorMaoAtual() == 3 && matchDoze.getValorPropostoTruco() == 6,
                "Valor aceito subiu para 3 e proposta atual é 6.");
        afirme(matchDoze.responderTrucoJogador(TrucoBetAction.AUMENTAR), "Jogador aumentou para Nove (9).");
        afirme(matchDoze.getValorMaoAtual() == 6 && matchDoze.getValorPropostoTruco() == 9,
                "Valor aceito subiu para 6 e proposta atual é 9.");
        afirme(matchDoze.executarPassoBot(), "Bot aumentou para Doze (12).");
        afirme(matchDoze.getValorMaoAtual() == 9 && matchDoze.getValorPropostoTruco() == 12,
                "Valor aceito subiu para 9 e proposta atual é 12.");
        afirme(!matchDoze.getAcoesApostaValidasJogador().contains(TrucoBetAction.AUMENTAR),
                "No pedido de Doze (12), não é permitido aumentar além de 12.");
        afirme(matchDoze.responderTrucoJogador(TrucoBetAction.ACEITAR), "Jogador aceitou jogar valendo 12!");
        afirme(matchDoze.getValorMaoAtual() == 12 && !matchDoze.isPedidoTrucoPendente(),
                "Mão confirmada valendo 12 pontos.");

        // Joga 2 vazas (2x0) vencendo a mão de 12 pontos -> encerra a partida!
        matchDoze.jogarCartaJogadorPorIndice(0);
        matchDoze.executarPassoBot();
        matchDoze.avancarAposPausaVaza();
        matchDoze.jogarCartaJogadorPorIndice(0);
        matchDoze.executarPassoBot();
        matchDoze.avancarAposPausaVaza();

        afirme(matchDoze.isPartidaFinalizada() && matchDoze.getEquipeVencedoraPartida() == TrucoTeam.EQUIPE_JOGADOR,
                "Partida encerrada quando Equipe Jogador atingiu 12 pontos!");
        afirme(matchDoze.getPontosEquipeJogador() == 12, "Placar final registrou 12 pontos.");

        // 2.3 Jogador corre de um pedido de Seis (6) após ter aceito Truco (3) -> Adversário ganha 3 pts
        TrucoMatch matchCorrerSeis = new TrucoMatch(TrucoMode.INDIVIDUAL, iaControlada(TrucoBetAction.AUMENTAR, false), 0, 0, 0);
        matchCorrerSeis.iniciarNovaMao(new TrucoDeck());
        matchCorrerSeis.pedirTrucoJogador();      // propõe 3
        matchCorrerSeis.executarPassoBot();       // aceita 3 e propõe 6
        afirme(matchCorrerSeis.responderTrucoJogador(TrucoBetAction.CORRER), "Jogador correu do Seis (6).");
        afirme(matchCorrerSeis.getPontosEquipeAdversaria() == 3,
                "Ao correr do Seis (6), Adversários recebem exatamente 3 pontos (valor aceito anterior).");
        System.out.println("  -> Sucesso: Escalada 1->3->6->9->12, correr e encerramento aos 12 pts validados.");
    }

    private static void testarMaoDeOnzeEMaoDeFerro() {
        System.out.println("[3/5] Testando regras Paulista de Mão de 11 e Mão de Ferro...");

        // Mão de 11: Equipe Jogador com 11 x 6
        TrucoMatch m11 = new TrucoMatch(TrucoMode.INDIVIDUAL, iaControlada(TrucoBetAction.ACEITAR, false), 11, 6, 0);
        m11.iniciarNovaMao(new TrucoDeck());
        afirme(m11.isMaoDeOnze() && m11.isPedidoTrucoPendente() && m11.getValorPropostoTruco() == 3,
                "Com 11 x 6, Mão de 11 exige decisão de jogar por 3 pts ou correr.");
        afirme(!m11.getAcoesApostaValidasJogador().contains(TrucoBetAction.AUMENTAR),
                "Na Mão de 11 não é permitido aumentar para 6.");
        afirme(m11.responderTrucoJogador(TrucoBetAction.CORRER), "Equipe de 11 correu da Mão de 11.");
        afirme(m11.getPontosEquipeAdversaria() == 7, "Ao correr da Mão de 11, adversário ganha 1 ponto (6 -> 7).");

        // Mão de Ferro: 11 x 11
        TrucoMatch mFerro = new TrucoMatch(TrucoMode.INDIVIDUAL, iaControlada(TrucoBetAction.ACEITAR, false), 11, 11, 0);
        mFerro.iniciarNovaMao(new TrucoDeck());
        afirme(mFerro.isMaoDeFerro() && mFerro.getValorMaoAtual() == 1 && !mFerro.isPedidoTrucoPendente(),
                "Com 11 x 11, inicia Mão de Ferro valendo 1 ponto sem pedido pendente.");
        afirme(!mFerro.getAcoesApostaValidasJogador().contains(TrucoBetAction.PEDIR_TRUCO),
                "Na Mão de Ferro pedidos de Truco são proibidos.");
        System.out.println("  -> Sucesso: Mão de 11 e Mão de Ferro validadas.");
    }

    private static void testarDesaceleracaoPausaBEretomadaEUltimaVaza() {
        System.out.println("[4/5] Testando pausas ~1s/Bot e ~2s/vaza, bloqueio de duplicadas e pausa B/retomada E...");
        ScreenSaver painel = new ScreenSaver();
        try {
            TrucoMatch match = new TrucoMatch(TrucoMode.DUPLAS, iaControlada(TrucoBetAction.ACEITAR, false), 0, 0, 0);
            match.iniciarNovaMao(criarBaralhoDuplas(c(Rank.VALETE, Suit.OUROS)));
            TrucoMatchView view = painel.getTrucoMatchView();
            view.setMatch(match);

            painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);

            // Jogador joga carta 0 -> pausa de ~1.0s para o Bot 1!
            afirme(view.jogarCartaIndex(0), "Jogador jogou a 1a carta.");
            afirme(match.getVazaAtual().getJogadas().size() == 1,
                    "Bot 1 NÃO joga instantaneamente — aguarda pausa visível de ~1 segundo.");
            afirme("BOT".equals(view.getTipoPausaAtiva()) && view.getCronometroPausaSegundos() >= 0.9,
                    "Temporizador de pausa do Bot (~1.0s) está armado.");
            afirme(!view.jogarCartaIndex(0),
                    "Jogada duplicada do jogador é bloqueada enquanto o Bot está pendente.");

            // Sai com B para o Salão -> tempo NÃO avança enquanto fora da mesa!
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Saiu com B para o Salão.");
            painel.avancarTempoTruco(5.0);
            afirme(match.getVazaAtual().getJogadas().size() == 1,
                    "Ao sair com B, as transições visuais pausam e preservam exatamente a mesa.");

            // Retorna com E -> retoma exatamente do ponto pausado!
            painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);
            painel.avancarTempoTruco(0.5);
            afirme(match.getVazaAtual().getJogadas().size() == 1, "Com 0.5s ainda aguarda Bot 1.");
            painel.avancarTempoTruco(0.6); // total 1.1s >= 1.0s
            afirme(match.getVazaAtual().getJogadas().size() == 2, "Após ~1.0s na tela, Bot 1 jogou sua carta.");

            // Avança mais ~1.0s para Parceiro e ~1.0s para Bot 2 -> completa Vaza 1!
            painel.avancarTempoTruco(1.05);
            afirme(match.getVazaAtual().getJogadas().size() == 3, "Após ~1.0s, Parceiro jogou sua carta.");
            painel.avancarTempoTruco(1.05);
            afirme(match.getVazaAtual().getJogadas().size() == 4 && match.isAguardandoTransicaoVaza(),
                    "Após ~1.0s, Bot 2 jogou e vaza resolvida entrou na pausa de ~2.0s.");
            afirme("VAZA".equals(view.getTipoPausaAtiva()) && view.getCronometroPausaSegundos() >= 1.9,
                    "Pausa de vaza resolvida (~2.0s) mantém todas as 4 cartas visíveis.");

            // Sai com B no meio dos 2.0s e volta com E -> cartas continuam visíveis até completar os 2.0s!
            painel.avancarTempoTruco(1.0);
            painel.processarRetornoB();
            painel.avancarTempoTruco(10.0); // pausado no salão!
            afirme(match.isAguardandoTransicaoVaza() && match.getVazaAtual().getJogadas().size() == 4,
                    "Pausa de vaza preservada intacta ao sair com B.");
            painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);
            painel.avancarTempoTruco(1.1); // completa 2.1s -> abre Vaza 2!
            afirme(!match.isAguardandoTransicaoVaza() && match.getVazaAtual().getNumeroVaza() == 2,
                    "Após completar ~2.0s na tela, abriu a Vaza 2.");
            afirme(match.getUltimaVazaResolvida() != null && match.getUltimaVazaResolvida().getNumeroVaza() == 1,
                    "Última vaza (Vaza 1) permanece disponível para consulta durante a Vaza 2.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Pausa ~1s/Bot, ~2s/vaza, pausa em B e retomada em E validadas.");
    }

    private static void testarSelecaoModoConfirmacaoTrocaERenderizacao() {
        System.out.println("[5/5] Testando seleção de modo, confirmação ao trocar e renderização off-screen...");
        ScreenSaver painel = new ScreenSaver();
        try {
            TrucoMatchView view = painel.getTrucoMatchView();
            afirme(view.isExibindoSelecaoModo(), "Ao iniciar, mesa de Truco exibe escolha entre Individual e Duplas.");

            // Escolhe modo Duplas via atalho
            painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);
            painel.processarComandoTruco("DUPLAS");
            afirme(!view.isExibindoSelecaoModo() && view.getMatch().getModo() == TrucoMode.DUPLAS,
                    "Atalho DUPLAS iniciou partida em modo Duplas.");

            // Simula progresso (1 carta jogada) e tenta trocar de modo -> exige confirmação!
            view.jogarCartaIndex(0);
            view.solicitarTrocaDeModo();
            afirme(view.isConfirmandoTrocaModo(),
                    "Trocar de modo durante partida em andamento exige confirmação explícita.");
            view.cancelarTrocaDeModo();
            afirme(!view.isConfirmandoTrocaModo() && view.getMatch() != null,
                    "Cancelar troca preserva a partida em andamento.");
            view.solicitarTrocaDeModo();
            view.confirmarTrocaDeModo();
            afirme(view.isExibindoSelecaoModo(), "Confirmar troca retorna à tela de seleção de modo.");

            // Renderização off-screen em 1280x720 e 1920x1080
            int[][] resolucoes = {{1280, 720}, {1920, 1080}};
            for (int[] res : resolucoes) {
                painel.setSize(new Dimension(res[0], res[1]));
                BufferedImage img = new BufferedImage(res[0], res[1], BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                view.render(g, res[0], res[1]); // Tela seleção de modo
                view.iniciarPartidaNoModo(TrucoMode.DUPLAS);
                view.render(g, res[0], res[1]); // Mesa Duplas
                g.dispose();
            }
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Seleção de modo, confirmação de troca e renderização validados.");
    }
}
