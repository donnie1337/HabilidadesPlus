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
        if (skill == SkillType.MINERACAO && power.name().equals("Veio Farto")) {
            double chance = Math.min(100.0, level * config.config().getDouble("mineracao.veio-farto.chance-drop-duplo-por-nivel", 0.1));
            lore.add(message(config, "gui.veio-farto-chance", Map.of("chance", formatPercent(chance))));
        }
        if (skill == SkillType.MINERACAO && power.name().equals("Super Quebrador")) {
            double chance = level < 100 ? 0.0 : Math.min(100.0, Math.floor(level / 10.0) * config.config().getDouble("mineracao.superbreaker.chance-drop-triplo-por-nivel", 0.5));
            lore.addAll(wrap("&7Durante o Super Quebrador, o drop pode ser triplicado."));
            lore.add(message(config, "gui.superbreaker-chance", Map.of("chance", formatPercent(chance))));
        }
        if (skill == SkillType.MINERACAO && power.name().equals("Mineração Precisa")) {
            double chance = level < 75 ? 0.0 : Math.min(100.0, level * config.config().getDouble(
                    "mineracao.mineracao-precisa.chance-preservar-por-nivel", 0.1));
            lore.add(message(config, "gui.mineracao-precisa-chance",
                    Map.of("chance", formatPercent(chance))));
        }
        lore.add("");
        lore.add(message(config, "gui.item-poder-nivel", Map.of("nivel", String.valueOf(power.level()))));
        String color = !power.implemented() ? "&8" : unlocked ? "&a" : "&8";
        return item(power.icon(), color + power.name(), lore);
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
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 32) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.US, "%.2f", value).replace(".", ",");
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
