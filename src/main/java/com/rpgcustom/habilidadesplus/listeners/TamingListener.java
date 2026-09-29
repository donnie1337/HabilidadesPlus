package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.Material;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TamingListener implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final Map<UUID, Long> wildCallCooldown = new HashMap<>();

    public TamingListener(JavaPlugin plugin, ConfigManager configManager, XpManager xpManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        AnimalTamer owner = event.getOwner();
        if (!(owner instanceof Player player)) return;

        double xp = configManager.config().getDouble("xp.domesticacao.xp-por-domesticar", 250);
        xpManager.addXp(player, SkillType.DOMESTICACAO, xp);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPetDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Tameable pet
                && pet.getOwner() instanceof Player owner) {
            int level = level(owner);
            int unlock = configManager.config().getInt("domesticacao.vinculo-fiel.nivel-desbloqueio", 10);
            if (level >= unlock) {
                double bonus = Math.min(
                        configManager.config().getDouble("domesticacao.vinculo-fiel.bonus-dano-maximo", 30.0),
                        level * configManager.config().getDouble("domesticacao.vinculo-fiel.bonus-dano-por-nivel", 0.03)
                );
                event.setDamage(event.getDamage() * (1.0 + Math.max(0.0, bonus) / 100.0));
            }
        }

        if (event.getEntity() instanceof Tameable pet
                && pet.getOwner() instanceof Player owner) {
            int level = level(owner);
            int unlock = configManager.config().getInt("domesticacao.instinto-protetor.nivel-desbloqueio", 50);
            if (level >= unlock) {
                double reduction = Math.min(
                        configManager.config().getDouble("domesticacao.instinto-protetor.reducao-maxima", 30.0),
                        level * configManager.config().getDouble("domesticacao.instinto-protetor.reducao-por-nivel", 0.03)
                );
                event.setDamage(event.getDamage() * (1.0 - Math.max(0.0, reduction) / 100.0));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWildCall(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking() || player.getInventory().getItemInMainHand().getType() != Material.BONE) return;
        if (!event.getAction().isRightClick()) return;

        int level = level(player);
        int unlock = configManager.config().getInt("domesticacao.chamado-selvagem.nivel-desbloqueio", 100);
        if (level < unlock) return;

        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(1L,
                configManager.config().getLong("domesticacao.chamado-selvagem.recarga-segundos", 60L)) * 1000L;
        long last = wildCallCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownMs) return;

        double radius = Math.max(1.0,
                configManager.config().getDouble("domesticacao.chamado-selvagem.raio", 16.0));
        int duration = Math.max(20,
                configManager.config().getInt("domesticacao.chamado-selvagem.duracao-ticks", 200));
        int affected = 0;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Tameable tameable)
                    || !(entity instanceof LivingEntity living)
                    || !(tameable.getOwner() instanceof Player owner)
                    || !owner.getUniqueId().equals(player.getUniqueId())) continue;

            living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0));
            living.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0));
            affected++;
        }

        if (affected > 0) {
            wildCallCooldown.put(player.getUniqueId(), now);
            player.sendMessage("§aChamado Selvagem fortaleceu " + affected + " companheiro(s).");
        }
    }

    private int level(Player player) {
        return xpManager.getDataManager().getProfile(player.getUniqueId()).getLevel(SkillType.DOMESTICACAO);
    }
}
