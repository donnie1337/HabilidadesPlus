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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

/**
 * Salva/carrega o progresso de cada jogador em plugins/HabilidadesPlus/playerdata/<uuid>.yml
 * Mantem um indice em memoria com os perfis conhecidos. A carga inicial dos
 * arquivos e feita de forma assincrona para que a thread principal nunca
 * precise varrer playerdata ao abrir o ranking.
 */
public class DataManager {

    private final JavaPlugin plugin;
    private final File folder;
    private final ConcurrentMap<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, PlayerProfile> allProfiles = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private volatile CompletableFuture<Void> preloadFuture;

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Nao foi possivel criar a pasta de dados dos jogadores.");
        }
    }

    public PlayerProfile getProfile(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            PlayerProfile profile = allProfiles.get(id);
            if (profile == null) {
                profile = load(id);
                allProfiles.putIfAbsent(id, profile);
            }
            return profile;
        });
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

    /**
     * Inicia uma unica carga assincrona dos perfis existentes em disco.
     * Chamadas seguintes reutilizam o mesmo Future e nao iniciam novas varreduras.
     */
    public synchronized CompletableFuture<Void> preloadAllProfilesAsync() {
        if (preloadFuture != null) {
            return preloadFuture;
        }

        preloadFuture = CompletableFuture
                .supplyAsync(this::loadProfilesFromDisk)
                .thenAccept(loaded -> loaded.forEach((uuid, profile) -> allProfiles.putIfAbsent(uuid, profile)));
        preloadFuture.whenComplete((ignored, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING,
                        "Nao foi possivel carregar o indice de perfis dos jogadores.", error);
            }
        });
        return preloadFuture;
    }

    public boolean isAllProfilesLoaded() {
        CompletableFuture<Void> future = preloadFuture;
        return future != null && future.isDone() && !future.isCompletedExceptionally();
    }

    /**
     * Executa o callback na thread principal quando a carga inicial terminar.
     * O callback nao e executado se a carga falhar ou o plugin estiver desativado.
     */
    public void whenAllProfilesLoaded(Runnable callback) {
        preloadAllProfilesAsync().whenComplete((ignored, error) -> {
            if (error != null || !plugin.isEnabled()) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, callback);
        });
    }

    /**
     * Retorna um snapshot dos perfis em memoria. Este metodo nunca faz I/O;
     * durante o startup o snapshot pode estar incompleto ate o preload terminar.
     */
    public Map<UUID, PlayerProfile> getAllProfiles() {
        // Nunca acessa o disco. A carga inicial e feita por preloadAllProfilesAsync().
        // O cache de perfis ativos tem precedencia sobre qualquer snapshot antigo.
        Map<UUID, PlayerProfile> profiles = new HashMap<>(allProfiles);
        profiles.putAll(cache);
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
        profile.setLenhadorArvoresReplantadas(yaml.getLong("LENHADOR.arvores-replantadas", 0));
        return profile;
    }

    public boolean save(PlayerProfile profile) {
        allProfiles.put(profile.getUuid(), profile);
        YamlConfiguration yaml = new YamlConfiguration();
        for (SkillType type : SkillType.values()) {
            PlayerSkillData data = profile.getData(type);
            yaml.set(type.name() + ".nivel", data.getLevel());
            yaml.set(type.name() + ".xp", data.getCurrentXp());
        }
        yaml.set("LENHADOR.arvores-replantadas", profile.getLenhadorArvoresReplantadas());

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

    private Map<UUID, PlayerProfile> loadProfilesFromDisk() {
        Map<UUID, PlayerProfile> loaded = new HashMap<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return loaded;
        }

        for (File file : files) {
            try {
                UUID uuid = UUID.fromString(file.getName().substring(0, file.getName().length() - 4));
                loaded.put(uuid, load(uuid));
            } catch (IllegalArgumentException ignored) {
                // Ignora arquivos que nao sejam playerdata validos.
            }
        }
        return loaded;
    }
}
