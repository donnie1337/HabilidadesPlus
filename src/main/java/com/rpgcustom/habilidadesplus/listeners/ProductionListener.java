package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Recompensa atividades de produção: reparar itens na bigorna e retirar
 * itens fundidos de fornalhas, altos-fornos e defumadores.
 */
public class ProductionListener implements Listener {

    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final NamespacedKey furnaceOwnerKey;

    public ProductionListener(JavaPlugin plugin, ConfigManager configManager, XpManager xpManager) {
        this.configManager = configManager;
        this.xpManager = xpManager;
        this.furnaceOwnerKey = new NamespacedKey(plugin, "fundicao-owner");
    }

    @EventHandler(ignoreCancelled = true)
    public void onRepair(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL || event.getRawSlot() != 2) return;
        if (!(event.getWhoClicked() instanceof Player player) || player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;
        ItemStack original = event.getInventory().getItem(0);
        if (original == null || !(original.getItemMeta() instanceof Damageable before)
                || !(result.getItemMeta() instanceof Damageable after)
                || after.getDamage() >= before.getDamage()) return;

        double xp = configManager.config().getDouble("xp.reparacao.xp-por-reparo", 75);
        xpManager.addXp(player, SkillType.REPARACAO, xp);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnacePlaced(BlockPlaceEvent event) {
        if (!isFurnace(event.getBlock().getType())) return;
        registerOwner(event.getBlock(), event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceOpened(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isFurnace(event.getInventory().getType())) return;
        registerOwner(event.getInventory(), player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceClicked(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isFurnace(event.getInventory().getType())) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;
        if (event.getRawSlot() > 1) return;
        registerOwner(event.getInventory(), player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        Player owner = ownerPlayer(furnaceState(event.getBlock()));
        if (owner == null) return;

        double bonus = progression(owner, 25,
                "fundicao.brasa-eficiente.bonus-por-nivel",
                "fundicao.brasa-eficiente.bonus-maximo",
                0.05, 50.0);
        if (bonus <= 0.0) return;

        event.setBurnTime((int) Math.ceil(event.getBurnTime() * (1.0 + bonus / 100.0)));

        ItemStack fuel = event.getFuel();
        if (fuel == null || !isRecoverableFuel(fuel.getType())) return;
        double recoveryChance = progression(owner, 150,
                "fundicao.recuperacao-combustivel.chance-por-nivel",
                "fundicao.recuperacao-combustivel.chance-maxima",
                0.05, 15.0);
        if (recoveryChance > 0.0 && Math.random() * 100.0 < recoveryChance) {
            giveRecoveredFuel(owner, fuel.getType(), event.getBlock());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceStartSmelt(FurnaceStartSmeltEvent event) {
        Player owner = ownerPlayer(furnaceState(event.getBlock()));
        if (owner == null) return;

        double reduction = progression(owner, 100,
                "fundicao.forja-acelerada.reducao-tempo-por-nivel",
                "fundicao.forja-acelerada.reducao-tempo-maxima",
                0.05, 50.0);
        if (reduction <= 0.0) return;

        int cookTime = Math.max(1, (int) Math.round(
                event.getTotalCookTime() * (1.0 - reduction / 100.0)
        ));
        event.setTotalCookTime(cookTime);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || event.getItemAmount() <= 0) return;

        awardSmeltingXp(player, event.getItemAmount());
        giveRefinedExtra(player, new ItemStack(event.getItemType()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceHopperExtract(InventoryMoveItemEvent event) {
        TileState furnace = furnaceState(event.getSource());
        if (furnace == null || !isOutputItem(event.getSource(), event.getItem())) return;

        Player owner = ownerPlayer(furnace);
        if (owner == null || owner.getGameMode() == GameMode.CREATIVE) return;

        awardSmeltingXp(owner, event.getItem().getAmount());
        giveRefinedExtra(owner, event.getDestination(), event.getItem().clone());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceBroken(BlockBreakEvent event) {
        if (isFurnace(event.getBlock().getType())) {
            clearOwner(event.getBlock());
        }
    }

    private void giveRefinedExtra(Player player, ItemStack output) {
        double chance = progression(player, 75,
                "fundicao.liga-refinada.chance-por-nivel",
                "fundicao.liga-refinada.chance-maxima",
                0.05, 50.0);
        if (chance <= 0.0 || Math.random() * 100.0 >= chance) return;

        ItemStack extra = output.clone();
        extra.setAmount(1);
        player.getInventory().addItem(extra).values()
                .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private void giveRefinedExtra(Player owner, Inventory destination, ItemStack output) {
        double chance = progression(owner, 75,
                "fundicao.liga-refinada.chance-por-nivel",
                "fundicao.liga-refinada.chance-maxima",
                0.05, 50.0);
        if (chance <= 0.0 || Math.random() * 100.0 >= chance) return;

        ItemStack extra = output.clone();
        extra.setAmount(1);
        destination.addItem(extra).values().forEach(item -> {
            var location = destination.getLocation();
            if (location != null && location.getWorld() != null) {
                location.getWorld().dropItemNaturally(location, item);
            }
        });
    }

    private Player ownerPlayer(TileState state) {
        if (state == null) return null;
        String value = state.getPersistentDataContainer().get(furnaceOwnerKey, PersistentDataType.STRING);
        UUID ownerId = value == null ? null : parseUuid(value);
        return ownerId == null ? null : Bukkit.getPlayer(ownerId);
    }

    private double progression(Player player, int unlock, String perLevelPath, String maxPath,
                               double defaultPerLevel, double defaultMax) {
        if (player == null) return 0.0;
        int level = xpManager.getDataManager().getProfile(player.getUniqueId()).getLevel(SkillType.FUNDICAO);
        if (level < unlock) return 0.0;

        double perLevel = Math.max(0.0,
                configManager.config().getDouble(perLevelPath, defaultPerLevel));
        double max = Math.max(0.0,
                configManager.config().getDouble(maxPath, defaultMax));
        return Math.min(max, level * perLevel);
    }

    private void awardSmeltingXp(Player player, int amount) {
        if (amount <= 0) return;
        double xpPerItem = configManager.config().getDouble("xp.fundicao.xp-por-item-fundido", 15);
        double bonus = progression(player, 125,
                "fundicao.experiencia-metalurgica.bonus-por-nivel",
                "fundicao.experiencia-metalurgica.bonus-maximo",
                0.05, 25.0);
        xpManager.addXp(player, SkillType.FUNDICAO, xpPerItem * amount * (1.0 + bonus / 100.0));
    }

    private boolean isRecoverableFuel(Material material) {
        return material == Material.COAL
                || material == Material.CHARCOAL
                || material == Material.COAL_BLOCK;
    }

    private void giveRecoveredFuel(Player player, Material material, Block furnace) {
        ItemStack recovered = new ItemStack(material);
        player.getInventory().addItem(recovered).values().forEach(item -> {
            if (furnace.getWorld() != null) {
                furnace.getWorld().dropItemNaturally(furnace.getLocation(), item);
            }
        });
    }

    private void registerOwner(Inventory inventory, Player player) {
        TileState state = furnaceState(inventory);
        if (state != null) registerOwner(state, player);
    }

    private void registerOwner(Block block, Player player) {
        TileState state = furnaceState(block);
        if (state != null) registerOwner(state, player);
    }

    private void registerOwner(TileState state, Player player) {
        if (state.getPersistentDataContainer().has(furnaceOwnerKey, PersistentDataType.STRING)) return;
        state.getPersistentDataContainer().set(
                furnaceOwnerKey,
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );
        state.update(true, false);
    }

    private void clearOwner(Block block) {
        TileState state = furnaceState(block);
        if (state == null) return;
        state.getPersistentDataContainer().remove(furnaceOwnerKey);
        state.update(true, false);
    }

    private TileState furnaceState(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof TileState state && isFurnace(state.getType()) ? state : null;
    }

    private TileState furnaceState(Block block) {
        BlockState state = block.getState();
        return state instanceof TileState tile && isFurnace(block.getType()) ? tile : null;
    }

    private boolean isOutputItem(Inventory source, ItemStack moved) {
        if (source.getSize() <= 2 || moved == null || moved.getType().isAir()) return false;
        ItemStack output = source.getItem(2);
        return output != null && output.isSimilar(moved);
    }

    private boolean isFurnace(InventoryType type) {
        return type == InventoryType.FURNACE
                || type == InventoryType.BLAST_FURNACE
                || type == InventoryType.SMOKER;
    }

    private boolean isFurnace(Material material) {
        return material == Material.FURNACE
                || material == Material.BLAST_FURNACE
                || material == Material.SMOKER;
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
