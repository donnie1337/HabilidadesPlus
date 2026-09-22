package com.rpgcustom.habilidadesplus.leveling;

import com.rpgcustom.habilidadesplus.SkillType;
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

    private double mineracaoFatorNivel100;
    private double mineracaoFatorNivel250;
    private double mineracaoFatorNivel500;
    private double mineracaoFatorNivel750;
    private double mineracaoFatorNivel1000;

    public LevelingManager(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        this.curve = LevelCurve.fromConfig(config.getString("nivelamento.curva"), LevelCurve.LINEAR);
        this.xpBase = config.getDouble("nivelamento.xp-base", 100);
        this.multiplicador = config.getDouble("nivelamento.multiplicador", 45);
        this.expoente = config.getDouble("nivelamento.expoente", 1.8);
        this.nivelMaximo = config.getInt("nivelamento.nivel-maximo", 1000);

        this.mineracaoFatorNivel100 = config.getDouble("nivelamento.mineracao.fator-nivel-100", 1.25);
        this.mineracaoFatorNivel250 = config.getDouble("nivelamento.mineracao.fator-nivel-250", 1.75);
        this.mineracaoFatorNivel500 = config.getDouble("nivelamento.mineracao.fator-nivel-500", 2.75);
        this.mineracaoFatorNivel750 = config.getDouble("nivelamento.mineracao.fator-nivel-750", 4.25);
        this.mineracaoFatorNivel1000 = config.getDouble("nivelamento.mineracao.fator-nivel-1000", 6.0);
    }

    /**
     * XP necessario para sair de currentLevel e chegar em currentLevel + 1.
     * Mantem a curva global para as habilidades que nao possuem uma curva propria.
     */
    public double xpParaProximoNivel(int currentLevel) {
        return calcularXpBase(currentLevel);
    }

    /**
     * XP necessario para uma habilidade especifica.
     * Mineracao possui uma curva progressivamente mais pesada.
     */
    public double xpParaProximoNivel(SkillType skill, int currentLevel) {
        double xp = calcularXpBase(currentLevel);

        if (skill != SkillType.MINERACAO) {
            return xp;
        }

        return xp * fatorProgressivoMineracao(currentLevel);
    }

    private double calcularXpBase(int currentLevel) {
        return switch (curve) {
            case LINEAR -> xpBase + (currentLevel * multiplicador);
            case EXPONENCIAL -> xpBase * Math.pow(currentLevel + 1, expoente);
        };
    }

    private double fatorProgressivoMineracao(int level) {
        if (level <= 100) {
            return interpolar(0, 1.0, 100, mineracaoFatorNivel100, level);
        }
        if (level <= 250) {
            return interpolar(100, mineracaoFatorNivel100, 250, mineracaoFatorNivel250, level);
        }
        if (level <= 500) {
            return interpolar(250, mineracaoFatorNivel250, 500, mineracaoFatorNivel500, level);
        }
        if (level <= 750) {
            return interpolar(500, mineracaoFatorNivel500, 750, mineracaoFatorNivel750, level);
        }
        return interpolar(750, mineracaoFatorNivel750, 1000, mineracaoFatorNivel1000, Math.min(level, 1000));
    }

    private double interpolar(int nivelInicial, double fatorInicial, int nivelFinal, double fatorFinal, int nivel) {
        double progresso = (double) (nivel - nivelInicial) / (nivelFinal - nivelInicial);
        return fatorInicial + ((fatorFinal - fatorInicial) * progresso);
    }

    public int getNivelMaximo() {
        return nivelMaximo;
    }
}
