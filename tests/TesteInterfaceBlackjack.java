package tests;

import model.BlackjackEngine;
import model.Card;
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
import java.util.Collections;
import java.util.List;

/**
 * Bateria de testes automatizados da interface gráfica da mesa de Blackjack.
 *
 * Validações obrigatórias:
 * 1. Validação de aposta: rejeita zero, negativos, ímpares e acima do saldo.
 * 2. Proteção contra overflow aritmético de long nos botões de fichas.
 * 3. Preservação do estado da rodada ao alternar entre salão e mesa (sem perda de progresso).
 * 4. Injeção de dependência via setBlackjackEngine com implementação alternativa de teste,
 *    comprovando que desenho, cliques e teclas usam o motor injetado sem referências residuais ao mock.
 * 5. Indicação discreta de modo de demonstração restrita ao BlackjackMockSimulator.
 */
public class TesteInterfaceBlackjack {

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("  TESTES DE INTERFACE, CONTROLES E INJECAO DA MESA");
        System.out.println("====================================================\n");

        testarValidacoesApostaEOverflow();
        testarControleTeclasFisicasIndependentes();
        testarPreservacaoRodadaAoAlternarTelas();
        testarInjecaoMotorAlternativo();

        System.out.println("\n----------------------------------------------------");
        System.out.println("RESULTADO: TODOS OS TESTES DA INTERFACE PASSARAM! [OK]");
        System.out.println("----------------------------------------------------");
    }

    private static void testarValidacoesApostaEOverflow() {
        System.out.println("1. Testando Validacoes de Aposta e Protecao Contra Overflow:");

        BlackjackMockSimulator mock = new BlackjackMockSimulator();
        BlackjackTableView view = new BlackjackTableView(mock);

        // Aposta padrão inicial
        afirme(view.getApostaPreparada() == 100L, "Aposta inicial padrao e 100 fichas");

        // Rejeição de zero
        boolean aceitouZero = view.setApostaPreparada(0L);
        afirme(!aceitouZero, "Aposta de 0 fichas foi rejeitada");
        afirme(view.getApostaPreparada() == 100L, "Valor anterior foi preservado apos tentativa de aposta zero");

        // Rejeição de negativos
        boolean aceitouNegativo = view.setApostaPreparada(-50L);
        afirme(!aceitouNegativo, "Aposta negativa (-50) foi rejeitada");

        // Rejeição de valores ímpares (regra 3:2 sem arredondamento)
        boolean aceitouImpar = view.setApostaPreparada(101L);
        afirme(!aceitouImpar, "Aposta impar (101) foi rejeitada");

        // Rejeição de valor acima do saldo virtual
        long saldo = mock.getSaldoVirtual(); // 50.000
        boolean aceitouAcimaSaldo = view.setApostaPreparada(saldo + 2L);
        afirme(!aceitouAcimaSaldo, "Aposta acima do saldo virtual foi rejeitada");

        // Aceitação de aposta par válida dentro do saldo
        boolean aceitouValida = view.setApostaPreparada(2500L);
        afirme(aceitouValida, "Aposta par valida (2500) foi aceita");
        afirme(view.getApostaPreparada() == 2500L, "Valor da aposta foi atualizado para 2500");

        // Proteção contra overflow de long na adição de fichas
        view.adicionarAposta(Long.MAX_VALUE);
        afirme(view.getApostaPreparada() == 2500L,
                "Adicao com overflow de long foi interceptada com seguranca sem alterar a aposta");
        afirme(view.getMensagemAlerta().contains("limite"),
                "Mensagem de alerta sobre limite numerico foi exibida");

        // Teste de Limpar Aposta e Bloqueio de Aposta Zero
        view.limparAposta();
        afirme(view.getApostaPreparada() == 0L, "Botao Limpar Aposta zerou a selecao (aposta = 0)");

        boolean apostouZero = view.processarTecla("ESPACO");
        afirme(mock.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA,
                "Tentativa de apostar zero foi bloqueada sem iniciar a rodada");
        afirme(view.getMensagemAlerta().contains("maior que zero"),
                "Alerta de aposta estritamente maior que zero foi exibido");

        // Ficha adicionada após limpar recupera aposta válida
        view.adicionarAposta(50L);
        afirme(view.getApostaPreparada() == 50L, "Adicionar ficha de 50 apos limpar define aposta = 50");
        System.out.println();
    }

    private static void testarControleTeclasFisicasIndependentes() {
        System.out.println("2. Testando Controle Independente de Teclas Fisicas (W + Seta Cima):");

        ScreenSaver painel = new ScreenSaver();

        // 1. Pressiona W e UP simultaneamente
        painel.simularPressionarTecla("W");
        painel.simularPressionarTecla("UP");
        afirme(painel.isMovendoParaCima(), "Segurando W + Seta UP: movimento para cima ativo");

        // 2. Solta apenas W; a Seta UP continua pressionada
        painel.simularSoltarTecla("W");
        afirme(painel.isMovendoParaCima(), "Soltando W mas mantendo Seta UP: movimento para cima continua ativo!");

        // 3. Solta a Seta UP; o movimento agora cessa
        painel.simularSoltarTecla("UP");
        afirme(!painel.isMovendoParaCima(), "Soltando Seta UP: movimento para cima cessou completamente");

        // 4. Tecla S no salão move para baixo
        painel.simularPressionarTecla("S");
        afirme(painel.isMovendoParaBaixo(), "Tecla S no salao aciona movimento para baixo");
        painel.simularSoltarTecla("S");
        afirme(!painel.isMovendoParaBaixo(), "Soltar tecla S cessa movimento para baixo");

        painel.pararLoop();
        System.out.println();
    }

    private static void testarPreservacaoRodadaAoAlternarTelas() {
        System.out.println("3. Testando Preservacao de Rodada em Andamento ao Alternar Telas:");

        ScreenSaver painel = new ScreenSaver();
        BlackjackEngine motorOriginal = painel.getBlackjackEngine();
        afirme(motorOriginal != null, "Motor de Blackjack inicializado no painel");

        // Inicia uma partida na mesa
        boolean iniciou = motorOriginal.iniciarPartida(200L);
        afirme(iniciou, "Partida iniciada com aposta de 200 fichas");
        afirme(motorOriginal.getEstadoRodada() == RoundState.TURNO_JOGADOR, "Rodada esta no TURNO_JOGADOR");
        int cartasJogadorAntes = motorOriginal.getMaoJogador().size();
        afirme(cartasJogadorAntes == 2, "Jogador recebeu 2 cartas");

        // Simula transição para a mesa
        painel.setEstadoAtual(ScreenSaver.GameState.MENU_BLACKJACK);
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_BLACKJACK, "Painel abriu a tela da mesa");

        // Simula saída para o salão (pressionando B ou botão voltar)
        painel.setEstadoAtual(ScreenSaver.GameState.SALAO);
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.SALAO, "Painel retornou ao salao");

        // Simula reabertura da mesa (pressionando E próximo à mesa)
        painel.setEstadoAtual(ScreenSaver.GameState.MENU_BLACKJACK);
        afirme(painel.getEstadoAtual() == ScreenSaver.GameState.MENU_BLACKJACK, "Painel reabriu a tela da mesa");

        // Garante que o motor de jogo continua exatamente a mesma instância
        BlackjackEngine motorAposTransicao = painel.getBlackjackEngine();
        afirme(motorOriginal == motorAposTransicao, "A mesma instancia do motor e preservada entre transicoes");
        afirme(motorAposTransicao.getEstadoRodada() == RoundState.TURNO_JOGADOR,
                "Estado TURNO_JOGADOR continua ativo, sem reset da rodada");
        afirme(motorAposTransicao.getMaoJogador().size() == cartasJogadorAntes,
                "Cartas da mao do jogador foram preservadas intactas");
        afirme(motorAposTransicao.getApostaAtual() == 200L,
                "Valor da aposta em andamento foi preservado intacto");

        painel.pararLoop();
        System.out.println();
    }

    private static void testarInjecaoMotorAlternativo() {
        System.out.println("3. Testando setBlackjackEngine com Motor Alternativo de Teste:");

        ScreenSaver painel = new ScreenSaver();
        BlackjackEngine motorAntigo = painel.getBlackjackEngine();

        // Cria implementação alternativa de teste de BlackjackEngine
        MotorAlternativoTeste motorInjetado = new MotorAlternativoTeste();

        // Injeta no painel principal
        painel.setBlackjackEngine(motorInjetado);

        afirme(painel.getBlackjackEngine() == motorInjetado,
                "Painel passou a usar diretamente a nova instancia de BlackjackEngine");
        afirme(painel.getBlackjackTableView().getEngine() == motorInjetado,
                "BlackjackTableView foi recriada apontando para o motor injetado");

        // Testa renderização com o novo motor
        BufferedImage buffer = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = buffer.createGraphics();
        painel.getBlackjackTableView().render(g2d, 1000, 700);
        g2d.dispose();
        afirme(motorInjetado.chamadasGetSaldo > 0,
                "Renderizacao consultou o saldo do motor injetado (Chamadas: " + motorInjetado.chamadasGetSaldo + ")");

        // Testa acionamento de ações no motor injetado via atalhos
        motorInjetado.setEstado(RoundState.TURNO_JOGADOR);
        painel.getBlackjackTableView().processarTecla("HIT");
        afirme(motorInjetado.chamadasPedirCarta == 1,
                "Comando de tecla HIT acionou pedirCarta no motor injetado");

        painel.getBlackjackTableView().processarTecla("STAND");
        afirme(motorInjetado.chamadasParar == 1,
                "Comando de tecla STAND acionou parar no motor injetado");

        // Verifica que o motor antigo não recebeu chamadas
        afirme(motorAntigo.getEstadoRodada() == RoundState.AGUARDANDO_APOSTA,
                "Motor antigo permaneceu isolado sem chamadas espúrias");

        // Testa renderização do Salão (HUD) consultando o saldo do motor injetado
        int chamadasAntesHUD = motorInjetado.chamadasGetSaldo;
        painel.setSize(new Dimension(1000, 700));
        BufferedImage bufferSalao = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gSalao = bufferSalao.createGraphics();
        painel.paint(gSalao);
        gSalao.dispose();
        afirme(motorInjetado.chamadasGetSaldo > chamadasAntesHUD,
                "HUD do salao consultou diretamente o saldo do motor injetado (" + motorInjetado.getSaldoVirtual() + " fichas)");

        painel.pararLoop();
        System.out.println();
    }

    private static void afirme(boolean condicao, String descricao) {
        if (condicao) {
            System.out.println("  [OK] " + descricao);
        } else {
            System.err.println("  [FALHA] " + descricao);
            System.exit(1);
        }
    }

    /**
     * Motor mock alternativo para validar injeção de dependência via setBlackjackEngine.
     */
    private static class MotorAlternativoTeste implements BlackjackEngine {
        int chamadasGetSaldo = 0;
        int chamadasPedirCarta = 0;
        int chamadasParar = 0;
        private RoundState estado = RoundState.AGUARDANDO_APOSTA;
        private final List<Card> maoJogador = new ArrayList<>();
        private final List<Card> dealerVisivel = new ArrayList<>();

        public MotorAlternativoTeste() {
            maoJogador.add(new Card(Suit.COPAS, Rank.DEZ));
            maoJogador.add(new Card(Suit.ESPADAS, Rank.NOVE));
            dealerVisivel.add(new Card(Suit.OUROS, Rank.VALETE));
        }

        public void setEstado(RoundState estado) {
            this.estado = estado;
        }

        @Override
        public boolean iniciarPartida(long aposta) {
            this.estado = RoundState.TURNO_JOGADOR;
            return true;
        }

        @Override
        public boolean pedirCarta() {
            chamadasPedirCarta++;
            return true;
        }

        @Override
        public void parar() {
            chamadasParar++;
            this.estado = RoundState.FINALIZADA;
        }

        @Override
        public RoundState getEstadoRodada() {
            return estado;
        }

        @Override
        public RoundResult getResultadoRodada() {
            return (estado == RoundState.FINALIZADA) ? RoundResult.JOGADOR_VITORIA : null;
        }

        @Override
        public List<Card> getMaoJogador() {
            return Collections.unmodifiableList(maoJogador);
        }

        @Override
        public List<Card> getCartasVisiveisDealer() {
            return Collections.unmodifiableList(dealerVisivel);
        }

        @Override
        public boolean hasCartaOcultaDealer() {
            return estado == RoundState.TURNO_JOGADOR;
        }

        @Override
        public int getPontosJogador() {
            return 19;
        }

        @Override
        public int getPontosDealerVisiveis() {
            return 10;
        }

        @Override
        public long getSaldoVirtual() {
            chamadasGetSaldo++;
            return 999_998L;
        }

        @Override
        public long getApostaAtual() {
            return 100L;
        }
    }
}

