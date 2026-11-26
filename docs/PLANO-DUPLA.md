# Plano de trabalho em dupla

Tema confirmado: **Cassino Social 2D Isométrico (estilo Habbo)**. O jogador controla seu avatar pelo salão do cassino, desvia de mesas, cadeiras, balcão e paredes, interage contextualmente com as três mesas de jogo e disputa partidas solo com fichas 100% virtuais (**Blackjack**, **Truco Paulista** e **Poker Texas Hold'em**).

## Identificação da Dupla
- **Integrante A — Enzo Gabriel Tomas De Souza (`@Enzootomas`):** Frente Mundo Isométrico, Hierarquia Visual, Interfaces das Mesas (`ScreenSaver`, `BlackjackTableView`, `TrucoTableView`, layout inicial de `PokerTableView`), Áudio e Truco Paulista.
- **Integrante B — Guilherme Bueno (`@guibuenoc`):** Frente Motores de Regras do Blackjack (`Deck`, `Hand`, `BlackjackGame`) e do Poker Texas Hold'em (`PokerEngine`, `PokerHandEvaluator`, `PokerSimpleAI`, `TexasHoldemGame`), Integração Final do Poker na Interface, Testes Determinísticos e Documentação de Entrega.

| Etapa | Enzo (`@Enzootomas`) | Guilherme Bueno (`@guibuenoc`) | Critério de aceite |
|---|---|---|---|
| **1 — Base e POO [CONCLUÍDA]** | `MyShape`, `MyRectangle` e `MySquare` | `MyEllipse`, `MyCircle` e suíte base de POO | Hierarquia compila; invariantes validados; pilares explicados |
| **2 — Salão Isométrico, Janela e Controles [CONCLUÍDA]** | `App` (tela cheia/ESC), loop (`Timer` 60 FPS com `dt`), projeção 2:1 e `IsometricHelper` | Revisão de `ScreenSaver` (Key Bindings, *z-sorting*), `MyCharacter` (sprite PNG, som WAV, *footprint*) | Janela sem bordas em tela cheia, `ESC` encerra, movimento normalizado, colisão por eixos com deslizamento |
| **3 — Mobília e Ambientação do Salão [CONCLUÍDA]** | `CasinoCounter`, `CasinoChair`, paredes isométricas, portal com letreiro e tapete | Validação de rotas livres, profundidade frente/trás e testes de colisão | Salão ambientado com *z-sorting* correto e 77 verificações em `TestePOO` passando |
| **4 — Mesas de Blackjack, Truco e Poker Integradas [CONCLUÍDA]** | `BlackjackTableView`, `TrucoRound` + `TrucoTableView`, 3ª mesa de Poker no salão e layout de `PokerTableView` | `Deck`, `Hand`, `BlackjackGame`, contrato `PokerEngine`, `TexasHoldemGame`, `PokerHandEvaluator`, `PokerSimpleAI` e conexão final `PokerTableView` ↔ `PokerEngine` | Três mesas jogáveis no salão com rotas livres, cartas ocultas protegidas, ações contextuais e preservação por `B`/`E` |
| **5 — Revisão Final, Testes e Preparação da Entrega [EM FINALIZAÇÃO]** | Revisão de PRs e ensaio da apresentação | Bateria de 11 suítes em processos separados (`exit 0`), `try/finally` nos testes Swing e documentação final | 11 suítes passando (`exit 0`); compilação limpa via PowerShell/IDE; guia de apresentação pronto |

---

## Contrato 1: Interface (View) $\leftrightarrow$ Motor de Blackjack (`BlackjackEngine`)
- **Tipos compartilhados:** `Suit`, `Rank`, `Card` (imutável), `RoundState` (`AGUARDANDO_APOSTA`, `TURNO_JOGADOR`, `FINALIZADA`), `RoundResult`.
- **Regras:** 1 baralho de 52 cartas (`SecureRandom` em produção, determinístico em testes), dealer para em todo 17 (inclusive *soft 17*), apostas estritamente positivas e pares (`long` com `Math.addExact`/`Math.multiplyExact`) para pagamento 3:2 exato no Blackjack Natural, liquidação única, rollback atômico no esgotamento determinístico e ocultação estrita da 2ª carta do dealer durante `TURNO_JOGADOR`.

---

## Contrato 2: Mesa e Motor do Truco Paulista (`TrucoRound` + `TrucoTableView`)
- **Posicionamento:** `CasinoTable` (`gameName = "Truco"`) em `(320, 280)`, base sólida `80x50`.
- **Regras:** baralho limpo de 40 cartas (`TrucoDeck`), vira e manilhas cíclicas (*Zap > Copeta > Espadilha > Picafumo*), melhor de 3 vazas com todas as regras de empate (*canga*), ocultação rigorosa das cartas do Bot e IA honesta (`TrucoSimpleAI`).
- **Controles:** clique nas cartas da mão ou teclas `1`, `2`, `3`; `N` para nova mão; `B` / `E` preservando a mão em andamento.

---

## Contrato 3: Terceira Mesa e Motor de Poker Texas Hold'em (`PokerEngine` + `TexasHoldemGame` + `PokerTableView`)
- **Posicionamento e Circulação:** `CasinoTable` (`gameName = "Poker"`) em `(340, 50)` no setor nordeste, base sólida `80x50`, com `40 px` de folga vertical até as cadeiras (`y = 140`, avatar `24x24`), mantendo rotas livres entre Blackjack `(160, 160)`, Truco `(320, 280)` e Poker `(340, 50)`.
- **Regras da Primeira Versão (Heads-Up 1v1):**
  - **Blinds Pares e Alternância de Botão:** `SMALL_BLIND_PADRAO = 50L` e `BIG_BLIND_PADRAO = 100L`. No Heads-Up, o **Botão (Dealer)** paga o Small Blind e **age primeiro no `PRE_FLOP`**, passando a **agir por último** no `FLOP`, `TURN` e `RIVER`. O botão alterna a cada nova mão.
  - **Ações Válidas (`getAcoesValidasJogador()`):** `DESISTIR` (`F`), `PASSAR` (`C`/`Espaço` quando `valorParaPagar == 0`), `PAGAR` (`C`/`Espaço` quando `valorParaPagar > 0`) e `AUMENTAR` (`R`/`1` mínimo `getAumentoMinimoJogador()`, `2` = 2x BB, `3` = All-In `getAumentoMaximoJogador()`), com teto de `MAX_AUMENTOS_POR_RUA = 3` aumentos por rua.
  - **All-In, Excesso e Split Pot:** excesso não igualado (*uncalled bet*) é devolvido imediatamente; quando um lado fica em *all-in* com apostas igualadas, as comunitárias restantes são abertas até o `SHOWDOWN`; em empate exato na melhor mão de 5 cartas dentre 7 (`PokerHandEvaluator`), o pote é dividido igualmente.
  - **Ocultação Estrita das Cartas do Bot:** `getCartasVisiveisBot()` retorna lista vazia durante todas as ruas de apostas **e também após desistência (*Fold*)**; as 2 cartas privadas do Bot são reveladas **exclusivamente no `SHOWDOWN`**.
  - **Preservação de Estado:** ao sair com `B` e voltar com `E`, `ScreenSaver` preserva exatamente a mesma instância de `PokerEngine`.

---

## Guia Rápido de Apresentação e Defesa (Prova Prática)

### 1. Exemplos Diretos dos 4 Pilares no Código
1. **Abstração:** `MyShape` é `abstract` com `draw(Graphics2D)` abstrato; `BlackjackEngine`, `PokerEngine` e `PokerBotStrategy` abstraem os motores e a IA.
2. **Encapsulamento:** validação de dimensões positivas em `MyShape`, invariantes 1:1 em `MySquare`/`MyCircle`, snapshots imutáveis `List.copyOf` em `Deck`/`Hand`/`TrucoRound`/`TexasHoldemGame` e ocultação estrita das cartas fechadas do Dealer/Bot.
3. **Herança:** `MyRectangle`, `MyEllipse` e `MyCharacter` estendem `MyShape`; `MySquare`, `CasinoTable`, `CasinoCounter` e `CasinoChair` estendem `MyRectangle`; `MyCircle` estende `MyEllipse`.
4. **Polimorfismo:** `ScreenSaver` ordena `List<MyShape> entidadesRenderizaveis` por `getDepthZ()` e invoca `entidade.draw(g2d)`; `BlackjackTableView` e `PokerTableView` operam exclusivamente sobre interfaces (`BlackjackEngine` e `PokerEngine`).

### 2. Alterações Simples ao Vivo (Treino da Dupla)
- **Guilherme (Motores de Regras):** fazer o dealer comprar também em *Soft 17* em `BlackjackGame.parar()` usando `maoDealerSimulada.isMaoSoft()`, ou alterar `SMALL_BLIND_PADRAO`/`BIG_BLIND_PADRAO`/`MAX_AUMENTOS_POR_RUA` em `TexasHoldemGame`.
- **Enzo (Mundo e Interface):** adicionar uma nova `CasinoChair` sólida ao salão em `ScreenSaver.inicializarEntidades()` (herdando colisão e *z-sorting* automaticamente) ou ajustar `VELOCIDADE_AVATAR`.

---

## Comandos de Compilação e Testes (Windows PowerShell)
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
java -cp "bin;src" tests.TestePokerHoldem
java -cp "bin;src" tests.TesteInterfacePoker
```
