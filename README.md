# HabilidadesPlus

Plugin de RPG customizado **inspirado no mcMMO** para **Minecraft 26.2 "Chaos Cubed"**,
Spigot (`spigot-api 26.2-R0.1-SNAPSHOT`), bytecode compilado visando Java 21
(roda normalmente em servidores com Java 26, ja que a JVM e retrocompativel).

> **Importante sobre a "base do mcMMO":** o mcMMO real (mcMMO-Dev/mcMMO no GitHub)
> tem milhares de arquivos acumulados em mais de 10 anos de desenvolvimento, e
> compila contra artefatos que so existem no Nexus/BuildTools da Spigot — coisa
> que este ambiente de chat nao consegue baixar para testar a compilacao de verdade.
> Em vez de copiar o codigo-fonte deles (o que tambem passaria por cima da licenca
> deles), eu escrevi este plugin do zero, usando a mesma ideia central do mcMMO
> (quebrar blocos/lutar/pescar/domesticar da XP em "habilidades", que sobem de
> nivel e desbloqueiam progresso) e as duas funcionalidades customizadas que voce
> pediu: **XP + nivel na action bar** e **`/mcmmo` abrindo uma GUI**.
> Validei os metodos da API (Player#sendActionBar, Player#showTitle,
> Bukkit#createInventory com Component, etc.) contra a documentacao real da
> Spigot API 26.2, mas nao consegui compilar o `.jar` aqui dentro — recomendo
> rodar `mvn clean package` no seu ambiente e me avisar se aparecer algum erro,
> que eu corrijo rapido.

## O que esta implementado

- **12 habilidades**: Mineracao, Escavacao, Lenhador, Ervanismo, Pesca, Acrobacia,
  Espadas, Machados, Desarmado, Arqueria, Domesticacao, Alquimia.
- **XP configuravel por bloco/acao** em `config.yml` (adicione ou remova blocos
  livremente).
- **Nivelamento configuravel** (curva LINEAR ou EXPONENCIAL, nivel maximo).
- **Action bar**: mostra `Habilidade: +XX XP (Nivel: X)` a cada meio segundo
  (agrupando os ganhos, para nao piscar uma mensagem a cada bloco quebrado) —
  igual ao formato do print que voce mandou.
- **Level up**: titulo na tela + som, separado da action bar de XP.
- **`/mcmmo`**: abre uma GUI mostrando nivel, XP e barra de progresso de cada
  habilidade, mais um item de "Poder Total" (soma de todos os niveis).
- **`/mcmmo reload`**: recarrega config.yml e messages.yml sem reiniciar o servidor.
- **Persistencia**: progresso salvo em `plugins/HabilidadesPlus/playerdata/<uuid>.yml`.
- **Protecao antifarm basica**: bloco colocado pelo jogador nao da XP ao quebrar.
- **Roll de Acrobacia**: chance de anular dano de queda, crescendo com o nivel
  (igual ao mcMMO original).

## O que ficou de fora (por escopo) e pode ser adicionado depois

- As demais ~2 habilidades do mcMMO original (Repair/Reparo e Salvage/Salvagem),
  sub-habilidades, "abilities" ativas (ex: Super Breaker, Serrated Strikes,
  Berserk) e o sistema completo de perks/talentos.
- PlaceholderAPI, scoreboard lateral, GUI com paginacao/abas por habilidade.
- Traducao completa para outros idiomas (esta tudo em PT-BR).

Se quiser, posso adicionar qualquer um desses itens numa proxima rodada.

## Estrutura do projeto

```
HabilidadesPlus/
├── pom.xml
└── src/main/
    ├── resources/
    │   ├── plugin.yml
    │   ├── config.yml       <- XP por bloco/acao, curva de nivel, etc.
    │   └── messages.yml     <- todas as mensagens (action bar, GUI, level up)
    └── java/com/rpgcustom/mcmmo/
        ├── HabilidadesPlus.java           <- classe principal (onEnable/onDisable)
        ├── SkillType.java           <- enum das 12 habilidades
        ├── data/                    <- perfil do jogador + salvar/carregar YAML
        ├── leveling/                <- calculo de XP necessario por nivel
        ├── xp/XpManager.java        <- ganho de XP, action bar, level up
        ├── gui/                     <- menu do /mcmmo
        ├── commands/MMOCommand.java <- /mcmmo e /mcmmo reload
        └── listeners/               <- um listener por grupo de habilidades
```

## Como compilar

Pre-requisitos: **Maven 3+** e **JDK 21 ou mais novo** (JDK 26 inclusive) instalado.

```bash
cd HabilidadesPlus
mvn clean package
```

O `.jar` final aparece em `target/HabilidadesPlus-1.0.0.jar`. Copie para a pasta
`plugins/` do seu servidor Spigot 26.2 e reinicie.

Se o Maven reclamar que nao encontra `org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT`,
confirme que seu servidor/maquina tem acesso a
`https://hub.spigotmc.org/nexus/content/groups/public/` (repositorio ja
declarado no `pom.xml`) ou gere o artefato localmente com o `BuildTools.jar`
da Spigot.

## Como customizar

- **Valores de XP**: edite `config.yml` (secao `xp:`), depois `/mcmmo reload`.
- **Textos/cores**: edite `messages.yml` (aceita `&` para cores), depois `/mcmmo reload`.
- **Curva de nivel**: `nivelamento.curva`, `xp-base`, `multiplicador`, `expoente`
  em `config.yml`.
- **Nomes/icones das habilidades**: `SkillType.java`.
- **Layout da GUI**: `gui/MMOMenu.java` (slots, borda, itens).

## Permissoes

- `habilidadesplus.use` (default: true) — necessaria para ganhar XP e usar `/mcmmo`.
- `habilidadesplus.admin` (default: op) — necessaria para `/mcmmo reload`.
