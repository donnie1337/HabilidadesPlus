package com.rpgcustom.habilidadesplus.data;

import com.rpgcustom.habilidadesplus.SkillType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Salva/carrega o progresso de cada jogador em plugins/HabilidadesPlus/playerdata/<uuid>.yml
 * Mantem tambem um cache em memoria (perfis so sao lidos do disco uma vez, no join).
 */
public class DataManager {

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, PlayerProfile> cache = new HashMap<>();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerProfile getProfile(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public void unload(UUID uuid) {
        PlayerProfile profile = cache.remove(uuid);
        if (profile != null) {
            save(profile);
        }
    }

    public void saveAll() {
        for (PlayerProfile profile : cache.values()) {
            save(profile);
        }
    }

    private PlayerProfile load(UUID uuid) {
        PlayerProfile profile = new PlayerProfile(uuid);
        File file = new File(folder, uuid + ".yml");
        if (!file.exists()) {
            return profile;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (SkillType type : SkillType.values()) {
            String path = type.name();
            int level = yaml.getInt(path + ".nivel", 0);
            double xp = yaml.getDouble(path + ".xp", 0);
            PlayerSkillData data = profile.getData(type);
            data.setLevel(level);
            data.setCurrentXp(xp);
        }
        return profile;
    }

    public void save(PlayerProfile profile) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (SkillType type : SkillType.values()) {
            PlayerSkillData data = profile.getData(type);
            yaml.set(type.name() + ".nivel", data.getLevel());
            yaml.set(type.name() + ".xp", data.getCurrentXp());
        }

        File file = new File(folder, profile.getUuid() + ".yml");
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Nao foi possivel salvar dados do jogador " + profile.getUuid(), e);
        }
    }
}
