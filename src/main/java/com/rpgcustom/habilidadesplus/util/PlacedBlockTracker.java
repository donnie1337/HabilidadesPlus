package com.rpgcustom.habilidadesplus.util;

import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Mantem a protecao antifarm de blocos colocados mesmo apos reinicios.
 * A lista e limitada e os autosaves sao encadeados fora da thread principal.
 */
public final class PlacedBlockTracker {
    private static final long PENDING_DROP_TTL_NANOS = 2_000_000_000L;

    private final JavaPlugin plugin;
    private final File file;
    private int maxEntries;
    private final Set<String> placed = new HashSet<>();
    private final Map<String, Long> pendingDropProtection = new HashMap<>();
    private CompletableFuture<Void> saveFuture = CompletableFuture.completedFuture(null);
    private boolean dirty;
    private boolean capacityWarningLogged;

    public PlacedBlockTracker(JavaPlugin plugin) {
        this(plugin, 100_000);
    }

    public PlacedBlockTracker(JavaPlugin plugin, int maxEntries) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "placed-blocks.yml");
        this.maxEntries = Math.max(1, maxEntries);
        load();
    }

    public synchronized void setMaxEntries(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    private synchronized void load() {
        if (!file.exists()) return;
        placed.addAll(YamlConfiguration.loadConfiguration(file).getStringList("blocks"));
        if (placed.size() > maxEntries) {
            plugin.getLogger().warning("A lista de blocos protegidos excede o limite configurado; novos blocos nao serao registrados ate reduzir a lista.");
        }
    }

    public synchronized boolean add(Block block) {
        String key = key(block);
        if (placed.contains(key)) return true;
        if (placed.size() >= maxEntries) {
            if (!capacityWarningLogged) {
                capacityWarningLogged = true;
                plugin.getLogger().warning("Limite de blocos protegidos atingido (" + maxEntries + "). Reduza placed-blocks.yml ou aumente geral.max-blocos-protegidos.");
            }
            return false;
        }
        placed.add(key);
        dirty = true;
        return true;
    }

    /**
     * Remove o marcador do bloco quebrado e protege o drop correspondente por
     * alguns segundos, permitindo que o listener de drops reconheca o mesmo
     * bloco sem manter a entrada persistente indefinidamente.
     */
    public synchronized boolean removeIfPlaced(Block block) {
        String key = key(block);
        boolean removed = placed.remove(key);
        if (removed) {
            pendingDropProtection.put(key, System.nanoTime() + PENDING_DROP_TTL_NANOS);
            dirty = true;
        }
        return removed;
    }

    /** Remove um marcador quando o bloco foi destruido por outro mecanismo. */
    public synchronized boolean discard(Block block) {
        boolean removed = placed.remove(key(block));
        pendingDropProtection.remove(key(block));
        if (removed) {
            dirty = true;
        }
        return removed;
    }

    public synchronized boolean consumeProtectedDrop(Block block) {
        Long expiresAt = pendingDropProtection.remove(key(block));
        return expiresAt != null && expiresAt >= System.nanoTime();
    }

    /**
     * Captura um snapshot no thread principal e faz a serializacao/escrita em
     * uma cadeia assincrona. Chamadas consecutivas nunca executam duas escritas
     * simultaneas no mesmo arquivo.
     */
    public synchronized void saveIfDirty() {
        if (!dirty) return;

        dirty = false;
        List<String> snapshot = snapshot();
        saveFuture = saveFuture
                .handle((ignored, error) -> null)
                .thenRunAsync(() -> {
                    if (!writeSnapshot(snapshot)) {
                        synchronized (PlacedBlockTracker.this) {
                            dirty = true;
                        }
                    }
                });
    }

    /**
     * Usado no desligamento: aguarda autosaves pendentes e grava o snapshot
     * final de forma síncrona para não perder alterações recentes.
     */
    public void save() {
        CompletableFuture<Void> pending;
        synchronized (this) {
            pending = saveFuture;
        }
        pending.join();

        List<String> snapshot;
        synchronized (this) {
            snapshot = snapshot();
            dirty = false;
        }
        if (!writeSnapshot(snapshot)) {
            synchronized (this) {
                dirty = true;
            }
        }
        synchronized (this) {
            saveFuture = CompletableFuture.completedFuture(null);
        }
    }

    private List<String> snapshot() {
        return placed.stream().sorted().toList();
    }

    private boolean writeSnapshot(List<String> snapshot) {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Nao foi possivel criar a pasta do HabilidadesPlus.");
            return false;
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blocks", snapshot);
        try {
            yaml.save(temporary);
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Nao foi possivel salvar os blocos protegidos.", exception);
            return false;
        }
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
