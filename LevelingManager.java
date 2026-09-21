package com.rpgcustom.habilidadesplus.leveling;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Responsavel por calcular quanto XP e necessario para ir de um nivel
 * para o proximo, de acordo com a curva escolhida no config.yml.
 */
public class LevelingManager {

    private LevelCurve curve;
    private double xpBase;
    private double multiplicador;
    private double expoente;
    private int nivelMaximo;

    public LevelingManager(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        this.curve = LevelCurve.fromConfig(config.getString("nivelamento.curva"), LevelCurve.LINEAR);
        this.xpBase = config.getDouble("nivelamento.xp-base", 100);
        this.multiplicador = config.getDouble("nivelamento.multiplicador", 45);
        this.expoente = config.getDouble("nivelamento.expoente", 1.8);
        this.nivelMaximo = config.getInt("nivelamento.nivel-maximo", 100);
    }

    /**
     * XP necessario para sair de currentLevel e chegar em currentLevel + 1.
     */
    public double xpParaProximoNivel(int currentLevel) {
        return switch (curve) {
            case LINEAR -> xpBase + (currentLevel * multiplicador);
            case EXPONENCIAL -> xpBase * Math.pow(currentLevel + 1, expoente);
        };
    }

    public int getNivelMaximo() {
        return nivelMaximo;
    }
}
