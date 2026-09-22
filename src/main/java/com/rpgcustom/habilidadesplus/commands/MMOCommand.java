package com.rpgcustom.habilidadesplus.commands;

import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.gui.MMOMenu;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public class MMOCommand implements TabExecutor {

    private final DataManager dataManager;
    private final LevelingManager levelingManager;
    private final ConfigManager configManager;
    private final Runnable reloadAction;

    public MMOCommand(DataManager dataManager, LevelingManager levelingManager,
                      ConfigManager configManager, Runnable reloadAction) {
        this.dataManager = dataManager;
        this.levelingManager = levelingManager;
        this.configManager = configManager;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("habilidadesplus.admin")
                && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return List.of();
    }
}
