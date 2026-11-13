package view;

import model.Card;
import model.Rank;
import model.Suit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Interface gráfica completa e interativa da Mesa de Poker (Texas Hold'em 1 vs 1).
 *
 * <p>Responsabilidades visuais e de navegação:</p>
 * <ul>
 *   <li>Tampo oval de feltro bordô/vinho nobre (Burgundy Poker Table) com borda de mogno.</li>
 *   <li>Área do Bot: exibe avatar, fichas e exatamente 2 cartas fechadas (versos),
 *       reveladas apenas no Showdown quando autorizado pelo fluxo do jogo.</li>
 *   <li>Área Central (The Board): reserva cinco posições para as cartas comunitárias,
 *       exibindo apenas as cartas reveladas na fase atual (Flop: 3, Turn: 4, River/Showdown: 5).</li>
 *   <li>Pote central com contagem de fichas virtuais.</li>
 *   <li>Área do Jogador: exibe as 2 cartas privativas (Hole Cards) abertas, classificação da mão e stack.</li>
 *   <li>Controles contextuais: exibe dinamicamente "Passar" (Check) ou "Pagar" (Call),
 *       habilita "Aumentar" (Raise) e "Desistir" (Fold), além de "Voltar ao Salão" (B) e "Nova Mão" (N).</li>
 *   <li>Indicação clara de "Poker (Texas Hold'em) - Em desenvolvimento (Aguardando motor oficial)".</li>
 *   <li>Preservação do estado da mão entre alternâncias de telas (E / B).</li>
 * </ul>
 */
public class PokerTableView {

    public enum PokerPhase {
        PRE_FLOP("Pré-Flop", 0),
        FLOP("Flop", 3),
        TURN("Turn", 4),
        RIVER("River", 5),
        SHOWDOWN("Showdown", 5);

        private final String nome;
        private final int cartasAbertas;

        PokerPhase(String nome, int cartasAbertas) {
            this.nome = nome;
            this.cartasAbertas = cartasAbertas;
        }

        public String getNome() {
            return nome;
        }

        public int getCartasAbertas() {
            return cartasAbertas;
        }
    }

    private final ScreenSaver screenSaver;
    private final NumberFormat formatadorMoeda = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));

    // Fases e Estado da Mão
    private PokerPhase faseAtual = PokerPhase.PRE_FLOP;
    private boolean maoFinalizada = false;
    private String mensagemDesfecho = "";
    private boolean botCartasReveladas = false;

    // Fichas e Pote (long para compatibilidade com o sistema de fichas)
    private long poteCentral = 100L;
    private long apostaJogador = 50L;
    private long apostaBot = 50L;
    private long stackJogador = 950L;
    private long stackBot = 950L;
    private long valorAumento = 50L;

    // Cartas de exemplo para o layout (substituíveis pelo motor do colega)
    private final List<Card> holeCardsJogador = new ArrayList<>();
    private final List<Card> holeCardsBot = new ArrayList<>();
    private final List<Card> boardComunitario = new ArrayList<>();

    // Áreas de interação para cliques do mouse
    private final Rectangle btnFold = new Rectangle();
    private final Rectangle btnCheckCall = new Rectangle();
    private final Rectangle btnRaise = new Rectangle();
    private final Rectangle btnVoltar = new Rectangle();
    private final Rectangle btnNovaMao = new Rectangle();

    // Bounds das 5 cartas comunitárias para verificação
    private final List<Rectangle> boundsBoard = new ArrayList<>();

    public PokerTableView(ScreenSaver screenSaver) {
        this.screenSaver = screenSaver;
        iniciarNovaMaoExemplo();
    }

    /**
     * Inicializa uma mão de exemplo rica para apresentação do layout do Texas Hold'em.
     */
    public void iniciarNovaMaoExemplo() {
        this.faseAtual = PokerPhase.PRE_FLOP;
        this.maoFinalizada = false;
        this.mensagemDesfecho = "";
        this.botCartasReveladas = false;

        this.apostaJogador = 50L;
        this.apostaBot = 50L;
        this.poteCentral = 100L;

        // Atualiza stack com saldo do jogador se disponível
        if (screenSaver != null && screenSaver.getBlackjackEngine() != null) {
            this.stackJogador = screenSaver.getBlackjackEngine().getSaldoVirtual();
        }

        // 2 Hole Cards do Jogador: Ás de Espadas e Rei de Copas
        this.holeCardsJogador.clear();
        this.holeCardsJogador.add(new Card(Suit.ESPADAS, Rank.AS));
        this.holeCardsJogador.add(new Card(Suit.COPAS, Rank.REI));

        // 2 Hole Cards do Bot: Dama de Ouros e Valete de Paus (fechadas até Showdown)
        this.holeCardsBot.clear();
        this.holeCardsBot.add(new Card(Suit.OUROS, Rank.DAMA));
        this.holeCardsBot.add(new Card(Suit.PAUS, Rank.VALETE));

        // 5 Cartas Comunitárias do Board:
        // Flop: 10 de Copas, 2 de Espadas, Ás de Ouros
        // Turn: 7 de Paus
        // River: Rei de Espadas (completando Dois Pares de Ás e Rei para o jogador)
        this.boardComunitario.clear();
        this.boardComunitario.add(new Card(Suit.COPAS, Rank.DEZ));
        this.boardComunitario.add(new Card(Suit.ESPADAS, Rank.DOIS));
        this.boardComunitario.add(new Card(Suit.OUROS, Rank.AS));
        this.boardComunitario.add(new Card(Suit.PAUS, Rank.SETE));
        this.boardComunitario.add(new Card(Suit.ESPADAS, Rank.REI));
    }

    /**
     * Renderiza todos os elementos visuais da mesa de Poker Texas Hold'em.
     */
    public void render(Graphics2D g, int largura, int altura) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Fundo do salão escuro
        g.setColor(new Color(10, 14, 20));
        g.fillRect(0, 0, largura, altura);

        // 2. Tampo da mesa em feltro bordô nobre
        desenharMesaFeltro(g, largura, altura);

        // 3. Cabeçalho e Identificação da Mesa
        desenharCabecalho(g, largura);

        // 4. Área do Bot (Topo)
        desenharAreaBot(g, largura, altura);

        // 5. Área Central: Pote e 5 Cartas Comunitárias
        desenharAreaBoardEPot(g, largura, altura);

        // 6. Área do Jogador (Base)
        desenharAreaJogador(g, largura, altura);

        // 7. Controles e Ações
        desenharPainelAcoes(g, largura, altura);

        // 8. Botão Voltar ao Salão (B)
        desenharBotaoVoltar(g, largura, altura);

        // 9. Banner de Desfecho no Showdown ou Fold
        if (maoFinalizada) {
            desenharBannerDesfecho(g, largura, altura);
        }
    }

    private void desenharMesaFeltro(Graphics2D g, int largura, int altura) {
        int margemX = 55;
        int margemY = 40;
        int mesaW = largura - (margemX * 2);
        int mesaH = altura - (margemY * 2);

        // Borda externa de mogno escuro almofadado
        g.setColor(new Color(42, 18, 18));
        g.fillRoundRect(margemX - 10, margemY - 10, mesaW + 20, mesaH + 20, 48, 48);

        // Filete dourado ornamental
        g.setColor(new Color(212, 175, 55));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(margemX - 5, margemY - 5, mesaW + 10, mesaH + 10, 42, 42);
        g.setStroke(new BasicStroke(1.0f));

        // Feltro bordô com gradiente radial/linear luxuoso
        GradientPaint feltro = new GradientPaint(
                largura / 2.0f, margemY, new Color(110, 18, 28),
                largura / 2.0f, margemY + mesaH, new Color(55, 8, 14)
        );
        g.setPaint(feltro);
        g.fillRoundRect(margemX, margemY, mesaW, mesaH, 36, 36);

        // Linha interna de demarcação do feltro de Poker
        g.setColor(new Color(212, 175, 55, 70));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(margemX + 24, margemY + 24, mesaW - 48, mesaH - 48, 28, 28);
        g.setStroke(new BasicStroke(1.0f));
    }

    private void desenharCabecalho(Graphics2D g, int largura) {
        int cx = largura / 2;

        // Placa do título principal
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(255, 225, 130));
        FontMetrics fm = g.getFontMetrics();
        String titulo = "TEXAS HOLD'EM POKER (1 vs 1)";
        g.drawString(titulo, cx - (fm.stringWidth(titulo) / 2), 65);

        // Indicador de modo e fase ativa
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(255, 190, 70));
        String sub = "Fase: " + faseAtual.getNome().toUpperCase() + "  |  Modo: Demonstração Visual (Aguardando motor)";
        FontMetrics fmSub = g.getFontMetrics();
        g.drawString(sub, cx - (fmSub.stringWidth(sub) / 2), 82);
    }

    private void desenharAreaBot(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int yBot = 100;

        // Informações do Bot
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(230, 230, 230));
        String infoBot = "Crupiê / Bot — Stack: " + formatarMoeda(stackBot) + " | Aposta: " + formatarMoeda(apostaBot);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(infoBot, cx - (fm.stringWidth(infoBot) / 2), yBot);

        // As duas cartas do Bot (fechadas até Showdown)
        int cardW = 54;
        int cardH = 76;
        int gap = 12;
        int startX = cx - cardW - (gap / 2);
        int cardY = yBot + 10;

        for (int i = 0; i < 2; i++) {
            int x = startX + i * (cardW + gap);
            if (botCartasReveladas && i < holeCardsBot.size()) {
                desenharCartaAberta(g, holeCardsBot.get(i), x, cardY, cardW, cardH);
            } else {
                desenharCartaFechada(g, x, cardY, cardW, cardH);
            }
        }
    }

    private void desenharAreaBoardEPot(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int cy = (altura / 2) - 10;

        // Pote Central (The Pot)
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(cx - 100, cy - 65, 200, 32, 12, 12);
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(cx - 100, cy - 65, 200, 32, 12, 12);
        g.setStroke(new BasicStroke(1.0f));

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(255, 235, 120));
        String txtPote = "POTE: " + formatarMoeda(poteCentral) + " Fichas";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txtPote, cx - (fm.stringWidth(txtPote) / 2), cy - 44);

        // Reserva dos 5 espaços das Cartas Comunitárias (The Board)
        int cardW = 60;
        int cardH = 84;
        int gap = 12;
        int totalW = (5 * cardW) + (4 * gap);
        int startX = cx - (totalW / 2);
        int boardY = cy - 20;

        boundsBoard.clear();
        int abertas = faseAtual.getCartasAbertas();

        for (int i = 0; i < 5; i++) {
            int x = startX + i * (cardW + gap);
            Rectangle rect = new Rectangle(x, boardY, cardW, cardH);
            boundsBoard.add(rect);

            String rotuloSlot = (i < 3) ? "FLOP" : (i == 3 ? "TURN" : "RIVER");

            if (i < abertas && i < boardComunitario.size()) {
                desenharCartaAberta(g, boardComunitario.get(i), x, boardY, cardW, cardH);
            } else {
                desenharSlotVazio(g, x, boardY, cardW, cardH, rotuloSlot);
            }
        }
    }

    private void desenharSlotVazio(Graphics2D g, int x, int y, int w, int h, String rotulo) {
        // Fundo semitransparente do slot reservado
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(x, y, w, h, 8, 8);

        // Borda tracejada/dourada
        g.setColor(new Color(212, 175, 55, 130));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setStroke(new BasicStroke(1.0f));

        // Rótulo da posição (FLOP / TURN / RIVER)
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(new Color(220, 190, 110, 170));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(rotulo, x + (w - fm.stringWidth(rotulo)) / 2, y + (h / 2) + 4);
    }

    private void desenharAreaJogador(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int yJogador = altura - 230;

        // As duas Hole Cards abertas do Jogador
        int cardW = 60;
        int cardH = 84;
        int gap = 14;
        int startX = cx - cardW - (gap / 2);

        for (int i = 0; i < holeCardsJogador.size(); i++) {
            int x = startX + i * (cardW + gap);
            desenharCartaAberta(g, holeCardsJogador.get(i), x, yJogador, cardW, cardH);
        }

        // Rótulo de classificação da mão do jogador
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(255, 230, 140));
        String rankingMao = "Sua Mão: " + classificarMaoExemplo();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(rankingMao, cx - (fm.stringWidth(rankingMao) / 2), yJogador + cardH + 18);

        // Saldo e aposta do jogador
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(180, 240, 180));
        String infoJogador = "Stack: " + formatarMoeda(stackJogador) + " Fichas  |  Sua Aposta: " + formatarMoeda(apostaJogador);
        FontMetrics fmInfo = g.getFontMetrics();
        g.drawString(infoJogador, cx - (fmInfo.stringWidth(infoJogador) / 2), yJogador + cardH + 34);
    }

    private String classificarMaoExemplo() {
        switch (faseAtual) {
            case PRE_FLOP:
                return "Ás e Rei Altos (Conectores de mesmo naipe não-formados)";
            case FLOP:
                return "Par de Ás";
            case TURN:
                return "Par de Ás (K kicker)";
            case RIVER:
            case SHOWDOWN:
                return "Dois Pares (Áses e Reis)";
            default:
                return "Carta Alta";
        }
    }

    private void desenharPainelAcoes(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int yBotoes = altura - 100;
        int btnW = 125;
        int btnH = 38;
        int gap = 12;

        boolean podeAgir = !maoFinalizada && faseAtual != PokerPhase.SHOWDOWN;
        boolean devePagar = (apostaBot > apostaJogador);
        String txtCheckCall = devePagar ? "PAGAR (C)" : "PASSAR (C)";

        // 1. Botão FOLD (Desistir - Tecla F)
        int xFold = cx - (btnW * 3 / 2) - gap;
        btnFold.setBounds(xFold, yBotoes, btnW, btnH);
        desenharBotao(g, btnFold, "DESISTIR (F)", new Color(140, 30, 30), podeAgir);

        // 2. Botão CHECK / CALL (Passar ou Pagar - Tecla C ou ESPAÇO)
        int xCheck = cx - (btnW / 2);
        btnCheckCall.setBounds(xCheck, yBotoes, btnW, btnH);
        desenharBotao(g, btnCheckCall, txtCheckCall, new Color(28, 110, 60), podeAgir);

        // 3. Botão RAISE (Aumentar - Tecla R ou 1, 2, 3)
        int xRaise = cx + (btnW / 2) + gap;
        btnRaise.setBounds(xRaise, yBotoes, btnW, btnH);
        desenharBotao(g, btnRaise, "AUMENTAR (R)", new Color(175, 125, 20), podeAgir && (stackJogador >= valorAumento));
    }

    private void desenharBotaoVoltar(Graphics2D g, int largura, int altura) {
        int btnW = 150;
        int btnH = 34;
        btnVoltar.setBounds(40, altura - 60, btnW, btnH);

        g.setColor(new Color(25, 30, 45));
        g.fillRoundRect(btnVoltar.x, btnVoltar.y, btnW, btnH, 10, 10);
        g.setColor(new Color(180, 140, 50));
        g.drawRoundRect(btnVoltar.x, btnVoltar.y, btnW, btnH, 10, 10);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        String txt = "← Voltar ao Salão (B)";
        g.drawString(txt, btnVoltar.x + (btnW - fm.stringWidth(txt)) / 2, btnVoltar.y + 22);
    }

    private void desenharBannerDesfecho(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int bannerW = 480;
        int bannerH = 95;
        int bannerX = cx - (bannerW / 2);
        int bannerY = (altura / 2) - 80;

        g.setColor(new Color(10, 12, 18, 235));
        g.fillRoundRect(bannerX, bannerY, bannerW, bannerH, 16, 16);
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(bannerX, bannerY, bannerW, bannerH, 16, 16);
        g.setStroke(new BasicStroke(1.0f));

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(255, 235, 120));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(mensagemDesfecho, cx - (fm.stringWidth(mensagemDesfecho) / 2), bannerY + 32);

        // Botão Nova Mão (N)
        int btnW = 180;
        int btnH = 36;
        btnNovaMao.setBounds(cx - (btnW / 2), bannerY + 45, btnW, btnH);
        desenharBotao(g, btnNovaMao, "NOVA MÃO (N)", new Color(30, 110, 50), true);
    }

    private void desenharBotao(Graphics2D g, Rectangle rect, String texto, Color corBase, boolean habilitado) {
        if (!habilitado) {
            g.setColor(new Color(60, 60, 65, 180));
            g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
            g.setColor(new Color(90, 90, 95));
            g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
            g.setColor(new Color(140, 140, 145));
        } else {
            g.setColor(corBase);
            g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
            g.setColor(new Color(255, 215, 0));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
            g.setStroke(new BasicStroke(1.0f));
            g.setColor(Color.WHITE);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, rect.x + (rect.width - fm.stringWidth(texto)) / 2, rect.y + 23);
    }

    private void desenharCartaAberta(Graphics2D g, Card carta, int x, int y, int w, int h) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(180, 180, 180));
        g.drawRoundRect(x, y, w, h, 8, 8);

        boolean vermelho = (carta.getSuit() == Suit.COPAS || carta.getSuit() == Suit.OUROS);
        g.setColor(vermelho ? new Color(190, 20, 20) : new Color(20, 20, 20));

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(carta.getRank().getSimbolo(), x + 5, y + 15);

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        String naipeSimbolo = carta.getSuit().getSimbolo();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(naipeSimbolo, x + (w - fm.stringWidth(naipeSimbolo)) / 2, y + (h / 2) + 8);
    }

    private void desenharCartaFechada(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(18, 40, 80));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(212, 175, 55));
        g.drawRoundRect(x, y, w, h, 8, 8);

        // Padrão ornamental do verso
        g.setColor(new Color(30, 60, 120));
        g.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 5, 5);
        g.drawLine(x + 4, y + 4, x + w - 4, y + h - 4);
        g.drawLine(x + w - 4, y + 4, x + 4, y + h - 4);
    }

    /**
     * Processa cliques do mouse na mesa de Poker.
     * @return true se o usuário clicou para voltar ao salão; false caso contrário.
     */
    public boolean processarClique(int x, int y) {
        if (btnVoltar.contains(x, y)) {
            return true;
        }

        if (maoFinalizada) {
            if (btnNovaMao.contains(x, y)) {
                iniciarNovaMaoExemplo();
            }
            return false;
        }

        if (btnFold.contains(x, y)) {
            executarFold();
        } else if (btnCheckCall.contains(x, y)) {
            executarCheckCall();
        } else if (btnRaise.contains(x, y)) {
            executarRaise();
        }

        return false;
    }

    /**
     * Processa comandos de atalho de teclado na mesa de Poker.
     */
    public void processarTecla(String comando) {
        if ("B".equalsIgnoreCase(comando) || "VOLTAR".equalsIgnoreCase(comando)) {
            if (screenSaver != null) {
                screenSaver.processarRetornoB();
            }
            return;
        }

        if (maoFinalizada) {
            if ("N".equalsIgnoreCase(comando) || "NOVA_MAO".equalsIgnoreCase(comando)) {
                iniciarNovaMaoExemplo();
            }
            return;
        }

        if ("FOLD".equalsIgnoreCase(comando) || "F".equalsIgnoreCase(comando)) {
            executarFold();
        } else if ("CHECK".equalsIgnoreCase(comando) || "CALL".equalsIgnoreCase(comando)
                || "C".equalsIgnoreCase(comando) || "ESPACO".equalsIgnoreCase(comando)) {
            executarCheckCall();
        } else if ("RAISE".equalsIgnoreCase(comando) || "R".equalsIgnoreCase(comando)
                || "FICHA_1".equalsIgnoreCase(comando) || "FICHA_2".equalsIgnoreCase(comando) || "FICHA_3".equalsIgnoreCase(comando)) {
            executarRaise();
        }
    }

    /**
     * Executa a ação de Fold (Desistir).
     */
    public void executarFold() {
        if (maoFinalizada) return;
        this.maoFinalizada = true;
        this.mensagemDesfecho = "Você desistiu da mão (Fold). Vitória do Bot!";
        this.botCartasReveladas = false;
    }

    /**
     * Executa a ação de Check ou Call e avança de fase no Texas Hold'em.
     */
    public void executarCheckCall() {
        if (maoFinalizada) return;

        // Iguala apostas se houver diferença
        if (apostaBot > apostaJogador) {
            long diff = apostaBot - apostaJogador;
            apostaJogador += diff;
            stackJogador -= diff;
            poteCentral += diff;
        }

        avancarFase();
    }

    /**
     * Executa a ação de Raise (Aumentar) e avança de fase.
     */
    public void executarRaise() {
        if (maoFinalizada) return;
        if (stackJogador < valorAumento) return;

        // Jogador aumenta a aposta
        stackJogador -= valorAumento;
        apostaJogador += valorAumento;
        poteCentral += valorAumento;

        // Bot emparelha a aposta de exemplo
        if (stackBot >= valorAumento) {
            stackBot -= valorAumento;
            apostaBot += valorAumento;
            poteCentral += valorAumento;
        }

        avancarFase();
    }

    private void avancarFase() {
        switch (faseAtual) {
            case PRE_FLOP:
                faseAtual = PokerPhase.FLOP;
                break;
            case FLOP:
                faseAtual = PokerPhase.TURN;
                break;
            case TURN:
                faseAtual = PokerPhase.RIVER;
                break;
            case RIVER:
                faseAtual = PokerPhase.SHOWDOWN;
                maoFinalizada = true;
                botCartasReveladas = true;
                mensagemDesfecho = "Showdown: Você venceu a mão com Dois Pares (Áses e Reis)!";
                break;
            case SHOWDOWN:
                maoFinalizada = true;
                break;
        }
    }

    private String formatarMoeda(long valor) {
        return formatadorMoeda.format(valor);
    }

    // Getters para consulta e testes de interface
    public PokerPhase getFaseAtual() {
        return faseAtual;
    }

    public void setFaseAtual(PokerPhase fase) {
        this.faseAtual = fase;
    }

    public boolean isMaoFinalizada() {
        return maoFinalizada;
    }

    public boolean isBotCartasReveladas() {
        return botCartasReveladas;
    }

    public String getMensagemDesfecho() {
        return mensagemDesfecho;
    }

    public long getPoteCentral() {
        return poteCentral;
    }

    public long getApostaJogador() {
        return apostaJogador;
    }

    public long getApostaBot() {
        return apostaBot;
    }

    public List<Card> getHoleCardsJogador() {
        return Collections.unmodifiableList(holeCardsJogador);
    }

    public List<Card> getHoleCardsBot() {
        return Collections.unmodifiableList(holeCardsBot);
    }

    public List<Card> getBoardComunitario() {
        return Collections.unmodifiableList(boardComunitario);
    }

    public List<Rectangle> getBoundsBoard() {
        return Collections.unmodifiableList(boundsBoard);
    }

    public Rectangle getBtnFold() {
        return new Rectangle(btnFold);
    }

    public Rectangle getBtnCheckCall() {
        return new Rectangle(btnCheckCall);
    }

    public Rectangle getBtnRaise() {
        return new Rectangle(btnRaise);
    }

    public Rectangle getBtnVoltar() {
        return new Rectangle(btnVoltar);
    }

    public Rectangle getBtnNovaMao() {
        return new Rectangle(btnNovaMao);
    }
}
