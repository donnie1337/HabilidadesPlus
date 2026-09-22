package com.rpgcustom.habilidadesplus.gui;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import com.rpgcustom.habilidadesplus.data.PlayerSkillData;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import com.rpgcustom.habilidadesplus.util.ConfigManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constroi o inventario mostrado quando o jogador digita /mcmmo.
 */
public class MMOMenu {

    private static final int TAMANHO = 54;
    // Slots internos usados para os icones das habilidades (evita a borda)
    private static final int[] SLOTS_HABILIDADES = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31
    };
    private static final int SLOT_PODER = 49;

    private MMOMenu() {
    }

    public static void open(Player player, DataManager dataManager, LevelingManager levelingManager, ConfigManager configManager) {
        MMOMenuHolder holder = new MMOMenuHolder();
        Component titulo = LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.msg("gui.titulo-menu"));
        Inventory inventory = Bukkit.createInventory(holder, TAMANHO, titulo);
        holder.setInventory(inventory);

        PlayerProfile profile = dataManager.getProfile(player.getUniqueId());

        preencherBorda(inventory);

        SkillType[] skills = SkillType.values();
        for (int i = 0; i < skills.length && i < SLOTS_HABILIDADES.length; i++) {
            inventory.setItem(SLOTS_HABILIDADES[i], criarItemHabilidade(skills[i], profile, levelingManager, configManager));
        }

        inventory.setItem(SLOT_PODER, criarItemPoder(profile, configManager));

        player.openInventory(inventory);
    }

    private static void preencherBorda(Inventory inventory) {
        ItemStack vidro = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = vidro.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            vidro.setItemMeta(meta);
        }
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, vidro);
        }
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_PODER) continue;
            inventory.setItem(i, vidro);
        }
    }

    private static ItemStack criarItemHabilidade(SkillType skill, PlayerProfile profile, LevelingManager levelingManager, ConfigManager configManager) {
        PlayerSkillData data = profile.getData(skill);
        int nivelMaximo = levelingManager.getNivelMaximo();

        ItemStack item = new ItemStack(skill.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("&e&l" + skill.getDisplayName()));

            List<String> lore = new ArrayList<>();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("nivel", String.valueOf(data.getLevel()));
            lore.add(MessageUtil.colorize(MessageUtil.placeholders(configManager.msg("gui.item-nivel"), placeholders)));

            if (data.getLevel() >= nivelMaximo) {
                lore.add(MessageUtil.colorize(configManager.msg("gui.item-nivel-maximo")));
            } else {
                double necessario = levelingManager.xpParaProximoNivel(data.getLevel());

                Map<String, String> xpPlaceholders = new HashMap<>();
                xpPlaceholders.put("xpAtual", String.valueOf((int) data.getCurrentXp()));
                xpPlaceholders.put("xpNecessario", String.valueOf((int) necessario));
                lore.add(MessageUtil.colorize(MessageUtil.placeholders(configManager.msg("gui.item-xp"), xpPlaceholders)));

                String barra = MessageUtil.barraDeProgresso(data.getCurrentXp(), necessario, 20);
                int porcentagem = necessario <= 0 ? 100 : (int) Math.min(100, Math.round((data.getCurrentXp() / necessario) * 100));

                Map<String, String> progressoPlaceholders = new HashMap<>();
                progressoPlaceholders.put("barra", barra);
                progressoPlaceholders.put("porcentagem", String.valueOf(porcentagem));
                lore.add(MessageUtil.colorize(MessageUtil.placeholders(configManager.msg("gui.item-progresso"), progressoPlaceholders)));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack criarItemPoder(PlayerProfile profile, ConfigManager configManager) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(configManager.msg("gui.item-poder-nome")));

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("poder", String.valueOf(profile.getPowerLevel()));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize(MessageUtil.placeholders(configManager.msg("gui.item-poder-lore"), placeholders)));
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }
}
