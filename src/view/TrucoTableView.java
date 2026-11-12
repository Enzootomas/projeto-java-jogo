package view;

import model.Card;
import model.Suit;
import model.TrucoHandResult;
import model.TrucoPlayer;
import model.TrucoRound;
import model.TrucoRules;
import model.TrucoTrick;
import model.TrucoTrickResult;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface gráfica completa e interativa da Mesa de Truco Paulista (1 vs 1).
 *
 * <p>Consome estritamente o motor de regras {@link TrucoRound}, sem duplicar
 * regras de comparação ou pontuação na interface visual.</p>
 *
 * <p>Apresenta:
 * <ul>
 *   <li>Tampo temático em feltro azul anil clássico de cassino.</li>
 *   <li>Área do Bot: exibe apenas os versos das cartas restantes na mão (sem vazamento de dados).</li>
 *   <li>Área central: carta Vira aberta, indicação clara da Manilha da rodada e os 4 tombos.</li>
 *   <li>Área de disputa da vaza: cartas jogadas abertas do Jogador e do Bot.</li>
 *   <li>Placar oficial de até 3 vazas com indicação de vitória ou canga (empate).</li>
 *   <li>Área do Jogador: cartas abertas da mão com seleção por clique ou atalhos (1, 2, 3).</li>
 *   <li>Banner de conclusão: Vitória, Derrota ou Empate com botão "Nova mão" (ou tecla N).</li>
 *   <li>Botão e atalho B para retornar ao salão, preservando a mão em andamento.</li>
 * </ul>
 * </p>
 */
public class TrucoTableView {

    private final ScreenSaver screenSaver;

    // Áreas clicáveis do mouse
    private final List<Rectangle> boundsCartasJogador = new ArrayList<>();
    private final Rectangle btnNovaMao = new Rectangle();
    private final Rectangle btnVoltar = new Rectangle();

    public TrucoTableView(ScreenSaver screenSaver) {
        this.screenSaver = screenSaver;
    }

    /**
     * Renderiza todos os elementos visuais da mesa de Truco.
     */
    public void render(Graphics2D g, int largura, int altura) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        TrucoRound round = (screenSaver != null) ? screenSaver.getTrucoRound() : null;

        // 1. Fundo do salão escuro
        g.setColor(new Color(12, 16, 24));
        g.fillRect(0, 0, largura, altura);

        // 2. Feltro da mesa de Truco (azul anil sofisticado com moldura)
        desenharMesaFeltro(g, largura, altura);

        if (round == null) {
            desenharMensagemSemRodada(g, largura, altura);
            return;
        }

        // 3. Cabeçalho e Placar de Vazas
        desenharCabecalhoEPlacar(g, round, largura);

        // 4. Área do Bot (Topo: apenas versos de cartas)
        desenharAreaBot(g, round, largura, altura);

        // 5. Área Central da Mesa (Vira, Manilha e Cartas da Vaza)
        desenharAreaCentralMesa(g, round, largura, altura);

        // 6. Área do Jogador (Base: cartas abertas da mão)
        desenharAreaJogador(g, round, largura, altura);

        // 7. Banner de Conclusão de Mão (Vitória, Derrota ou Empate)
        if (round.isFinalizada()) {
            desenharBannerFimDeMao(g, round, largura, altura);
        }

