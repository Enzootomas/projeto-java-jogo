package view;

import model.CasinoCounter;
import model.CasinoChair;
import model.CasinoTable;
import model.IsometricHelper;
import model.MyCharacter;
import model.MyRectangle;
import model.MyShape;

import java.awt.BasicStroke;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import model.BlackjackEngine;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Painel principal do jogo (ScreenSaver).
 *
 * Responsabilidades:
 * - Gerencia o loop de atualização (~60 FPS com delta time medido).
 * - Entrada contínua por Swing Key Bindings com limpeza ao perder foco.
 * - Renderização do salão isométrico e z-sorting por profundidade.
 * - Gerencia a transição entre o Salão e a Mesa de Blackjack interativa.
 * - Demonstra polimorfismo através de List<MyShape>.
 */
public class ScreenSaver extends JPanel implements ActionListener {

    public enum GameState {
        SALAO,
        MENU_BLACKJACK
    }

    private GameState estadoAtual = GameState.SALAO;

    // Dimensões do chão lógico do salão (em pixels de mundo)
    public static final int WORLD_GRID_TILES = 16;
    public static final int WORLD_SIZE = (int) (WORLD_GRID_TILES * IsometricHelper.TILE_SIZE_WORLD); // 512x512

    // Entidades
    private MyCharacter player;
    private CasinoTable mesaBlackjack;
    private CasinoCounter balcao;
    private List<CasinoChair> cadeiras;
    private List<MyShape> obstaculos;
    private List<MyShape> entidadesRenderizaveis;

    // Interface e Motor do Blackjack
    private BlackjackEngine blackjackEngine;
    private BlackjackTableView blackjackTableView;

    // Loop e Tempo
    private Timer timer;
    private long lastTimeNano;
    private static final double VELOCIDADE_AVATAR = 180.0; // pixels por segundo na tela

    // Teclas ativas via Swing Key Bindings
    private final Set<String> teclasPressionadas = new HashSet<>();

    // Origem da projeção isométrica na tela
    private double originX;
    private double originY;

    // Mensagem de interação
    private boolean proximoDaMesa = false;

    public ScreenSaver() {
        setBackground(new Color(15, 18, 26));
        setDoubleBuffered(true);
        setFocusable(true);

        inicializarEntidades();
        configurarMesaBlackjack();
        configurarKeyBindings();
        configurarTratamentoFoco();
        configurarMouseListener();

        this.lastTimeNano = System.nanoTime();
        this.timer = new Timer(16, this); // ~60 FPS
        this.timer.start();
    }

    /**
     * Pré-carrega recursos e inicializa entidades fora do loop de renderização.
     */
    private void inicializarEntidades() {
        this.obstaculos = new ArrayList<>();
        this.entidadesRenderizaveis = new ArrayList<>();
        this.cadeiras = new ArrayList<>();

        // Cria o avatar posicionado no tapete da entrada do salão
        String spriteAvatar = "src/image/avatar.png";
        String somInteracao = "src/sound/interact.wav";
        this.player = new MyCharacter(spriteAvatar, 60, 320, 24, 24, somInteracao);

        // Cria a mesa de Blackjack com seu footprint de base sólida no chão
        String spriteMesa = "src/image/table.png";
        this.mesaBlackjack = new CasinoTable(160, 160, 80, 50, spriteMesa, "Blackjack Solo", 0, 0);

        // Cria o balcão do bar com footprint sólido na lateral norte
        this.balcao = new CasinoCounter(110, 40, 80, 30, "BAR", 0, 0);

        // Cria cadeiras de cassino com bases sólidas
        CasinoChair c1 = new CasinoChair(140, 85, 20, 0, 0);  // Perto do balcão
        CasinoChair c2 = new CasinoChair(310, 140, 20, 0, 0); // Área de descanso lateral
        CasinoChair c3 = new CasinoChair(310, 180, 20, 0, 0); // Área de descanso lateral

        this.cadeiras.add(c1);
        this.cadeiras.add(c2);
        this.cadeiras.add(c3);

        // Adiciona à lista de obstáculos para colisão física no solo
        this.obstaculos.add(this.mesaBlackjack);
        this.obstaculos.add(this.balcao);
        for (CasinoChair c : this.cadeiras) {
            this.obstaculos.add(c);
        }

        // Adiciona à lista heterogênea polimórfica de renderização com Z-sorting
        this.entidadesRenderizaveis.add(this.mesaBlackjack);
        this.entidadesRenderizaveis.add(this.balcao);
        for (CasinoChair c : this.cadeiras) {
            this.entidadesRenderizaveis.add(c);
        }
        this.entidadesRenderizaveis.add(this.player);
    }

