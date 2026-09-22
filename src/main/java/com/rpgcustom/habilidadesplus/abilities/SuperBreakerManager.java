package com.rpgcustom.habilidadesplus.abilities;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
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

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, Long> activeUntil = new HashMap<>();

    public SuperBreakerManager(JavaPlugin plugin, ConfigManager configManager, DataManager dataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
    }

    @EventHandler(ignoreCancelled = true)
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
        if (activeTasks.containsKey(uuid)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < cooldownUntil.getOrDefault(uuid, 0L)) {
            return;
        }

        int level = dataManager.getProfile(uuid).getLevel(SkillType.MINERACAO);
        if (level <= 0) {
            return;
        }

        long durationTicks = calculateDurationTicks(level);
        long cooldownTicks = Math.max(0L, configManager.config()
                .getLong("mineracao.superbreaker.delay-ativacao-segundos", 30L)) * 20L;

        activeUntil.put(uuid, now + durationTicks * 50L);
        cooldownUntil.put(uuid, now + cooldownTicks * 50L);

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            activeTasks.remove(uuid);
            activeUntil.remove(uuid);
        }, durationTicks);
        activeTasks.put(uuid, task);

        String mensagem = MessageUtil.placeholders(
                configManager.msg("mineracao.superbreaker-ativado"),
                Map.of(
                        "nivel", String.valueOf(level),
                        "duracao", formatSeconds(durationTicks)
                )
        );
        player.sendMessage(MessageUtil.colorize(mensagem));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!activeTasks.containsKey(uuid)) {
            return;
        }

        if (System.currentTimeMillis() >= activeUntil.getOrDefault(uuid, 0L)) {
            stop(uuid);
            return;
        }

        if (!isMiningBlock(event.getBlock())) {
            return;
        }

        event.setInstaBreak(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stop(event.getPlayer().getUniqueId());
    }

    public void stopAll() {
        for (UUID uuid : new HashSet<>(activeTasks.keySet())) {
            stop(uuid);
        }
        cooldownUntil.clear();
    }

    private void stop(UUID uuid) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        activeUntil.remove(uuid);
    }

    private long calculateDurationTicks(int level) {
        double base = Math.max(0.1, configManager.config().getDouble(
                "mineracao.superbreaker.duracao-base-segundos", 3.0));
        double perLevel = Math.max(0.0, configManager.config().getDouble(
                "mineracao.superbreaker.duracao-por-nivel", 0.02));
        double max = Math.max(base, configManager.config().getDouble(
                "mineracao.superbreaker.duracao-maxima-segundos", 15.0));

        double seconds = Math.min(max, base + (level * perLevel));
        return Math.max(1L, Math.round(seconds * 20.0));
    }

    private String formatSeconds(long ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
    }

    private boolean isMiningBlock(Block block) {
        Material material = block.getType();
        return material.isSolid() && material != Material.BEDROCK && material != Material.BARRIER;
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
