package view;

import model.CasinoCounter;
import model.CasinoChair;
import model.CasinoTable;
import model.IsometricHelper;
import model.MyCharacter;
import model.MyRectangle;
import model.MyShape;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.Locale;
import model.RoundState;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import model.BlackjackEngine;
import model.BlackjackGame;
import model.PokerEngine;
import model.TexasHoldemGame;
import model.TexasHoldemTableGame;
import model.TrucoDeck;
import model.TrucoPlayer;
import model.TrucoRound;
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
        MENU_BLACKJACK,
        MENU_TRUCO,
        MENU_POKER
    }

    private GameState estadoAtual = GameState.SALAO;

    // Dimensões do chão lógico do salão (em pixels de mundo)
    public static final int WORLD_GRID_TILES = 16;
    public static final int WORLD_SIZE = (int) (WORLD_GRID_TILES * IsometricHelper.TILE_SIZE_WORLD); // 512x512

    // Entidades
    private MyCharacter player;
    private CasinoTable mesaBlackjack;
    private CasinoTable mesaTruco;
    private CasinoTable mesaPoker;
    private CasinoTable mesaAtiva;
    private CasinoCounter balcao;
    private List<CasinoChair> cadeiras;
    private List<MyShape> obstaculos;
    private List<MyShape> entidadesRenderizaveis;

    // Interface e Motor do Blackjack
    private BlackjackEngine blackjackEngine;
    private BlackjackTableView blackjackTableView;

    // Interface e Motor do Truco Paulista
    private TrucoRound trucoRound;
    private TrucoTableView trucoTableView;
    private TrucoMatchView trucoMatchView;
    private boolean usandoTrucoRoundLegado = false;
    private TrucoPlayer proximoPrimeiroDaMao = TrucoPlayer.JOGADOR;

    // Interface da Mesa de Poker (Texas Hold'em)
    // Interface e Motor da Mesa de Poker (Texas Hold'em)
    private PokerEngine pokerEngine;
    private PokerTableView pokerTableView;

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
    // Mensagem de interação e áreas de layout do HUD
    private boolean proximoDaMesa = false;
    private final Rectangle hudBounds = new Rectangle();
    private final Rectangle promptBounds = new Rectangle();
    private final NumberFormat formatadorFichas = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));

    public ScreenSaver() {
        setBackground(new Color(18, 12, 16));
        setDoubleBuffered(true);
        setFocusable(true);

        inicializarEntidades();
        configurarMesaBlackjack();
        configurarMesaTruco();
        configurarMesaPoker();
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
        this.mesaBlackjack = new CasinoTable(160, 160, 80, 50, spriteMesa, "Blackjack", 0, 0);

        // Cria a mesa de Truco em área ampla com circulação livre
        this.mesaTruco = new CasinoTable(320, 280, 80, 50, spriteMesa, "Truco", 0, 0);

        // Cria a terceira mesa (Poker Texas Hold'em) no setor nordeste com circulação desimpedida
        this.mesaPoker = new CasinoTable(340, 50, 80, 50, spriteMesa, "Poker", 0, 0);

        // Cria o balcão do bar com footprint sólido na lateral norte
        this.balcao = new CasinoCounter(110, 40, 80, 30, "BAR", 0, 0);

        // Cria cadeiras associadas ao bar e às três mesas, sempre voltadas para o tampo correspondente
        this.cadeiras.add(new CasinoChair(138, 78, 18, CasinoChair.Orientation.NORTE, 0, 0));
        this.cadeiras.add(new CasinoChair(166, 78, 18, CasinoChair.Orientation.NORTE, 0, 0));
        this.cadeiras.add(new CasinoChair(192, 134, 18, CasinoChair.Orientation.SUL, 0, 0));
        this.cadeiras.add(new CasinoChair(134, 176, 18, CasinoChair.Orientation.LESTE, 0, 0));
        this.cadeiras.add(new CasinoChair(314, 66, 18, CasinoChair.Orientation.LESTE, 0, 0));
        this.cadeiras.add(new CasinoChair(386, 108, 18, CasinoChair.Orientation.NORTE, 0, 0));
        this.cadeiras.add(new CasinoChair(368, 254, 18, CasinoChair.Orientation.SUL, 0, 0));
        this.cadeiras.add(new CasinoChair(352, 338, 18, CasinoChair.Orientation.NORTE, 0, 0));

        // Adiciona à lista de obstáculos para colisão física no solo
        this.obstaculos.add(this.mesaBlackjack);
        this.obstaculos.add(this.mesaTruco);
        this.obstaculos.add(this.mesaPoker);
        this.obstaculos.add(this.balcao);
        for (CasinoChair c : this.cadeiras) {
            this.obstaculos.add(c);
        }

        // Adiciona à lista heterogênea polimórfica de renderização com Z-sorting
        this.entidadesRenderizaveis.add(this.mesaBlackjack);
        this.entidadesRenderizaveis.add(this.mesaTruco);
        this.entidadesRenderizaveis.add(this.mesaPoker);
        this.entidadesRenderizaveis.add(this.balcao);
        for (CasinoChair c : this.cadeiras) {
            this.entidadesRenderizaveis.add(c);
        }
        this.entidadesRenderizaveis.add(this.player);
    }

    /**
     * Inicializa a interface da mesa de Blackjack acoplada ao motor oficial de regras BlackjackGame.
     * O simulador/mock BlackjackMockSimulator continua disponível para testes que dependam dele.
     */
    private void configurarMesaBlackjack() {
        this.blackjackEngine = new BlackjackGame();
        this.blackjackTableView = new BlackjackTableView(this.blackjackEngine);
    }

    /**
     * Inicializa a interface da mesa de Truco acoplada ao motor oficial TrucoRound.
     */
    private void configurarMesaTruco() {
        this.trucoRound = TrucoRound.iniciarComBaralho(new TrucoDeck(), TrucoPlayer.JOGADOR);
        this.trucoTableView = new TrucoTableView(this);
        this.trucoMatchView = new TrucoMatchView(this);
        this.usandoTrucoRoundLegado = false;
    }

    /**
     * Inicializa a interface da mesa de Poker Texas Hold'em.
     */
    private void configurarMesaPoker() {
        this.pokerEngine = new TexasHoldemTableGame();
        this.pokerEngine.iniciarNovaMao();
        this.pokerTableView = new PokerTableView(this, this.pokerEngine);
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
                processarRetornoB();
            }
        } else if (estadoAtual == GameState.MENU_TRUCO && trucoTableView != null) {
            boolean sairParaSalao = (!usandoTrucoRoundLegado && trucoMatchView != null)
                    ? trucoMatchView.processarClique(x, y)
                    : trucoTableView.processarClique(x, y);
            if (sairParaSalao) {
                processarRetornoB();
            }
        } else if (estadoAtual == GameState.MENU_POKER && pokerTableView != null) {
            boolean sairParaSalao = pokerTableView.processarClique(x, y);
            if (sairParaSalao) {
                processarRetornoB();
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
                } else if (estadoAtual == GameState.MENU_TRUCO) {
                    processarComandoTruco("S");
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

        // Tecla ESPACO: Apostar/Jogar no Blackjack ou Check/Call no Poker
        im.put(KeyStroke.getKeyStroke("pressed SPACE"), "press_SPACE");
        am.put("press_SPACE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoAtual == GameState.MENU_BLACKJACK && blackjackTableView != null) {
                    blackjackTableView.processarTecla("ESPACO");
                } else if (estadoAtual == GameState.MENU_POKER && pokerTableView != null) {
                    pokerTableView.processarTecla("ESPACO");
                }
            }
        });

        // Teclas numericas para selecao de fichas (Blackjack/Poker) ou cartas (Truco)
        configurarTeclaAtalhoMesa(im, am, "1", "FICHA_1");
        configurarTeclaAtalhoMesa(im, am, "2", "FICHA_2");
        configurarTeclaAtalhoMesa(im, am, "3", "FICHA_3");

        // Teclas de acao do Poker Texas Hold'em
        configurarTeclaAtalhoMesa(im, am, "F", "FOLD");
        configurarTeclaAtalhoMesa(im, am, "C", "CHECK");
        // Teclas de acao do Poker Texas Hold'em e Truco Paulista
        configurarTeclaAtalhoMesa(im, am, "F", "FOLD");
        configurarTeclaAtalhoMesa(im, am, "C", "CHECK");
        configurarTeclaAtalhoMesa(im, am, "R", "RAISE");
        configurarTeclaAtalhoMesa(im, am, "T", "TRUCO");
        configurarTeclaAtalhoMesa(im, am, "U", "AUMENTAR");
        configurarTeclaAtalhoMesa(im, am, "M", "TROCAR_MODO");
        configurarTeclaAtalhoMesa(im, am, "I", "INDIVIDUAL");

        // Tecla N para Nova Mão no Truco ou Poker quando finalizada
        im.put(KeyStroke.getKeyStroke("pressed N"), "press_N");
        am.put("press_N", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoAtual == GameState.MENU_TRUCO && trucoTableView != null) {
                    processarComandoTruco("NOVA_MAO");
                } else if (estadoAtual == GameState.MENU_POKER && pokerTableView != null) {
                    pokerTableView.processarTecla("NOVA_MAO");
                }
            }
        });

        // Tecla B para retornar ao salao a partir de qualquer mesa
        im.put(KeyStroke.getKeyStroke("pressed B"), "acaoB");
        am.put("acaoB", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processarRetornoB();
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
                } else if (estadoAtual == GameState.MENU_TRUCO && trucoTableView != null) {
                    processarComandoTruco(comando);
                } else if (estadoAtual == GameState.MENU_POKER && pokerTableView != null) {
                    pokerTableView.processarTecla(comando);
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
                } else if (estadoAtual == GameState.MENU_TRUCO) {
                    if ("A".equals(codigoFisico)) {
                        processarComandoTruco("ACEITAR");
                    } else if ("D".equals(codigoFisico)) {
                        processarComandoTruco("DUPLAS");
                    }
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

    public void processarRetornoB() {
        if (estadoAtual == GameState.MENU_BLACKJACK || estadoAtual == GameState.MENU_TRUCO || estadoAtual == GameState.MENU_POKER) {
            player.playSound();
            estadoAtual = GameState.SALAO;
            teclasPressionadas.clear();
        }
    }

    public void processarInteracaoE() {
        if (estadoAtual == GameState.SALAO) {
            if (mesaAtiva != null) {
                player.playSound();
                if ("Truco".equalsIgnoreCase(mesaAtiva.getGameName())) {
                    estadoAtual = GameState.MENU_TRUCO;
                } else if ("Poker".equalsIgnoreCase(mesaAtiva.getGameName())) {
                    estadoAtual = GameState.MENU_POKER;
                } else {
                    estadoAtual = GameState.MENU_BLACKJACK;
                }
                teclasPressionadas.clear();
            }
        } else if (estadoAtual == GameState.MENU_BLACKJACK || estadoAtual == GameState.MENU_TRUCO || estadoAtual == GameState.MENU_POKER) {
            player.playSound();
            estadoAtual = GameState.SALAO;
            teclasPressionadas.clear();
        }
    }

    /**
     * Determina de forma unificada qual mesa está ao alcance do jogador.
     * Se múltiplas mesas estiverem ao alcance, seleciona deterministamente a que tiver
     * menor distância física euclidiana ao quadrado (getDistanceSqToPlayer).
     */
    public void atualizarMesaMaisProxima() {
        CasinoTable maisProxima = null;
        double menorDistSq = Double.MAX_VALUE;

        CasinoTable[] mesas = {mesaBlackjack, mesaTruco, mesaPoker};
        for (CasinoTable mesa : mesas) {
            if (mesa != null && mesa.isPlayerNear(player, 35)) {
                double distSq = mesa.getDistanceSqToPlayer(player);
                if (distSq < menorDistSq) {
                    menorDistSq = distSq;
                    maisProxima = mesa;
                }
            }
        }

        this.mesaAtiva = maisProxima;
        this.proximoDaMesa = (this.mesaAtiva != null);
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
        if (mesaTruco != null) {
            mesaTruco.updateOrigins(originX, originY);
        }
        if (mesaPoker != null) {
            mesaPoker.updateOrigins(originX, originY);
        }
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

            // Seleção unificada da mesa mais próxima para interação
            atualizarMesaMaisProxima();
        }
        if (estadoAtual == GameState.MENU_TRUCO && !usandoTrucoRoundLegado && trucoMatchView != null) {
            trucoMatchView.atualizarTemporizacao(dt);
        } else if (estadoAtual == GameState.MENU_POKER && pokerTableView != null) {
            pokerTableView.atualizarTemporizacao(dt);
        }
        if (false) {
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
        } else if (estadoAtual == GameState.MENU_BLACKJACK) {
            desenharMenuBlackjack(g2d);
        } else if (estadoAtual == GameState.MENU_TRUCO) {
            desenharMenuTruco(g2d);
        } else if (estadoAtual == GameState.MENU_POKER) {
            desenharMenuPoker(g2d);
        }

        g2d.dispose();
    }

    /**
     * Renderiza o salão isométrico, ordenando as entidades por profundidade (Z-Sorting).
     */
    private void desenharSalao(Graphics2D g2d) {
        // Fundo em gradiente quente (mogno escuro / bordô noturno)
        GradientPaint bgGrad = new GradientPaint(
                0, 0, new Color(16, 11, 15),
                0, Math.max(1, getHeight()), new Color(28, 17, 22)
        );
        g2d.setPaint(bgGrad);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        desenharPisoIsometrico(g2d);
        desenharParedesIsometricas(g2d);
        desenharPortalEntrada(g2d);
        desenharTapeteIsometrico(g2d);

        // Iluminação ambiente suave sobre o centro do salão (efeito lustre quente)
        float centroLuzX = (float) IsometricHelper.toScreenX(WORLD_SIZE * 0.5, WORLD_SIZE * 0.5, originX);
        float centroLuzY = (float) IsometricHelper.toScreenY(WORLD_SIZE * 0.5, WORLD_SIZE * 0.5, originY);
        float raioLuz = 320.0f;
        RadialGradientPaint luzAmbiente = new RadialGradientPaint(
                new Point2D.Float(centroLuzX, centroLuzY),
                raioLuz,
                new float[]{0.0f, 0.55f, 1.0f},
                new Color[]{
                        new Color(255, 215, 130, 34),
                        new Color(210, 145, 70, 14),
                        new Color(0, 0, 0, 0)
                }
        );
        g2d.setPaint(luzAmbiente);
        g2d.fillOval((int) (centroLuzX - raioLuz), (int) (centroLuzY - raioLuz * 0.6f),
                (int) (raioLuz * 2), (int) (raioLuz * 1.2f));

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
        } else {
            promptBounds.setBounds(0, 0, 0, 0);
        }
    }

    /**
     * Desenha as duas paredes isométricas de fundo com lambris de mogno, papel de parede bordô,
     * molduras douradas e arandelas de iluminação suave.
     */
    private void desenharParedesIsometricas(Graphics2D g2d) {
        int alturaParede = 138;
        int alturaLambris = 46;

        int xCentro = (int) Math.round(originX);
        int yCentro = (int) Math.round(originY);

        int xExtremoEsq = (int) Math.round(IsometricHelper.toScreenX(0, WORLD_SIZE, originX));
        int yExtremoEsq = (int) Math.round(IsometricHelper.toScreenY(0, WORLD_SIZE, originY));

        int xExtremoDir = (int) Math.round(IsometricHelper.toScreenX(WORLD_SIZE, 0, originX));
        int yExtremoDir = (int) Math.round(IsometricHelper.toScreenY(WORLD_SIZE, 0, originY));

        // 1. Parede Esquerda (ao longo de x = 0)
        int[] paredeEsqX = {xCentro, xExtremoEsq, xExtremoEsq, xCentro};
        int[] paredeEsqY = {yCentro, yExtremoEsq, yExtremoEsq - alturaParede, yCentro - alturaParede};
        g2d.setColor(new Color(44, 18, 24));
        g2d.fillPolygon(paredeEsqX, paredeEsqY, 4);

        // Lambris em madeira mogno na parte inferior da parede esquerda
        int[] lambrisEsqY = {yCentro, yExtremoEsq, yExtremoEsq - alturaLambris, yCentro - alturaLambris};
        g2d.setColor(new Color(52, 24, 16));
        g2d.fillPolygon(paredeEsqX, lambrisEsqY, 4);

        g2d.setColor(new Color(78, 34, 42, 130));
        for (int i = 64; i < WORLD_SIZE; i += 64) {
            int pxChao = (int) Math.round(IsometricHelper.toScreenX(0, i, originX));
            int pyChao = (int) Math.round(IsometricHelper.toScreenY(0, i, originY));
            g2d.drawLine(pxChao, pyChao, pxChao, pyChao - alturaParede);
        }

        g2d.setColor(new Color(196, 156, 58));
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.drawLine(xCentro, yCentro, xExtremoEsq, yExtremoEsq);
        g2d.drawLine(xCentro, yCentro - alturaLambris, xExtremoEsq, yExtremoEsq - alturaLambris);
        g2d.drawLine(xCentro, yCentro - alturaParede, xExtremoEsq, yExtremoEsq - alturaParede);

        // 2. Parede Direita (ao longo de y = 0)
        int[] paredeDirX = {xCentro, xExtremoDir, xExtremoDir, xCentro};
        int[] paredeDirY = {yCentro, yExtremoDir, yExtremoDir - alturaParede, yCentro - alturaParede};
        g2d.setColor(new Color(56, 22, 30));
        g2d.fillPolygon(paredeDirX, paredeDirY, 4);

        int[] lambrisDirY = {yCentro, yExtremoDir, yExtremoDir - alturaLambris, yCentro - alturaLambris};
        g2d.setColor(new Color(66, 30, 20));
        g2d.fillPolygon(paredeDirX, lambrisDirY, 4);

        g2d.setColor(new Color(92, 40, 50, 130));
        for (int i = 64; i < WORLD_SIZE; i += 64) {
            int pxChao = (int) Math.round(IsometricHelper.toScreenX(i, 0, originX));
            int pyChao = (int) Math.round(IsometricHelper.toScreenY(i, 0, originY));
            g2d.drawLine(pxChao, pyChao, pxChao, pyChao - alturaParede);
        }

        g2d.setColor(new Color(220, 180, 68));
        g2d.drawLine(xCentro, yCentro, xExtremoDir, yExtremoDir);
        g2d.drawLine(xCentro, yCentro - alturaLambris, xExtremoDir, yExtremoDir - alturaLambris);
        g2d.drawLine(xCentro, yCentro - alturaParede, xExtremoDir, yExtremoDir - alturaParede);

        g2d.setColor(new Color(235, 198, 86));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawLine(xCentro, yCentro, xCentro, yCentro - alturaParede);
        g2d.setStroke(new BasicStroke(1.0f));

        desenharArandelaParede(g2d, 0, 140, 88);
        desenharArandelaParede(g2d, 150, 0, 88);
        desenharArandelaParede(g2d, 340, 0, 88);
    }

    private void desenharArandelaParede(Graphics2D g2d, double wx, double wy, int altura) {
        int sx = (int) Math.round(IsometricHelper.toScreenX(wx, wy, originX));
        int sy = (int) Math.round(IsometricHelper.toScreenY(wx, wy, originY)) - altura;

        g2d.setColor(new Color(255, 215, 120, 32));
        g2d.fillOval(sx - 24, sy - 24, 48, 48);
        g2d.setColor(new Color(255, 230, 150, 65));
        g2d.fillOval(sx - 11, sy - 11, 22, 22);

        g2d.setColor(new Color(215, 175, 62));
        g2d.fillRect(sx - 2, sy + 2, 4, 8);
        g2d.setColor(new Color(255, 245, 200));
        g2d.fillOval(sx - 4, sy - 5, 8, 9);
    }

    /**
     * Desenha o portal de entrada do cassino com colunas de mogno/ouro e letreiro iluminado.
     */
    private void desenharPortalEntrada(Graphics2D g2d) {
        int alturaPortal = 104;
        int sx1 = (int) Math.round(IsometricHelper.toScreenX(0, 270, originX));
        int sy1 = (int) Math.round(IsometricHelper.toScreenY(0, 270, originY));

        int sx2 = (int) Math.round(IsometricHelper.toScreenX(0, 370, originX));
        int sy2 = (int) Math.round(IsometricHelper.toScreenY(0, 370, originY));

        int[] vaoX = {sx1, sx2, sx2, sx1};
        int[] vaoY = {sy1, sy2, sy2 - alturaPortal, sy1 - alturaPortal};
        g2d.setColor(new Color(14, 10, 14));
        g2d.fillPolygon(vaoX, vaoY, 4);

        GradientPaint brilhoPortal = new GradientPaint(
                (sx1 + sx2) / 2.0f, (sy1 + sy2) / 2.0f - alturaPortal, new Color(255, 210, 105, 85),
                (sx1 + sx2) / 2.0f, (sy1 + sy2) / 2.0f, new Color(255, 190, 80, 22)
        );
        g2d.setPaint(brilhoPortal);
        g2d.fillPolygon(vaoX, vaoY, 4);

        g2d.setColor(new Color(224, 184, 68));
        g2d.setStroke(new BasicStroke(3.2f));
        g2d.drawLine(sx1, sy1, sx1, sy1 - alturaPortal);
        g2d.drawLine(sx2, sy2, sx2, sy2 - alturaPortal);
        g2d.drawLine(sx1, sy1 - alturaPortal, sx2, sy2 - alturaPortal);
        g2d.setStroke(new BasicStroke(1.0f));

        int placaW = 150;
        int placaH = 26;
        int midX = (sx1 + sx2) / 2;
        int midY = (sy1 + sy2) / 2 - alturaPortal - 16;

        g2d.setColor(new Color(0, 0, 0, 125));
        g2d.fillRoundRect(midX - (placaW / 2) + 2, midY + 2, placaW, placaH, 8, 8);
        g2d.setColor(new Color(38, 16, 18));
        g2d.fillRoundRect(midX - (placaW / 2), midY, placaW, placaH, 8, 8);
        g2d.setColor(new Color(242, 202, 82));
        g2d.setStroke(new BasicStroke(1.4f));
        g2d.drawRoundRect(midX - (placaW / 2), midY, placaW, placaH, 8, 8);
        g2d.setStroke(new BasicStroke(1.0f));

        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2d.setColor(new Color(255, 238, 150));
        FontMetrics fm = g2d.getFontMetrics();
        String textoPortal = "★ CASSINO SOCIAL ★";
        g2d.drawString(textoPortal, midX - (fm.stringWidth(textoPortal) / 2), midY + 17);
    }

    /**
     * Desenha o tapete vermelho bordô aveludado com frisos dourados conectando a entrada ao centro do salão.
     */
    private void desenharTapeteIsometrico(Graphics2D g2d) {
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

        g2d.setColor(new Color(0, 0, 0, 75));
        int[] sx = {tx[0], tx[1] + 2, tx[2] + 2, tx[3]};
        int[] sy = {ty[0] + 2, ty[1] + 2, ty[2] + 3, ty[3] + 2};
        g2d.fillPolygon(sx, sy, 4);

        g2d.setColor(new Color(128, 18, 28));
        g2d.fillPolygon(tx, ty, 4);

        g2d.setColor(new Color(225, 185, 68));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawPolygon(tx, ty, 4);

        g2d.setStroke(new BasicStroke(1.1f));
        g2d.setColor(new Color(245, 215, 115, 155));
        int[] inTx = new int[4];
        int[] inTy = new int[4];
        double cx = (verticesMundo[0][0] + verticesMundo[1][0] + verticesMundo[2][0] + verticesMundo[3][0]) / 4.0;
        double cy = (verticesMundo[0][1] + verticesMundo[1][1] + verticesMundo[2][1] + verticesMundo[3][1]) / 4.0;
        for (int i = 0; i < 4; i++) {
            double interX = verticesMundo[i][0] * 0.87 + cx * 0.13;
            double interY = verticesMundo[i][1] * 0.87 + cy * 0.13;
            inTx[i] = (int) Math.round(IsometricHelper.toScreenX(interX, interY, originX));
            inTy[i] = (int) Math.round(IsometricHelper.toScreenY(interX, interY, originY));
        }
        g2d.drawPolygon(inTx, inTy, 4);
        g2d.setStroke(new BasicStroke(1.0f));
    }

    /**
     * Desenha os losangos do piso do cassino em paleta harmônica de madeira escura/ardósia (2:1 exata).
     */
    private void desenharPisoIsometrico(Graphics2D g2d) {
        Color corTile1 = new Color(40, 30, 36);
        Color corTile2 = new Color(31, 23, 29);
        Color corLinha = new Color(92, 72, 56, 105);

        for (int tx = 0; tx < WORLD_GRID_TILES; tx++) {
            for (int ty = 0; ty < WORLD_GRID_TILES; ty++) {
                double wx = tx * IsometricHelper.TILE_SIZE_WORLD;
                double wy = ty * IsometricHelper.TILE_SIZE_WORLD;
                double wSize = IsometricHelper.TILE_SIZE_WORLD;

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

    /**
     * Monta as linhas de resumo de apostas, pote, pontos e ganhos possíveis consultando
     * diretamente os motores ativos ({@link BlackjackEngine}, {@link PokerEngine} e {@link TrucoRound}),
     * sem duplicar cálculos de liquidação na interface.
     */
    public List<String> getLinhasResumoApostasHUD() {
        List<String> linhas = new ArrayList<>();

        if (blackjackEngine != null && blackjackEngine.getEstadoRodada() == RoundState.TURNO_JOGADOR) {
            long aposta = blackjackEngine.getApostaAtual();
            long lucroComum = blackjackEngine.getLucroPossivelVitoriaComum();
            long retornoComum = blackjackEngine.getRetornoTotalPossivelVitoriaComum();
            long lucroBJ = blackjackEngine.getLucroPossivelBlackjackNatural();
            long retornoBJ = blackjackEngine.getRetornoTotalPossivelBlackjackNatural();
            linhas.add("Blackjack  |  Aposta: " + formatarFichas(aposta)
                    + "  |  Vitória 1:1: lucro +" + formatarFichas(lucroComum) + " (retorno " + formatarFichas(retornoComum) + ")"
                    + "  |  BJ 3:2: lucro +" + formatarFichas(lucroBJ) + " (retorno " + formatarFichas(retornoBJ) + ")");
        }

        if (pokerEngine != null && pokerEngine.getEstagioAtual().isMaoEmAndamento()) {
            long comprometido = pokerEngine.getTotalComprometidoJogadorNaMao();
            long pote = pokerEngine.getPoteTotal();
            long lucroPotencial = pokerEngine.getLucroLiquidoPotencialVitoriaAtual();
            long retornoPotencial = pokerEngine.getRetornoTotalPotencialVitoriaAtual();
            long retornoEmpate = pokerEngine.getRetornoPotencialEmpateAtual();
            linhas.add("Poker  |  Comprometido: " + formatarFichas(comprometido)
                    + "  |  Pote: " + formatarFichas(pote)
                    + "  |  Vitória atual: lucro +" + formatarFichas(lucroPotencial) + " (retorno " + formatarFichas(retornoPotencial) + ")"
                    + "  |  Empate: " + formatarFichas(retornoEmpate));
        }

        if (!usandoTrucoRoundLegado && trucoMatchView != null && trucoMatchView.getMatch() != null
                && !trucoMatchView.getMatch().isPartidaFinalizada()) {
            model.TrucoMatch tm = trucoMatchView.getMatch();
            String extra = tm.isPedidoTrucoPendente()
                    ? "  |  Pedido: " + tm.getValorPropostoTruco() + " pts (" + tm.getNomeQuemPediuTruco() + ")"
                    : "";
            linhas.add("Truco (" + tm.getModo().getNomeExibicao() + ")  |  Placar: "
                    + tm.getPontosEquipeJogador() + " × " + tm.getPontosEquipeAdversaria() + " (até 12)"
                    + "  |  Mão vale: " + tm.getValorMaoAtual() + " pt(s)" + extra);
        } else if (trucoRound != null && !trucoRound.isFinalizada()) {
            linhas.add("Truco  |  Sem aposta em fichas  |  Em disputa: "
                    + trucoRound.getPontosEmDisputa() + " ponto"
                    + "  |  Vazas: Você " + trucoRound.getVazasVencidasJogador()
                    + " × " + trucoRound.getVazasVencidasBot() + " Bot");
        }

        return linhas;
    }

    private String formatarFichas(long valor) {
        return formatadorFichas.format(valor);
    }

    private void desenharHUD(Graphics2D g2d) {
        List<String> linhasAtivas = getLinhasResumoApostasHUD();

        Font fonteTitulo = new Font("SansSerif", Font.BOLD, 15);
        Font fonteSaldo = new Font("SansSerif", Font.BOLD, 13);
        Font fonteLinha = new Font("SansSerif", Font.PLAIN, 12);
        Font fonteControles = new Font("SansSerif", Font.PLAIN, 11);

        long saldoBlackjack = (blackjackEngine != null) ? blackjackEngine.getSaldoVirtual() : 0L;
        long saldoPoker = (pokerEngine != null) ? pokerEngine.getSaldoJogador() : 0L;

        String textoTitulo = "CASSINO SOCIAL 2D — SALÃO PRINCIPAL (OFFLINE)";
        String textoSaldo = "Saldo Blackjack: " + formatarFichas(saldoBlackjack) + " fichas"
                + "   |   Stack Poker: " + formatarFichas(saldoPoker) + " fichas (sem dinheiro real)";
        String textoVazio = "Nenhuma rodada ativa no momento — aproxime-se de uma mesa e pressione E.";
        String textoControles = "Controles: WASD ou Setas (diagonais)  |  E: Abrir mesa  |  B: Voltar  |  ESC: Sair";

        int larguraConteudo = Math.max(
                g2d.getFontMetrics(fonteTitulo).stringWidth(textoTitulo),
                g2d.getFontMetrics(fonteSaldo).stringWidth(textoSaldo)
        );
        larguraConteudo = Math.max(larguraConteudo, g2d.getFontMetrics(fonteControles).stringWidth(textoControles));
        FontMetrics fmLinha = g2d.getFontMetrics(fonteLinha);
        if (linhasAtivas.isEmpty()) {
            larguraConteudo = Math.max(larguraConteudo, fmLinha.stringWidth(textoVazio));
        } else {
            for (String linha : linhasAtivas) {
                larguraConteudo = Math.max(larguraConteudo, fmLinha.stringWidth(linha) + 14);
            }
        }

        int hudX = 18;
        int hudY = 16;
        int hudW = Math.min(Math.max(540, larguraConteudo + 34), Math.max(540, getWidth() - 36));
        int qtdLinhasApostas = Math.max(1, linhasAtivas.size());
        int hudH = 88 + (qtdLinhasApostas * 19);

        hudBounds.setBounds(hudX, hudY, hudW, hudH);

        g2d.setColor(new Color(0, 0, 0, 110));
        g2d.fillRoundRect(hudX + 3, hudY + 3, hudW, hudH, 12, 12);

        g2d.setColor(new Color(20, 13, 17, 218));
        g2d.fillRoundRect(hudX, hudY, hudW, hudH, 12, 12);

        g2d.setColor(new Color(224, 184, 68));
        g2d.setStroke(new BasicStroke(1.4f));
        g2d.drawRoundRect(hudX, hudY, hudW, hudH, 12, 12);
        g2d.setStroke(new BasicStroke(1.0f));

        g2d.setFont(fonteTitulo);
        g2d.setColor(new Color(255, 228, 132));
        g2d.drawString(textoTitulo, hudX + 16, hudY + 24);

        g2d.setFont(fonteSaldo);
        g2d.setColor(new Color(112, 240, 165));
        g2d.drawString(textoSaldo, hudX + 16, hudY + 45);

        g2d.setColor(new Color(215, 175, 65, 85));
        g2d.drawLine(hudX + 14, hudY + 53, hudX + hudW - 14, hudY + 53);

        g2d.setFont(fonteLinha);
        int yLinha = hudY + 70;
        if (linhasAtivas.isEmpty()) {
            g2d.setColor(new Color(198, 190, 196));
            g2d.drawString(textoVazio, hudX + 16, yLinha);
            yLinha += 19;
        } else {
            for (String linha : linhasAtivas) {
                Color corTag;
                if (linha.startsWith("Blackjack")) {
                    corTag = new Color(90, 220, 145);
                } else if (linha.startsWith("Poker")) {
                    corTag = new Color(255, 198, 112);
                } else {
                    corTag = new Color(130, 198, 255);
                }
                g2d.setColor(corTag);
                g2d.fillOval(hudX + 16, yLinha - 8, 6, 6);
                g2d.setColor(new Color(244, 240, 236));
                g2d.drawString(linha, hudX + 27, yLinha);
                yLinha += 19;
            }
        }

        g2d.setFont(fonteControles);
        g2d.setColor(new Color(185, 178, 184));
        g2d.drawString(textoControles, hudX + 16, hudY + hudH - 10);
    }

    private void desenharBalcaoInteracao(Graphics2D g2d) {
        if (mesaAtiva == null) {
            promptBounds.setBounds(0, 0, 0, 0);
            return;
        }
        int cx = getWidth() / 2;
        int cy = getHeight() - 78;
        int w = 390;
        int h = 46;
        int x = cx - (w / 2);
        int y = cy - (h / 2);

        promptBounds.setBounds(x, y, w, h);

        g2d.setColor(new Color(0, 0, 0, 110));
        g2d.fillRoundRect(x + 2, y + 3, w, h, 14, 14);
        g2d.setColor(new Color(24, 14, 18, 232));
        g2d.fillRoundRect(x, y, w, h, 14, 14);
        g2d.setColor(new Color(240, 200, 76));
        g2d.setStroke(new BasicStroke(1.6f));
        g2d.drawRoundRect(x, y, w, h, 14, 14);
        g2d.setStroke(new BasicStroke(1.0f));

        g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2d.setColor(new Color(255, 236, 130));
        String texto = "E — Interagir com a mesa de " + mesaAtiva.getGameName();
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(texto, cx - (fm.stringWidth(texto) / 2), cy + 5);
    }

    public Rectangle getHudBounds() {
        return new Rectangle(hudBounds);
    }

    public Rectangle getPromptBounds() {
        return new Rectangle(promptBounds);
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
     * Renderiza a interface da mesa de Truco Paulista através de TrucoTableView.
     */
    private void desenharMenuTruco(Graphics2D g2d) {
        if (trucoTableView != null) {
        if (!usandoTrucoRoundLegado && trucoMatchView != null) {
            trucoMatchView.render(g2d, getWidth(), getHeight());
        } else if (trucoTableView != null) {
            trucoTableView.render(g2d, getWidth(), getHeight());
        }
        }
    }

    /**
     * Renderiza a interface da mesa de Poker Texas Hold'em através de PokerTableView.
     */
    private void desenharMenuPoker(Graphics2D g2d) {
        if (pokerTableView != null) {
            pokerTableView.render(g2d, getWidth(), getHeight());
        }
    }

    /**
     * Inicia uma nova mão de Truco 1 vs 1 alternando o primeiro a jogar.
     */
    public void iniciarNovaMaoTruco() {
        this.proximoPrimeiroDaMao = (this.proximoPrimeiroDaMao == TrucoPlayer.JOGADOR) ? TrucoPlayer.BOT : TrucoPlayer.JOGADOR;
        this.trucoRound = TrucoRound.iniciarComBaralho(new TrucoDeck(), this.proximoPrimeiroDaMao);
        if (this.trucoRound.isTurnoBot()) {
            this.trucoRound.jogarTurnoBot();
        }
    }

    public TrucoRound getTrucoRound() {
        return trucoRound;
    }

    public void setTrucoRound(TrucoRound trucoRound) {
        if (trucoRound != null) {
            this.trucoRound = trucoRound;
            this.usandoTrucoRoundLegado = true;
        }
    }

    public TrucoTableView getTrucoTableView() {
        return trucoTableView;
    }

    public TrucoMatchView getTrucoMatchView() {
        return trucoMatchView;
    }

    /**
     * Avança manualmente o temporizador visual do Truco (usado em testes determinísticos sem Thread.sleep).
     * Respeita a regra de pausa automática: só avança quando o painel estiver em {@link GameState#MENU_TRUCO}.
     */
    public void avancarTempoTruco(double segundos) {
        if (estadoAtual == GameState.MENU_TRUCO && !usandoTrucoRoundLegado && trucoMatchView != null) {
            trucoMatchView.atualizarTemporizacao(segundos);
        }
    }

    public void processarComandoTruco(String comando) {
        if (estadoAtual == GameState.MENU_TRUCO) {
            if (!usandoTrucoRoundLegado && trucoMatchView != null) {
                trucoMatchView.processarTecla(comando);
            } else if (trucoTableView != null) {
                trucoTableView.processarTecla(comando);
            }
        }
    }


    public PokerEngine getPokerEngine() {
        return pokerEngine;
    }

    /**
     * Permite conectar qualquer implementação compatível com o contrato {@link PokerEngine}
     * (produção ou motor determinístico de testes), atualizando a {@link PokerTableView}.
     */
    public void setPokerEngine(PokerEngine engine) {
        if (engine != null) {
            this.pokerEngine = engine;
            this.pokerTableView = new PokerTableView(this, engine);
        }
    }

    public PokerTableView getPokerTableView() {
        return pokerTableView;
    }

    public void setPokerTableView(PokerTableView pokerTableView) {
        if (pokerTableView != null) {
            this.pokerTableView = pokerTableView;
        }
    }

    public void processarComandoPoker(String comando) {
        if (estadoAtual == GameState.MENU_POKER && pokerTableView != null) {
            pokerTableView.processarTecla(comando);
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

    public CasinoTable getMesaTruco() {
        return mesaTruco;
    }

    public CasinoTable getMesaPoker() {
        return mesaPoker;
    }

    public CasinoTable getMesaAtiva() {
        return mesaAtiva;
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

    public void avancarTempoPoker(double segundos) {
        if (estadoAtual == GameState.MENU_POKER && pokerTableView != null && segundos > 0.0) {
            pokerTableView.atualizarTemporizacao(segundos);
        }
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

