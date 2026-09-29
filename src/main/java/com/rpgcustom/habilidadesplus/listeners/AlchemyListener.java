package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import io.papermc.paper.potion.PotionMix;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sistema de Alquimia inspirado no mcMMO.
 *
 * <p>O ultimo jogador que interagiu com o suporte recebe o XP e tem seu nivel
 * usado para Catálise, Concoções e os demais poderes. Suportes automatizados
 * continuam funcionando para receitas vanilla, mas receitas especiais exigem
 * um alquimista identificado para evitar bypass por funis.</p>
 */
public class AlchemyListener implements Listener {
    private static final int DEFAULT_CUSTOM_DURATION_TICKS = 20 * 180;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final Map<Location, UUID> lastBrewer = new HashMap<>();
    private final Map<Material, Formula> formulas = new LinkedHashMap<>();
    private final List<NamespacedKey> potionMixKeys = new ArrayList<>();
    private final NamespacedKey denseAppliedKey;
    private final NamespacedKey potentAppliedKey;

    public AlchemyListener(JavaPlugin plugin, ConfigManager configManager, XpManager xpManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.xpManager = xpManager;
        this.denseAppliedKey = new NamespacedKey(plugin, "alchemy_dense_applied");
        this.potentAppliedKey = new NamespacedKey(plugin, "alchemy_potent_applied");

        registerFormulas();
        registerPotionMixes();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingStandClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.BREWING || top.getLocation() == null) return;
        if (event.getWhoClicked() instanceof Player player) {
            lastBrewer.put(blockLocation(top.getLocation()), player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingStandDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.BREWING || top.getLocation() == null) return;
        if (event.getWhoClicked() instanceof Player player) {
            lastBrewer.put(blockLocation(top.getLocation()), player.getUniqueId());
        }
    }

    /**
     * Catálise usa a API nativa do Paper para reduzir os ticks restantes da
     * fermentação. Não cria tarefas repetitivas nem acelera o tick do servidor.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBrewingStart(BrewingStartEvent event) {
        Player player = brewerAt(event.getBlock().getLocation());
        if (player == null || !configManager.habilidadeAtiva(player)) return;

        int level = getAlchemyLevel(player);
        int unlock = configManager.config().getInt("alquimia.catalise.nivel-desbloqueio", 1);
        if (level < unlock) return;

        double minSpeed = Math.max(1.0,
                configManager.config().getDouble("alquimia.catalise.multiplicador-minimo", 1.0));
        double maxSpeed = Math.max(minSpeed,
                configManager.config().getDouble("alquimia.catalise.multiplicador-maximo", 4.0));
        int maxBonusLevel = Math.max(unlock,
                configManager.config().getInt("alquimia.catalise.nivel-maximo-bonus", 1000));

        double progress = maxBonusLevel == unlock
                ? 1.0
                : Math.min(1.0, Math.max(0.0, (level - unlock) / (double) (maxBonusLevel - unlock)));
        double speed = minSpeed + (maxSpeed - minSpeed) * progress;

        int originalTicks = event.getBrewingTime();
        event.setBrewingTime(Math.max(1, (int) Math.round(originalTicks / speed)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        Location location = blockLocation(event.getBlock().getLocation());
        UUID brewerId = lastBrewer.remove(location);
        Player player = brewerId == null ? null : Bukkit.getPlayer(brewerId);

        BrewerInventory contents = event.getContents();
        ItemStack ingredient = contents.getIngredient();
        Formula formula = ingredient == null ? null : formulas.get(ingredient.getType());

        // Receitas especiais são globais no brewer do Paper, portanto a
        // autorização por nível precisa ser aplicada na finalização do brew.
        if (formula != null) {
            if (player == null || !player.isOnline() || !configManager.habilidadeAtiva(player)) {
                event.setCancelled(true);
                return;
            }
            int level = getAlchemyLevel(player);
            int required = formula.requiredLevel(configManager);
            if (level < required) {
                event.setCancelled(true);
                player.sendMessage("§cVocê precisa de Alquimia nível " + required
                        + " para usar esta fórmula.");
                return;
            }
        }

        if (player == null || !player.isOnline() || !configManager.habilidadeAtiva(player)) return;
        int level = getAlchemyLevel(player);

        applyEssenciaDensa(event.getResults(), level);
        applyDestilacaoPotente(event.getResults(), level);
        tryPreserveIngredient(contents, ingredient, level);

        int potions = 0;
        for (ItemStack result : event.getResults()) {
            if (result != null && isPotion(result.getType())) {
                potions += Math.max(1, result.getAmount());
            }
        }
        if (potions <= 0) return;

        double xpPerPotion = configManager.config().getDouble(
                "xp.alquimia.xp-por-pocao-fermentada", 150);
        xpManager.addXp(player, SkillType.ALQUIMIA, xpPerPotion * potions);
    }

    /**
     * Remove somente as receitas registradas pelo HabilidadesPlus.
     */
    public void shutdown() {
        for (NamespacedKey key : potionMixKeys) {
            Bukkit.getPotionBrewer().removePotionMix(key);
        }
        potionMixKeys.clear();
        lastBrewer.clear();
    }

