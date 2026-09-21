package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class CombatListener implements Listener {

    private final ConfigManager configManager;
    private final XpManager xpManager;

    public CombatListener(ConfigManager configManager, XpManager xpManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        boolean vitimaEhJogador = event.getEntity() instanceof Player;
        boolean contarPvp = configManager.config().getBoolean("xp.combate.contar-pvp", false);
        if (vitimaEhJogador && !contarPvp) return;

        Object causador = event.getDamager();

        if (causador instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player atirador) {
                double xp = configManager.config().getDouble("xp.combate.xp-por-acerto-arco", 20);
                xpManager.addXp(atirador, SkillType.ARQUERIA, xp);
            }
            return;
        }

        if (causador instanceof Player atacante) {
            ItemStack arma = atacante.getInventory().getItemInMainHand();
            Material tipo = arma.getType();

            if (isEspada(tipo)) {
                double xp = configManager.config().getDouble("xp.combate.xp-por-acerto-espada", 15);
                xpManager.addXp(atacante, SkillType.ESPADAS, xp);
            } else if (isMachado(tipo)) {
                double xp = configManager.config().getDouble("xp.combate.xp-por-acerto-machado", 18);
                xpManager.addXp(atacante, SkillType.MACHADOS, xp);
            } else if (tipo == Material.AIR) {
                double xp = configManager.config().getDouble("xp.combate.xp-por-acerto-desarmado", 12);
                xpManager.addXp(atacante, SkillType.DESARMADO, xp);
            }
        }
    }

    private boolean isEspada(Material tipo) {
        return tipo.name().endsWith("_SWORD");
    }

    private boolean isMachado(Material tipo) {
        return tipo.name().endsWith("_AXE");
    }
}
