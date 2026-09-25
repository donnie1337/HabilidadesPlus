package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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
    /*
     * Resolve by the vanilla namespaced keys at runtime. This keeps the plugin
     * compatible with Paper versions where the old Bukkit constant names changed.
     */
    private static final List<String> ARMOR_ENCHANTMENT_KEYS = List.of(
            "protection",
            "fire_protection",
            "projectile_protection",
            "blast_protection",
            "thorns",
            "unbreaking",
            "mending"
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
            applyLuckyBait(caught, level, event.getHook().getLocation());
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

    private void applyLuckyBait(Item caught, int level, Location hookLocation) {
        if (level < ISCA_DE_SORTE_UNLOCK) return;

        // A Isca de Sorte mantém 80% de captura normal e 20% de tesouro.
        double treasureChance = Math.max(0.0, Math.min(20.0,
                configManager.config().getDouble("pesca.isca-de-sorte.chance-tesouro", 20.0)));
        if (random.nextDouble() * 100.0 >= treasureChance) return;

        // Tesouros só aparecem em uma área aberta com mais de 10x10 blocos
        // de água. Piscinas/farms pequenas continuam pescando normalmente.
        if (!hasLargeFishingArea(hookLocation)) return;
        if (TREASURE_ITEMS.contains(caught.getItemStack().getType())) return;

        caught.setItemStack(randomLuckyTreasure(level));
    }

    private boolean hasLargeFishingArea(Location hookLocation) {
        if (hookLocation == null || hookLocation.getWorld() == null) return false;

        int centerX = hookLocation.getBlockX();
        int centerY = hookLocation.getBlockY();
        int centerZ = hookLocation.getBlockZ();
        int waterBlocks = 0;

        // Área mínima: 6x6 na horizontal e 2 blocos de profundidade.
        // O retângulo contém 72 posições e precisa estar totalmente preenchido.
        for (int x = centerX - 3; x <= centerX + 2; x++) {
            for (int z = centerZ - 3; z <= centerZ + 2; z++) {
                for (int y = centerY - 1; y <= centerY; y++) {
                    if (hookLocation.getWorld().getBlockAt(x, y, z).getType() == Material.WATER) {
                        waterBlocks++;
                    }
                }
            }
        }
        return waterBlocks >= 72;
    }

    private ItemStack randomLuckyTreasure(int level) {
        // A qualidade da recompensa acompanha o nível de Pesca:
        // 35% armadura, 35% minério e 30% outros tesouros.
        int roll = random.nextInt(100);
        if (roll < 35) return randomEnchantedArmor(level);
        if (roll < 70) return randomOreTreasure(level);
        return randomOtherTreasure(level);
    }

    private ItemStack randomEnchantedArmor(int level) {
        Material[] armorSet;
        if (level >= 500) {
            armorSet = new Material[]{
                    Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                    Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
            };
        } else if (level >= 250) {
            armorSet = new Material[]{
                    Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS
            };
        } else if (level >= 100) {
            armorSet = new Material[]{
                    Material.IRON_HELMET, Material.IRON_CHESTPLATE,
                    Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                    Material.TURTLE_HELMET
            };
        } else if (level >= 50) {
            armorSet = new Material[]{
                    Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE,
                    Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                    Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
                    Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS
            };
        } else {
            armorSet = new Material[]{
                    Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                    Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS
            };
        }

        ItemStack armor = new ItemStack(armorSet[random.nextInt(armorSet.length)]);
        List<Enchantment> available = resolveArmorEnchantments(level);
        if (available.isEmpty()) return armor;

        int tier = armorTier(level);
        int enchantmentCount = Math.min(1 + random.nextInt(Math.min(3, tier + 1)), available.size());
        Set<Enchantment> selected = new java.util.HashSet<>();
        while (selected.size() < enchantmentCount) {
            selected.add(available.get(random.nextInt(available.size())));
        }

        int maxLevel = Math.min(3, 1 + tier / 2);
        for (Enchantment enchantment : selected) {
            int enchantmentLevel = 1 + random.nextInt(
                    Math.max(1, Math.min(maxLevel, enchantment.getMaxLevel()))
            );
            armor.addUnsafeEnchantment(enchantment, enchantmentLevel);
        }
        return armor;
    }

    private List<Enchantment> resolveArmorEnchantments(int level) {
        List<Enchantment> available = new ArrayList<>();
        int tier = armorTier(level);
        int keysToUse = Math.min(ARMOR_ENCHANTMENT_KEYS.size(), 2 + tier);
        for (int i = 0; i < keysToUse; i++) {
            Enchantment enchantment = Enchantment.getByKey(
                    new NamespacedKey("minecraft", ARMOR_ENCHANTMENT_KEYS.get(i))
            );
            if (enchantment != null) available.add(enchantment);
        }
        // Remendo só aparece nas recompensas de nível intermediário/alto.
        if (tier >= 3) {
            Enchantment mending = Enchantment.getByKey(new NamespacedKey("minecraft", "mending"));
            if (mending != null && !available.contains(mending)) available.add(mending);
        }
        return available;
    }

    private int armorTier(int level) {
        if (level >= 500) return 4;
        if (level >= 250) return 3;
        if (level >= 100) return 2;
        if (level >= 50) return 1;
        return 0;
    }

    private ItemStack randomOreTreasure(int level) {
        Material[] ores;
        if (level >= 750) {
            ores = new Material[]{
                    Material.DIAMOND, Material.EMERALD, Material.NETHERITE_SCRAP
            };
        } else if (level >= 250) {
            ores = new Material[]{
                    Material.DIAMOND, Material.EMERALD
            };
        } else if (level >= 100) {
            ores = new Material[]{
                    Material.RAW_IRON, Material.RAW_GOLD, Material.IRON_INGOT,
                    Material.GOLD_INGOT, Material.LAPIS_LAZULI, Material.REDSTONE
            };
        } else if (level >= 50) {
            ores = new Material[]{
                    Material.COAL, Material.RAW_COPPER, Material.COPPER_INGOT,
                    Material.RAW_IRON, Material.IRON_NUGGET
            };
        } else {
            ores = new Material[]{
                    Material.COAL, Material.RAW_COPPER, Material.IRON_NUGGET
            };
        }

        Material material = ores[random.nextInt(ores.length)];
        int amount;
        if (material == Material.NETHERITE_SCRAP) {
            amount = 1;
        } else if (material == Material.DIAMOND || material == Material.EMERALD) {
            amount = level >= 750 ? 1 + random.nextInt(2) : 1;
        } else {
            amount = 1 + random.nextInt(3);
        }
        return new ItemStack(material, amount);
    }

    private ItemStack randomOtherTreasure(int level) {
        List<Material> treasures = new ArrayList<>(List.of(
                Material.NAME_TAG,
                Material.BOW,
                Material.FISHING_ROD,
                Material.EXPERIENCE_BOTTLE,
                Material.ENCHANTED_BOOK
        ));
        if (level >= 50) {
            treasures.add(Material.SADDLE);
            treasures.add(Material.NAUTILUS_SHELL);
        }
        if (level >= 250) {
            treasures.add(Material.HEART_OF_THE_SEA);
        }

        return new ItemStack(treasures.get(random.nextInt(treasures.size())));
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
