package com.rpgcustom.habilidadesplus.listeners;

import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    private final DataManager dataManager;
    private final XpManager xpManager;

    public PlayerJoinQuitListener(DataManager dataManager, XpManager xpManager) {
        this.dataManager = dataManager;
        this.xpManager = xpManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Carrega (ou cria) o perfil do jogador assim que ele entra, deixando
        // pronto em cache para o primeiro ganho de XP nao ter que ler do disco.
        dataManager.getProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dataManager.unload(event.getPlayer().getUniqueId());
        xpManager.clearPlayer(event.getPlayer().getUniqueId());
    }
}
