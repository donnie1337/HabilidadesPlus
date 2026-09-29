package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class CombatListener implements Listener {

    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final Map<UUID, SkillType> projectileSkills = new ConcurrentHashMap<>();
    private final Map<UUID, Location> projectileOrigins = new ConcurrentHashMap<>();
    private final Set<UUID> syntheticDamageTargets = ConcurrentHashMap.newKeySet();

    public CombatListener(ConfigManager configManager, XpManager xpManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Projectile projectile)) return;

        ItemStack weapon = event.getBow();
        SkillType skill = weapon != null && weapon.getType() == Material.CROSSBOW
                ? SkillType.BESTAS : SkillType.ARQUERIA;

        projectileSkills.put(projectile.getUniqueId(), skill);
        projectileOrigins.put(projectile.getUniqueId(), player.getLocation().clone());

        if (!(projectile instanceof AbstractArrow arrow)) return;

        int level = level(player, skill);
        if (skill == SkillType.ARQUERIA) {
            int unlock = configManager.config().getInt("arqueria.tiro-perfurante.nivel-desbloqueio", 100);
            if (level >= unlock && roll(progressiveChance(level,
                    "arqueria.tiro-perfurante.chance-por-nivel", 0.02,
                    "arqueria.tiro-perfurante.chance-maxima", 20.0))) {
                arrow.setPierceLevel(Math.max(1, arrow.getPierceLevel()));
            }
        } else {
            int unlock = configManager.config().getInt("bestas.salva-perfurante.nivel-desbloqueio", 100);
            if (level >= unlock && roll(progressiveChance(level,
                    "bestas.salva-perfurante.chance-por-nivel", 0.03,
                    "bestas.salva-perfurante.chance-maxima", 30.0))) {
                arrow.setPierceLevel(Math.max(1, arrow.getPierceLevel()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (syntheticDamageTargets.contains(victim.getUniqueId())) return;

        boolean victimIsPlayer = victim instanceof Player;
        boolean countPvp = configManager.config().getBoolean("xp.combate.contar-pvp", false);
        if (victimIsPlayer && !countPvp) return;

        if (event.getDamager() instanceof Projectile projectile) {
            handleProjectileDamage(event, victim, projectile);
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) return;

        Material type = attacker.getInventory().getItemInMainHand().getType();
        if (isEspada(type)) {
            handleSword(event, attacker, victim, countPvp);
            addCombatXp(attacker, SkillType.ESPADAS, "xp-por-acerto-espada", 15);
        } else if (isMachado(type)) {
            handleAxe(event, attacker, victim);
            addCombatXp(attacker, SkillType.MACHADOS, "xp-por-acerto-machado", 18);
        } else if (type == Material.MACE) {
            handleMace(event, attacker, victim, countPvp);
            addCombatXp(attacker, SkillType.CLAVA, "xp-por-acerto-clava", 22);
        } else if (isLanca(type)) {
            handleSpear(event, attacker);
            addCombatXp(attacker, SkillType.LANCAS, "xp-por-acerto-lanca", 24);
        } else if (type == Material.AIR) {
            handleUnarmed(event, attacker, victim);
            addCombatXp(attacker, SkillType.DESARMADO, "xp-por-acerto-desarmado", 12);
        }

        if (victim instanceof Player defender && isEspada(defender.getInventory().getItemInMainHand().getType())) {
            tryReverseGuard(event, defender, attacker);
        }
    }

    private void handleProjectileDamage(EntityDamageByEntityEvent event, LivingEntity victim, Projectile projectile) {
        ProjectileSource source = projectile.getShooter();
        if (!(source instanceof Player shooter)) return;

        if ("TRIDENT".equals(projectile.getType().name())) {
            int level = level(shooter, SkillType.TRIDENTES);
            int unlock = configManager.config().getInt("tridentes.arpao-firme.nivel-desbloqueio", 15);
            if (level >= unlock) {
                event.setDamage(event.getDamage() * (1.0 + cappedLevelBonus(level,
                        "tridentes.arpao-firme.bonus-dano-por-nivel", 0.03,
                        "tridentes.arpao-firme.bonus-dano-maximo", 30.0) / 100.0));
            }
            int currentUnlock = configManager.config().getInt("tridentes.correnteza.nivel-desbloqueio", 75);
            if (level >= currentUnlock && (victim.isInWater() || shooter.isInWater())) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        configManager.config().getInt("tridentes.correnteza.duracao-ticks", 60), 0));
            }
            addCombatXp(shooter, SkillType.TRIDENTES, "xp-por-acerto-tridente", 24);
            return;
        }

        SkillType skill = projectileSkills.get(projectile.getUniqueId());
        if (skill == null && projectile.getType().name().contains("ARROW")) skill = SkillType.ARQUERIA;
        if (skill == null) return;

        int level = level(shooter, skill);
        if (skill == SkillType.ARQUERIA) {
            int steadyUnlock = configManager.config().getInt("arqueria.mira-serena.nivel-desbloqueio", 10);
            Location origin = projectileOrigins.get(projectile.getUniqueId());
            if (level >= steadyUnlock && origin != null && origin.getWorld() == victim.getWorld()) {
                double distance = origin.distance(victim.getLocation());
                double minDistance = configManager.config().getDouble("arqueria.mira-serena.distancia-minima", 12.0);
                if (distance > minDistance) {
                    double perBlock = configManager.config().getDouble("arqueria.mira-serena.bonus-por-bloco", 0.005);
                    double maxBonus = configManager.config().getDouble("arqueria.mira-serena.bonus-maximo", 0.35);
                    double bonus = Math.min(maxBonus, (distance - minDistance) * perBlock);
                    event.setDamage(event.getDamage() * (1.0 + bonus));
                }
            }

            int heavyUnlock = configManager.config().getInt("arqueria.flecha-pesada.nivel-desbloqueio", 40);
            if (level >= heavyUnlock && roll(progressiveChance(level,
                    "arqueria.flecha-pesada.chance-por-nivel", 0.02,
                    "arqueria.flecha-pesada.chance-maxima", 20.0))) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        configManager.config().getInt("arqueria.flecha-pesada.duracao-ticks", 50), 0));
            }
            addCombatXp(shooter, SkillType.ARQUERIA, "xp-por-acerto-arco", 20);
        } else {
            int reinforcedUnlock = configManager.config().getInt("bestas.mecanismo-reforcado.nivel-desbloqueio", 10);
            if (level >= reinforcedUnlock) {
                double bonus = cappedLevelBonus(level,
                        "bestas.mecanismo-reforcado.bonus-dano-por-nivel", 0.025,
                        "bestas.mecanismo-reforcado.bonus-dano-maximo", 25.0);
                event.setDamage(event.getDamage() * (1.0 + bonus / 100.0));
            }

            int impactUnlock = configManager.config().getInt("bestas.virote-impacto.nivel-desbloqueio", 50);
            if (level >= impactUnlock && roll(progressiveChance(level,
                    "bestas.virote-impacto.chance-por-nivel", 0.025,
                    "bestas.virote-impacto.chance-maxima", 25.0))) {
                Vector push = projectile.getVelocity().clone().normalize().multiply(
                        configManager.config().getDouble("bestas.virote-impacto.forca-recuo", 0.75));
                push.setY(Math.max(0.2, push.getY()));
                victim.setVelocity(victim.getVelocity().add(push));
            }
            addCombatXp(shooter, SkillType.BESTAS, "xp-por-acerto-besta", 22);
        }

        if (!(projectile instanceof AbstractArrow arrow) || arrow.getPierceLevel() <= 0) {
            projectileSkills.remove(projectile.getUniqueId());
            projectileOrigins.remove(projectile.getUniqueId());
        }
    }

    private void handleSword(EntityDamageByEntityEvent event, Player attacker, LivingEntity victim, boolean countPvp) {
        int level = level(attacker, SkillType.ESPADAS);

        int bleedUnlock = configManager.config().getInt("espadas.corte-profundo.nivel-desbloqueio", 10);
        if (level >= bleedUnlock && roll(progressiveChance(level,
                "espadas.corte-profundo.chance-por-nivel", 0.03,
                "espadas.corte-profundo.chance-maxima", 30.0))) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,
                    configManager.config().getInt("espadas.corte-profundo.duracao-ticks", 60), 0));
        }

        int arcUnlock = configManager.config().getInt("espadas.arco-lamina.nivel-desbloqueio", 40);
        if (level < arcUnlock || !roll(progressiveChance(level,
                "espadas.arco-lamina.chance-por-nivel", 0.02,
                "espadas.arco-lamina.chance-maxima", 20.0))) return;

        double radius = configManager.config().getDouble("espadas.arco-lamina.raio", 2.5);
        double ratio = configManager.config().getDouble("espadas.arco-lamina.fracao-dano", 0.35);
        Set<UUID> hit = new HashSet<>();
        for (Entity nearby : victim.getNearbyEntities(radius, 1.5, radius)) {
            if (!(nearby instanceof LivingEntity target) || target == attacker || target == victim) continue;
            if (target instanceof Player && !countPvp) continue;
            if (!hit.add(target.getUniqueId())) continue;
            dealSyntheticDamage(target, Math.max(0.5, event.getFinalDamage() * ratio), attacker);
        }
    }

    private void tryReverseGuard(EntityDamageByEntityEvent event, Player defender, Player attacker) {
        int level = level(defender, SkillType.ESPADAS);
        int unlock = configManager.config().getInt("espadas.guarda-reversa.nivel-desbloqueio", 100);
        if (level < unlock || !roll(progressiveChance(level,
                "espadas.guarda-reversa.chance-por-nivel", 0.015,
                "espadas.guarda-reversa.chance-maxima", 15.0))) return;

        double reduction = Math.min(0.90, Math.max(0.0,
                configManager.config().getDouble("espadas.guarda-reversa.reducao-dano", 0.40)));
        double reflect = Math.max(0.0,
                configManager.config().getDouble("espadas.guarda-reversa.fracao-refletida", 0.20));
        double original = event.getDamage();
        event.setDamage(original * (1.0 - reduction));
        dealSyntheticDamage(attacker, original * reflect, defender);
    }

    private void handleAxe(EntityDamageByEntityEvent event, Player attacker, LivingEntity victim) {
        int level = level(attacker, SkillType.MACHADOS);

        int impactUnlock = configManager.config().getInt("machados.impacto-brutal.nivel-desbloqueio", 10);
        if (level >= impactUnlock && roll(progressiveChance(level,
                "machados.impacto-brutal.chance-por-nivel", 0.02,
                "machados.impacto-brutal.chance-maxima", 20.0))) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    configManager.config().getInt("machados.impacto-brutal.duracao-ticks", 50), 0));
        }

        int guardUnlock = configManager.config().getInt("machados.fenda-guarda.nivel-desbloqueio", 50);
        if (level >= guardUnlock && victim instanceof Player player && player.isBlocking()) {
            double bonus = configManager.config().getDouble("machados.fenda-guarda.bonus-dano", 0.25);
            event.setDamage(event.getDamage() * (1.0 + Math.max(0.0, bonus)));
        }

        int executeUnlock = configManager.config().getInt("machados.golpe-carrasco.nivel-desbloqueio", 100);
        if (level >= executeUnlock) {
            double threshold = configManager.config().getDouble("machados.golpe-carrasco.limiar-vida", 0.30);
            double maxHealth = victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                    ? victim.getHealth()
                    : victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            if (maxHealth > 0 && victim.getHealth() / maxHealth <= threshold) {
                double bonus = configManager.config().getDouble("machados.golpe-carrasco.bonus-dano", 0.30);
                event.setDamage(event.getDamage() * (1.0 + Math.max(0.0, bonus)));
            }
        }
    }

    private void handleUnarmed(EntityDamageByEntityEvent event, Player attacker, LivingEntity victim) {
        int level = level(attacker, SkillType.DESARMADO);
        int fistUnlock = configManager.config().getInt("desarmado.punho-pedra.nivel-desbloqueio", 10);
        if (level >= fistUnlock) {
            double bonus = cappedLevelBonus(level,
                    "desarmado.punho-pedra.bonus-dano-por-nivel", 0.025,
                    "desarmado.punho-pedra.bonus-dano-maximo", 25.0);
            event.setDamage(event.getDamage() * (1.0 + bonus / 100.0));
        }

        int deflectUnlock = configManager.config().getInt("desarmado.desvio-rapido.nivel-desbloqueio", 75);
        if (level >= deflectUnlock && roll(progressiveChance(level,
                "desarmado.desvio-rapido.chance-por-nivel", 0.02,
                "desarmado.desvio-rapido.chance-maxima", 20.0))) {
            Vector direction = victim.getLocation().toVector().subtract(attacker.getLocation().toVector());
            if (direction.lengthSquared() > 0.001) direction.normalize();
            direction.multiply(configManager.config().getDouble("desarmado.desvio-rapido.forca-recuo", 0.7));
            direction.setY(0.25);
            victim.setVelocity(victim.getVelocity().add(direction));
        }
    }

    private void handleMace(EntityDamageByEntityEvent event, Player attacker, LivingEntity victim, boolean countPvp) {
        int level = level(attacker, SkillType.CLAVA);
        int groundUnlock = configManager.config().getInt("clava.choque-solo.nivel-desbloqueio", 15);
        if (level >= groundUnlock && roll(progressiveChance(level,
                "clava.choque-solo.chance-por-nivel", 0.02,
                "clava.choque-solo.chance-maxima", 20.0))) {
            double radius = configManager.config().getDouble("clava.choque-solo.raio", 3.0);
            double force = configManager.config().getDouble("clava.choque-solo.forca", 0.8);
            for (Entity nearby : victim.getNearbyEntities(radius, 2.0, radius)) {
                if (!(nearby instanceof LivingEntity target) || target == attacker) continue;
                if (target instanceof Player && !countPvp) continue;
                Vector push = target.getLocation().toVector().subtract(victim.getLocation().toVector());
                if (push.lengthSquared() > 0.001) push.normalize();
                push.multiply(force);
                push.setY(0.35);
                target.setVelocity(target.getVelocity().add(push));
            }
        }

        int fallUnlock = configManager.config().getInt("clava.peso-queda.nivel-desbloqueio", 75);
        if (level >= fallUnlock && attacker.getFallDistance() > 1.5f) {
            double perBlock = configManager.config().getDouble("clava.peso-queda.bonus-por-bloco", 0.03);
            double max = configManager.config().getDouble("clava.peso-queda.bonus-maximo", 0.50);
            double bonus = Math.min(max, attacker.getFallDistance() * perBlock);
            event.setDamage(event.getDamage() * (1.0 + bonus));
        }
    }

    private void handleSpear(EntityDamageByEntityEvent event, Player attacker) {
        int level = level(attacker, SkillType.LANCAS);
        int firmUnlock = configManager.config().getInt("lancas.estocada-firme.nivel-desbloqueio", 10);
        if (level >= firmUnlock) {
            double bonus = cappedLevelBonus(level,
                    "lancas.estocada-firme.bonus-dano-por-nivel", 0.025,
                    "lancas.estocada-firme.bonus-dano-maximo", 25.0);
            event.setDamage(event.getDamage() * (1.0 + bonus / 100.0));
        }

        int chargeUnlock = configManager.config().getInt("lancas.passo-investida.nivel-desbloqueio", 75);
        Vector velocity = attacker.getVelocity();
        double horizontal = Math.hypot(velocity.getX(), velocity.getZ());
        double minimum = configManager.config().getDouble("lancas.passo-investida.velocidade-minima", 0.18);
        if (level >= chargeUnlock && horizontal >= minimum) {
            double bonus = configManager.config().getDouble("lancas.passo-investida.bonus-dano", 0.30);
            event.setDamage(event.getDamage() * (1.0 + Math.max(0.0, bonus)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() == null
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || arrow.getPierceLevel() <= 0) {
            projectileSkills.remove(event.getEntity().getUniqueId());
            projectileOrigins.remove(event.getEntity().getUniqueId());
        }
    }

    private void dealSyntheticDamage(LivingEntity target, double damage, Player source) {
        if (damage <= 0 || target.isDead()) return;
        syntheticDamageTargets.add(target.getUniqueId());
        try {
            target.damage(damage, source);
        } finally {
            syntheticDamageTargets.remove(target.getUniqueId());
        }
    }

    private int level(Player player, SkillType skill) {
        return xpManager.getDataManager().getProfile(player.getUniqueId()).getLevel(skill);
    }

    private double progressiveChance(int level, String perLevelPath, double defaultPerLevel,
                                     String maxPath, double defaultMax) {
        return Math.min(
                Math.max(0.0, configManager.config().getDouble(maxPath, defaultMax)),
                level * Math.max(0.0, configManager.config().getDouble(perLevelPath, defaultPerLevel))
        );
    }

    private double cappedLevelBonus(int level, String perLevelPath, double defaultPerLevel,
                                    String maxPath, double defaultMax) {
        return progressiveChance(level, perLevelPath, defaultPerLevel, maxPath, defaultMax);
    }

    private boolean roll(double percent) {
        return percent > 0.0 && ThreadLocalRandom.current().nextDouble(100.0) < percent;
    }

    private void addCombatXp(Player player, SkillType skill, String configKey, double defaultValue) {
        double xp = configManager.config().getDouble("xp.combate." + configKey, defaultValue);
        xpManager.addXp(player, skill, xp);
    }

    private boolean isEspada(Material type) {
        return type.name().endsWith("_SWORD");
    }

    private boolean isMachado(Material type) {
        return type.name().endsWith("_AXE");
    }

    private boolean isLanca(Material type) {
        String name = type.name();
        return name.equals("SPEAR") || name.endsWith("_SPEAR");
    }
}
