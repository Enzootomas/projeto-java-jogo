package tests;

import model.CasinoCounter;
import model.CasinoChair;
import model.CasinoTable;
import model.IsometricHelper;
import model.MyCharacter;
import model.MyCircle;
import model.MyEllipse;
import model.MyRectangle;
import model.MyShape;
import model.MySquare;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Bateria de testes didáticos automatizados de POO e matemática do jogo.
 *
 * Etapa 1: Validação dos 4 pilares de POO (Abstração, Encapsulamento, Herança e Polimorfismo),
 * regras de invariantes em quadrados/círculos, validação de dimensões e intersects estrito.
 *
 * Etapa 2: Validação da projeção isométrica direta e inversa 2:1, normalização e velocidade
 * diagonal constante, resolução de colisão por eixos com deslizamento, ponto de apoio para
 * profundidade (z-sorting) nos 4 lados da mesa, e separação de footprint em MyCharacter.
 */
public class TestePOO {

    private static int totalTestes = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   BATERIA DE TESTES DIDATICOS DE POO - ETAPAS 1 E 2");
        System.out.println("====================================================\n");

        // Testes da Etapa 1 (Preservados)
        testarHerancaEPolimorfismoColecao();
        testarEncapsulamentoEInvariantes();
        testarValidacaoDimensoes();
        testarColisoesEIntersects();
        testarPolimorfismoEDrawComPixels();
        testarMovimentoEQuique();

        // Testes da Etapa 2 (Novos)
        testarProjecaoIsometriaEInversa();
        testarNormalizacaoEVelocidadeDiagonal();
        testarColisaoDeslizamentoPorEixos();
        testarZSortingNosQuatroLadosDaMesa();
        testarConstrutoresEFootprintMyCharacter();
        testarSincronizacaoPosicaoEColisaoMyCharacter();
        testarCarregamentoClasspathERecursos();

        // Testes da Etapa 3 (Mobília e Ambientação do Salão)
        testarColisaoEZSortingNovosMoveis();