        // 8. Botão Voltar ao Salão (B)
        desenharBotaoVoltar(g, largura, altura);
    }

    private void desenharMesaFeltro(Graphics2D g, int largura, int altura) {
        int margemX = 50;
        int margemY = 40;
        int mesaW = largura - (margemX * 2);
        int mesaH = altura - (margemY * 2);

        // Borda externa de madeira nobre
        g.setColor(new Color(45, 28, 18));
        g.fillRoundRect(margemX - 8, margemY - 8, mesaW + 16, mesaH + 16, 32, 32);

        // Filete dourado
        g.setColor(new Color(212, 175, 55));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(margemX - 4, margemY - 4, mesaW + 8, mesaH + 8, 28, 28);
        g.setStroke(new BasicStroke(1.0f));

        // Feltro azul anil com gradiente radial/linear
        GradientPaint feltro = new GradientPaint(
                largura / 2.0f, margemY, new Color(28, 48, 82),
                largura / 2.0f, margemY + mesaH, new Color(14, 25, 45)
        );
        g.setPaint(feltro);
        g.fillRoundRect(margemX, margemY, mesaW, mesaH, 24, 24);

        // Moldura interna do feltro
        g.setColor(new Color(255, 255, 255, 20));
        g.drawRoundRect(margemX + 10, margemY + 10, mesaW - 20, mesaH - 20, 18, 18);
    }

    private void desenharCabecalhoEPlacar(Graphics2D g, TrucoRound round, int largura) {
        int centroX = largura / 2;

        // Título
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(new Color(255, 215, 0));
        String titulo = "MESA DE TRUCO PAULISTA — 1 vs 1";
        FontMetrics fmT = g.getFontMetrics();
        g.drawString(titulo, centroX - (fmT.stringWidth(titulo) / 2), 70);

        // Placar das 3 Vazas
        List<TrucoTrick> vazas = round.getVazas();
        int boxW = 100;
        int boxH = 26;
        int espaco = 14;
        int totalW = (3 * boxW) + (2 * espaco);
        int startX = centroX - (totalW / 2);
        int placarY = 82;

        for (int i = 1; i <= 3; i++) {
            int bx = startX + ((i - 1) * (boxW + espaco));
            TrucoTrick vaza = (i <= vazas.size()) ? vazas.get(i - 1) : null;

            Color corFundo;
            Color corTexto;
            String rotulo;

            if (vaza == null) {
                corFundo = new Color(30, 40, 60, 140);
                corTexto = new Color(160, 170, 190);
                rotulo = "Vaza " + i + ": —";
            } else if (vaza.isCompleta()) {
                TrucoTrickResult res = vaza.getResultado();
                if (res == TrucoTrickResult.VITORIA_JOGADOR) {
                    corFundo = new Color(39, 174, 96, 220);
                    corTexto = Color.WHITE;
                    rotulo = "Vaza " + i + ": VOCÊ";
                } else if (res == TrucoTrickResult.VITORIA_BOT) {
                    corFundo = new Color(192, 57, 43, 220);
                    corTexto = Color.WHITE;
                    rotulo = "Vaza " + i + ": BOT";
                } else {
                    corFundo = new Color(243, 156, 18, 220);
                    corTexto = Color.BLACK;
                    rotulo = "Vaza " + i + ": CANGA";
                }
            } else {
                corFundo = new Color(41, 128, 185, 200);
                corTexto = Color.WHITE;
                rotulo = "Vaza " + i + ": EM JOGO";
            }

            g.setColor(corFundo);
            g.fillRoundRect(bx, placarY, boxW, boxH, 8, 8);
            g.setColor(new Color(255, 255, 255, 80));
            g.drawRoundRect(bx, placarY, boxW, boxH, 8, 8);

            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(corTexto);
            FontMetrics fmB = g.getFontMetrics();
            g.drawString(rotulo, bx + (boxW - fmB.stringWidth(rotulo)) / 2, placarY + 17);
        }
    }

    private void desenharAreaBot(Graphics2D g, TrucoRound round, int largura, int altura) {
        int centroX = largura / 2;
        int botY = 125;

        // Identificação do Bot
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(220, 220, 230));
        String txtBot = "BOT (Adversário) — Cartas na mão: " + round.getQuantidadeCartasBot();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txtBot, centroX - (fm.stringWidth(txtBot) / 2), botY);

        // Versos de cartas do Bot (sem revelar valor)
        int qtd = round.getQuantidadeCartasBot();
        int cardW = 55;
        int cardH = 75;
        int espaco = 10;
        int inicioX = centroX - ((qtd * (cardW + espaco) - espaco) / 2);

        for (int i = 0; i < qtd; i++) {
            int cx = inicioX + (i * (cardW + espaco));
            desenharVersoCarta(g, cx, botY + 8, cardW, cardH);
        }
    }

    private void desenharVersoCarta(Graphics2D g, int x, int y, int w, int h) {
        // Sombra
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(x + 2, y + 2, w, h, 8, 8);

        // Fundo vermelho carmesim do verso
        g.setColor(new Color(160, 25, 25));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(220, 200, 160));
        g.drawRoundRect(x, y, w, h, 8, 8);

        // Padrão decorativo do verso
        g.setColor(new Color(120, 15, 15));
        g.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 6, 6);
        g.setColor(new Color(255, 215, 0, 120));
        g.drawRoundRect(x + 6, y + 6, w - 12, h - 12, 4, 4);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        String icone = "♣";
        g.drawString(icone, x + (w - fm.stringWidth(icone)) / 2, y + (h / 2) + 6);
    }

    private void desenharAreaCentralMesa(Graphics2D g, TrucoRound round, int largura, int altura) {
        int centroX = largura / 2;
        int centroY = altura / 2 - 10;

        // 1. Carta Vira e Informações da Manilha (lado esquerdo do centro)
        int viraX = centroX - 220;
        int viraY = centroY - 50;

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(255, 215, 0));
        g.drawString("VIRA", viraX + 18, viraY - 10);

        desenharCartaAberta(g, round.getVira(), viraX, viraY, 70, 100);

        // Informação da Manilha e Tombos
        int infoManilhaX = viraX - 10;
        int infoManilhaY = viraY + 118;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(241, 196, 15));
        String manilhaTxt = "MANILHA: " + round.getManilhaRank().getNomeEmPortugues().toUpperCase() + " (" + round.getManilhaRank().getSimbolo() + ")";
        g.drawString(manilhaTxt, infoManilhaX, infoManilhaY);

        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(200, 210, 230));
        g.drawString("Tombos: ♣ > ♥ > ♠ > ♦", infoManilhaX, infoManilhaY + 14);

        // 2. Disputa da Vaza Atual (centro da mesa)
        TrucoTrick vazaAtual = round.getVazaAtual();
        int disputaY = centroY - 50;
        int cardW = 75;
        int cardH = 105;

        // Slot da Carta do Bot
        int slotBotX = centroX - 45;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(220, 220, 230));
        FontMetrics fm = g.getFontMetrics();
        String lblBot = "Carta do Bot";
        g.drawString(lblBot, slotBotX + (cardW - fm.stringWidth(lblBot)) / 2, disputaY - 8);

        if (vazaAtual != null && vazaAtual.getCartaBot() != null) {
            desenharCartaAberta(g, vazaAtual.getCartaBot(), slotBotX, disputaY, cardW, cardH);
        } else {
            desenharSlotVazio(g, slotBotX, disputaY, cardW, cardH, "Aguardando Bot");
        }

        // Slot da Carta do Jogador
        int slotJogX = centroX + 65;
        String lblJog = "Sua Carta";
        g.drawString(lblJog, slotJogX + (cardW - fm.stringWidth(lblJog)) / 2, disputaY - 8);

        if (vazaAtual != null && vazaAtual.getCartaJogador() != null) {
            desenharCartaAberta(g, vazaAtual.getCartaJogador(), slotJogX, disputaY, cardW, cardH);
        } else {
            desenharSlotVazio(g, slotJogX, disputaY, cardW, cardH, "Aguardando Você");
        }

        // Indicador de Turno Ativo
        int avisoTurnoY = centroY + 80;
        if (!round.isFinalizada()) {
            if (round.isTurnoJogador()) {
                desenharAvisoTurno(g, centroX, avisoTurnoY, "SUA VEZ DE JOGAR — Escolha uma carta abaixo (Clique ou Teclas 1, 2, 3)",
                        new Color(39, 174, 96, 220), Color.WHITE);
            } else {
                desenharAvisoTurno(g, centroX, avisoTurnoY, "VEZ DO BOT — O adversário está jogando...",
                        new Color(230, 126, 34, 220), Color.WHITE);
            }
        }
    }

    private void desenharAvisoTurno(Graphics2D g, int centroX, int y, String texto, Color corFundo, Color corTexto) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(texto) + 30;
        int h = 26;

        g.setColor(corFundo);
        g.fillRoundRect(centroX - (w / 2), y, w, h, 10, 10);
        g.setColor(new Color(255, 255, 255, 80));
        g.drawRoundRect(centroX - (w / 2), y, w, h, 10, 10);

        g.setColor(corTexto);
        g.drawString(texto, centroX - (fm.stringWidth(texto) / 2), y + 18);
    }

    private void desenharAreaJogador(Graphics2D g, TrucoRound round, int largura, int altura) {
        int centroX = largura / 2;
        int jogadorY = altura - 180;

        List<Card> mao = round.getMaoJogador();

        // Título da área do jogador
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(240, 240, 245));
        String txtMao = "SUA MÃO";
        if (round.isTurnoJogador() && !round.isFinalizada()) {
            txtMao += " (Selecione uma carta com clique ou atalhos [1], [2], [3])";
        }
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txtMao, centroX - (fm.stringWidth(txtMao) / 2), jogadorY - 14);

        int cardW = 82;
        int cardH = 118;
        int espaco = 22;
        int qtd = mao.size();

        boundsCartasJogador.clear();

        if (qtd == 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(180, 190, 205));
            String semCartas = "Você já jogou todas as suas cartas nesta mão.";
            FontMetrics fmS = g.getFontMetrics();
            g.drawString(semCartas, centroX - (fmS.stringWidth(semCartas) / 2), jogadorY + 50);
            return;
        }

        int inicioX = centroX - ((qtd * (cardW + espaco) - espaco) / 2);

        for (int i = 0; i < qtd; i++) {
            int cx = inicioX + (i * (cardW + espaco));
            Card carta = mao.get(i);

            Rectangle r = new Rectangle(cx, jogadorY, cardW, cardH);
            boundsCartasJogador.add(r);

            // Destaque de interatividade se for a vez do jogador
            boolean interativa = round.isTurnoJogador() && !round.isFinalizada();
            if (interativa) {
                g.setColor(new Color(255, 215, 0, 80));
                g.fillRoundRect(cx - 3, jogadorY - 3, cardW + 6, cardH + 6, 12, 12);
            }

            desenharCartaAberta(g, carta, cx, jogadorY, cardW, cardH);

            // Indicador de tecla de atalho [1], [2], [3]
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(interativa ? new Color(255, 215, 0) : new Color(150, 160, 175));
            String atalho = "[" + (i + 1) + "]";
            FontMetrics fmA = g.getFontMetrics();
            g.drawString(atalho, cx + (cardW - fmA.stringWidth(atalho)) / 2, jogadorY + cardH + 16);
        }
    }

    private void desenharBannerFimDeMao(Graphics2D g, TrucoRound round, int largura, int altura) {
        int centroX = largura / 2;
        int centroY = altura / 2 + 15;

        TrucoHandResult res = round.getResultadoMao();
        String titulo;
        String subtitulo;
        Color corFundo;
        Color corBorda;

        if (res == TrucoHandResult.VITORIA_JOGADOR) {
            titulo = "VITÓRIA!";
            subtitulo = "Parabéns! Você venceu a mão de Truco!";
            corFundo = new Color(20, 80, 45, 240);
            corBorda = new Color(46, 204, 113);
        } else if (res == TrucoHandResult.VITORIA_BOT) {
            titulo = "DERROTA!";
            subtitulo = "O Bot venceu a mão. Mais sorte na próxima!";
            corFundo = new Color(100, 25, 20, 240);
            corBorda = new Color(231, 76, 60);
        } else {
            titulo = "EMPATE!";
            subtitulo = "Mão anulada após 3 empates consecutivos (Cangas).";
            corFundo = new Color(40, 50, 70, 240);
            corBorda = new Color(241, 196, 15);
        }

        int painelW = 420;
        int painelH = 135;
        int px = centroX - (painelW / 2);
        int py = centroY - (painelH / 2);

        // Sombra
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(px + 4, py + 4, painelW, painelH, 16, 16);

        // Painel
        g.setColor(corFundo);
        g.fillRoundRect(px, py, painelW, painelH, 16, 16);
        g.setColor(corBorda);
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(px, py, painelW, painelH, 16, 16);
        g.setStroke(new BasicStroke(1.0f));

        // Título
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        FontMetrics fmT = g.getFontMetrics();
        g.drawString(titulo, centroX - (fmT.stringWidth(titulo) / 2), py + 34);

        // Subtítulo
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(230, 235, 245));
        FontMetrics fmS = g.getFontMetrics();
        g.drawString(subtitulo, centroX - (fmS.stringWidth(subtitulo) / 2), py + 58);

        // Botão Nova Mão
        int btnW = 180;
        int btnH = 38;
        btnNovaMao.setBounds(centroX - (btnW / 2), py + 78, btnW, btnH);

        g.setPaint(new GradientPaint(btnNovaMao.x, btnNovaMao.y, new Color(41, 128, 185),
                btnNovaMao.x, btnNovaMao.y + btnH, new Color(31, 97, 141)));
        g.fillRoundRect(btnNovaMao.x, btnNovaMao.y, btnW, btnH, 10, 10);
        g.setColor(new Color(255, 215, 0));
        g.drawRoundRect(btnNovaMao.x, btnNovaMao.y, btnW, btnH, 10, 10);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        FontMetrics fmBtn = g.getFontMetrics();
        String txtBtn = "NOVA MÃO (N)";
        g.drawString(txtBtn, btnNovaMao.x + (btnW - fmBtn.stringWidth(txtBtn)) / 2, btnNovaMao.y + 24);
    }

    private void desenharBotaoVoltar(Graphics2D g, int largura, int altura) {
        int btnW = 200;
        int btnH = 36;
        btnVoltar.setBounds(largura - btnW - 40, altura - 50, btnW, btnH);

        g.setColor(new Color(30, 35, 45));
        g.fillRoundRect(btnVoltar.x, btnVoltar.y, btnW, btnH, 8, 8);
        g.setColor(new Color(200, 200, 210, 100));
        g.drawRoundRect(btnVoltar.x, btnVoltar.y, btnW, btnH, 8, 8);

        g.setColor(new Color(240, 240, 250));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        String txt = "VOLTAR AO SALÃO (B)";
        g.drawString(txt, btnVoltar.x + (btnW - fm.stringWidth(txt)) / 2, btnVoltar.y + 23);
    }

    private void desenharSlotVazio(Graphics2D g, int x, int y, int w, int h, String rotulo) {
        g.setColor(new Color(255, 255, 255, 20));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setStroke(new BasicStroke(1.0f));

        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(rotulo, x + (w - fm.stringWidth(rotulo)) / 2, y + (h / 2) + 4);
    }

    private void desenharCartaAberta(Graphics2D g, Card carta, int x, int y, int w, int h) {
        // Sombra
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRoundRect(x + 3, y + 3, w, h, 8, 8);

        // Corpo da carta
        g.setColor(new Color(252, 252, 254));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(180, 180, 190));
        g.drawRoundRect(x, y, w, h, 8, 8);

        Color corNaipe = (carta.getSuit() == Suit.COPAS || carta.getSuit() == Suit.OUROS) ?
                new Color(205, 30, 30) : new Color(25, 25, 30);
        g.setColor(corNaipe);

        // Canto superior esquerdo: símbolo do rank e naipe
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(carta.getRank().getSimbolo(), x + 6, y + 18);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(carta.getSuit().getSimbolo(), x + 6, y + 32);

        // Centro: naipe em destaque
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        FontMetrics fmCentro = g.getFontMetrics();
        String simCentro = carta.getSuit().getSimbolo();
        g.drawString(simCentro, x + (w - fmCentro.stringWidth(simCentro)) / 2, y + (h / 2) + 11);

        // Canto inferior direito
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        String simRank = carta.getRank().getSimbolo();
        FontMetrics fmR = g.getFontMetrics();
        g.drawString(simRank, x + w - fmR.stringWidth(simRank) - 6, y + h - 8);
    }

    private void desenharMensagemSemRodada(Graphics2D g, int largura, int altura) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        String msg = "Nenhuma rodada de Truco ativa.";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg, (largura - fm.stringWidth(msg)) / 2, altura / 2);
    }

    /**
     * Processa cliques do mouse na mesa de Truco.
     *
     * @return true se o usuário clicou no botão para voltar ao salão.
     */
    public boolean processarClique(int x, int y) {
        if (btnVoltar.contains(x, y)) {
            return true;
        }

        // Suporte retrocompatível à coordenada central inferior do botão Voltar
        if (screenSaver != null) {
            int w = screenSaver.getWidth();
            int h = screenSaver.getHeight();
            int painelH = Math.min(500, Math.max(300, h - 80));
            int py = (h - painelH) / 2;
            int btnY = py + painelH - 85;
            int btnX = (w - 260) / 2;
            if (x >= btnX && x <= btnX + 260 && y >= btnY && y <= btnY + 48) {
                return true;
            }
        }

        TrucoRound round = (screenSaver != null) ? screenSaver.getTrucoRound() : null;
        if (round == null) {
            return false;
        }

        // Clique em "Nova Mão"
        if (round.isFinalizada() && btnNovaMao.contains(x, y)) {
            if (screenSaver != null) {
                screenSaver.iniciarNovaMaoTruco();
            }
            return false;
        }

        // Clique em uma carta da mão do jogador
        if (!round.isFinalizada() && round.isTurnoJogador()) {
            for (int i = 0; i < boundsCartasJogador.size(); i++) {
                if (boundsCartasJogador.get(i).contains(x, y)) {
                    jogarCartaIndex(i);
                    break;
                }
            }
        }

        return false;
    }

    /**
     * Processa comandos de tecla da mesa de Truco.
     */
    public void processarTecla(String comando) {
        if (comando == null) {
            return;
        }
        if ("VOLTAR".equalsIgnoreCase(comando) || "B".equalsIgnoreCase(comando)) {
            if (screenSaver != null) {
                screenSaver.processarRetornoB();
            }
            return;
        }

        TrucoRound round = (screenSaver != null) ? screenSaver.getTrucoRound() : null;
        if (round == null) {
            return;
        }

        if ("NOVA_MAO".equalsIgnoreCase(comando) || "N".equalsIgnoreCase(comando)) {
            if (round.isFinalizada() && screenSaver != null) {
                screenSaver.iniciarNovaMaoTruco();
            }
            return;
        }

        if (!round.isFinalizada() && round.isTurnoJogador()) {
            if ("CARTA_1".equalsIgnoreCase(comando) || "1".equalsIgnoreCase(comando) || "FICHA_1".equalsIgnoreCase(comando)) {
                jogarCartaIndex(0);
            } else if ("CARTA_2".equalsIgnoreCase(comando) || "2".equalsIgnoreCase(comando) || "FICHA_2".equalsIgnoreCase(comando)) {
                jogarCartaIndex(1);
            } else if ("CARTA_3".equalsIgnoreCase(comando) || "3".equalsIgnoreCase(comando) || "FICHA_3".equalsIgnoreCase(comando)) {
                jogarCartaIndex(2);
            }
        }
    }

    /**
     * Joga a carta correspondente ao índice na mão do jogador.
     */
    public void jogarCartaIndex(int index) {
        TrucoRound round = (screenSaver != null) ? screenSaver.getTrucoRound() : null;
        if (round == null || round.isFinalizada() || !round.isTurnoJogador()) {
            return;
        }

        List<Card> mao = round.getMaoJogador();
        if (index >= 0 && index < mao.size()) {
            Card carta = mao.get(index);
            round.jogarCartaJogador(carta);

            if (screenSaver != null && screenSaver.getPlayer() != null) {
                screenSaver.getPlayer().playSound();
            }

            // Se for a vez do Bot jogar (por exemplo, responding to player's move)
            if (!round.isFinalizada() && round.isTurnoBot()) {
                round.jogarTurnoBot();
            }
        }
    }

    public List<Rectangle> getBoundsCartasJogador() {
        return boundsCartasJogador;
    }

    public Rectangle getBtnNovaMao() {
        return btnNovaMao;
    }

    public Rectangle getBtnVoltar() {
        return btnVoltar;
    }
}
