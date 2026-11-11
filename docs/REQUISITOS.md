# Requisitos e objetivo didático

## Objetivo
Consolidar abstração, encapsulamento, herança e polimorfismo em um jogo 2D Java desktop. Ambos os integrantes devem explicar toda a solução, inclusive código auxiliado por IA.

| Pilar | Evidência esperada | Pergunta de apresentação |
|---|---|---|
| Abstração | MyShape abstrata com draw abstrato | Por que não instanciamos MyShape? |
| Encapsulamento | Estado protegido/privado, acesso controlado e dimensões válidas | Como impedimos estado inválido? |
| Herança | Hierarquia oficial de formas e personagem | O que é herdado e o que é especializado? |
| Polimorfismo | List<MyShape> chamando draw sobrescrito | Como a mesma chamada desenha objetos diferentes? |

## Checklist obrigatório
- [x] Hierarquia oficial de formas e construtores conforme documento do professor (`MyShape`, `MyRectangle`, `MySquare`, `MyEllipse`, `MyCircle`).
- [x] Validação de dimensões estritamente positivas e invariantes de quadrado e círculo mantidos.
- [x] Java Swing desktop: JFrame, JPanel e Graphics2D (sem libGDX, web ou frameworks externos).
- [x] Tela cheia sem bordas ocupando a resolução do monitor (`GraphicsDevice.setFullScreenWindow` / `setUndecorated(true)`).
- [x] Tecla ESC encerra o jogo imediatamente em qualquer estado.
- [x] Setas e WASD com controle simultâneo de teclas pressionadas (Swing Key Bindings), normalização de vetor e diagonais fluidas.
- [x] Salão isométrico renderizado por tiles com proporção 2:1 exata e ordenação de profundidade (z-sorting) pela base dos pés.
- [x] Colisão no plano do chão (*footprint* / base) calculada via `intersects(MyShape)` com deslizamento por eixos, sem usar o sprite vertical inteiro como obstáculo.
- [x] Sprites PNG reais de avatar e ambiente carregados na memória antes do loop.
- [x] Áudio WAV de interação pré-carregado com `Clip`, rebobinado (`setFramePosition(0)`) antes da reprodução e liberado no encerramento.
- [x] Mesa interativa que detecta proximidade (`isPlayerNear`), exibe prompt e abre a tela de transição do Blackjack com botão para voltar ao salão.
- [ ] Implementação completa das cartas e apostas do Blackjack solo (Etapa 4).
- [ ] Remoção de elementos por laço reverso em coleções dinâmicas de cartas/fichas.
- [x] Código compila e executa por terminal PowerShell e IDE.
- [x] README, diagrama Mermaid e histórico Git da dupla.

## Organização prevista
- `src/App.java`: inicialização do JFrame em tela cheia na Event Dispatch Thread (EDT).
- `src/model/`:
  - `MyShape.java`, `MyRectangle.java`, `MySquare.java`, `MyEllipse.java`, `MyCircle.java` (Geometria e colisões da base).
  - `MyCharacter.java`: avatar do jogador que estende `MyShape`, contendo sprite, direção, animação e áudio.
  - `CasinoTable.java` / `Obstacle.java`: mobílias e mesas do cassino com base de colisão e gatilho de interação.
  - `BlackjackGame.java`: regras da partida solo contra o crupiê (fichas virtuais).
- `src/view/ScreenSaver.java` (ou painel do cassino): renderização isométrica, z-sorting, loop do jogo (`Timer` a 60 FPS) e escuta de teclado.
- `src/image/` e `src/sound/`: recursos gráficos (pixel art) e efeitos sonoros WAV.
- `tests/TestePOO.java`: testes automatizados de lógica e POO.

## Entrega
O documento fornecido indica entrega de 23/11 a 27/11/2026 e prova em 27/11. Conferir o dia específico da dupla com o professor.

## Verificação manual
Executar na máquina da apresentação: conferir tela cheia sem bordas, ESC, resposta de WASD e setas com diagonais, movimentação fluida do avatar pelo salão isométrico, colisão correta nos pés das mesas/paredes, ordenação correta de quem está na frente/atrás (z-sorting), abertura do Blackjack ao interagir com a mesa, som sem engasgos e encerramento limpo.
