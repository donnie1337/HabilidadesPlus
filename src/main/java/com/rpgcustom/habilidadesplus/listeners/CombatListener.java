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
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatListener implements Listener {

    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final Map<UUID, SkillType> projectileSkills = new ConcurrentHashMap<>();

    public CombatListener(ConfigManager configManager, XpManager xpManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    /**
     * Registra a arma usada no disparo. Quando a flecha acertar, conseguimos
     * distinguir corretamente uma besta de um arco.
     */
    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getProjectile() instanceof Projectile projectile)) return;

        ItemStack weapon = event.getBow();
        if (weapon != null && weapon.getType() == Material.CROSSBOW) {
            projectileSkills.put(projectile.getUniqueId(), SkillType.BESTAS);
        } else {
            projectileSkills.put(projectile.getUniqueId(), SkillType.ARQUERIA);
        }
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
            if (!(source instanceof Player atirador)) return;

            if ("TRIDENT".equals(projectile.getType().name())) {
                addCombatXp(atirador, SkillType.TRIDENTES, "xp-por-acerto-tridente", 24);
                return;
            }

            SkillType skill = projectileSkills.remove(projectile.getUniqueId());
            if (skill == SkillType.BESTAS) {
                addCombatXp(atirador, SkillType.BESTAS, "xp-por-acerto-besta", 22);
            } else if (skill == SkillType.ARQUERIA || projectile.getType().name().contains("ARROW")) {
                addCombatXp(atirador, SkillType.ARQUERIA, "xp-por-acerto-arco", 20);
            }
            return;
        }

        if (!(causador instanceof Player atacante)) return;

        Material tipo = atacante.getInventory().getItemInMainHand().getType();
        if (isEspada(tipo)) {
            addCombatXp(atacante, SkillType.ESPADAS, "xp-por-acerto-espada", 15);
        } else if (isMachado(tipo)) {
            addCombatXp(atacante, SkillType.MACHADOS, "xp-por-acerto-machado", 18);
        } else if (tipo == Material.MACE) {
            addCombatXp(atacante, SkillType.CLAVA, "xp-por-acerto-clava", 22);
        } else if (isLanca(tipo)) {
            addCombatXp(atacante, SkillType.LANCAS, "xp-por-acerto-lanca", 24);
        } else if (tipo == Material.AIR) {
            addCombatXp(atacante, SkillType.DESARMADO, "xp-por-acerto-desarmado", 12);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() == null) {
            projectileSkills.remove(event.getEntity().getUniqueId());
        }
    }

    private void addCombatXp(Player player, SkillType skill, String configKey, double defaultValue) {
        double xp = configManager.config().getDouble("xp.combate." + configKey, defaultValue);
        xpManager.addXp(player, skill, xp);
    }

    private boolean isEspada(Material tipo) {
        return tipo.name().endsWith("_SWORD");
    }

    private boolean isMachado(Material tipo) {
        return tipo.name().endsWith("_AXE");
    }

    private boolean isLanca(Material tipo) {
        String name = tipo.name();
        return name.equals("SPEAR") || name.endsWith("_SPEAR");
    }
}
