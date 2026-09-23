package com.rpgcustom.habilidadesplus;

import com.rpgcustom.habilidadesplus.abilities.SuperBreakerManager;
import com.rpgcustom.habilidadesplus.abilities.PrecisionMiningManager;
import com.rpgcustom.habilidadesplus.abilities.VeioFartoManager;
import com.rpgcustom.habilidadesplus.commands.LuzCommand;
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
import com.rpgcustom.habilidadesplus.top1.Top1SkillService;
import com.rpgcustom.habilidadesplus.top1.Top1PlaceholderExpansion;
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
    private Top1SkillService top1SkillService;
    private PlacedBlockTracker placedBlockTracker;
    private SuperBreakerManager superBreakerManager;
    private VeioFartoManager veioFartoManager;
    private PrecisionMiningManager precisionMiningManager;
    private LuzCommand luzCommand;
    private BukkitTask autosaveTask;
    private BukkitTask luzActionBarTask;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.placedBlockTracker = new PlacedBlockTracker(this, configManager.maxBlocosProtegidos());
        this.levelingManager = new LevelingManager(configManager.config());
        this.top1SkillService = new Top1SkillService(dataManager);
        dataManager.preloadAllProfilesAsync().whenComplete((ignored, error) -> {
            // O indice e carregado fora da thread principal. Quando terminar,
            // qualquer cache de Top 1 construido durante o startup fica invalido.
            top1SkillService.invalidate();
        });
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new Top1PlaceholderExpansion(this).register();
        }
        this.xpManager = new XpManager(this, dataManager, levelingManager, configManager, top1SkillService);
        this.superBreakerManager = new SuperBreakerManager(this, configManager, dataManager);
        this.veioFartoManager = new VeioFartoManager(
                dataManager, configManager, superBreakerManager, placedBlockTracker
        );
        this.precisionMiningManager = new PrecisionMiningManager(dataManager, configManager);
        this.luzCommand = new LuzCommand();

        registerListeners();
        registerCommands();
        luzActionBarTask = getServer().getScheduler().runTaskTimer(this, luzCommand::atualizarActionBars, 20L, 20L);
        startAutosave();

        getLogger().info("HabilidadesPlus habilitado. Comandos: /mcmmo, /habilidades e /luz.");
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public String getTop1Tag(java.util.UUID playerId) {
        SkillType skill = top1SkillService == null ? null : top1SkillService.getTop1Skill(playerId);
        return skill == null ? "" : configManager.top1Tag(skill);
    }

    public String getTop1SkillDisplayName(java.util.UUID playerId) {
        SkillType skill = top1SkillService == null ? null : top1SkillService.getTop1Skill(playerId);
        return skill == null ? "" : skill.getDisplayName();
    }

    @Override
    public void onDisable() {
        if (xpManager != null) {
            xpManager.stopTask();
        }
        if (superBreakerManager != null) {
            superBreakerManager.stopAll();
        }
        if (luzActionBarTask != null) {
            luzActionBarTask.cancel();
        }
        if (luzCommand != null) {
            luzCommand.desativarTodos();
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
        pm.registerEvents(superBreakerManager, this);
        pm.registerEvents(veioFartoManager, this);
        pm.registerEvents(precisionMiningManager, this);
        pm.registerEvents(new FishingListener(configManager, xpManager), this);
        pm.registerEvents(new CombatListener(configManager, xpManager), this);
        pm.registerEvents(new AcrobaticsListener(configManager, xpManager, dataManager), this);
        pm.registerEvents(new TamingListener(configManager, xpManager), this);
        pm.registerEvents(new AlchemyListener(configManager, xpManager), this);
        pm.registerEvents(new ProductionListener(configManager, xpManager), this);
        pm.registerEvents(new PlayerJoinQuitListener(dataManager, xpManager), this);
        pm.registerEvents(new MMOMenuListener(dataManager, levelingManager, configManager, top1SkillService), this);
    }

    private void registerCommands() {
        MMOCommand mmoCommand = new MMOCommand(dataManager, levelingManager, configManager, top1SkillService, this::reloadRuntime);
        PluginCommand command = Objects.requireNonNull(
                getCommand("mcmmo"),
                "O comando mcmmo nao foi encontrado no plugin.yml"
        );
        command.setExecutor(mmoCommand);
        command.setTabCompleter(mmoCommand);

        PluginCommand luz = Objects.requireNonNull(
                getCommand("luz"),
                "O comando luz nao foi encontrado no plugin.yml"
        );
        luz.setExecutor(luzCommand);
    }

    private void reloadRuntime() {
        configManager.load();
        placedBlockTracker.setMaxEntries(configManager.maxBlocosProtegidos());
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
