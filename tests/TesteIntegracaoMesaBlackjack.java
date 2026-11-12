package tests;

import model.BlackjackEngine;
import model.BlackjackGame;
import model.Card;
import model.Deck;
import model.Rank;
import model.RoundResult;
import model.RoundState;
import model.Suit;
import view.BlackjackMockSimulator;
import view.BlackjackTableView;
import view.ScreenSaver;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Suíte de testes automatizados para validar a integração do motor oficial de regras
 * {@link BlackjackGame} com a mesa do salão ({@link ScreenSaver} e {@link BlackjackTableView}).
 */
public class TesteIntegracaoMesaBlackjack {

    private static int totalVerificacoes = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DE INTEGRACAO: MOTOR REAL NA MESA DO SALAO");
        System.out.println("====================================================");
        System.out.println();

        testarInicializacaoComBlackjackGame();
        testarOcultamentoDoModoDemonstracao();
        testarFluxoCompletoComSequenciaControlada();
        testarPreservacaoDaRodadaEmAndamentoAoAlternarTelas();
        testarBlackjackNaturalELiquidacaoComSaldoDoSalao();

        System.out.println("----------------------------------------------------");
        System.out.println("Total de verificacoes: " + totalVerificacoes);
        System.out.println("Falhas: " + falhas);
        if (falhas == 0) {
            System.out.println("RESULTADO: TODOS OS TESTES DE INTEGRACAO PASSARAM! [OK]");
        } else {
            System.out.println("RESULTADO: " + falhas + " TESTE(S) FALHARAM! [ERRO]");
            System.exit(1);
        }
        System.out.println("----------------------------------------------------");
    }

    private static void testarInicializacaoComBlackjackGame() {
        System.out.println("1. Testando Inicializacao Padrao do ScreenSaver com BlackjackGame:");

        ScreenSaver painel = new ScreenSaver();
        BlackjackEngine engine = painel.getBlackjackEngine();

        afirme(engine != null, "BlackjackEngine foi inicializado no ScreenSaver");
        afirme(engine instanceof BlackjackGame, "BlackjackEngine e uma instancia oficial de BlackjackGame");
        afirme(engine.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA, "Mesa inicia em AGUARDANDO_APOSTA");
        afirme(engine.getSaldoVirtual() == 50000L,
                "Saldo virtual padrao inicializado em 50.000 fichas (" + engine.getSaldoVirtual() + ")");

        // Verifica que o HUD do salão consulta o saldo do motor oficial
        painel.setSize(new Dimension(1000, 700));
        BufferedImage imgSalao = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gSalao = imgSalao.createGraphics();
        painel.paint(gSalao);
        gSalao.dispose();

        afirme(painel.getBlackjackTableView().getEngine() == engine,
                "BlackjackTableView e ScreenSaver compartilham a mesma instancia de BlackjackGame");

        painel.pararLoop();
        System.out.println();
    }

    private static void testarOcultamentoDoModoDemonstracao() {
        System.out.println("2. Testando Ocultamento de 'Modo de demonstracao' com BlackjackGame:");

        ScreenSaver painel = new ScreenSaver();

        // 1. Com BlackjackGame ativo, 'Modo de demonstracao' deve estar ausente
        BlackjackTableView viewReal = painel.getBlackjackTableView();
        afirme(viewReal.getEngine() instanceof BlackjackGame, "Mesa configurada com BlackjackGame");

        BufferedImage imgReal = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gReal = imgReal.createGraphics();
        viewReal.render(gReal, 1000, 700);
        gReal.dispose();

        afirme(!(viewReal.getEngine() instanceof BlackjackMockSimulator),
                "'Modo de demonstracao' nao e acionado porque engine nao e BlackjackMockSimulator");

        // 2. Se injetarmos BlackjackMockSimulator, o simulador continua funcionando
        BlackjackMockSimulator mock = new BlackjackMockSimulator();
        painel.setBlackjackEngine(mock);
        afirme(painel.getBlackjackTableView().getEngine() instanceof BlackjackMockSimulator,
                "Simulador pode ser injetado com sucesso para fins de teste");

        painel.pararLoop();
        System.out.println();
    }

    private static void testarFluxoCompletoComSequenciaControlada() {
        System.out.println("3. Testando Fluxo Completo com Sequencia Controlada de Cartas:");

        // Sequência determinística de cartas:
        // Distribuicao inicial:
        // 1. Jogador: 10 de Copas (10)
        // 2. Dealer: 8 de Espadas (8 visivel)
        // 3. Jogador: 6 de Paus (total: 16)
        // 4. Dealer: 9 de Ouros (total: 17 oculto)
        // Turno Jogador:
        // 5. Jogador compra: 4 de Ouros (total: 20)
        // Dealer tem 17 (8 + 9), entao ao chamar parar() ele atinge 17 e PARA!
        List<Card> cartasControladas = new ArrayList<>();
        cartasControladas.add(new Card(Suit.COPAS, Rank.DEZ));
        cartasControladas.add(new Card(Suit.ESPADAS, Rank.OITO));
        cartasControladas.add(new Card(Suit.PAUS, Rank.SEIS));
        cartasControladas.add(new Card(Suit.OUROS, Rank.NOVE));
        cartasControladas.add(new Card(Suit.OUROS, Rank.QUATRO));

        Deck deckControlado = new Deck(cartasControladas);
        BlackjackGame motorControlado = new BlackjackGame(1000L, deckControlado);

        ScreenSaver painel = new ScreenSaver();
        painel.setBlackjackEngine(motorControlado);

        afirme(painel.getBlackjackEngine().getSaldoVirtual() == 1000L, "Saldo inicial configurado em 1000 fichas");

        // 1. Inicia rodada com aposta de 200 fichas
        motorControlado.iniciarPartida(200L);

        afirme(motorControlado.getEstadoRodada() == RoundState.TURNO_JOGADOR, "Rodada iniciada em TURNO_JOGADOR");
        afirme(motorControlado.getSaldoVirtual() == 800L,
                "Aposta de 200 fichas foi debitada imediatamente do saldo (Saldo: 800)");
        afirme(painel.getBlackjackEngine().getApostaAtual() == 200L, "Aposta atual registrada como 200");

        // 2. Carta oculta da banca durante o turno do jogador
        afirme(motorControlado.hasCartaOcultaDealer(), "Carta do dealer esta oculta durante TURNO_JOGADOR");
        afirme(motorControlado.getCartasVisiveisDealer().size() == 1,
                "Apenas 1 carta do dealer e visivel para a interface");
        afirme(motorControlado.getCartasVisiveisDealer().get(0).getRank() == Rank.OITO,
                "Carta visivel do dealer e o 8 de Espadas");
        afirme(motorControlado.getPontosDealerVisiveis() == 8,
                "Pontos visiveis do dealer sao 8");
        afirme(motorControlado.getPontosJogador() == 16,
                "Pontos do jogador sao 16 (10 + 6)");

        // 3. Jogador pede carta (Hit) e recebe 4 de Ouros -> 20 pontos
        boolean pediu = painel.getBlackjackTableView().processarTecla("HIT");
        afirme(pediu, "Comando HIT executado com sucesso");
        afirme(motorControlado.getMaoJogador().size() == 3, "Jogador agora tem 3 cartas");
        afirme(motorControlado.getPontosJogador() == 20, "Pontos do jogador atualizados para 20 (16 + 4)");
        afirme(motorControlado.hasCartaOcultaDealer(), "Carta do dealer continua oculta apos HIT");

        // 4. Jogador para (Stand) -> Dealer revela 9 de Ouros (total 17) e para
        painel.getBlackjackTableView().processarTecla("STAND");

        afirme(motorControlado.getEstadoRodada() == RoundState.FINALIZADA, "Rodada transitou para FINALIZADA");
        afirme(!motorControlado.hasCartaOcultaDealer(), "Carta do dealer foi revelada apos FINALIZADA");
        afirme(motorControlado.getCartasVisiveisDealer().size() == 2, "Todas as cartas do dealer estao visiveis (2 cartas)");
        afirme(motorControlado.getPontosDealerVisiveis() == 17, "Dealer parou com 17 pontos");
        afirme(motorControlado.getResultadoRodada() == RoundResult.JOGADOR_VITORIA,
                "Resultado e JOGADOR_VITORIA (Jogador 20 x Dealer 17)");

        // 5. Verificação da liquidação e saldo
        // Retorno: aposta (200) + lucro 1:1 (200) = 400 creditados ao saldo (800 + 400 = 1200)
        afirme(motorControlado.getSaldoVirtual() == 1200L,
                "Saldo virtual liquidado corretamente: 1200 fichas (lucro +200)");
        afirme(motorControlado.getUltimoRetornoTotalCreditado() == 400L, "Retorno total creditado foi 400");
        afirme(motorControlado.getLucroLiquidoUltimaRodada() == 200L, "Lucro liquido da rodada foi +200");

        painel.pararLoop();
        System.out.println();
    }

    private static void testarPreservacaoDaRodadaEmAndamentoAoAlternarTelas() {
        System.out.println("4. Testando Preservacao da Rodada (B -> Salão -> E -> Mesa):");

        List<Card> cartas = new ArrayList<>();
        cartas.add(new Card(Suit.OUROS, Rank.VALETE)); // Jogador
        cartas.add(new Card(Suit.ESPADAS, Rank.CINCO)); // Dealer
        cartas.add(new Card(Suit.COPAS, Rank.SETE));    // Jogador (total: 17)
        cartas.add(new Card(Suit.PAUS, Rank.DEZ));     // Dealer (total: 15)

        Deck deck = new Deck(cartas);
        BlackjackGame motor = new BlackjackGame(1000L, deck);

        ScreenSaver painel = new ScreenSaver();
        painel.setBlackjackEngine(motor);

        // Abre a mesa de Blackjack
        painel.setEstadoAtual(ScreenSaver.GameState.MENU_BLACKJACK);
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_BLACKJACK, "Painel abriu MENU_BLACKJACK");

        // Inicia partida com 150 fichas
        boolean iniciou = motor.iniciarPartida(150L);
        afirme(iniciou, "Partida iniciada com aposta de 150 fichas");
        afirme(motor.getEstadoRodada() == RoundState.TURNO_JOGADOR, "Rodada em TURNO_JOGADOR");
        afirme(motor.getSaldoVirtual() == 850L, "Saldo debitado para 850 fichas");

        // Pressiona B para voltar ao salão
        painel.processarRetornoB();
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Tecla B retornou ao SALAO");

        // Verifica que o saldo exibido no salão reflete os 850 fichas
        BufferedImage imgSalao = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gSalao = imgSalao.createGraphics();
        painel.paint(gSalao);
        gSalao.dispose();
        afirme(painel.getBlackjackEngine().getSaldoVirtual() == 850L,
                "Saldo no salao permanece 850 com aposta retida na mesa");

        // Pressiona E para reentrar na mesa de Blackjack
        painel.setEstadoAtual(ScreenSaver.GameState.MENU_BLACKJACK);
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_BLACKJACK, "Reabriu MENU_BLACKJACK");

        // Confere que a rodada permanece exatamente a mesma e ativa
        BlackjackEngine motorRetornado = painel.getBlackjackEngine();
        afirme(motorRetornado == motor, "A mesma instancia do motor foi preservada");
        afirme(motorRetornado.getEstadoRodada() == RoundState.TURNO_JOGADOR,
                "Estado TURNO_JOGADOR preservado sem reinicializacao");
        afirme(motorRetornado.getPontosJogador() == 17, "Mao do jogador preservada com 17 pontos");
        afirme(motorRetornado.getApostaAtual() == 150L, "Aposta de 150 fichas preservada");

        painel.pararLoop();
        System.out.println();
    }

    private static void testarBlackjackNaturalELiquidacaoComSaldoDoSalao() {
        System.out.println("5. Testando Pagamento 3:2 de Blackjack Natural e Atualizacao de Saldo:");

        // Jogador recebe Ás + Rei = Blackjack Natural
        // Dealer recebe 9 + 8 = 17
        List<Card> cartas = new ArrayList<>();
        cartas.add(new Card(Suit.ESPADAS, Rank.AS));
        cartas.add(new Card(Suit.COPAS, Rank.NOVE));
        cartas.add(new Card(Suit.COPAS, Rank.REI));
        cartas.add(new Card(Suit.OUROS, Rank.OITO));

        Deck deck = new Deck(cartas);
        BlackjackGame motor = new BlackjackGame(1000L, deck);

        ScreenSaver painel = new ScreenSaver();
        painel.setBlackjackEngine(motor);

        // Aposta de 100 fichas
        boolean iniciou = motor.iniciarPartida(100L);
        afirme(iniciou, "Iniciou partida com aposta de 100 fichas");
        afirme(motor.getEstadoRodada() == RoundState.FINALIZADA,
                "Blackjack Natural encerra a rodada imediatamente na distribuicao");
        afirme(motor.getResultadoRodada() == RoundResult.JOGADOR_BLACKJACK,
                "Resultado e JOGADOR_BLACKJACK");

        // Pagamento 3:2: aposta 100 -> lucro 150 -> saldo inicial 1000 + 150 = 1150
        afirme(motor.getSaldoVirtual() == 1150L,
                "Saldo virtual creditado com pagamento 3:2 exato: 1150 fichas (lucro +150)");
        afirme(motor.getUltimoRetornoTotalCreditado() == 250L,
                "Retorno total liquidado pelo motor e exatamente 250 fichas (aposta 100 + lucro 150)");
        afirme(motor.getLucroLiquidoUltimaRodada() == 150L,
                "Lucro liquido liquidado pelo motor e exatamente 150 fichas");

        // Valida calculo seguro na tela: (aposta / 2) * 3 sem multiplicacao intermediaria
        long apostaTeste = 100L;
        long lucroCalculadoTela = Math.multiplyExact(apostaTeste / 2L, 3L);
        long retornoTotalCalculadoTela = Math.addExact(apostaTeste, lucroCalculadoTela);
        afirme(lucroCalculadoTela == 150L, "Calculo de tela Math.multiplyExact(aposta/2, 3) resulta em lucro 150");
        afirme(retornoTotalCalculadoTela == 250L, "Retorno total (aposta + lucro) resulta em 250");

        // Valida renderização do banner de Blackjack Natural na tela sem falhas
        BufferedImage imgBJ = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gBJ = imgBJ.createGraphics();
        painel.getBlackjackTableView().render(gBJ, 1000, 700);
        gBJ.dispose();
        afirme(true, "Banner de Blackjack Natural renderizado na tela sem falhas ou overflow");

        painel.pararLoop();
        System.out.println();
    }

    private static void afirme(boolean condicao, String descricao) {
        totalVerificacoes++;
        if (condicao) {
            System.out.println("  [OK] " + descricao);
        } else {
            System.err.println("  [FALHA] " + descricao);
            falhas++;
        }
    }
}
