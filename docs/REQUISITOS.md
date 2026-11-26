# Requisitos e objetivo didático

## Objetivo
Consolidar abstração, encapsulamento, herança e polimorfismo em um jogo 2D Java desktop. Ambos os integrantes devem explicar toda a solução, inclusive código auxiliado por IA.

| Pilar | Evidência no projeto | Pergunta de apresentação |
|---|---|---|
| Abstração | `MyShape` abstrata com `draw` abstrato; contratos `BlackjackEngine` e `PokerEngine` | Por que não instanciamos `MyShape`, `BlackjackEngine` ou `PokerEngine`? |
| Encapsulamento | Estado protegido/privado, dimensões válidas, snapshots `List.copyOf` e cartas ocultas do Dealer/Bot | Como impedimos estado inválido ou vazamento de cartas fechadas? |
| Herança | Hierarquia oficial (`MyShape` → `MyRectangle`/`MySquare`, `MyEllipse`/`MyCircle`, `MyCharacter`, mobília) | O que é herdado e o que é especializado em cada subclasse? |
| Polimorfismo | `List<MyShape>` chamando `draw` sobrescrito com *z-sorting*; Views acopladas a `BlackjackEngine` e `PokerEngine` | Como a mesma chamada desenha objetos diferentes ou troca de motor em tempo de execução? |

## Checklist obrigatório
- [x] Hierarquia oficial de formas e construtores conforme documento do professor (`MyShape`, `MyRectangle`, `MySquare`, `MyEllipse`, `MyCircle`).
- [x] Validação de dimensões estritamente positivas e invariantes de quadrado e círculo mantidos nos construtores e setters.
- [x] Java Swing desktop: `JFrame`, `JPanel` e `Graphics2D` (sem libGDX, web ou frameworks externos).
- [x] Tela cheia sem bordas ocupando a resolução do monitor (`GraphicsDevice.setFullScreenWindow` / `setUndecorated(true)`).
- [x] Tecla `ESC` encerra o jogo imediatamente em qualquer estado (salão ou qualquer uma das três mesas).
- [x] Setas e `WASD` com controle simultâneo de teclas pressionadas (Swing Key Bindings), normalização de vetor e diagonais fluidas.
- [x] Salão isométrico renderizado por tiles com proporção 2:1 exata e ordenação de profundidade (*z-sorting*) pela base dos pés.
- [x] Ambientação completa do salão: duas paredes isométricas ao fundo com molduras, portal de entrada com letreiro luminoso, tapete decorativo e rotas livres entre todas as mesas.
- [x] Mobília de cassino (`CasinoCounter` e `CasinoChair`) como subclasses de `MyRectangle` com *footprint* sólido no chão e renderização volumétrica.
- [x] Colisão no plano do chão (*footprint* / base) calculada via `intersects(MyShape)` com deslizamento por eixos, sem usar o sprite vertical inteiro como obstáculo.
- [x] Sprites PNG reais de avatar e ambiente carregados na memória antes do loop (`ResourceLoader`).
- [x] Áudio WAV de interação pré-carregado com `Clip`, rebobinado (`setFramePosition(0)`) antes da reprodução e liberado no encerramento (`dispose()`).
- [x] Mesa interativa de Blackjack acoplada ao motor oficial `BlackjackGame` (`BlackjackTableView`), preservando a rodada ativa ao alternar `B` / `E`.
- [x] Mesa de Truco Paulista integrada no salão com interface completa (`TrucoTableView`) acoplada ao `TrucoRound` (com retorno por `B` preservando a mão em andamento e saída por `ESC`).
- [x] Motor de regras oficial do Truco Paulista (`model`): baralho limpo de 40 cartas (`TrucoDeck`), vira, manilhas por naipes (*Zap, Copeta, Espadilha, Picafumo*), resolução de mão de 3 vazas com regras de empate/encerramento antecipado e IA do Bot (`TrucoSimpleAI`) com visibilidade estrita.
- [x] Terceira mesa de Poker Texas Hold'em (1 vs 1) integrada no salão (`CasinoTable` em `(340, 50)`) e acoplada ao motor oficial `TexasHoldemGame` (`PokerEngine` + `PokerTableView`), com cartas comunitárias por fase, ações contextuais, blinds, *all-in*, *split pot*, ocultação estrita das cartas do Bot (inclusive em *Fold*) e preservação da mão entre `B` e `E`.
- [x] Seleção contextual unificada da mesa mais próxima (`getDistanceSqToPlayer`) entre Blackjack, Truco e Poker para exibição do prompt (`E — Interagir com a mesa de [Jogo]`) e tratamento da tecla `E`.
- [x] Implementação completa das cartas e apostas do Blackjack solo (`Deck`, `Hand`, `BlackjackGame` implementando `BlackjackEngine`), integrada à mesa oficial do cassino.
- [x] Remoção de elementos por laço reverso em coleções dinâmicas de cartas (`Deck.limparComLacoReverso()`, `Hand.limpar()`, rollback atômico em `BlackjackGame.parar()` e limpeza de listas em `TexasHoldemGame.limparComLacoReverso()`).
- [x] Código compila e executa por terminal PowerShell e IDE, com **11 suítes automatizadas** passando em processos separados com código de saída `0`.
- [x] `README.md`, diagramas Mermaid (`docs/ARQUITETURA.md`), plano de apresentação (`docs/PLANO-DUPLA.md`) e histórico Git autêntico da dupla.

