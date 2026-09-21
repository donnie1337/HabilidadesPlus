package com.rpgcustom.habilidadesplus.leveling;

public class LevelUpResult {

    private final boolean leveledUp;
    private final int levelsGained;
    private final int newLevel;

    public LevelUpResult(boolean leveledUp, int levelsGained, int newLevel) {
        this.leveledUp = leveledUp;
        this.levelsGained = levelsGained;
        this.newLevel = newLevel;
    }

    public boolean isLeveledUp() {
        return leveledUp;
    }

    public int getLevelsGained() {
        return levelsGained;
    }

    public int getNewLevel() {
        return newLevel;
    }
}
