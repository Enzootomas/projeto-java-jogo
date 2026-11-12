# Projeto Java Jogo - Cassino Social 2D Isométrico

Trabalho didático em dupla de Programação Orientada a Objetos e jogos 2D desktop, desenvolvido em Java puro com **Swing**, **Graphics2D** e **Java Sound (`Clip`)**, sem engines ou frameworks externos.

## Tema Confirmado
Cassino social 2D isométrico em pixel art (estilo Habbo Hotel) com identidade própria e fichas **100% virtuais** (offline, sem dinheiro real). O jogador controla seu avatar pelo salão do cassino, desvia de obstáculos e mobílias, aproxima-se das mesas interativas e joga partidas solo contra bots em **Blackjack** e **Truco Paulista**.

## Estado Atual Validado
- **Etapa 1 — Base e POO (Concluída e testada):** Hierarquia oficial de formas geométricas (`MyShape`, `MyRectangle`, `MySquare`, `MyEllipse`, `MyCircle`), validação estrita de dimensões positivas, preservação de invariantes nos setters de quadrado/círculo e detecção de colisão AABB (`intersects`).
- **Etapa 2 — Salão Isométrico, Janela e Controles (Concluída e testada):** Janela `JFrame` em tela cheia sem bordas (`App.java`), loop a ~60 FPS com delta time, projeção isométrica 2:1 (`IsometricHelper`), movimentação contínua com Swing Key Bindings (`WASD` e setas simultâneas com diagonais normalizadas), avatar com sprite PNG (`MyCharacter`) e *footprint* no solo desacoplado do sprite, deslizamento por eixos, ordenação de profundidade (*z-sorting*), áudio WAV pré-carregado com `Clip` rebobinado e encerramento imediato por `ESC` em qualquer tela.
- **Etapa 3 — Mobília e Ambientação (Concluída e testada):** Paredes isométricas com molduras, portal iluminado de entrada, tapete decorativo, balcão do bar (`CasinoCounter`) e cadeiras (`CasinoChair`) integrados ao polimorfismo de `List<MyShape>`.
- **Etapa 4 — Mesas Interativas, Blackjack e Truco Paulista (Concluída e integrada):**
  - **Seleção contextual no salão:** detecção por proximidade (`isPlayerNear`) e desempate pela menor distância euclidiana ao quadrado entre as mesas de **Blackjack** e **Truco**, exibindo prompt dinâmico (`E — Interagir com a mesa de [Jogo]`) e preservando rodadas em andamento ao alternar entre mesa e salão (`B` / `E`).
  - **Mesa e Motor Oficial de Blackjack (`BlackjackGame` + `BlackjackTableView`):** baralho de 52 cartas (`Deck`) com `SecureRandom` em produção e injeção determinística para testes, cálculo flexível de Ases (`Hand`), apostas pares em fichas virtuais (`long`) com proteção contra overflow, ocultação estrita da segunda carta da banca durante `TURNO_JOGADOR`, dealer parando em todo 17 (inclusive *soft 17*), pagamento 3:2 para Blackjack Natural e sincronização imediata de saldo com o HUD do salão.
  - **Mesa e Motor Oficial de Truco Paulista (`TrucoRound` + `TrucoTableView`):** baralho limpo de 40 cartas (`TrucoDeck`), carta vira e cálculo cíclico de manilhas (*Zap, Copeta, Espadilha, Picafumo*), melhor de 3 vazas com todas as regras de empate (*canga*), ocultação rigorosa das cartas do Bot, IA tática honesta (`TrucoSimpleAI`) e jogadas por clique ou atalhos (`1`, `2`, `3`, `N` para nova mão).
- **Bateria Automatizada Completa:** **9 suítes independentes** executando sem falhas (incluindo mais de 440 verificações unitárias, de integração e renderização visual off-screen).

## Objetivo Didático e Guia de Apresentação
Compreender e demonstrar na prática os quatro pilares de POO (**abstração**, **encapsulamento**, **herança** e **polimorfismo**):
- **Abstração:** `MyShape` (`abstract` com `draw(Graphics2D)` abstrato) e contrato `BlackjackEngine` separando o *que* cada componente faz de *como* implementa.
- **Encapsulamento:** `Card` e `TrucoTrick` imutáveis, validação de invariantes em `MyShape`/`MySquare`/`MyCircle`, coleções internas protegidas com snapshots `List.copyOf`, ocultação da carta fechada do dealer e liquidação única de apostas.
- **Herança:** `MyRectangle` e `MyEllipse` estendendo `MyShape`; `MySquare` e mobílias (`CasinoTable`, `CasinoCounter`, `CasinoChair`) estendendo `MyRectangle`; `MyCircle` estendendo `MyEllipse`; `MyCharacter` estendendo `MyShape`; uso de `SecureRandom extends Random` em `Deck`.
- **Polimorfismo:** renderização unificada do salão via `List<MyShape>` invocando `draw(g2d)` sobrescrito após *z-sorting*, e desacoplamento de `BlackjackTableView` operando sobre qualquer `BlackjackEngine` (`BlackjackGame` ou `BlackjackMockSimulator`).

## Comece por aqui
- [Requisitos e checklist](docs/REQUISITOS.md)
- [Divisão de trabalho, contratos e guia de apresentação](docs/PLANO-DUPLA.md)
- [Arquitetura de referência e diagramas UML](docs/ARQUITETURA.md)
- [Prompt inicial para o Antigravity](docs/ANTIGRAVITY.md)
- [Instruções de desenvolvimento](AGENTS.md)

## Compilação e Execução

### 1. Clonar o repositório
```bash
git clone https://github.com/Enzootomas/projeto-java-jogo.git
cd projeto-java-jogo
```

### 2. Compilar todo o projeto (`src` e `tests`)
No **PowerShell (Windows)**:
```powershell
New-Item -ItemType Directory -Force -Path bin | Out-Null
$arquivos = Get-ChildItem -Path src,tests -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d bin -cp src $arquivos
```

No **Linux / macOS (bash)**:
```bash
mkdir -p bin
javac -encoding UTF-8 -d bin -cp src $(find src tests -name "*.java")
```

### 3. Executar o jogo em tela cheia
No **PowerShell (Windows)**:
```powershell
java -cp "bin;src" App
```

No **Linux / macOS**:
```bash
java -cp bin:src App
```
*(O classpath inclui `src` para disponibilizar `image/` e `sound/` durante o desenvolvimento. Pressione `ESC` a qualquer momento para encerrar o jogo).*

### 4. Executar as 9 suítes automatizadas de testes (PowerShell)
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
```

## Equipe
- **Integrante A (Mundo Isométrico, Interfaces Gráficas e Truco Paulista):** Enzo Gabriel Tomas De Souza (`@Enzootomas`)
- **Integrante B (Motor de Regras de Cartas, Apostas, Testes Determinísticos e Revisão):** Guilherme Bueno (`@guibuenoc`)

Entrega indicada no documento: 23 a 27 de novembro de 2026. Prova: 27 de novembro. Confirmar a data específica com o professor.
