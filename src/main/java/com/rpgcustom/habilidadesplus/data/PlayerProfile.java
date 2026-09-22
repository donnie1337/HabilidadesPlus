package com.rpgcustom.habilidadesplus.data;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.leveling.LevelUpResult;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Todo o progresso de um jogador: uma PlayerSkillData por habilidade.
 */
public class PlayerProfile {

    private final UUID uuid;
    private final Map<SkillType, PlayerSkillData> skills = new EnumMap<>(SkillType.class);

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        for (SkillType type : SkillType.values()) {
            skills.put(type, new PlayerSkillData());
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public PlayerSkillData getData(SkillType type) {
        return skills.get(type);
    }

    public int getLevel(SkillType type) {
        return skills.get(type).getLevel();
    }

    public void setLevel(SkillType type, int level) {
        skills.get(type).setLevel(level);
        skills.get(type).setCurrentXp(0);
    }

    public int getPowerLevel() {
        int total = 0;
        for (PlayerSkillData data : skills.values()) {
            total += data.getLevel();
        }
        return total;
    }

    /**
     * Adiciona XP a uma habilidade e aplica quantos level ups forem necessarios.
     * Retorna um LevelUpResult indicando se e quantos niveis foram ganhos.
     */
    public LevelUpResult addXp(SkillType type, double amount, LevelingManager levelingManager) {
        PlayerSkillData data = skills.get(type);
        int maxLevel = levelingManager.getNivelMaximo();

        if (data.getLevel() >= maxLevel) {
            // Ja esta no nivel maximo, nao acumula XP extra
            return new LevelUpResult(false, 0, data.getLevel());
        }

        data.addXp(amount);

        int levelsGained = 0;
        while (data.getLevel() < maxLevel) {
            double necessario = levelingManager.xpParaProximoNivel(type, data.getLevel());
            if (data.getCurrentXp() >= necessario) {
                data.setCurrentXp(data.getCurrentXp() - necessario);
                data.setLevel(data.getLevel() + 1);
                levelsGained++;
            } else {
                break;
            }
        }

        if (data.getLevel() >= maxLevel) {
            data.setLevel(maxLevel);
            data.setCurrentXp(0);
        }

        return new LevelUpResult(levelsGained > 0, levelsGained, data.getLevel());
    }
}
