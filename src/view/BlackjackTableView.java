package view;

import model.BlackjackEngine;
import model.Card;
import model.RoundResult;
import model.RoundState;
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
import java.util.List;
import java.util.Locale;

/**
 * Interface gráfica completa e interativa da Mesa de Blackjack.
 *
 * <p>Consome estritamente o contrato {@link BlackjackEngine}.
 * Desenha o feltro verde, a área da banca (com suporte à carta oculta),
 * a área do jogador, o painel de apostas em fichas virtuais (long),
 * botões interativos e os banners de resultado em português.</p>
 */
public class BlackjackTableView {

    private final BlackjackEngine engine;
    private final NumberFormat formatadorMoeda = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));

    // Valor da aposta sendo montada pelo jogador antes de iniciar
    private long apostaPreparada = 100L; // Padrão par

    // Áreas retangulares para detecção de cliques do mouse
    private final Rectangle btnApostar = new Rectangle();
    private final Rectangle btnPedirCarta = new Rectangle();
    private final Rectangle btnParar = new Rectangle();
    private final Rectangle btnVoltar = new Rectangle();
    private final Rectangle btnFicha50 = new Rectangle();
    private final Rectangle btnFicha100 = new Rectangle();
    private final Rectangle btnFicha500 = new Rectangle();
    private final Rectangle btnFicha1000 = new Rectangle();
    private final Rectangle btnLimparAposta = new Rectangle();

    // Feedback visual temporário
    private String mensagemAlerta = "";
    private long tempoAlerta = 0;

    public BlackjackTableView(BlackjackEngine engine) {
        this.engine = engine;
    }

    /**
     * Renderiza todos os elementos visuais da mesa de Blackjack.
     */
    public void render(Graphics2D g, int largura, int altura) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Fundo da sala do cassino (tons escuros e luxuosos)
        g.setColor(new Color(15, 20, 26));
        g.fillRect(0, 0, largura, altura);

        // 2. Tampo da Mesa semicircular (feltro verde clássico com moldura de mogno)
        desenharMesaFeltro(g, largura, altura);

        // 3. Cabeçalho: Título e Carteira de Fichas Virtuais (long)
        desenharCabecalho(g, largura);

        // 4. Área da Banca / Dealer (Topo)
        desenharAreaDealer(g, largura, altura);

        // 5. Área Central: Placar de Aposta e Banner de Resultado
        desenharAreaCentral(g, largura, altura);

        // 6. Área do Jogador (Base)
        desenharAreaJogador(g, largura, altura);

        // 7. Painel de Controles e Botões Interativos
        desenharControles(g, largura, altura);

        // 8. Mensagem de alerta temporária (ex.: aposta ímpar ou saldo insuficiente)
        desenharAlerta(g, largura, altura);

        // 9. Indicação discreta de modo de demonstração (quando o mock visual estiver ativo)
        if (engine instanceof BlackjackMockSimulator) {
            desenharIndicacaoDemonstracao(g, largura, altura);
        }
    }

    private void desenharMesaFeltro(Graphics2D g, int largura, int altura) {
        int margemX = 60;
        int margemY = 70;
        int mesaW = largura - (margemX * 2);
        int mesaH = altura - (margemY * 2);

        // Borda de madeira / mogno
        g.setColor(new Color(60, 25, 15));
        g.fillRoundRect(margemX - 12, margemY - 12, mesaW + 24, mesaH + 24, 60, 60);
        g.setColor(new Color(212, 175, 55)); // Friso dourado
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(margemX - 12, margemY - 12, mesaW + 24, mesaH + 24, 60, 60);

        // Feltro verde com gradiente radial/vertical
        GradientPaint feltro = new GradientPaint(
                largura / 2.0f, margemY, new Color(16, 95, 52),
                largura / 2.0f, margemY + mesaH, new Color(9, 58, 32)
        );
        g.setPaint(feltro);
        g.fillRoundRect(margemX, margemY, mesaW, mesaH, 50, 50);

        // Linha interna dourada do feltro
        g.setColor(new Color(212, 175, 55, 90));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8, 8}, 0));
        g.drawRoundRect(margemX + 25, margemY + 25, mesaW - 50, mesaH - 50, 40, 40);
        g.setStroke(new BasicStroke(1));
    }

    private void desenharCabecalho(Graphics2D g, int largura) {
        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("MESA DE BLACKJACK — CASSINO SOCIAL", 80, 45);

        // Painel do Saldo Virtual
        String saldoTexto = "FICHAS: " + formatadorMoeda.format(engine.getSaldoVirtual()) + " ✪";
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        int saldoW = fm.stringWidth(saldoTexto);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(largura - saldoW - 120, 20, saldoW + 40, 38, 12, 12);
        g.setColor(new Color(255, 215, 0));
        g.drawRoundRect(largura - saldoW - 120, 20, saldoW + 40, 38, 12, 12);
        g.drawString(saldoTexto, largura - saldoW - 100, 46);
    }

    private void desenharAreaDealer(Graphics2D g, int largura, int altura) {
        int centroX = largura / 2;
        int dealerY = altura / 2 - 190;

        String textoTitulo = "BANCA (DEALER)";
        if (engine.getEstadoRodada() != RoundState.AGUARDANDO_APOSTA) {
            textoTitulo += " — Pontos visíveis: " + engine.getPontosDealerVisiveis();
        }

        g.setColor(new Color(220, 220, 220));
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(textoTitulo, centroX - (fm.stringWidth(textoTitulo) / 2), dealerY - 15);

        List<Card> visiveis = engine.getCartasVisiveisDealer();
        boolean temOculta = engine.hasCartaOcultaDealer();

        int totalCartas = visiveis.size() + (temOculta ? 1 : 0);
        int cardW = 80;
        int cardH = 115;
        int espaco = 18;
        int inicioX = centroX - ((totalCartas * (cardW + espaco) - espaco) / 2);

        if (totalCartas == 0) {
            // Desenha silhueta vazia indicando local das cartas
            desenharSlotVazio(g, centroX - cardW - 10, dealerY, cardW, cardH, "Carta 1");
            desenharSlotVazio(g, centroX + 10, dealerY, cardW, cardH, "Carta 2");
            return;
        }

        int x = inicioX;
        for (Card carta : visiveis) {
            desenharCartaAberta(g, carta, x, dealerY, cardW, cardH);
            x += cardW + espaco;
        }

        if (temOculta) {
            desenharVersoCarta(g, x, dealerY, cardW, cardH);
        }
    }

    private void desenharAreaCentral(Graphics2D g, int largura, int altura) {
        int centroX = largura / 2;
        int centroY = altura / 2 - 35;

        // Placar de aposta da rodada
        long apostaExibida = (engine.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA) ?
                apostaPreparada : engine.getApostaAtual();

        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(centroX - 160, centroY - 22, 320, 44, 20, 20);
        g.setColor(new Color(212, 175, 55));
        g.drawRoundRect(centroX - 160, centroY - 22, 320, 44, 20, 20);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String textoAposta = "APOSTA DA RODADA: " + formatadorMoeda.format(apostaExibida) + " ✪";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(textoAposta, centroX - (fm.stringWidth(textoAposta) / 2), centroY + 6);

        // Banner de resultado quando a rodada estiver FINALIZADA
        if (engine.getEstadoRodada() == RoundState.FINALIZADA) {
            desenharBannerResultado(g, largura, centroY + 45);
        }
    }

    private void desenharBannerResultado(Graphics2D g, int largura, int bannerY) {
        int centroX = largura / 2;
        RoundResult res = engine.getResultadoRodada();

        Color corFundo = new Color(0, 0, 0, 210);
        Color corBorda = Color.WHITE;
        String texto = res.getDescricaoPadrao();

        switch (res) {
            case JOGADOR_BLACKJACK:
                corBorda = new Color(255, 215, 0);
                texto = "★ BLACKJACK NATURAL! PAGAMENTO 3:2 (+ " +
                        formatadorMoeda.format((engine.getApostaAtual() * 3) / 2) + " FICHAS) ★";
                break;
            case JOGADOR_VITORIA:
                corBorda = new Color(46, 204, 113);
                texto = "✔ VITÓRIA DO JOGADOR! (+ " + formatadorMoeda.format(engine.getApostaAtual()) + " FICHAS)";
                break;
            case EMPATE_PUSH:
                corBorda = new Color(241, 196, 15);
                texto = "▬ EMPATE (PUSH)! TODAS AS FICHAS FORAM DEVOLVIDAS";
                break;
            case JOGADOR_ESTOUROU:
                corBorda = new Color(231, 76, 60);
                texto = "✖ ESTOUROU (BUST)! MAIS DE 21 PONTOS";
                break;
            case DEALER_VITORIA:
            case DEALER_BLACKJACK:
                corBorda = new Color(231, 76, 60);
                texto = "✖ A BANCA VENCEU A RODADA";
                break;
            default:
                break;
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        FontMetrics fm = g.getFontMetrics();
        int bannerW = fm.stringWidth(texto) + 60;
        int bannerH = 40;

        g.setColor(corFundo);
        g.fillRoundRect(centroX - (bannerW / 2), bannerY - 24, bannerW, bannerH, 16, 16);
        g.setColor(corBorda);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(centroX - (bannerW / 2), bannerY - 24, bannerW, bannerH, 16, 16);
        g.drawString(texto, centroX - (fm.stringWidth(texto) / 2), bannerY + 2);
        g.setStroke(new BasicStroke(1));
    }

    private void desenharAreaJogador(Graphics2D g, int largura, int altura) {
        int centroX = largura / 2;
        int jogadorY = altura / 2 + 75;

        String textoTitulo = "SUA MÃO";
        if (engine.getEstadoRodada() != RoundState.AGUARDANDO_APOSTA) {
            textoTitulo += " — Pontos: " + engine.getPontosJogador();
        }

        g.setColor(new Color(220, 220, 220));
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(textoTitulo, centroX - (fm.stringWidth(textoTitulo) / 2), jogadorY - 15);

        List<Card> mao = engine.getMaoJogador();
        int cardW = 80;
        int cardH = 115;
        int espaco = 18;

        if (mao.isEmpty()) {
            desenharSlotVazio(g, centroX - cardW - 10, jogadorY, cardW, cardH, "Carta 1");
            desenharSlotVazio(g, centroX + 10, jogadorY, cardW, cardH, "Carta 2");
            return;
        }

        int inicioX = centroX - ((mao.size() * (cardW + espaco) - espaco) / 2);
        int x = inicioX;
        for (Card carta : mao) {
            desenharCartaAberta(g, carta, x, jogadorY, cardW, cardH);
            x += cardW + espaco;
        }
    }

    private void desenharSlotVazio(Graphics2D g, int x, int y, int w, int h, String rotulo) {
        g.setColor(new Color(255, 255, 255, 25));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.setStroke(new BasicStroke(1));

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(rotulo, x + (w - fm.stringWidth(rotulo)) / 2, y + h / 2 + 4);
    }

    private void desenharCartaAberta(Graphics2D g, Card carta, int x, int y, int w, int h) {
        // Sombra da carta
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRoundRect(x + 4, y + 4, w, h, 10, 10);

        // Corpo branco com cantos arredondados
        g.setColor(new Color(250, 250, 252));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(180, 180, 190));
        g.drawRoundRect(x, y, w, h, 10, 10);

        Color corNaipe = (carta.getSuit() == Suit.COPAS || carta.getSuit() == Suit.OUROS) ?
                new Color(205, 30, 30) : new Color(25, 25, 30);
        g.setColor(corNaipe);

        // Canto superior esquerdo: símbolo do rank e naipe
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString(carta.getRank().getSimbolo(), x + 7, y + 20);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString(carta.getSuit().getSimbolo(), x + 7, y + 36);

        // Centro da carta: naipe grande
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        FontMetrics fmCentro = g.getFontMetrics();
        String simCentro = carta.getSuit().getSimbolo();
        g.drawString(simCentro, x + (w - fmCentro.stringWidth(simCentro)) / 2, y + (h / 2) + 12);

        // Canto inferior direito invertido
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        String simRank = carta.getRank().getSimbolo();
        FontMetrics fmR = g.getFontMetrics();
        g.drawString(simRank, x + w - fmR.stringWidth(simRank) - 7, y + h - 8);
    }

    private void desenharVersoCarta(Graphics2D g, int x, int y, int w, int h) {
        // Sombra
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRoundRect(x + 4, y + 4, w, h, 10, 10);

        // Borda dourada externa
        g.setColor(new Color(212, 175, 55));
        g.fillRoundRect(x, y, w, h, 10, 10);

        // Fundo azul marinho luxuoso
        g.setColor(new Color(20, 35, 75));
        g.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 8, 8);

        // Padrão geométrico xadrez/losango estilizado
        g.setColor(new Color(180, 40, 40));
        g.setStroke(new BasicStroke(1.2f));
        for (int lx = x + 10; lx < x + w - 10; lx += 12) {
            for (int ly = y + 10; ly < y + h - 10; ly += 14) {
                g.drawOval(lx, ly, 6, 6);
            }
        }
        g.setStroke(new BasicStroke(1));

        // Brasão central dourado
        g.setColor(new Color(212, 175, 55));
        g.fillOval(x + (w / 2) - 15, y + (h / 2) - 15, 30, 30);
        g.setColor(new Color(20, 35, 75));
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("♠", x + (w / 2) - 6, y + (h / 2) + 6);
    }

    private void desenharControles(Graphics2D g, int largura, int altura) {
        int centroX = largura / 2;
        int barraY = altura - 110;

        RoundState estado = engine.getEstadoRodada();

        // 1. Painel de Seleção de Apostas (ativo em AGUARDANDO_APOSTA ou FINALIZADA)
        if (estado == RoundState.AGUARDANDO_APOSTA || estado == RoundState.FINALIZADA) {
            int fichaW = 85;
            int fichaH = 34;
            int xFichas = centroX - 250;

            btnFicha50.setBounds(xFichas, barraY - 45, fichaW, fichaH);
            desenharBotaoFicha(g, btnFicha50, "+50", new Color(41, 128, 185));

            btnFicha100.setBounds(xFichas + 95, barraY - 45, fichaW, fichaH);
            desenharBotaoFicha(g, btnFicha100, "+100", new Color(39, 174, 96));

            btnFicha500.setBounds(xFichas + 190, barraY - 45, fichaW, fichaH);
            desenharBotaoFicha(g, btnFicha500, "+500", new Color(142, 68, 173));

            btnFicha1000.setBounds(xFichas + 285, barraY - 45, fichaW, fichaH);
            desenharBotaoFicha(g, btnFicha1000, "+1.000", new Color(211, 84, 0));

            btnLimparAposta.setBounds(xFichas + 380, barraY - 45, fichaW, fichaH);
            desenharBotaoFicha(g, btnLimparAposta, "Limpar", new Color(192, 57, 43));

            // Botão Apostar / Nova Rodada
            int btnW = 280;
            int btnH = 46;
            btnApostar.setBounds(centroX - (btnW / 2), barraY, btnW, btnH);
            String textoApostar = (estado == RoundState.FINALIZADA) ?
                    "NOVA RODADA (ESPAÇO)" : "APOSTAR E JOGAR (ESPAÇO)";
            desenharBotaoPrincipal(g, btnApostar, textoApostar, new Color(46, 204, 113), new Color(39, 174, 96));

        } else if (estado == RoundState.TURNO_JOGADOR) {
            // 2. Ações do Turno do Jogador: Pedir Carta (Hit) e Parar (Stand)
            int btnW = 200;
            int btnH = 46;

            btnPedirCarta.setBounds(centroX - btnW - 15, barraY, btnW, btnH);
            desenharBotaoPrincipal(g, btnPedirCarta, "PEDIR CARTA (H)", new Color(52, 152, 219), new Color(41, 128, 185));

            btnParar.setBounds(centroX + 15, barraY, btnW, btnH);
            desenharBotaoPrincipal(g, btnParar, "PARAR (S)", new Color(230, 126, 34), new Color(211, 84, 0));
        }

        // Botão de Voltar ao Salão (sempre visível no canto inferior)
        btnVoltar.setBounds(largura - 240, altura - 55, 200, 36);
        desenharBotaoSecundario(g, btnVoltar, "VOLTAR AO SALÃO (B)");
    }

    private void desenharIndicacaoDemonstracao(Graphics2D g, int largura, int altura) {
        String texto = "Modo de demonstração";
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(texto);

        int x = 70;
        int y = altura - 25;

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(x - 8, y - 14, tw + 16, 20, 6, 6);
        g.setColor(new Color(255, 215, 0, 200));
        g.drawString(texto, x, y);
    }

    private void desenharBotaoFicha(Graphics2D g, Rectangle r, String texto, Color corFundo) {
        g.setColor(corFundo);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g.setColor(new Color(255, 255, 255, 80));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, r.x + (r.width - fm.stringWidth(texto)) / 2, r.y + 22);
    }

    private void desenharBotaoPrincipal(Graphics2D g, Rectangle r, String texto, Color cor1, Color cor2) {
        g.setPaint(new GradientPaint(r.x, r.y, cor1, r.x, r.y + r.height, cor2));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g.setColor(new Color(255, 255, 255, 120));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, r.x + (r.width - fm.stringWidth(texto)) / 2, r.y + 28);
    }

    private void desenharBotaoSecundario(Graphics2D g, Rectangle r, String texto) {
        g.setColor(new Color(40, 45, 55));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setColor(new Color(200, 200, 210, 90));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

        g.setColor(new Color(230, 230, 240));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, r.x + (r.width - fm.stringWidth(texto)) / 2, r.y + 23);
    }

    private void desenharAlerta(Graphics2D g, int largura, int altura) {
        if (mensagemAlerta != null && !mensagemAlerta.isEmpty() && System.currentTimeMillis() < tempoAlerta) {
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(mensagemAlerta) + 40;
            int h = 32;
            int x = (largura - w) / 2;
            int y = altura - 155;

            g.setColor(new Color(231, 76, 60, 230));
            g.fillRoundRect(x, y, w, h, 10, 10);
            g.setColor(Color.WHITE);
            g.drawString(mensagemAlerta, x + 20, y + 21);
        }
    }

    /**
     * Processa cliques do mouse nas áreas de botões da interface.
     *
     * @return true se o clique solicitou saída para o salão (botão Voltar).
     */
    public boolean processarClique(int mx, int my) {
        if (btnVoltar.contains(mx, my)) {
            return true;
        }

        RoundState estado = engine.getEstadoRodada();

        if (estado == RoundState.AGUARDANDO_APOSTA || estado == RoundState.FINALIZADA) {
            if (btnFicha50.contains(mx, my)) {
                adicionarAposta(50L);
            } else if (btnFicha100.contains(mx, my)) {
                adicionarAposta(100L);
            } else if (btnFicha500.contains(mx, my)) {
                adicionarAposta(500L);
            } else if (btnFicha1000.contains(mx, my)) {
                adicionarAposta(1000L);
            } else if (btnLimparAposta.contains(mx, my)) {
                apostaPreparada = 20L; // Reinicia para aposta par mínima permitida
            } else if (btnApostar.contains(mx, my)) {
                acionarApostar();
            }
        } else if (estado == RoundState.TURNO_JOGADOR) {
            if (btnPedirCarta.contains(mx, my)) {
                engine.pedirCarta();
            } else if (btnParar.contains(mx, my)) {
                engine.parar();
            }
        }
        return false;
    }

    /**
     * Trata atalhos de teclado na tela da mesa.
     */
    public boolean processarTecla(String comando) {
        if ("VOLTAR".equals(comando) || "B".equals(comando)) {
            return true;
        }

        RoundState estado = engine.getEstadoRodada();

        if (estado == RoundState.AGUARDANDO_APOSTA || estado == RoundState.FINALIZADA) {
            if ("APOSTAR".equals(comando) || "ESPACO".equals(comando)) {
                acionarApostar();
                return true;
            } else if ("FICHA_1".equals(comando)) {
                adicionarAposta(50L);
            } else if ("FICHA_2".equals(comando)) {
                adicionarAposta(100L);
            } else if ("FICHA_3".equals(comando)) {
                adicionarAposta(500L);
            }
        } else if (estado == RoundState.TURNO_JOGADOR) {
            if ("PEDIR_CARTA".equals(comando) || "HIT".equals(comando)) {
                engine.pedirCarta();
                return true;
            } else if ("PARAR".equals(comando) || "STAND".equals(comando)) {
                engine.parar();
                return true;
            }
        }
        return false;
    }

    /**
     * Incrementa o valor da aposta com verificação contra overflow de long e saldo disponível.
     */
    public void adicionarAposta(long valor) {
        if (valor <= 0) {
            exibirAlerta("Valor de ficha deve ser positivo!");
            return;
        }
        try {
            long novoValor = Math.addExact(apostaPreparada, valor);
            if (novoValor > engine.getSaldoVirtual()) {
                exibirAlerta("Saldo virtual insuficiente para aumentar a aposta!");
                return;
            }
            this.apostaPreparada = novoValor;
        } catch (ArithmeticException ex) {
            exibirAlerta("O valor da aposta excede o limite numérico permitido!");
        }
    }

    /**
     * Define diretamente a aposta validando positividade, paridade e saldo.
     */
    public boolean setApostaPreparada(long novaAposta) {
        if (novaAposta <= 0) {
            exibirAlerta("A aposta deve ser estritamente maior que zero!");
            return false;
        }
        if (novaAposta % 2 != 0) {
            exibirAlerta("O valor da aposta deve ser par para permitir pagamento 3:2!");
            return false;
        }
        if (novaAposta > engine.getSaldoVirtual()) {
            exibirAlerta("Saldo virtual insuficiente para realizar esta aposta!");
            return false;
        }
        this.apostaPreparada = novaAposta;
        return true;
    }

    private void acionarApostar() {
        if (apostaPreparada <= 0) {
            exibirAlerta("A aposta deve ser estritamente maior que zero!");
            return;
        }
        if (apostaPreparada % 2 != 0) {
            exibirAlerta("O valor da aposta deve ser par para permitir pagamento 3:2!");
            return;
        }
        if (apostaPreparada > engine.getSaldoVirtual()) {
            exibirAlerta("Saldo virtual insuficiente para realizar esta aposta!");
            return;
        }

        boolean sucesso = engine.iniciarPartida(apostaPreparada);
        if (!sucesso) {
            exibirAlerta("Não foi possível iniciar a rodada com esta aposta.");
        }
    }

    public void exibirAlerta(String msg) {
        this.mensagemAlerta = msg;
        this.tempoAlerta = System.currentTimeMillis() + 3000;
    }

    public String getMensagemAlerta() {
        return mensagemAlerta;
    }

    public long getApostaPreparada() {
        return apostaPreparada;
    }

    public BlackjackEngine getEngine() {
        return engine;
    }
}
