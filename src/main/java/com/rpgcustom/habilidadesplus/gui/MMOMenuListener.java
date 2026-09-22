package com.rpgcustom.habilidadesplus.gui;

import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MMOMenuListener implements Listener {

    private final DataManager dataManager;
    private final LevelingManager levelingManager;
    private final ConfigManager configManager;

    public MMOMenuListener(DataManager dataManager, LevelingManager levelingManager, ConfigManager configManager) {
        this.dataManager = dataManager;
        this.levelingManager = levelingManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MMOMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        if (holder.ranking()) {
            if (event.getRawSlot() == 31) {
                MMOMenu.open(player, dataManager, levelingManager, configManager);
                return;
            }
            if (event.getRawSlot() == 33 && holder.skill() != null) {
                SkillType[] skills = SkillType.values();
                int current = holder.skill().ordinal();
                int next = (current + 1) % skills.length;
                MMOMenu.openRanking(player, skills[next], dataManager, configManager);
            }
            return;
        }

        if (holder.skill() == null) {
            if (event.getRawSlot() == 50) {
                MMOMenu.openRanking(player, SkillType.MINERACAO, dataManager, configManager);
                return;
            }
            SkillType skill = MMOMenu.skillAtSlot(event.getRawSlot());
            if (skill != null) {
                MMOMenu.openSkill(player, skill, dataManager, levelingManager, configManager);
            }
            return;
        }

        if (event.getRawSlot() == 31) {
            MMOMenu.open(player, dataManager, levelingManager, configManager);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MMOMenuHolder) {
            event.setCancelled(true);
        }
    }
}
