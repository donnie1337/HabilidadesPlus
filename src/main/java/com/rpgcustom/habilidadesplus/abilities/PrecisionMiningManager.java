package com.rpgcustom.habilidadesplus.abilities;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class PrecisionMiningManager implements Listener {
    private static final long MINING_CONTEXT_TTL_NANOS = 250_000_000L;

    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final Map<UUID, Long> miningContextUntil = new HashMap<>();

    public PrecisionMiningManager(DataManager dataManager, ConfigManager configManager) {
        this.dataManager = dataManager;
        this.configManager = configManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (!isPickaxe(item.getType())) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();
        if (!configManager.habilidadeAtiva(event.getPlayer()) || !isMiningContext(uuid)) {
            return;
        }

        int level = dataManager.getProfile(uuid).getLevel(SkillType.MINERACAO);
        int unlockLevel = configManager.config().getInt(
                "mineracao.mineracao-precisa.nivel-desbloqueio", 75
        );
        if (level < unlockLevel) {
            return;
        }

        double chance = Math.min(100.0, level * configManager.config().getDouble(
                "mineracao.mineracao-precisa.chance-preservar-por-nivel", 0.1
        ));
        if (ThreadLocalRandom.current().nextDouble(100.0) < chance) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!configManager.habilidadeAtiva(event.getPlayer())
                || !isPickaxe(item.getType())
                || !event.getBlock().isPreferredTool(item)) {
            return;
        }
        miningContextUntil.put(event.getPlayer().getUniqueId(),
                System.nanoTime() + MINING_CONTEXT_TTL_NANOS);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!configManager.habilidadeAtiva(event.getPlayer())
                || !isPickaxe(item.getType())
                || !event.getBlock().isPreferredTool(item)) {
            return;
        }
        miningContextUntil.put(event.getPlayer().getUniqueId(),
                System.nanoTime() + MINING_CONTEXT_TTL_NANOS);
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        miningContextUntil.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        miningContextUntil.remove(event.getPlayer().getUniqueId());
    }

    private boolean isMiningContext(UUID uuid) {
        Long expiresAt = miningContextUntil.get(uuid);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.nanoTime()) {
            miningContextUntil.remove(uuid);
            return false;
        }
        return true;
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
