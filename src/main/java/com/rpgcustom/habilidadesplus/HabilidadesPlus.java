package com.rpgcustom.habilidadesplus;

import com.rpgcustom.habilidadesplus.commands.MMOCommand;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.gui.MMOMenuListener;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import com.rpgcustom.habilidadesplus.listeners.AcrobaticsListener;
import com.rpgcustom.habilidadesplus.listeners.AlchemyListener;
import com.rpgcustom.habilidadesplus.listeners.CombatListener;
import com.rpgcustom.habilidadesplus.listeners.FishingListener;
import com.rpgcustom.habilidadesplus.listeners.GatheringListener;
import com.rpgcustom.habilidadesplus.listeners.PlayerJoinQuitListener;
import com.rpgcustom.habilidadesplus.listeners.ProductionListener;
import com.rpgcustom.habilidadesplus.listeners.TamingListener;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HabilidadesPlus extends JavaPlugin {

    private ConfigManager configManager;
    private DataManager dataManager;
    private LevelingManager levelingManager;
    private XpManager xpManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.levelingManager = new LevelingManager(configManager.config());
        this.xpManager = new XpManager(this, dataManager, levelingManager, configManager);

        registerListeners();
        registerCommands();

        getLogger().info("HabilidadesPlus habilitado - Minecraft 26.2 / Spigot / Java 26.");
    }

    @Override
    public void onDisable() {
        if (xpManager != null) {
            xpManager.stopTask();
        }
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("HabilidadesPlus desabilitado, progresso dos jogadores salvo.");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new GatheringListener(this, configManager, xpManager), this);
        pm.registerEvents(new FishingListener(configManager, xpManager), this);
        pm.registerEvents(new CombatListener(configManager, xpManager), this);
        pm.registerEvents(new AcrobaticsListener(configManager, xpManager, dataManager), this);
        pm.registerEvents(new TamingListener(configManager, xpManager), this);
        pm.registerEvents(new AlchemyListener(configManager, xpManager), this);
        pm.registerEvents(new ProductionListener(configManager, xpManager), this);
        pm.registerEvents(new PlayerJoinQuitListener(dataManager, xpManager), this);
        pm.registerEvents(new MMOMenuListener(dataManager, levelingManager), this);
    }

    private void registerCommands() {
        MMOCommand mmoCommand = new MMOCommand(dataManager, levelingManager, configManager, xpManager);
        getCommand("mcmmo").setExecutor(mmoCommand);
    }
}
