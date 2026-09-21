package com.rpgcustom.habilidadesplus;

import org.bukkit.Material;

/**
 * Todas as habilidades disponiveis no HabilidadesPlus.
 * O displayName e usado nas mensagens (action bar, GUI) e pode
 * ser trocado livremente aqui sem afetar o resto do codigo.
 */
public enum SkillType {

    MINERACAO("Mineracao", Material.IRON_PICKAXE),
    ESCAVACAO("Escavacao", Material.IRON_SHOVEL),
    LENHADOR("Lenhador", Material.IRON_AXE),
    ERVANISMO("Ervanismo", Material.WHEAT),
    PESCA("Pesca", Material.FISHING_ROD),
    ACROBACIA("Acrobacia", Material.FEATHER),
    ESPADAS("Espadas", Material.IRON_SWORD),
    MACHADOS("Machados", Material.DIAMOND_AXE),
    DESARMADO("Desarmado", Material.LEATHER_BOOTS),
    ARQUERIA("Arqueria", Material.BOW),
    DOMESTICACAO("Domesticacao", Material.BONE),
    ALQUIMIA("Alquimia", Material.BREWING_STAND);

    private final String displayName;
    private final Material icon;

    SkillType(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }
}
