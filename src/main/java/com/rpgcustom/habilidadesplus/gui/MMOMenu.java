package com.rpgcustom.habilidadesplus.gui;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import com.rpgcustom.habilidadesplus.data.PlayerSkillData;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import com.rpgcustom.habilidadesplus.top1.Top1SkillService;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

public final class MMOMenu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31};
    // Mantém vazio o slot onde o antigo Polegar de Cogumelo era exibido.
    private static final int ERVANISMO_EMPTY_POWER_INDEX = 3;

    private MMOMenu() {}

    public static void open(Player player, DataManager data, LevelingManager levels, ConfigManager config) {
        MMOMenuHolder holder = new MMOMenuHolder(null);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                MessageUtil.colorize(config.msg("gui.titulo-menu")));
        holder.setInventory(inventory);
        PlayerProfile profile = data.getProfile(player.getUniqueId());
        boolean profilesReady = data.isAllProfilesLoaded();
        Map<java.util.UUID, PlayerProfile> profiles = data.getAllProfiles();
        SkillType[] skills = SkillType.values();
        for (int index = 0; index < skills.length && index < SLOTS.length; index++) {
            inventory.setItem(SLOTS[index], skillItem(skills[index], profile, levels, config));
        }
        inventory.setItem(51, rankingBook(player, config, profiles, profilesReady));
        inventory.setItem(47, profileItem(player, profile, config));
        player.openInventory(inventory);
    }

    public static void openRanking(Player player, SkillType selected, DataManager data,
                                  ConfigManager config) {
        if (!data.isAllProfilesLoaded()) {
            player.sendMessage(MessageUtil.colorize("&7O ranking ainda está carregando. Aguarde um instante."));
            data.whenAllProfilesLoaded(() -> {
                if (player.isOnline()) {
                    openRanking(player, selected, data, config);
                }
            });
            return;
        }

        Map<java.util.UUID, PlayerProfile> profiles = data.getAllProfiles();
        MMOMenuHolder holder = new MMOMenuHolder(selected, true);
        Inventory inventory = Bukkit.createInventory(holder, 36, MessageUtil.colorize("&8Ranking de Habilidades"));
        holder.setInventory(inventory);

        List<Map.Entry<java.util.UUID, PlayerProfile>> ranking = profiles.entrySet().stream()
                .filter(entry -> entry.getValue().getLevel(selected) > 0)
                .sorted(Comparator
                        .<Map.Entry<java.util.UUID, PlayerProfile>>comparingInt(entry -> entry.getValue().getLevel(selected))
                        .reversed()
                        .thenComparing(entry -> {
                            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                            return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
                        }))
                .limit(7)
                .toList();

        int slot = 10;
        int position = 1;
        for (Map.Entry<java.util.UUID, PlayerProfile> entry : ranking) {
            inventory.setItem(slot++, rankingPlayerItem(entry.getKey(), entry.getValue(), entry.getValue().getLevel(selected), position++, selected, config));
        }

        inventory.setItem(31, item(Material.ARROW, "&cVoltar", List.of("", "&7Voltar ao menu principal.")));
        inventory.setItem(29, rankingFilterItem(selected));
        inventory.setItem(33, rankingBook(player, config, profiles, true));
        player.openInventory(inventory);
    }

    /**
     * Abre o ranking já selecionando a habilidade em que o jogador é Top 1.
     * Caso ele ainda não lidere nenhuma habilidade, mantém Mineração como
     * filtro inicial padrão.
     */
    public static void openPersonalizedRanking(Player player, DataManager data,
                                               ConfigManager config,
                                               Top1SkillService top1Skills) {
        if (!data.isAllProfilesLoaded()) {
            player.sendMessage(MessageUtil.colorize("&7O ranking ainda está carregando. Aguarde um instante."));
            data.whenAllProfilesLoaded(() -> {
                if (player.isOnline()) {
                    openPersonalizedRanking(player, data, config, top1Skills);
                }
            });
            return;
        }

        SkillType selected = top1Skills.getTop1Skill(player.getUniqueId());
        openRanking(player, selected == null ? SkillType.MINERACAO : selected, data, config);
    }

    private static ItemStack rankingBook(Player player, ConfigManager config,
                                         Map<java.util.UUID, PlayerProfile> profiles,
                                         boolean profilesReady) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (!profilesReady) {
            lore.add("&eO ranking está sendo carregado...");
            lore.add("&7Clique novamente em alguns instantes.");
            return item(Material.WRITABLE_BOOK, "&bRanking de Habilidades", lore);
        }
        lore.add("&7Sua posição em cada habilidade:");

        addRankingCategory(lore, "&eColeta e recursos", new SkillType[]{
                SkillType.MINERACAO, SkillType.LENHADOR, SkillType.ESCAVACAO, SkillType.ERVANISMO, SkillType.PESCA
        }, player, profiles);
        addRankingCategory(lore, "&eProdução e utilidade", new SkillType[]{
                SkillType.FUNDICAO, SkillType.ALQUIMIA, SkillType.REPARACAO
        }, player, profiles);
        addRankingCategory(lore, "&eCombate corpo a corpo", new SkillType[]{
                SkillType.ESPADAS, SkillType.MACHADOS, SkillType.CLAVA, SkillType.DESARMADO
        }, player, profiles);
        addRankingCategory(lore, "&eCombate à distância", new SkillType[]{
                SkillType.ARQUERIA, SkillType.BESTAS, SkillType.TRIDENTES, SkillType.LANCAS
        }, player, profiles);
        addRankingCategory(lore, "&eMobilidade e companheiros", new SkillType[]{
                SkillType.ACROBACIA, SkillType.DOMESTICACAO
        }, player, profiles);

        lore.add("");
        lore.add("&7Clique para abrir o ranking.");
        return item(Material.WRITABLE_BOOK, "&bRanking de Habilidades", lore);
    }

    private static void addRankingCategory(List<String> lore, String category, SkillType[] skills,
                                           Player player,
                                           Map<java.util.UUID, PlayerProfile> profiles) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) {
            // O jogador normalmente já está no snapshot, mas manter o fallback
            // evita uma GUI vazia em caso de entrada durante o preload.
            profile = new PlayerProfile(player.getUniqueId());
        }
        lore.add("");
        lore.add(category);
        for (SkillType skill : skills) {
            int level = profile.getLevel(skill);
            int position = rankingPosition(player.getUniqueId(), skill, profiles);
            String positionText = position > 0 ? "&b#" + position : "&8Sem ranking";
            lore.add("&8• &7" + skill.getDisplayName() + ": &a" + level + " &8(" + positionText + "&8)");
        }
    }

    private static int rankingPosition(java.util.UUID target, SkillType skill,
                                       Map<java.util.UUID, PlayerProfile> profiles) {
        List<Map.Entry<java.util.UUID, PlayerProfile>> ranking = profiles.entrySet().stream()
                .filter(entry -> entry.getValue().getLevel(skill) > 0)
                .sorted(Comparator
                        .<Map.Entry<java.util.UUID, PlayerProfile>>comparingInt(entry -> entry.getValue().getLevel(skill))
                        .reversed()
                        .thenComparing(entry -> {
                            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                            return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
                        }))
                .toList();

        for (int index = 0; index < ranking.size(); index++) {
            if (ranking.get(index).getKey().equals(target)) return index + 1;
        }
        return 0;
    }

    private static ItemStack rankingFilterItem(SkillType selected) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Clique no funil para alternar a habilidade.");
        lore.add("");
        for (SkillType skill : SkillType.values()) {
            String color = skill == selected ? "&a✔ " : "&7• ";
            lore.add(color + skill.getDisplayName());
        }
        return item(Material.HOPPER, "&bFiltro de Habilidade", lore);
    }

    private static ItemStack rankingPlayerItem(java.util.UUID uuid, PlayerProfile profile, int level, int position,
                                               SkillType selected, ConfigManager config) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
            meta.setOwningPlayer(offline);
            meta.setDisplayName(MessageUtil.colorize("&b#" + position + " &8• &a" + name));
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (selected == SkillType.LENHADOR) {
                lore.add(MessageUtil.colorize("&fÁrvores replantadas: &a" + profile.getLenhadorArvoresReplantadas()));
            } else if (selected == SkillType.MINERACAO) {
                lore.add(MessageUtil.colorize("&fBlocos minerados: &a" + profile.getMineracaoBlocosMinerados()));
            }
            lore.add(MessageUtil.colorize("&fNível: &a" + level));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    public static void openSkill(Player player, SkillType skill, DataManager data,
                                 LevelingManager levels, ConfigManager config) {
        MMOMenuHolder holder = new MMOMenuHolder(skill);
        Inventory inventory = Bukkit.createInventory(holder, 36,
                message(config, "gui.titulo-habilidade", Map.of("habilidade", skill.getDisplayName())));
        holder.setInventory(inventory);
        PlayerProfile profile = data.getProfile(player.getUniqueId());
        PlayerSkillData current = profile.getData(skill);
        List<SkillCatalog.Power> powers = SkillCatalog.definition(skill).powers();
        for (int index = 0; index < powers.size() && index < SLOTS.length; index++) {
            if (skill == SkillType.ERVANISMO && index == ERVANISMO_EMPTY_POWER_INDEX) {
                continue;
            }
            inventory.setItem(SLOTS[index], powerItem(powers.get(index), skill, current.getLevel(), config));
        }
        inventory.setItem(29, profileItem(player, profile, config));
        inventory.setItem(31, item(Material.ARROW, config.msg("gui.voltar-nome"),
                List.of("", config.msg("gui.voltar-lore"))));
        inventory.setItem(33, skillItem(skill, profile, levels, config));
        player.openInventory(inventory);
    }

    public static SkillType skillAtSlot(int slot) {
        for (int index = 0; index < SLOTS.length; index++) {
            if (SLOTS[index] == slot) return SkillType.values()[index];
        }
        return null;
    }

    private static ItemStack skillItem(SkillType skill, PlayerProfile profile,
                                       LevelingManager levels, ConfigManager config) {
        PlayerSkillData data = profile.getData(skill);
        boolean maximum = data.getLevel() >= levels.getNivelMaximo();
        double needed = maximum ? 0 : levels.xpParaProximoNivel(skill, data.getLevel());
        SkillCatalog.Definition definition = SkillCatalog.definition(skill);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(wrap("&7" + definition.description()));
        lore.add("");
        if (maximum) {
            lore.add(config.msg("gui.item-nivel-maximo"));
            lore.add("&a" + bar(1, 1));
        } else {
            lore.add(message(config, "gui.item-xp", Map.of(
                    "xpAtual", number(data.getCurrentXp()),
                    "xpNecessario", number(needed))));
            lore.add("&a" + bar(data.getCurrentXp(), needed));
        }
        lore.add("");
        lore.add(config.msg("gui.lista-poderes"));
        for (SkillCatalog.Power power : definition.powers()) {
            String state;
            if (!power.implemented()) state = config.msg("gui.estado-planejado");
            else if (data.getLevel() >= power.level()) state = config.msg("gui.estado-desbloqueado");
            else state = config.msg("gui.estado-bloqueado");
            lore.add(message(config, "gui.linha-poder", Map.of(
                    "nivel", String.valueOf(power.level()), "poder", power.name(), "estado", state)));
        }
        lore.add("");
        lore.add(config.msg("gui.clique-poderes"));
        return item(skill.getIcon(), message(config, "gui.item-habilidade-nome", Map.of(
                "habilidade", skill.getDisplayName(), "nivel", String.valueOf(data.getLevel()))), lore);
    }

    private static ItemStack powerItem(SkillCatalog.Power power, SkillType skill, int level, ConfigManager config) {
        boolean unlocked = level >= power.level();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(wrap("&7" + power.description()));
        lore.add("");
        if (skill == SkillType.MINERACAO && power.name().equals("Veio Farto")) {
            double chance = Math.min(100.0, level * config.config().getDouble("mineracao.veio-farto.chance-drop-duplo-por-nivel", 0.1));
            lore.add(message(config, "gui.veio-farto-chance", Map.of("chance", formatPercent(chance))));
        }
        if (skill == SkillType.MINERACAO && power.name().equals("Super Quebrador")) {
            double chance = level < 100 ? 0.0 : Math.min(100.0, Math.floor(level / 10.0) * config.config().getDouble("mineracao.superbreaker.chance-drop-triplo-por-nivel", 0.5));
            double duration = superBreakerDuration(level, config);
            lore.add("&d⌛ &fDuração: &e" + formatSeconds(duration) + " segundo(s)");
            lore.addAll(wrap("&7Durante o Super Quebrador, o drop pode ser triplicado."));
            lore.add(message(config, "gui.superbreaker-chance", Map.of("chance", formatPercent(chance))));
        }
        if (skill == SkillType.MINERACAO && power.name().equals("Mineração Precisa")) {
            double chance = level < 75 ? 0.0 : Math.min(100.0, level * config.config().getDouble(
                    "mineracao.mineracao-precisa.chance-preservar-por-nivel", 0.1));
            lore.add(message(config, "gui.mineracao-precisa-chance",
                    Map.of("chance", formatPercent(chance))));
        }

        if (skill == SkillType.ESCAVACAO) {
            if (power.name().equals("Duplo Drop")) {
                double chance = Math.min(
                        config.config().getDouble("escavacao.duplo-drop.chance-maxima", 50.0),
                        level * config.config().getDouble("escavacao.duplo-drop.chance-por-nivel", 0.05));
                lore.add("&b• &fChance atual: &e" + formatPercent(chance) + "%");
            }
            if (power.name().equals("Giga Broca")) {
                double duration = Math.min(
                        config.config().getDouble("escavacao.giga-broca.duracao-maxima", 20.0),
                        config.config().getDouble("escavacao.giga-broca.duracao-nivel-25", 5.0)
                                + Math.max(0, level - 25) * config.config().getDouble(
                                "escavacao.giga-broca.duracao-por-nivel", 0.01));
                lore.add("&d⌛ &fDuração: &e" + formatPercent(duration) + "s");
                lore.add("&b• &fRecarga: &e" + formatPercent(config.config().getDouble(
                        "escavacao.giga-broca.recarga-segundos", 120.0)) + "s");
            }
            if (power.name().equals("Arqueologia")) {
                int unlock = config.config().getInt("escavacao.arqueologia.nivel-desbloqueio", 10);
                double chance = level >= unlock
                        ? Math.min(config.config().getDouble("escavacao.arqueologia.chance-maxima", 12.5),
                        config.config().getDouble("escavacao.arqueologia.chance-base", 0.25)
                                + level * config.config().getDouble("escavacao.arqueologia.chance-por-nivel", 0.015))
                        : 0.0;
                lore.add("&b• &fChance de tesouro: &e" + formatPercent(chance) + "%");
            }
            if (power.name().equals("Escavador Experiente")) {
                int unlock = config.config().getInt("escavacao.escavador-experiente.nivel-desbloqueio", 100);
                double bonus = config.config().getDouble("escavacao.escavador-experiente.bonus-xp", 0.10) * 100.0;
                lore.add("&b• &fBônus de XP em tesouros: &e+" + formatPercent(level >= unlock ? bonus : 0.0) + "%");
            }
            if (power.name().equals("Mestre da Escavação")) {
                int unlock = config.config().getInt("escavacao.mestre-da-escavacao.nivel-desbloqueio", 750);
                double bonus = config.config().getDouble("escavacao.mestre-da-escavacao.bonus-chance", 3.0);
                lore.add("&b• &fBônus de chance: &e+" + formatPercent(level >= unlock ? bonus : 0.0) + "%");
            }
        }


        if (skill == SkillType.ERVANISMO) {
            if (power.name().equals("Duplo Drop")) {
                lore.add("&b• &fChance atual: &e" + formatPercent(herbalismDoubleDropChance(level, config)) + "%");
            }
            if (power.name().equals("Terra Verde")) {
                double duration = config.config().getDouble("ervanismo.terra-verde.duracao-segundos", 20.0);
                double cooldown = config.config().getDouble("ervanismo.terra-verde.recarga-segundos", 120.0);
                lore.add("&d⌛ &fDuração: &e" + formatSeconds(duration) + " segundo(s)");
                lore.add("&b• &fRecarga: &e" + formatSeconds(cooldown) + " segundo(s)");
                lore.add("&7Durante o efeito: &e3x drops &7e Polegar Verde garantido.");
            }
            if (power.name().equals("Dieta de Fazendeiro")) {
                int rank = farmerDietRank(level, config);
                lore.add("&b• &fRank atual: &e" + rank + "&7/5");
                lore.add("&b• &fBônus: &e+" + rank + " &7fome restaurada");
            }
            if (power.name().equals("Polegar Verde")) {
                double chance = level >= config.config().getInt("ervanismo.polegar-verde.nivel-desbloqueio", 250)
                        ? Math.min(100.0, level * 100.0 / Math.max(1, config.config().getInt(
                        "ervanismo.polegar-verde.nivel-maximo-chance", 1000))) : 0.0;
                lore.add("&b• &fChance de Replantio: &e" + formatPercent(chance) + "%");
            }
            if (power.name().equals("Sorte de Hylian")) {
                double chance = Math.min(
                        config.config().getDouble("ervanismo.sorte-hylian.chance-maxima", 10.0),
                        level * config.config().getDouble("ervanismo.sorte-hylian.chance-maxima", 10.0)
                                / Math.max(1, config.config().getInt("ervanismo.sorte-hylian.nivel-maximo-chance", 1000)));
                lore.add("&b• &fChance de tesouro: &e" + formatPercent(chance) + "%");
            }
            if (power.name().equals("Polegar de Cogumelo")) {
                double chance = Math.min(
                        config.config().getDouble("ervanismo.polegar-cogumelo.chance-maxima", 50.0),
                        level * config.config().getDouble("ervanismo.polegar-cogumelo.chance-maxima", 50.0)
                                / Math.max(1, config.config().getInt("ervanismo.polegar-cogumelo.nivel-maximo-chance", 1000)));
                lore.add("&b• &fChance de conversão: &e" + formatPercent(chance) + "%");
            }
            if (power.name().equals("Colheita Verdejante")) {
                double chance = level >= config.config().getInt("ervanismo.colheita-verdejante.nivel-desbloqueio", 1000)
                        ? Math.min(
                        config.config().getDouble("ervanismo.colheita-verdejante.chance-maxima", 50.0),
                        level * config.config().getDouble("ervanismo.colheita-verdejante.chance-maxima", 50.0)
                                / Math.max(1, config.config().getInt("ervanismo.colheita-verdejante.nivel-maximo-chance", 1000)))
                        : 0.0;
                lore.add("&b• &fChance de Drop Triplo: &e" + formatPercent(chance) + "%");
            }
        }

        if (skill == SkillType.LENHADOR) {
            if (power.name().equals("Derrubada de Árvores")) {
                int leafLevel = Math.max(1, level);
                double tempoNivel1 = config.config().getDouble(
                        "lenhador.leaf-cutter.tempo-nivel-1-segundos", 120.0);
                double tempoNivel500 = config.config().getDouble(
                        "lenhador.leaf-cutter.tempo-nivel-500-segundos", 30.0);
                double tempoNivel1000 = config.config().getDouble(
                        "lenhador.leaf-cutter.tempo-nivel-1000-segundos", 12.0);
                double tempo;
                if (leafLevel <= 500) {
                    double t = (leafLevel - 1) / 499.0;
                    tempo = tempoNivel1 + (tempoNivel500 - tempoNivel1) * t;
                } else {
                    double t = Math.min(1.0, (leafLevel - 500) / 500.0);
                    tempo = tempoNivel500 + (tempoNivel1000 - tempoNivel500) * t;
                }
                lore.add("&b• &fCorte de Folhas: &e" + formatPercent(tempo) + "s para folhas");
            }

            if (power.name().equals("Duplo Drop")) {
                double chance = Math.min(50.0, level * config.config().getDouble(
                        "lenhador.double-drop.chance-por-nivel", 0.05));
                lore.add("&b• &fChance atual: &e" + formatPercent(chance) + "%");
            }

            if (power.name().equals("Machado Reforçado")) {
                double baseChance = level * config.config().getDouble(
                        "lenhador.machado-reforcado.chance-preservar-por-nivel", 0.05);
                int efficientUnlock = config.config().getInt(
                        "lenhador.colheita-eficiente.nivel-desbloqueio", 100);
                double efficientBonus = level >= efficientUnlock
                        ? (level - efficientUnlock + 1) * config.config().getDouble(
                        "lenhador.colheita-eficiente.bonus-por-nivel", 0.025)
                        : 0.0;
                double chance = Math.min(75.0, baseChance + efficientBonus);
                lore.add("&b• &fChance atual: &e" + formatPercent(chance) + "%");
            }

            if (power.name().equals("Colheita Eficiente")) {
                int unlock = config.config().getInt(
                        "lenhador.colheita-eficiente.nivel-desbloqueio", 100);
                double bonus = level >= unlock
                        ? (level - unlock + 1) * config.config().getDouble(
                        "lenhador.colheita-eficiente.bonus-por-nivel", 0.025)
                        : 0.0;
                lore.add("&b• &fBônus atual: &e+" + formatPercent(bonus) + "%");
                double baseChance = level * config.config().getDouble(
                        "lenhador.machado-reforcado.chance-preservar-por-nivel", 0.05);
                double totalChance = Math.min(75.0, baseChance + bonus);
                lore.add("&b• &fChance total do machado: &e" + formatPercent(totalChance) + "%");
            }

            if (power.name().equals("Crítico do Lenhador")) {
                int unlock = config.config().getInt(
                        "lenhador.critico-lenhador.nivel-desbloqueio", 500);
                double chance = 0.0;
                if (level >= unlock) {
                    chance = getLenhadorCriticoChance(level, unlock);
                }
                lore.add("&b• &fChance atual: &e" + formatPercent(chance) + "%");
            }

            if (power.name().equals("Replantio Automático")) {
                int unlock = config.config().getInt(
                        "lenhador.replantio-automatico.nivel-desbloqueio", 10);
                double chance = level >= unlock
                        ? Math.min(100.0, level * config.config().getDouble(
                        "lenhador.replantio-automatico.chance-por-nivel", 0.10))
                        : 0.0;
                lore.add("&b• &fChance atual: &e" + formatPercent(chance) + "%");
            }

            if (power.name().equals("Combo de Corte")) {
                int unlock = config.config().getInt(
                        "lenhador.combo-de-corte.nivel-desbloqueio", 150);
                int maxCombo = Math.max(1, config.config().getInt(
                        "lenhador.combo-de-corte.combo-maximo", 5));
                double bonusPorCombo = Math.max(0.0, config.config().getDouble(
                        "lenhador.combo-de-corte.bonus-xp-por-combo", 5.0));
                double maxBonus = level >= unlock ? (maxCombo - 1) * bonusPorCombo : 0.0;
                lore.add("&b• &fBônus máximo atual: &e+" + formatPercent(maxBonus) + "% XP");
            }
        }

        if (power.implemented() && !unlocked) {
            lore.add("");
            lore.add("&cEste poder será desbloqueado no nível " + power.level() + ".");
        }

        String color = power.implemented() ? "&a" : "&8";
        return item(power.icon(), color + power.name() + " &8(" + skill.getDisplayName() + ")", lore);
    }

    private static ItemStack profileItem(Player player, PlayerProfile profile, ConfigManager config) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(message(config, "gui.item-perfil-nome", Map.of(
                    "jogador", player.getName(),
                    "poder", String.valueOf(profile.getPowerLevel()))));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(config.msg("gui.item-perfil-habilidades"));
            addProfileCategory(lore, config.msg("gui.perfil-categoria-combate"), new SkillType[]{
                    SkillType.ESPADAS, SkillType.MACHADOS, SkillType.ARQUERIA, SkillType.DESARMADO,
                    SkillType.CLAVA, SkillType.TRIDENTES, SkillType.BESTAS, SkillType.LANCAS
            }, profile);
            addProfileCategory(lore, config.msg("gui.perfil-categoria-coleta"), new SkillType[]{
                    SkillType.MINERACAO, SkillType.LENHADOR, SkillType.ESCAVACAO, SkillType.ERVANISMO,
                    SkillType.PESCA, SkillType.FUNDICAO
            }, profile);
            addProfileCategory(lore, config.msg("gui.perfil-categoria-utilidade"), new SkillType[]{
                    SkillType.ALQUIMIA, SkillType.ACROBACIA, SkillType.DOMESTICACAO, SkillType.REPARACAO
            }, profile);
            meta.setLore(color(lore));
            head.setItemMeta(meta);
        }
        return head;
    }

    private static void addProfileCategory(List<String> lore, String category, SkillType[] skills,
                                            PlayerProfile profile) {
        lore.add("");
        lore.add(category);
        for (SkillType skill : skills) {
            lore.add("&8• &7" + skill.getDisplayName() + ": &a" + profile.getData(skill).getLevel());
        }
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            meta.setLore(color(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String message(ConfigManager config, String path, Map<String, String> values) {
        return MessageUtil.colorize(MessageUtil.placeholders(config.msg(path), new HashMap<>(values)));
    }

    private static List<String> color(List<String> lines) {
        return lines.stream().map(MessageUtil::colorize).toList();
    }

    private static List<String> wrap(String text) {
        String prefix = text.startsWith("&") && text.length() >= 2 ? text.substring(0, 2) : "";
        String body = prefix.isEmpty() ? text : text.substring(2);
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : body.split(" ")) {
            if (!line.isEmpty() && line.length() + word.length() > 32) {
                lines.add(prefix + line);
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(prefix + line);
        return lines;
    }

    private static double getLenhadorCriticoChance(int level, int unlock) {
        if (level < unlock) return 0.0;

        int[] levels = {100, 200, 300, 500, 700, 900, 999, 1000};
        double[] chances = {5.0, 10.0, 15.0, 20.0, 30.0, 40.0, 45.0, 50.0};

        if (level <= levels[0]) return chances[0];
        for (int i = 1; i < levels.length; i++) {
            if (level <= levels[i]) {
                double levelSpan = levels[i] - levels[i - 1];
                double chanceSpan = chances[i] - chances[i - 1];
                double progress = (level - levels[i - 1]) / levelSpan;
                return chances[i - 1] + progress * chanceSpan;
            }
        }
        return chances[chances.length - 1];
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.US, "%.2f", value).replace(".", ",");
    }

    private static double herbalismDoubleDropChance(int level, ConfigManager config) {
        double max = config.config().getDouble("ervanismo.duplo-drop.chance-maxima", 100.0);
        int maxLevel = Math.max(1, config.config().getInt("ervanismo.duplo-drop.nivel-maximo-chance", 1000));
        return Math.min(max, level * max / maxLevel);
    }

    private static int farmerDietRank(int level, ConfigManager config) {
        if (level >= config.config().getInt("ervanismo.dieta-fazendeiro.nivel-5", 1000)) return 5;
        if (level >= config.config().getInt("ervanismo.dieta-fazendeiro.nivel-4", 800)) return 4;
        if (level >= config.config().getInt("ervanismo.dieta-fazendeiro.nivel-3", 600)) return 3;
        if (level >= config.config().getInt("ervanismo.dieta-fazendeiro.nivel-2", 400)) return 2;
        if (level >= config.config().getInt("ervanismo.dieta-fazendeiro.nivel-1", 200)) return 1;
        return 0;
    }

    private static String formatSeconds(double value) {
        if (value == Math.rint(value)) return String.valueOf((int) value);
        return formatPercent(value);
    }

    private static double superBreakerDuration(int level, ConfigManager config) {
        return interpolate(level,
                new int[]{10, 50, 100, 150, 200, 250, 500, 1000},
                new double[]{
                        config.config().getDouble("mineracao.superbreaker.duracao-nivel-10", 5.0),
                        config.config().getDouble("mineracao.superbreaker.duracao-nivel-50", 6.0),
                        config.config().getDouble("mineracao.superbreaker.duracao-nivel-100", 7.0),
                        config.config().getDouble("mineracao.superbreaker.duracao-nivel-150", 8.0),
                        config.config().getDouble("mineracao.superbreaker.duracao-nivel-200", 9.0),
                        config.config().getDouble("mineracao.superbreaker.duracao-nivel-250", 10.0),
                        config.config().getDouble("mineracao.superbreaker.duracao-nivel-500", 15.0),
                        config.config().getDouble("mineracao.superbreaker.duracao-nivel-1000", 20.0)
                });
    }

    private static double interpolate(int level, int[] levels, double[] values) {
        if (level <= levels[0]) return values[0];
        for (int i = 1; i < levels.length; i++) {
            if (level <= levels[i]) {
                double ratio = (level - levels[i - 1]) / (double) (levels[i] - levels[i - 1]);
                return values[i - 1] + (values[i] - values[i - 1]) * ratio;
            }
        }
        return values[values.length - 1];
    }

    private static String number(double number) {
        return number >= 1000
                ? String.format(java.util.Locale.US, "%.1fk", number / 1000).replace(".0k", "k")
                : String.valueOf((int) number);
    }

    private static String bar(double current, double needed) {
        int filled = needed <= 0 ? 20 : (int) Math.min(20, Math.round(current / needed * 20));
        return "■".repeat(filled) + "&8" + "■".repeat(20 - filled);
    }
}
