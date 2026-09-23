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
     * Todas as habilidades usam a mesma dificuldade de progressao.
     * O que varia entre elas e a quantidade de XP concedida por acao.
     */
    public double xpParaProximoNivel(SkillType skill, int currentLevel) {
        return calcularXpBase(currentLevel);
    }

    private double calcularXpBase(int currentLevel) {
        return switch (curve) {
            case LINEAR -> xpBase + (currentLevel * multiplicador);
            case EXPONENCIAL -> xpBase * Math.pow(currentLevel + 1, expoente);
        };
    }

    public int getNivelMaximo() {
        return nivelMaximo;
    }
}
