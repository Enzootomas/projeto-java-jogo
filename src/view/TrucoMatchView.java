package view;

import model.Card;
import model.Suit;
import model.TrucoBetAction;
import model.TrucoMatch;
import model.TrucoMode;
import model.TrucoPlayedCard;
import model.TrucoSeat;
import model.TrucoTableTrick;
import model.TrucoTeam;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Interface gráfica completa da Mesa de Truco Paulista com:
 * <ul>
 *   <li>Seleção de modo ao entrar na mesa ({@link TrucoMode#INDIVIDUAL} ou {@link TrucoMode#DUPLAS})
 *       e confirmação explícita para trocar de modo durante uma partida em andamento.</li>
 *   <li>Layout para 2 lugares (Individual) ou 4 lugares alternados por equipe (Duplas).</li>
 *   <li>Controles de pedidos de Truco (1 → 3 → 6 → 9 → 12), Aceitar, Correr, Aumentar e Mão de 11.</li>
 *   <li>Ritmo desacelerado sem bloquear a EDT do Swing: pausa visível de ~1,0s antes de cada ação
 *       de Bot e ~2,0s exibindo todas as cartas da vaza resolvida antes de abrir a próxima vaza,
 *       pausando automaticamente ao sair com B e retomando do mesmo ponto ao voltar com E.</li>
 *   <li>Painel lateral da última vaza resolvida disponível para consulta durante toda a mão.</li>
 * </ul>
 */
public class TrucoMatchView {

    /** Pausa visível antes de cada Bot jogar carta ou responder pedido de Truco (segundos). */
    public static final double PAUSA_BOT_SEGUNDOS = 1.0;

    /** Pausa visível mantendo todas as cartas da vaza resolvida na mesa (segundos). */
    public static final double PAUSA_VAZA_RESOLVIDA_SEGUNDOS = 2.0;

    private final ScreenSaver screenSaver;

    private TrucoMatch match;
    private boolean exibindoSelecaoModo = true;
    private boolean confirmandoTrocaModo = false;

    // Temporizador não-bloqueante (acumulado via dt do loop de 60 FPS apenas enquanto MENU_TRUCO está ativo)
    private double cronometroPausaSegundos = 0.0;
    private String tipoPausaAtiva = "NENHUMA"; // "NENHUMA", "BOT", "VAZA"

    // Áreas clicáveis
    private final List<Rectangle> boundsCartasJogador = new ArrayList<>(3);
    private final Rectangle btnModoIndividual = new Rectangle();
    private final Rectangle btnModoDuplas = new Rectangle();
    private final Rectangle btnTrocarModo = new Rectangle();
    private final Rectangle btnConfirmarTroca = new Rectangle();
    private final Rectangle btnCancelarTroca = new Rectangle();

    private final Rectangle btnPedirTruco = new Rectangle();
    private final Rectangle btnAceitar = new Rectangle();
    private final Rectangle btnCorrer = new Rectangle();
    private final Rectangle btnAumentar = new Rectangle();

    private final Rectangle btnNovaMao = new Rectangle();
    private final Rectangle btnVoltar = new Rectangle();

    public TrucoMatchView(ScreenSaver screenSaver) {
        this.screenSaver = screenSaver;
    }

    /**
     * Inicia uma nova partida no modo escolhido (Individual ou Duplas).
     */
    public void iniciarPartidaNoModo(TrucoMode modo) {
        this.match = new TrucoMatch(modo);
        this.exibindoSelecaoModo = false;
        this.confirmandoTrocaModo = false;
        sincronizarTemporizadorAposMudancaEstado();
        solicitarRepintura();
    }

    /**
     * Permite injetar uma partida determinística (para testes automatizados).
     */
    public void setMatch(TrucoMatch matchDeterministico) {
        this.match = matchDeterministico;
        this.exibindoSelecaoModo = (matchDeterministico == null);
        this.confirmandoTrocaModo = false;
        sincronizarTemporizadorAposMudancaEstado();
        solicitarRepintura();
    }

    /**
     * Atualiza a temporização não-bloqueante do Truco usando o {@code dt} do loop do {@link ScreenSaver}.
     * Chamado exclusivamente quando a tela {@code MENU_TRUCO} está visível, pausando automaticamente
     * ao sair com {@code B} e retomando do exato ponto restante ao voltar com {@code E}.
     */
    public void atualizarTemporizacao(double dt) {
        if (dt <= 0.0 || match == null || exibindoSelecaoModo || confirmandoTrocaModo) {
            return;
        }

        if (match.isAguardandoTransicaoVaza()) {
            if (!"VAZA".equals(tipoPausaAtiva)) {
                tipoPausaAtiva = "VAZA";
                cronometroPausaSegundos = PAUSA_VAZA_RESOLVIDA_SEGUNDOS;
                return;
            }
            cronometroPausaSegundos -= dt;
            if (cronometroPausaSegundos <= 0.0) {
                cronometroPausaSegundos = 0.0;
                tipoPausaAtiva = "NENHUMA";
                match.avancarAposPausaVaza();
                sincronizarTemporizadorAposMudancaEstado();
                solicitarRepintura();
            }
            return;
        }

        if (match.isAguardandoAcaoBot()) {
            if (!"BOT".equals(tipoPausaAtiva)) {
                tipoPausaAtiva = "BOT";
                cronometroPausaSegundos = PAUSA_BOT_SEGUNDOS;
                return;
            }
            cronometroPausaSegundos -= dt;
            if (cronometroPausaSegundos <= 0.0) {
                cronometroPausaSegundos = 0.0;
                tipoPausaAtiva = "NENHUMA";
                match.executarPassoBot();
                if (screenSaver != null && screenSaver.getPlayer() != null) {
                    screenSaver.getPlayer().playSound();
                }
                sincronizarTemporizadorAposMudancaEstado();
                solicitarRepintura();
            }
            return;
        }

        tipoPausaAtiva = "NENHUMA";
        cronometroPausaSegundos = 0.0;
    }

    private void sincronizarTemporizadorAposMudancaEstado() {
        if (match == null) {
            tipoPausaAtiva = "NENHUMA";
            cronometroPausaSegundos = 0.0;
            return;
        }
        if (match.isAguardandoTransicaoVaza()) {
            tipoPausaAtiva = "VAZA";
            cronometroPausaSegundos = PAUSA_VAZA_RESOLVIDA_SEGUNDOS;
        } else if (match.isAguardandoAcaoBot()) {
            tipoPausaAtiva = "BOT";
            cronometroPausaSegundos = PAUSA_BOT_SEGUNDOS;
        } else {
            tipoPausaAtiva = "NENHUMA";
            cronometroPausaSegundos = 0.0;
        }
    }

    // =========================================================================
    // RENDERIZACAO
    // =========================================================================

    public void render(Graphics2D g, int largura, int altura) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(12, 16, 24));
        g.fillRect(0, 0, largura, altura);

        desenharMesaFeltro(g, largura, altura);
        desenharBotaoVoltar(g, altura);

        if (exibindoSelecaoModo || match == null) {
            desenharTelaSelecaoModo(g, largura, altura);
            return;
        }

        desenharCabecalhoEPlacar(g, largura);
        desenharAssentosBots(g, largura, altura);
        desenharAreaCentralMesa(g, largura, altura);
        desenharPainelUltimaVaza(g, largura, altura);
        desenharAreaJogador(g, largura, altura);
        desenharBarraApostasTruco(g, largura, altura);

        if (match.isPedidoTrucoPendente()) {
            desenharBannerPedidoTruco(g, largura, altura);
        }

        if (match.isMaoFinalizada()) {
            desenharBannerFimDeMaoOuPartida(g, largura, altura);
        }

        if (confirmandoTrocaModo) {
            desenharModalConfirmacaoTrocaModo(g, largura, altura);
        }
    }

    private void desenharMesaFeltro(Graphics2D g, int largura, int altura) {
        int mx = 48;
        int my = 36;
        int mw = largura - (mx * 2);
        int mh = altura - (my * 2);

        g.setColor(new Color(45, 28, 18));
        g.fillRoundRect(mx - 8, my - 8, mw + 16, mh + 16, 32, 32);

        g.setColor(new Color(212, 175, 55));
        g.setStroke(new BasicStroke(2.2f));
        g.drawRoundRect(mx - 4, my - 4, mw + 8, mh + 8, 28, 28);
        g.setStroke(new BasicStroke(1.0f));

        GradientPaint feltro = new GradientPaint(
                largura / 2.0f, my, new Color(24, 46, 82),
                largura / 2.0f, my + mh, new Color(12, 24, 46)
        );
        g.setPaint(feltro);
        g.fillRoundRect(mx, my, mw, mh, 24, 24);
    }

    private void desenharTelaSelecaoModo(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int cy = altura / 2;

        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(new Color(255, 224, 120));
        String titulo = "TRUCO PAULISTA — ESCOLHA O MODO DE JOGO";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(titulo, cx - (fm.stringWidth(titulo) / 2), cy - 95);

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(215, 225, 240));
        String sub = "Partida até 12 pontos  |  Pedidos: 1 → 3 → 6 → 9 → 12  |  Mão de 11 e Mão de Ferro";
        FontMetrics fmSub = g.getFontMetrics();
        g.drawString(sub, cx - (fmSub.stringWidth(sub) / 2), cy - 66);

        int cardW = 280;
        int cardH = 120;
        int gap = 36;

        btnModoIndividual.setBounds(cx - cardW - gap / 2, cy - 28, cardW, cardH);
        desenharCardModo(g, btnModoIndividual, "INDIVIDUAL (1 vs 1)", "Você contra 1 Bot",
                "Atalho: [1] ou [I]", new Color(32, 92, 142));

        btnModoDuplas.setBounds(cx + gap / 2, cy - 28, cardW, cardH);
        desenharCardModo(g, btnModoDuplas, "DUPLAS (2 vs 2)", "Você + Parceiro Bot vs 2 Bots",
                "Atalho: [2] ou [D]", new Color(46, 112, 74));
    }

    private void desenharCardModo(Graphics2D g, Rectangle r, String titulo, String desc, String atalho, Color cor) {
        g.setColor(cor);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        g.setColor(new Color(240, 202, 82));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        g.setStroke(new BasicStroke(1.0f));

        int cx = r.x + r.width / 2;
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(Color.WHITE);
        FontMetrics fmT = g.getFontMetrics();
        g.drawString(titulo, cx - fmT.stringWidth(titulo) / 2, r.y + 42);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(230, 240, 248));
        FontMetrics fmD = g.getFontMetrics();
        g.drawString(desc, cx - fmD.stringWidth(desc) / 2, r.y + 70);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(255, 232, 135));
        FontMetrics fmA = g.getFontMetrics();
        g.drawString(atalho, cx - fmA.stringWidth(atalho) / 2, r.y + 98);
    }

    private void desenharCabecalhoEPlacar(Graphics2D g, int largura) {
        int cx = largura / 2;

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(255, 224, 120));
        String titulo = "TRUCO PAULISTA — " + match.getModo().getNomeExibicao().toUpperCase();
        FontMetrics fmT = g.getFontMetrics();
        g.drawString(titulo, cx - (fmT.stringWidth(titulo) / 2), 62);

        // Placar da partida (até 12) + Valor atual da mão
        String badgeExtra = match.isMaoDeFerro() ? "  |  MÃO DE FERRO"
                : (match.isMaoDeOnze() ? "  |  MÃO DE 11" : "");
        String sub = "Placar (até 12): Sua Equipe " + match.getPontosEquipeJogador()
                + " × " + match.getPontosEquipeAdversaria() + " Adversários"
                + "   |   Mão vale: " + match.getValorMaoAtual() + " pt(s)"
                + "   |   Vazas: " + match.getVazasVencidasEquipe(TrucoTeam.EQUIPE_JOGADOR)
                + " × " + match.getVazasVencidasEquipe(TrucoTeam.EQUIPE_ADVERSARIA)
                + badgeExtra;

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(220, 238, 255));
        FontMetrics fmS = g.getFontMetrics();
        g.drawString(sub, cx - (fmS.stringWidth(sub) / 2), 82);

        // Botão Trocar Modo (canto superior direito)
        btnTrocarModo.setBounds(largura - 210, 48, 148, 30);
        desenharBotao(g, btnTrocarModo, "Trocar Modo (M)", new Color(40, 56, 86), true);
    }

    private void desenharAssentosBots(Graphics2D g, int largura, int altura) {
        List<TrucoSeat> seats = match.getSeats();
        int cx = largura / 2;
        int cy = altura / 2 - 15;

        if (match.getModo() == TrucoMode.INDIVIDUAL) {
            desenharAssentoBot(g, seats.get(1), cx, 108, true);
        } else {
            // 4 lugares: 2 = Parceiro (Topo), 1 = Bot 1 (Esquerda), 3 = Bot 2 (Direita)
            desenharAssentoBot(g, seats.get(2), cx, 106, true);
            desenharAssentoBot(g, seats.get(1), 155, cy - 20, false);
            desenharAssentoBot(g, seats.get(3), largura - 155, cy - 20, false);
        }
    }

    private void desenharAssentoBot(Graphics2D g, TrucoSeat seat, int centroX, int topoY, boolean horizontal) {
        boolean ehVez = !match.isMaoFinalizada() && !match.isAguardandoTransicaoVaza()
                && !match.isPedidoTrucoPendente() && match.getSeatVezAtual() == seat.getSeatIndex();

        String etiquetaEquipe = (seat.getEquipe() == TrucoTeam.EQUIPE_JOGADOR) ? " (Parceiro)" : " (Adv.)";
        String rotulo = seat.getNome() + (match.getModo() == TrucoMode.DUPLAS ? etiquetaEquipe : "");
        if (ehVez) {
            rotulo = "▶ " + rotulo + " (pensando...)";
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(ehVez ? new Color(255, 232, 110) : new Color(210, 224, 240));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(rotulo, centroX - fm.stringWidth(rotulo) / 2, topoY);

        int qtd = seat.getQuantidadeCartas();
        int cw = 36;
        int ch = 52;
        int gap = 8;
        int totalW = qtd > 0 ? (qtd * cw + (qtd - 1) * gap) : 0;
        int sx = centroX - totalW / 2;
        int sy = topoY + 8;

        for (int i = 0; i < qtd; i++) {
            desenharVersoCarta(g, sx + i * (cw + gap), sy, cw, ch);
        }
    }

    private void desenharAreaCentralMesa(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int cy = altura / 2 - 15;

        // Vira e Manilha à esquerda da área central
        int viraX = cx - 220;
        int viraY = cy - 42;
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(255, 224, 130));
        g.drawString("VIRA", viraX + 12, viraY - 6);
        if (match.getVira() != null) {
            desenharCartaAberta(g, match.getVira(), viraX, viraY, 56, 80);
            g.setColor(new Color(255, 235, 160));
            g.drawString("Manilha: " + match.getManilhaRank().getSimbolo(), viraX - 6, viraY + 95);
        }

        // Cartas jogadas na vaza atual (mantidas visíveis durante os 2s de pausa!)
        TrucoTableTrick vaza = match.getVazaAtual();
        if (vaza != null) {
            List<TrucoPlayedCard> jogadas = vaza.getJogadas();
            int cw = 56;
            int ch = 80;
            int gap = 14;
            int totalW = Math.max(1, jogadas.size()) * cw + Math.max(0, jogadas.size() - 1) * gap;
            int startX = cx - totalW / 2 + 20;
            int cardY = cy - 42;

            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(200, 225, 250));
            String cabVaza = "VAZA " + vaza.getNumeroVaza();
            FontMetrics fmV = g.getFontMetrics();
            g.drawString(cabVaza, cx + 20 - fmV.stringWidth(cabVaza) / 2, cardY - 12);

            if (jogadas.isEmpty()) {
                g.setColor(new Color(255, 255, 255, 45));
                g.drawRoundRect(cx - 28, cardY, 96, 80, 10, 10);
                g.setFont(new Font("SansSerif", Font.ITALIC, 11));
                g.setColor(new Color(190, 205, 225));
                g.drawString("Aguardando...", cx - 16, cardY + 44);
            } else {
                for (int i = 0; i < jogadas.size(); i++) {
                    TrucoPlayedCard j = jogadas.get(i);
                    int x = startX + i * (cw + gap);
                    desenharCartaAberta(g, j.getCarta(), x, cardY, cw, ch);
                    g.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g.setColor(j.getEquipe() == TrucoTeam.EQUIPE_JOGADOR
                            ? new Color(130, 240, 175) : new Color(255, 170, 150));
                    FontMetrics fmN = g.getFontMetrics();
                    g.drawString(j.getNomeParticipante(), x + (cw - fmN.stringWidth(j.getNomeParticipante())) / 2,
                            cardY + ch + 14);
                }
            }

            // Resultado visível durante a pausa de ~2s da vaza resolvida
            if (match.isAguardandoTransicaoVaza()) {
                String res = vaza.getResumoResultado();
                g.setFont(new Font("SansSerif", Font.BOLD, 13));
                FontMetrics fmR = g.getFontMetrics();
                int rw = fmR.stringWidth(res) + 28;
                g.setColor(new Color(14, 20, 32, 220));
                g.fillRoundRect(cx + 20 - rw / 2, cardY + ch + 22, rw, 26, 10, 10);
                g.setColor(new Color(255, 220, 95));
                g.drawRoundRect(cx + 20 - rw / 2, cardY + ch + 22, rw, 26, 10, 10);
                g.drawString(res, cx + 20 - fmR.stringWidth(res) / 2, cardY + ch + 39);
            }
        }

        // Descrição da última ação
        String desc = match.getUltimaAcaoDescricao();
        if (desc != null && !desc.isBlank()) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g.setColor(new Color(235, 232, 210));
            FontMetrics fmD = g.getFontMetrics();
            g.drawString(desc, cx - fmD.stringWidth(desc) / 2, cy + 92);
        }
    }

    /**
     * Painel discreto à direita mantendo a última vaza resolvida disponível para consulta durante toda a mão.
     */
    private void desenharPainelUltimaVaza(Graphics2D g, int largura, int altura) {
        TrucoTableTrick ultima = match.getUltimaVazaResolvida();
        if (ultima == null) {
            return;
        }
        int pw = 210;
        int ph = 86 + (ultima.getJogadas().size() * 16);
        int px = largura - pw - 62;
        int py = altura - ph - 125;

        g.setColor(new Color(14, 22, 36, 210));
        g.fillRoundRect(px, py, pw, ph, 10, 10);
        g.setColor(new Color(195, 165, 72));
        g.drawRoundRect(px, py, pw, ph, 10, 10);

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(255, 228, 130));
        g.drawString("ÚLTIMA VAZA (Vaza " + ultima.getNumeroVaza() + ")", px + 10, py + 18);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        int y = py + 36;
        for (TrucoPlayedCard j : ultima.getJogadas()) {
            g.setColor(new Color(228, 235, 245));
            g.drawString(j.getNomeParticipante() + ": " + j.getCarta().getSiglaVisual(), px + 12, y);
            y += 16;
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(140, 230, 175));
        g.drawString(ultima.getResumoResultado(), px + 10, py + ph - 12);
    }

    private void desenharAreaJogador(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int yCartas = altura - 196;
        int cw = 64;
        int ch = 90;
        int gap = 16;

        List<Card> mao = match.getMaoJogador();
        boundsCartasJogador.clear();

        int totalW = mao.size() * cw + Math.max(0, mao.size() - 1) * gap;
        int startX = cx - totalW / 2;

        boolean podeJogar = match.podeJogadorJogarCarta();
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(podeJogar ? new Color(130, 245, 170) : new Color(200, 210, 225));
        String tituloMao = podeJogar
                ? "SUA VEZ — Clique em uma carta ou pressione 1, 2 ou 3"
                : "SUA MÃO (aguardando turno / transição)";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(tituloMao, cx - fm.stringWidth(tituloMao) / 2, yCartas - 10);

        for (int i = 0; i < mao.size(); i++) {
            int x = startX + i * (cw + gap);
            Rectangle r = new Rectangle(x, yCartas, cw, ch);
            boundsCartasJogador.add(r);
            desenharCartaAberta(g, mao.get(i), x, yCartas, cw, ch);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(255, 230, 120));
            g.drawString("[" + (i + 1) + "]", x + cw / 2 - 8, yCartas + ch + 13);
        }
    }

    private void desenharBarraApostasTruco(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int y = altura - 82;
        int bw = 132;
        int bh = 34;
        int gap = 12;

        Set<TrucoBetAction> acoes = match.getAcoesApostaValidasJogador();
        boolean podePedir = acoes.contains(TrucoBetAction.PEDIR_TRUCO);
        boolean podeAceitar = acoes.contains(TrucoBetAction.ACEITAR);
        boolean podeCorrer = acoes.contains(TrucoBetAction.CORRER);
        boolean podeAumentar = acoes.contains(TrucoBetAction.AUMENTAR);

        int proximoValor = match.isPedidoTrucoPendente()
                ? TrucoMatch.proximoValorAposta(match.getValorPropostoTruco())
                : TrucoMatch.proximoValorAposta(match.getValorMaoAtual());

        int totalW = (4 * bw) + (3 * gap);
        int startX = cx - totalW / 2;

        btnPedirTruco.setBounds(startX, y, bw, bh);
        desenharBotao(g, btnPedirTruco, TrucoMatch.nomeDoValorAposta(proximoValor) + " (T)",
                new Color(175, 115, 20), podePedir);

        btnAceitar.setBounds(startX + (bw + gap), y, bw, bh);
        desenharBotao(g, btnAceitar, "ACEITAR (A)", new Color(34, 130, 72), podeAceitar);

        btnCorrer.setBounds(startX + 2 * (bw + gap), y, bw, bh);
        desenharBotao(g, btnCorrer, "CORRER (C)", new Color(156, 36, 36), podeCorrer);

        btnAumentar.setBounds(startX + 3 * (bw + gap), y, bw, bh);
        String txtAum = "PEDIR " + TrucoMatch.nomeDoValorAposta(proximoValor) + " (U)";
        desenharBotao(g, btnAumentar, txtAum, new Color(138, 68, 170), podeAumentar);
    }

    private void desenharBannerPedidoTruco(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int bw = 540;
        int bh = 74;
        int bx = cx - bw / 2;
        int by = 104;

        g.setColor(new Color(26, 14, 22, 238));
        g.fillRoundRect(bx, by, bw, bh, 14, 14);
        g.setColor(new Color(255, 205, 65));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(bx, by, bw, bh, 14, 14);
        g.setStroke(new BasicStroke(1.0f));

        String l1 = match.getNomeQuemPediuTruco() + " pediu "
                + TrucoMatch.nomeDoValorAposta(match.getValorPropostoTruco())
                + " (proposto: " + match.getValorPropostoTruco() + " pts | atual: " + match.getValorMaoAtual() + " pt)";
        String l2 = "Deve responder: " + match.getEquipeQueDeveResponderTruco().getNome()
                + (match.getEquipeQueDeveResponderTruco() == TrucoTeam.EQUIPE_JOGADOR
                        ? " — [A] Aceitar  |  [C] Correr  |  [U] Aumentar"
                        : " (aguardando decisão do Bot...)");

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(255, 230, 120));
        FontMetrics fm1 = g.getFontMetrics();
        g.drawString(l1, cx - fm1.stringWidth(l1) / 2, by + 28);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.WHITE);
        FontMetrics fm2 = g.getFontMetrics();
        g.drawString(l2, cx - fm2.stringWidth(l2) / 2, by + 52);
    }

    private void desenharBannerFimDeMaoOuPartida(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int bw = 500;
        int bh = 102;
        int bx = cx - bw / 2;
        int by = altura / 2 - 58;

        g.setColor(new Color(12, 16, 26, 238));
        g.fillRoundRect(bx, by, bw, bh, 16, 16);
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(bx, by, bw, bh, 16, 16);
        g.setStroke(new BasicStroke(1.0f));

        String msg = match.getUltimaAcaoDescricao();
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(255, 235, 130));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg, cx - fm.stringWidth(msg) / 2, by + 32);

        btnNovaMao.setBounds(cx - 105, by + 48, 210, 36);
        String txtBtn = match.isPartidaFinalizada() ? "NOVA PARTIDA (N)" : "NOVA MÃO (N)";
        desenharBotao(g, btnNovaMao, txtBtn, new Color(34, 130, 72), true);
    }

    private void desenharModalConfirmacaoTrocaModo(Graphics2D g, int largura, int altura) {
        int cx = largura / 2;
        int cy = altura / 2;
        int mw = 460;
        int mh = 130;

        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(0, 0, largura, altura);

        g.setColor(new Color(24, 18, 26, 245));
        g.fillRoundRect(cx - mw / 2, cy - mh / 2, mw, mh, 16, 16);
        g.setColor(new Color(255, 210, 80));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(cx - mw / 2, cy - mh / 2, mw, mh, 16, 16);
        g.setStroke(new BasicStroke(1.0f));

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(255, 232, 140));
        String t1 = "Abandonar a partida atual para trocar de modo?";
        FontMetrics fm1 = g.getFontMetrics();
        g.drawString(t1, cx - fm1.stringWidth(t1) / 2, cy - 26);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(225, 225, 235));
        String t2 = "O placar atual (" + match.getPontosEquipeJogador() + " × "
                + match.getPontosEquipeAdversaria() + ") será perdido.";
        FontMetrics fm2 = g.getFontMetrics();
        g.drawString(t2, cx - fm2.stringWidth(t2) / 2, cy - 4);

        btnConfirmarTroca.setBounds(cx - 165, cy + 16, 150, 36);
        desenharBotao(g, btnConfirmarTroca, "Confirmar (S)", new Color(160, 42, 42), true);

        btnCancelarTroca.setBounds(cx + 15, cy + 16, 150, 36);
        desenharBotao(g, btnCancelarTroca, "Cancelar (N)", new Color(46, 86, 130), true);
    }

    private void desenharBotaoVoltar(Graphics2D g, int altura) {
        btnVoltar.setBounds(48, altura - 66, 162, 34);
        desenharBotao(g, btnVoltar, "← Voltar ao Salão (B)", new Color(25, 32, 48), true);
    }

    private void desenharBotao(Graphics2D g, Rectangle r, String texto, Color corBase, boolean habilitado) {
        if (!habilitado) {
            g.setColor(new Color(55, 60, 72, 180));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 9, 9);
            g.setColor(new Color(95, 102, 115));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 9, 9);
            g.setColor(new Color(145, 150, 160));
        } else {
            g.setColor(corBase);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 9, 9);
            g.setColor(new Color(235, 198, 82));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 9, 9);
            g.setColor(Color.WHITE);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, r.x + (r.width - fm.stringWidth(texto)) / 2, r.y + (r.height + fm.getAscent()) / 2 - 2);
    }

    private void desenharCartaAberta(Graphics2D g, Card carta, int x, int y, int w, int h) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(175, 175, 185));
        g.drawRoundRect(x, y, w, h, 8, 8);

        boolean vermelho = (carta.getSuit() == Suit.COPAS || carta.getSuit() == Suit.OUROS);
        g.setColor(vermelho ? new Color(195, 22, 28) : new Color(24, 24, 30));

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString(carta.getRank().getSimbolo(), x + 6, y + 17);

        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        String naipe = carta.getSuit().getSimbolo();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(naipe, x + (w - fm.stringWidth(naipe)) / 2, y + h / 2 + 9);
    }

    private void desenharVersoCarta(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(22, 44, 88));
        g.fillRoundRect(x, y, w, h, 6, 6);
        g.setColor(new Color(218, 178, 64));
        g.drawRoundRect(x, y, w, h, 6, 6);
    }

    // =========================================================================
    // CLIQUES E ATALHOS
    // =========================================================================

    public boolean processarClique(int x, int y) {
        if (btnVoltar.contains(x, y)) {
            return true;
        }
        if (screenSaver != null && exibindoSelecaoModo) {
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

        if (confirmandoTrocaModo) {
            if (btnConfirmarTroca.contains(x, y)) {
                confirmarTrocaDeModo();
            } else if (btnCancelarTroca.contains(x, y)) {
                cancelarTrocaDeModo();
            }
            return false;
        }

        if (exibindoSelecaoModo || match == null) {
            if (btnModoIndividual.contains(x, y)) {
                iniciarPartidaNoModo(TrucoMode.INDIVIDUAL);
            } else if (btnModoDuplas.contains(x, y)) {
                iniciarPartidaNoModo(TrucoMode.DUPLAS);
            }
            return false;
        }

        if (btnTrocarModo.contains(x, y)) {
            solicitarTrocaDeModo();
            return false;
        }

        if (match.isMaoFinalizada()) {
            if (btnNovaMao.contains(x, y)) {
                acionarNovaMaoOuPartida();
            }
            return false;
        }

        if (btnPedirTruco.contains(x, y)) {
            executarAcaoAposta(TrucoBetAction.PEDIR_TRUCO);
            return false;
        }
        if (btnAceitar.contains(x, y)) {
            executarAcaoAposta(TrucoBetAction.ACEITAR);
            return false;
        }
        if (btnCorrer.contains(x, y)) {
            executarAcaoAposta(TrucoBetAction.CORRER);
            return false;
        }
        if (btnAumentar.contains(x, y)) {
            executarAcaoAposta(TrucoBetAction.AUMENTAR);
            return false;
        }

        if (match.podeJogadorJogarCarta()) {
            for (int i = 0; i < boundsCartasJogador.size(); i++) {
                if (boundsCartasJogador.get(i).contains(x, y)) {
                    jogarCartaIndex(i);
                    break;
                }
            }
        }
        return false;
    }

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

        if (confirmandoTrocaModo) {
            if ("S".equalsIgnoreCase(comando) || "CONFIRMAR".equalsIgnoreCase(comando) || "ACEITAR".equalsIgnoreCase(comando)) {
                confirmarTrocaDeModo();
            } else if ("N".equalsIgnoreCase(comando) || "NOVA_MAO".equalsIgnoreCase(comando) || "CANCELAR".equalsIgnoreCase(comando)) {
                cancelarTrocaDeModo();
            }
            return;
        }

        if (exibindoSelecaoModo || match == null) {
            if ("1".equals(comando) || "FICHA_1".equalsIgnoreCase(comando) || "INDIVIDUAL".equalsIgnoreCase(comando)) {
                iniciarPartidaNoModo(TrucoMode.INDIVIDUAL);
            } else if ("2".equals(comando) || "FICHA_2".equalsIgnoreCase(comando) || "DUPLAS".equalsIgnoreCase(comando)) {
                iniciarPartidaNoModo(TrucoMode.DUPLAS);
            }
            return;
        }

        if ("TROCAR_MODO".equalsIgnoreCase(comando) || "M".equalsIgnoreCase(comando)) {
            solicitarTrocaDeModo();
            return;
        }

        if (match.isMaoFinalizada()) {
            if ("NOVA_MAO".equalsIgnoreCase(comando) || "N".equalsIgnoreCase(comando)) {
                acionarNovaMaoOuPartida();
            }
            return;
        }

        if ("TRUCO".equalsIgnoreCase(comando) || "T".equalsIgnoreCase(comando)) {
            executarAcaoAposta(TrucoBetAction.PEDIR_TRUCO);
        } else if ("ACEITAR".equalsIgnoreCase(comando) || "A".equalsIgnoreCase(comando)) {
            executarAcaoAposta(TrucoBetAction.ACEITAR);
        } else if ("CORRER".equalsIgnoreCase(comando) || "C".equalsIgnoreCase(comando) || "CHECK".equalsIgnoreCase(comando)) {
            executarAcaoAposta(TrucoBetAction.CORRER);
        } else if ("AUMENTAR".equalsIgnoreCase(comando) || "U".equalsIgnoreCase(comando) || "RAISE".equalsIgnoreCase(comando)) {
            executarAcaoAposta(TrucoBetAction.AUMENTAR);
        } else if ("1".equals(comando) || "FICHA_1".equalsIgnoreCase(comando)) {
            jogarCartaIndex(0);
        } else if ("2".equals(comando) || "FICHA_2".equalsIgnoreCase(comando)) {
            jogarCartaIndex(1);
        } else if ("3".equals(comando) || "FICHA_3".equalsIgnoreCase(comando)) {
            jogarCartaIndex(2);
        }
    }

    public boolean jogarCartaIndex(int index) {
        if (match == null || !match.podeJogadorJogarCarta()) {
            return false;
        }
        boolean jogou = match.jogarCartaJogadorPorIndice(index);
        if (jogou) {
            if (screenSaver != null && screenSaver.getPlayer() != null) {
                screenSaver.getPlayer().playSound();
            }
            sincronizarTemporizadorAposMudancaEstado();
            solicitarRepintura();
        }
        return jogou;
    }

    public boolean executarAcaoAposta(TrucoBetAction acao) {
        if (match == null || acao == null) {
            return false;
        }
        boolean ok = match.responderTrucoJogador(acao);
        if (ok) {
            sincronizarTemporizadorAposMudancaEstado();
            solicitarRepintura();
        }
        return ok;
    }

    public void solicitarTrocaDeModo() {
        if (match != null && !match.isPartidaFinalizada() && match.hasProgressoNaPartida()) {
            this.confirmandoTrocaModo = true;
        } else {
            this.exibindoSelecaoModo = true;
            this.confirmandoTrocaModo = false;
        }
        solicitarRepintura();
    }

    public void confirmarTrocaDeModo() {
        this.confirmandoTrocaModo = false;
        this.exibindoSelecaoModo = true;
        this.match = null;
        sincronizarTemporizadorAposMudancaEstado();
        solicitarRepintura();
    }

    public void cancelarTrocaDeModo() {
        this.confirmandoTrocaModo = false;
        solicitarRepintura();
    }

    private void acionarNovaMaoOuPartida() {
        if (match == null) {
            return;
        }
        if (match.isPartidaFinalizada()) {
            iniciarPartidaNoModo(match.getModo());
        } else {
            match.iniciarNovaMao();
            sincronizarTemporizadorAposMudancaEstado();
            solicitarRepintura();
        }
    }

    private void solicitarRepintura() {
        if (screenSaver != null) {
            screenSaver.repaint();
        }
    }

    // Getters para testes
    public TrucoMatch getMatch() {
        return match;
    }

    public boolean isExibindoSelecaoModo() {
        return exibindoSelecaoModo;
    }

    public boolean isConfirmandoTrocaModo() {
        return confirmandoTrocaModo;
    }

    public double getCronometroPausaSegundos() {
        return cronometroPausaSegundos;
    }

    public String getTipoPausaAtiva() {
        return tipoPausaAtiva;
    }

    public List<Rectangle> getBoundsCartasJogador() {
        return Collections.unmodifiableList(boundsCartasJogador);
    }

    public Rectangle getBtnModoIndividual() {
        return new Rectangle(btnModoIndividual);
    }

    public Rectangle getBtnModoDuplas() {
        return new Rectangle(btnModoDuplas);
    }

    public Rectangle getBtnVoltar() {
        return new Rectangle(btnVoltar);
    }
}
