# Instruções para desenvolvimento

Este é um trabalho didático em dupla de POO e jogos 2D. A equipe deve compreender e explicar todo o código.

- Usar Java desktop, Swing, Graphics2D e Java Sound Clip. Não converter para aplicação web ou outro framework.
- Antes de implementar, ler README.md, docs/REQUISITOS.md e docs/PLANO-DUPLA.md.
- Implementar uma etapa pequena por vez; explicar objetivo, classes alteradas, conceito de POO aplicado e como verificar.
- Preservar MyShape abstrata; MyRectangle e MyEllipse derivadas; MySquare de MyRectangle; MyCircle de MyEllipse; MyCharacter de MyShape.
- Encapsular estado e validar dimensões. Quadrados e círculos devem manter dimensões iguais nos setters.
- Demonstrar polimorfismo real por List<MyShape> e draw sobrescrito.
- Movimentação simultânea por teclas pressionadas, permitindo diagonais. ESC encerra; janela sem bordas em tela cheia.
- Pré-carregar sprites e áudio. Nunca abrir arquivos de som a cada colisão; rebobinar Clip antes de tocar.
- Remover elementos de listas por laço reverso. Não remover dentro de for-each.
- Manter código simples, nomes claros e comentários que expliquem decisões. Evitar abstrações sem necessidade.
- Atualizar documentação e diagrama quando mudar a arquitetura.
- Compilar e relatar exatamente os testes executados; não afirmar que GUI ou som foram testados sem execução.
- Não marcar requisitos como concluídos antes da verificação.
- Não criar histórico de contribuições fictício. Cada integrante deve realizar seus próprios commits e revisar o colega.
