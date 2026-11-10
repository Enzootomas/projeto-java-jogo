# Plano de trabalho em dupla

Tema sugerido, ainda a confirmar: nave coleta cristais e desvia de obstáculos. Priorizar movimentação, colisões e renderização antes de efeitos e dificuldade.

| Etapa | Integrante A | Integrante B | Critério de aceite |
|---|---|---|---|
| 1 — Base e POO | MyShape, MyRectangle e MySquare | MyEllipse, MyCircle e diagrama | Hierarquia compila; ambos explicam os quatro pilares |
| 2 — Janela e controle | App, janela e loop | Input por estado das teclas | Tela cheia, ESC, WASD, setas e diagonais funcionam |
| 3 — Recursos e colisão | MyCharacter e sprites | Clip e colisões | Imagem visível, som pré-carregado e laço reverso |
| 4 — Regras do jogo | Pontuação e coleta | Vidas e fim de jogo | Partida completa e reinício verificáveis |
| 5 — Entrega | Testar e revisar código de B | Testar e revisar código de A | Checklist completo e apresentação ensaiada |

A e B são papéis provisórios, a preencher com os nomes da dupla. Ambos revisam e compreendem o código inteiro; alternar quem implementa e quem explica.

## Git
1. Clonar o repositório e criar uma branch por tarefa, por exemplo feat/movimentacao.
2. Fazer commits próprios com mensagens claras, como feat: adicionar controle diagonal.
3. Abrir pull request com objetivo, arquivos alterados e validação.
4. O colega revisa, executa e pede explicações antes da integração.
5. Atualizar main local antes da próxima tarefa. Não usar force push na main.

## Modelo de tarefa
- Objetivo:
- Responsável e revisor:
- Arquivos e passos de implementação:
- Conceito de POO aplicado:
- Critérios de aceite:
- Testes realizados:

## Perguntas para os dois
Explicar draw polimórfico, invariantes de quadrado/círculo, estados das teclas, colisão bounding box, laço reverso e ciclo de vida do Clip. Cada um deve conseguir alterar uma regra simples durante a apresentação.
