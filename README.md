# HabilidadesPlus

Plugin de RPG para **Paper 26.2**, inspirado no sistema de progressao do mcMMO.
Mostra XP e nivel na action bar e oferece uma interface grafica pelos comandos
`/mcmmo` e `/habilidades`.

## Requisitos

- Servidor Paper compativel com Minecraft 26.2
- Java 25 ou mais novo
- Maven 3.9+ apenas para compilar o projeto

## Instalacao

1. Baixe o artefato `habilidadeplus.jar` da execucao mais recente do GitHub Actions
   ou compile o projeto.
2. Copie o arquivo para a pasta `plugins/` do servidor.
3. Reinicie o servidor. Nao use gerenciadores de hot reload de plugins.
4. Confirme no console a mensagem `HabilidadesPlus habilitado`.
5. Dentro do jogo, use `/mcmmo` ou `/habilidades`.

Se os comandos aparecerem como desconhecidos, confira se o plugin aparece em
`/plugins` e procure no console o erro ocorrido durante a inicializacao. Um
servidor incompatível ou uma versao antiga do Java impede o registro dos comandos.

## Funcionalidades

- 18 habilidades com XP e nivel individuais.
- Curvas de nivel LINEAR e EXPONENCIAL configuraveis.
- Action bar que agrupa ganhos de XP para evitar spam.
- Menu com progresso, poder total e catalogo de poderes.
- Ranking carregado em segundo plano e mantido em cache para evitar leitura de disco ao abrir a GUI.
- Ao abrir o ranking pelo livro com pena, o filtro seleciona automaticamente a habilidade em que o jogador é Top 1.
- Mineração respeita permissão, mundos desativados, ferramenta adequada e proteção contra blocos colocados.
- Marcadores de blocos colocados são limitados e salvos de forma assíncrona durante o autosave.
- Persistencia por UUID em `plugins/HabilidadesPlus/playerdata/`.
- Autosave configuravel e escrita atomica dos arquivos dos jogadores.
- Protecao antifarm persistente para blocos colocados por jogadores.
- XP de Alquimia concedido somente apos uma fermentacao real.
- Rolamento de Acrobacia funcional.
- `/mcmmo reload` para recarregar configuracoes.

### Estado dos poderes

O Rolamento de Acrobacia esta implementado. Os demais poderes exibidos no
catalogo sao propostas para evolucoes futuras e aparecem claramente marcados
como **planejados**, sem indicar falsamente que ja afetam o jogo.

## Comandos e permissoes

| Comando | Permissao | Padrao |
| --- | --- | --- |
| `/mcmmo`, `/habilidades` | `habilidadesplus.use` | Todos os jogadores |
| `/mcmmo reload` | `habilidadesplus.admin` | Operadores |

A permissao `habilidadesplus.use` possui `default: true`; portanto, qualquer
jogador pode usar os comandos e ganhar XP sem configuracao adicional.

## Configuracao

- `config.yml`: XP, curva de nivel, nivel maximo, mundos desativados e autosave.
- `messages.yml`: mensagens, cores e textos da GUI.
- `playerdata/<uuid>.yml`: progresso individual.
- `placed-blocks.yml`: blocos colocados protegidos contra farm de XP.

O limite de marcadores persistidos pode ser ajustado em `geral.max-blocos-protegidos`.

Depois de alterar `config.yml` ou `messages.yml`, execute `/mcmmo reload`.

## Compilacao e testes

```bash
mvn clean verify
```

O arquivo final sera criado em:

```text
target/habilidadeplus.jar
```

O GitHub Actions executa a compilacao e os testes automaticamente em pushes e
pull requests direcionados para `main`.

## Estrutura

```text
src/main/java/com/rpgcustom/habilidadesplus/
├── commands/    comandos e autocomplete
├── data/        perfis e persistencia
├── gui/         menu e catalogo de poderes
├── leveling/    curvas e level up
├── listeners/   fontes de XP e protecoes
├── util/        configuracoes, mensagens e blocos protegidos
└── xp/          concessao de XP e action bar
```
