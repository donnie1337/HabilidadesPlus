package com.rpgcustom.habilidadesplus;

import org.bukkit.Material;

/**
 * Habilidades disponíveis no HabilidadesPlus.
 * Os identificadores do enum também são usados para salvar os dados do jogador;
 * por isso as habilidades antigas mantêm os seus identificadores originais.
 */
public enum SkillType {

    MINERACAO("Mineração", Material.IRON_PICKAXE),
    LENHADOR("Lenhador", Material.IRON_AXE),
    ESCAVACAO("Escavação", Material.IRON_SHOVEL),
    ERVANISMO("Herbalismo", Material.WHEAT),
    PESCA("Pesca", Material.FISHING_ROD),
    ALQUIMIA("Alquimia", Material.BREWING_STAND),
    FUNDICAO("Fundição", Material.FURNACE),

    ESPADAS("Espadas", Material.IRON_SWORD),
    MACHADOS("Machados", Material.DIAMOND_AXE),
    ARQUERIA("Arqueiro", Material.BOW),
    ACROBACIA("Acrobacia", Material.FEATHER),
    DESARMADO("Desarmado", Material.LEATHER_BOOTS),
    DOMESTICACAO("Adestramento", Material.BONE),
    REPARACAO("Reparação", Material.ANVIL),
    CLAVA("Clava", Material.MACE),
    TRIDENTES("Tridentes", Material.TRIDENT),
    BESTAS("Bestas", Material.CROSSBOW),
    LANCAS("Lanças", material("SPEAR", Material.TRIDENT));

    private final String displayName;
    private final Material icon;

    SkillType(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    private static Material material(String materialName, Material fallback) {
        Material material = Material.matchMaterial(materialName);
        return material != null ? material : fallback;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }
}
