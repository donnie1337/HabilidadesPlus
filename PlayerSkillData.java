package com.rpgcustom.habilidadesplus.data;

/**
 * Guarda o progresso de UM jogador em UMA habilidade: o nivel atual
 * e o XP acumulado desde o ultimo level up (nao o XP total historico).
 */
public class PlayerSkillData {

    private int level;
    private double currentXp;

    public PlayerSkillData() {
        this(0, 0);
    }

    public PlayerSkillData(int level, double currentXp) {
        this.level = level;
        this.currentXp = currentXp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getCurrentXp() {
        return currentXp;
    }

    public void setCurrentXp(double currentXp) {
        this.currentXp = currentXp;
    }

    public void addXp(double amount) {
        this.currentXp += amount;
    }
}
