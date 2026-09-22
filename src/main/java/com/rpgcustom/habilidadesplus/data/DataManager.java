package com.rpgcustom.habilidadesplus.data;

import com.rpgcustom.habilidadesplus.SkillType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private final Set<UUID> dirty = new HashSet<>();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Nao foi possivel criar a pasta de dados dos jogadores.");
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
        dirty.remove(uuid);
    }

    public Map<UUID, PlayerProfile> getLoadedProfiles() {
        return Map.copyOf(cache);
    }

    public Map<UUID, PlayerProfile> getAllProfiles() {
        Map<UUID, PlayerProfile> profiles = new HashMap<>(cache);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return Map.copyOf(profiles);
        for (File file : files) {
            try {
                UUID uuid = UUID.fromString(file.getName().substring(0, file.getName().length() - 4));
                profiles.putIfAbsent(uuid, load(uuid));
            } catch (IllegalArgumentException ignored) {
                // Ignora arquivos que nao sejam playerdata validos.
            }
        }
        return Map.copyOf(profiles);
    }

    public void markDirty(UUID uuid) {
        dirty.add(uuid);
    }

    public void saveDirty() {
        for (UUID uuid : Set.copyOf(dirty)) {
            PlayerProfile profile = cache.get(uuid);
            if (profile != null && save(profile)) {
                dirty.remove(uuid);
            }
        }
    }

    public void saveAll() {
        for (PlayerProfile profile : cache.values()) {
            save(profile);
        }
        dirty.clear();
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

    public boolean save(PlayerProfile profile) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (SkillType type : SkillType.values()) {
            PlayerSkillData data = profile.getData(type);
            yaml.set(type.name() + ".nivel", data.getLevel());
            yaml.set(type.name() + ".xp", data.getCurrentXp());
        }

        File file = new File(folder, profile.getUuid() + ".yml");
        File temporary = new File(folder, profile.getUuid() + ".yml.tmp");
        try {
            yaml.save(temporary);
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Nao foi possivel salvar dados do jogador " + profile.getUuid(), e);
            return false;
        }
    }
}
