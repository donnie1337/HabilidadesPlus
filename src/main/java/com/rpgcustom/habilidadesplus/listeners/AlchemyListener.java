package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Concede XP de Alquimia quando o jogador retira uma pocao pronta
 * dos 3 slots de saida do suporte de fermentacao (brewing stand).
 * E uma forma simplificada de detectar "uma pocao foi fermentada",
 * sem precisar rastrear quem colocou os ingredientes originalmente.
 */
public class AlchemyListener implements Listener {

    private final ConfigManager configManager;
    private final XpManager xpManager;

    public AlchemyListener(ConfigManager configManager, XpManager xpManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingStandClick(InventoryClickEvent event) {
        Inventory clicado = event.getClickedInventory();
        if (clicado == null || clicado.getType() != InventoryType.BREWING) return;
        if (event.getSlot() > 2) return; // slots 0,1,2 sao as 3 saidas de pocao

        ItemStack item = event.getCurrentItem();
        if (item == null || !isPocao(item.getType())) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;

        double xp = configManager.config().getDouble("xp.alquimia.xp-por-pocao-fermentada", 150);
        xpManager.addXp(player, SkillType.ALQUIMIA, xp);
    }

    private boolean isPocao(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }
}
