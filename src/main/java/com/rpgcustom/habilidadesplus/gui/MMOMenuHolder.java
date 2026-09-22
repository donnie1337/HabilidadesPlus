package com.rpgcustom.habilidadesplus.gui;

import com.rpgcustom.habilidadesplus.SkillType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MMOMenuHolder implements InventoryHolder {
    private Inventory inventory; private final SkillType skill;
    public MMOMenuHolder(SkillType skill){this.skill=skill;}
    public SkillType skill(){return skill;}
    @Override public Inventory getInventory(){return inventory;}
    public void setInventory(Inventory inventory){this.inventory=inventory;}
}