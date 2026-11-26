package tests;

import model.Card;
import model.CasinoTable;
import model.Deck;
import model.MyCharacter;
import model.PokerAction;
import model.PokerBotStrategy;
import model.PokerEngine;
import model.PokerParticipant;
import model.PokerRoundResult;
import model.PokerStage;
import model.Rank;
import model.Suit;
import model.TexasHoldemGame;
import model.TexasHoldemTableGame;
import view.PokerTableView;
import view.ScreenSaver;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Suíte de testes automatizados da interface e integração da Mesa de Poker (Texas Hold'em),
 * validando a conexão real com {@link TexasHoldemGame} via {@link PokerEngine}, ocultação
 * rigorosa das cartas do Bot, ações contextuais, preservação de estado e navegação entre as três mesas.
 */
public class TesteInterfacePoker {

    private static int totalVerificacoes = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DE INTERFACE E INTEGRACAO DA MESA DE POKER");
        System.out.println("====================================================");

        testarInstanciacaoEMotorRealNoScreenSaver();
        testarPassagemLivreERotasEntreTresMesas();
        testarDesempateEuclidianoEAvisoMesmoDestino();
        testarTransicaoEntradaESaidaMesaPoker();
        testarPreservacaoMaoAoAlternarTelas();
        testarCincoEspacosBoardECartasAbertasPorFase();
        testarOcultamentoCartasBotEmFoldEShowdown();
        testarAcoesValidasShowdownVitoriaDerrotaEmpateEAtalhos();
        testarNavegacaoEntreTodasAsMesas();
        testarRenderizacaoOffScreen();

        System.out.println("----------------------------------------------------");
        System.out.println("Total de verificacoes (Interface Poker): " + totalVerificacoes);
        System.out.println("RESULTADO: TODOS OS 10 TESTES DE INTERFACE DO POKER PASSARAM! [OK]");
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

    private static PokerBotStrategy botPassivo() {
        return (maoBot, com, estagio, pote, valorPagar, saldoBot, bb, podeAumentar) ->
                valorPagar > 0L ? PokerBotStrategy.Decision.pagar() : PokerBotStrategy.Decision.passar();
    }

    private static TexasHoldemGame criarMotorDeterministicoVitoriaJogador() {
        List<Card> cartas = new ArrayList<>();
        // Mão 1: Jogador (SB/Botão) recebe A♠ K♠, Bot (BB) recebe 2♣ 7♦
        // Flop: A♥ K♥ 4♣ | Turn: 9♦ | River: 3♠ -> Jogador faz Dois Pares (Ases e Reis)
        cartas.add(c(Rank.AS, Suit.ESPADAS));
        cartas.add(c(Rank.DOIS, Suit.PAUS));
        cartas.add(c(Rank.REI, Suit.ESPADAS));
        cartas.add(c(Rank.SETE, Suit.OUROS));
        cartas.add(c(Rank.AS, Suit.COPAS));
        cartas.add(c(Rank.REI, Suit.COPAS));
        cartas.add(c(Rank.QUATRO, Suit.PAUS));
        cartas.add(c(Rank.NOVE, Suit.OUROS));
        cartas.add(c(Rank.TRES, Suit.ESPADAS));

        // Mão 2 (após Nova Mão): Bot alterna para Botão (SB)
        cartas.add(c(Rank.TRES, Suit.PAUS));
        cartas.add(c(Rank.DAMA, Suit.COPAS));
        cartas.add(c(Rank.OITO, Suit.OUROS));
        cartas.add(c(Rank.DAMA, Suit.ESPADAS));
        cartas.add(c(Rank.DOIS, Suit.COPAS));
        cartas.add(c(Rank.CINCO, Suit.ESPADAS));
        cartas.add(c(Rank.NOVE, Suit.PAUS));
        cartas.add(c(Rank.VALETE, Suit.OUROS));
        cartas.add(c(Rank.QUATRO, Suit.COPAS));

        TexasHoldemGame motor = new TexasHoldemGame(
                1000L, 1000L, 50L, 100L,
                new Deck(cartas),
                botPassivo(),
                PokerParticipant.JOGADOR
        );
        motor.iniciarNovaMao();
        return motor;
    }

    /**
     * 1. Testa a instanciação da mesa de Poker em (340, 50) e o acoplamento ao motor real TexasHoldemGame.
     */
    public static void testarInstanciacaoEMotorRealNoScreenSaver() {
        System.out.println("[1/10] Testando instanciação da mesa de Poker e motor real TexasHoldemGame...");
        ScreenSaver painel = new ScreenSaver();
        try {
            CasinoTable mesaPoker = painel.getMesaPoker();
            afirme(mesaPoker != null, "Mesa de Poker deve existir no painel.");
            afirme(mesaPoker.getX() == 340 && mesaPoker.getY() == 50,
                    "Mesa de Poker deve estar posicionada em (340, 50).");
            afirme("Poker".equalsIgnoreCase(mesaPoker.getGameName()),
                    "Nome do jogo da mesa deve ser 'Poker'.");
            afirme(painel.getObstaculos().contains(mesaPoker),
                    "Mesa de Poker deve constar na lista de obstáculos sólidos.");
            afirme(painel.getEntidadesRenderizaveis().contains(mesaPoker),
                    "Mesa de Poker deve constar na lista polimórfica de renderização.");

            PokerEngine engine = painel.getPokerEngine();
            afirme(engine != null, "PokerEngine deve estar inicializado no ScreenSaver.");
            afirme(engine instanceof TexasHoldemTableGame,
                    "ScreenSaver deve iniciar com o motor oficial de 6 participantes TexasHoldemTableGame.");
            afirme(((TexasHoldemTableGame) engine).getSeats().size() == 6,
                    "Mesa oficial de Poker deve iniciar com 6 lugares (1 jogador + 5 Bots).");
            afirme(painel.getPokerTableView() != null, "PokerTableView deve estar inicializada.");
            afirme(painel.getPokerTableView().getEngine() == engine,
                    "PokerTableView e ScreenSaver devem compartilhar a mesma instância de PokerEngine.");
            afirme(engine.getEstagioAtual() == PokerStage.PRE_FLOP,
                    "Mão inicial de Poker deve abrir pronta no Pré-Flop.");
            afirme(engine.getMaoJogador().size() == 2, "Jogador deve receber 2 hole cards.");
            afirme(engine.hasCartasOcultasBot() && engine.getCartasVisiveisBot().isEmpty(),
                    "Cartas do Bot devem iniciar estritamente ocultas.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Mesa de Poker conectada ao motor real TexasHoldemGame.");
    }

    /**
     * 2. Valida o caminho a pé entre as três mesas (Blackjack, Truco e Poker) sem bloqueios da mobília.
     */
    public static void testarPassagemLivreERotasEntreTresMesas() {
        System.out.println("[2/10] Testando passagem a pé e rotas livres entre as três mesas...");
        ScreenSaver painel = new ScreenSaver();
        try {
            MyCharacter player = painel.getPlayer();
            CasinoTable mesaTruco = painel.getMesaTruco();
            CasinoTable mesaPoker = painel.getMesaPoker();

            int folgaVerticalPokerCadeira = 140 - (mesaPoker.getY() + mesaPoker.getHeight());
            afirme(folgaVerticalPokerCadeira >= player.getHeight(),
                    "Deve haver folga vertical suficiente entre a mesa de Poker e as cadeiras.");

            player.setX(160);
            player.setY(220);
            afirme(!temColisao(player, 160, 220, painel.getObstaculos()),
                    "Posição perto do Blackjack deve ser livre de colisões.");

            player.setX(250);
            player.setY(110);
            afirme(!temColisao(player, 250, 110, painel.getObstaculos()),
                    "Corredor central em direção ao Poker deve estar desimpedido.");

            player.setX(340);
            player.setY(110);
            afirme(!temColisao(player, 340, 110, painel.getObstaculos()),
                    "Ponto de interação em frente à mesa de Poker deve ser livre de colisão.");
            afirme(mesaPoker.isPlayerNear(player, 35),
                    "Avatar deve estar ao alcance da mesa de Poker em (340, 110).");

            player.setX(250);
            player.setY(250);
            afirme(!temColisao(player, 250, 250, painel.getObstaculos()),
                    "Passagem entre Poker e Truco deve estar desimpedida.");

            player.setX(320);
            player.setY(240);
            afirme(!temColisao(player, 320, 240, painel.getObstaculos()),
                    "Ponto de interação da mesa de Truco deve ser livre de colisão.");
            afirme(mesaTruco.isPlayerNear(player, 35),
                    "Avatar deve estar ao alcance da mesa de Truco em (320, 240).");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Rotas entre Blackjack, Truco e Poker estão 100% livres.");
    }

    private static boolean temColisao(MyCharacter player, int x, int y, List<model.MyShape> obstaculos) {
        model.MyRectangle caixa = new model.MyRectangle(x, y, player.getWidth(), player.getHeight());
        if (obstaculos != null) {
            for (model.MyShape obst : obstaculos) {
                if (obst != player && caixa.intersects(obst)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 3. Testa consistência entre seleção contextual no salão e transição da tecla E.
     */
    public static void testarDesempateEuclidianoEAvisoMesmoDestino() {
        System.out.println("[3/10] Testando consistência entre aviso do salão e tecla E...");
        ScreenSaver painel = new ScreenSaver();
        try {
            MyCharacter player = painel.getPlayer();
            CasinoTable mesaPoker = painel.getMesaPoker();

            player.setX(340);
            player.setY(110);
            painel.atualizarMesaMaisProxima();

            afirme(painel.getMesaAtiva() == mesaPoker, "Mesa ativa deve ser a de Poker.");

            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_POKER,
                    "Tecla E deve abrir MENU_POKER.");

            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO,
                    "Tecla B deve retornar ao SALAO.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Aviso e transição da tecla E são consistentes.");
    }

    /**
     * 4. Testa entrada (E), saída (B) e clique no botão "Voltar ao Salão".
     */
    public static void testarTransicaoEntradaESaidaMesaPoker() {
        System.out.println("[4/10] Testando entrada (E), saída (B) e clique no botão Voltar...");
        ScreenSaver painel = new ScreenSaver();
        try {
            MyCharacter player = painel.getPlayer();
            player.setX(340);
            player.setY(110);
            painel.atualizarMesaMaisProxima();

            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_POKER, "Deve entrar no MENU_POKER.");

            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Tecla B deve retornar ao SALAO.");

            painel.processarInteracaoE();
            PokerTableView view = painel.getPokerTableView();
            BufferedImage img = new BufferedImage(1024, 768, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            view.render(g, 1024, 768);
            g.dispose();

            Rectangle btnVoltar = view.getBtnVoltar();
            painel.processarCliqueMouse(btnVoltar.x + 5, btnVoltar.y + 5);
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO,
                    "Clique no botão Voltar ao Salão deve retornar ao SALAO.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Transições por teclado e mouse validadas.");
    }

    /**
     * 5. Testa preservação da mesma instância de PokerEngine e estado da mão ao sair com B e voltar com E.
     */
    public static void testarPreservacaoMaoAoAlternarTelas() {
        System.out.println("[5/10] Testando preservação da mão real ao alternar telas (E -> B -> E)...");
        ScreenSaver painel = new ScreenSaver();
        try {
            TexasHoldemGame motor = criarMotorDeterministicoVitoriaJogador();
            painel.setPokerEngine(motor);

            MyCharacter player = painel.getPlayer();
            player.setX(340);
            player.setY(110);
            painel.atualizarMesaMaisProxima();

            painel.processarInteracaoE();
            PokerTableView view = painel.getPokerTableView();

            // Paga os 50 do Small Blind -> Bot dá check -> avança ao FLOP
            afirme(view.executarCheckCall(), "Call no Pré-Flop deve ser aceito pelo motor.");
            afirme(view.getEstagioAtual() == PokerStage.FLOP, "Fase deve avançar para FLOP.");
            afirme(view.getBoardComunitario().size() == 3, "FLOP deve abrir 3 cartas comunitárias.");

            long poteAntes = view.getPoteCentral();
            List<Card> holeCardsAntes = view.getHoleCardsJogador();
            List<Card> boardAntes = view.getBoardComunitario();

            // Sai com B e reentra com E
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Deve estar no SALAO.");

            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_POKER, "Deve reabrir MENU_POKER.");

            afirme(painel.getPokerEngine() == motor, "Mesma instância de PokerEngine deve ser preservada.");
            afirme(painel.getPokerTableView().getEstagioAtual() == PokerStage.FLOP,
                    "Estágio FLOP deve continuar ativo sem reset.");
            afirme(painel.getPokerTableView().getPoteCentral() == poteAntes,
                    "Pote central deve ser preservado.");
            afirme(painel.getPokerTableView().getHoleCardsJogador().equals(holeCardsAntes),
                    "Hole cards do jogador devem permanecer intactas.");
            afirme(painel.getPokerTableView().getBoardComunitario().equals(boardAntes),
                    "Cartas comunitárias do Flop devem permanecer intactas.");
            afirme(painel.getPokerTableView().getCartasVisiveisBot().isEmpty(),
                    "Cartas do Bot devem continuar ocultas após retorno.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Mão real preservada integralmente entre saídas e reentradas.");
    }

    /**
     * 6. Testa os 5 slots reservados do board e a abertura progressiva (0 -> 3 -> 4 -> 5) via motor real.
     */
    public static void testarCincoEspacosBoardECartasAbertasPorFase() {
        System.out.println("[6/10] Testando 5 slots do board e abertura real por fase (0, 3, 4, 5)...");
        TexasHoldemGame motor = criarMotorDeterministicoVitoriaJogador();
        PokerTableView view = new PokerTableView(null, motor);

        afirme(view.getEstagioAtual() == PokerStage.PRE_FLOP && view.getBoardComunitario().isEmpty(),
                "Pré-Flop deve exibir 0 cartas comunitárias.");

        view.executarCheckCall();
        afirme(view.getEstagioAtual() == PokerStage.FLOP && view.getBoardComunitario().size() == 3,
                "Flop deve exibir 3 cartas comunitárias.");

        view.executarCheckCall();
        afirme(view.getEstagioAtual() == PokerStage.TURN && view.getBoardComunitario().size() == 4,
                "Turn deve exibir 4 cartas comunitárias.");

        view.executarCheckCall();
        afirme(view.getEstagioAtual() == PokerStage.RIVER && view.getBoardComunitario().size() == 5,
                "River deve exibir 5 cartas comunitárias.");

        BufferedImage img = new BufferedImage(900, 650, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        view.render(g, 900, 650);
        g.dispose();

        afirme(view.getBoundsBoard().size() == 5,
                "Devem existir exatamente 5 espaços reservados no board comunitário.");
        System.out.println("  -> Sucesso: Board comunitário sincronizado com o motor em todas as fases.");
    }

    /**
     * 7. Comprova que as cartas do Bot não vazam em nenhuma consulta da View/Engine fora do Showdown,
     *    incluindo Fold no Pré-Flop, e que são reveladas exclusivamente no Showdown.
     */
    public static void testarOcultamentoCartasBotEmFoldEShowdown() {
        System.out.println("[7/10] Testando ocultamento rigoroso do Bot em Fold e revelação apenas no Showdown...");

        // Caminho A: Fold no Pré-Flop mantém cartas do Bot estritamente fechadas
        TexasHoldemGame motorFold = criarMotorDeterministicoVitoriaJogador();
        PokerTableView viewFold = new PokerTableView(null, motorFold);

        afirme(viewFold.getCartasVisiveisBot().isEmpty() && !viewFold.isBotCartasReveladas(),
                "No Pré-Flop as cartas do Bot estão ocultas.");
        afirme(motorFold.getMelhorMaoBotShowdown() == null,
                "Avaliação de Showdown do Bot é nula no Pré-Flop.");

        afirme(viewFold.executarFold(), "Fold no Pré-Flop deve ser aceito.");
        afirme(viewFold.isMaoFinalizada() && viewFold.getEstagioAtual() == PokerStage.ENCERRADA_POR_DESISTENCIA,
                "Mão encerra em ENCERRADA_POR_DESISTENCIA após Fold.");
        afirme(!viewFold.isBotCartasReveladas(),
                "Cartas do Bot DEVEM continuar fechadas após Fold no Pré-Flop.");
        afirme(viewFold.getCartasVisiveisBot().isEmpty(),
                "getCartasVisiveisBot() na View retorna lista vazia após Fold (sem vazamento).");
        afirme(motorFold.hasCartasOcultasBot() && motorFold.getMelhorMaoBotShowdown() == null,
                "Motor confirma ocultação e não expõe mão do Bot após Fold.");

        // Nova Mão via atalho N após Fold alterna botão Dealer para o Bot
        viewFold.processarTecla("N");
        afirme(!viewFold.isMaoFinalizada() && viewFold.getEstagioAtual() == PokerStage.PRE_FLOP,
                "Atalho N inicia nova mão no Pré-Flop após Fold.");
        afirme(motorFold.getBotaoDealer() == PokerParticipant.BOT,
                "Botão Dealer alternou para o Bot na nova mão.");
        afirme(!viewFold.isBotCartasReveladas() && viewFold.getCartasVisiveisBot().isEmpty(),
                "Cartas do Bot seguem ocultas na nova mão.");

        // Caminho B: Showdown revela as 2 cartas do Bot somente ao encerrar o River
        TexasHoldemGame motorShowdown = criarMotorDeterministicoVitoriaJogador();
        PokerTableView viewShowdown = new PokerTableView(null, motorShowdown);

        viewShowdown.executarCheckCall(); // Flop
        afirme(!viewShowdown.isBotCartasReveladas() && viewShowdown.getCartasVisiveisBot().isEmpty(),
                "Bot permanece oculto no Flop.");
        viewShowdown.executarCheckCall(); // Turn
        afirme(!viewShowdown.isBotCartasReveladas() && viewShowdown.getCartasVisiveisBot().isEmpty(),
                "Bot permanece oculto no Turn.");
        viewShowdown.executarCheckCall(); // River
        afirme(!viewShowdown.isBotCartasReveladas() && viewShowdown.getCartasVisiveisBot().isEmpty(),
                "Bot permanece oculto no River.");
        viewShowdown.executarCheckCall(); // Showdown

        afirme(viewShowdown.getEstagioAtual() == PokerStage.SHOWDOWN, "Mão atingiu o SHOWDOWN.");
        afirme(viewShowdown.isBotCartasReveladas() && viewShowdown.getCartasVisiveisBot().size() == 2,
                "No SHOWDOWN o motor autoriza e a View expõe exatamente 2 cartas do Bot.");
        afirme(motorShowdown.getMelhorMaoJogadorShowdown() != null && motorShowdown.getMelhorMaoBotShowdown() != null,
                "Ambas as mãos avaliadas ficam disponíveis exclusivamente no SHOWDOWN.");

        System.out.println("  -> Sucesso: Zero vazamento de cartas do Bot em Fold e todas as ruas pré-Showdown.");
    }

    /**
     * 8. Testa ações válidas, atalhos (F, C, ESPACO, R, 2, 3, N), vitória, derrota, empate e ações inválidas.
     */
    public static void testarAcoesValidasShowdownVitoriaDerrotaEmpateEAtalhos() {
        System.out.println("[8/10] Testando ações válidas, atalhos, vitória, derrota, empate e ações inválidas...");

        // 8.1 Vitória no Showdown com Raise via atalho R e conservação de fichas
        TexasHoldemGame motorVit = criarMotorDeterministicoVitoriaJogador();
        PokerTableView viewVit = new PokerTableView(null, motorVit);
        long totalFichas = motorVit.getSaldoJogador() + motorVit.getSaldoBot() + motorVit.getPoteTotal();

        afirme(motorVit.getAcoesValidasJogador().contains(PokerAction.PAGAR)
                        && !motorVit.getAcoesValidasJogador().contains(PokerAction.PASSAR),
                "No Pré-Flop com 50 a pagar, PAGAR é válido e PASSAR é proibido.");
        afirme(!motorVit.passar(), "Chamada direta a passar() com aposta pendente retorna false.");
        afirme(!viewVit.executarRaiseValor(30L), "Aumento abaixo do Big Blind (30 < 100) é rejeitado.");

        viewVit.processarTecla("R"); // Raise +100 no Pré-Flop -> Bot paga -> Flop (pote = 400)
        afirme(viewVit.getEstagioAtual() == PokerStage.FLOP && viewVit.getPoteCentral() == 400L,
                "Atalho R aumentou +100, Bot pagou e abriu o Flop com pote de 400.");
        afirme(motorVit.getMelhorMaoAtualJogador() != null,
                "A partir do Flop o motor fornece a classificação atual da mão do jogador.");

        viewVit.processarTecla("ESPACO"); // Turn
        viewVit.processarTecla("C");      // River
        viewVit.processarTecla("C");      // Showdown
        afirme(motorVit.getResultadoMao() == PokerRoundResult.JOGADOR_VENCEU_SHOWDOWN,
                "Jogador venceu no Showdown com Dois Pares (Ases e Reis).");
        afirme(motorVit.getSaldoJogador() + motorVit.getSaldoBot() == totalFichas,
                "Conservação exata de fichas após vitória no Showdown (1200 + 800 = 2000).");
        afirme(!viewVit.executarFold() && !viewVit.executarRaise(),
                "Ações de aposta são bloqueadas após o encerramento da mão.");

        // 8.2 Derrota no Showdown (Bot tem Trinca de Ases, Jogador tem Par de Reis)
        List<Card> cartasDerrota = List.of(
                c(Rank.REI, Suit.ESPADAS), c(Rank.AS, Suit.COPAS),
                c(Rank.DAMA, Suit.ESPADAS), c(Rank.AS, Suit.OUROS),
                c(Rank.AS, Suit.PAUS), c(Rank.REI, Suit.COPAS), c(Rank.DOIS, Suit.OUROS),
                c(Rank.SETE, Suit.PAUS), c(Rank.NOVE, Suit.ESPADAS)
        );
        TexasHoldemGame motorDer = new TexasHoldemGame(
                1000L, 1000L, 50L, 100L, new Deck(cartasDerrota), botPassivo(), PokerParticipant.JOGADOR);
        motorDer.iniciarNovaMao();
        PokerTableView viewDer = new PokerTableView(null, motorDer);
        viewDer.processarTecla("C"); // Flop
        viewDer.processarTecla("C"); // Turn
        viewDer.processarTecla("C"); // River
        viewDer.processarTecla("C"); // Showdown
        afirme(motorDer.getResultadoMao() == PokerRoundResult.BOT_VENCEU_SHOWDOWN,
                "Bot venceu no Showdown com Trinca de Ases.");
        afirme(motorDer.getSaldoJogador() == 900L && motorDer.getSaldoBot() == 1100L,
                "Liquidação correta na derrota (Jogador 900, Bot 1100).");

        // 8.3 Empate no Showdown (Split Pot: Royal Flush comunitário)
        List<Card> cartasEmpate = List.of(
                c(Rank.DOIS, Suit.COPAS), c(Rank.TRES, Suit.PAUS),
                c(Rank.QUATRO, Suit.COPAS), c(Rank.CINCO, Suit.PAUS),
                c(Rank.DEZ, Suit.ESPADAS), c(Rank.VALETE, Suit.ESPADAS), c(Rank.DAMA, Suit.ESPADAS),
                c(Rank.REI, Suit.ESPADAS), c(Rank.AS, Suit.ESPADAS)
        );
        TexasHoldemGame motorEmp = new TexasHoldemGame(
                1000L, 1000L, 50L, 100L, new Deck(cartasEmpate), botPassivo(), PokerParticipant.JOGADOR);
        motorEmp.iniciarNovaMao();
        PokerTableView viewEmp = new PokerTableView(null, motorEmp);
        viewEmp.processarTecla("FICHA_3"); // All-In via atalho 3 -> Bot paga -> runout automático até Showdown!
        afirme(motorEmp.getEstagioAtual() == PokerStage.SHOWDOWN,
                "Atalho 3 (All-In) com call do Bot corre automaticamente até o Showdown.");
        afirme(motorEmp.getResultadoMao() == PokerRoundResult.EMPATE_DIVISAO_POTE,
                "Royal Flush na mesa gera EMPATE_DIVISAO_POTE.");
        afirme(motorEmp.getSaldoJogador() == 1000L && motorEmp.getSaldoBot() == 1000L,
                "Split Pot devolve exatamente 1000 fichas para cada participante.");

        System.out.println("  -> Sucesso: Vitória, derrota, empate (All-In), atalhos e ações inválidas validados.");
    }

    /**
     * 9. Testa navegação completa entre Salão, Blackjack, Truco e Poker sem regressões.
     */
    public static void testarNavegacaoEntreTodasAsMesas() {
        System.out.println("[9/10] Testando navegação completa entre as três mesas do cassino...");
        ScreenSaver painel = new ScreenSaver();
        try {
            MyCharacter player = painel.getPlayer();

            // 1. Blackjack
            player.setX(160);
            player.setY(220);
            painel.atualizarMesaMaisProxima();
            afirme(painel.getMesaAtiva() == painel.getMesaBlackjack(), "Mesa ativa deve ser Blackjack.");
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_BLACKJACK, "Deve abrir MENU_BLACKJACK.");
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Deve voltar ao SALAO.");

            // 2. Truco
            player.setX(320);
            player.setY(240);
            painel.atualizarMesaMaisProxima();
            afirme(painel.getMesaAtiva() == painel.getMesaTruco(), "Mesa ativa deve ser Truco.");
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_TRUCO, "Deve abrir MENU_TRUCO.");
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Deve voltar ao SALAO.");

            // 3. Poker
            player.setX(340);
            player.setY(110);
            painel.atualizarMesaMaisProxima();
            afirme(painel.getMesaAtiva() == painel.getMesaPoker(), "Mesa ativa deve ser Poker.");
            painel.processarInteracaoE();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_POKER, "Deve abrir MENU_POKER.");
            painel.processarRetornoB();
            afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Deve voltar ao SALAO.");
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Navegação integrada entre as três mesas e o salão validada.");
    }

    /**
     * 10. Testa renderização gráfica off-screen em múltiplas resoluções e estágios reais do motor.
     */
    public static void testarRenderizacaoOffScreen() {
        System.out.println("[10/10] Testando renderização gráfica off-screen em múltiplas resoluções...");
        ScreenSaver painel = new ScreenSaver();
        try {
            int[][] resolucoes = {
                    {800, 600},
                    {1024, 768},
                    {1280, 720},
                    {1920, 1080}
            };

            for (int[] res : resolucoes) {
                TexasHoldemGame motor = criarMotorDeterministicoVitoriaJogador();
                painel.setPokerEngine(motor);
                PokerTableView view = painel.getPokerTableView();

                BufferedImage imagem = new BufferedImage(res[0], res[1], BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = imagem.createGraphics();

                // Pré-Flop
                view.render(g, res[0], res[1]);
                // Flop
                view.executarCheckCall();
                view.render(g, res[0], res[1]);
                // Turn
                view.executarCheckCall();
                view.render(g, res[0], res[1]);
                // River
                view.executarCheckCall();
                view.render(g, res[0], res[1]);
                // Showdown
                view.executarCheckCall();
                view.render(g, res[0], res[1]);

                g.dispose();
            }
        } finally {
            painel.pararLoop();
        }
        System.out.println("  -> Sucesso: Renderização off-screen concluída sem exceções.");
    }
}
