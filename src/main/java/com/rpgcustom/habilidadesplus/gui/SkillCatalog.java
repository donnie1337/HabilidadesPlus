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
            case MINERACAO -> d("Extraia recursos das profundezas com eficiência e precisão.", implemented("Veio Farto",1,Material.COAL_ORE,"Chance de dobrar o drop de minérios ao minerar. Aumenta 0,1% por nível de Mineração."), active("Super Quebrador",10,Material.DIAMOND_PICKAXE,"Aumenta a eficiência de mineração por um período progressivo conforme o nível de Mineração. No nível 100+, também pode triplicar o drop."), implemented("Mineração Precisa",75,Material.IRON_PICKAXE,"Chance de preservar a durabilidade da picareta ao minerar."), p("Prospector",200,Material.GOLD_ORE,"Aumenta a chance de encontrar recursos e minérios raros ao minerar."));
            case LENHADOR -> d("Domine a madeira e cuide da floresta enquanto colhe.",
                    implemented("Double Drop",1,Material.OAK_LOG,"Chance de receber madeira adicional: +0,05% por nível, até 50% de chance máxima."),
                    implemented("Tree Feller",25,Material.IRON_AXE,"Derruba os troncos conectados da árvore de uma vez. Segure SHIFT para cortar tronco por tronco."),
                    implemented("Leaf Cutter",1,Material.OAK_LEAVES,"Faz as folhas da árvore desaparecerem mais rapidamente após a colheita."),
                    implemented("Machado Reforçado",1,Material.DIAMOND_AXE,"Passiva de durabilidade: 0,05% de chance por nível de preservar o desgaste do machado."),
                    implemented("Colheita Eficiente",100,Material.NETHERITE_AXE,"A partir do nível 100, adiciona +0,025% por nível à chance de preservar o machado, acumulando com Machado Reforçado."),
                    implemented("Crítico do Lenhador",500,Material.DIAMOND,"No nível 500: 0,15% de chance. Aumenta +0,05 ponto percentual a cada 100 níveis, até 0,50%."),
                    implemented("Combo de Corte",150,Material.GOLDEN_AXE,"Cortar árvores completas consecutivamente em até 10 segundos aumenta temporariamente o XP da árvore. Cada combo adiciona +5% de XP, até +20%."),
                    implemented("Replantio Automático",10,Material.OAK_SAPLING,"Passiva desbloqueada no nível 10. Ao derrubar uma árvore completa, há chance de replantar automaticamente a muda correspondente; chega a 100% no nível 1000."));
            case ESCAVACAO -> d("Encontre materiais escondidos e abra caminho pelo terreno.", p("Peneira Rápida",5,Material.GRAVEL,"Chance de encontrar itens úteis ao escavar."),p("Passo de Toupeira",30,Material.IRON_SHOVEL,"Aumenta temporariamente a velocidade de escavação."));
            case ERVANISMO -> d("Transforme plantações em colheitas mais produtivas.", p("Colheita Viva",5,Material.WHEAT,"Chance de colher produtos extras."),p("Sementes de Retorno",20,Material.WHEAT_SEEDS,"Replanta culturas maduras automaticamente."),p("Jardim Próspero",55,Material.BONE_MEAL,"Aumenta a chance de crescimento acelerado."));
            case PESCA -> d("Pesque com técnica e encontre tesouros nas águas.", p("Isca de Sorte",5,Material.FISHING_ROD,"Melhora levemente a chance de tesouros."),p("Linha Firme",25,Material.TRIPWIRE_HOOK,"Reduz a chance de perder a pesca."),p("Maré Generosa",60,Material.HEART_OF_THE_SEA,"Chance de ganhar uma pesca adicional."));
            case ALQUIMIA -> d("Prepare poções com maior rendimento e estabilidade.", p("Mistura Estável",5,Material.BREWING_STAND,"Reduz o risco de desperdício no preparo."),p("Essência Densa",30,Material.GLOWSTONE_DUST,"Aumenta a duração de poções próprias.")); 
            case FUNDICAO -> d("Aprimore fornalhas e aproveite melhor cada recurso fundido.", p("Brasa Eficiente",10,Material.COAL,"Pequena economia de combustível."),p("Liga Refinada",35,Material.IRON_INGOT,"Chance de receber um item fundido extra."),p("Forja Acelerada",70,Material.FURNACE,"Aumenta a velocidade de fundição."));
            case ESPADAS -> d("Lute com controle, dano contínuo e precisão corpo a corpo.", p("Corte Profundo",5,Material.IRON_SWORD,"Chance de aplicar sangramento breve."),p("Arco de Lâmina",15,Material.DIAMOND_SWORD,"Atinge inimigos próximos em um golpe especial."),p("Guarda Reversa",50,Material.SHIELD,"Chance de reduzir e devolver parte do dano."));
            case MACHADOS -> d("Use golpes pesados para abrir a defesa dos inimigos.", p("Impacto Brutal",5,Material.IRON_AXE,"Chance de aplicar lentidão curta ao alvo."),p("Fenda de Guarda",25,Material.IRON_AXE,"Causa dano extra contra inimigos protegidos."),p("Golpe do Carrasco",65,Material.NETHERITE_AXE,"Aumenta o dano contra alvos com pouca vida."));
            case ARQUERIA -> d("Acerte à distância com disparos mais precisos.", p("Mira Serena",5,Material.BOW,"Aumenta o dano de flechas em longas distâncias."),p("Flecha Pesada",25,Material.ARROW,"Chance de aplicar lentidão ao acertar."),p("Tiro Perfurante",60,Material.SPECTRAL_ARROW,"Permite atravessar um alvo ocasionalmente."));
            case ACROBACIA -> d("Movimente-se com agilidade e sobreviva a quedas perigosas.", active("Rolamento",5,Material.FEATHER,"Chance de anular totalmente o dano de queda."),p("Esquiva",35,Material.RABBIT_FOOT,"Chance de reduzir pela metade um ataque recebido."));
            case DESARMADO -> d("Use os punhos para controlar inimigos e sobreviver ao combate.", p("Punho de Pedra",5,Material.LEATHER,"Aumenta levemente o dano desarmado."),p("Desvio Rápido",30,Material.IRON_NUGGET,"Chance de empurrar o inimigo ao atacar."));
            case DOMESTICACAO -> d("Fortaleça seus companheiros e mantenha-os protegidos.", p("Vínculo Fiel",5,Material.BONE,"Pets causam um pouco mais de dano."),p("Instinto Protetor",30,Material.WOLF_ARMOR,"Pets recebem menos dano."),p("Chamado Selvagem",65,Material.WOLF_SPAWN_EGG,"Aliados próximos recebem um breve bônus."));
            case REPARACAO -> d("Recupere ferramentas e armaduras usando menos recursos.", p("Oficina Cuidadosa",5,Material.ANVIL,"Reparos recuperam um pouco mais de durabilidade."),p("Têmpera Durável",40,Material.IRON_INGOT,"Chance de preservar parte do material usado."));
            case CLAVA -> d("Esmague inimigos com impactos fortes e controle de área.", p("Choque de Solo",10,Material.MACE,"Impactos têm chance de empurrar inimigos próximos."),p("Peso da Queda",45,Material.ANVIL,"Aumenta o dano de ataques em queda."));
            case TRIDENTES -> d("Controle distância e água com arremessos precisos.", p("Arpão Firme",10,Material.TRIDENT,"Aumenta o dano de tridentes arremessados."),p("Correnteza",45,Material.PRISMARINE_CRYSTALS,"Atingir na água aplica lentidão ao alvo."));
            case BESTAS -> d("Dispare virotes com força e alcance calculados.", p("Mecanismo Reforçado",5,Material.CROSSBOW,"Aumenta levemente o dano dos virotes."),p("Virote de Impacto",35,Material.ARROW,"Chance de aplicar recuo adicional."),p("Salva Perfurante",70,Material.FIREWORK_ROCKET,"Disparos especiais atravessam inimigos."));
            case LANCAS -> d("Use alcance e investidas para dominar o combate.", p("Estocada Firme",5,Material.STONE_SPEAR,"Aumenta o dano em ataques de lança."),p("Passo de Investida",35,Material.FEATHER,"Golpes em movimento causam dano adicional."));
        };
    }
    private static Definition d(String description, Power... powers){return new Definition(description,List.of(powers));}
    private static Power p(String name,int level,Material icon,String description){return new Power(name,level,icon,description,false);}
    private static Power active(String name,int level,Material icon,String description){return new Power(name,level,icon,description,true);}
    private static Power implemented(String name,int level,Material icon,String description){return new Power(name,level,icon,description,true);}
}