    private void registerFormulas() {
        // Concoções I - nível 25
        formula(Material.CARROT, PotionEffectType.HASTE, 0, "concoctions-i", "haste");
        formula(Material.QUARTZ, PotionEffectType.ABSORPTION, 0, "concoctions-i", "absorcao");
        formula(Material.SLIME_BALL, PotionEffectType.MINING_FATIGUE, 0, "concoctions-i", "fadiga");

        // Concoções II - nível 75
        formula(Material.BROWN_MUSHROOM, PotionEffectType.NAUSEA, 0, "concoctions-ii", "nausea");
        formula(Material.INK_SAC, PotionEffectType.BLINDNESS, 0, "concoctions-ii", "cegueira");
        formula(Material.APPLE, PotionEffectType.HEALTH_BOOST, 0, "concoctions-ii", "vida");
        formula(Material.ROTTEN_FLESH, PotionEffectType.HUNGER, 0, "concoctions-ii", "fome");

        // Mestre Alquimista - nível 150
        formula(Material.GOLDEN_APPLE, PotionEffectType.RESISTANCE, 0, "mestre-alquimista", "resistencia");
        formula(Material.FERN, PotionEffectType.SATURATION, 0, "mestre-alquimista", "saturacao");
        formula(Material.POISONOUS_POTATO, PotionEffectType.WITHER, 0, "mestre-alquimista", "decadencia");
    }

    private void formula(Material ingredient, PotionEffectType effect, int amplifier,
                         String powerPath, String keySuffix) {
        formulas.put(ingredient, new Formula(ingredient, effect, amplifier, powerPath, keySuffix));
    }

