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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SuperEscavadorManager implements Listener {

    private static final double EFICIENCIA_BONUS = 20.0;
    private static final NamespacedKey EFICIENCIA_KEY =
            new NamespacedKey("habilidadesplus", "super_escavador_eficiencia");

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Map<UUID, Long> cooldownMessageUntil = new HashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, AttributeModifier> efficiencyModifiers = new HashMap<>();

    public SuperEscavadorManager(JavaPlugin plugin, ConfigManager configManager, DataManager dataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!configManager.habilidadeAtiva(player)) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null
                && event.getClickedBlock().getType().isInteractable()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isCorrectTool(item.getType())) return;

        UUID uuid = player.getUniqueId();
        int level = dataManager.getProfile(uuid).getLevel(SkillType.ESCAVACAO);
        int unlock = configManager.config().getInt("escavacao.giga-broca.nivel-desbloqueio", 25);
        if (level < unlock) return;
        if (activeTasks.containsKey(uuid)) return;

        long now = System.currentTimeMillis();
        long cooldownEnd = cooldownUntil.getOrDefault(uuid, 0L);
        if (now < cooldownEnd) {
            long remainingSeconds = (long) Math.ceil((cooldownEnd - now) / 1000.0);
            long nextMessage = cooldownMessageUntil.getOrDefault(uuid, 0L);
            if (now >= nextMessage) {
                player.sendActionBar(MessageUtil.colorize("&cGiga Broca em recarga: &e" + remainingSeconds + "s"));
                cooldownMessageUntil.put(uuid, now + 1000L);
            }
            return;
        }
        cooldownMessageUntil.remove(uuid);

        long durationTicks = calculateDurationTicks(level);
        long cooldownTicks = calculateCooldownTicks(level);
        cooldownUntil.put(uuid, now + cooldownTicks * 50L);

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            removeEfficiencyBonus(player);
            activeTasks.remove(uuid);
            if (player.isOnline()) {
                player.sendMessage(MessageUtil.colorize(
                        configManager.msg("escavacao.super-escavacao-finalizada")
                ));
            }
        }, durationTicks);
        activeTasks.put(uuid, task);

        String mensagem = MessageUtil.placeholders(
                configManager.msg("escavacao.super-escavacao-ativada"),
                Map.of(
                        "nivel", String.valueOf(level),
                        "duracao", formatSeconds(durationTicks),
                        "recarga", formatSeconds(cooldownTicks)
                )
        );
        player.sendMessage(MessageUtil.colorize(mensagem));
        player.sendActionBar(MessageUtil.colorize("&6&lSUPER ESCAVAÇÃO! &f• &eVelocidade aumentada"));
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!configManager.habilidadeAtiva(player)
                || !isActive(uuid)
                || !isCorrectTool(item.getType())
                || !event.getBlock().isPreferredTool(item)) {
            return;
        }

        applyEfficiencyBonus(player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isActive(player.getUniqueId()) && isCorrectTool(player.getInventory().getItemInMainHand().getType())) {
            applyEfficiencyBonus(player);
        }
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Player player = event.getPlayer();
        if (isActive(player.getUniqueId()) && isCorrectTool(player.getInventory().getItemInMainHand().getType())) {
            applyEfficiencyBonus(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        stop(uuid);
        cooldownUntil.remove(uuid);
        cooldownMessageUntil.remove(uuid);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!configManager.habilidadeAtiva(event.getPlayer())) {
            removeEfficiencyBonus(event.getPlayer());
        }
    }

    public boolean isActive(UUID uuid) {
        return activeTasks.containsKey(uuid);
    }

    public void stopAll() {
        for (UUID uuid : new HashSet<>(activeTasks.keySet())) stop(uuid);
        cooldownUntil.clear();
        cooldownMessageUntil.clear();
    }

    private void stop(UUID uuid) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) task.cancel();

        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            removeEfficiencyBonus(player);
        } else {
            efficiencyModifiers.remove(uuid);
        }
    }

    private void applyEfficiencyBonus(Player player) {
        removeEfficiencyBonus(player);
        AttributeInstance attribute = player.getAttribute(Attribute.MINING_EFFICIENCY);
        if (attribute == null) return;

        AttributeModifier modifier = new AttributeModifier(
                EFICIENCIA_KEY,
                EFICIENCIA_BONUS,
                AttributeModifier.Operation.ADD_NUMBER
        );
        attribute.addTransientModifier(modifier);
        efficiencyModifiers.put(player.getUniqueId(), modifier);
    }

    private void removeEfficiencyBonus(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MINING_EFFICIENCY);
        AttributeModifier modifier = efficiencyModifiers.remove(player.getUniqueId());
        if (attribute == null) return;

        if (modifier != null) {
            attribute.removeModifier(modifier);
        } else {
            AttributeModifier existing = attribute.getModifier(EFICIENCIA_KEY);
            if (existing != null) attribute.removeModifier(existing);
        }
    }

    private String formatSeconds(long ticks) {
        double seconds = ticks / 20.0;
        if (seconds == Math.rint(seconds)) {
            return String.valueOf((long) seconds);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }

    private long calculateDurationTicks(int level) {
        double base = configManager.config().getDouble("escavacao.giga-broca.duracao-nivel-25", 5.0);
        double perLevel = configManager.config().getDouble("escavacao.giga-broca.duracao-por-nivel", 0.01);
        double max = configManager.config().getDouble("escavacao.giga-broca.duracao-maxima", 20.0);
        double seconds = Math.min(max, base + Math.max(0, level - 25) * perLevel);
        return Math.max(1L, Math.round(seconds * 20.0));
    }

    private long calculateCooldownTicks(int level) {
        double seconds = configManager.config().getDouble("escavacao.giga-broca.recarga-segundos", 120.0);
        return Math.max(1L, Math.round(seconds * 20.0));
    }

    private boolean isCorrectTool(Material material) {
        return material == Material.WOODEN_SHOVEL
                || material == Material.STONE_SHOVEL
                || material == Material.IRON_SHOVEL
                || material == Material.GOLDEN_SHOVEL
                || material == Material.DIAMOND_SHOVEL
                || material == Material.NETHERITE_SHOVEL;
    }
}
