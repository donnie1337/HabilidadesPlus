package com.rpgcustom.habilidadesplus.xp;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import com.rpgcustom.habilidadesplus.leveling.LevelUpResult;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import com.rpgcustom.habilidadesplus.util.ActionBarUtil;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

/**
 * Ponto central de ganho de XP. As habilidades chamam apenas addXp(...) daqui.
 *
 * O XP e aplicado ao PlayerProfile IMEDIATAMENTE (para nao perder progresso
 * se o jogador desconectar), mas a exibicao na action bar e agrupada em um
 * pequeno intervalo (geral.intervalo-actionbar-ticks) para nao piscar uma
 * mensagem a cada bloco quebrado.
 */
public class XpManager {

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final LevelingManager levelingManager;
    private final ConfigManager configManager;

    // uuid -> (habilidade -> xp acumulado desde a ultima atualizacao da action bar)
    private final Map<UUID, Map<SkillType, Double>> pendingDisplay = new HashMap<>();

    private BukkitTask task;

    public XpManager(JavaPlugin plugin, DataManager dataManager, LevelingManager levelingManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.levelingManager = levelingManager;
        this.configManager = configManager;
        startTask();
    }

    public void startTask() {
        stopTask();
        long interval = Math.max(1, configManager.intervaloActionbarTicks());
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::flush, interval, interval);
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * Concede XP de uma habilidade a um jogador. baseAmount ja deve ser o
     * valor "cru" vindo do config.yml; o multiplicador global e aplicado aqui.
     */
    public void addXp(Player player, SkillType skill, double baseAmount) {
        if (baseAmount <= 0) return;
        if (!player.hasPermission("habilidadesplus.use")) return;
        if (configManager.mundoDesabilitado(player.getWorld().getName())) return;

        double amount = baseAmount * configManager.xpMultiplicadorGlobal();
        if (amount <= 0) return;

        PlayerProfile profile = dataManager.getProfile(player.getUniqueId());
        if (profile.getLevel(skill) >= levelingManager.getNivelMaximo()) return;
        LevelUpResult result = profile.addXp(skill, amount, levelingManager);
        dataManager.markDirty(player.getUniqueId());

        boolean milestone = result.isLeveledUp() && result.getNewLevel() % 100 == 0;

        // Em milestones (100, 200, 300...), o aviso fica somente no Title.
        // Nao envia o mesmo ganho de XP pela Action Bar nesse momento.
        if (!milestone) {
            pendingDisplay
                    .computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(SkillType.class))
                    .merge(skill, amount, Double::sum);
        }

        if (milestone) {
            announceLevelUp(player, skill, result);
        }
    }

    private void flush() {
        if (pendingDisplay.isEmpty()) return;

        String formato = configManager.msg("actionbar.formato-xp");
        String separador = configManager.msg("actionbar.separador");

        for (Map.Entry<UUID, Map<SkillType, Double>> entry : pendingDisplay.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            PlayerProfile profile = dataManager.getProfile(player.getUniqueId());
            StringBuilder mensagem = new StringBuilder();
            boolean primeiro = true;

            for (Map.Entry<SkillType, Double> ganho : entry.getValue().entrySet()) {
                SkillType skill = ganho.getKey();
                int xpGanho = (int) Math.round(ganho.getValue());
                if (xpGanho <= 0) continue;

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("habilidade", skill.getDisplayName());
                placeholders.put("xp", String.valueOf(xpGanho));
                placeholders.put("nivel", String.valueOf(profile.getLevel(skill)));

                if (!primeiro) {
                    mensagem.append(separador);
                }
                mensagem.append(MessageUtil.placeholders(formato, placeholders));
                primeiro = false;
            }

            if (mensagem.length() > 0) {
                ActionBarUtil.send(player, mensagem.toString());
            }
        }

        pendingDisplay.clear();
    }

    private void announceLevelUp(Player player, SkillType skill, LevelUpResult result) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("habilidade", skill.getDisplayName());
        placeholders.put("nivel", String.valueOf(result.getNewLevel()));

        String tituloTexto = MessageUtil.placeholders(configManager.msg("level-up.titulo"), placeholders);
        String subtituloTexto = MessageUtil.placeholders(configManager.msg("level-up.subtitulo"), placeholders);

        player.sendTitle(
                MessageUtil.colorize(tituloTexto),
                MessageUtil.colorize(subtituloTexto),
                5, 40, 10
        );

        String somConfigurado = configManager.msg("level-up.som");
        String soundKey = somConfigurado.toLowerCase(Locale.ROOT).replace('_', '.');
        Sound som = Registry.SOUNDS.get(NamespacedKey.minecraft(soundKey));
        player.playSound(player.getLocation(),
                som != null ? som : Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    public void clearPlayer(UUID uuid) {
        pendingDisplay.remove(uuid);
    }
}
