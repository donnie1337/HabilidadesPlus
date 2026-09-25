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
import org.bukkit.event.inventory.FurnaceExtractEvent;
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
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || event.getItemAmount() <= 0) return;

        awardSmeltingXp(player, event.getItemAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceHopperExtract(InventoryMoveItemEvent event) {
        TileState furnace = furnaceState(event.getSource());
        if (furnace == null || !isOutputItem(event.getSource(), event.getItem())) return;

        UUID ownerId = furnace.getPersistentDataContainer().get(furnaceOwnerKey, PersistentDataType.STRING) == null
                ? null
                : parseUuid(furnace.getPersistentDataContainer().get(furnaceOwnerKey, PersistentDataType.STRING));
        if (ownerId == null) return;

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner == null || owner.getGameMode() == GameMode.CREATIVE) return;

        awardSmeltingXp(owner, event.getItem().getAmount());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceBroken(BlockBreakEvent event) {
        if (isFurnace(event.getBlock().getType())) {
            clearOwner(event.getBlock());
        }
    }

    private void awardSmeltingXp(Player player, int amount) {
        if (amount <= 0) return;
        double xpPerItem = configManager.config().getDouble("xp.fundicao.xp-por-item-fundido", 15);
        xpManager.addXp(player, SkillType.FUNDICAO, xpPerItem * amount);
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
