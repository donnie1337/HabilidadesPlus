package com.rpgcustom.habilidadesplus.data;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.leveling.LevelUpResult;
import com.rpgcustom.habilidadesplus.leveling.LevelingManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProfileTest {
    @Test
    void appliesMultipleLevelUpsAndCarriesRemainingXp() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("nivelamento.curva", "LINEAR");
        config.set("nivelamento.xp-base", 100);
        config.set("nivelamento.multiplicador", 50);
        config.set("nivelamento.nivel-maximo", 100);
        LevelingManager levels = new LevelingManager(config);
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID());

        LevelUpResult result = profile.addXp(SkillType.PESCA, 275, levels);

        assertTrue(result.isLeveledUp());
        assertEquals(2, result.getLevelsGained());
        assertEquals(2, profile.getLevel(SkillType.PESCA));
        assertEquals(25, profile.getData(SkillType.PESCA).getCurrentXp());
    }

    @Test
    void stopsAtMaximumLevelAndClearsOverflow() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("nivelamento.curva", "LINEAR");
        config.set("nivelamento.xp-base", 10);
        config.set("nivelamento.multiplicador", 0);
        config.set("nivelamento.nivel-maximo", 2);
        LevelingManager levels = new LevelingManager(config);
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID());

        profile.addXp(SkillType.PESCA, 100, levels);

        assertEquals(2, profile.getLevel(SkillType.PESCA));
        assertEquals(0, profile.getData(SkillType.PESCA).getCurrentXp());
    }
}
