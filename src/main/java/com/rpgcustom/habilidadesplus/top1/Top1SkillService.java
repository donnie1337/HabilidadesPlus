package com.rpgcustom.habilidadesplus.top1;

import com.rpgcustom.habilidadesplus.SkillType;
import com.rpgcustom.habilidadesplus.data.DataManager;
import com.rpgcustom.habilidadesplus.data.PlayerProfile;
import org.bukkit.Bukkit;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public final class Top1SkillService {

    private final DataManager dataManager;

    public Top1SkillService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public SkillType getTop1Skill(UUID playerId) {
        if (playerId == null) return null;

        SkillType bestSkill = null;
        int bestLevel = 0;
        Map<UUID, PlayerProfile> profiles = dataManager.getAllProfiles();

        for (SkillType skill : SkillType.values()) {
            UUID winner = profiles.entrySet().stream()
                    .filter(entry -> entry.getValue().getLevel(skill) > 0)
                    .sorted(Comparator
                            .<Map.Entry<UUID, PlayerProfile>>comparingInt(entry -> entry.getValue().getLevel(skill))
                            .reversed()
                            .thenComparing(entry -> {
                                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                                return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
                            }))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (!playerId.equals(winner)) continue;

            int level = profiles.get(playerId).getLevel(skill);
            if (level > bestLevel) {
                bestLevel = level;
                bestSkill = skill;
            }
        }

        return bestSkill;
    }
}
