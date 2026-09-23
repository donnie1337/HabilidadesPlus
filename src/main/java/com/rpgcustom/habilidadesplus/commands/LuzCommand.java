package com.rpgcustom.habilidadesplus.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LuzCommand implements CommandExecutor, Listener {

    private static final int DURACAO = PotionEffect.INFINITE_DURATION;
    private static final int PISCADAS = 3;
    private static final long ACTION_BAR_DURATION_TICKS = 40L;
    private static final String MENSAGEM_LEITE = "Não apague sua luz para caber no mundo de ninguém.";
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
            mostrarActionBar(player, "§cLuz noturna desativada");
        } else {
            ativos.add(uuid);
            aplicarVisaoNoturna(player);
            iniciarPiscadas(player);
            mostrarActionBar(player, "§aLuz noturna ativada");
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoTomarLeite(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.MILK_BUCKET) return;

        Player player = event.getPlayer();
        if (!ativos.contains(player.getUniqueId())) return;

        Bukkit.getScheduler().runTask(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    if (!ativos.contains(player.getUniqueId()) || !player.isOnline()) return;
                    aplicarVisaoNoturna(player);
                    enviarMensagemGlobal(player);
                }
        );
    }

    private void enviarMensagemGlobal(Player player) {
        Plugin chatPlus = Bukkit.getPluginManager().getPlugin("ChatPlus");
        if (chatPlus != null && chatPlus.isEnabled()) {
            try {
                Method method = chatPlus.getClass().getMethod("forceGlobalMessage", Player.class, String.class);
                method.invoke(chatPlus, player, MENSAGEM_LEITE);
                return;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Cai para o broadcast simples caso a API do ChatPlus não esteja disponível.
            }
        }
        Bukkit.broadcastMessage("§7[G]§r §f" + MENSAGEM_LEITE);
    }

    public void atualizarActionBars() {
        for (UUID uuid : Set.copyOf(ativos)) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                ativos.remove(uuid);
                continue;
            }
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

    private void mostrarActionBar(Player player, String mensagem) {
        player.sendActionBar(Component.text(mensagem));
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                () -> player.sendActionBar(Component.empty()),
                ACTION_BAR_DURATION_TICKS
        );
    }

    private void iniciarPiscadas(Player player) {
        org.bukkit.plugin.java.JavaPlugin plugin = org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass());
        for (int i = 0; i < PISCADAS; i++) {
            long removerEm = 2L + (i * 4L);
            long aplicarEm = removerEm + 1L;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (ativos.contains(player.getUniqueId()) && player.isOnline()) {
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                }
            }, removerEm);
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (ativos.contains(player.getUniqueId()) && player.isOnline()) {
                    aplicarVisaoNoturna(player);
                }
            }, aplicarEm);
        }
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