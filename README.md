# Projeto Java Jogo - Cassino Social 2D Isométrico

Trabalho didático em dupla de Programação Orientada a Objetos e jogos 2D desktop, com Java Swing, Graphics2D e Java Sound.

## Tema Confirmado
Cassino social 2D isométrico em pixel art (estilo Habbo Hotel) com identidade própria. O jogador controla seu avatar pelo salão do cassino, desvia de obstáculos/mobílias, aproxima-se de mesas interativas para jogar partidas solo contra bots (iniciando por Blackjack com fichas 100% virtuais) e explora o ambiente.

## Estado Atual
- **Etapa 1 (Concluída e testada):** Hierarquia base de formas geométricas (`MyShape`, `MyRectangle`, `MySquare`, `MyEllipse`, `MyCircle`), validação de dimensões e invariantes, algoritmo de colisão bounding box `intersects`, e testes didáticos automatizados com inspeção de pixels.
- **Etapa 2 (Concluída e testada):** Janela Swing em tela cheia sem bordas (`App.java`), loop a 60 FPS com delta time, salão isométrico 2:1 com tiles losangos, controles contínuos com Swing Key Bindings (WASD/setas e diagonais normalizadas), avatar com sprite PNG real (`MyCharacter`) e base de colisão desacoplada das dimensões visuais, colisão com deslizamento por eixos, ordenação de profundidade (z-sorting), mesa de Blackjack (`CasinoTable`), balcão de interação (`[E] Interagir`) com áudio WAV pré-carregado via `Clip`, e tela de transição do Blackjack com retorno e tecla ESC funcional em qualquer tela.
- **Bateria de Testes:** 47 verificações automáticas passam com 0 falhas em `tests/TestePOO.java` + teste de renderização visual off-screen em `tests/TesteRenderizacao.java`.

## Objetivo Didático
Compreender e demonstrar abstração, encapsulamento, herança e polimorfismo. Os dois integrantes devem saber explicar toda a lógica, inclusive alterações feitas com auxílio de IA.

## Comece por aqui
- [Requisitos e checklist](docs/REQUISITOS.md)
- [Divisão de trabalho e revisão em dupla](docs/PLANO-DUPLA.md)
- [Arquitetura de referência](docs/ARQUITETURA.md)
- [Prompt inicial para o Antigravity](docs/ANTIGRAVITY.md)
- [Instruções para assistentes de programação](AGENTS.md)

## Abrir no Antigravity
Clone este repositório pela opção de clonar repositório do seu editor ou pelo terminal:

```bash
git clone https://github.com/Enzootomas/projeto-java-jogo.git
cd projeto-java-jogo
```

Abra a pasta no Antigravity e utilize docs/ANTIGRAVITY.md. Por ser privado, o Git poderá pedir autenticação. Não coloque credenciais no código.

## Execução prevista
Após implementar os arquivos Java e com um JDK instalado:

```bash
javac -encoding UTF-8 -d bin -cp src src/App.java src/model/*.java src/view/*.java
java -cp bin:src App
```

No Windows use `java -cp "bin;src" App`. O classpath inclui src para disponibilizar image/ e sound/ durante o desenvolvimento; na distribuição, os recursos devem acompanhar os bytecodes.

## Equipe
- Integrante A: a preencher.
- Integrante B: a preencher.

Entrega indicada no documento: 23 a 27 de novembro de 2026. Prova: 27 de novembro. Confirmar a data específica com o professor.
