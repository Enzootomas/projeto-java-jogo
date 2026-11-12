# Plano de trabalho em dupla

Tema confirmado: **Cassino Social 2D Isométrico (estilo Habbo)**. O jogador controla seu avatar pelo salão do cassino, desvia de mesas, cadeiras, balcão e paredes, interage contextualmente com as mesas de jogo e disputa partidas solo com fichas 100% virtuais (Blackjack e Truco Paulista).

## Identificação da Dupla
- **Integrante A — Enzo Gabriel Tomas De Souza (`@Enzootomas`):** Frente Mundo Isométrico, Hierarquia Visual, Interfaces das Mesas (`ScreenSaver`, `BlackjackTableView`, `TrucoTableView`), Áudio e Truco Paulista.
- **Integrante B — Guilherme Bueno (`@guibuenoc`):** Frente Motor de Regras do Blackjack (`Deck`, `Hand`, `BlackjackGame`), Matemática de Apostas Virtuais, Testes Determinísticos, Revisão Técnica e Documentação de Entrega.

| Etapa | Enzo (`@Enzootomas`) | Guilherme Bueno (`@guibuenoc`) | Critério de aceite |
|---|---|---|---|
| **1 — Base e POO [CONCLUÍDA]** | `MyShape`, `MyRectangle` e `MySquare` | `MyEllipse`, `MyCircle` e suíte base de POO | Hierarquia compila; invariantes validados; pilares explicados |
| **2 — Salão Isométrico, Janela e Controles [CONCLUÍDA]** | `App` (tela cheia/ESC), loop (`Timer` 60 FPS com `dt`), projeção 2:1 e `IsometricHelper` | Revisão de `ScreenSaver` (Key Bindings, *z-sorting*), `MyCharacter` (sprite PNG, som WAV, *footprint*) | Janela sem bordas em tela cheia, `ESC` encerra, movimento normalizado, colisão por eixos com deslizamento |
| **3 — Mobília e Ambientação do Salão [CONCLUÍDA]** | `CasinoCounter`, `CasinoChair`, paredes isométricas, portal com letreiro e tapete | Validação de rotas livres, profundidade frente/trás e testes de colisão | Salão ambientado com *z-sorting* correto e 77 verificações em `TestePOO` passando |
| **4 — Mesas de Jogo, Motores e Integração [CONCLUÍDA]** | `BlackjackTableView`, mesa e motor `TrucoRound` + `TrucoTableView`, integração em `ScreenSaver` | `Deck`, `Hand`, `BlackjackGame` (`BlackjackEngine`), testes determinísticos de baralho, motor e rollback | Blackjack e Truco 100% jogáveis nas mesas do salão, carta oculta protegida, saldo sincronizado e retomada por `B`/`E` |
| **5 — Revisão Cruzada, Testes e Preparação da Entrega [EM FINALIZAÇÃO]** | Efeitos sonoros (`Clip` pré-carregado/rebobinado) e estabilização das suítes gráficas | Revisão técnica de PRs, bateria de 9 suítes automatizadas, documentação final e roteiro de defesa ao vivo | 9 suítes passando (`exit 0`); compilação limpa via PowerShell/IDE; guia de apresentação pronto para a prova |

## Divisão de Frentes de Trabalho

O desenvolvimento é organizado em duas frentes complementares e independentes:

1. **Frente Mundo e Interface (Enzo Gabriel Tomas De Souza):**
   * Salão isométrico, avatar, controles contínuos, detecção de colisões no chão, *z-sorting*, transição de telas/menus, interfaces gráficas das mesas, sprites em pixel art, Truco Paulista e efeitos sonoros com `Clip`.
   * Branches de trabalho: `feat/mundo-<tarefa>` e `feat/integracao-blackjack`.
