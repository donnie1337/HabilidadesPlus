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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Ponto central de ganho de XP. As habilidades chamam apenas addXp(...) daqui.
 */
public class XpManager {

    private final DataManager dataManager;
    private final LevelingManager levelingManager;
    private final ConfigManager configManager;
    private final Map<UUID, BukkitTask> levelUpActionbarTasks = new HashMap<>();

    public XpManager(JavaPlugin plugin, DataManager dataManager, LevelingManager levelingManager, ConfigManager configManager) {
        this.dataManager = dataManager;
        this.levelingManager = levelingManager;
        this.configManager = configManager;
    }

    /**
     * Mantido para compatibilidade com o ciclo de reload do plugin.
     * A Action Bar agora e atualizada imediatamente a cada ganho de XP.
     */
    public void startTask() {
        stopTask();
    }

    public void stopTask() {
        for (BukkitTask task : levelUpActionbarTasks.values()) {
            task.cancel();
        }
        levelUpActionbarTasks.clear();
    }

    public void addXp(Player player, SkillType skill, double baseAmount) {
        if (baseAmount <= 0) return;
        if (!player.hasPermission("habilidadesplus.use")) return;
        if (configManager.mundoDesabilitado(player.getWorld().getName())) return;

        double amount = baseAmount * configManager.xpMultiplicadorGlobal();
        if (amount <= 0) return;

        PlayerProfile profile = dataManager.getProfile(player.getUniqueId());
        int poderGeralAntes = profile.getPowerLevel();
        int nivelHabilidadeAntes = profile.getLevel(skill);

        if (nivelHabilidadeAntes >= levelingManager.getNivelMaximo()) return;

        LevelUpResult result = profile.addXp(skill, amount, levelingManager);
        dataManager.markDirty(player.getUniqueId());

        int poderGeralDepois = profile.getPowerLevel();
        int nivelHabilidadeDepois = profile.getLevel(skill);

        boolean atingiuPoderGeral100 = poderGeralAntes < 100 && poderGeralDepois >= 100;
        boolean atingiuPoderHabilidade100 = nivelHabilidadeAntes < 100 && nivelHabilidadeDepois >= 100;

        if (atingiuPoderGeral100) {
            announcePowerMilestone(player, "poder.milestone-geral", null, poderGeralDepois);
        }
        if (atingiuPoderHabilidade100) {
            announcePowerMilestone(player, "poder.milestone-habilidade", skill, nivelHabilidadeDepois);
        }

        boolean levelUp = result.isLeveledUp();

        if (levelUp) {
            showLevelUpActionbar(player, skill, result);
        } else {
            showXpActionbar(player, skill, amount);
        }

        boolean milestone = levelUp && result.getNewLevel() % 100 == 0;
        if (milestone) {
            announceLevelUp(player, skill, result);
        }
    }

    private void showXpActionbar(Player player, SkillType skill, double xpGanho) {
        if (levelUpActionbarTasks.containsKey(player.getUniqueId())) {
            return;
        }

        PlayerProfile profile = dataManager.getProfile(player.getUniqueId());
        int nivel = profile.getLevel(skill);
        String xpAtual;
        String xpNecessario;

        if (nivel >= levelingManager.getNivelMaximo()) {
            xpAtual = "MAX";
            xpNecessario = "MAX";
        } else {
            var skillData = profile.getData(skill);
            xpAtual = String.valueOf((int) Math.round(skillData.getCurrentXp()));
            xpNecessario = String.valueOf((int) Math.round(levelingManager.xpParaProximoNivel(nivel)));
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("habilidade", skill.getDisplayName());
        placeholders.put("xpGanho", String.valueOf((int) Math.round(xpGanho)));
        placeholders.put("xpAtual", xpAtual);
        placeholders.put("xpNecessario", xpNecessario);
        placeholders.put("nivel", String.valueOf(nivel));

        String mensagem = MessageUtil.placeholders(configManager.msg("actionbar.formato-progresso"), placeholders);
        ActionBarUtil.send(player, MessageUtil.colorize(mensagem));
    }

    private void showLevelUpActionbar(Player player, SkillType skill, LevelUpResult result) {
        UUID uuid = player.getUniqueId();

        BukkitTask previousTask = levelUpActionbarTasks.remove(uuid);
        if (previousTask != null) {
            previousTask.cancel();
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("habilidade", skill.getDisplayName());
        placeholders.put("nivel", String.valueOf(result.getNewLevel()));

        String mensagem = MessageUtil.placeholders(configManager.msg("level-up.actionbar"), placeholders);
        ActionBarUtil.send(player, MessageUtil.colorize(mensagem));

        long durationTicks = Math.max(1L, configManager.config().getLong("actionbar.duracao-level-up-ticks", 40L));
        BukkitTask task = Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("HabilidadesPlus"),
                () -> {
                    levelUpActionbarTasks.remove(uuid);
                    if (player.isOnline()) {
                        showCurrentXpActionbar(player, skill);
                    }
                },
                durationTicks
        );
        levelUpActionbarTasks.put(uuid, task);
    }

    private void showCurrentXpActionbar(Player player, SkillType skill) {
        PlayerProfile profile = dataManager.getProfile(player.getUniqueId());
        int nivel = profile.getLevel(skill);
        String xpAtual;
        String xpNecessario;

        if (nivel >= levelingManager.getNivelMaximo()) {
            xpAtual = "MAX";
            xpNecessario = "MAX";
        } else {
            var skillData = profile.getData(skill);
            xpAtual = String.valueOf((int) Math.round(skillData.getCurrentXp()));
            xpNecessario = String.valueOf((int) Math.round(levelingManager.xpParaProximoNivel(nivel)));
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("habilidade", skill.getDisplayName());
        placeholders.put("xpGanho", "0");
        placeholders.put("xpAtual", xpAtual);
        placeholders.put("xpNecessario", xpNecessario);
        placeholders.put("nivel", String.valueOf(nivel));

        String mensagem = MessageUtil.placeholders(configManager.msg("actionbar.formato-progresso"), placeholders);
        ActionBarUtil.send(player, MessageUtil.colorize(mensagem));
    }

    private void announcePowerMilestone(Player player, String messagePath, SkillType skill, int poder) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("poder", String.valueOf(poder));
        placeholders.put("jogador", player.getName());
        if (skill != null) {
            placeholders.put("habilidade", skill.getDisplayName());
        }

        String mensagem = MessageUtil.placeholders(configManager.msg(messagePath), placeholders);
        Bukkit.broadcastMessage(MessageUtil.colorize(mensagem));
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
        BukkitTask task = levelUpActionbarTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}
