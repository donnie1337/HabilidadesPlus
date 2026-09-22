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
import com.rpgcustom.habilidadesplus.util.PlacedBlockTracker;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class HabilidadesPlus extends JavaPlugin {

    private ConfigManager configManager;
    private DataManager dataManager;
    private LevelingManager levelingManager;
    private XpManager xpManager;
    private PlacedBlockTracker placedBlockTracker;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.placedBlockTracker = new PlacedBlockTracker(this);
        this.levelingManager = new LevelingManager(configManager.config());
        this.xpManager = new XpManager(this, dataManager, levelingManager, configManager);

        registerListeners();
        registerCommands();
        startAutosave();

        getLogger().info("HabilidadesPlus habilitado. Comandos: /mcmmo e /habilidades.");
    }

    @Override
    public DataManager getDataManager() {
        return dataManager;
    }

    public void onDisable() {
        if (xpManager != null) {
            xpManager.stopTask();
        }
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        if (dataManager != null) {
            dataManager.saveAll();
        }
        if (placedBlockTracker != null) {
            placedBlockTracker.save();
        }
        getLogger().info("HabilidadesPlus desabilitado, progresso dos jogadores salvo.");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new GatheringListener(configManager, xpManager, placedBlockTracker), this);
        pm.registerEvents(new FishingListener(configManager, xpManager), this);
        pm.registerEvents(new CombatListener(configManager, xpManager), this);
        pm.registerEvents(new AcrobaticsListener(configManager, xpManager, dataManager), this);
        pm.registerEvents(new TamingListener(configManager, xpManager), this);
        pm.registerEvents(new AlchemyListener(configManager, xpManager), this);
        pm.registerEvents(new ProductionListener(configManager, xpManager), this);
        pm.registerEvents(new PlayerJoinQuitListener(dataManager, xpManager), this);
        pm.registerEvents(new MMOMenuListener(dataManager, levelingManager, configManager), this);
    }

    private void registerCommands() {
        MMOCommand mmoCommand = new MMOCommand(dataManager, levelingManager, configManager, this::reloadRuntime);
        PluginCommand command = Objects.requireNonNull(
                getCommand("mcmmo"),
                "O comando mcmmo nao foi encontrado no plugin.yml"
        );
        command.setExecutor(mmoCommand);
        command.setTabCompleter(mmoCommand);
    }

    private void reloadRuntime() {
        configManager.load();
        levelingManager.reload(configManager.config());
        xpManager.startTask();
        startAutosave();
    }

    private void startAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        long ticks = Math.max(20L, configManager.autosaveMinutos() * 60L * 20L);
        autosaveTask = getServer().getScheduler().runTaskTimer(this, () -> {
            dataManager.saveDirty();
            placedBlockTracker.saveIfDirty();
        }, ticks, ticks);
    }
}
