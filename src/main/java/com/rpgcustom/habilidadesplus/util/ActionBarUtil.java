package com.rpgcustom.habilidadesplus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Envia texto para a action bar do jogador.
 * A Spigot API atual expoe Player#sendActionBar(Component) nativamente
 * (Adventure), entao nao e preciso usar mais o antigo ChatMessageType/spigot().
 */
public class ActionBarUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    private ActionBarUtil() {
    }

    public static void send(Player player, String legacyText) {
        Component component = SERIALIZER.deserialize(legacyText);
        player.sendActionBar(component);
    }
}
