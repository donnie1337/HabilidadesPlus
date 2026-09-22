package com.rpgcustom.habilidadesplus.util;

import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/** Mantem a protecao antifarm de blocos colocados mesmo apos reinicios. */
public final class PlacedBlockTracker {
    private final JavaPlugin plugin;
    private final File file;
    private final Set<String> placed = new HashSet<>();
    private boolean dirty;

    public PlacedBlockTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "placed-blocks.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        placed.addAll(YamlConfiguration.loadConfiguration(file).getStringList("blocks"));
    }

    public void add(Block block) {
        if (placed.add(key(block))) dirty = true;
    }

    public boolean removeIfPlaced(Block block) {
        boolean removed = placed.remove(key(block));
        if (removed) dirty = true;
        return removed;
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    public void save() {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Nao foi possivel criar a pasta do HabilidadesPlus.");
            return;
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blocks", placed.stream().sorted().toList());
        try {
            yaml.save(temporary);
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Nao foi possivel salvar os blocos protegidos.", exception);
        }
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
