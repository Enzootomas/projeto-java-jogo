# Projeto Java Jogo

Trabalho didático em dupla de Programação Orientada a Objetos e jogos 2D desktop, com Java Swing, Graphics2D e Java Sound.

## Estado atual
Repositório preparado com planejamento e instruções. O jogo ainda não foi implementado. Código-base do professor e tema definitivo ainda precisam ser confirmados. Não há compilação ou testes do jogo nesta etapa.

## Objetivo didático
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
