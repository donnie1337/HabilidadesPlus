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
