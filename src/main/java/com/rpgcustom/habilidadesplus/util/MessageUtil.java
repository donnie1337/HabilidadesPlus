package com.rpgcustom.habilidadesplus.util;

import org.bukkit.ChatColor;

import java.util.Map;

public class MessageUtil {

    private MessageUtil() {
    }

    /**
     * Traduz codigos de cor '&' (usados em config.yml/messages.yml) para
     * o formato de secao usado por itens de GUI, nomes de inventario, etc.
     */
    public static String colorize(String texto) {
        return ChatColor.translateAlternateColorCodes('&', texto);
    }

    /**
     * Monta uma barra de progresso textual, ex: "██████░░░░" para 60%.
     */
    public static String barraDeProgresso(double atual, double necessario, int tamanho) {
        double percentual = necessario <= 0 ? 1.0 : Math.min(1.0, atual / necessario);
        int preenchido = (int) Math.round(percentual * tamanho);
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GREEN);
        for (int i = 0; i < tamanho; i++) {
            if (i == preenchido) {
                sb.append(ChatColor.GRAY);
            }
            sb.append(i < preenchido ? '█' : '░');
        }
        return sb.toString();
    }

    /**
     * Substitui placeholders no formato {chave} pelos valores do mapa.
     */
    public static String placeholders(String texto, Map<String, String> valores) {
        String resultado = texto;
        for (Map.Entry<String, String> entry : valores.entrySet()) {
            resultado = resultado.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return resultado;
    }
}
