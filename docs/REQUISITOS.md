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
- [ ] Java Swing: JFrame, JPanel e Graphics2D.
- [ ] Hierarquia oficial e construtores conforme documento do professor.
- [ ] Tela cheia sem bordas ocupando o monitor.
- [ ] ESC encerra imediatamente.
- [ ] Setas e WASD com movimento diagonal simultâneo.
- [ ] Sprites carregados e renderizados.
- [ ] Áudio WAV pré-carregado com Clip e rebobinado antes da reprodução.
- [ ] Colisão por bounding box em intersects(MyShape).
- [ ] Remoção de elementos por laço reverso.
- [ ] Código compila e executa por terminal e IDE.
- [ ] README, diagrama Mermaid e histórico Git da dupla.

## Organização prevista
src/App.java; src/model/{MyShape,MyRectangle,MySquare,MyEllipse,MyCircle,MyCharacter}.java; src/view/ScreenSaver.java; src/image/; src/sound/; bin/ gerado pela compilação.

ScreenSaver concentra entidades, init, atualização/actionPerformed e renderização/paintComponent. Pré-carregamento ocorre antes do loop de jogo.

## Entrega
O documento fornecido indica entrega de 23/11 a 27/11/2026 e prova em 27/11. Conferir o dia específico da dupla com o professor.

## Verificação manual
Executar na máquina da apresentação: conferir tela cheia, ESC, cada controle, diagonais, limites, colisões repetidas, som sem travamentos e remoção de múltiplos objetos no mesmo quadro.
