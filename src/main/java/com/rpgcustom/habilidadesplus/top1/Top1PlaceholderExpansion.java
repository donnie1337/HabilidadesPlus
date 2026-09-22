package com.rpgcustom.habilidadesplus.top1;

import com.rpgcustom.habilidadesplus.HabilidadesPlus;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/** PlaceholderAPI integration for the dynamic Top 1 skill tag. */
public final class Top1PlaceholderExpansion extends PlaceholderExpansion {

    private final HabilidadesPlus plugin;

    public Top1PlaceholderExpansion(HabilidadesPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "habilidade";
    }

    @Override
    public String getAuthor() {
        return "HabilidadesPlus";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || !"tag".equalsIgnoreCase(params)) return null;
        return plugin.getTop1Tag(player.getUniqueId());
    }
}
