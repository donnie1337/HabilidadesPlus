package com.rpgcustom.habilidadesplus.xp;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import com.rpgcustom.habilidadesplus.leveling.LevelUpResult;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
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

}        boolean milestone = result.isLeveledUp() && result.getNewLevel() % 100 == 0;

        if (milestone) {
            announceLevelUp(player, skill, result);
        }
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
        pendingDisplay.remove(uuid);
    }
}