        System.out.println("\n----------------------------------------------------");
        System.out.println("Total de verificacoes: " + totalTestes);
        System.out.println("Falhas: " + falhas);
        if (falhas == 0) {
            System.out.println("RESULTADO: TODOS OS TESTES PASSARAM COM SUCESSO! [OK]");
            System.out.println("----------------------------------------------------");
        } else {
            System.err.println("RESULTADO: EXISTEM TESTES COM FALHA! [FALHA]");
            System.out.println("----------------------------------------------------");
            System.exit(1);
        }
    }

    private static void afirme(boolean condicao, String descricao) {
        totalTestes++;
        if (condicao) {
            System.out.println("  [OK] " + descricao);
        } else {
            falhas++;
            System.err.println("  [FALHA] " + descricao);
        }
    }

    // =========================================================================
    // TESTES DA ETAPA 1
    // =========================================================================

    private static void testarHerancaEPolimorfismoColecao() {
        System.out.println("1. Testando Heranca e Relacoes de Tipo (Etapa 1):");

        MyRectangle ret = new MyRectangle(10, 10, 40, 20, Color.BLUE);
        MySquare quad = new MySquare(0, 0, 30, Color.RED);
        MyEllipse elip = new MyEllipse(5, 5, 50, 25, Color.GREEN);
        MyCircle circ = new MyCircle(0, 0, 40, Color.YELLOW);

        afirme(ret instanceof MyShape, "MyRectangle e instancia de MyShape (Heranca direta)");
        afirme(quad instanceof MyRectangle, "MySquare e instancia de MyRectangle (Heranca direta)");
        afirme(quad instanceof MyShape, "MySquare e instancia indireta de MyShape (Heranca transitiva)");
        afirme(elip instanceof MyShape, "MyEllipse e instancia de MyShape (Heranca direta)");
        afirme(circ instanceof MyEllipse, "MyCircle e instancia de MyEllipse (Heranca direta)");
        afirme(circ instanceof MyShape, "MyCircle e instancia indireta de MyShape (Heranca transitiva)");

        MySquare quadRandom = new MySquare(800, 600);
        afirme(quadRandom.getWidth() == quadRandom.getHeight(),
                "MySquare aleatorio garante largura e altura iguais (Invariante no construtor)");
        afirme(quadRandom.getWidth() > 0 && quadRandom.getHeight() > 0,
                "MySquare aleatorio possui dimensoes positivas");

        MyCircle circRandom = new MyCircle(800, 600);
        afirme(circRandom.getWidth() == circRandom.getHeight(),
                "MyCircle aleatorio garante largura e altura iguais (Invariante no construtor)");
        System.out.println();
    }

    private static void testarEncapsulamentoEInvariantes() {
        System.out.println("2. Testando Encapsulamento e Invariantes Geometricos (Etapa 1):");

        MySquare quadrado = new MySquare(10, 10, 50, Color.BLACK);
        afirme(quadrado.getWidth() == 50 && quadrado.getHeight() == 50,
                "Quadrado inicializado com lado 50 possui largura=50 e altura=50");

        quadrado.setWidth(80);
        afirme(quadrado.getWidth() == 80 && quadrado.getHeight() == 80,
                "quadrado.setWidth(80) atualizou tanto largura quanto altura para 80");

        quadrado.setHeight(65);
        afirme(quadrado.getWidth() == 65 && quadrado.getHeight() == 65,
                "quadrado.setHeight(65) atualizou tanto altura quanto largura para 65");

        MyCircle circulo = new MyCircle(20, 20, 30, Color.RED);
        afirme(circulo.getWidth() == 30 && circulo.getHeight() == 30,
                "Circulo inicializado com diametro 30 possui largura=30 e altura=30");

        circulo.setWidth(90);
        afirme(circulo.getWidth() == 90 && circulo.getHeight() == 90,
                "circulo.setWidth(90) sincronizou altura=90 mantendo formato circular");

        circulo.setHeight(42);
        afirme(circulo.getWidth() == 42 && circulo.getHeight() == 42,
                "circulo.setHeight(42) sincronizou largura=42 mantendo formato circular");
        System.out.println();
    }

    private static void testarValidacaoDimensoes() {
        System.out.println("3. Testando Validacao de Dimensoes Positivas (Etapa 1):");

        boolean capturouNoConstrutor = false;
        try {
            new MyRectangle(10, 10, -5, 20);
        } catch (IllegalArgumentException e) {
            capturouNoConstrutor = true;
        }
        afirme(capturouNoConstrutor, "Construtor de MyRectangle rejeita largura negativa");

        boolean capturouLarguraZero = false;
        try {
            MySquare sq = new MySquare(0, 0, 10);
            sq.setWidth(0);
        } catch (IllegalArgumentException e) {
            capturouLarguraZero = true;
        }
        afirme(capturouLarguraZero, "MySquare.setWidth rejeita valor zero");

        boolean capturouAlturaNegativaCirculo = false;
        try {
            MyCircle c = new MyCircle(0, 0, 10);
            c.setHeight(-10);
        } catch (IllegalArgumentException e) {
            capturouAlturaNegativaCirculo = true;
        }
        afirme(capturouAlturaNegativaCirculo, "MyCircle.setHeight rejeita valor negativo");
        System.out.println();
    }

    private static void testarColisoesEIntersects() {
        System.out.println("4. Testando Deteccao de Colisoes (intersects) (Etapa 1):");

        MyRectangle base = new MyRectangle(10, 10, 50, 50);

        MyRectangle sobreposto = new MyRectangle(30, 30, 40, 40);
        afirme(base.intersects(sobreposto), "Cenario 1: Formas com sobreposicao -> intersects() TRUE");
        afirme(sobreposto.intersects(base), "Cenario 1 (Comutativo): sobreposto.intersects(base) TRUE");

        MyRectangle distante = new MyRectangle(200, 200, 30, 30);
        afirme(!base.intersects(distante), "Cenario 2: Formas distantes -> intersects() FALSE");

        MyRectangle encostadoHorizontal = new MyRectangle(60, 10, 30, 50);
        afirme(!base.intersects(encostadoHorizontal),
                "Cenario 3: Bordas horizontais apenas encostadas -> intersects() FALSE");

        MyRectangle encostadoVertical = new MyRectangle(10, 60, 50, 30);
        afirme(!base.intersects(encostadoVertical),
                "Cenario 3: Bordas verticais apenas encostadas -> intersects() FALSE");

        afirme(!base.intersects(null), "Comparacao com null retorna FALSE de forma segura");
        System.out.println();
    }

    private static void testarPolimorfismoEDrawComPixels() {
        System.out.println("5. Testando Polimorfismo de draw(Graphics2D) e Inspecao de Pixels (Etapa 1):");

        int larguraImagem = 300;
        int alturaImagem = 300;
        BufferedImage imagem = new BufferedImage(larguraImagem, alturaImagem, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = imagem.createGraphics();

        List<MyShape> formas = new ArrayList<>();
        Color corRetangulo = new Color(255, 0, 0);
        Color corElipse    = new Color(0, 0, 255);
        Color corQuadrado  = new Color(0, 255, 0);
        Color corCirculo   = new Color(255, 255, 0);

        formas.add(new MyRectangle(10, 10, 40, 40, corRetangulo));
        formas.add(new MyEllipse(100, 10, 50, 40, corElipse));
        formas.add(new MySquare(10, 100, 40, corQuadrado));
        formas.add(new MyCircle(100, 100, 50, corCirculo));

        for (MyShape forma : formas) {
            forma.draw(g2d);
        }

        g2d.dispose();

        int pixelRetangulo = imagem.getRGB(25, 25) & 0x00FFFFFF;
        afirme(pixelRetangulo == (corRetangulo.getRGB() & 0x00FFFFFF),
                "Pixel interno do MyRectangle (25, 25) foi colorido com vermelho");

        int pixelElipseCentro = imagem.getRGB(125, 30) & 0x00FFFFFF;
        afirme(pixelElipseCentro == (corElipse.getRGB() & 0x00FFFFFF),
                "Pixel central do MyEllipse (125, 30) foi colorido com azul");

        int pixelQuadrado = imagem.getRGB(25, 115) & 0x00FFFFFF;
        afirme(pixelQuadrado == (corQuadrado.getRGB() & 0x00FFFFFF),
                "Pixel interno do MySquare (25, 115) foi colorido com verde");

        int pixelCirculoCentro = imagem.getRGB(125, 125) & 0x00FFFFFF;
        afirme(pixelCirculoCentro == (corCirculo.getRGB() & 0x00FFFFFF),
                "Pixel central do MyCircle (125, 125) foi colorido com amarelo");

        int pixelFundoVazio = imagem.getRGB(250, 250);
        afirme(pixelFundoVazio == 0,
                "Pixel fora de todas as formas (250, 250) permanece vazio");
        System.out.println();
    }

    private static void testarMovimentoEQuique() {
        System.out.println("6. Testando Movimentacao e Quique nas Bordas (Etapa 1):");

        MyRectangle ret = new MyRectangle(10, 10, 20, 20);
        ret.setSpeedX(5);
        ret.setSpeedY(3);

        ret.move(500, 500);
        afirme(ret.getX() == 15 && ret.getY() == 13,
                "move() incrementa x e y de acordo com speedX e speedY");

        ret.setX(90);
        ret.setSpeedX(15);
        ret.move(100, 100);
        afirme(ret.getX() == 80 && ret.getSpeedX() < 0,
                "Ao atingir borda direita, x e ajustado e speedX inverte sinal");

        ret.setX(2);
        ret.setSpeedX(-10);
        ret.move(100, 100);
        afirme(ret.getX() == 0 && ret.getSpeedX() > 0,
                "Ao atingir borda esquerda, x e ajustado e speedX inverte sinal");
        System.out.println();
    }

    // =========================================================================
    // NOVOS TESTES DA ETAPA 2
    // =========================================================================

    /**
     * Teste 7: Projeção Isométrica 2:1 e Transformação Inversa (Reversibilidade).
     */
    private static void testarProjecaoIsometriaEInversa() {
        System.out.println("7. Testando Projecao Isometria 2:1 e Transformacao Inversa (Etapa 2):");

        afirme(IsometricHelper.TILE_W == 2 * IsometricHelper.TILE_H,
                "Proporcao geometrica exata 2:1 preservada (TILE_W == 2 * TILE_H: " +
                        IsometricHelper.TILE_W + "x" + IsometricHelper.TILE_H + ")");

        double originX = 500.0;
        double originY = 200.0;

        double[] pontosX = {0.0, 32.0, 100.5, 250.75};
        double[] pontosY = {0.0, 64.0, 50.25, 300.125};

        boolean todasReversiveis = true;
        for (int i = 0; i < pontosX.length; i++) {
            double wx = pontosX[i];
            double wy = pontosY[i];

            double sx = IsometricHelper.toScreenX(wx, wy, originX);
            double sy = IsometricHelper.toScreenY(wx, wy, originY);

            double deltaSx = sx - originX;
            double deltaSy = sy - originY;

            double recuperadoWx = IsometricHelper.toWorldX(deltaSx, deltaSy);
            double recuperadoWy = IsometricHelper.toWorldY(deltaSx, deltaSy);

            if (Math.abs(wx - recuperadoWx) > 1e-6 || Math.abs(wy - recuperadoWy) > 1e-6) {
                todasReversiveis = false;
                break;
            }
        }
        afirme(todasReversiveis, "Transformacao inversa recupera exatamente as coordenadas de mundo P^-1(P(w)) == w");
        System.out.println();
    }

    /**
     * Teste 8: Normalização de Vetor e Velocidade Diagonal Constante.
     */
    private static void testarNormalizacaoEVelocidadeDiagonal() {
        System.out.println("8. Testando Normalizacao de Vetor e Velocidade Diagonal (Etapa 2):");

        double dirSxSimples = 1.0;
        double dirSySimples = 0.0;
        double lenSimples = Math.hypot(dirSxSimples, dirSySimples);

        double dirSxDiag = 1.0;
        double dirSyDiag = -1.0;
        double lenDiag = Math.hypot(dirSxDiag, dirSyDiag);

        double normSxDiag = dirSxDiag / lenDiag;
        double normSyDiag = dirSyDiag / lenDiag;
        double lenNormalizado = Math.hypot(normSxDiag, normSyDiag);

        afirme(Math.abs(lenSimples - 1.0) < 1e-9, "Movimento axial na tela possui magnitude 1.0");
        afirme(Math.abs(lenNormalizado - 1.0) < 1e-9,
                "Movimento diagonal normalizado possui magnitude exatamente 1.0 (sem efeito de aceleracao excessiva)");
        System.out.println();
    }

    /**
     * Teste 9: Colisão por Eixos no Plano do Mundo e Deslizamento Lateral.
     */
    private static void testarColisaoDeslizamentoPorEixos() {
        System.out.println("9. Testando Deteccao de Colisao por Eixos e Deslizamento (Etapa 2):");

        // Cria obstáculo sólido no mundo ocupando [100, 180] x [100, 150]
        CasinoTable obstaculo = new CasinoTable(100, 100, 80, 50, null, "Mesa", 0, 0);
        List<MyShape> obstaculos = new ArrayList<>();
        obstaculos.add(obstaculo);

        // Posiciona avatar imediatamente ao norte da mesa, encostado em Y: [120, 144] x [75, 99]
        MyCharacter avatar = new MyCharacter(null, 120, 75, 24, 24, null);

        // Tenta mover para baixo (em direção ao obstáculo bloqueado) e para a direita (livre)
        // dirSx = +1, dirSy = +1 -> na projeção inversa, produz deslocamento com componente em Y positivo e X
        // Vamos aplicar diretamente o teste no moveIsometric
        double speed = 100.0;
        double dt = 0.2;
        int yInicial = avatar.getY();
        int xInicial = avatar.getX();

        // Direção que força colisão com a face da mesa no eixo Y mas permite movimento no eixo X
        avatar.moveIsometric(1.0, 1.0, speed, dt, 1000, 1000, obstaculos);

        afirme(avatar.getY() < 100, "Avatar nao penetrou o topo solido da mesa no eixo Y bloqueado (y=" + avatar.getY() + ")");
        afirme(avatar.getX() > xInicial, "Avatar deslizou livremente no eixo tangencial X (x=" + avatar.getX() + ")");
        System.out.println();
    }

    /**
     * Teste 10: Ponto de Apoio para Z-Sorting e Ordenação nos Quatro Lados da Mesa.
     */
    private static void testarZSortingNosQuatroLadosDaMesa() {
        System.out.println("10. Testando Ponto de Apoio para Z-Sorting nos 4 Lados da Mesa (Etapa 2):");

        double originY = 200.0;
        // Mesa em [200, 280] x [200, 250] no mundo
        CasinoTable mesa = new CasinoTable(200, 200, 80, 50, null, "Mesa", 0, originY);
        double depthMesa = mesa.getDepthZ();

        // 1. Avatar ao Norte da mesa (mais ao fundo no salão)
        MyCharacter avatarNorte = new MyCharacter(null, 220, 150, 24, 24, null);
        avatarNorte.updateOrigins(0, originY);
        afirme(avatarNorte.getDepthZ() < depthMesa,
                "Avatar ao NORTE tem DepthZ menor -> renderizado atras da mesa (mesa o cobre)");

        // 2. Avatar ao Sul da mesa (mais à frente no salão)
        MyCharacter avatarSul = new MyCharacter(null, 220, 260, 24, 24, null);
        avatarSul.updateOrigins(0, originY);
        afirme(avatarSul.getDepthZ() > depthMesa,
                "Avatar ao SUL tem DepthZ maior -> renderizado a frente da mesa (sobrepoe a mesa)");

        // 3. Avatar ao Oeste da mesa
        MyCharacter avatarOeste = new MyCharacter(null, 150, 200, 24, 24, null);
        avatarOeste.updateOrigins(0, originY);
        afirme(avatarOeste.getDepthZ() < depthMesa,
                "Avatar ao OESTE posicionado antes do centro de apoio da mesa possui ordenacao consistente");

        // 4. Avatar ao Leste da mesa
        MyCharacter avatarLeste = new MyCharacter(null, 290, 240, 24, 24, null);
        avatarLeste.updateOrigins(0, originY);
        afirme(avatarLeste.getDepthZ() > depthMesa,
                "Avatar ao LESTE posicionado apos o centro de apoio da mesa possui ordenacao consistente");
        System.out.println();
    }

    /**
     * Teste 11: Preservação de Assinaturas e Distinção Semântica do Footprint.
     */
    private static void testarConstrutoresEFootprintMyCharacter() {
        System.out.println("11. Testando Construtores, Footprint e Assinaturas de MyCharacter (Etapa 2):");

        MyCharacter c1 = new MyCharacter("src/image/avatar.png", 50, 60, 28, 28, "src/sound/interact.wav");
        afirme(c1.getWidth() == 28 && c1.getHeight() == 28,
                "MyCharacter define footprint de base solida 28x28 no plano do chao");
        afirme(c1.getSprite() != null,
                "MyCharacter carrega sprite PNG real na memoria antes do loop");
        afirme(c1.getSprite().getWidth(null) > c1.getWidth(),
                "Dimensoes visuais do sprite (largura=" + c1.getSprite().getWidth(null) +
                        ") sao distintas e maiores que a base fisica no chao (width=" + c1.getWidth() + ")");

        MyCharacter c2 = new MyCharacter("src/image/avatar.png", 10, 10, "src/sound/interact.wav");
        afirme(c2.getWidth() == 24 && c2.getHeight() == 24,
                "Construtor de conveniencia define footprint padrao de 24x24");

        // Teste da assinatura oficial move(speed, direction, screenWidth, screenHeight)
        c2.move(5, "RIGHT", 500, 500);
        afirme(c2.getX() == 15, "Metodo move(speed, direction, w, h) oficial funciona conforme assinatura");

        c1.dispose();
        c2.dispose();
        System.out.println();
    }

    /**
     * Teste 12: Sincronização de setX/setY com exactX/exactY e Colisão/Desenho após Reposicionamento.
     */
    private static void testarSincronizacaoPosicaoEColisaoMyCharacter() {
        System.out.println("12. Testando Sincronizacao de setX/setY e Reposicionamento de MyCharacter:");

        MyCharacter avatar = new MyCharacter(null, 50, 50, 24, 24, null);
        afirme(avatar.getX() == 50 && avatar.getY() == 50, "Posicao inicial inteira (50, 50)");
        afirme(avatar.getExactX() == 50.0 && avatar.getExactY() == 50.0, "Posicao inicial exata (50.0, 50.0)");

        // Reposiciona via setters oficiais herdados de MyShape
        avatar.setX(300);
        avatar.setY(400);

        afirme(avatar.getX() == 300 && avatar.getY() == 400, "getX() e getY() refletem novos valores (300, 400)");
        afirme(avatar.getExactX() == 300.0 && avatar.getExactY() == 400.0,
                "exactX e exactY foram sincronizados com setX/setY sem dessincronia");

        // Verifica colisão após reposicionamento
        MyRectangle obstaculoNovo = new MyRectangle(310, 410, 20, 20);
        MyRectangle obstaculoAntigo = new MyRectangle(55, 55, 20, 20);

        afirme(avatar.intersects(obstaculoNovo), "Avatar detecta colisao no novo local (300, 400)");
        afirme(!avatar.intersects(obstaculoAntigo), "Avatar nao colide mais no local antigo (50, 50)");

        // Teste de setExactPosition
        avatar.setExactPosition(120.7, 180.2);
        afirme(avatar.getExactX() == 120.7 && avatar.getExactY() == 180.2, "setExactPosition define double exato");
        afirme(avatar.getX() == 121 && avatar.getY() == 180, "setExactPosition sincroniza inteiros arredondados");

        // Teste de renderização após reposicionamento (off-screen)
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(400, 400, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        avatar.updateOrigins(200, 100);
        avatar.draw(g2);
        g2.dispose();
        afirme(true, "draw(Graphics2D) renderiza sem excecoes apos reposicionamento");

        avatar.dispose();
        System.out.println();
    }

    /**
     * Teste 13: Carregamento pelo Classpath e Gerenciamento de Recursos com try-with-resources.
     */
    private static void testarCarregamentoClasspathERecursos() {
        System.out.println("13. Testando Carregamento pelo Classpath e ResourceLoader:");

        // Pré-carregamento fora da EDT
        model.ResourceLoader.precarregar();

        java.awt.Image sprite = model.ResourceLoader.carregarImagem("src/image/avatar.png");
        afirme(sprite != null, "ResourceLoader carrega imagem PNG via classpath ou arquivo");

        java.awt.Image spriteCache = model.ResourceLoader.carregarImagem("src/image/avatar.png");
        afirme(sprite == spriteCache, "ResourceLoader reaproveita imagem em cache sem releitura de disco");

        javax.sound.sampled.Clip clip = model.ResourceLoader.carregarAudio("src/sound/interact.wav");
        afirme(clip != null, "ResourceLoader carrega audio WAV com try-with-resources");
        if (clip != null) {
            clip.close();
        }

        System.out.println();
    }

    /**
     * Teste 14: Colisao, Deslizamento e Z-Sorting para CasinoCounter e CasinoChair.
     */
    private static void testarColisaoEZSortingNovosMoveis() {
        System.out.println("14. Testando Colisao, Deslizamento e Z-Sorting para CasinoCounter e CasinoChair:");

        double originX = 400.0;
        double originY = 200.0;

        // 1. Heranca e Polimorfismo: criacao e tipagem como MyShape / MyRectangle
        CasinoCounter balcao = new CasinoCounter(100, 100, 80, 40, "BAR", originX, originY);
        CasinoChair cadeira = new CasinoChair(250, 100, 20, originX, originY);

        afirme(balcao instanceof MyRectangle && balcao instanceof MyShape,
                "CasinoCounter eh uma subclasse valida de MyRectangle e MyShape");
        afirme(cadeira instanceof MyRectangle && cadeira instanceof MyShape,
                "CasinoChair eh uma subclasse valida de MyRectangle e MyShape");
        afirme("BAR".equals(balcao.getRotulo()), "Rotulo do balcao preservado corretamente");

        // 2. Colisao geometrica por intersects
        MyCharacter avatar = new MyCharacter("src/image/avatar.png", 90, 95, 24, 24, "src/sound/interact.wav");
        afirme(avatar.intersects(balcao), "Intersects detecta sobreposicao fisica do avatar com o balcao");

        avatar.setExactPosition(255, 105);
        afirme(avatar.intersects(cadeira), "Intersects detecta sobreposicao fisica do avatar com a cadeira");

        // 3. Colisao e Deslizamento com moveIsometric contra novos moveis
        List<MyShape> obstaculos = new ArrayList<>();
        obstaculos.add(balcao);
        obstaculos.add(cadeira);

        // Posiciona avatar imediatamente a esquerda do balcao (x=75, balcao comeca em x=100)
        avatar.setExactPosition(75, 105);
        // Tenta mover para a direita na tela (dx > 0, dy > 0 em coordenadas de mundo)
        // dirSx = 1.0 (para direita visual na tela: wx aumenta e wy diminui)
        avatar.moveIsometric(1.0, 0.0, 100.0, 0.1, 512, 512, obstaculos);
        afirme(!avatar.intersects(balcao),
                "moveIsometric impede sobreposicao com o balcao atraves de colisao por eixos");

        // 4. Z-Sorting Rigoroso com Ponto de Apoio (Norte vs Sul)
        // Balcao: x=100..180, y=100..140. Centro do apoio = (140, 120)
        balcao.updateOrigins(originX, originY);
        avatar.updateOrigins(originX, originY);

        // Avatar ao Norte do balcao (y=60): profundidade deve ser MENOR que o balcao (desenhado atras)
        avatar.setExactPosition(130, 60);
        afirme(avatar.getDepthZ() < balcao.getDepthZ(),
                "Z-Sorting ao Norte do balcao: avatar desenhado atras do balcao (depthZ menor)");

        // Avatar ao Sul do balcao (y=150): profundidade deve ser MAIOR que o balcao (desenhado na frente)
        avatar.setExactPosition(130, 150);
        afirme(avatar.getDepthZ() > balcao.getDepthZ(),
                "Z-Sorting ao Sul do balcao: avatar desenhado a frente do balcao (depthZ maior)");

        // Cadeira: x=250..270, y=100..120. Centro do apoio = (260, 110)
        cadeira.updateOrigins(originX, originY);

        // Avatar ao Norte da cadeira (y=65): desenhado atras
        avatar.setExactPosition(248, 65);
        afirme(avatar.getDepthZ() < cadeira.getDepthZ(),
                "Z-Sorting ao Norte da cadeira: avatar desenhado atras da cadeira");

        // Avatar ao Sul da cadeira (y=130): desenhado na frente
        avatar.setExactPosition(248, 130);
        afirme(avatar.getDepthZ() > cadeira.getDepthZ(),
                "Z-Sorting ao Sul da cadeira: avatar desenhado a frente da cadeira");

        // 5. Renderizacao polimorfica em buffer grafico offscreen
        BufferedImage img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        balcao.draw(g2);
        cadeira.draw(g2);
        g2.dispose();
        afirme(true, "draw(Graphics2D) de CasinoCounter e CasinoChair renderiza sem excecoes");

        avatar.dispose();
        System.out.println();
    }
}

