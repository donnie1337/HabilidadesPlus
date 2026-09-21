package com.rpgcustom.habilidadesplus.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marca um Inventory como sendo o menu do HabilidadesPlus, para que o listener
 * saiba que deve cancelar cliques nele sem depender de comparar o titulo.
 */
public class MMOMenuHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
