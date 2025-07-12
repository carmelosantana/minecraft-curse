package org.xpfarm.curse.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.xpfarm.curse.CursePlugin;

public class ConfigManager {
    
    private final CursePlugin plugin;
    private FileConfiguration config;
    
    public ConfigManager(CursePlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    // Plague Configuration
    public int getMaxRounds() {
        return config.getInt("plague.maxRounds", 7);
    }
    
    public boolean isScaleWithXP() {
        return config.getBoolean("plague.scaleWithXP", true);
    }
    
    public int getSpawnRadius() {
        return config.getInt("plague.spawnRadius", 20);
    }
    
    public int getTimeLimitPerRound() {
        return config.getInt("plague.timeLimitPerRound", 60);
    }
    
    public boolean isAllowTerrainDamage() {
        return config.getBoolean("plague.allowTerrainDamage", false);
    }
    
    public int getMaxActivePlagues() {
        return config.getInt("plague.maxActivePlagues", 3);
    }
    
    public boolean isVisualEffectsEnabled() {
        return config.getBoolean("plague.visualEffects", true);
    }
    
    public int getCombatRadius() {
        return config.getInt("plague.combatRadius", 30);
    }
    
    public int getMinDistanceFromVillages() {
        return config.getInt("plague.minDistanceFromVillages", 100);
    }
    
    public int getResetCooldownMinutes() {
        return config.getInt("plague.resetCooldownMinutes", 5);
    }
    
    // Leaderboard Configuration
    public boolean isLeaderboardEnabled() {
        return config.getBoolean("leaderboard.enabled", true);
    }
    
    public int getLeaderboardDisplayCount() {
        return config.getInt("leaderboard.displayCount", 10);
    }
    
    // Debug Configuration
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }
    
    public boolean isLogMobSpawns() {
        return config.getBoolean("debug.logMobSpawns", false);
    }
    
    public boolean isLogPlayerActions() {
        return config.getBoolean("debug.logPlayerActions", false);
    }
}
