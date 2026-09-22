package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * Recompensa atividades de produção: reparar itens na bigorna e retirar
 * itens fundidos de fornalhas, altos-fornos e defumadores.
 */
public class ProductionListener implements Listener {

    private final ConfigManager configManager;
    private final XpManager xpManager;

    public ProductionListener(ConfigManager configManager, XpManager xpManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onRepair(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL || event.getRawSlot() != 2) return;
        if (!(event.getWhoClicked() instanceof Player player) || player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;
        ItemStack original = event.getInventory().getItem(0);
        if (original == null || !(original.getItemMeta() instanceof Damageable before)
                || !(result.getItemMeta() instanceof Damageable after)
                || after.getDamage() >= before.getDamage()) return;

        double xp = configManager.config().getDouble("xp.reparacao.xp-por-reparo", 75);
        xpManager.addXp(player, SkillType.REPARACAO, xp);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || event.getItemAmount() <= 0) return;

        double xpPerItem = configManager.config().getDouble("xp.fundicao.xp-por-item-fundido", 15);
        xpManager.addXp(player, SkillType.FUNDICAO, xpPerItem * event.getItemAmount());
    }
}
