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

Ambos trabalham em seus respectivos computadores, com suas próprias contas Git. **Nenhum integrante faz commit ou push diretamente na branch `main`**. O colega iniciará seus trabalhos puxando a branch compartilhada `feat/base-cassino`.

---

## Contrato de Integração: Interface (View) $\leftrightarrow$ Motor de Regras (Model)

Para garantir que o colega possa desenvolver o motor do Blackjack e Enzo possa desenvolver a interface da mesa sem dependência mútua, definimos o seguinte contrato conceitual simples em Java:

```java
package model;

import java.util.List;

public interface BlackjackEngine {
    // Ações do Jogador
    boolean iniciarPartida(int valorAposta);
    boolean pedirCarta(); // Hit
    void parar();         // Stand

    // Consultas de Estado para a Interface (View)
    int getSaldoVirtual();
    int getApostaAtual();
    List<Card> getMaoJogador();
    List<Card> getMaoDealer();
    int getPontosJogador();
    int getPontosDealerVisiveis();
    boolean isRodadaFinalizada();
    String getResultadoTexto(); // "Jogador Venceu", "Dealer Venceu", "Empate", etc.
}
```

* O colega implementará as classes de regras (`Card`, `Deck`, `Hand`, `BlackjackGame implements BlackjackEngine`).
* A interface (`ScreenSaver` / tela de Blackjack) apenas consultará esses métodos para desenhar as cartas e processar os botões de ação.

---

## Fluxo Git da Dupla

1. **Branch Base Compartilhada:** `feat/base-cassino` (contém o salão isométrico, hierarquia de formas, avatar, assets e testes das Etapas 1 e 2).
2. **Para o Colega Iniciar:**
   ```bash
   git fetch origin
   git checkout feat/base-cassino
   git checkout -b feat/regras-<nome-da-tarefa>
   ```
3. **Padrão Obrigatório de Commits:**
   `tipo(escopo): descrição curta em português`
   Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
4. **Revisão:**
   Cada integrante abre Pull Request ou submete sua branch para revisão do colega antes de qualquer merge.

