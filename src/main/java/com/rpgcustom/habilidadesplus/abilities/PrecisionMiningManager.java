package com.rpgcustom.habilidadesplus.abilities;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class PrecisionMiningManager implements Listener {

    private final DataManager dataManager;
    private final ConfigManager configManager;

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

    private boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE
                || material == Material.STONE_PICKAXE
                || material == Material.IRON_PICKAXE
                || material == Material.GOLDEN_PICKAXE
                || material == Material.DIAMOND_PICKAXE
                || material == Material.NETHERITE_PICKAXE;
    }
}