    /**
     * Inicializa a interface da mesa de Blackjack acoplada ao simulador/mock visual.
     * O motor oficial de regras será fornecido pelo colega na classe BlackjackGame.
     */
    private void configurarMesaBlackjack() {
        this.blackjackEngine = new BlackjackMockSimulator();
        this.blackjackTableView = new BlackjackTableView(this.blackjackEngine);
    }

    private void configurarMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                processarCliqueMouse(e.getX(), e.getY());
            }
        });
    }

    /**
     * Processa cliques do mouse na tela.
     */
    public void processarCliqueMouse(int x, int y) {
        if (estadoAtual == GameState.MENU_BLACKJACK && blackjackTableView != null) {
            boolean sairParaSalao = blackjackTableView.processarClique(x, y);
            if (sairParaSalao) {
                player.playSound();
                estadoAtual = GameState.SALAO;
                teclasPressionadas.clear();
            }
        }
    }

    /**
     * Configura Swing Key Bindings com WHEN_IN_FOCUSED_WINDOW.
     */
    private void configurarKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // Mapeamento individual de cada tecla física direcional (Pressionar e Soltar)
        vincularTeclaFisica(im, am, "W", "W");
        vincularTeclaFisica(im, am, "UP", "UP");
        vincularTeclaFisica(im, am, "A", "A");
        vincularTeclaFisica(im, am, "LEFT", "LEFT");
        vincularTeclaFisica(im, am, "D", "D");
        vincularTeclaFisica(im, am, "RIGHT", "RIGHT");
        vincularTeclaFisica(im, am, "DOWN", "DOWN");

        // Tecla S: no salao move para baixo; na mesa de blackjack aciona PARAR (Stand)
        im.put(KeyStroke.getKeyStroke("pressed S"), "press_S");
        am.put("press_S", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoAtual == GameState.SALAO) {
                    teclasPressionadas.add("S");
                } else if (estadoAtual == GameState.MENU_BLACKJACK && blackjackTableView != null) {
                    blackjackTableView.processarTecla("STAND");
                }
            }
        });
        im.put(KeyStroke.getKeyStroke("released S"), "rel_S");
        am.put("rel_S", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                teclasPressionadas.remove("S");
            }
        });

        // Tecla H: Pedir Carta (Hit) na mesa
        im.put(KeyStroke.getKeyStroke("pressed H"), "press_H");
        am.put("press_H", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoAtual == GameState.MENU_BLACKJACK && blackjackTableView != null) {
                    blackjackTableView.processarTecla("HIT");
                }
            }
        });

        // Tecla ESPACO: Apostar e Jogar na mesa
        im.put(KeyStroke.getKeyStroke("pressed SPACE"), "press_SPACE");
        am.put("press_SPACE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoAtual == GameState.MENU_BLACKJACK && blackjackTableView != null) {
                    blackjackTableView.processarTecla("ESPACO");
                }
            }
        });

        // Teclas numericas para selecao de fichas na mesa
        configurarTeclaAtalhoMesa(im, am, "1", "FICHA_1");
        configurarTeclaAtalhoMesa(im, am, "2", "FICHA_2");
        configurarTeclaAtalhoMesa(im, am, "3", "FICHA_3");

        // Tecla B para retornar ao salao a partir da mesa de Blackjack
        im.put(KeyStroke.getKeyStroke("pressed B"), "acaoB");
        am.put("acaoB", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoAtual == GameState.MENU_BLACKJACK) {
                    player.playSound();
                    estadoAtual = GameState.SALAO;
                    teclasPressionadas.clear();
                }
            }
        });

        // Tecla E para alternar interacao com a mesa
        im.put(KeyStroke.getKeyStroke("pressed E"), "acaoE");
        am.put("acaoE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processarInteracaoE();
            }
        });

        // Tecla ESC para encerramento imediato em qualquer estado
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "acaoEsc");
        am.put("acaoEsc", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encerrarJogo();
            }
        });
    }

    private void configurarTeclaAtalhoMesa(InputMap im, ActionMap am, String tecla, String comando) {
        im.put(KeyStroke.getKeyStroke("pressed " + tecla), "press_" + comando);
        am.put("press_" + comando, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoAtual == GameState.MENU_BLACKJACK && blackjackTableView != null) {
                    blackjackTableView.processarTecla(comando);
                }
            }
        });
    }

    private void vincularTeclaFisica(InputMap im, ActionMap am, String tecla, String codigoFisico) {
        im.put(KeyStroke.getKeyStroke("pressed " + tecla), "press_" + codigoFisico);
        am.put("press_" + codigoFisico, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoAtual == GameState.SALAO) {
                    teclasPressionadas.add(codigoFisico);
                }
            }
        });

        im.put(KeyStroke.getKeyStroke("released " + tecla), "rel_" + codigoFisico);
        am.put("rel_" + codigoFisico, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                teclasPressionadas.remove(codigoFisico);
            }
        });
    }

    /**
     * Limpa as teclas ativas quando a janela perde o foco.
     */
    private void configurarTratamentoFoco() {
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                teclasPressionadas.clear();
            }
        });
    }

    private void processarInteracaoE() {
        if (estadoAtual == GameState.SALAO) {
            if (proximoDaMesa) {
                player.playSound();
                estadoAtual = GameState.MENU_BLACKJACK;
                teclasPressionadas.clear();
            }
        } else if (estadoAtual == GameState.MENU_BLACKJACK) {
            player.playSound();
            estadoAtual = GameState.SALAO;
            teclasPressionadas.clear();
        }
    }

    /**
     * Para o timer e libera recursos de áudio sem encerrar a JVM (ideal para testes).
     */
    public void pararLoop() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        if (player != null) {
            player.dispose();
        }
    }

    /**
     * Encerra a aplicação de forma limpa, liberando recursos de áudio.
     */
    public void encerrarJogo() {
        pararLoop();
        System.exit(0);
    }

    /**
     * Loop principal acionado pelo Timer (~60 FPS).
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastTimeNano) / 1_000_000_000.0;
        this.lastTimeNano = now;

        // Limita saltos após pausas longas ou redimensionamentos
        dt = Math.min(dt, 0.05);

        atualizarLogica(dt);
        repaint();
    }

    private void atualizarLogica(double dt) {
        // Atualiza a origem da projeção para manter o salão centralizado na tela
        Dimension tam = getSize();
        this.originX = tam.width / 2.0;
        this.originY = Math.max(120, tam.height * 0.25);

        player.updateOrigins(originX, originY);
        mesaBlackjack.updateOrigins(originX, originY);
        if (balcao != null) {
            balcao.updateOrigins(originX, originY);
        }
        if (cadeiras != null) {
            for (CasinoChair c : cadeiras) {
                c.updateOrigins(originX, originY);
            }
        }

        if (estadoAtual == GameState.SALAO) {
            // Calcula direção visual desejada na tela a partir das teclas físicas ativas
            boolean moveUp = teclasPressionadas.contains("W") || teclasPressionadas.contains("UP");
            boolean moveDown = teclasPressionadas.contains("S") || teclasPressionadas.contains("DOWN");
            boolean moveLeft = teclasPressionadas.contains("A") || teclasPressionadas.contains("LEFT");
            boolean moveRight = teclasPressionadas.contains("D") || teclasPressionadas.contains("RIGHT");

            double dirSx = 0.0;
            double dirSy = 0.0;
            if (moveUp) dirSy -= 1.0;
            if (moveDown) dirSy += 1.0;
            if (moveLeft) dirSx -= 1.0;
            if (moveRight) dirSx += 1.0;

            // Movimentação com vetor normalizado, velocidade constante e colisão com deslizamento
            player.moveIsometric(dirSx, dirSy, VELOCIDADE_AVATAR, dt, WORLD_SIZE, WORLD_SIZE, obstaculos);

            // Verifica proximidade da mesa para interação
            this.proximoDaMesa = mesaBlackjack.isPlayerNear(player, 35);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        if (estadoAtual == GameState.SALAO) {
            desenharSalao(g2d);
        } else {
            desenharMenuBlackjack(g2d);
        }

        g2d.dispose();
    }

    /**
     * Renderiza o salão isométrico, ordenando as entidades por profundidade (Z-Sorting).
     */
    private void desenharSalao(Graphics2D g2d) {
        desenharPisoIsometrico(g2d);
        desenharParedesIsometricas(g2d);
        desenharPortalEntrada(g2d);
        desenharTapeteIsometrico(g2d);

        // Z-Sorting das entidades pelo ponto de apoio no solo
        entidadesRenderizaveis.sort(Comparator.comparingDouble(s -> {
            if (s instanceof MyCharacter) {
                return ((MyCharacter) s).getDepthZ();
            } else if (s instanceof CasinoTable) {
                return ((CasinoTable) s).getDepthZ();
            } else if (s instanceof CasinoCounter) {
                return ((CasinoCounter) s).getDepthZ();
            } else if (s instanceof CasinoChair) {
                return ((CasinoChair) s).getDepthZ();
            }
            return (double) s.getY();
        }));

        // Renderização polimórfica de List<MyShape>
        for (MyShape entidade : entidadesRenderizaveis) {
            entidade.draw(g2d);
        }

        desenharHUD(g2d);

        if (proximoDaMesa) {
            desenharBalcaoInteracao(g2d);
        }
    }

    /**
     * Desenha as duas paredes isométricas de fundo (Noroeste e Nordeste) com molduras.
     */
    private void desenharParedesIsometricas(Graphics2D g2d) {
        int alturaParede = 135;

        // Vértices do chão no fundo
        int xCentro = (int) Math.round(originX);
        int yCentro = (int) Math.round(originY);

        int xExtremoEsq = (int) Math.round(IsometricHelper.toScreenX(0, WORLD_SIZE, originX));
        int yExtremoEsq = (int) Math.round(IsometricHelper.toScreenY(0, WORLD_SIZE, originY));

        int xExtremoDir = (int) Math.round(IsometricHelper.toScreenX(WORLD_SIZE, 0, originX));
        int yExtremoDir = (int) Math.round(IsometricHelper.toScreenY(WORLD_SIZE, 0, originY));

        // 1. Parede Esquerda (Nordeste de mundo, ao longo de x = 0)
        int[] paredeEsqX = {xCentro, xExtremoEsq, xExtremoEsq, xCentro};
        int[] paredeEsqY = {yCentro, yExtremoEsq, yExtremoEsq - alturaParede, yCentro - alturaParede};
        g2d.setColor(new Color(22, 28, 44));
        g2d.fillPolygon(paredeEsqX, paredeEsqY, 4);

        // Painéis verticais decorativos na parede esquerda
        g2d.setColor(new Color(30, 38, 60, 160));
        for (int i = 64; i < WORLD_SIZE; i += 64) {
            int pxChao = (int) Math.round(IsometricHelper.toScreenX(0, i, originX));
            int pyChao = (int) Math.round(IsometricHelper.toScreenY(0, i, originY));
            g2d.drawLine(pxChao, pyChao, pxChao, pyChao - alturaParede);
        }

        // Rodapé e moldura superior da parede esquerda
        g2d.setColor(new Color(180, 140, 50));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawLine(xCentro, yCentro, xExtremoEsq, yExtremoEsq);
        g2d.drawLine(xCentro, yCentro - alturaParede, xExtremoEsq, yExtremoEsq - alturaParede);

        // 2. Parede Direita (Noroeste de mundo, ao longo de y = 0)
        int[] paredeDirX = {xCentro, xExtremoDir, xExtremoDir, xCentro};
        int[] paredeDirY = {yCentro, yExtremoDir, yExtremoDir - alturaParede, yCentro - alturaParede};
        g2d.setColor(new Color(28, 36, 56));
        g2d.fillPolygon(paredeDirX, paredeDirY, 4);

        // Painéis verticais decorativos na parede direita
        g2d.setColor(new Color(38, 48, 75, 160));
        for (int i = 64; i < WORLD_SIZE; i += 64) {
            int pxChao = (int) Math.round(IsometricHelper.toScreenX(i, 0, originX));
            int pyChao = (int) Math.round(IsometricHelper.toScreenY(i, 0, originY));
            g2d.drawLine(pxChao, pyChao, pxChao, pyChao - alturaParede);
        }

        // Rodapé e moldura superior da parede direita
        g2d.setColor(new Color(212, 175, 55));
        g2d.drawLine(xCentro, yCentro, xExtremoDir, yExtremoDir);
        g2d.drawLine(xCentro, yCentro - alturaParede, xExtremoDir, yExtremoDir - alturaParede);

        // Canto central (encontro das paredes)
        g2d.setColor(new Color(230, 190, 80));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawLine(xCentro, yCentro, xCentro, yCentro - alturaParede);
        g2d.setStroke(new BasicStroke(1.0f));
    }

    /**
     * Desenha o portal de entrada do cassino com colunas e letreiro iluminado.
     */
    private void desenharPortalEntrada(Graphics2D g2d) {
        // Portal localizado na parede esquerda entre wy = 270 e wy = 370
        int alturaPortal = 100;
        int sx1 = (int) Math.round(IsometricHelper.toScreenX(0, 270, originX));
        int sy1 = (int) Math.round(IsometricHelper.toScreenY(0, 270, originY));

        int sx2 = (int) Math.round(IsometricHelper.toScreenX(0, 370, originX));
        int sy2 = (int) Math.round(IsometricHelper.toScreenY(0, 370, originY));

        // Vão de entrada (fundo escuro iluminado)
        int[] vaoX = {sx1, sx2, sx2, sx1};
        int[] vaoY = {sy1, sy2, sy2 - alturaPortal, sy1 - alturaPortal};
        g2d.setColor(new Color(10, 12, 18));
        g2d.fillPolygon(vaoX, vaoY, 4);

        // Brilho quente convidativo saindo da entrada
        g2d.setColor(new Color(255, 200, 80, 45));
        g2d.fillPolygon(vaoX, vaoY, 4);

        // Colunas e moldura do portal
        g2d.setColor(new Color(212, 175, 55));
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawLine(sx1, sy1, sx1, sy1 - alturaPortal);
        g2d.drawLine(sx2, sy2, sx2, sy2 - alturaPortal);
        g2d.drawLine(sx1, sy1 - alturaPortal, sx2, sy2 - alturaPortal);
        g2d.setStroke(new BasicStroke(1.0f));

        // Placa do letreiro sobre o portal
        int placaW = 140;
        int placaH = 26;
        int midX = (sx1 + sx2) / 2;
        int midY = (sy1 + sy2) / 2 - alturaPortal - 15;

        g2d.setColor(new Color(30, 15, 10));
        g2d.fillRoundRect(midX - (placaW / 2), midY, placaW, placaH, 8, 8);
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawRoundRect(midX - (placaW / 2), midY, placaW, placaH, 8, 8);

        // Letreiro iluminado em neon dourado
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2d.setColor(new Color(255, 235, 120));
        g2d.drawString("★ CASSINO SOCIAL ★", midX - 62, midY + 17);
    }

    /**
     * Desenha o tapete vermelho aveludado com frisos dourados no chão do salão.
     */
    private void desenharTapeteIsometrico(Graphics2D g2d) {
        // O tapete conecta a entrada (x=0, y=285..355) até a área central (x=210, y=200..260)
        double[][] verticesMundo = {
                {0, 285},
                {210, 195},
                {240, 245},
                {0, 355}
        };

        int[] tx = new int[4];
        int[] ty = new int[4];

        for (int i = 0; i < 4; i++) {
            tx[i] = (int) Math.round(IsometricHelper.toScreenX(verticesMundo[i][0], verticesMundo[i][1], originX));
            ty[i] = (int) Math.round(IsometricHelper.toScreenY(verticesMundo[i][0], verticesMundo[i][1], originY));
        }

        // Veludo vermelho bordô
        g2d.setColor(new Color(150, 18, 24));
        g2d.fillPolygon(tx, ty, 4);

        // Borda dourada ornamentada
        g2d.setColor(new Color(220, 180, 50));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawPolygon(tx, ty, 4);

        // Friso decorativo interno
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setColor(new Color(240, 210, 100, 140));
        int[] inTx = new int[4];
        int[] inTy = new int[4];
        for (int i = 0; i < 4; i++) {
            double cx = (verticesMundo[0][0] + verticesMundo[1][0] + verticesMundo[2][0] + verticesMundo[3][0]) / 4.0;
            double cy = (verticesMundo[0][1] + verticesMundo[1][1] + verticesMundo[2][1] + verticesMundo[3][1]) / 4.0;
            double interX = verticesMundo[i][0] * 0.85 + cx * 0.15;
            double interY = verticesMundo[i][1] * 0.85 + cy * 0.15;
            inTx[i] = (int) Math.round(IsometricHelper.toScreenX(interX, interY, originX));
            inTy[i] = (int) Math.round(IsometricHelper.toScreenY(interX, interY, originY));
        }
        g2d.drawPolygon(inTx, inTy, 4);
    }

    /**
     * Desenha os losangos do piso do cassino (proporção isométrica 2:1 exata).
     */
    private void desenharPisoIsometrico(Graphics2D g2d) {
        Color corTile1 = new Color(30, 40, 60);
        Color corTile2 = new Color(24, 32, 48);
        Color corLinha = new Color(45, 60, 85, 120);

        for (int tx = 0; tx < WORLD_GRID_TILES; tx++) {
            for (int ty = 0; ty < WORLD_GRID_TILES; ty++) {
                double wx = tx * IsometricHelper.TILE_SIZE_WORLD;
                double wy = ty * IsometricHelper.TILE_SIZE_WORLD;
                double wSize = IsometricHelper.TILE_SIZE_WORLD;

                // 4 vértices do tile losango projetado na tela
                int xNorte = (int) Math.round(IsometricHelper.toScreenX(wx, wy, originX));
                int yNorte = (int) Math.round(IsometricHelper.toScreenY(wx, wy, originY));

                int xLeste = (int) Math.round(IsometricHelper.toScreenX(wx + wSize, wy, originX));
                int yLeste = (int) Math.round(IsometricHelper.toScreenY(wx + wSize, wy, originY));

                int xSul = (int) Math.round(IsometricHelper.toScreenX(wx + wSize, wy + wSize, originX));
                int ySul = (int) Math.round(IsometricHelper.toScreenY(wx + wSize, wy + wSize, originY));

                int xOeste = (int) Math.round(IsometricHelper.toScreenX(wx, wy + wSize, originX));
                int yOeste = (int) Math.round(IsometricHelper.toScreenY(wx, wy + wSize, originY));

                int[] pontosX = {xNorte, xLeste, xSul, xOeste};
                int[] pontosY = {yNorte, yLeste, ySul, yOeste};

                g2d.setColor(((tx + ty) % 2 == 0) ? corTile1 : corTile2);
                g2d.fillPolygon(pontosX, pontosY, 4);

                g2d.setColor(corLinha);
                g2d.drawPolygon(pontosX, pontosY, 4);
            }
        }
    }

    private void desenharHUD(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(20, 20, 520, 85);
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawRect(20, 20, 520, 85);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2d.drawString("CASSINO SOCIAL 2D - SALAO PRINCIPAL (OFFLINE)", 35, 45);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Controles: WASD ou Setas (Movimento em diagonal) | ESC: Sair", 35, 68);

        g2d.setColor(new Color(100, 255, 150));
        long saldo = (blackjackEngine != null) ? blackjackEngine.getSaldoVirtual() : 0L;
        String saldoFormatado = String.format(java.util.Locale.GERMAN, "%,d", saldo);
        g2d.drawString("Saldo Virtual: " + saldoFormatado + " Fichas (Sem dinheiro real)", 35, 88);
    }

    private void desenharBalcaoInteracao(Graphics2D g2d) {
        int cx = getWidth() / 2;
        int cy = getHeight() - 100;

        g2d.setColor(new Color(0, 0, 0, 210));
        g2d.fillRoundRect(cx - 160, cy - 25, 320, 50, 15, 15);
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawRoundRect(cx - 160, cy - 25, 320, 50, 15, 15);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2d.setColor(Color.YELLOW);
        String texto = "[ E ] - Interagir com a Mesa de Blackjack";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(texto, cx - (fm.stringWidth(texto) / 2), cy + 6);
    }

    /**
     * Renderiza a interface completa da mesa de Blackjack.
     */
    private void desenharMenuBlackjack(Graphics2D g2d) {
        if (blackjackTableView != null) {
            blackjackTableView.render(g2d, getWidth(), getHeight());
        }
    }

    /**
     * Permite conectar qualquer motor de Blackjack compativel com o contrato BlackjackEngine.
     * Quando o colega finalizar a implementacao de BlackjackGame, basta injeta-la aqui.
     */
    public void setBlackjackEngine(BlackjackEngine engine) {
        if (engine != null) {
            this.blackjackEngine = engine;
            this.blackjackTableView = new BlackjackTableView(engine);
        }
    }

    public BlackjackEngine getBlackjackEngine() {
        return blackjackEngine;
    }

    public BlackjackTableView getBlackjackTableView() {
        return blackjackTableView;
    }

    public MyCharacter getPlayer() {
        return player;
    }

    public CasinoTable getMesaBlackjack() {
        return mesaBlackjack;
    }

    public GameState getEstadoAtual() {
        return estadoAtual;
    }

    public void setEstadoAtual(GameState novoEstado) {
        this.estadoAtual = novoEstado;
        this.teclasPressionadas.clear();
    }

    public void processarComandoMesa(String comando) {
        if (estadoAtual == GameState.MENU_BLACKJACK && blackjackTableView != null) {
            blackjackTableView.processarTecla(comando);
        }
    }

    public void simularPressionarTecla(String codigoFisico) {
        if (estadoAtual == GameState.SALAO) {
            this.teclasPressionadas.add(codigoFisico);
        }
    }

    public void simularSoltarTecla(String codigoFisico) {
        this.teclasPressionadas.remove(codigoFisico);
    }

    public boolean isMovendoParaCima() {
        return teclasPressionadas.contains("W") || teclasPressionadas.contains("UP");
    }

    public boolean isMovendoParaBaixo() {
        return teclasPressionadas.contains("S") || teclasPressionadas.contains("DOWN");
    }

    public boolean isMovendoParaEsquerda() {
        return teclasPressionadas.contains("A") || teclasPressionadas.contains("LEFT");
    }

    public boolean isMovendoParaDireita() {
        return teclasPressionadas.contains("D") || teclasPressionadas.contains("RIGHT");
    }

    public CasinoCounter getBalcao() {
        return balcao;
    }

    public List<CasinoChair> getCadeiras() {
        return cadeiras;
    }

    public List<MyShape> getObstaculos() {
        return obstaculos;
    }

    public List<MyShape> getEntidadesRenderizaveis() {
        return entidadesRenderizaveis;
    }
}

