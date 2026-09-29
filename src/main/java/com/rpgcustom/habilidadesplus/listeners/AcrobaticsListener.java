package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

public class AcrobaticsListener implements Listener {

    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final DataManager dataManager;

    public AcrobaticsListener(ConfigManager configManager, XpManager xpManager, DataManager dataManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
        this.dataManager = dataManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDodge(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        int level = dataManager.getProfile(player.getUniqueId()).getLevel(SkillType.ACROBACIA);
        int unlock = configManager.config().getInt("acrobacia.esquiva.nivel-desbloqueio", 75);
        if (level < unlock) return;

        double perLevel = Math.max(0.0,
                configManager.config().getDouble("acrobacia.esquiva.chance-por-nivel", 0.015));
        double maxChance = Math.max(0.0,
                configManager.config().getDouble("acrobacia.esquiva.chance-maxima", 15.0));
        double chance = Math.min(maxChance, level * perLevel);

        if (ThreadLocalRandom.current().nextDouble(100.0) < chance) {
            double reduction = Math.min(0.90, Math.max(0.0,
                    configManager.config().getDouble("acrobacia.esquiva.reducao-dano", 0.50)));
            event.setDamage(event.getDamage() * (1.0 - reduction));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        double dano = event.getFinalDamage();
        if (dano <= 0) return;

        int nivelAtual = dataManager.getProfile(player.getUniqueId()).getLevel(SkillType.ACROBACIA);
        double chancePorNivel = configManager.config().getDouble("xp.acrobacia.chance-role-por-nivel", 0.002);
        double chanceMaxima = configManager.config().getDouble("xp.acrobacia.chance-role-maxima", 0.4);
        double chance = Math.min(chanceMaxima, nivelAtual * chancePorNivel);

        if (Math.random() < chance) {
            event.setCancelled(true);
            player.sendMessage(MessageUtil.colorize(configManager.msg("acrobacia.mensagem-role")));
        }

        double xpPorPonto = configManager.config().getDouble("xp.acrobacia.xp-por-ponto-de-dano-queda", 8);
        xpManager.addXp(player, SkillType.ACROBACIA, dano * xpPorPonto);
    }
}
