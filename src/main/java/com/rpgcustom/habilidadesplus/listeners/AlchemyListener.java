package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Atribui a fermentacao ao ultimo jogador que interagiu com o suporte e concede
 * XP somente quando o BrewEvent confirma que uma fermentacao terminou.
 */
public class AlchemyListener implements Listener {
    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final Map<Location, UUID> lastBrewer = new HashMap<>();

    public AlchemyListener(ConfigManager configManager, XpManager xpManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingStandClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.BREWING || top.getLocation() == null) return;
        if (event.getWhoClicked() instanceof Player player) {
            lastBrewer.put(blockLocation(top.getLocation()), player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingStandDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.BREWING || top.getLocation() == null) return;
        if (event.getWhoClicked() instanceof Player player) {
            lastBrewer.put(blockLocation(top.getLocation()), player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        UUID brewerId = lastBrewer.remove(blockLocation(event.getBlock().getLocation()));
        if (brewerId == null) return;
        Player player = Bukkit.getPlayer(brewerId);
        if (player == null || !player.isOnline()) return;

        int potions = 0;
        for (int slot = 0; slot < 3; slot++) {
            ItemStack item = event.getContents().getItem(slot);
            if (item != null && isPotion(item.getType())) potions += item.getAmount();
        }
        if (potions <= 0) return;
        double xpPerPotion = configManager.config().getDouble("xp.alquimia.xp-por-pocao-fermentada", 150);
        xpManager.addXp(player, SkillType.ALQUIMIA, xpPerPotion * potions);
    }

    private boolean isPotion(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    private Location blockLocation(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
