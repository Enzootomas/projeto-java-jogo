# Plano de trabalho em dupla

Tema confirmado: **Cassino Social 2D Isométrico (estilo Habbo)**. O jogador controla seu avatar pelo salão do cassino, desvia de mesas e paredes, interage com a mesa de jogo e joga Blackjack solo contra o crupiê (fichas virtuais).

| Etapa | Integrante A | Integrante B | Critério de aceite |
|---|---|---|---|
| **1 — Base e POO [CONCLUÍDA]** | `MyShape`, `MyRectangle` e `MySquare` | `MyEllipse`, `MyCircle` e testes | Hierarquia compila; 32 testes automáticos passam; pilares explicados |
| **2 — Salão Isométrico, Janela e Controles [CONCLUÍDA]** | `App` (tela cheia/ESC), loop (Timer 60 FPS com dt), projeção 2:1 e `IsometricHelper` | `ScreenSaver` (Key Bindings, z-sorting, mesa), `MyCharacter` (sprite PNG, som WAV, footprint) | Janela sem bordas em tela cheia, ESC encerra, movimento normalizado, colisão por eixos com deslizamento, 47 testes passando |
| **3 — Mobília e NPCs do Salão** | Criação de novos obstáculos e balcões com footprints | Comportamento de perambulação simples de NPCs no salão | Salão mais vivo com novos elementos que respeitam z-sorting e colisões |
| **4 — Mesa Interativa e Blackjack Solo** | Gatilho de interação com a mesa e interface de jogo | Motor de regras do Blackjack e ciclo de apostas (fichas virtuais) | Chegar perto da mesa abre o Blackjack; jogador aposta, pede carta/para e volta ao salão |
| **5 — Áudio, Polimento e Entrega** | Efeitos sonoros (`Clip` pré-carregado e rebobinado) | Revisão geral, remoção por laço reverso e testes na máquina da apresentação | Áudio sem travamentos; código compila via PowerShell/IDE; ensaio dos dois integrantes |

## Divisão de Frentes de Trabalho

O desenvolvimento segue dividido em duas frentes complementares e independentes:

1. **Frente Mundo e Interface (Enzo):**
   * Salão isométrico, avatar, controles contínuos, detecção de colisões no chão, z-sorting, transição de telas/menus, sprites em pixel art e reprodução de efeitos sonoros com `Clip`.
   * Branches de trabalho: `feat/mundo-<tarefa>`
2. **Frente Regras e Motor do Jogo (Colega):**
   * Baralho de 52 cartas, distribuição, cálculo de pontuação (Ás como 1 ou 11), mãos do jogador e do dealer, inteligência do dealer (parar no 17), ciclo de apostas e gestão de saldo de fichas virtuais.
   * Branches de trabalho: `feat/regras-<tarefa>`

Ambos trabalham em seus respectivos computadores, com suas próprias contas Git. **Nenhum integrante faz commit ou push diretamente na branch `main`**. O colega iniciará seus trabalhos puxando a branch compartilhada `feat/regras-contrato`.

---

## Contrato de Integração Oficial: Interface (View) $\leftrightarrow$ Motor de Regras (Model)

Para garantir que o colega possa desenvolver o motor do Blackjack e Enzo possa desenvolver a interface da mesa sem acoplamento ou vazamento de segredos, formalizamos a interface `BlackjackEngine` e seus tipos de suporte:

### 1. Tipos de Apoio
- **`Suit` (Enum):** `COPAS`, `ESPADAS`, `OUROS`, `PAUS` (com símbolo e nome em português).
- **`Rank` (Enum):** `AS` a `REI` (valores de Blackjack 2 a 10, figuras valendo 10 e Ás valendo 11 ou 1).
- **`Card` (Classe Imutável):** representa a carta com naipe e valor, com formatação para apresentação textual e visual.
- **`RoundState` (Enum):** estados explícitos da rodada:
  - `AGUARDANDO_APOSTA`: início ou entre rodadas.
  - `TURNO_JOGADOR`: cartas distribuídas, dealer com 1 carta visível e 1 oculta virada para baixo.
  - `FINALIZADA`: desfecho apurado, carta oculta revelada e saldo liquidado.
- **`RoundResult` (Enum):** resultados com liquidação matemática:
  - `EM_ANDAMENTO`: rodada ativa.
  - `JOGADOR_BLACKJACK`: vitória com 21 natural. Paga **3:2** sobre a aposta.
  - `JOGADOR_VITORIA`: pontuação maior que dealer ou dealer estourou. Paga **1:1**.
  - `EMPATE_PUSH`: empate na pontuação final. Devolve a aposta original integralmente.
  - `JOGADOR_ESTOUROU` / `DEALER_VITORIA` / `DEALER_BLACKJACK`: derrota do jogador (retorno 0).

