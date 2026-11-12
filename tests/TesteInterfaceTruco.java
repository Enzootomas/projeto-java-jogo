package tests;

import model.Card;
import model.Rank;
import model.Suit;
import model.TrucoDeck;
import model.TrucoHandResult;
import model.TrucoPlayer;
import model.TrucoRound;
import model.TrucoTrickResult;
import view.ScreenSaver;
import view.TrucoTableView;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Suíte de testes automatizados para a interface gráfica da Mesa de Truco (TrucoTableView)
 * e sua integração com o ScreenSaver e com o motor de regras TrucoRound.
 */
public class TesteInterfaceTruco {

    private static int totalVerificacoes = 0;
    private static int totalFalhas = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   TESTES DE INTERFACE E INTEGRACAO DA MESA DE TRUCO");
        System.out.println("====================================================");

        testarInicializacaoEContratoVisual();
        testarOcultamentoDeCartasDoBot();
        testarSelecaoEJogadaPorCliqueEAtalhos();
        testarMaoIniciadaPeloBot();
        testarPreservacaoDeMaoAoAlternarEntreSalaoEMesa();
        testarDesfechoEMecanicaDeNovaMao();
        testarRenderizacaoVisualOffscreen();

        System.out.println("\n----------------------------------------------------");
        System.out.println("Total de verificacoes da Interface do Truco: " + totalVerificacoes);
        System.out.println("Falhas: " + totalFalhas);
        if (totalFalhas == 0) {
            System.out.println("RESULTADO: TODOS OS TESTES DA INTERFACE DO TRUCO PASSARAM! [OK]");
        } else {
            System.err.println("RESULTADO: HOUVE " + totalFalhas + " FALHAS NOS TESTES!");
            System.exit(1);
        }
        System.out.println("----------------------------------------------------");
    }

    private static void afirme(boolean condicao, String mensagem) {
        totalVerificacoes++;
        if (condicao) {
            System.out.println("  [OK] " + mensagem);
        } else {
            totalFalhas++;
            System.err.println("  [FALHA] " + mensagem);
        }
    }

    private static void testarInicializacaoEContratoVisual() {
        System.out.println("\n1. Testando Inicializacao do ScreenSaver com TrucoTableView e TrucoRound:");

        ScreenSaver painel = new ScreenSaver();
        TrucoRound round = painel.getTrucoRound();
        TrucoTableView view = painel.getTrucoTableView();

        afirme(round != null, "Motor TrucoRound foi inicializado no ScreenSaver");
        afirme(view != null, "TrucoTableView foi inicializada no ScreenSaver");
        afirme(round.getMaoJogador().size() == 3, "Jogador inicia a mao com exatamente 3 cartas");
        afirme(round.getQuantidadeCartasBot() == 3, "Bot possui exatamente 3 cartas restantes");
        afirme(round.getVira() != null, "Carta Vira definida e consultavel");
        afirme(round.getManilhaRank() != null, "Rank da manilha definido a partir da vira");
        afirme(round.getVazas().size() == 1, "Primeira vaza criada automaticamente");
        afirme(!round.isFinalizada(), "Mao recem-criada nao esta finalizada");
    }

    private static void testarOcultamentoDeCartasDoBot() {
        System.out.println("\n2. Testando Ocultamento Rigoroso das Cartas da Mao do Bot:");

        ScreenSaver painel = new ScreenSaver();
        TrucoRound round = painel.getTrucoRound();
        TrucoTableView view = painel.getTrucoTableView();

        // Antes de jogar, a vaza atual nao tem carta do bot (se for a vez do jogador)
        if (round.isTurnoJogador()) {
            afirme(round.getVazaAtual().getCartaBot() == null, "Carta do bot na mesa e nula antes dele jogar");
        }
        // A interface consulta estritamente round.getQuantidadeCartasBot() para desenhar versos
        afirme(round.getQuantidadeCartasBot() == 3, "Quantidade de cartas do bot e 3");

        // Simula renderizacao: nenhuma carta secreta do bot e exposta
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        view.render(g, 800, 600);
        g.dispose();

        afirme(round.getQuantidadeCartasBot() == 3, "Renderizacao nao alterou nem vazou a mao do bot");
    }

    private static void testarSelecaoEJogadaPorCliqueEAtalhos() {
        System.out.println("\n3. Testando Selecao e Jogadas por Clique e Atalhos Apenas no Turno do Jogador:");

        Card vira = new Card(Suit.OUROS, Rank.VALETE); // Manilha Rei (K)
        List<Card> maoJ = Arrays.asList(
                new Card(Suit.PAUS, Rank.TRES),
                new Card(Suit.ESPADAS, Rank.QUATRO),
                new Card(Suit.COPAS, Rank.TRES)
        );
        List<Card> maoB = Arrays.asList(
                new Card(Suit.ESPADAS, Rank.CINCO),
                new Card(Suit.COPAS, Rank.DOIS),
                new Card(Suit.OUROS, Rank.DOIS)
        );

        ScreenSaver painel = new ScreenSaver();
        TrucoRound round = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);
        painel.setTrucoRound(round);
        painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);
        TrucoTableView view = painel.getTrucoTableView();

        // Renderiza para calcular os bounds das cartas da mão
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        view.render(g, 800, 600);
        g.dispose();

        List<Rectangle> bounds = view.getBoundsCartasJogador();
        afirme(bounds.size() == 3, "Bounds de 3 cartas calculados na interface");

        // 3.1: Jogada via atalho '1' (joga a primeira carta da mao: 3 de Paus)
        painel.processarComandoTruco("1");
        afirme(round.getMaoJogador().size() == 2, "Apos atalho 1, jogador ficou com 2 cartas");
        afirme(round.getVazas().get(0).getCartaJogador().equals(new Card(Suit.PAUS, Rank.TRES)),
                "Carta 3 de Paus foi jogada na Vaza 1");
        afirme(round.getVazas().get(0).getCartaBot() != null, "Bot respondeu automaticamente no seu turno");
        afirme(round.getVazas().get(0).isCompleta(), "Vaza 1 foi completada");
        afirme(round.getVazas().get(0).getResultado() == TrucoTrickResult.VITORIA_JOGADOR,
                "Jogador venceu a Vaza 1 com 3 de Paus");

        // 3.2: Como o jogador venceu a Vaza 1, ele joga primeiro na Vaza 2
        afirme(round.getVezAtual() == TrucoPlayer.JOGADOR, "Jogador joga primeiro na Vaza 2");

        // Renderiza novamente para atualizar os bounds (agora restam 2 cartas)
        g = img.createGraphics();
        view.render(g, 800, 600);
        g.dispose();

        bounds = view.getBoundsCartasJogador();
        afirme(bounds.size() == 2, "Bounds atualizados para 2 cartas restantes");

        // 3.3: Jogada via clique do mouse na carta 4 de Espadas (indice 0 da mao restante)
        Rectangle rCarta = bounds.get(0);
        int clickX = rCarta.x + (rCarta.width / 2);
        int clickY = rCarta.y + (rCarta.height / 2);

        painel.processarCliqueMouse(clickX, clickY);

        afirme(round.getMaoJogador().size() == 1, "Apos clique, jogador ficou com 1 carta");
        afirme(round.getVazas().get(1).isCompleta(), "Vaza 2 foi completada apos jogada do jogador e resposta do bot");

        // 3.4: Como o bot jogou 2 e superou o 4, o bot vence a Vaza 2 e abre a Vaza 3
        afirme(round.getVazas().get(1).getResultado() == TrucoTrickResult.VITORIA_BOT, "Bot venceu a Vaza 2");
        afirme(round.getVazas().size() == 3, "Vaza 3 foi aberta para desempate (1x1)");
        afirme(round.getVazaAtual().getCartaBot() != null, "Bot abriu a Vaza 3 jogando sua carta primeiro");
        afirme(round.isTurnoJogador(), "Vez de o jogador responder na Vaza 3");

        // 3.5: Jogada via atalho FICHA_1 (compatível com tecla 1 do painel)
        painel.processarComandoTruco("FICHA_1");
        afirme(round.getMaoJogador().isEmpty(), "Jogador jogou sua ultima carta");
        afirme(round.isFinalizada(), "Mao foi finalizada apos conclusao da Vaza 3");
        afirme(round.getResultadoMao() == TrucoHandResult.VITORIA_JOGADOR,
                "Jogador venceu a mao por 2x1!");

        // 3.6: Tentativa de clicar na mao apos finalizada deve ser bloqueada
        painel.processarComandoTruco("1");
        painel.processarCliqueMouse(clickX, clickY);
        afirme(round.isFinalizada(), "Mao permanece finalizada sem repeticao de jogadas");
    }

    private static void testarMaoIniciadaPeloBot() {
        System.out.println("\n4. Testando Mao Iniciada pelo Bot (Turno Inicial do Bot):");

        Card vira = new Card(Suit.OUROS, Rank.VALETE); // Manilha Rei (K)
        List<Card> maoJ = Arrays.asList(
                new Card(Suit.PAUS, Rank.TRES),
                new Card(Suit.COPAS, Rank.TRES),
                new Card(Suit.OUROS, Rank.TRES)
        );
        List<Card> maoB = Arrays.asList(
                new Card(Suit.ESPADAS, Rank.QUATRO),
                new Card(Suit.COPAS, Rank.QUATRO),
                new Card(Suit.OUROS, Rank.QUATRO)
        );

        ScreenSaver painel = new ScreenSaver();
        // Inicia mao com o Bot sendo o primeiro a jogar
        TrucoRound round = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.BOT);
        painel.setTrucoRound(round);
        painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);

        afirme(round.getVazaAtual().getPrimeiroAJogar() == TrucoPlayer.BOT, "Primeiro a jogar na vaza 1 e o Bot");
        afirme(round.getVazaAtual().getCartaBot() != null, "Bot realizou a jogada de abertura da vaza 1");
        afirme(round.getVazaAtual().getCartaJogador() == null, "Jogador ainda nao jogou");
        afirme(round.isTurnoJogador(), "Apos a abertura do Bot, e a vez do jogador responder");
        afirme(round.getQuantidadeCartasBot() == 2, "Bot ficou com 2 cartas restantes na mao");

        // Jogador responde usando tecla '1'
        painel.processarComandoTruco("1");

        afirme(round.getVazas().get(0).isCompleta(), "Vaza 1 completada apos resposta do jogador");
        afirme(round.getVazas().get(0).getResultado() == TrucoTrickResult.VITORIA_JOGADOR,
                "Jogador superou a carta do Bot na Vaza 1");
    }

    private static void testarPreservacaoDeMaoAoAlternarEntreSalaoEMesa() {
        System.out.println("\n5. Testando Preservacao da Mao em Andamento ao Alternar Entre Salao e Mesa (E / B):");

        ScreenSaver painel = new ScreenSaver();
        painel.setEstadoAtual(ScreenSaver.GameState.SALAO);

        // Posiciona jogador proximo da mesa de Truco
        painel.getPlayer().setExactPosition(320, 260);
        painel.atualizarMesaMaisProxima();
        afirme(painel.getMesaAtiva() != null && "Truco".equalsIgnoreCase(painel.getMesaAtiva().getGameName()),
                "Mesa de Truco foi detectada como mesa ativa por proximidade");

        // Entra na mesa de Truco com tecla E
        painel.processarInteracaoE();
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_TRUCO, "Tecla E abriu a Mesa de Truco");

        TrucoRound roundOriginal = painel.getTrucoRound();
        afirme(roundOriginal != null, "Instancia do TrucoRound esta ativa");
        int cartasIniciaisJogador = roundOriginal.getMaoJogador().size();

        // Joga uma carta na mesa
        painel.processarComandoTruco("1");
        int cartasAposJogada = roundOriginal.getMaoJogador().size();
        afirme(cartasAposJogada == cartasIniciaisJogador - 1, "Jogador jogou uma carta");

        // Retorna ao salão com tecla B
        painel.processarRetornoB();
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Tecla B retornou ao Salao");

        // Move o avatar pelo salão
        painel.getPlayer().setExactPosition(325, 265);
        painel.atualizarMesaMaisProxima();

        // Reabre a mesa de Truco com tecla E
        painel.processarInteracaoE();
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_TRUCO, "Tecla E reabriu a Mesa de Truco");

        TrucoRound roundRetomado = painel.getTrucoRound();
        afirme(roundRetomado == roundOriginal, "A MESMA instancia de TrucoRound foi preservada (sem reset)");
        afirme(roundRetomado.getMaoJogador().size() == cartasAposJogada,
                "Cartas da mao do jogador foram mantidas intactas ao retornar");
        afirme(roundRetomado.getVazas().get(0).getCartaJogador() != null,
                "Cartas jogadas na vaza anterior foram preservadas intactas");
    }

    private static void testarDesfechoEMecanicaDeNovaMao() {
        System.out.println("\n6. Testando Desfecho de Fim de Mao e Botao/Atalho 'Nova Mao':");

        Card vira = new Card(Suit.OUROS, Rank.VALETE);
        List<Card> maoJ = Arrays.asList(
                new Card(Suit.PAUS, Rank.TRES),
                new Card(Suit.COPAS, Rank.TRES),
                new Card(Suit.OUROS, Rank.TRES)
        );
        List<Card> maoB = Arrays.asList(
                new Card(Suit.ESPADAS, Rank.QUATRO),
                new Card(Suit.COPAS, Rank.QUATRO),
                new Card(Suit.OUROS, Rank.QUATRO)
        );

        ScreenSaver painel = new ScreenSaver();
        TrucoRound round1 = new TrucoRound(maoJ, maoB, vira, TrucoPlayer.JOGADOR);
        painel.setTrucoRound(round1);
        painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);
        TrucoTableView view = painel.getTrucoTableView();

        // Joga duas vezes para liquidar em 2x0
        painel.processarComandoTruco("1");
        painel.processarComandoTruco("1");

        afirme(round1.isFinalizada(), "Mao 1 foi finalizada");
        afirme(round1.getResultadoMao() == TrucoHandResult.VITORIA_JOGADOR, "Resultado: Vitoria do Jogador");

        // Renderiza tela finalizada para posicionar o botao Nova Mao
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        view.render(g, 800, 600);
        g.dispose();

        Rectangle btnNovaMao = view.getBtnNovaMao();
        afirme(btnNovaMao.width > 0 && btnNovaMao.height > 0, "Botao 'Nova Mao' desenhado na tela de desfecho");

        // Aciona "Nova Mao" via atalho 'N'
        painel.processarComandoTruco("N");

        TrucoRound round2 = painel.getTrucoRound();
        afirme(round2 != round1, "Uma nova instancia de TrucoRound foi iniciada para a nova mao");
        afirme(!round2.isFinalizada(), "A nova mao esta em andamento");
        afirme(round2.getMaoJogador().size() == 3, "Jogador recebeu 3 novas cartas do baralho");
        afirme(round2.getQuantidadeCartasBot() <= 3, "Bot recebeu novas cartas do baralho");
    }

    private static void testarRenderizacaoVisualOffscreen() {
        System.out.println("\n7. Testando Renderizacao Visual Off-Screen e Botoes de Acao:");

        ScreenSaver painel = new ScreenSaver();
        painel.setEstadoAtual(ScreenSaver.GameState.MENU_TRUCO);
        TrucoTableView view = painel.getTrucoTableView();

        BufferedImage img = new BufferedImage(1024, 768, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Renderiza em resolução 1024x768
        view.render(g, 1024, 768);
        g.dispose();

        afirme(view.getBtnVoltar().width > 0, "Botao Voltar ao Salao renderizado com dimensoes validas");

        // Clica no botao Voltar e valida retorno ao salão
        Rectangle btnVoltar = view.getBtnVoltar();
        painel.processarCliqueMouse(btnVoltar.x + 5, btnVoltar.y + 5);
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO,
                "Clique no botao Voltar retornou ao GameState.SALAO");
    }
}