## Organização do projeto
- `src/App.java`: pré-carregamento de recursos fora da EDT e inicialização do `JFrame` em tela cheia na Event Dispatch Thread.
- `src/model/`:
  - `MyShape.java`, `MyRectangle.java`, `MySquare.java`, `MyEllipse.java`, `MyCircle.java`: hierarquia geométrica, invariantes e colisão `intersects`.
  - `IsometricHelper.java`, `ResourceLoader.java`: projeção isométrica 2:1 direta/inversa, profundidade `Z` e cache seguro de imagens/áudio.
  - `MyCharacter.java`: avatar do jogador que estende `MyShape`, com *footprint* no solo, movimentação diagonal normalizada, deslizamento por eixos e áudio `Clip`.
  - `CasinoTable.java`, `CasinoCounter.java`, `CasinoChair.java`: mobílias interativas e sólidas que estendem `MyRectangle`.
  - `Card.java`, `Suit.java`, `Rank.java`, `Deck.java`: modelo imutável de cartas e baralho de 52 cartas compartilhados entre os jogos.
  - `BlackjackEngine.java`, `RoundState.java`, `RoundResult.java`, `Hand.java`, `BlackjackGame.java`: contrato, mão com cálculo de Ases e motor oficial do Blackjack solo.
  - `TrucoRules.java`, `TrucoCardComparator.java`, `TrucoDeck.java`, `TrucoPlayer.java`, `TrucoTrick.java`, `TrucoTrickResult.java`, `TrucoHandResult.java`, `TrucoSimpleAI.java`, `TrucoRound.java`: motor completo do Truco Paulista 1v1.
  - `PokerEngine.java`, `PokerParticipant.java`, `PokerStage.java`, `PokerAction.java`, `PokerRoundResult.java`, `PokerHandRank.java`, `EvaluatedPokerHand.java`, `PokerHandEvaluator.java`, `PokerBotStrategy.java`, `PokerSimpleAI.java`, `TexasHoldemGame.java`: contrato, avaliador de 5 cartas dentre 7, IA honesta e motor oficial do Poker Texas Hold'em 1v1.
- `src/view/`:
  - `ScreenSaver.java`: salão isométrico, paredes, portal, tapete, *z-sorting*, loop `Timer` (~60 FPS), Key Bindings e máquina de estados entre salão e as três mesas.
  - `BlackjackTableView.java`, `BlackjackMockSimulator.java`: interface gráfica da mesa de Blackjack e simulador auxiliar de testes.
  - `TrucoTableView.java`: interface gráfica da mesa de Truco Paulista.
  - `PokerTableView.java`: interface gráfica da mesa de Poker Texas Hold'em acoplada ao `PokerEngine`.
- `src/image/` e `src/sound/`: recursos gráficos PNG em pixel art e efeito sonoro WAV.
- `tests/`:
  - `TestePOO.java`, `TesteRenderizacao.java`
  - `TesteContratoBlackjack.java`, `TesteBaralhoEMao.java`, `TesteMotorBlackjack.java`, `TesteIntegracaoMesaBlackjack.java`, `TesteInterfaceBlackjack.java`
  - `TesteTrucoPaulista.java`, `TesteInterfaceTruco.java`
  - `TestePokerHoldem.java`, `TesteInterfacePoker.java`

## Entrega
O documento fornecido indica entrega de 23/11 a 27/11/2026 e prova em 27/11. Conferir o dia específico da dupla com o professor.

## Verificação manual na máquina da apresentação
Executar `java -cp "bin;src" App` na máquina da apresentação e conferir: tela cheia sem bordas, `ESC` imediato, resposta de `WASD` e setas com diagonais fluidas, colisão nos pés das mesas/balcão/cadeiras com deslizamento, *z-sorting* frente/trás, som sem engasgos, partidas completas nas três mesas (Blackjack, Truco e Poker), ocultação de cartas do Dealer/Bot até o momento autorizado, preservação da mão ao alternar `B` e `E` e encerramento limpo.
