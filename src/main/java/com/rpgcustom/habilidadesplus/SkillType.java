package com.rpgcustom.habilidadesplus;

import org.bukkit.Material;

/**
 * Habilidades disponíveis no HabilidadesPlus.
 * Os identificadores do enum também são usados para salvar os dados do jogador;
 * por isso as habilidades antigas mantêm os seus identificadores originais.
 */
public enum SkillType {

    // Coleta e recursos
    MINERACAO("Mineração", Material.IRON_PICKAXE),
    LENHADOR("Lenhador", Material.IRON_AXE),
    ESCAVACAO("Escavação", Material.IRON_SHOVEL),
    ERVANISMO("Herbalismo", Material.WHEAT),
    PESCA("Pesca", Material.FISHING_ROD),

    // Produção e utilidade
    FUNDICAO("Fundição", Material.FURNACE),
    ALQUIMIA("Alquimia", Material.BREWING_STAND),
    REPARACAO("Reparação", Material.ANVIL),

    // Combate corpo a corpo
    ESPADAS("Espadas", Material.IRON_SWORD),
    MACHADOS("Machados", Material.DIAMOND_AXE),
    CLAVA("Clava", Material.MACE),
    DESARMADO("Desarmado", Material.LEATHER_BOOTS),

    // Combate à distância
    ARQUERIA("Arqueiro", Material.BOW),
    BESTAS("Bestas", Material.CROSSBOW),
    TRIDENTES("Tridentes", Material.TRIDENT),
    LANCAS("Lanças", Material.STONE_SPEAR),

    // Mobilidade e companheiros
    ACROBACIA("Acrobacia", Material.FEATHER),
    DOMESTICACAO("Adestramento", Material.BONE);

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
