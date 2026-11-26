# Projeto Java Jogo - Cassino Social 2D Isométrico

Trabalho didático em dupla de Programação Orientada a Objetos e jogos 2D desktop, desenvolvido em Java puro com **Swing**, **Graphics2D** e **Java Sound (`Clip`)**, sem engines ou frameworks externos.

## Tema Confirmado
Cassino social 2D isométrico em pixel art (estilo Habbo Hotel) com identidade própria e fichas **100% virtuais** (offline, sem dinheiro real). O jogador controla seu avatar pelo salão do cassino, desvia de obstáculos e mobílias, aproxima-se das três mesas interativas e disputa partidas solo contra bots em **Blackjack**, **Truco Paulista** e **Poker Texas Hold'em**.

## Estado Atual Validado
- **Etapa 1 — Base e POO (Concluída e testada):** Hierarquia oficial de formas geométricas (`MyShape`, `MyRectangle`, `MySquare`, `MyEllipse`, `MyCircle`), validação estrita de dimensões positivas, preservação de invariantes nos setters de quadrado/círculo e detecção de colisão AABB (`intersects`).
- **Etapa 2 — Salão Isométrico, Janela e Controles (Concluída e testada):** Janela `JFrame` em tela cheia sem bordas (`App.java`), loop a ~60 FPS com delta time, projeção isométrica 2:1 (`IsometricHelper`), movimentação contínua com Swing Key Bindings (`WASD` e setas simultâneas com diagonais normalizadas), avatar com sprite PNG (`MyCharacter`) e *footprint* no solo desacoplado do sprite, deslizamento por eixos, ordenação de profundidade (*z-sorting*), áudio WAV pré-carregado com `Clip` rebobinado e encerramento imediato por `ESC` em qualquer tela.
- **Etapa 3 — Mobília e Ambientação (Concluída e testada):** Paredes isométricas com molduras, portal iluminado de entrada, tapete decorativo, balcão do bar (`CasinoCounter`) e cadeiras (`CasinoChair`) integrados ao polimorfismo de `List<MyShape>`.
- **Etapa 4 — Três Mesas Interativas Completas e Integradas (Concluída e testada):**
  - **Seleção contextual no salão:** detecção por proximidade (`isPlayerNear`, raio 35 px) e desempate determinístico pela menor distância euclidiana ao quadrado entre as mesas de **Blackjack**, **Truco** e **Poker**, com rotas livres entre todas as mesas, prompt dinâmico (`E — Interagir com a mesa de [Jogo]`) e preservação da partida ao alternar entre mesa e salão (`B` / `E`).
  - **Blackjack (`BlackjackGame` + `BlackjackTableView`):** baralho de 52 cartas (`Deck`) com `SecureRandom`, cálculo flexível de Ases (`Hand`), apostas pares em `long`, ocultação da 2ª carta da banca durante `TURNO_JOGADOR`, dealer parando em todo 17, pagamento 3:2 para Blackjack Natural e sincronização com o HUD do salão.
  - **Truco Paulista (`TrucoRound` + `TrucoTableView`):** baralho limpo de 40 cartas (`TrucoDeck`), carta vira e manilhas por naipe (*Zap, Copeta, Espadilha, Picafumo*), melhor de 3 vazas com todas as regras de empate (*canga*), ocultação das cartas do Bot e IA honesta (`TrucoSimpleAI`).
  - **Poker Texas Hold'em 1v1 (`TexasHoldemGame` + `PokerTableView`):** contrato oficial `PokerEngine`, blinds pares (`50/100`), alternância de botão Dealer Heads-Up, ações contextuais guiadas por `getAcoesValidasJogador()` (`DESISTIR`, `PASSAR`, `PAGAR`, `AUMENTAR`, all-in com devolução de excesso), 5 cartas comunitárias por fase (`Pré-Flop`, `Flop`, `Turn`, `River`, `Showdown`), avaliador combinatório $\binom{7}{5}$ (`PokerHandEvaluator`), *split pot*, IA honesta (`PokerSimpleAI`) e ocultação estrita das cartas do Bot inclusive em desistência (*Fold*), revelando-as exclusivamente no `SHOWDOWN`.
- **Bateria Automatizada Completa:** **11 suítes independentes** executando em processos separados com código de saída `0`.

## Objetivo Didático (Os 4 Pilares de POO)
- **Abstração:** `MyShape` (`abstract` com `draw(Graphics2D)` abstrato) e contratos `BlackjackEngine` / `PokerEngine` / `PokerBotStrategy`.
- **Encapsulamento:** `Card`, `TrucoTrick` e `EvaluatedPokerHand` imutáveis; invariantes em `MyShape`/`MySquare`/`MyCircle`; snapshots `List.copyOf`; ocultação rigorosa de cartas fechadas do Dealer/Bot; remoção de coleções dinâmicas por laço reverso.
- **Herança:** `MyRectangle`, `MyEllipse` e `MyCharacter` estendendo `MyShape`; `MySquare`, `CasinoTable`, `CasinoCounter` e `CasinoChair` estendendo `MyRectangle`; `MyCircle` estendendo `MyEllipse`.
- **Polimorfismo:** `List<MyShape>` chamando `draw(g2d)` sobrescrito após *z-sorting*; views acopladas a interfaces (`BlackjackEngine`, `PokerEngine`) permitindo alternar motor real e motores determinísticos nos testes.

## Documentação
- [Requisitos e checklist](docs/REQUISITOS.md)
- [Divisão de trabalho, contratos e guia de apresentação](docs/PLANO-DUPLA.md)
- [Arquitetura de referência e diagramas UML](docs/ARQUITETURA.md)
- [Prompt inicial para o Antigravity](docs/ANTIGRAVITY.md)
- [Instruções de desenvolvimento](AGENTS.md)

## Compilação e Execução (Windows PowerShell)

### 1. Compilar todo o projeto (`src` e `tests`)
```powershell
New-Item -ItemType Directory -Force -Path bin | Out-Null
$arquivos = Get-ChildItem -Path src,tests -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d bin -cp src $arquivos
```

### 2. Executar o jogo em tela cheia
```powershell
java -cp "bin;src" App
```
*(No Linux/macOS use `java -cp bin:src App`. Pressione `ESC` a qualquer momento para encerrar o jogo).*

### 3. Executar todas as 11 suítes automatizadas de testes (PowerShell)
```powershell
java -cp "bin;src" tests.TestePOO
java -cp "bin;src" tests.TesteRenderizacao
java -cp "bin;src" tests.TesteContratoBlackjack
java -cp "bin;src" tests.TesteBaralhoEMao
java -cp "bin;src" tests.TesteMotorBlackjack
java -cp "bin;src" tests.TesteIntegracaoMesaBlackjack
java -cp "bin;src" tests.TesteInterfaceBlackjack
java -cp "bin;src" tests.TesteTrucoPaulista
java -cp "bin;src" tests.TesteInterfaceTruco
java -cp "bin;src" tests.TestePokerHoldem
java -cp "bin;src" tests.TesteInterfacePoker
```

## Equipe
- **Integrante A (Mundo Isométrico, Interfaces Gráficas e Truco Paulista):** Enzo Gabriel Tomas De Souza (`@Enzootomas`)
- **Integrante B (Motores de Regras de Blackjack e Poker, Integração, Testes Determinísticos e Documentação):** Guilherme Bueno (`@guibuenoc`)

Entrega indicada no documento: 23 a 27 de novembro de 2026. Prova: 27 de novembro. Confirmar a data específica com o professor.
