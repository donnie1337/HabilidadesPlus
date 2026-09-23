package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.PlacedBlockTracker;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.GameMode;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Item;

/**
 * Cobre 4 habilidades de uma vez, pois todas nascem do mesmo evento
 * (quebrar um bloco): Mineracao, Escavacao, Lenhador e Ervanismo.
 *
 * Inclui protecao basica contra "farm" de XP: um bloco colocado pelo
 * proprio jogador nao da XP ao ser quebrado (evita, por exemplo, colocar
 * e quebrar areia repetidamente).
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

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final PlacedBlockTracker placedBlockTracker;

    public GatheringListener(JavaPlugin plugin, ConfigManager configManager, XpManager xpManager,
                             PlacedBlockTracker placedBlockTracker) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.xpManager = xpManager;
        this.placedBlockTracker = placedBlockTracker;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!configManager.mundoDesabilitado(event.getBlock().getWorld().getName())
                && isConfiguredGatheringBlock(event.getBlock().getType())) {
            if (!placedBlockTracker.add(event.getBlock())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (placedBlockTracker.removeIfPlaced(block)) {
            return;
        }

        Material material = block.getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        for (int i = 0; i < HABILIDADES.length; i++) {
            Double xp = configManager.xpDeSeConfigurado(SECOES[i], material.name());
            if (xp == null) {
                continue;
            }
            if (HABILIDADES[i] == SkillType.MINERACAO && !isValidMiningTool(block, tool)) {
                return;
            }
            xpManager.addXp(player, HABILIDADES[i], xp);
            return;
        }
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

    private boolean isConfiguredGatheringBlock(Material material) {
        for (String section : SECOES) {
            if (configManager.xpDeSeConfigurado(section, material.name()) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidMiningTool(Block block, ItemStack tool) {
        if (!isPickaxe(tool.getType())) {
            return false;
        }
        return block.isPreferredTool(tool);
    }

    private boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE
                || material == Material.STONE_PICKAXE
                || material == Material.IRON_PICKAXE
                || material == Material.GOLDEN_PICKAXE
                || material == Material.DIAMOND_PICKAXE
                || material == Material.NETHERITE_PICKAXE;
    }
}    private void handleWoodBreak(BlockBreakEvent event, Player player, Block root) {
        int level = xpManager.getDataManager().getProfile(player.getUniqueId()).getLevel(SkillType.LENHADOR);
        ItemStack tool = player.getInventory().getItemInMainHand();

        Double xp = configManager.xpDeSeConfigurado("lenhador", root.getType().name());
        if (xp != null) {
            xpManager.addXp(player, SkillType.LENHADOR, xp);
        }

        int unlock = configManager.config().getInt("lenhador.tree-feller.nivel-desbloqueio", 25);
        if (level >= unlock && isAxe(tool.getType()) && !player.isSneaking()) {
            event.setDropItems(false);
            breakTree(player, root, tool, level);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWoodDrop(BlockDropItemEvent event) {
        if (!isWood(event.getBlockState().getType())) return;

        Player player = event.getPlayer();
        int level = xpManager.getDataManager().getProfile(player.getUniqueId()).getLevel(SkillType.LENHADOR);
        if (level <= 0) return;

        boolean doubled = shouldDoubleDrop(level);
        for (Item item : event.getItems()) {
            if (doubled) {
                ItemStack stack = item.getItemStack();
                stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() * 2));
                item.setItemStack(stack);
            }
        }
        tryRareWoodDrop(player, level);
    }

    private void breakTree(Player player, Block root, ItemStack tool, int level) {
        List<Block> logs = collectConnectedLogs(root);
        int maxBlocks = Math.min(MAX_TREE_BLOCKS_HARD_LIMIT,
                Math.max(1, configManager.config().getInt("lenhador.tree-feller.max-troncos", 64)));
        if (logs.size() > maxBlocks) {
            logs = new ArrayList<>(logs.subList(0, maxBlocks));
        }

        for (Block log : logs) {
            if (placedBlockTracker.isPlaced(log)) continue;
            if (!isWood(log.getType())) continue;

            Double xp = configManager.xpDeSeConfigurado("lenhador", log.getType().name());
            if (xp != null) xpManager.addXp(player, SkillType.LENHADOR, xp);

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

        if (configManager.config().getBoolean("lenhador.leaf-cutter.remover-folhas-automaticamente", true)) {
            removeNearbyLeaves(logs, level);
        }
    }


