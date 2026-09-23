package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.PlacedBlockTracker;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
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

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final PlacedBlockTracker placedBlockTracker;
    private final Random random = new Random();
    private final Map<UUID, Long> lastTreeFellAt = new HashMap<>();
    private final Map<UUID, Integer> cuttingCombos = new HashMap<>();

    public GatheringListener(JavaPlugin plugin, ConfigManager configManager, XpManager xpManager,
                             PlacedBlockTracker placedBlockTracker) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.xpManager = xpManager;
        this.placedBlockTracker = placedBlockTracker;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!configManager.mundoDesabilitado(event.getBlock().getWorld().getName())
                && isConfiguredGatheringBlock(event.getBlock().getType())
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

        Material material = block.getType();

        if (isWood(material)) {
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

            xpManager.addXp(player, HABILIDADES[i], xp);
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWoodDrop(BlockDropItemEvent event) {
        if (placedBlockTracker.consumeProtectedDrop(event.getBlock())) return;
        if (!isWood(event.getBlockState().getType())) return;

        Player player = event.getPlayer();
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAxeDamage(EntityDamageItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isAxe(event.getItem().getType())) return;
        if (configManager.mundoDesabilitado(player.getWorld().getName())) return;

        int level = getLenhadorLevel(player);
        if (level <= 0) return;

        double baseChance = level * configManager.config().getDouble(
                "lenhador.machado-reforcado.chance-preservar-por-nivel", 0.05);

        int efficientUnlock = configManager.config().getInt(
                "lenhador.colheita-eficiente.nivel-desbloqueio", 100);
        double efficientBonus = level >= efficientUnlock
                ? (level - efficientUnlock + 1) * configManager.config().getDouble(
                "lenhador.colheita-eficiente.bonus-por-nivel", 0.025)
                : 0.0;

        double chance = Math.min(75.0, baseChance + efficientBonus);
        if (random.nextDouble() * 100.0 < chance) {
            event.setCancelled(true);
        }
    }

    private void handleWoodBreak(BlockBreakEvent event, Player player, Block root) {
        int level = getLenhadorLevel(player);
        ItemStack tool = player.getInventory().getItemInMainHand();

        int unlock = configManager.config().getInt("lenhador.tree-feller.nivel-desbloqueio", 25);
        if (level >= unlock && isAxe(tool.getType()) && !player.isSneaking()) {
            event.setDropItems(false);
            breakTree(player, root, tool, level);
            return;
        }

        Double xp = configManager.xpDeSeConfigurado("lenhador", root.getType().name());
        if (xp != null) {
            xpManager.addXp(player, SkillType.LENHADOR, xp);
        }
    }

    private void breakTree(Player player, Block root, ItemStack tool, int level) {
        List<Block> logs = collectConnectedLogs(root);
        int maxBlocks = Math.min(
                MAX_TREE_BLOCKS_HARD_LIMIT,
                Math.max(1, configManager.config().getInt("lenhador.tree-feller.max-troncos", 64))
        );
        if (logs.size() > maxBlocks) {
            logs = new ArrayList<>(logs.subList(0, maxBlocks));
        }

        double treeBaseXp = 0.0;
        for (Block log : logs) {
            if (placedBlockTracker.isPlaced(log) || !isWood(log.getType())) continue;

            Double xp = configManager.xpDeSeConfigurado("lenhador", log.getType().name());
            if (xp != null) {
                treeBaseXp += xp;
                xpManager.addXp(player, SkillType.LENHADOR, xp);
            }

            Collection<ItemStack> drops = log.getDrops(tool, player);
            for (ItemStack drop : drops) {
                ItemStack copy = drop.clone();
                if (shouldDoubleDrop(level)) {
                    copy.setAmount(Math.min(copy.getMaxStackSize(), copy.getAmount() * 2));
                }
                log.getWorld().dropItemNaturally(log.getLocation(), copy);
            }

            tryRareWoodDrop(player, level);
            log.setType(Material.AIR, false);
        }

        applyCuttingComboBonus(player, treeBaseXp);
        tryAutoReplant(player, root, level);

        if (configManager.config().getBoolean(
                "lenhador.leaf-cutter.remover-folhas-automaticamente", true)) {
            removeNearbyLeaves(logs, level);
        }
    }

    private void applyCuttingComboBonus(Player player, double treeBaseXp) {
        if (treeBaseXp <= 0) return;

        int windowSeconds = Math.max(1, configManager.config().getInt(
                "lenhador.combo-de-corte.janela-segundos", 10));
        int maxCombo = Math.max(1, configManager.config().getInt(
                "lenhador.combo-de-corte.combo-maximo", 5));
        double bonusPorCombo = Math.max(0.0, configManager.config().getDouble(
                "lenhador.combo-de-corte.bonus-xp-por-combo", 5.0));

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastTreeFellAt.getOrDefault(uuid, 0L);
        int combo = last > 0L && now - last <= windowSeconds * 1000L
                ? Math.min(maxCombo, cuttingCombos.getOrDefault(uuid, 0) + 1)
                : 1;

        cuttingCombos.put(uuid, combo);
        lastTreeFellAt.put(uuid, now);

        double bonusPercent = Math.max(0, combo - 1) * bonusPorCombo;
        if (bonusPercent > 0) {
            xpManager.addXp(player, SkillType.LENHADOR, treeBaseXp * bonusPercent / 100.0);
        }
    }

    private void tryAutoReplant(Player player, Block root, int level) {
        int unlock = configManager.config().getInt(
                "lenhador.replantio-automatico.nivel-desbloqueio", 100);
        if (level < unlock) return;

        double chance = Math.min(100.0, level * configManager.config().getDouble(
                "lenhador.replantio-automatico.chance-por-nivel", 0.10));
        if (random.nextDouble() * 100.0 >= chance) return;

        Material sapling = getReplantMaterial(root.getType());
        if (sapling == null) return;

        Block target = root;
        if (!target.getType().isAir() || !canPlaceSapling(target, sapling)) {
            return;
        }

        target.setType(sapling, false);
        PlayerProfile profile = xpManager.getDataManager().getProfile(player.getUniqueId());
        profile.incrementLenhadorArvoresReplantadas();
        xpManager.getDataManager().markDirty(player.getUniqueId());
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
        return below.getType().isSolid()
                && !placedBlockTracker.isPlaced(target)
                && (sapling != Material.MANGROVE_PROPAGULE || below.getType() != Material.NETHER_WART_BLOCK);
    }

    private void removeNearbyLeaves(List<Block> logs, int level) {
        if (logs.isEmpty()) return;

        int maxLeaves = Math.max(1, configManager.config().getInt("lenhador.leaf-cutter.max-folhas", 200));

        // A busca começa nos troncos derrubados e só atravessa folhas.
        // Assim, uma árvore próxima não é atingida apenas por estar dentro de um raio.
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
                if (isLeaves(leaf.getType()) && !placedBlockTracker.isPlaced(leaf)) {
                    leaf.setType(Material.AIR, false);
                }
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

    private List<Block> collectConnectedLogs(Block root) {
        List<Block> result = new ArrayList<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(root);

        while (!queue.isEmpty() && result.size() < MAX_TREE_BLOCKS_HARD_LIMIT) {
            Block current = queue.poll();
            String key = current.getWorld().getUID() + ":" + current.getX() + ":" + current.getY() + ":" + current.getZ();
            if (!visited.add(key)) continue;
            if (!isWood(current.getType())) continue;

            result.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        queue.add(current.getRelative(dx, dy, dz));
                    }
                }
            }
        }

        return result;
    }

    private void tryRareWoodDrop(Player player, int level) {
        int unlock = configManager.config().getInt(
                "lenhador.critico-lenhador.nivel-desbloqueio", 500);
        if (level < unlock) return;

        double base = configManager.config().getDouble(
                "lenhador.critico-lenhador.chance-no-nivel-desbloqueio", 0.15);
        double increment = configManager.config().getDouble(
                "lenhador.critico-lenhador.incremento-por-100-niveis", 0.05);
        double chance = Math.min(
                configManager.config().getDouble("lenhador.critico-lenhador.chance-maxima", 0.50),
                base + Math.max(0, (level - unlock) / 100) * increment
        );

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
        return name.endsWith("_LEAVES");
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
