package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import com.rpgcustom.habilidadesplus.abilities.SuperEscavadorManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import com.rpgcustom.habilidadesplus.util.PlacedBlockTracker;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Coleta de Mineracao, Escavacao, Lenhador e Ervanismo.
 * As mecanicas de Lenhador sao voltadas a madeira e manejo de arvores,
 * sem misturar com o dano de combate da habilidade Machados.
 */
public class GatheringListener implements Listener {

    private static final SkillType[] HABILIDADES = {
            SkillType.MINERACAO,
            SkillType.ESCAVACAO,
            SkillType.LENHADOR,
            SkillType.ERVANISMO
    };
    private static final String[] SECOES = {
            "mineracao",
            "escavacao",
            "lenhador",
            "ervanismo"
    };
    private static final int MAX_TREE_BLOCKS_HARD_LIMIT = 512;
    private static final BlockFace[] TREE_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final PlacedBlockTracker placedBlockTracker;
    private final SuperEscavadorManager superEscavadorManager;
    private final Random random = new Random();
    private final Map<UUID, Long> lastTreeFellAt = new HashMap<>();
    private final Map<UUID, Integer> cuttingCombos = new HashMap<>();
    private final Map<UUID, Integer> comboTreeCounts = new HashMap<>();
    private final Map<UUID, Long> comboStageStartedAt = new HashMap<>();
    private final Map<UUID, Integer> lastShownCuttingCombo = new HashMap<>();
    private final Set<UUID> treeFellerInProgress = new HashSet<>();