### 2. Regras Específicas da Partida Solo (v1) e Matemática Financeira
- **Quantidade de Baralhos:** 1 baralho francês padrão de **52 cartas** (`Deck.TOTAL_CARTAS_PADRAO = 52`), embaralhado com `SecureRandom` em produção e injetável com `Random` / sequência determinística nos testes.
- **Comportamento do Dealer no 17 (incluindo *Soft 17*):** o dealer compra cartas enquanto sua pontuação for menor que 17 e **para em todo 17 ou superior** (*Dealer stands on all 17s*, tanto *hard 17* quanto *soft 17*, ex.: Ás + 6 = 17 → para).
- **Blackjack Natural vs. 21 Comum e Empate (Push):**
  - **Blackjack Natural** ocorre exclusivamente com **exatamente 2 cartas** somando 21 (1 Ás + 1 carta de valor 10). Mãos de 3 ou mais cartas somando 21 são tratadas como **21 comum**.
  - Se ambos (jogador e dealer) obtiverem Blackjack Natural nas 2 cartas iniciais, o resultado é `EMPATE_PUSH` (devolução integral da aposta).
  - Não há *split*, *seguro (insurance)*, *double down* ou *surrender* nesta primeira versão.
- **Fichas Virtuais em `long` e Prevenção de Overflow:** saldo e apostas utilizam o tipo primitivo `long`, validando limites e empregando aritmética verificada (`Math.addExact` / `Math.multiplyExact`) nos cálculos de liquidação.
- **Regra da Aposta Par:** para garantir que o pagamento padrão de Blackjack 3:2 seja efetuado por divisão inteira exata sem truncamento ou centavos perdidos:
  $$\text{Retorno} = \text{aposta} + \frac{\text{aposta} \times 3}{2}$$
  O método `iniciarPartida(long valorAposta)` **aceita exclusivamente valores estritamente positivos e pares** (`valorAposta % 2 == 0`). Apostas ímpares devem ser rejeitadas retornando `false`.
- **Liquidação Única:** a aposta é debitada do saldo no momento em que `iniciarPartida` é chamado. O crédito do prêmio é processado **exatamente uma única vez** no momento da transição para o estado `FINALIZADA`.

### 3. Visibilidade Segura e Proteção contra Vazamento de Cartas
- Durante o `TURNO_JOGADOR`, a interface precisa saber que há uma segunda carta na mesa virada para baixo, mas **não pode ter acesso à carta oculta** antes da sua revelação:
  - `getCartasVisiveisDealer()` retorna apenas a primeira carta visível durante o turno do jogador.
  - `hasCartaOcultaDealer()` retorna `true` durante o turno do jogador, permitindo à View desenhar o verso da carta.
  - Somente após o jogador parar (`parar()`) ou a rodada transitar para `FINALIZADA`, `getCartasVisiveisDealer()` passa a retornar todas as cartas da banca e `hasCartaOcultaDealer()` passa a retornar `false`.
- **Imutabilidade:** tanto `getMaoJogador()` quanto `getCartasVisiveisDealer()` retornam listas protegidas por `Collections.unmodifiableList`, impedindo que a interface modifique as coleções internas do motor.

### 4. Matriz de Ações Permitidas por Estado

| Ação | `AGUARDANDO_APOSTA` | `TURNO_JOGADOR` | `FINALIZADA` |
|---|---|---|---|
| `iniciarPartida(valorAposta)` | **Permitido** (deve ser par e $\le$ saldo) | **Proibido** (retorna `false`) | **Permitido** (inicia nova rodada) |
| `pedirCarta()` (Hit) | **Proibido** (retorna `false`) | **Permitido** (se estourar $\to$ `FINALIZADA`) | **Proibido** (retorna `false`) |
| `parar()` (Stand) | **Proibido** (sem efeito) | **Permitido** (dealer joga $\to$ `FINALIZADA`) | **Proibido** (sem efeito) |

### 5. Código da Interface `BlackjackEngine`

```java
package model;

import java.util.List;

public interface BlackjackEngine {
    boolean iniciarPartida(long valorAposta);
    boolean pedirCarta();
    void parar();

    long getSaldoVirtual();
    long getApostaAtual();
    RoundState getEstadoRodada();
    RoundResult getResultadoRodada();

    List<Card> getMaoJogador();
    List<Card> getCartasVisiveisDealer();
    boolean hasCartaOcultaDealer();
    int getPontosJogador();
    int getPontosDealerVisiveis();
}
```

---

## Fluxo Git da Dupla

1. **Branch de Contrato Compartilhada:** `feat/regras-contrato` (contém o contrato oficial compilado, tipos de dados imutáveis e testes automatizados).
2. **Para o Colega Iniciar:**
   ```bash
   git fetch origin
   git checkout feat/regras-contrato
   git checkout -b feat/regras-<nome-da-tarefa>
   ```
3. **Padrão Obrigatório de Commits:**
   `tipo(escopo): descrição curta em português`
   Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
4. **Revisão:**
   Cada integrante abre Pull Request ou submete sua branch para revisão do colega antes de qualquer merge.