2. **Frente Regras, Motores de Cartas e Qualidade (Guilherme Bueno):**
   * Baralho de 52 cartas (`Deck`), cálculo de mãos e Ases (`Hand`), motor oficial `BlackjackGame` (dealer no 17, apostas pares, pagamento 3:2, rollback atômico), expansão de motores de cartas (Texas Hold'em), testes determinísticos e documentação técnica.
   * Branches de trabalho: `feat/regras-<tarefa>` e `docs/<tarefa>`.

Ambos trabalham em seus respectivos computadores, com suas próprias contas Git, revisando via Pull Request. **Nenhum integrante faz commit, push ou merge diretamente na branch `main`**.

---

## Contrato de Integração Oficial: Interface (View) $\leftrightarrow$ Motor de Blackjack (Model)

Para garantir desacoplamento total entre interface visual e regras de negócio sem vazamento de cartas ocultas, a mesa consome a interface `BlackjackEngine`:

### 1. Tipos de Apoio
- **`Suit` (Enum):** `COPAS`, `ESPADAS`, `OUROS`, `PAUS` (com símbolo e nome em português).
- **`Rank` (Enum):** `AS` a `REI` (valores de Blackjack 2 a 10, figuras valendo 10 e Ás valendo 11 ou 1).
- **`Card` (Classe Imutável):** representa a carta com naipe e valor, compartilhada entre os jogos de cartas do cassino.
- **`RoundState` (Enum):** estados explícitos da rodada:
  - `AGUARDANDO_APOSTA`: início ou entre rodadas.
  - `TURNO_JOGADOR`: cartas distribuídas, dealer com 1 carta visível e 1 oculta virada para baixo.
  - `FINALIZADA`: desfecho apurado, carta oculta revelada e saldo liquidado.
- **`RoundResult` (Enum):** resultados com liquidação matemática:
  - `EM_ANDAMENTO`: rodada ativa.
  - `JOGADOR_BLACKJACK`: vitória com 21 natural (2 cartas). Paga **3:2** sobre a aposta.
  - `JOGADOR_VITORIA`: pontuação maior que dealer ou dealer estourou. Paga **1:1**.
  - `EMPATE_PUSH`: empate na pontuação final ou duplo Blackjack Natural. Devolve a aposta integralmente.
  - `JOGADOR_ESTOUROU` / `DEALER_VITORIA` / `DEALER_BLACKJACK`: derrota do jogador (retorno 0).

### 2. Regras Específicas da Partida Solo (v1) e Matemática Financeira
- **Quantidade de Baralhos:** 1 baralho francês padrão de **52 cartas** (`Deck.TOTAL_CARTAS_PADRAO = 52`), embaralhado com `SecureRandom` em produção e estritamente determinístico quando injetado nos testes (`BlackjackGame(saldo, deck)`).
- **Comportamento do Dealer no 17 (incluindo *Soft 17*):** o dealer compra cartas enquanto sua pontuação for menor que 17 e **para em todo 17 ou superior** (*Dealer stands on all 17s*, tanto *hard 17* quanto *soft 17*, ex.: Ás + 6 = 17 → para).
- **Atomicidade no Esgotamento Determinístico:** caso um baralho injetado acabe no meio das compras da banca em `parar()`, o motor restaura as cartas ao topo e preserva intactos o estado `TURNO_JOGADOR`, a carta oculta, a mão da banca e o saldo.
- **Fichas Virtuais em `long` e Regra da Aposta Par:** saldo e apostas usam `long` com `Math.addExact`, `Math.subtractExact` e `Math.multiplyExact`. `iniciarPartida(long valorAposta)` exige aposta estritamente positiva e par (`valorAposta % 2 == 0`) para pagamento 3:2 exato sem truncamento:
  $$\text{Retorno} = \text{aposta} + \frac{\text{aposta}}{2} \times 3$$
- **Liquidação Única:** a aposta é debitada ao iniciar a partida e o retorno é creditado exatamente uma única vez na transição para `FINALIZADA`.

### 3. Visibilidade Segura e Imutabilidade
- Durante `TURNO_JOGADOR`, `getCartasVisiveisDealer()` expõe apenas a 1ª carta aberta (`List.copyOf`) e `hasCartaOcultaDealer()` retorna `true`. Somente em `FINALIZADA` todas as cartas da banca são reveladas.

### 4. Matriz de Ações Permitidas por Estado

| Ação | `AGUARDANDO_APOSTA` | `TURNO_JOGADOR` | `FINALIZADA` |
|---|---|---|---|
| `iniciarPartida(valorAposta)` | **Permitido** (par e $\le$ saldo) | **Proibido** (retorna `false`) | **Permitido** (inicia nova rodada) |
| `pedirCarta()` (Hit) | **Proibido** (retorna `false`) | **Permitido** (se estourar $\to$ `FINALIZADA`) | **Proibido** (retorna `false`) |
| `parar()` (Stand) | **Proibido** (sem efeito) | **Permitido** (dealer joga $\to$ `FINALIZADA`) | **Proibido** (sem efeito) |

---

## Expansão Multijogos no Salão: Mesa e Interface Completa de Truco Paulista

Além do Blackjack, o salão conta com a **Mesa de Truco Paulista** 100% funcional:
- **Reaproveitamento de `CasinoTable`:** `gameName = "Truco"`, base sólida `80x50` em `(320, 280)` no setor sudeste do salão, com plaqueta visual `"TRUCO"`.
- **Critério Unificado de Proximidade:** raio de alcance de `35 px` com desempate determinístico pela menor distância euclidiana ao quadrado (`getDistanceSqToPlayer(player)`).
- **Interface Interativa (`TrucoTableView` em `GameState.MENU_TRUCO`):**
  - Exibe feltro azul anil, carta **Vira** aberta, indicação da **Manilha** da mão, cartas jogadas na vaza atual, placar das 3 vazas e versos fechados das cartas restantes do Bot (sem vazamento da mão secreta).
  - Permite jogar clicando nas cartas abertas da mão ou pelos atalhos `1`, `2`, `3`, iniciar nova mão com `N` após o desfecho e alternar com `B` / `E` preservando a mesma instância de `TrucoRound` em andamento.

### Regras Oficiais do Motor `TrucoRound` (`model`)
- **Baralho Limpo (40 cartas):** exclui 8, 9 e 10 (`TrucoDeck`).
- **Força das Cartas Comuns:**
  $$4 < 5 < 6 < 7 < \text{Q} < \text{J} < \text{K} < \text{A} < 2 < 3$$
  Cartas comuns de mesmo valor empatam a vaza (*canga*).
- **Manilhas (Vira Cíclica e Tombos):** rank imediatamente seguinte ao da vira ($3 \to 4$), desempatadas obrigatoriamente por naipe:
  $$\text{Paus (Zap)} > \text{Copas (Copeta)} > \text{Espadas (Espadilha)} > \text{Ouros (Picafumo)}$$
- **Resolução Completa de 3 Vazas e IA Honesta (`TrucoSimpleAI`):** cobre vitórias 2x0, 2x1 e todos os desempates de *canga* na 1ª, 2ª ou 3ª vaza; o Bot decide apenas com base na própria mão, na vira e na carta aberta na vaza atual.

---

## Guia Rápido de Apresentação e Defesa (Prova Prática)

### 1. Exemplos Diretos dos 4 Pilares no Código
1. **Abstração:** `MyShape` é `abstract` com `draw(Graphics2D)` abstrato; `BlackjackEngine` abstrai o motor da mesa de Blackjack.
2. **Encapsulamento:** validação de dimensões positivas em `MyShape`, invariantes 1:1 em `MySquare`/`MyCircle`, snapshots imutáveis `List.copyOf` em `Deck`/`Hand`/`TrucoRound` e proteção da carta oculta do dealer em `BlackjackGame`.
3. **Herança:** `MyRectangle`, `MyEllipse` e `MyCharacter` estendem `MyShape`; `MySquare`, `CasinoTable`, `CasinoCounter` e `CasinoChair` estendem `MyRectangle`; `MyCircle` estende `MyEllipse`.
4. **Polimorfismo:** `ScreenSaver` ordena `List<MyShape> entidadesRenderizaveis` por `getDepthZ()` e invoca `entidade.draw(g2d)` em laço único; `BlackjackTableView` renderiza qualquer implementação de `BlackjackEngine`.

### 2. Alterações Simples ao Vivo (Treino da Dupla)
- **Guilherme (Motor de Regras):** fazer o dealer comprar também em *Soft 17* em `BlackjackGame.parar()` usando `maoDealerSimulada.isMaoSoft()`, ou alterar o multiplicador de pagamento do Blackjack em `BlackjackGame`.
- **Enzo (Mundo e Interface):** adicionar uma nova `CasinoChair` sólida ao salão em `ScreenSaver.inicializarEntidades()` (herdando colisão e *z-sorting* automaticamente) ou ajustar `VELOCIDADE_AVATAR`.

---

## Fluxo Git e Comandos de Verificação

- **Padrão Obrigatório de Commits:** `tipo(escopo): descrição curta em português` (`feat`, `fix`, `refactor`, `test`, `docs`, `chore`).
- **Compilação e Execução de Todas as 9 Suítes (PowerShell):**
  ```powershell
  New-Item -ItemType Directory -Force -Path bin | Out-Null
  $arquivos = Get-ChildItem -Path src,tests -Recurse -Filter *.java | ForEach-Object { $_.FullName }
  javac -encoding UTF-8 -d bin -cp src $arquivos

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