    public GatheringListener(JavaPlugin plugin, ConfigManager configManager, XpManager xpManager,
                             PlacedBlockTracker placedBlockTracker, SuperEscavadorManager superEscavadorManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.xpManager = xpManager;
        this.placedBlockTracker = placedBlockTracker;
        this.superEscavadorManager = superEscavadorManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!configManager.mundoDesabilitado(event.getBlock().getWorld().getName())
                && isTreeProtectionBlock(event.getBlock().getType())
                && !placedBlockTracker.add(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (placedBlockTracker.removeIfPlaced(block)) return;
        if (treeFellerInProgress.contains(player.getUniqueId())) return;
        if (!configManager.habilidadeAtiva(player)) return;

        Material material = block.getType();

        if (isWood(material)) {
            // Lenhador só concede XP quando a madeira é quebrada com um machado.
            // Espadas, mãos e outras ferramentas deixam o bloco seguir o comportamento vanilla.
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!isAxe(tool.getType())) {
                return;
            }

            handleWoodBreak(event, player, block);
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        for (int i = 0; i < HABILIDADES.length; i++) {
            Double xp = configManager.xpDeSeConfigurado(SECOES[i], material.name());
            if (xp == null) continue;

            if (HABILIDADES[i] == SkillType.MINERACAO && !isValidMiningTool(block, tool)) {
                return;
            }
            if (HABILIDADES[i] == SkillType.ESCAVACAO && !isValidExcavationTool(tool)) {
                return;
            }

            xpManager.addXp(player, HABILIDADES[i], xp);
            if (HABILIDADES[i] == SkillType.ESCAVACAO && superEscavadorManager.isActive(player.getUniqueId())) {
                // Giga Broca segue a referência do mcMMO: 3x EXP durante a habilidade.
                xpManager.addXp(player, SkillType.ESCAVACAO, xp * 2.0);
            }
            if (HABILIDADES[i] == SkillType.ESCAVACAO) {
                tryExcavationTreasure(player, block, tool);
            }
            if (HABILIDADES[i] == SkillType.MINERACAO) {
                xpManager.getDataManager().getProfile(player.getUniqueId()).incrementMineracaoBlocosMinerados();
                xpManager.getDataManager().markDirty(player.getUniqueId());
            }
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExcavationDrop(BlockDropItemEvent event) {
        Block block = event.getBlock();
        if (!isExcavationBlock(block.getType())) return;
        if (placedBlockTracker.consumeProtectedDrop(block)) return;

        Player player = event.getPlayer();
        if (!configManager.habilidadeAtiva(player) || !isValidExcavationTool(player.getInventory().getItemInMainHand())) {
            return;
        }

        int level = getExcavationLevel(player);
        if (superEscavadorManager.isActive(player.getUniqueId())) {
            if (level < 100) return;

            double chance = Math.min(100.0, Math.floor(level / 10.0)
                    * configManager.config().getDouble(
                            "escavacao.giga-broca.chance-drop-triplo-por-10-niveis", 0.5));
            if (random.nextDouble() * 100.0 >= chance) return;

            int multiplicador = Math.max(1, (int) Math.round(configManager.config().getDouble(
                    "escavacao.giga-broca.multiplicador-drop", 3.0)));
            for (Item item : event.getItems()) {
                ItemStack stack = item.getItemStack();
                stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() * multiplicador));
                item.setItemStack(stack);
            }
            return;
        }

        if (!shouldExcavationDoubleDrop(level)) return;

        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() * 2));
            item.setItemStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWoodDrop(BlockDropItemEvent event) {
        if (placedBlockTracker.consumeProtectedDrop(event.getBlock())) return;
        if (!isWood(event.getBlockState().getType())) return;

        Player player = event.getPlayer();
        if (!configManager.habilidadeAtiva(player)) return;
        int level = getLenhadorLevel(player);
        if (level <= 0) return;

        if (shouldDoubleDrop(level)) {
            for (Item item : event.getItems()) {
                ItemStack stack = item.getItemStack();
                stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() * 2));
                item.setItemStack(stack);
            }
        }

        tryRareWoodDrop(player, level);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            placedBlockTracker.discard(block);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            placedBlockTracker.discard(block);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearCuttingCombo(event.getPlayer().getUniqueId());
        treeFellerInProgress.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAxeDamage(EntityDamageItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isAxe(event.getItem().getType())) return;
        if (!configManager.habilidadeAtiva(player)) return;

        int level = getLenhadorLevel(player);
        if (level <= 0) return;

        if (shouldPreserveAxe(level)) event.setCancelled(true);
    }

    private void tryExcavationTreasure(Player player, Block block, ItemStack tool) {
        int level = getExcavationLevel(player);
        int archaeologyUnlock = configManager.config().getInt(
                "escavacao.arqueologia.nivel-desbloqueio", 10);
        if (level < archaeologyUnlock || !isExcavationBlock(block.getType())) return;

        double chance = configManager.config().getDouble("escavacao.arqueologia.chance-base", 0.25)
                + level * configManager.config().getDouble("escavacao.arqueologia.chance-por-nivel", 0.015);

        int masterUnlock = configManager.config().getInt(
                "escavacao.mestre-da-escavacao.nivel-desbloqueio", 750);
        if (level >= masterUnlock) {
            chance += configManager.config().getDouble(
                    "escavacao.mestre-da-escavacao.bonus-chance", 3.0);
        }
        chance = Math.min(configManager.config().getDouble(
                "escavacao.arqueologia.chance-maxima", 12.5), chance);

        if (superEscavadorManager.isActive(player.getUniqueId())) {
            chance *= configManager.config().getDouble(
                    "escavacao.giga-broca.multiplicador-tesouro", 3.0);
        }
        if (random.nextDouble() * 100.0 >= chance) return;

        ConfigurationSection section = configManager.config().getConfigurationSection(
                "escavacao.tesouros." + block.getType().name());
        if (section == null) return;

        List<String> keys = new ArrayList<>(section.getKeys(false));
        List<String> eligible = new ArrayList<>();
        for (String key : keys) {
            String path = "escavacao.tesouros." + block.getType().name() + "." + key;
            int minLevel = section.getInt(key + ".nivel", 1);
            String rarity = section.getString(key + ".raridade", "comum");
            int rareUnlock = configManager.config().getInt(
                    "escavacao.tesouro-raro.nivel-desbloqueio", 250);
            if (level < minLevel) continue;
            if ("raro".equalsIgnoreCase(rarity) && level < rareUnlock) continue;
            if (configManager.config().getBoolean(path + ".habilitado", true)) {
                eligible.add(key);
            }
        }
        if (eligible.isEmpty()) return;

        String selected = selectExcavationTreasure(section, eligible);
        if (selected == null) return;
        String path = "escavacao.tesouros." + block.getType().name() + "." + selected;
        Material material = Material.matchMaterial(section.getString(selected + ".item", "COAL"));
        if (material == null) return;

        int min = Math.max(1, section.getInt(selected + ".quantidade-min", 1));
        int max = Math.max(min, section.getInt(selected + ".quantidade-max", min));
        int amount = min + random.nextInt(max - min + 1);
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(material, amount));

        int experiencedUnlock = configManager.config().getInt(
                "escavacao.escavador-experiente.nivel-desbloqueio", 100);
        if (level >= experiencedUnlock) {
            double xpBonus = Math.max(0.0, configManager.config().getDouble(
                    "escavacao.escavador-experiente.bonus-xp", 0.10));
            Double baseXp = configManager.xpDeSeConfigurado("escavacao", block.getType().name());
            if (baseXp != null) {
                xpManager.addXp(player, SkillType.ESCAVACAO, baseXp * xpBonus);
            }
        }

        player.sendTitle("", MessageUtil.colorize("&e&lARQUEOLOGIA! &fVocê encontrou &6" +
                amount + "x " + material.name()), 5, 60, 10);
    }

    private String selectExcavationTreasure(ConfigurationSection section, List<String> eligible) {
        double totalWeight = 0.0;
        for (String key : eligible) {
            totalWeight += Math.max(0.0, section.getDouble(key + ".peso", 1.0));
        }
        if (totalWeight <= 0.0) return null;

        double roll = random.nextDouble() * totalWeight;
        for (String key : eligible) {
            roll -= Math.max(0.0, section.getDouble(key + ".peso", 1.0));
            if (roll < 0.0) return key;
        }
        return eligible.get(eligible.size() - 1);
    }

    private boolean shouldExcavationDoubleDrop(int level) {
        int unlock = configManager.config().getInt(
                "escavacao.duplo-drop.nivel-desbloqueio", 1);
        if (level < unlock) return false;

        double chance = Math.min(
                configManager.config().getDouble("escavacao.duplo-drop.chance-maxima", 50.0),
                level * configManager.config().getDouble("escavacao.duplo-drop.chance-por-nivel", 0.05));
        // O multiplicador da Giga Broca é aplicado diretamente na chance do drop raro
        // pelo sistema de tesouros; aqui mantemos o Double Drop independente.
        return random.nextDouble() * 100.0 < Math.min(100.0, chance);
    }

    private boolean isExcavationBlock(Material material) {
        return configManager.xpDeSeConfigurado("escavacao", material.name()) != null;
    }

    private boolean isValidExcavationTool(ItemStack tool) {
        return isShovel(tool.getType());
    }

    private int getExcavationLevel(Player player) {
        return xpManager.getDataManager().getProfile(player.getUniqueId()).getLevel(SkillType.ESCAVACAO);
    }

    private double getExcavationGigaDuration(int level) {
        double base = configManager.config().getDouble("escavacao.giga-broca.duracao-nivel-25", 5.0);
        double perLevel = configManager.config().getDouble("escavacao.giga-broca.duracao-por-nivel", 0.01);
        double max = configManager.config().getDouble("escavacao.giga-broca.duracao-maxima", 20.0);
        return Math.min(max, base + Math.max(0, level - 25) * perLevel);
    }

    private boolean isShovel(Material material) {
        return material == Material.WOODEN_SHOVEL
                || material == Material.STONE_SHOVEL
                || material == Material.IRON_SHOVEL
                || material == Material.GOLDEN_SHOVEL
                || material == Material.DIAMOND_SHOVEL
                || material == Material.NETHERITE_SHOVEL;
    }

    private void handleWoodBreak(BlockBreakEvent event, Player player, Block root) {
        int level = getLenhadorLevel(player);
        ItemStack tool = player.getInventory().getItemInMainHand();

        int unlock = configManager.config().getInt("lenhador.tree-feller.nivel-desbloqueio", 25);
        if (level >= unlock && isAxe(tool.getType()) && !player.isSneaking()) {
            event.setCancelled(true);
            breakTree(player, root, tool, level);
            return;
        }

        Double xp = configManager.xpDeSeConfigurado("lenhador", root.getType().name());
        if (xp != null) {
            xpManager.addXp(player, SkillType.LENHADOR, xp);
        }
    }

    private void breakTree(Player player, Block root, ItemStack tool, int level) {
        int maxBlocks = Math.min(
                MAX_TREE_BLOCKS_HARD_LIMIT,
                Math.max(1, configManager.config().getInt("lenhador.tree-feller.max-troncos", 64))
        );
        List<Block> logs = collectConnectedLogs(root, maxBlocks);

        Block replantTarget = findTreeBase(logs);
        Material rootMaterial = root.getType();
        if (replantTarget != null) {
            rootMaterial = replantTarget.getType();
        }
        Double rootXp = configManager.xpDeSeConfigurado("lenhador", root.getType().name());
        double treeBaseXp = 0.0;
        int brokenLogs = 0;
        boolean rootBroken = false;
        UUID uuid = player.getUniqueId();
        treeFellerInProgress.add(uuid);
        try {
            for (Block log : logs) {
                if (placedBlockTracker.isPlaced(log) || !isWood(log.getType())) continue;
                if (!isAxe(tool.getType()) || tool.getAmount() <= 0) break;

                org.bukkit.event.block.BlockBreakEvent blockBreakEvent =
                        new org.bukkit.event.block.BlockBreakEvent(log, player);
                Bukkit.getPluginManager().callEvent(blockBreakEvent);
                if (blockBreakEvent.isCancelled()) continue;
                blockBreakEvent.setDropItems(false);

                Collection<ItemStack> drops = log.getDrops(tool, player);
                for (ItemStack drop : drops) {
                    ItemStack copy = drop.clone();
                    if (shouldDoubleDrop(level)) {
                        copy.setAmount(Math.min(copy.getMaxStackSize(), copy.getAmount() * 2));
                    }
                    log.getWorld().dropItemNaturally(log.getLocation(), copy);
                }

                log.setType(Material.AIR, false);
                brokenLogs++;
                if (log.equals(root)) {
                    rootBroken = true;
                }
                damageAxe(player, tool, level);
            }
        } finally {
            treeFellerInProgress.remove(uuid);
        }

        if (brokenLogs == 0 || !rootBroken) return;

        if (rootXp != null) {
            treeBaseXp = rootXp;
            xpManager.addXp(player, SkillType.LENHADOR, rootXp);
        }

        tryRareWoodDrop(player, level);

        applyCuttingComboBonus(player, treeBaseXp);
        if (replantTarget != null) {
            tryAutoReplant(player, replantTarget, rootMaterial, level);
        }

        if (configManager.config().getBoolean(
                "lenhador.leaf-cutter.remover-folhas-automaticamente", true)) {
            removeNearbyLeaves(logs, level, player);
        }
    }

    private void applyCuttingComboBonus(Player player, double treeBaseXp) {
        if (treeBaseXp <= 0) return;

        int unlock = configManager.config().getInt(
                "lenhador.combo-de-corte.nivel-desbloqueio", 150);
        if (getLenhadorLevel(player) < unlock) {
            clearCuttingCombo(player.getUniqueId());
            return;
        }

        int firstThreshold = Math.max(1, configManager.config().getInt(
                "lenhador.combo-de-corte.arvores-para-x2", 6));
        int nextThreshold = Math.max(1, configManager.config().getInt(
                "lenhador.combo-de-corte.arvores-por-proximo-nivel", 5));
        int firstWindowSeconds = Math.max(1, configManager.config().getInt(
                "lenhador.combo-de-corte.janela-inicial-segundos", 15));
        int nextWindowSeconds = Math.max(1, configManager.config().getInt(
                "lenhador.combo-de-corte.janela-apos-x2-segundos", 12));
        double bonusPerStage = Math.max(0.0, configManager.config().getDouble(
                "lenhador.combo-de-corte.bonus-por-estagio", 0.05));
        double maxBonus = Math.max(bonusPerStage, configManager.config().getDouble(
                "lenhador.combo-de-corte.bonus-maximo", 0.20));
        if (bonusPerStage <= 0.0) {
            clearCuttingCombo(player.getUniqueId());
            return;
        }
        int maxStage = Math.max(1, (int) Math.floor(maxBonus / bonusPerStage));

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastTreeFellAt.getOrDefault(uuid, 0L);
        int stage = Math.max(0, cuttingCombos.getOrDefault(uuid, 0));
        int treesInCurrentStage = comboTreeCounts.getOrDefault(uuid, 0);
        long stageStartedAt = comboStageStartedAt.getOrDefault(uuid, 0L);

        int currentWindow = stage == 0 ? firstWindowSeconds : nextWindowSeconds;
        if (last == 0L || stageStartedAt == 0L || now - stageStartedAt > currentWindow * 1000L) {
            stage = 0;
            treesInCurrentStage = 0;
            stageStartedAt = now;
        }

        treesInCurrentStage++;

        int required = stage == 0 ? firstThreshold : nextThreshold;
        if (treesInCurrentStage >= required && stage < maxStage) {
            stage++;
            treesInCurrentStage = 0;
            stageStartedAt = now;
        }

        cuttingCombos.put(uuid, stage);
        comboTreeCounts.put(uuid, treesInCurrentStage);
        comboStageStartedAt.put(uuid, stageStartedAt);
        lastTreeFellAt.put(uuid, now);

        double bonus = Math.min(maxBonus, stage * bonusPerStage);
        if (bonus > 0.0) {
            xpManager.addXp(player, SkillType.LENHADOR, treeBaseXp * bonus);
        }

        if (lastShownCuttingCombo.getOrDefault(uuid, 0) != stage) {
            lastShownCuttingCombo.put(uuid, stage);
            showCuttingComboMessage(player, stage, bonus * 100.0);
        }
    }

    private void clearCuttingCombo(UUID uuid) {
        lastTreeFellAt.remove(uuid);
        cuttingCombos.remove(uuid);
        comboTreeCounts.remove(uuid);
        comboStageStartedAt.remove(uuid);
        lastShownCuttingCombo.remove(uuid);
    }

    private Block findTreeBase(List<Block> logs) {
        Block best = null;
        for (Block log : logs) {
            if (!isWood(log.getType())) continue;
            Block below = log.getRelative(org.bukkit.block.BlockFace.DOWN);
            if (!below.getType().isSolid()) continue;
            if (best == null || log.getY() < best.getY()) best = log;
        }
        if (best != null) return best;
        for (Block log : logs) {
            if (!isWood(log.getType())) continue;
            if (best == null || log.getY() < best.getY()) best = log;
        }
        return best;
    }

    private void tryAutoReplant(Player player, Block root, Material rootMaterial, int level) {
        int unlock = configManager.config().getInt(
                "lenhador.replantio-automatico.nivel-desbloqueio", 10);
        if (level < unlock) return;

        double chance = Math.min(100.0, level * configManager.config().getDouble(
                "lenhador.replantio-automatico.chance-por-nivel", 0.10));
        if (random.nextDouble() * 100.0 >= chance) return;

        Material sapling = getReplantMaterial(rootMaterial);
        if (sapling == null) return;

        Block target = root;
        if (!target.getType().isAir() || !canPlaceSapling(target, sapling)) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !target.getType().isAir() || !canPlaceSapling(target, sapling)) {
                return;
            }

            target.setType(sapling, false);

            PlayerProfile profile = xpManager.getDataManager().getProfile(player.getUniqueId());
            long replanted = profile.incrementLenhadorArvoresReplantadas();
            xpManager.getDataManager().markDirty(player.getUniqueId());

            if (replanted > 0 && replanted % 100 == 0) {
                broadcastReplantMilestone(player, replanted);
            }
        });
    }

    private void broadcastReplantMilestone(Player player, long replanted) {
        String message = configManager.config().getString("lenhador.replantio-automatico.mensagem-marco", "");
        String jogador = getCargoColoredPlayerName(player);
        message = message.replace("{jogador}", jogador)
                .replace("{arvores}", String.valueOf(replanted));
        for (String line : message.split("\n", -1)) {
            // O chat pode ocultar mensagens totalmente vazias; um espaço preserva
            // visualmente cada linha em branco configurada antes/depois do anúncio.
            String linha = line.isEmpty() ? " " : line;
            player.getServer().broadcastMessage(MessageUtil.colorize(linha));
        }
    }

    private String getCargoColoredPlayerName(Player player) {
        String fallback = "&f" + player.getName();
        Plugin cargoPlus = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (cargoPlus == null || !cargoPlus.isEnabled()) {
            return fallback;
        }

        try {
            Method getCargoColor = cargoPlus.getClass().getMethod("getCargoColor", String.class);
            Method apiMethod = cargoPlus.getClass().getMethod("api");
            Object api = apiMethod.invoke(cargoPlus);
            Method apiGetGroup = api.getClass().getMethod("getGroup", UUID.class);
            Object group = apiGetGroup.invoke(api, player.getUniqueId());
            if (!(group instanceof String groupName) || groupName.isBlank()) {
                return fallback;
            }

            Object color = getCargoColor.invoke(cargoPlus, groupName);
            if (color instanceof String colorText && !colorText.isBlank()) {
                return colorText + player.getName();
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // CargoPlus e opcional; se a API mudar, mantemos o nome branco.
        }

        return fallback;
    }

    private void showCuttingComboMessage(Player player, int stage, double bonusPercent) {
        if (stage <= 0) {
            return;
        }

        String message = String.format(
                "&c&lCOMBO DE CORTE! &fEstágio %d &7• &a+%.1f%% XP",
                stage,
                bonusPercent
        );

        // O mcMMO usa a actionbar para as mensagens de XP. Em vez de disputar
        // esse mesmo espaço, o combo aparece como subtitle do título, acima da
        // actionbar, deixando a mensagem de XP do mcMMO intacta.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.sendTitle("", MessageUtil.colorize(message), 5, 60, 10);
            }
        });
    }

    private Material getReplantMaterial(Material log) {
        return switch (log) {
            case OAK_LOG -> Material.OAK_SAPLING;
            case SPRUCE_LOG -> Material.SPRUCE_SAPLING;
            case BIRCH_LOG -> Material.BIRCH_SAPLING;
            case JUNGLE_LOG -> Material.JUNGLE_SAPLING;
            case ACACIA_LOG -> Material.ACACIA_SAPLING;
            case DARK_OAK_LOG -> Material.DARK_OAK_SAPLING;
            case MANGROVE_LOG -> Material.MANGROVE_PROPAGULE;
            case CHERRY_LOG -> Material.CHERRY_SAPLING;
            default -> null;
        };
    }

    private boolean canPlaceSapling(Block target, Material sapling) {
        Block below = target.getRelative(org.bukkit.block.BlockFace.DOWN);
        return isSaplingSoil(below.getType(), sapling)
                && !placedBlockTracker.isPlaced(target)
                && target.getType().isAir();
    }

    private boolean isSaplingSoil(Material material, Material sapling) {
        if (sapling == Material.MANGROVE_PROPAGULE) {
            return material == Material.MUD
                    || material == Material.DIRT
                    || material == Material.GRASS_BLOCK
                    || material == Material.PODZOL
                    || material == Material.COARSE_DIRT
                    || material == Material.ROOTED_DIRT;
        }
        return material == Material.DIRT
                || material == Material.GRASS_BLOCK
                || material == Material.PODZOL
                || material == Material.COARSE_DIRT
                || material == Material.ROOTED_DIRT
                || material == Material.MOSS_BLOCK;
    }

    private void removeNearbyLeaves(List<Block> logs, int level, Player player) {
        if (logs.isEmpty()) return;

        int maxLeaves = Math.max(1, configManager.config().getInt("lenhador.leaf-cutter.max-folhas", 200));

        Set<String> visited = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>(logs);
        List<Block> leaves = new ArrayList<>();

        while (!queue.isEmpty() && leaves.size() < maxLeaves) {
            Block current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        Block next = current.getRelative(dx, dy, dz);
                        String key = next.getWorld().getUID() + ":" + next.getX() + ":" + next.getY() + ":" + next.getZ();

                        if (!visited.add(key)) continue;
                        if (placedBlockTracker.isPlaced(next)) continue;

                        if (isLeaves(next.getType())) {
                            leaves.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }

        double totalSeconds = getLeafDecayTimeSeconds(level);
        double variation = Math.max(0.0, configManager.config().getDouble(
                "lenhador.leaf-cutter.variacao-aleatoria-segundos", 0.35));

        for (Block leaf : leaves) {
            double multiplier = 1.0 + ((random.nextDouble() * 2.0 - 1.0) * variation);
            long delayTicks = Math.max(1L, Math.round(totalSeconds * multiplier * 20.0));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!isLeaves(leaf.getType()) || placedBlockTracker.isPlaced(leaf)) return;
                org.bukkit.event.block.BlockBreakEvent breakEvent =
                        new org.bukkit.event.block.BlockBreakEvent(leaf, player);
                Bukkit.getPluginManager().callEvent(breakEvent);
                if (breakEvent.isCancelled()) return;
                breakEvent.setDropItems(false);
                leaf.setType(Material.AIR, false);
            }, delayTicks);
        }
    }

    private double getLeafDecayTimeSeconds(int level) {
        double level1 = Math.max(1.0, configManager.config().getDouble(
                "lenhador.leaf-cutter.tempo-nivel-1-segundos", 120.0));
        double level500 = Math.max(1.0, configManager.config().getDouble(
                "lenhador.leaf-cutter.tempo-nivel-500-segundos", 30.0));
        double level1000 = Math.max(1.0, configManager.config().getDouble(
                "lenhador.leaf-cutter.tempo-nivel-1000-segundos", 12.0));

        int clampedLevel = Math.max(1, Math.min(1000, level));

        if (clampedLevel <= 500) {
            double progress = (clampedLevel - 1) / 499.0;
            return level1 + (level500 - level1) * progress;
        }

        double progress = (clampedLevel - 500) / 500.0;
        return level500 + (level1000 - level500) * progress;
    }

    private List<Block> collectConnectedLogs(Block root, int maxBlocks) {
        List<Block> result = new ArrayList<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(root);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            String key = current.getWorld().getUID() + ":" + current.getX() + ":" + current.getY() + ":" + current.getZ();
            if (!visited.add(key)) continue;
            if (!isWood(current.getType())) continue;

            result.add(current);

            for (BlockFace face : TREE_FACES) {
                queue.add(current.getRelative(face));
            }
        }

        return result;
    }

    private void tryRareWoodDrop(Player player, int level) {
        int unlock = configManager.config().getInt(
                "lenhador.critico-lenhador.nivel-desbloqueio", 100);
        if (level < unlock) return;

        double chance = getLenhadorCriticoChance(level, unlock);

        if (random.nextDouble() * 100.0 >= chance) return;

        Material[] rewards = {
                Material.DIAMOND,
                Material.EMERALD,
                Material.GOLD_INGOT,
                Material.IRON_INGOT,
                Material.LAPIS_LAZULI,
                Material.REDSTONE
        };
        Material reward = rewards[random.nextInt(rewards.length)];
        player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(reward));
    }

    private double getLenhadorCriticoChance(int level, int unlock) {
        if (level < unlock) return 0.0;

        // Curva de chance do Crítico do Lenhador:
        // 100=2%, 200=4%, 300=6%, 500=10%, 700=15%,
        // 900=20%, 999=25%, 1000+=30%.
        int[] levels = {100, 200, 300, 500, 700, 900, 999, 1000};
        double[] chances = {2.0, 4.0, 6.0, 10.0, 15.0, 20.0, 25.0, 30.0};

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

    private boolean shouldDoubleDrop(int level) {
        double chance = Math.min(
                configManager.config().getDouble("lenhador.double-drop.chance-maxima", 50.0),
                level * configManager.config().getDouble("lenhador.double-drop.chance-por-nivel", 0.05)
        );
        return random.nextDouble() * 100.0 < chance;
    }

    private int getLenhadorLevel(Player player) {
        return xpManager.getDataManager().getProfile(player.getUniqueId()).getLevel(SkillType.LENHADOR);
    }

    private boolean isWood(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM");
    }

    private boolean isLeaves(Material material) {
        String name = material.name();
        return name.endsWith("_LEAVES")
                || material == Material.NETHER_WART_BLOCK
                || material == Material.WARPED_WART_BLOCK
                || material == Material.SHROOMLIGHT;
    }

    private boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE
                || material == Material.STONE_AXE
                || material == Material.IRON_AXE
                || material == Material.GOLDEN_AXE
                || material == Material.DIAMOND_AXE
                || material == Material.NETHERITE_AXE;
    }

    private boolean isConfiguredGatheringBlock(Material material) {
        for (String section : SECOES) {
            if (configManager.xpDeSeConfigurado(section, material.name()) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isTreeProtectionBlock(Material material) {
        return isWood(material) || isLeaves(material) || isConfiguredGatheringBlock(material);
    }

    private boolean shouldPreserveAxe(int level) {
        double baseChance = level * configManager.config().getDouble(
                "lenhador.machado-reforcado.chance-preservar-por-nivel", 0.05);
        int efficientUnlock = configManager.config().getInt(
                "lenhador.colheita-eficiente.nivel-desbloqueio", 100);
        double efficientBonus = level >= efficientUnlock
                ? (level - efficientUnlock + 1) * configManager.config().getDouble(
                "lenhador.colheita-eficiente.bonus-por-nivel", 0.025)
                : 0.0;
        double chance = Math.min(75.0, baseChance + efficientBonus);
        return random.nextDouble() * 100.0 < chance;
    }

    private void damageAxe(Player player, ItemStack tool, int level) {
        if (!isAxe(tool.getType()) || tool.getAmount() <= 0 || shouldPreserveAxe(level)) return;

        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0 && random.nextInt(unbreaking + 1) != 0) return;

        PlayerItemDamageEvent damageEvent = new PlayerItemDamageEvent(player, tool, 1);
        Bukkit.getPluginManager().callEvent(damageEvent);
        if (damageEvent.isCancelled() || damageEvent.getDamage() <= 0) return;

        if (!(tool.getItemMeta() instanceof Damageable damageable)) return;
        int damage = damageable.getDamage() + damageEvent.getDamage();
        if (damage >= tool.getType().getMaxDurability()) {
            tool.setAmount(tool.getAmount() - 1);
            return;
        }

        damageable.setDamage(damage);
        tool.setItemMeta(damageable);
    }

    private boolean isValidMiningTool(Block block, ItemStack tool) {
        return isPickaxe(tool.getType()) && block.isPreferredTool(tool);
    }

    private boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE
                || material == Material.STONE_PICKAXE
                || material == Material.IRON_PICKAXE
                || material == Material.GOLDEN_PICKAXE
                || material == Material.DIAMOND_PICKAXE
                || material == Material.NETHERITE_PICKAXE;
    }
}