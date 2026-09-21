package com.rpgcustom.habilidadesplus.leveling;

public enum LevelCurve {
    LINEAR,
    EXPONENCIAL;

    public static LevelCurve fromConfig(String value, LevelCurve fallback) {
        if (value == null) return fallback;
        try {
            return LevelCurve.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
