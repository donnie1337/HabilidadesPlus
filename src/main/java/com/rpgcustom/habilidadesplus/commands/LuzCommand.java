package com.rpgcustom.habilidadesplus.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LuzCommand implements CommandExecutor {

    private static final int DURACAO = 220;
    private final Set<UUID> ativos = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        if (!player.hasPermission("habilidadesplus.use")) {
            player.sendMessage("§cVoce nao tem permissao para usar este comando.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (ativos.remove(uuid)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.sendActionBar(Component.text("§7Visão noturna: §cdesativada"));
        } else {
            ativos.add(uuid);
            aplicarVisaoNoturna(player);
            player.sendActionBar(Component.text("§aVisão noturna: §2ativada"));
        }
        return true;
    }

    public void atualizarActionBars() {
        for (UUID uuid : Set.copyOf(ativos)) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                ativos.remove(uuid);
                continue;
            }
            aplicarVisaoNoturna(player);
            player.sendActionBar(Component.text("§aVisão noturna: §2ativada"));
        }
    }

    public void desativarTodos() {
        for (UUID uuid : Set.copyOf(ativos)) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        }
        ativos.clear();
    }

    private void aplicarVisaoNoturna(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION,
                DURACAO,
                0,
                false,
                false,
                false
        ));
    }
}
