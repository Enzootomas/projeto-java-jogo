package view;

import model.Card;
import model.EvaluatedPokerHand;
import model.PokerAction;
import model.PokerEngine;
import model.PokerParticipant;
import model.PokerRoundResult;
import model.PokerStage;
import model.Suit;
import model.TexasHoldemGame;

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
import java.util.Set;

/**
 * Interface gráfica interativa da Mesa de Poker Texas Hold'em (1 vs 1) acoplada ao
 * contrato oficial {@link PokerEngine}.
 *
 * <p>Toda a lógica de regras, apostas, blinds, avaliação de mãos, turnos do Bot e
 * ocultação de cartas permanece concentrada no motor ({@link TexasHoldemGame}).
 * A interface apenas consulta o estado público exposto por {@link PokerEngine} e
 * encaminha as ações do jogador.</p>
 */
public class PokerTableView {

    private final ScreenSaver screenSaver;
    private final PokerEngine engine;
    private final NumberFormat formatadorMoeda = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));

    // Áreas de interação para cliques do mouse
    private final Rectangle btnFold = new Rectangle();
    private final Rectangle btnCheckCall = new Rectangle();
    private final Rectangle btnRaise = new Rectangle();
    private final Rectangle btnVoltar = new Rectangle();
    private final Rectangle btnNovaMao = new Rectangle();

    // Bounds das 5 cartas comunitárias para verificação e layout
    private final List<Rectangle> boundsBoard = new ArrayList<>(5);

    // Mensagem temporária de feedback para ações inválidas ou falta de fichas
    private String mensagemAlerta = "";
    private long alertaExpiraEmMs = 0L;

    /**
     * Cria a view de Poker inicializando um motor oficial {@link TexasHoldemGame} padrão
     * com uma mão pronta para jogar.
     *
     * @param screenSaver referência ao painel principal do salão (pode ser nulo em testes isolados).
     */
    public PokerTableView(ScreenSaver screenSaver) {
        this(screenSaver, criarMotorPadraoIniciado());
    }

    /**
     * Cria a view de Poker acoplada a uma instância específica de {@link PokerEngine}
     * (produção ou motor determinístico injetado em testes).
     *
     * @param screenSaver referência ao painel principal do salão (pode ser nulo em testes isolados).
     * @param engine      implementação de {@link PokerEngine} (não pode ser nula).
     */
    public PokerTableView(ScreenSaver screenSaver, PokerEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("O motor de Poker (PokerEngine) não pode ser nulo.");
        }
        this.screenSaver = screenSaver;
        this.engine = engine;
    }

    private static PokerEngine criarMotorPadraoIniciado() {
        TexasHoldemGame motor = new TexasHoldemGame();
        motor.iniciarNovaMao();
        return motor;
    }

    /**
     * Renderiza todos os elementos visuais da mesa de Poker Texas Hold'em a partir do estado do motor.
     */
    public void render(Graphics2D g, int largura, int altura) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Fundo do salão escuro
        g.setColor(new Color(10, 14, 20));
        g.fillRect(0, 0, largura, altura);

        // 2. Tampo da mesa em feltro bordô nobre
        desenharMesaFeltro(g, largura, altura);

        // 3. Cabeçalho, fase atual, blinds e turno
        desenharCabecalho(g, largura);

        // 4. Área do Bot (Topo) — consulta estritamente getCartasVisiveisBot() / hasCartasOcultasBot()
        desenharAreaBot(g, largura);

        // 5. Área Central: Pote, última ação e 5 espaços do Board comunitário
        desenharAreaBoardEPot(g, largura, altura);

        // 6. Área do Jogador (Base)
        desenharAreaJogador(g, largura, altura);

        // 7. Controles contextuais guiados por engine.getAcoesValidasJogador()
        desenharPainelAcoes(g, largura, altura);

        // 8. Botão Voltar ao Salão (B)
        desenharBotaoVoltar(g, altura);

        // 9. Banner de Desfecho (Showdown, Desistência ou Aguardando Mão)
        PokerStage estagio = engine.getEstagioAtual();
        if (estagio.isMaoFinalizada() || estagio == PokerStage.AGUARDANDO_MAO) {
            desenharBannerDesfecho(g, largura, altura);
        }

        // 10. Alerta temporário (ex.: ação inválida ou saldo insuficiente)
        desenharAlerta(g, largura, altura);
    }

    private void desenharMesaFeltro(Graphics2D g, int largura, int altura) {
        int margemX = 55;
        int margemY = 40;
        int mesaW = largura - (margemX * 2);
        int mesaH = altura - (margemY * 2);

        g.setColor(new Color(42, 18, 18));
        g.fillRoundRect(margemX - 10, margemY - 10, mesaW + 20, mesaH + 20, 48, 48);

        g.setColor(new Color(212, 175, 55));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(margemX - 5, margemY - 5, mesaW + 10, mesaH + 10, 42, 42);
        g.setStroke(new BasicStroke(1.0f));

        GradientPaint feltro = new GradientPaint(
                largura / 2.0f, margemY, new Color(110, 18, 28),
                largura / 2.0f, margemY + mesaH, new Color(55, 8, 14)
        );
        g.setPaint(feltro);
        g.fillRoundRect(margemX, margemY, mesaW, mesaH, 36, 36);

        g.setColor(new Color(212, 175, 55, 70));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(margemX + 24, margemY + 24, mesaW - 48, mesaH - 48, 28, 28);
        g.setStroke(new BasicStroke(1.0f));
    }

    private void desenharCabecalho(Graphics2D g, int largura) {
        int cx = largura / 2;

        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(255, 225, 130));
        FontMetrics fm = g.getFontMetrics();
        String titulo = "TEXAS HOLD'EM POKER (1 vs 1)";
        g.drawString(titulo, cx - (fm.stringWidth(titulo) / 2), 64);

        PokerStage estagio = engine.getEstagioAtual();
        PokerParticipant turno = engine.getTurnoAtual();
        String txtTurno;
        if (turno == PokerParticipant.JOGADOR) {
            txtTurno = "Sua vez";
        } else if (turno == PokerParticipant.BOT) {
            txtTurno = "Turno do Bot";
        } else {
            txtTurno = "Mão encerrada";
        }

        String sub = "Fase: " + estagio.getNomeExibicao().toUpperCase(Locale.ROOT)
                + "  |  Blinds: " + formatarMoeda(engine.getSmallBlind()) + "/" + formatarMoeda(engine.getBigBlind())
                + "  |  " + txtTurno;

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(255, 205, 100));
        FontMetrics fmSub = g.getFontMetrics();
        g.drawString(sub, cx - (fmSub.stringWidth(sub) / 2), 82);
    }

    private void desenharAreaBot(Graphics2D g, int largura) {
        int cx = largura / 2;
        int yBot = 104;

        String marcadorDealer = (engine.getBotaoDealer() == PokerParticipant.BOT) ? " [D]" : "";
        String infoBot = "Bot" + marcadorDealer
                + " — Stack: " + formatarMoeda(engine.getSaldoBot())
                + " | Aposta: " + formatarMoeda(engine.getApostaBotNaRua());

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(235, 235, 235));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(infoBot, cx - (fm.stringWidth(infoBot) / 2), yBot);

        int cardW = 54;
        int cardH = 76;
        int gap = 12;
        int qtdCartasBot = Math.max(2, engine.getQuantidadeCartasBot());
        int totalW = (qtdCartasBot * cardW) + ((qtdCartasBot - 1) * gap);
        int startX = cx - (totalW / 2);
        int cardY = yBot + 10;

        List<Card> cartasVisiveisBot = engine.getCartasVisiveisBot();
        boolean ocultas = engine.hasCartasOcultasBot();

        for (int i = 0; i < qtdCartasBot; i++) {
            int x = startX + i * (cardW + gap);
            if (!ocultas && i < cartasVisiveisBot.size()) {
                desenharCartaAberta(g, cartasVisiveisBot.get(i), x, cardY, cardW, cardH);
            } else if (engine.getQuantidadeCartasBot() > 0) {
                desenharCartaFechada(g, x, cardY, cardW, cardH);
            } else {
                desenharSlotVazio(g, x, cardY, cardW, cardH, "BOT");
            }
        }

        EvaluatedPokerHand maoBotShowdown = engine.getMelhorMaoBotShowdown();
        if (maoBotShowdown != null) {
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(255, 210, 140));
            String descBot = "Mão do Bot: " + maoBotShowdown.getDescricaoCurta();
            FontMetrics fmDesc = g.getFontMetrics();
            g.drawString(descBot, cx - (fmDesc.stringWidth(descBot) / 2), cardY + cardH + 16);
        }
    }

    private void desenharAreaBoardEPot(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int cy = (altura / 2) - 10;

        // Pote Central
        g.setColor(new Color(0, 0, 0, 165));
        g.fillRoundRect(cx - 115, cy - 68, 230, 32, 12, 12);
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(cx - 115, cy - 68, 230, 32, 12, 12);
        g.setStroke(new BasicStroke(1.0f));

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(255, 235, 120));
        String txtPote = "POTE: " + formatarMoeda(engine.getPoteTotal()) + " Fichas";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txtPote, cx - (fm.stringWidth(txtPote) / 2), cy - 47);

        // 5 espaços reservados do Board comunitário
        int cardW = 60;
        int cardH = 84;
        int gap = 12;
        int totalW = (5 * cardW) + (4 * gap);
        int startX = cx - (totalW / 2);
        int boardY = cy - 22;

        boundsBoard.clear();
        List<Card> comunitarias = engine.getCartasComunitarias();

        for (int i = 0; i < 5; i++) {
            int x = startX + i * (cardW + gap);
            boundsBoard.add(new Rectangle(x, boardY, cardW, cardH));

            String rotuloSlot = (i < 3) ? "FLOP" : (i == 3 ? "TURN" : "RIVER");
            if (i < comunitarias.size()) {
                desenharCartaAberta(g, comunitarias.get(i), x, boardY, cardW, cardH);
            } else {
                desenharSlotVazio(g, x, boardY, cardW, cardH, rotuloSlot);
            }
        }

        // Feedback da última ação da mesa
        String ultimaAcao = engine.getUltimaAcaoDescricao();
        if (ultimaAcao != null && !ultimaAcao.isBlank()) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g.setColor(new Color(235, 225, 195));
            FontMetrics fmAcao = g.getFontMetrics();
            g.drawString(ultimaAcao, cx - (fmAcao.stringWidth(ultimaAcao) / 2), boardY + cardH + 22);
        }
    }

    private void desenharSlotVazio(Graphics2D g, int x, int y, int w, int h, String rotulo) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(x, y, w, h, 8, 8);

        g.setColor(new Color(212, 175, 55, 130));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setStroke(new BasicStroke(1.0f));

        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(new Color(220, 190, 110, 170));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(rotulo, x + (w - fm.stringWidth(rotulo)) / 2, y + (h / 2) + 4);
    }

    private void desenharAreaJogador(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int yJogador = altura - 230;

        int cardW = 60;
        int cardH = 84;
        int gap = 14;
        int startX = cx - cardW - (gap / 2);

        List<Card> maoJogador = engine.getMaoJogador();
        for (int i = 0; i < 2; i++) {
            int x = startX + i * (cardW + gap);
            if (i < maoJogador.size()) {
                desenharCartaAberta(g, maoJogador.get(i), x, yJogador, cardW, cardH);
            } else {
                desenharSlotVazio(g, x, yJogador, cardW, cardH, "CARTA " + (i + 1));
            }
        }

        // Classificação oficial vinda do motor (a partir do Flop)
        EvaluatedPokerHand melhorAtual = engine.getMelhorMaoAtualJogador();
        String textoMao = (melhorAtual != null)
                ? "Sua Mão: " + melhorAtual.getDescricaoCurta()
                : (maoJogador.size() == 2 ? "Sua Mão: Cartas fechadas (Pré-Flop)" : "Aguardando distribuição");

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(255, 230, 140));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(textoMao, cx - (fm.stringWidth(textoMao) / 2), yJogador + cardH + 18);

        String marcadorDealer = (engine.getBotaoDealer() == PokerParticipant.JOGADOR) ? " [D]" : "";
        String infoJogador = "Você" + marcadorDealer
                + " — Stack: " + formatarMoeda(engine.getSaldoJogador()) + " Fichas"
                + "  |  Sua Aposta: " + formatarMoeda(engine.getApostaJogadorNaRua());

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(180, 240, 180));
        FontMetrics fmInfo = g.getFontMetrics();
        g.drawString(infoJogador, cx - (fmInfo.stringWidth(infoJogador) / 2), yJogador + cardH + 35);
    }

    private void desenharPainelAcoes(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int yBotoes = altura - 102;
        int btnW = 152;
        int btnH = 38;
        int gap = 12;

        Set<PokerAction> acoesValidas = engine.getAcoesValidasJogador();
        boolean podeFold = acoesValidas.contains(PokerAction.DESISTIR);
        boolean podePassar = acoesValidas.contains(PokerAction.PASSAR);
        boolean podePagar = acoesValidas.contains(PokerAction.PAGAR);
        boolean podeAumentar = acoesValidas.contains(PokerAction.AUMENTAR);

        String txtCheckCall;
        if (podePagar) {
            txtCheckCall = "PAGAR " + formatarMoeda(engine.getValorParaPagarJogador()) + " (C)";
        } else {
            txtCheckCall = "PASSAR (C)";
        }

        long aumentoPadrao = engine.getAumentoMinimoJogador();
        String txtRaise = podeAumentar && aumentoPadrao > 0L
                ? "AUMENTAR +" + formatarMoeda(aumentoPadrao) + " (R)"
                : "AUMENTAR (R)";

        int xFold = cx - (btnW * 3 / 2) - gap;
        btnFold.setBounds(xFold, yBotoes, btnW, btnH);
        desenharBotao(g, btnFold, "DESISTIR (F)", new Color(140, 30, 30), podeFold);

        int xCheck = cx - (btnW / 2);
        btnCheckCall.setBounds(xCheck, yBotoes, btnW, btnH);
        desenharBotao(g, btnCheckCall, txtCheckCall, new Color(28, 110, 60), podePassar || podePagar);

        int xRaise = cx + (btnW / 2) + gap;
        btnRaise.setBounds(xRaise, yBotoes, btnW, btnH);
        desenharBotao(g, btnRaise, txtRaise, new Color(175, 125, 20), podeAumentar);

        // Dica discreta de atalhos de aumento (1 = Mín, 2 = 2x BB, 3 = All-In)
        if (podeAumentar) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(220, 210, 185));
            String dica = "Atalhos de aumento: [1/R] Mínimo (+" + formatarMoeda(aumentoPadrao)
                    + ")  |  [2] 2x BB  |  [3] All-In (+" + formatarMoeda(engine.getAumentoMaximoJogador()) + ")";
            FontMetrics fmDica = g.getFontMetrics();
            g.drawString(dica, cx - (fmDica.stringWidth(dica) / 2), yBotoes + btnH + 18);
        }
    }

    private void desenharBotaoVoltar(Graphics2D g, int altura) {
        int btnW = 155;
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
        int bannerW = 540;
        int bannerH = 112;
        int bannerX = cx - (bannerW / 2);
        int bannerY = (altura / 2) - 96;

        g.setColor(new Color(10, 12, 18, 238));
        g.fillRoundRect(bannerX, bannerY, bannerW, bannerH, 16, 16);
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(bannerX, bannerY, bannerW, bannerH, 16, 16);
        g.setStroke(new BasicStroke(1.0f));

        String tituloDesfecho = getMensagemDesfecho();
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(255, 235, 120));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(tituloDesfecho, cx - (fm.stringWidth(tituloDesfecho) / 2), bannerY + 28);

        PokerRoundResult res = engine.getResultadoMao();
        if (res.isShowdown() && engine.getMelhorMaoJogadorShowdown() != null && engine.getMelhorMaoBotShowdown() != null) {
            String detalhe = "Você: " + engine.getMelhorMaoJogadorShowdown().getDescricaoCurta()
                    + "  x  Bot: " + engine.getMelhorMaoBotShowdown().getDescricaoCurta();
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(220, 230, 240));
            FontMetrics fmDet = g.getFontMetrics();
            g.drawString(detalhe, cx - (fmDet.stringWidth(detalhe) / 2), bannerY + 48);
        }

        int btnW = 190;
        int btnH = 36;
        btnNovaMao.setBounds(cx - (btnW / 2), bannerY + 62, btnW, btnH);
        desenharBotao(g, btnNovaMao, "NOVA MÃO (N)", new Color(30, 110, 50), true);
    }

    private void desenharAlerta(Graphics2D g, int largura, int altura) {
        if (mensagemAlerta == null || mensagemAlerta.isEmpty() || System.currentTimeMillis() > alertaExpiraEmMs) {
            return;
        }
        int cx = largura / 2;
        int y = altura - 135;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(mensagemAlerta) + 32;
        g.setColor(new Color(140, 25, 25, 225));
        g.fillRoundRect(cx - w / 2, y - 16, w, 24, 10, 10);
        g.setColor(new Color(255, 220, 180));
        g.drawString(mensagemAlerta, cx - fm.stringWidth(mensagemAlerta) / 2, y);
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

        g.setColor(new Color(30, 60, 120));
        g.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 5, 5);
        g.drawLine(x + 4, y + 4, x + w - 4, y + h - 4);
        g.drawLine(x + w - 4, y + 4, x + 4, y + h - 4);
    }

    // =========================================================================
    // TRATAMENTO DE CLIQUES E ATALHOS DE TECLADO (DELEGANDO AO PokerEngine)
    // =========================================================================

    /**
     * Processa cliques do mouse na mesa de Poker.
     *
     * @return {@code true} se o usuário clicou no botão "Voltar ao Salão"; {@code false} caso contrário.
     */
    public boolean processarClique(int x, int y) {
        if (btnVoltar.contains(x, y)) {
            return true;
        }

        PokerStage estagio = engine.getEstagioAtual();
        if (estagio.isMaoFinalizada() || estagio == PokerStage.AGUARDANDO_MAO) {
            if (btnNovaMao.contains(x, y)) {
                iniciarNovaMao();
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
     * Processa comandos de atalho de teclado na mesa de Poker, respeitando
     * estritamente {@link PokerEngine#getAcoesValidasJogador()}.
     */
    public void processarTecla(String comando) {
        if (comando == null) {
            return;
        }
        if ("B".equalsIgnoreCase(comando) || "VOLTAR".equalsIgnoreCase(comando)) {
            if (screenSaver != null) {
                screenSaver.processarRetornoB();
            }
            return;
        }

        PokerStage estagio = engine.getEstagioAtual();
        if (estagio.isMaoFinalizada() || estagio == PokerStage.AGUARDANDO_MAO) {
            if ("N".equalsIgnoreCase(comando) || "NOVA_MAO".equalsIgnoreCase(comando)) {
                iniciarNovaMao();
            }
            return;
        }

        if ("FOLD".equalsIgnoreCase(comando) || "F".equalsIgnoreCase(comando)) {
            executarFold();
        } else if ("CHECK".equalsIgnoreCase(comando) || "CALL".equalsIgnoreCase(comando)
                || "C".equalsIgnoreCase(comando) || "ESPACO".equalsIgnoreCase(comando)) {
            executarCheckCall();
        } else if ("RAISE".equalsIgnoreCase(comando) || "R".equalsIgnoreCase(comando)
                || "FICHA_1".equalsIgnoreCase(comando)) {
            executarRaise();
        } else if ("FICHA_2".equalsIgnoreCase(comando)) {
            executarRaiseComMultiplicadorBB(2L);
        } else if ("FICHA_3".equalsIgnoreCase(comando)) {
            executarAllIn();
        }
    }

    /**
     * Inicia uma nova mão no motor de Poker.
     *
     * @return {@code true} se o motor iniciou a mão; {@code false} caso contrário.
     */
    public boolean iniciarNovaMao() {
        boolean iniciou = engine.iniciarNovaMao();
        if (!iniciou) {
            exibirAlerta("Não foi possível iniciar nova mão (verifique saldo para os blinds).");
        }
        solicitarRepintura();
        return iniciou;
    }

    /**
     * Encaminha a ação de Fold (Desistir) ao motor se permitida por {@link PokerEngine#getAcoesValidasJogador()}.
     *
     * @return {@code true} se o motor aceitou a desistência.
     */
    public boolean executarFold() {
        if (!engine.getAcoesValidasJogador().contains(PokerAction.DESISTIR)) {
            exibirAlerta("Ação Desistir indisponível neste momento.");
            return false;
        }
        boolean ok = engine.desistir();
        solicitarRepintura();
        return ok;
    }

    /**
     * Encaminha contextualmente {@link PokerEngine#passar()} ou {@link PokerEngine#pagar()}
     * conforme o conjunto de ações válidas informado pelo motor.
     *
     * @return {@code true} se o motor executou Check ou Call.
     */
    public boolean executarCheckCall() {
        Set<PokerAction> acoes = engine.getAcoesValidasJogador();
        boolean ok = false;
        if (acoes.contains(PokerAction.PASSAR)) {
            ok = engine.passar();
        } else if (acoes.contains(PokerAction.PAGAR)) {
            ok = engine.pagar();
        } else {
            exibirAlerta("Ação Passar/Pagar indisponível neste momento.");
        }
        solicitarRepintura();
        return ok;
    }

    /**
     * Encaminha o aumento mínimo válido ({@link PokerEngine#getAumentoMinimoJogador()}) ao motor.
     *
     * @return {@code true} se o aumento foi aceito pelo motor.
     */
    public boolean executarRaise() {
        if (!engine.getAcoesValidasJogador().contains(PokerAction.AUMENTAR)) {
            exibirAlerta("Ação Aumentar indisponível nesta rua.");
            return false;
        }
        long incremento = engine.getAumentoMinimoJogador();
        return executarRaiseValor(incremento);
    }

    /**
     * Encaminha um aumento específico ao motor (validado exclusivamente pelo motor).
     *
     * @param valorAcimaDaApostaAtual valor adicional acima da maior aposta da rua.
     * @return {@code true} se aceito pelo motor.
     */
    public boolean executarRaiseValor(long valorAcimaDaApostaAtual) {
        if (!engine.getAcoesValidasJogador().contains(PokerAction.AUMENTAR)) {
            exibirAlerta("Ação Aumentar indisponível nesta rua.");
            return false;
        }
        boolean ok = engine.aumentar(valorAcimaDaApostaAtual);
        if (!ok) {
            exibirAlerta("Valor de aumento inválido para a rua atual.");
        }
        solicitarRepintura();
        return ok;
    }

    private boolean executarRaiseComMultiplicadorBB(long multiplicador) {
        if (!engine.getAcoesValidasJogador().contains(PokerAction.AUMENTAR)) {
            exibirAlerta("Ação Aumentar indisponível nesta rua.");
            return false;
        }
        long desejado = Math.multiplyExact(engine.getBigBlind(), multiplicador);
        long maximo = engine.getAumentoMaximoJogador();
        long valor = Math.min(desejado, maximo);
        if (valor < engine.getAumentoMinimoJogador()) {
            valor = engine.getAumentoMinimoJogador();
        }
        return executarRaiseValor(valor);
    }

    private boolean executarAllIn() {
        if (!engine.getAcoesValidasJogador().contains(PokerAction.AUMENTAR)) {
            exibirAlerta("All-In por aumento indisponível nesta rua.");
            return false;
        }
        return executarRaiseValor(engine.getAumentoMaximoJogador());
    }

    private void exibirAlerta(String msg) {
        this.mensagemAlerta = msg;
        this.alertaExpiraEmMs = System.currentTimeMillis() + 3000L;
    }

    private void solicitarRepintura() {
        if (screenSaver != null) {
            screenSaver.repaint();
        }
    }

    private String formatarMoeda(long valor) {
        return formatadorMoeda.format(valor);
    }

    // =========================================================================
    // CONSULTAS PARA TESTES E INSPECAO DA VIEW (SEM VAZAMENTO DE CARTAS DO BOT)
    // =========================================================================

    public PokerEngine getEngine() {
        return engine;
    }

    public PokerStage getEstagioAtual() {
        return engine.getEstagioAtual();
    }

    public boolean isMaoFinalizada() {
        return engine.getEstagioAtual().isMaoFinalizada();
    }

    /**
     * Indica se as cartas do Bot estão efetivamente reveladas pelo motor (exclusivamente no Showdown).
     */
    public boolean isBotCartasReveladas() {
        return !engine.hasCartasOcultasBot() && !engine.getCartasVisiveisBot().isEmpty();
    }

    public String getMensagemDesfecho() {
        return engine.getResultadoMao().getDescricaoPadrao();
    }

    public String getMensagemAlerta() {
        return mensagemAlerta;
    }

    public long getPoteCentral() {
        return engine.getPoteTotal();
    }

    public long getApostaJogador() {
        return engine.getApostaJogadorNaRua();
    }

    public long getApostaBot() {
        return engine.getApostaBotNaRua();
    }

    public List<Card> getHoleCardsJogador() {
        return engine.getMaoJogador();
    }

    /**
     * Retorna exclusivamente as cartas do Bot autorizadas pelo motor (lista vazia fora do Showdown).
     */
    public List<Card> getCartasVisiveisBot() {
        return engine.getCartasVisiveisBot();
    }

    public List<Card> getBoardComunitario() {
        return engine.getCartasComunitarias();
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
