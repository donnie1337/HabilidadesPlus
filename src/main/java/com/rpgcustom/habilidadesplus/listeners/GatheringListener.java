package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.PlacedBlockTracker;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Cobre 4 habilidades de uma vez, pois todas nascem do mesmo evento
 * (quebrar um bloco): Mineracao, Escavacao, Lenhador e Ervanismo.
 *
 * Inclui protecao basica contra "farm" de XP: um bloco colocado pelo
 * proprio jogador nao da XP ao ser quebrado (evita, por exemplo, colocar
 * e quebrar areia repetidamente).
 */
public class GatheringListener implements Listener {

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

    private final ConfigManager configManager;
    private final XpManager xpManager;
    private final PlacedBlockTracker placedBlockTracker;

    public GatheringListener(ConfigManager configManager, XpManager xpManager,
                             PlacedBlockTracker placedBlockTracker) {
        this.configManager = configManager;
        this.xpManager = xpManager;
        this.placedBlockTracker = placedBlockTracker;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        placedBlockTracker.add(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (placedBlockTracker.removeIfPlaced(block)) {
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
