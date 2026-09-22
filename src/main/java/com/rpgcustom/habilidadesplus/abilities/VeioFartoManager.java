package com.rpgcustom.habilidadesplus.abilities;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class VeioFartoManager implements Listener {

    private static final Set<Material> MINERIOS = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS
    );

    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final SuperBreakerManager superBreakerManager;

    public VeioFartoManager(DataManager dataManager, ConfigManager configManager,
                            SuperBreakerManager superBreakerManager) {
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.superBreakerManager = superBreakerManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onOreDrop(BlockDropItemEvent event) {
        if (!MINERIOS.contains(event.getBlockState().getType())) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();
        int level = dataManager.getProfile(uuid).getLevel(SkillType.MINERACAO);
        if (level < 1 || event.getItems().isEmpty()) {
            return;
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        boolean superQuebrador = superBreakerManager.isActive(uuid);
        if (superQuebrador && level < 100) {
            return;
        }
        String path = superQuebrador
                ? "mineracao.superbreaker.chance-drop-triplo-por-nivel"
                : "mineracao.veio-farto.chance-drop-duplo-por-nivel";
        double chance;
        if (superQuebrador) {
            double chancePorDezNiveis = configManager.config().getDouble(path, 0.5);
            chance = Math.min(100.0, Math.floor(level / 10.0) * chancePorDezNiveis);
        } else {
            chance = Math.min(100.0, level * configManager.config().getDouble(path, 0.1));
        }

        if (ThreadLocalRandom.current().nextDouble(100.0) >= chance) {
            return;
        }

        int multiplicador = superQuebrador ? 3 : 2;
        for (Item drop : event.getItems()) {
            ItemStack stack = drop.getItemStack();
            int amount = stack.getAmount();
            stack.setAmount(amount * multiplicador);
            drop.setItemStack(stack);
        }
    }
}
