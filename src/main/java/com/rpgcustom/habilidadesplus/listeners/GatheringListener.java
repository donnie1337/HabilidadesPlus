package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.PlacedBlockTracker;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

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

    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final PlacedBlockTracker placedBlockTracker;

    public GatheringListener(ConfigManager configManager, XpManager xpManager,
                             PlacedBlockTracker placedBlockTracker) {
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
}