    private void registerPotionMixes() {
        for (Formula formula : formulas.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "alchemy_" + formula.keySuffix());

            // Evita duplicação em /reload ou em reinicializações incompletas.
            Bukkit.getPotionBrewer().removePotionMix(key);

            ItemStack result = customPotion(formula.effect(), DEFAULT_CUSTOM_DURATION_TICKS,
                    formula.amplifier());
            RecipeChoice input = PotionMix.createPredicateChoice(this::isAwkwardPotion);
            RecipeChoice ingredient = new RecipeChoice.MaterialChoice(formula.ingredient());

            Bukkit.getPotionBrewer().addPotionMix(new PotionMix(key, result, input, ingredient));
            potionMixKeys.add(key);
        }
    }

    private ItemStack customPotion(PotionEffectType effect, int duration, int amplifier) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(PotionType.AWKWARD);
        meta.addCustomEffect(new PotionEffect(effect, duration, amplifier), true);
        potion.setItemMeta(meta);
        return potion;
    }

    private boolean isAwkwardPotion(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        return meta.getBasePotionType() == PotionType.AWKWARD;
    }

    private void tryPreserveIngredient(BrewerInventory inventory, ItemStack ingredient, int level) {
        if (ingredient == null || ingredient.getType().isAir()) return;

        int unlock = configManager.config().getInt(
                "alquimia.mistura-estavel.nivel-desbloqueio", 10);
        if (level < unlock) return;

        double chancePerLevel = Math.max(0.0, configManager.config().getDouble(
                "alquimia.mistura-estavel.chance-por-nivel", 0.02));
        double maxChance = Math.max(0.0, configManager.config().getDouble(
                "alquimia.mistura-estavel.chance-maxima", 20.0));
        double chance = Math.min(maxChance, level * chancePerLevel);

        if (ThreadLocalRandom.current().nextDouble(100.0) >= chance) return;

        ItemStack preserved = ingredient.clone();
        preserved.setAmount(1);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack current = inventory.getIngredient();
            if (current == null || current.getType().isAir()) {
                inventory.setIngredient(preserved);
                return;
            }
            if (current.isSimilar(preserved) && current.getAmount() < current.getMaxStackSize()) {
                current.setAmount(current.getAmount() + 1);
                inventory.setIngredient(current);
            }
        });
    }

    private void applyEssenciaDensa(List<ItemStack> results, int level) {
        int unlock = configManager.config().getInt(
                "alquimia.essencia-densa.nivel-desbloqueio", 50);
        if (level < unlock) return;

        double bonusPerLevel = Math.max(0.0, configManager.config().getDouble(
                "alquimia.essencia-densa.bonus-duracao-por-nivel", 0.05));
        double maxBonus = Math.max(0.0, configManager.config().getDouble(
                "alquimia.essencia-densa.bonus-duracao-maximo", 50.0));
        double bonus = Math.min(maxBonus, level * bonusPerLevel) / 100.0;
        if (bonus <= 0.0) return;

        for (ItemStack result : results) {
            if (!isPotionStack(result) || !(result.getItemMeta() instanceof PotionMeta meta)) continue;
            if (meta.getPersistentDataContainer().has(denseAppliedKey, PersistentDataType.BYTE)) continue;

            boolean changed = false;
            for (PotionEffect effect : potionEffects(meta)) {
                if (effect.getType().isInstant() || effect.getDuration() <= 0) continue;
                int duration = Math.max(1, (int) Math.round(effect.getDuration() * (1.0 + bonus)));
                meta.addCustomEffect(effect.withDuration(duration), true);
                changed = true;
            }

            if (changed) {
                meta.getPersistentDataContainer().set(denseAppliedKey, PersistentDataType.BYTE, (byte) 1);
                result.setItemMeta(meta);
            }
        }
    }

    private void applyDestilacaoPotente(List<ItemStack> results, int level) {
        int unlock = configManager.config().getInt(
                "alquimia.destilacao-potente.nivel-desbloqueio", 100);
        if (level < unlock) return;

        double chancePerLevel = Math.max(0.0, configManager.config().getDouble(
                "alquimia.destilacao-potente.chance-por-nivel", 0.02));
        double maxChance = Math.max(0.0, configManager.config().getDouble(
                "alquimia.destilacao-potente.chance-maxima", 20.0));
        int maxAmplifier = Math.max(0, configManager.config().getInt(
                "alquimia.destilacao-potente.amplificador-maximo", 1));
        double chance = Math.min(maxChance, level * chancePerLevel);

        for (ItemStack result : results) {
            if (!isPotionStack(result) || !(result.getItemMeta() instanceof PotionMeta meta)) continue;
            if (meta.getPersistentDataContainer().has(potentAppliedKey, PersistentDataType.BYTE)) continue;
            if (ThreadLocalRandom.current().nextDouble(100.0) >= chance) continue;

            boolean changed = false;
            for (PotionEffect effect : potionEffects(meta)) {
                if (effect.getAmplifier() >= maxAmplifier) continue;
                meta.addCustomEffect(effect.withAmplifier(effect.getAmplifier() + 1), true);
                changed = true;
            }

            if (changed) {
                meta.getPersistentDataContainer().set(potentAppliedKey, PersistentDataType.BYTE, (byte) 1);
                result.setItemMeta(meta);
            }
        }
    }

    private List<PotionEffect> potionEffects(PotionMeta meta) {
        Map<PotionEffectType, PotionEffect> effects = new LinkedHashMap<>();

        PotionType baseType = meta.getBasePotionType();
        if (baseType != null) {
            for (PotionEffect effect : baseType.getPotionEffects()) {
                effects.put(effect.getType(), effect);
            }
        }
        for (PotionEffect effect : meta.getCustomEffects()) {
            effects.put(effect.getType(), effect);
        }

        return List.copyOf(effects.values());
    }

    private int getAlchemyLevel(Player player) {
        return xpManager.getDataManager().getProfile(player.getUniqueId()).getLevel(SkillType.ALQUIMIA);
    }

    private Player brewerAt(Location location) {
        UUID uuid = lastBrewer.get(blockLocation(location));
        if (uuid == null) return null;
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline() ? player : null;
    }

    private boolean isPotionStack(ItemStack item) {
        return item != null && isPotion(item.getType());
    }

    private boolean isPotion(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    private Location blockLocation(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private record Formula(Material ingredient, PotionEffectType effect, int amplifier,
                           String powerPath, String keySuffix) {
        int requiredLevel(ConfigManager configManager) {
            return configManager.config().getInt(
                    "alquimia." + powerPath + ".nivel-desbloqueio",
                    switch (powerPath) {
                        case "concoctions-i" -> 25;
                        case "concoctions-ii" -> 75;
                        default -> 150;
                    });
        }
    }
}
