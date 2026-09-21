package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cobre 4 habilidades de uma vez, pois todas nascem do mesmo evento
 * (quebrar um bloco): Mineracao, Escavacao, Lenhador e Ervanismo.
 *
 * Inclui protecao basica contra "farm" de XP: um bloco colocado pelo
 * proprio jogador nao da XP ao ser quebrado (evita, por exemplo, colocar
 * e quebrar areia repetidamente).
 */
public class GatheringListener implements Listener {

    private static final String METADATA_COLOCADO = "custommmo_colocado_por_jogador";

    private static final SkillType[] HABILIDADES = {
            SkillType.MINERACAO,
            SkillType.ESCAVACAO,
            SkillType.LENHADOR,
            SkillType.ERVANISMO
    };
    private static final String[] SECOES = {
            "mineracao",
            "escavacao",
            "lenhador",
            "ervanismo"
    };

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final XpManager xpManager;

    public GatheringListener(JavaPlugin plugin, ConfigManager configManager, XpManager xpManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        event.getBlock().setMetadata(METADATA_COLOCADO, new FixedMetadataValue(plugin, true));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (block.hasMetadata(METADATA_COLOCADO)) {
            block.removeMetadata(METADATA_COLOCADO, plugin);
            return;
        }

        Material material = block.getType();
        for (int i = 0; i < HABILIDADES.length; i++) {
            Double xp = configManager.xpDeSeConfigurado(SECOES[i], material.name());
            if (xp != null) {
                xpManager.addXp(player, HABILIDADES[i], xp);
                return;
            }
        }
    }
}
