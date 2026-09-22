package com.rpgcustom.habilidadesplus.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Carrega e da acesso ao config.yml e ao messages.yml, incluindo
 * recarregamento em tempo de execucao (/mcmmo reload).
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Garante que novas chaves adicionadas em atualizacoes futuras existam,
        // usando o messages.yml embutido no jar como base de comparacao/merge.
        try (InputStream defaultStream = plugin.getResource("messages.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                messages.setDefaults(defaults);
            }
        } catch (IOException ignored) {
            // Se falhar, apenas seguimos com o messages.yml existente
        }
    }

    public FileConfiguration config() {
        return config;
    }

    public FileConfiguration messages() {
        return messages;
    }

    public String msg(String path) {
        // Sem um segundo argumento aqui: assim, se a chave nao existir no
        // messages.yml do usuario, o Bukkit consulta automaticamente os
        // "defaults" (o messages.yml embutido no jar) definidos em load().
        String valor = messages.getString(path);
        return valor != null ? valor : "";
    }

    public double xpMultiplicadorGlobal() {
        return config.getDouble("geral.multiplicador-xp-global", 1.0);
    }

    public int intervaloActionbarTicks() {
        return config.getInt("geral.intervalo-actionbar-ticks", 10);
    }

    public int autosaveMinutos() {
        return Math.max(1, config.getInt("geral.autosave-minutos", 3));
    }

    public boolean mundoDesabilitado(String mundo) {
        List<String> lista = config.getStringList("geral.mundos-desabilitados");
        return lista.contains(mundo);
    }

    /**
     * Retorna o XP configurado para uma chave dentro de uma secao de xp.*,
     * ou 0 se a chave nao estiver configurada (ou seja, sem XP).
     */
    public double xpDe(String secao, String chave) {
        ConfigurationSection sec = config.getConfigurationSection("xp." + secao);
        if (sec == null) return 0;
        return sec.getDouble(chave, 0);
    }

    /**
     * Igual a xpDe, mas retorna null se a chave nao estiver configurada
     * (util para distinguir "vale 0 XP" de "nao pertence a essa habilidade").
     */
    public Double xpDeSeConfigurado(String secao, String chave) {
        ConfigurationSection sec = config.getConfigurationSection("xp." + secao);
        if (sec == null || !sec.contains(chave)) return null;
        return sec.getDouble(chave);
    }
}
