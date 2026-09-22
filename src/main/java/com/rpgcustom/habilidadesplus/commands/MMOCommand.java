package com.rpgcustom.habilidadesplus.commands;

import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.gui.MMOMenu;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import com.rpgcustom.habilidadesplus.xp.XpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MMOCommand implements CommandExecutor {

    private final DataManager dataManager;
    private final LevelingManager levelingManager;
    private final ConfigManager configManager;
    private final XpManager xpManager;

    public MMOCommand(DataManager dataManager, LevelingManager levelingManager, ConfigManager configManager, XpManager xpManager) {
        this.dataManager = dataManager;
        this.levelingManager = levelingManager;
        this.configManager = configManager;
        this.xpManager = xpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("habilidadesplus.admin")) {
                sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.sem-permissao")));
                return true;
            }
            configManager.load();
            levelingManager.reload(configManager.config());
            xpManager.startTask();
            sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.reload-sucesso")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(configManager.msg("comandos.apenas-jogador")));
            return true;
        }

        MMOMenu.open(player, dataManager, levelingManager);
        return true;
    }
}
