package com.rpgcustom.habilidadesplus.abilities;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class SuperBreakerManager implements Listener {

    private static final double EFICIENCIA_BONUS = 20.0;
    private static final NamespacedKey EFICIENCIA_KEY =
            new NamespacedKey("habilidadesplus", "super_quebrador_eficiencia");

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Map<UUID, Long> cooldownMessageUntil = new HashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, Long> activeUntil = new HashMap<>();
    private final Map<UUID, AttributeModifier> efficiencyModifiers = new HashMap<>();

    public SuperBreakerManager(JavaPlugin plugin, ConfigManager configManager, DataManager dataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("habilidadesplus.use")) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isPickaxe(item.getType())) {
            return;
        }

        UUID uuid = player.getUniqueId();
        int level = dataManager.getProfile(uuid).getLevel(SkillType.MINERACAO);

        int nivelDesbloqueio = configManager.config().getInt("mineracao.superbreaker.nivel-desbloqueio", 10);
        if (level < nivelDesbloqueio) {
            return;
        }

        if (activeTasks.containsKey(uuid)) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownEnd = cooldownUntil.getOrDefault(uuid, 0L);
        if (now < cooldownEnd) {
            long remainingSeconds = (long) Math.ceil((cooldownEnd - now) / 1000.0);
            long nextMessage = cooldownMessageUntil.getOrDefault(uuid, 0L);
            if (now >= nextMessage) {
                sendCooldownMessage(player, remainingSeconds);
                cooldownMessageUntil.put(uuid, now + 1000L);
            }
            return;
        }

        long durationTicks = calculateDurationTicks(level);
        long cooldownTicks = calculateCooldownTicks(level);

        activeUntil.put(uuid, now + durationTicks * 50L);
        cooldownUntil.put(uuid, now + cooldownTicks * 50L);
        applyEfficiencyBonus(player);

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            removeEfficiencyBonus(player);
            activeTasks.remove(uuid);
            activeUntil.remove(uuid);
            sendEndedMessage(player);
        }, durationTicks);
        activeTasks.put(uuid, task);

        String mensagem = MessageUtil.placeholders(
                configManager.msg("mineracao.superbreaker-ativado"),
                Map.of(
                        "nivel", String.valueOf(level),
                        "duracao", formatSeconds(durationTicks),
                        "recarga", formatSeconds(cooldownTicks)
                )
        );
        player.sendMessage(MessageUtil.colorize(mensagem));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        stop(uuid);
        cooldownMessageUntil.remove(uuid);
    }

    public boolean isActive(UUID uuid) {
        return activeTasks.containsKey(uuid);
    }

    public void stopAll() {
        for (UUID uuid : new HashSet<>(activeTasks.keySet())) {
            stop(uuid);
        }
        cooldownUntil.clear();
        cooldownMessageUntil.clear();
    }

    private void stop(UUID uuid) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            removeEfficiencyBonus(player);
        } else {
            efficiencyModifiers.remove(uuid);
        }

        activeUntil.remove(uuid);
    }

    private void applyEfficiencyBonus(Player player) {
        removeEfficiencyBonus(player);

        AttributeInstance attribute = player.getAttribute(Attribute.MINING_EFFICIENCY);
        if (attribute != null) {
            AttributeModifier modifier = new AttributeModifier(
                    EFICIENCIA_KEY,
                    EFICIENCIA_BONUS,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            attribute.addTransientModifier(modifier);
            efficiencyModifiers.put(player.getUniqueId(), modifier);
        }
    }

    private void removeEfficiencyBonus(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MINING_EFFICIENCY);
        AttributeModifier modifier = efficiencyModifiers.remove(player.getUniqueId());
        if (attribute != null) {
            if (modifier != null) {
                attribute.removeModifier(modifier);
            } else {
                AttributeModifier existing = attribute.getModifier(EFICIENCIA_KEY);
                if (existing != null) {
                    attribute.removeModifier(existing);
                }
            }
        }
    }

    private long calculateDurationTicks(int level) {
        double seconds = interpolate(level,
                new int[]{10, 50, 100, 150, 200, 250, 500, 1000},
                new double[]{
                        configManager.config().getDouble("mineracao.superbreaker.duracao-nivel-10", 5.0),
                        configManager.config().getDouble("mineracao.superbreaker.duracao-nivel-50", 6.0),
                        configManager.config().getDouble("mineracao.superbreaker.duracao-nivel-100", 7.0),
                        configManager.config().getDouble("mineracao.superbreaker.duracao-nivel-150", 8.0),
                        configManager.config().getDouble("mineracao.superbreaker.duracao-nivel-200", 9.0),
                        configManager.config().getDouble("mineracao.superbreaker.duracao-nivel-250", 10.0),
                        configManager.config().getDouble("mineracao.superbreaker.duracao-nivel-500", 15.0),
                        configManager.config().getDouble("mineracao.superbreaker.duracao-nivel-1000", 20.0)
                }
        );
        return Math.max(1L, Math.round(seconds * 20.0));
    }

    private long calculateCooldownTicks(int level) {
        double seconds = interpolateCooldown(level);
        return Math.max(1L, Math.round(seconds * 20.0));
    }

    private double interpolateCooldown(int level) {
        if (level <= 10) {
            return configManager.config().getDouble("mineracao.superbreaker.recarga-nivel-10", 120.0);
        }

        if (level <= 50) {
            return interpolate(level, new int[]{10, 50}, new double[]{
                    configManager.config().getDouble("mineracao.superbreaker.recarga-nivel-10", 120.0),
                    configManager.config().getDouble("mineracao.superbreaker.recarga-nivel-50", 140.0)
            });
        }

        int lowerLevel = ((level - 50) / 50) * 50 + 50;
        int upperLevel = Math.min(1000, lowerLevel + 50);
        if (lowerLevel >= 1000) {
            double baseCooldown = configManager.config().getDouble("mineracao.superbreaker.recarga-nivel-50", 140.0);
            double increment = configManager.config().getDouble("mineracao.superbreaker.recarga-incremento-50-niveis", 20.0);
            return baseCooldown + ((1000 - 50) / 50) * increment;
        }

        double baseCooldown = configManager.config().getDouble("mineracao.superbreaker.recarga-nivel-50", 140.0);
        double increment = configManager.config().getDouble("mineracao.superbreaker.recarga-incremento-50-niveis", 20.0);
        double lowerCooldown = baseCooldown + ((lowerLevel - 50) / 50) * increment;
        double upperCooldown = lowerCooldown + increment;
        return interpolate(level, new int[]{lowerLevel, upperLevel},
                new double[]{lowerCooldown, upperCooldown});
    }

    private double interpolate(int level, int[] levels, double[] values) {
        if (level <= levels[0]) {
            return values[0];
        }

        for (int i = 1; i < levels.length; i++) {
            if (level <= levels[i]) {
                double ratio = (level - levels[i - 1]) / (double) (levels[i] - levels[i - 1]);
                return values[i - 1] + ((values[i] - values[i - 1]) * ratio);
            }
        }

        return values[values.length - 1];
    }

    private String formatSeconds(long ticks) {
        double seconds = ticks / 20.0;
        if (seconds == Math.rint(seconds)) {
            return String.valueOf((long) seconds);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }

    private void sendEndedMessage(Player player) {
        String mensagem = MessageUtil.colorize(configManager.msg("mineracao.superbreaker-finalizado"));
        player.sendMessage(mensagem);
    }

    private void sendCooldownMessage(Player player, long remainingSeconds) {
        String mensagem = MessageUtil.placeholders(
                configManager.msg("mineracao.superbreaker-em-recarga"),
                Map.of("segundos", String.valueOf(remainingSeconds))
        );
        player.sendMessage(MessageUtil.colorize(mensagem));
    }

    private boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE
                || material == Material.STONE_PICKAXE
                || material == Material.IRON_PICKAXE
                || material == Material.GOLDEN_PICKAXE
                || material == Material.DIAMOND_PICKAXE
                || material == Material.NETHERITE_PICKAXE;
    }
}