package com.rpgcustom.habilidadesplus.commands;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import com.rpgcustom.habilidadesplus.gui.MMOMenu;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import com.rpgcustom.habilidadesplus.top1.Top1SkillService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MMOCommand implements TabExecutor {

    private final DataManager dataManager;
    private final LevelingManager levelingManager;
    private final ConfigManager configManager;
    private final Top1SkillService top1SkillService;
    private final Runnable reloadAction;

    public MMOCommand(DataManager dataManager, LevelingManager levelingManager,
                      ConfigManager configManager, Top1SkillService top1SkillService, Runnable reloadAction) {
        this.dataManager = dataManager;
        this.levelingManager = levelingManager;
        this.configManager = configManager;
        this.top1SkillService = top1SkillService;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("habilidadesplus.admin")) {
                sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.sem-permissao")));
                return true;
            }
            reloadAction.run();
            sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.reload-sucesso")));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("setnivel")) {
            return handleSetNivel(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.apenas-jogador")));
            return true;
        }

        if (!player.hasPermission("habilidadesplus.use")) {
            player.sendMessage(MessageUtil.colorize(configManager.msg("comandos.sem-permissao")));
            return true;
        }

        MMOMenu.open(player, dataManager, levelingManager, configManager);
        return true;
    }

    private boolean handleSetNivel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("habilidadesplus.admin")) {
            sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.sem-permissao")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.apenas-jogador")));
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.setnivel-uso")));
            return true;
        }

        SkillType skill = findSkill(args[1]);
        if (skill == null) {
            sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.setnivel-habilidade-invalida")));
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException ignored) {
            sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.setnivel-nivel-invalido")));
            return true;
        }

        if (level < 0 || level > levelingManager.getNivelMaximo()) {
            sender.sendMessage(MessageUtil.placeholders(
                    MessageUtil.colorize(configManager.msg("comandos.setnivel-fora-do-limite")),
                    java.util.Map.of("maximo", String.valueOf(levelingManager.getNivelMaximo()))
            ));
            return true;
        }

        PlayerProfile profile = dataManager.getProfile(player.getUniqueId());
        profile.setLevel(skill, level);
        dataManager.markDirty(player.getUniqueId());
        top1SkillService.invalidate();

        sender.sendMessage(MessageUtil.placeholders(
                MessageUtil.colorize(configManager.msg("comandos.setnivel-sucesso")),
                java.util.Map.of(
                        "habilidade", skill.getDisplayName(),
                        "nivel", String.valueOf(level)
                )
        ));
        return true;
    }

    private SkillType findSkill(String input) {
        for (SkillType skill : SkillType.values()) {
            if (skill.name().equalsIgnoreCase(input)
                    || skill.getDisplayName().equalsIgnoreCase(input)) {
                return skill;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("habilidadesplus.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> options = List.of("reload", "setnivel");
            return options.stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setnivel")) {
            String input = args[1].toLowerCase(Locale.ROOT);
            List<String> skills = new ArrayList<>();
            for (SkillType skill : SkillType.values()) {
                if (skill.name().toLowerCase(Locale.ROOT).startsWith(input)
                        || skill.getDisplayName().toLowerCase(Locale.ROOT).startsWith(input)) {
                    skills.add(skill.name().toLowerCase(Locale.ROOT));
                }
            }
            return skills;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setnivel")) {
            return List.of("<nivel>");
        }

        return List.of();
    }
}
