package com.rpgcustom.habilidadesplus.gui;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import com.rpgcustom.habilidadesplus.data.PlayerSkillData;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import com.rpgcustom.habilidadesplus.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public final class MMOMenu {
    private static final int[] SLOTS={10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31};
    private MMOMenu(){}

    public static void open(Player player,DataManager data,LevelingManager levels){
        MMOMenuHolder holder=new MMOMenuHolder(null);
        Inventory inv=Bukkit.createInventory(holder,54,MessageUtil.colorize("&8Habilidades"));
        holder.setInventory(inv); PlayerProfile profile=data.getProfile(player.getUniqueId());
        SkillType[] skills=SkillType.values();
        for(int i=0;i<skills.length&&i<SLOTS.length;i++)inv.setItem(SLOTS[i],skillItem(skills[i],profile,levels));
        inv.setItem(49,profileItem(player,profile)); player.openInventory(inv);
    }
    public static void openSkill(Player player,SkillType skill,DataManager data,LevelingManager levels){
        MMOMenuHolder holder=new MMOMenuHolder(skill);
        Inventory inv=Bukkit.createInventory(holder,54,MessageUtil.colorize("&8Habilidades &8• &b"+skill.getDisplayName()));
        holder.setInventory(inv); PlayerSkillData current=data.getProfile(player.getUniqueId()).getData(skill);
        List<SkillCatalog.Power> powers=SkillCatalog.definition(skill).powers();
        for(int i=0;i<powers.size()&&i<SLOTS.length;i++)inv.setItem(SLOTS[i],powerItem(powers.get(i),current.getLevel()));
        inv.setItem(45,item(Material.ARROW,"&cVoltar",List.of("","&7Clique para voltar às habilidades."))); player.openInventory(inv);
    }
    public static SkillType skillAtSlot(int slot) {
        for (int index = 0; index < SLOTS.length; index++) {
            if (SLOTS[index] == slot) {
                return SkillType.values()[index];
            }
        }
        return null;
    }

    private static ItemStack skillItem(SkillType skill,PlayerProfile profile,LevelingManager levels){
        PlayerSkillData d=profile.getData(skill); double need=levels.xpParaProximoNivel(d.getLevel()); SkillCatalog.Definition def=SkillCatalog.definition(skill);
        List<String> lore=new ArrayList<>(); lore.add(""); lore.addAll(wrap("&7"+def.description())); lore.add(""); lore.add("&7Progressão: &a"+number(d.getCurrentXp())+"&7/&a"+number(need)+" XP"); lore.add("&a"+bar(d.getCurrentXp(),need)); lore.add(""); lore.add("&7Poderes:");
        for(SkillCatalog.Power p:def.powers()) lore.add((d.getLevel()>=p.level()?"&a":"&8")+"• &7Nível &f"+p.level()+"&7: "+(d.getLevel()>=p.level()?"&a":"&8")+p.name()+(d.getLevel()>=p.level()?" &a✓":" &8✖"));
        lore.add("");lore.add("&aClique para ver os poderes.");
        return item(skill.getIcon(),"&b"+skill.getDisplayName()+" &8• &7Nível &a"+d.getLevel(),lore);
    }
    private static ItemStack powerItem(SkillCatalog.Power p,int level){
        boolean unlocked=level>=p.level(); List<String> lore=new ArrayList<>(); lore.add("");lore.addAll(wrap("&7"+p.description()));lore.add("");lore.add("&7Desbloqueio: "+(unlocked?"&a":"&c")+"Nível "+p.level());lore.add(unlocked?"&a✓ Poder desbloqueado":"&8✖ Poder bloqueado");
        return item(p.icon(),(unlocked?"&a":"&8")+p.name(),lore);
    }
    private static ItemStack profileItem(Player p,PlayerProfile profile){
        ItemStack head=new ItemStack(Material.PLAYER_HEAD); SkullMeta meta=(SkullMeta)head.getItemMeta(); if(meta!=null){meta.setOwningPlayer(p);meta.setDisplayName(MessageUtil.colorize("&b"+p.getName()+" &8• &7Poder &a"+profile.getPowerLevel()));List<String> lore=new ArrayList<>();lore.add("");lore.add("&7Níveis das habilidades:");for(SkillType s:SkillType.values())lore.add("&8• &7"+s.getDisplayName()+": &a"+profile.getLevel(s));meta.setLore(color(lore));head.setItemMeta(meta);}return head;
    }
    private static ItemStack item(Material m,String name,List<String> lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();if(meta!=null){meta.setDisplayName(MessageUtil.colorize(name));meta.setLore(color(lore));i.setItemMeta(meta);}return i;}
    private static List<String> color(List<String> lines){return lines.stream().map(MessageUtil::colorize).toList();}
    private static List<String> wrap(String text){List<String> lines=new ArrayList<>();StringBuilder line=new StringBuilder();for(String w:text.split(" ")){if(line.length()+w.length()>32){lines.add(line.toString());line.setLength(0);}if(!line.isEmpty())line.append(" ");line.append(w);}if(!line.isEmpty())lines.add(line.toString());return lines;}
    private static String number(double n){return n>=1000?String.format(java.util.Locale.US,"%.1fk",n/1000).replace(".0k","k"):String.valueOf((int)n);}
    private static String bar(double current,double needed){int filled=needed<=0?20:(int)Math.min(20,Math.round(current/needed*20));return "■".repeat(filled)+"&8"+"■".repeat(20-filled);}
}