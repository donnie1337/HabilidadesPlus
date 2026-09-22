package com.rpgcustom.habilidadesplus.leveling;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelingManagerTest {
    @Test
    void calculatesLinearCurve() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("nivelamento.curva", "LINEAR");
        config.set("nivelamento.xp-base", 100);
        config.set("nivelamento.multiplicador", 45);
        LevelingManager manager = new LevelingManager(config);

        assertEquals(100, manager.xpParaProximoNivel(0));
        assertEquals(550, manager.xpParaProximoNivel(10));
    }

    @Test
    void calculatesExponentialCurve() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("nivelamento.curva", "EXPONENCIAL");
        config.set("nivelamento.xp-base", 100);
        config.set("nivelamento.expoente", 2.0);
        LevelingManager manager = new LevelingManager(config);

        assertEquals(100, manager.xpParaProximoNivel(0));
        assertEquals(900, manager.xpParaProximoNivel(2));
    }
}
