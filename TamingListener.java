package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

public class TamingListener implements Listener {

    private final ConfigManager configManager;
    private final XpManager xpManager;

    public TamingListener(ConfigManager configManager, XpManager xpManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        AnimalTamer dono = event.getOwner();
        if (!(dono instanceof Player player)) return;

        double xp = configManager.config().getDouble("xp.domesticacao.xp-por-domesticar", 250);
        xpManager.addXp(player, SkillType.DOMESTICACAO, xp);
    }
}
