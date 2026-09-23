package com.rpgcustom.habilidadesplus.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.rpgcustom.habilidadesplus.SkillType;
import org.bukkit.entity.Player;
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

    /**
     * Regra comum de acesso às habilidades: permissao e mundo habilitado.
     */
    public boolean habilidadeAtiva(Player player) {
        return player != null
                && player.hasPermission("habilidadesplus.use")
                && !mundoDesabilitado(player.getWorld().getName());
    }

    public int maxBlocosProtegidos() {
        return Math.max(1, config.getInt("geral.max-blocos-protegidos", 100_000));
    }

    /**
     * O valor e configurado por cada dez niveis para manter o balanceamento
     * original. O nome antigo continua como fallback para configs existentes.
     */
    public double chanceDropTriploPorDezNiveis() {
        double value = config.getDouble(
                "mineracao.superbreaker.chance-drop-triplo-por-10-niveis",
                config.getDouble("mineracao.superbreaker.chance-drop-triplo-por-nivel", 0.5)
        );
        return Math.max(0.0, value);
    }

    public String top1Tag(SkillType skill) {
        if (skill == null) return "";
        String configured = config.getString("top1-tags." + skill.name());
        if (configured != null) return configured;
        return switch (skill) {
            case MINERACAO -> " &b⛏"; case LENHADOR -> " &6🪓"; case ESCAVACAO -> " &e⌂";
            case ERVANISMO -> " &a🌿"; case PESCA -> " &9🎣"; case FUNDICAO -> " &c♨";
            case ALQUIMIA -> " &5⚗"; case REPARACAO -> " &7⚒"; case ESPADAS -> " &c⚔";
            case MACHADOS -> " &6🪓"; case CLAVA -> " &4✹"; case DESARMADO -> " &f✊";
            case ARQUERIA -> " &e➳"; case BESTAS -> " &7⌁"; case TRIDENTES -> " &b🔱";
            case LANCAS -> " &f⚔"; case ACROBACIA -> " &d✦"; case DOMESTICACAO -> " &6♞";
        };
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
