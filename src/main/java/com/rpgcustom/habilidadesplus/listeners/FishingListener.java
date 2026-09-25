package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class FishingListener implements Listener {

    private static final int ISCA_DE_SORTE_UNLOCK = 5;
    private static final int LINHA_FIRME_UNLOCK = 30;
    private static final int MARE_GENEROSA_UNLOCK = 75;
    private static final Set<Material> TREASURE_ITEMS = Set.of(
            Material.NAUTILUS_SHELL,
            Material.NAME_TAG,
            Material.SADDLE,
            Material.ENCHANTED_BOOK,
            Material.BOW,
            Material.FISHING_ROD,
            Material.DIAMOND,
            Material.EMERALD,
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.EXPERIENCE_BOTTLE,
            Material.HEART_OF_THE_SEA
    );
    private static final Material[] LUCKY_ARMOR = {
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET
    };
    private static final List<Enchantment> ARMOR_ENCHANTMENTS = List.of(
            Enchantment.PROTECTION_ENVIRONMENTAL,
            Enchantment.PROTECTION_FIRE,
            Enchantment.PROTECTION_PROJECTILE,
            Enchantment.PROTECTION_EXPLOSIONS,
            Enchantment.THORNS,
            Enchantment.UNBREAKING,
            Enchantment.MENDING
    );

    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final Random random = new Random();

    public FishingListener(ConfigManager configManager, XpManager xpManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        int level = getFishingLevel(player);
        if (event.getCaught() instanceof Item caught) {
            applyLuckyBait(caught, level);
            applyGenerousTide(caught, level);
        }

        double xp = configManager.config().getDouble("xp.pesca.xp-por-pesca", 100);
        xpManager.addXp(player, SkillType.PESCA, xp);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFishingRodDamage(PlayerItemDamageEvent event) {
        if (event.getItem().getType() != Material.FISHING_ROD) return;

        Player player = event.getPlayer();
        if (!configManager.habilidadeAtiva(player)) return;

        int level = getFishingLevel(player);
        double chance = fishingChance(
                level,
                LINHA_FIRME_UNLOCK,
                "pesca.linha-firme.chance-preservar-por-nivel",
                "pesca.linha-firme.chance-maxima",
                0.05,
                50.0
        );
        if (chance > 0.0 && random.nextDouble() * 100.0 < chance) {
            event.setCancelled(true);
        }
    }

    private void applyLuckyBait(Item caught, int level) {
        double chance = fishingChance(
                level,
                ISCA_DE_SORTE_UNLOCK,
                "pesca.isca-de-sorte.chance-tesouro-por-nivel",
                "pesca.isca-de-sorte.chance-maxima",
                0.05,
                15.0
        );
        if (chance <= 0.0 || random.nextDouble() * 100.0 >= chance) return;
        if (TREASURE_ITEMS.contains(caught.getItemStack().getType())) return;

        caught.setItemStack(randomLuckyTreasure());
    }

    private ItemStack randomLuckyTreasure() {
        // A Isca de Sorte pode encontrar uma armadura encantada ou outros
        // tesouros raros, sem alterar a quantidade natural dos drops comuns.
        if (random.nextInt(100) < 35) {
            return randomEnchantedArmor();
        }

        Material[] otherTreasures = {
                Material.NAUTILUS_SHELL,
                Material.NAME_TAG,
                Material.SADDLE,
                Material.ENCHANTED_BOOK,
                Material.BOW,
                Material.FISHING_ROD,
                Material.DIAMOND,
                Material.EMERALD,
                Material.GOLD_INGOT,
                Material.IRON_INGOT,
                Material.EXPERIENCE_BOTTLE,
                Material.HEART_OF_THE_SEA
        };
        Material material = otherTreasures[random.nextInt(otherTreasures.length)];
        int amount = switch (material) {
            case DIAMOND, EMERALD, GOLD_INGOT, IRON_INGOT, EXPERIENCE_BOTTLE -> 1 + random.nextInt(3);
            default -> 1;
        };
        return new ItemStack(material, amount);
    }

    private ItemStack randomEnchantedArmor() {
        ItemStack armor = new ItemStack(LUCKY_ARMOR[random.nextInt(LUCKY_ARMOR.length)]);
        Set<Enchantment> selected = new java.util.HashSet<>();
        int enchantmentCount = 1 + random.nextInt(3);
        while (selected.size() < enchantmentCount) {
            selected.add(ARMOR_ENCHANTMENTS.get(random.nextInt(ARMOR_ENCHANTMENTS.size())));
        }
        for (Enchantment enchantment : selected) {
            int level = 1 + random.nextInt(Math.max(1, Math.min(3, enchantment.getMaxLevel())));
            armor.addUnsafeEnchantment(enchantment, level);
        }
        return armor;
    }

    private void applyGenerousTide(Item caught, int level) {
        double chance = fishingChance(
                level,
                MARE_GENEROSA_UNLOCK,
                "pesca.mare-generosa.chance-captura-extra-por-nivel",
                "pesca.mare-generosa.chance-maxima",
                0.05,
                25.0
        );
        if (chance <= 0.0 || random.nextDouble() * 100.0 >= chance) return;

        ItemStack stack = caught.getItemStack();
        stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() * 2));
        caught.setItemStack(stack);
    }

    private double fishingChance(int level, int unlock, String perLevelPath, String maxPath,
                                 double defaultPerLevel, double defaultMax) {
        if (level < unlock) return 0.0;
        double perLevel = Math.max(0.0, configManager.config().getDouble(perLevelPath, defaultPerLevel));
        double max = Math.max(0.0, configManager.config().getDouble(maxPath, defaultMax));
        return Math.min(max, level * perLevel);
    }

    private int getFishingLevel(Player player) {
        return xpManager.getDataManager().getProfile(player.getUniqueId()).getLevel(SkillType.PESCA);
    }
}
