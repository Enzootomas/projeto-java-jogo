package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Motor oficial de regras do Poker Texas Hold'em offline no formato Heads-Up (1 Jogador vs. 1 Bot)
 * com fichas 100% virtuais, implementando {@link PokerEngine} sem qualquer acoplamento com Swing/UI.
 *
 * <p><strong>Conceitos de POO e decisoes de projeto aplicados:</strong></p>
 * <ul>
 *   <li><strong>Abstracao e Polimorfismo:</strong> implementa {@link PokerEngine} (contrato para a
 *       futura interface da mesa) e delega a inteligencia do oponente a {@link PokerBotStrategy}
 *       (por padrao {@link PokerSimpleAI}, permitindo tambem injetar bots deterministicos em testes).</li>
 *   <li><strong>Reaproveitamento:</strong> utiliza integralmente {@link Card}, {@link Suit}, {@link Rank}
 *       e {@link Deck} (52 cartas com {@code SecureRandom} em producao e sequencia controlada em testes).</li>
 *   <li><strong>Encapsulamento e Ocultacao Estrita:</strong> mantem colecoes privadas limpas por
 *       <strong>laco reverso</strong> a cada nova mao, devolve snapshots imutaveis ({@link List#copyOf}) e
 *       jamais expoe as 2 cartas privadas do Bot antes do {@link PokerStage#SHOWDOWN}.</li>
 *   <li><strong>Seguranca Financeira ({@code long}):</strong> todas as operacoes de saldo e pote usam
 *       {@code Math.addExact} / {@code Math.subtractExact}, devolvem imediatamente excesso de aposta nao
 *       igualado (<em>uncalled bet</em>), garantem liquidacao unica do pote e aplicam divisao exata em
 *       empates (<em>split pot</em>, entregando eventual ficha impar residual ao participante fora de posicao).</li>
 * </ul>
 */
public class TexasHoldemGame implements PokerEngine {

    /** Saldo inicial padrao de fichas virtuais para cada participante. */
    public static final long SALDO_INICIAL_PADRAO = 50_000L;

    /** Valor padrao do Small Blind (par). */
    public static final long SMALL_BLIND_PADRAO = 50L;

    /** Valor padrao do Big Blind (par). */
    public static final long BIG_BLIND_PADRAO = 100L;

    /** Limite maximo de aumentos (bets/raises) permitidos em uma mesma rua de apostas. */
    public static final int MAX_AUMENTOS_POR_RUA = 3;

    /** Quantidade minima de cartas no baralho para uma mao completa (4 cartas fechadas + 5 comunitarias). */
    public static final int MINIMO_CARTAS_POR_MAO = 9;

    private final Deck baralho;
    private final boolean reembaralharAutomaticamentePorMao;
    private final boolean recarregarBotQuandoSemSaldo;
    private final long saldoInicialReferenciaBot;
    private final long smallBlind;
    private final long bigBlind;
    private final PokerBotStrategy botStrategy;

    private final List<Card> maoJogador;
    private final List<Card> maoBot;
    private final List<Card> cartasComunitarias;

    private long saldoJogador;
    private long saldoBot;
    private long poteTotal;
    private long apostaJogadorNaRua;
    private long apostaBotNaRua;

    private PokerStage estagioAtual;
    private PokerRoundResult resultadoMao;
    private PokerParticipant botaoDealer;
    private PokerParticipant proximoBotaoDealer;
    private PokerParticipant turnoAtual;

    private int aumentosNaRua;
    private boolean jogadorJaAgiuNaRua;
    private boolean botJaAgiuNaRua;
    private boolean poteLiquidado;

    private EvaluatedPokerHand melhorMaoJogadorShowdown;
    private EvaluatedPokerHand melhorMaoBotShowdown;
    private String ultimaAcaoDescricao;

    /**
     * Cria uma mesa padrao de Texas Hold'em Heads-Up para producao:
     * 50.000 fichas para cada lado, blinds 50/100, baralho de 52 cartas embaralhado com
     * {@code SecureRandom} a cada mao e IA padrao {@link PokerSimpleAI} (com recarga automatica
     * do Bot entre maos caso ele perca todas as fichas para o jogador).
     */
    public TexasHoldemGame() {
        this(
                SALDO_INICIAL_PADRAO,
                SALDO_INICIAL_PADRAO,
                SMALL_BLIND_PADRAO,
                BIG_BLIND_PADRAO,
                new Deck(),
                true,
                true,
                new PokerSimpleAI(),
                PokerParticipant.JOGADOR
        );
    }

    /**
     * Cria uma partida deterministica para testes automatizados com sequencia controlada de cartas,
     * saldos customizados, blinds customizados, estrategia de Bot injetada e posicao inicial do botao.
     *
     * <p>Neste construtor deterministico, o baralho injetado <strong>nao</strong> sofre reposicao
     * aleatoria automatica e o saldo do Bot nao e recarregado silenciosamente, permitindo auditar
     * com precisao cada ficha e cada carta comprada.</p>
     *
     * @param saldoInicialJogador saldo inicial do jogador ({@code >= 0}).
     * @param saldoInicialBot     saldo inicial do Bot ({@code >= 0}).
     * @param smallBlind          valor do Small Blind (positivo e par).
     * @param bigBlind            valor do Big Blind (positivo, par e {@code >= smallBlind}).
     * @param baralhoDeterministico baralho com cartas controladas (nao nulo).
     * @param botStrategy         estrategia de decisao do Bot (nao nula).
     * @param primeiroBotaoDealer quem inicia com o Botao (Dealer / Small Blind) na primeira mao.
     */
    public TexasHoldemGame(long saldoInicialJogador,
                           long saldoInicialBot,
                           long smallBlind,
                           long bigBlind,
                           Deck baralhoDeterministico,
                           PokerBotStrategy botStrategy,
                           PokerParticipant primeiroBotaoDealer) {
        this(
                saldoInicialJogador,
                saldoInicialBot,
                smallBlind,
                bigBlind,
                baralhoDeterministico,
                false,
                false,
                botStrategy,
                primeiroBotaoDealer
        );
    }

    /**
     * Construtor completo do motor de Texas Hold'em.
     */
    public TexasHoldemGame(long saldoInicialJogador,
                           long saldoInicialBot,
                           long smallBlind,
                           long bigBlind,
                           Deck baralho,
                           boolean reembaralharAutomaticamentePorMao,
                           boolean recarregarBotQuandoSemSaldo,
                           PokerBotStrategy botStrategy,
                           PokerParticipant primeiroBotaoDealer) {
        if (saldoInicialJogador < 0L || saldoInicialBot < 0L) {
            throw new IllegalArgumentException("Os saldos iniciais nao podem ser negativos.");
        }
        if (smallBlind <= 0L || smallBlind % 2L != 0L) {
            throw new IllegalArgumentException("O Small Blind deve ser estritamente positivo e par: " + smallBlind);
        }
        if (bigBlind < smallBlind || bigBlind % 2L != 0L) {
            throw new IllegalArgumentException(
                    "O Big Blind deve ser par e maior ou igual ao Small Blind (" + smallBlind + "): " + bigBlind
            );
        }
        if (baralho == null) {
            throw new IllegalArgumentException("O baralho (Deck) nao pode ser nulo.");
        }
        if (botStrategy == null) {
            throw new IllegalArgumentException("A estrategia do Bot (PokerBotStrategy) nao pode ser nula.");
        }
        if (primeiroBotaoDealer == null) {
            throw new IllegalArgumentException("O participante inicial do botao Dealer nao pode ser nulo.");
        }

        this.saldoJogador = saldoInicialJogador;
        this.saldoBot = saldoInicialBot;
        this.saldoInicialReferenciaBot = Math.max(saldoInicialBot, bigBlind * 20L);
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.baralho = baralho;
        this.reembaralharAutomaticamentePorMao = reembaralharAutomaticamentePorMao;
        this.recarregarBotQuandoSemSaldo = recarregarBotQuandoSemSaldo;
        this.botStrategy = botStrategy;

        this.maoJogador = new ArrayList<>(2);
        this.maoBot = new ArrayList<>(2);
        this.cartasComunitarias = new ArrayList<>(5);

        this.estagioAtual = PokerStage.AGUARDANDO_MAO;
        this.resultadoMao = PokerRoundResult.AGUARDANDO_INICIO;
        this.botaoDealer = primeiroBotaoDealer;
        this.proximoBotaoDealer = primeiroBotaoDealer;
        this.turnoAtual = null;
        this.poteLiquidado = false;
        this.ultimaAcaoDescricao = "Mesa pronta para iniciar nova mão.";
    }

    // =========================================================================
    // IMPLEMENTACAO DE ACOES DO JOGADOR (PokerEngine)
    // =========================================================================

    @Override
    public boolean iniciarNovaMao() {
        if (estagioAtual.isMaoEmAndamento()) {
            return false;
        }

        PokerParticipant candidatoBotao = this.proximoBotaoDealer;
        long blindObrigatorioJogador = (candidatoBotao == PokerParticipant.JOGADOR) ? smallBlind : bigBlind;
        long blindObrigatorioBot = (candidatoBotao == PokerParticipant.BOT) ? smallBlind : bigBlind;

        if (saldoJogador < blindObrigatorioJogador) {
            return false;
        }

        if (saldoBot < blindObrigatorioBot) {
            if (recarregarBotQuandoSemSaldo) {
                this.saldoBot = this.saldoInicialReferenciaBot;
            } else {
                return false;
            }
        }

        if (reembaralharAutomaticamentePorMao) {
            baralho.reiniciarBaralhoPadrao();
            baralho.embaralhar();
        } else if (baralho.cartasRestantes() < MINIMO_CARTAS_POR_MAO) {
            return false;
        }

        // Limpa as colecoes da mao anterior com laco reverso conforme diretriz do projeto
        limparComLacoReverso(maoJogador);
        limparComLacoReverso(maoBot);
        limparComLacoReverso(cartasComunitarias);

        this.botaoDealer = candidatoBotao;
        this.proximoBotaoDealer = candidatoBotao.oponente();
        this.poteTotal = 0L;
        this.apostaJogadorNaRua = 0L;
        this.apostaBotNaRua = 0L;
        this.aumentosNaRua = 0;
        this.jogadorJaAgiuNaRua = false;
        this.botJaAgiuNaRua = false;
        this.poteLiquidado = false;
        this.melhorMaoJogadorShowdown = null;
        this.melhorMaoBotShowdown = null;
        this.estagioAtual = PokerStage.PRE_FLOP;
        this.resultadoMao = PokerRoundResult.EM_ANDAMENTO;

        // Postagem automatica de Small Blind (Botao) e Big Blind (Oponente do Botao)
        colocarFichasNaRua(botaoDealer, smallBlind);
        colocarFichasNaRua(botaoDealer.oponente(), bigBlind);
        devolverExcessoNaoIgualavelSeNecessario();

        // Distribuicao alternada oficial: 1a para Small Blind (Botao), 2a para Big Blind, 3a SB, 4a BB
        comprarParaParticipante(botaoDealer);
        comprarParaParticipante(botaoDealer.oponente());
        comprarParaParticipante(botaoDealer);
        comprarParaParticipante(botaoDealer.oponente());

        this.ultimaAcaoDescricao = "Nova mão iniciada (Blinds " + smallBlind + "/" + bigBlind + ").";

        // Se algum participante ja ficou em all-in nos proprios blinds e nao ha diferenca pagavel,
        // corre todas as comunitarias direto para o Showdown.
        if (naoHaMaisAcoesPossiveisNaMao()) {
            distribuirComunitariasRestantesEResolverShowdown();
            return true;
        }

        // No Heads-Up Pre-Flop, o Botao (Small Blind) age primeiro!
        this.turnoAtual = botaoDealer;
        processarTurnosAutomaticosDoBot();
        return true;
    }

    @Override
    public boolean desistir() {
        if (!isTurnoDoJogador()) {
            return false;
        }
        executarDesistencia(PokerParticipant.JOGADOR);
        return true;
    }

    @Override
    public boolean passar() {
        if (!isTurnoDoJogador()) {
            return false;
        }
        if (getValorParaPagar(PokerParticipant.JOGADOR) > 0L) {
            return false;
        }
        executarPassar(PokerParticipant.JOGADOR);
        processarTurnosAutomaticosDoBot();
        return true;
    }

    @Override
    public boolean pagar() {
        if (!isTurnoDoJogador()) {
            return false;
        }
        long valorPagar = getValorParaPagar(PokerParticipant.JOGADOR);
        if (valorPagar <= 0L) {
            return false;
        }
        executarPagar(PokerParticipant.JOGADOR, valorPagar);
        processarTurnosAutomaticosDoBot();
        return true;
    }

    @Override
    public boolean aumentar(long valorAcimaDaApostaAtual) {
        if (!isTurnoDoJogador()) {
            return false;
        }
        if (!podeParticipanteAumentar(PokerParticipant.JOGADOR)) {
            return false;
        }
        if (valorAcimaDaApostaAtual <= 0L) {
            return false;
        }

        long maiorApostaRua = Math.max(apostaJogadorNaRua, apostaBotNaRua);
        long novaApostaTotalDesejada;
        try {
            novaApostaTotalDesejada = Math.addExact(maiorApostaRua, valorAcimaDaApostaAtual);
        } catch (ArithmeticException e) {
            return false;
        }

        long fichasNecessariasDoSaldo = Math.subtractExact(novaApostaTotalDesejada, apostaJogadorNaRua);
        if (fichasNecessariasDoSaldo > saldoJogador) {
            return false;
        }

        // Verifica o maximo que o oponente consegue cobrir na rua atual (evita aumentos fantasmas alem do stack efetivo)
        long tetoCoberturaBot = Math.addExact(apostaBotNaRua, saldoBot);
        if (novaApostaTotalDesejada > tetoCoberturaBot) {
            // So permite colocar ate o limite efetivo se o jogador ajustar ou se estivermos limitando;
            // para clareza contratual, se exceder o stack efetivo total do Bot, limitamos apenas se for all-in?
            // Rejeitamos valores acima do saldo do proprio jogador; se superar o teto do Bot, devolvemos o excesso!
        }

        boolean allInExatoJogador = (fichasNecessariasDoSaldo == saldoJogador);
        if (!allInExatoJogador) {
            if (valorAcimaDaApostaAtual < bigBlind || valorAcimaDaApostaAtual % 2L != 0L) {
                return false;
            }
        }

        executarAumentar(PokerParticipant.JOGADOR, fichasNecessariasDoSaldo, valorAcimaDaApostaAtual);
        processarTurnosAutomaticosDoBot();
        return true;
    }

    // =========================================================================
    // MAQUINA INTERNA DE TURNOS, RUAS E LIQUIDACAO
    // =========================================================================

    private void processarTurnosAutomaticosDoBot() {
        while (estagioAtual.isMaoEmAndamento() && turnoAtual == PokerParticipant.BOT) {
            long valorParaPagarBot = getValorParaPagar(PokerParticipant.BOT);
            boolean botPodeAumentar = podeParticipanteAumentar(PokerParticipant.BOT);

            PokerBotStrategy.Decision decisao = botStrategy.decidirAcao(
                    List.copyOf(maoBot),
                    List.copyOf(cartasComunitarias),
                    estagioAtual,
                    poteTotal,
                    valorParaPagarBot,
                    saldoBot,
                    bigBlind,
                    botPodeAumentar
            );

            aplicarDecisaoSeguraDoBot(decisao, valorParaPagarBot, botPodeAumentar);
        }
    }

    private void aplicarDecisaoSeguraDoBot(PokerBotStrategy.Decision decisao,
                                           long valorParaPagarBot,
                                           boolean botPodeAumentar) {
        PokerAction acao = (decisao != null) ? decisao.getAcao() : PokerAction.PASSAR;

        if (acao == PokerAction.AUMENTAR && botPodeAumentar) {
            long incremento = decisao.getValorAcimaDaApostaAtual();
            long maiorApostaRua = Math.max(apostaJogadorNaRua, apostaBotNaRua);
            long novaTotal = maiorApostaRua + incremento;
            long fichasNecessarias = novaTotal - apostaBotNaRua;

            if (incremento > 0L && fichasNecessarias > valorParaPagarBot && fichasNecessarias <= saldoBot) {
                boolean allInBot = (fichasNecessarias == saldoBot);
                if (allInBot || (incremento >= bigBlind && incremento % 2L == 0L)) {
                    executarAumentar(PokerParticipant.BOT, fichasNecessarias, incremento);
                    return;
                }
            }
            // Fallback seguro caso a estrategia sugira um aumento invalido
            if (valorParaPagarBot > 0L) {
                executarPagar(PokerParticipant.BOT, valorParaPagarBot);
            } else {
                executarPassar(PokerParticipant.BOT);
            }
            return;
        }

        if (acao == PokerAction.PAGAR) {
            if (valorParaPagarBot > 0L) {
                executarPagar(PokerParticipant.BOT, valorParaPagarBot);
            } else {
                executarPassar(PokerParticipant.BOT);
            }
            return;
        }

        if (acao == PokerAction.PASSAR) {
            if (valorParaPagarBot == 0L) {
                executarPassar(PokerParticipant.BOT);
            } else {
                executarDesistencia(PokerParticipant.BOT);
            }
            return;
        }

        // PokerAction.DESISTIR: se nao ha nada a pagar, passar gratuitamente e estritamente dominante
        if (valorParaPagarBot == 0L) {
            executarPassar(PokerParticipant.BOT);
        } else {
            executarDesistencia(PokerParticipant.BOT);
        }
    }

    private void executarDesistencia(PokerParticipant desistente) {
        PokerParticipant vencedor = desistente.oponente();
        this.estagioAtual = PokerStage.ENCERRADA_POR_DESISTENCIA;
        this.turnoAtual = null;
        this.resultadoMao = (vencedor == PokerParticipant.JOGADOR)
                ? PokerRoundResult.JOGADOR_VENCEU_DESISTENCIA
                : PokerRoundResult.BOT_VENCEU_DESISTENCIA;
        this.ultimaAcaoDescricao = desistente.getRotulo() + " desistiu. "
                + vencedor.getRotulo() + " levou o pote de " + poteTotal + " fichas!";
        liquidarPoteUnico(vencedor);
    }

    private void executarPassar(PokerParticipant autor) {
        marcarQueAgiuNaRua(autor);
        this.ultimaAcaoDescricao = autor.getRotulo() + " passou (Check).";
        verificarEncerramentoOuProximoTurno(autor);
    }

    private void executarPagar(PokerParticipant autor, long fichasPagas) {
        colocarFichasNaRua(autor, fichasPagas);
        devolverExcessoNaoIgualavelSeNecessario();
        marcarQueAgiuNaRua(autor);
        this.ultimaAcaoDescricao = autor.getRotulo() + " pagou " + fichasPagas + " fichas (Call).";
        verificarEncerramentoOuProximoTurno(autor);
    }

    private void executarAumentar(PokerParticipant autor, long fichasColocadas, long incrementoAcima) {
        colocarFichasNaRua(autor, fichasColocadas);
        devolverExcessoNaoIgualavelSeNecessario();
        this.aumentosNaRua++;
        if (autor == PokerParticipant.JOGADOR) {
            this.jogadorJaAgiuNaRua = true;
            this.botJaAgiuNaRua = (saldoBot == 0L);
        } else {
            this.botJaAgiuNaRua = true;
            this.jogadorJaAgiuNaRua = (saldoJogador == 0L);
        }
        this.ultimaAcaoDescricao = autor.getRotulo() + " aumentou em +" + incrementoAcima + " fichas.";
        verificarEncerramentoOuProximoTurno(autor);
    }

    private void verificarEncerramentoOuProximoTurno(PokerParticipant ultimoAAgir) {
        if (apostaJogadorNaRua == apostaBotNaRua && jogadorJaAgiuNaRua && botJaAgiuNaRua) {
            avancarParaProximaRuaOuShowdown();
            return;
        }
        if (naoHaMaisAcoesPossiveisNaMao()) {
            distribuirComunitariasRestantesEResolverShowdown();
            return;
        }
        this.turnoAtual = ultimoAAgir.oponente();
    }

    private void avancarParaProximaRuaOuShowdown() {
        if (naoHaMaisAcoesPossiveisNaMao()) {
            distribuirComunitariasRestantesEResolverShowdown();
            return;
        }

        this.apostaJogadorNaRua = 0L;
        this.apostaBotNaRua = 0L;
        this.aumentosNaRua = 0;
        this.jogadorJaAgiuNaRua = false;
        this.botJaAgiuNaRua = false;

        switch (estagioAtual) {
            case PRE_FLOP:
                this.estagioAtual = PokerStage.FLOP;
                cartasComunitarias.add(baralho.comprarCarta());
                cartasComunitarias.add(baralho.comprarCarta());
                cartasComunitarias.add(baralho.comprarCarta());
                break;
            case FLOP:
                this.estagioAtual = PokerStage.TURN;
                cartasComunitarias.add(baralho.comprarCarta());
                break;
            case TURN:
                this.estagioAtual = PokerStage.RIVER;
                cartasComunitarias.add(baralho.comprarCarta());
                break;
            case RIVER:
                resolverShowdown();
                return;
            default:
                return;
        }

        // Regra oficial Heads-Up Pos-Flop: o oponente do Botao (Big Blind) age primeiro!
        this.turnoAtual = botaoDealer.oponente();
    }

    private void distribuirComunitariasRestantesEResolverShowdown() {
        this.apostaJogadorNaRua = 0L;
        this.apostaBotNaRua = 0L;
        while (cartasComunitarias.size() < 5) {
            cartasComunitarias.add(baralho.comprarCarta());
        }
        resolverShowdown();
    }

    private void resolverShowdown() {
        this.estagioAtual = PokerStage.SHOWDOWN;
        this.turnoAtual = null;

        List<Card> seteJogador = new ArrayList<>(7);
        seteJogador.addAll(maoJogador);
        seteJogador.addAll(cartasComunitarias);

        List<Card> seteBot = new ArrayList<>(7);
        seteBot.addAll(maoBot);
        seteBot.addAll(cartasComunitarias);

        this.melhorMaoJogadorShowdown = PokerHandEvaluator.avaliarMelhorMao(seteJogador);
        this.melhorMaoBotShowdown = PokerHandEvaluator.avaliarMelhorMao(seteBot);

        int comparacao = melhorMaoJogadorShowdown.compareTo(melhorMaoBotShowdown);
        if (comparacao > 0) {
            this.resultadoMao = PokerRoundResult.JOGADOR_VENCEU_SHOWDOWN;
            this.ultimaAcaoDescricao = "Showdown: Jogador venceu com "
                    + melhorMaoJogadorShowdown.getDescricaoCurta() + "!";
            liquidarPoteUnico(PokerParticipant.JOGADOR);
        } else if (comparacao < 0) {
            this.resultadoMao = PokerRoundResult.BOT_VENCEU_SHOWDOWN;
            this.ultimaAcaoDescricao = "Showdown: Bot venceu com "
                    + melhorMaoBotShowdown.getDescricaoCurta() + ".";
            liquidarPoteUnico(PokerParticipant.BOT);
        } else {
            this.resultadoMao = PokerRoundResult.EMPATE_DIVISAO_POTE;
            this.ultimaAcaoDescricao = "Showdown empatado ("
                    + melhorMaoJogadorShowdown.getDescricaoCurta() + ")! Pote dividido.";
            liquidarDivisaoDePote();
        }
    }

    private void liquidarPoteUnico(PokerParticipant vencedor) {
        if (poteLiquidado) {
            return;
        }
        this.poteLiquidado = true;
        if (vencedor == PokerParticipant.JOGADOR) {
            this.saldoJogador = Math.addExact(this.saldoJogador, this.poteTotal);
        } else {
            this.saldoBot = Math.addExact(this.saldoBot, this.poteTotal);
        }
    }

    private void liquidarDivisaoDePote() {
        if (poteLiquidado) {
            return;
        }
        this.poteLiquidado = true;
        long metade = this.poteTotal / 2L;
        long fichaImparResidual = this.poteTotal % 2L;

        long parteJogador = metade;
        long parteBot = metade;

        // Regra oficial da ficha impar (odd chip): vai para o primeiro jogador fora de posicao (oponente do Botao)
        if (fichaImparResidual == 1L) {
            if (botaoDealer.oponente() == PokerParticipant.JOGADOR) {
                parteJogador = Math.addExact(parteJogador, 1L);
            } else {
                parteBot = Math.addExact(parteBot, 1L);
            }
        }

        this.saldoJogador = Math.addExact(this.saldoJogador, parteJogador);
        this.saldoBot = Math.addExact(this.saldoBot, parteBot);
    }

    private void colocarFichasNaRua(PokerParticipant participante, long quantidadeSolicitada) {
        if (quantidadeSolicitada <= 0L) {
            return;
        }
        if (participante == PokerParticipant.JOGADOR) {
            long efetivo = Math.min(quantidadeSolicitada, saldoJogador);
            this.saldoJogador = Math.subtractExact(this.saldoJogador, efetivo);
            this.apostaJogadorNaRua = Math.addExact(this.apostaJogadorNaRua, efetivo);
            this.poteTotal = Math.addExact(this.poteTotal, efetivo);
        } else {
            long efetivo = Math.min(quantidadeSolicitada, saldoBot);
            this.saldoBot = Math.subtractExact(this.saldoBot, efetivo);
            this.apostaBotNaRua = Math.addExact(this.apostaBotNaRua, efetivo);
            this.poteTotal = Math.addExact(this.poteTotal, efetivo);
        }
    }

    /**
     * Caso um participante aposte mais fichas na rua do que o oponente possui no total (aposta na rua + saldo restante),
     * devolve imediatamente o excesso inigualavel (uncalled bet) ao apostador, mantendo o pote 100% simetrico.
     */
    private void devolverExcessoNaoIgualavelSeNecessario() {
        long maximoCoberturaBot = Math.addExact(apostaBotNaRua, saldoBot);
        if (apostaJogadorNaRua > maximoCoberturaBot) {
            long excesso = Math.subtractExact(apostaJogadorNaRua, maximoCoberturaBot);
            this.apostaJogadorNaRua = maximoCoberturaBot;
            this.poteTotal = Math.subtractExact(this.poteTotal, excesso);
            this.saldoJogador = Math.addExact(this.saldoJogador, excesso);
        }

        long maximoCoberturaJogador = Math.addExact(apostaJogadorNaRua, saldoJogador);
        if (apostaBotNaRua > maximoCoberturaJogador) {
            long excesso = Math.subtractExact(apostaBotNaRua, maximoCoberturaJogador);
            this.apostaBotNaRua = maximoCoberturaJogador;
            this.poteTotal = Math.subtractExact(this.poteTotal, excesso);
            this.saldoBot = Math.addExact(this.saldoBot, excesso);
        }
    }

    private boolean naoHaMaisAcoesPossiveisNaMao() {
        // Se pelo menos um participante esta com saldo zero (all-in) e as apostas na rua ja estao igualadas,
        // nenhum dos dois pode apostar mais fichas nas ruas seguintes.
        return apostaJogadorNaRua == apostaBotNaRua && (saldoJogador == 0L || saldoBot == 0L);
    }

    private void marcarQueAgiuNaRua(PokerParticipant participante) {
        if (participante == PokerParticipant.JOGADOR) {
            this.jogadorJaAgiuNaRua = true;
        } else {
            this.botJaAgiuNaRua = true;
        }
    }

    private void comprarParaParticipante(PokerParticipant participante) {
        Card comprada = Objects.requireNonNull(baralho.comprarCarta());
        if (participante == PokerParticipant.JOGADOR) {
            maoJogador.add(comprada);
        } else {
            maoBot.add(comprada);
        }
    }

    private boolean isTurnoDoJogador() {
        return estagioAtual.isMaoEmAndamento() && turnoAtual == PokerParticipant.JOGADOR;
    }

    private long getValorParaPagar(PokerParticipant participante) {
        if (!estagioAtual.isMaoEmAndamento()) {
            return 0L;
        }
        if (participante == PokerParticipant.JOGADOR) {
            long dif = Math.max(0L, Math.subtractExact(apostaBotNaRua, apostaJogadorNaRua));
            return Math.min(dif, saldoJogador);
        } else {
            long dif = Math.max(0L, Math.subtractExact(apostaJogadorNaRua, apostaBotNaRua));
            return Math.min(dif, saldoBot);
        }
    }

    private boolean podeParticipanteAumentar(PokerParticipant participante) {
        if (!estagioAtual.isMaoEmAndamento() || aumentosNaRua >= MAX_AUMENTOS_POR_RUA) {
            return false;
        }
        long maiorApostaRua = Math.max(apostaJogadorNaRua, apostaBotNaRua);
        if (participante == PokerParticipant.JOGADOR) {
            long saldoAposPagar = saldoJogador - Math.max(0L, maiorApostaRua - apostaJogadorNaRua);
            return saldoAposPagar > 0L && saldoBot > 0L;
        } else {
            long saldoAposPagar = saldoBot - Math.max(0L, maiorApostaRua - apostaBotNaRua);
            return saldoAposPagar > 0L && saldoJogador > 0L;
        }
    }

    private static void limparComLacoReverso(List<Card> lista) {
        for (int i = lista.size() - 1; i >= 0; i--) {
            lista.remove(i);
        }
    }

    // =========================================================================
    // CONSULTAS PUBLICAS DE ESTADO E VISIBILIDADE SEGURA (PokerEngine)
    // =========================================================================

    @Override
    public PokerStage getEstagioAtual() {
        return estagioAtual;
    }

    @Override
    public PokerRoundResult getResultadoMao() {
        return resultadoMao;
    }

    @Override
    public PokerParticipant getTurnoAtual() {
        return turnoAtual;
    }

    @Override
    public PokerParticipant getBotaoDealer() {
        return botaoDealer;
    }

    @Override
    public long getSaldoJogador() {
        return saldoJogador;
    }

    @Override
    public long getSaldoBot() {
        return saldoBot;
    }

    @Override
    public long getPoteTotal() {
        return poteTotal;
    }

    @Override
    public long getApostaJogadorNaRua() {
        return apostaJogadorNaRua;
    }

    @Override
    public long getApostaBotNaRua() {
        return apostaBotNaRua;
    }

    @Override
    public long getValorParaPagarJogador() {
        return getValorParaPagar(PokerParticipant.JOGADOR);
    }

    @Override
    public long getSmallBlind() {
        return smallBlind;
    }

    @Override
    public long getBigBlind() {
        return bigBlind;
    }

    @Override
    public Set<PokerAction> getAcoesValidasJogador() {
        if (!isTurnoDoJogador()) {
            return Collections.emptySet();
        }
        EnumSet<PokerAction> acoes = EnumSet.of(PokerAction.DESISTIR);
        long paraPagar = getValorParaPagar(PokerParticipant.JOGADOR);
        if (paraPagar == 0L) {
            acoes.add(PokerAction.PASSAR);
        } else {
            acoes.add(PokerAction.PAGAR);
        }
        if (podeParticipanteAumentar(PokerParticipant.JOGADOR)) {
            acoes.add(PokerAction.AUMENTAR);
        }
        return Collections.unmodifiableSet(acoes);
    }

    @Override
    public List<Card> getMaoJogador() {
        return List.copyOf(maoJogador);
    }

    @Override
    public List<Card> getCartasComunitarias() {
        return List.copyOf(cartasComunitarias);
    }

    @Override
    public List<Card> getCartasVisiveisBot() {
        if (estagioAtual == PokerStage.SHOWDOWN) {
            return List.copyOf(maoBot);
        }
        return List.of();
    }

    @Override
    public boolean hasCartasOcultasBot() {
        return !maoBot.isEmpty() && estagioAtual != PokerStage.SHOWDOWN;
    }

    @Override
    public int getQuantidadeCartasBot() {
        return maoBot.size();
    }

    @Override
    public EvaluatedPokerHand getMelhorMaoJogadorShowdown() {
        return estagioAtual == PokerStage.SHOWDOWN ? melhorMaoJogadorShowdown : null;
    }

    @Override
    public EvaluatedPokerHand getMelhorMaoBotShowdown() {
        return estagioAtual == PokerStage.SHOWDOWN ? melhorMaoBotShowdown : null;
    }

    @Override
    public String getUltimaAcaoDescricao() {
        return ultimaAcaoDescricao;
    }
}
