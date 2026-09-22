package com.rpgcustom.habilidadesplus.gui;

import com.rpgcustom.habilidadesplus.SkillType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MMOMenuHolder implements InventoryHolder {
    private Inventory inventory;
    private final SkillType skill;
    private final boolean ranking;

    public MMOMenuHolder(SkillType skill) { this(skill, false); }
    public MMOMenuHolder(SkillType skill, boolean ranking) {
        this.skill = skill;
        this.ranking = ranking;
    }
    public SkillType skill() { return skill; }
    public boolean ranking() { return ranking; }
    @Override public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
}
