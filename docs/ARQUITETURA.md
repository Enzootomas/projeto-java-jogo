# Arquitetura de referência

Planejada; ainda sem implementação Java neste repositório.

```mermaid
classDiagram
    MyShape <|-- MyRectangle
    MyRectangle <|-- MySquare
    MyShape <|-- MyEllipse
    MyEllipse <|-- MyCircle
    MyShape <|-- MyCharacter
    class MyShape {
        <<abstract>>
        +draw(Graphics2D g)* void
        +move(int screenWidth, int screenHeight) void
        +intersects(MyShape other) boolean
    }
    class MyCharacter {
        -Image sprite
        -Clip collisionClip
        +draw(Graphics2D g) void
        +playSound() void
    }
```

App inicializa Swing na EDT. ScreenSaver mantém janela, painel, entrada e loop. O modelo contém as entidades. Uma List<MyShape> permite desenhar formas e personagem por polimorfismo. Este diagrama resume a hierarquia; o documento do professor define a lista completa de atributos, construtores e métodos.
