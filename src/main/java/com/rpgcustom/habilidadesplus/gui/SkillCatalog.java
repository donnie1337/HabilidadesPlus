package com.rpgcustom.habilidadesplus.gui;

import com.rpgcustom.habilidadesplus.SkillType;
import org.bukkit.Material;

import java.util.List;

public final class SkillCatalog {
    private SkillCatalog() {}

    public record Power(String name, int level, Material icon, String description, boolean implemented) {}
    public record Definition(String description, List<Power> powers) {}

    public static Definition definition(SkillType skill) {
        return switch (skill) {
            case MINERACAO -> d("Extraia recursos das profundezas com eficiência e precisão.", implemented("Veio Farto",1,Material.COAL_ORE,"Chance de duplicar o minério ao quebrá-lo."), active("Super Quebrador",25,Material.DIAMOND_PICKAXE,"Aumenta a eficiência de mineração por um período progressivo conforme o nível de Mineração. No nível 100+, também pode triplicar o drop."), implemented("Mineração Precisa",75,Material.IRON_PICKAXE,"Chance de preservar a durabilidade da picareta ao minerar."), p("Prospector",150,Material.GOLD_ORE,"Aumenta a chance de encontrar recursos e minérios raros ao minerar."));
            case LENHADOR -> d("Domine a madeira e cuide da floresta enquanto colhe.",
                    implemented("Duplo Drop",1,Material.OAK_LOG,"Chance de receber madeira adicional: +0,05% por nível, até 50% de chance máxima."),
                    implemented("Derrubada de Árvores",25,Material.IRON_AXE,"Derruba os troncos conectados da árvore de uma vez e ativa o Leaf Cutter: as folhas desaparecem progressivamente conforme o nível. Segure SHIFT para cortar tronco por tronco."),
                    implemented("Machado Reforçado",1,Material.DIAMOND_AXE,"Passiva de durabilidade: 0,05% de chance por nível de preservar o desgaste do machado."),
                    implemented("Colheita Eficiente",50,Material.NETHERITE_AXE,"A partir do nível 100, adiciona +0,025% por nível à chance de preservar o machado, acumulando com Machado Reforçado."),
                    implemented("Crítico do Lenhador",150,Material.DIAMOND,"No nível 500: 0,15% de chance de receber um recurso raro. Aumenta +0,05 ponto percentual a cada 100 níveis, até 0,40% no nível 1000."),
                    implemented("Combo de Corte",100,Material.GOLDEN_AXE,"Cortar árvores completas consecutivamente ativa estágios de combo. Cada estágio adiciona +5% de XP, até +20%."),
                    implemented("Replantio Automático",10,Material.OAK_SAPLING,"Passiva desbloqueada no nível 10. Ao derrubar uma árvore completa, há chance de replantar automaticamente a muda correspondente; chega a 100% no nível 1000."));
            case ESCAVACAO -> d("Encontre materiais escondidos e abra caminho pelo terreno.",
                    implemented("Duplo Drop",1,Material.GRAVEL,"Chance de receber o drop natural em dobro ao escavar."),
                    active("Super Escavação",25,Material.DIAMOND_SHOVEL,"Aumenta a eficiência de escavação por um período progressivo conforme o nível de Escavação. Em níveis mais altos, também pode potencializar os drops."),
                    implemented("Arqueologia",10,Material.BRUSH,"Aumenta a chance de encontrar tesouros ao escavar."),
                    implemented("Tesouro Raro",100,Material.GOLD_INGOT,"Desbloqueia tesouros raros nas tabelas de Arqueologia."),
                    implemented("Mestre da Escavação",150,Material.NETHERITE_SHOVEL,"Aumenta ainda mais a chance de encontrar tesouros."));
            case ERVANISMO -> d("Colete plantas, domine as plantações e espalhe a vida pelo mundo.",
                    implemented("Duplo Drop",50,Material.WHEAT,"Chance de receber o dobro dos drops de Herbalismo."),
                    active("Terra Verde",10,Material.WHEAT,"Habilidade ativa com enxada: melhora o replantio e pode conceder orbes de XP aleatórios ao colher."),
                    implemented("Dieta de Fazendeiro",200,Material.BREAD,"Aumenta a fome restaurada por alimentos cultivados."),
                    implemented("Polegar Verde",100,Material.WHEAT_SEEDS,"Replanta automaticamente toda plantação madura após a colheita."),
                    implemented("Sorte de Hylian",75,Material.IRON_SWORD,"Chance de encontrar tesouros ao cortar pequenas plantas com uma espada."),
                    implemented("Colheita Verdejante",125,Material.GOLDEN_CARROT,"Aumenta em 15% o XP de Herbalismo recebido ao colher plantas maduras."));
            case PESCA -> d("Pesque com técnica e encontre tesouros nas águas.", p("Isca de Sorte",5,Material.FISHING_ROD,"Melhora levemente a chance de tesouros."),p("Linha Firme",30,Material.TRIPWIRE_HOOK,"Reduz a chance de perder a pesca."),p("Maré Generosa",75,Material.HEART_OF_THE_SEA,"Chance de ganhar uma pesca adicional."));
            case ALQUIMIA -> d("Prepare poções com maior rendimento e estabilidade.", p("Mistura Estável",10,Material.BREWING_STAND,"Reduz o risco de desperdício no preparo."),p("Essência Densa",75,Material.GLOWSTONE_DUST,"Aumenta a duração de poções próprias."));
            case FUNDICAO -> d("Aprimore fornalhas e aproveite melhor cada recurso fundido.", p("Brasa Eficiente",10,Material.COAL,"Pequena economia de combustível."),p("Liga Refinada",50,Material.IRON_INGOT,"Chance de receber um item fundido extra."),p("Forja Acelerada",100,Material.FURNACE,"Aumenta a velocidade de fundição."));
            case ESPADAS -> d("Lute com controle, dano contínuo e precisão corpo a corpo.", p("Corte Profundo",10,Material.IRON_SWORD,"Chance de aplicar sangramento breve."),p("Arco de Lâmina",40,Material.DIAMOND_SWORD,"Atinge inimigos próximos em um golpe especial."),p("Guarda Reversa",100,Material.SHIELD,"Chance de reduzir e devolver parte do dano."));
            case MACHADOS -> d("Use golpes pesados para abrir a defesa dos inimigos.", p("Impacto Brutal",10,Material.IRON_AXE,"Chance de aplicar lentidão curta ao alvo."),p("Fenda de Guarda",50,Material.IRON_AXE,"Causa dano extra contra inimigos protegidos."),p("Golpe do Carrasco",100,Material.NETHERITE_AXE,"Aumenta o dano contra alvos com pouca vida."));
            case ARQUERIA -> d("Acerte à distância com disparos mais precisos.", p("Mira Serena",10,Material.BOW,"Aumenta o dano de flechas em longas distâncias."),p("Flecha Pesada",40,Material.ARROW,"Chance de aplicar lentidão ao acertar."),p("Tiro Perfurante",100,Material.SPECTRAL_ARROW,"Permite atravessar um alvo ocasionalmente."));
            case ACROBACIA -> d("Movimente-se com agilidade e sobreviva a quedas perigosas.", active("Rolamento",10,Material.FEATHER,"Chance de anular totalmente o dano de queda."),p("Esquiva",75,Material.RABBIT_FOOT,"Chance de reduzir pela metade um ataque recebido."));
            case DESARMADO -> d("Use os punhos para controlar inimigos e sobreviver ao combate.", p("Punho de Pedra",10,Material.LEATHER,"Aumenta levemente o dano desarmado."),p("Desvio Rápido",75,Material.IRON_NUGGET,"Chance de empurrar o inimigo ao atacar."));
            case DOMESTICACAO -> d("Fortaleça seus companheiros e mantenha-os protegidos.", p("Vínculo Fiel",10,Material.BONE,"Pets causam um pouco mais de dano."),p("Instinto Protetor",50,Material.WOLF_ARMOR,"Pets recebem menos dano."),p("Chamado Selvagem",100,Material.WOLF_SPAWN_EGG,"Aliados próximos recebem um breve bônus."));
            case REPARACAO -> d("Recupere ferramentas e armaduras usando menos recursos.", p("Oficina Cuidadosa",10,Material.ANVIL,"Reparos recuperam um pouco mais de durabilidade."),p("Têmpera Durável",75,Material.IRON_INGOT,"Chance de preservar parte do material usado."));
            case CLAVA -> d("Esmague inimigos com impactos fortes e controle de área.", p("Choque de Solo",15,Material.MACE,"Impactos têm chance de empurrar inimigos próximos."),p("Peso da Queda",75,Material.ANVIL,"Aumenta o dano de ataques em queda."));
            case TRIDENTES -> d("Controle distância e água com arremessos precisos.", p("Arpão Firme",15,Material.TRIDENT,"Aumenta o dano de tridentes arremessados."),p("Correnteza",75,Material.PRISMARINE_CRYSTALS,"Atingir na água aplica lentidão ao alvo."));
            case BESTAS -> d("Dispare virotes com força e alcance calculados.", p("Mecanismo Reforçado",10,Material.CROSSBOW,"Aumenta levemente o dano dos virotes."),p("Virote de Impacto",50,Material.ARROW,"Chance de aplicar recuo adicional."),p("Salva Perfurante",100,Material.FIREWORK_ROCKET,"Disparos especiais atravessam inimigos."));
            case LANCAS -> d("Use alcance e investidas para dominar o combate.", p("Estocada Firme",10,Material.STONE_SPEAR,"Aumenta o dano em ataques de lança."),p("Passo de Investida",75,Material.FEATHER,"Golpes em movimento causam dano adicional."));
        };
    }

    private static Definition d(String description, Power... powers) {
        List<Power> sorted = List.of(powers).stream()
                .sorted(java.util.Comparator.comparingInt(Power::level).thenComparing(Power::name))
                .toList();
        return new Definition(description, sorted);
    }

    private static Power p(String name,int level,Material icon,String description){return new Power(name,level,icon,description,false);}
    private static Power active(String name,int level,Material icon,String description){return new Power(name,level,icon,description,true);}
    private static Power implemented(String name,int level,Material icon,String description){return new Power(name,level,icon,description,true);}
}