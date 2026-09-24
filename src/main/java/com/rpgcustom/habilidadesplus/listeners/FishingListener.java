package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

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
            Material.FISHING_ROD
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

        Material[] treasures = TREASURE_ITEMS.toArray(Material[]::new);
        caught.setItemStack(new ItemStack(treasures[random.nextInt(treasures.length)]));
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
