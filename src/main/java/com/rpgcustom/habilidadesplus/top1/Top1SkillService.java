package com.rpgcustom.habilidadesplus.top1;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class Top1SkillService {

    private final DataManager dataManager;
    private final Map<UUID, SkillType> cache = new HashMap<>();
    private boolean dirty = true;

    public Top1SkillService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public synchronized void invalidate() {
        dirty = true;
    }

    public synchronized SkillType getTop1Skill(UUID playerId) {
        if (playerId == null) return null;
        if (dirty) rebuild();
        return cache.get(playerId);
    }

    private void rebuild() {
        cache.clear();

        Map<UUID, PlayerProfile> profiles = dataManager.getAllProfiles();
        Map<UUID, String> names = new HashMap<>();
        for (UUID uuid : profiles.keySet()) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            names.put(uuid, name == null ? "" : name.toLowerCase(Locale.ROOT));
        }

        Map<SkillType, UUID> winners = new HashMap<>();
        Map<SkillType, Integer> winnerLevels = new HashMap<>();

        for (SkillType skill : SkillType.values()) {
            UUID winner = null;
            int winnerLevel = 0;
            String winnerName = "";

            for (Map.Entry<UUID, PlayerProfile> entry : profiles.entrySet()) {
                int level = entry.getValue().getLevel(skill);
                if (level <= 0) continue;

                String name = names.getOrDefault(entry.getKey(), "");
                if (winner == null
                        || level > winnerLevel
                        || (level == winnerLevel && name.compareTo(winnerName) < 0)) {
                    winner = entry.getKey();
                    winnerLevel = level;
                    winnerName = name;
                }
            }

            if (winner != null) {
                winners.put(skill, winner);
                winnerLevels.put(skill, winnerLevel);
            }
        }

        for (SkillType skill : SkillType.values()) {
            UUID winner = winners.get(skill);
            if (winner == null) continue;

            int level = winnerLevels.getOrDefault(skill, 0);
            SkillType current = cache.get(winner);
            if (current == null || level > winnerLevels.getOrDefault(current, 0)) {
                cache.put(winner, skill);
            }
        }

        dirty = false;
    }
}
