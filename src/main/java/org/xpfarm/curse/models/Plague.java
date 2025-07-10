package org.xpfarm.curse.models;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.scheduler.BukkitTask;
import org.xpfarm.curse.CursePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Plague {
    
    private final UUID playerId;
    private final Player player;
    private final Location startLocation;
    private final CursePlugin plugin;
    
    private int currentRound;
    private int totalKills;
    private long startTime;
    private boolean isActive;
    private boolean hasAntidote;
    
    private BossBar bossBar;
    private List<Entity> activeMobs;
    private BukkitTask roundTask;
    private BukkitTask timeoutTask;
    
    public Plague(Player player, CursePlugin plugin) {
        this.playerId = player.getUniqueId();
        this.player = player;
        this.startLocation = player.getLocation().clone();
        this.plugin = plugin;
        
        this.currentRound = 1;
        this.totalKills = 0;
        this.startTime = System.currentTimeMillis();
        this.isActive = true;
        this.hasAntidote = false;
        
        this.activeMobs = new ArrayList<>();
        
        initializeBossBar();
    }
    
    private void initializeBossBar() {
        bossBar = plugin.getServer().createBossBar(
            "The Curse - Round " + currentRound,
            BarColor.RED,
            BarStyle.SEGMENTED_10
        );
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
    }
    
    public void nextRound() {
        currentRound++;
        updateBossBar();
        
        // Check if this is the final wave
        if (currentRound > plugin.getConfigManager().getMaxRounds()) {
            startFinalWave();
        } else {
            spawnNextWave();
        }
    }
    
    public void spawnNextWave() {
        // Clear previous mobs
        clearActiveMobs();
        
        // Calculate mob count and types based on round and player XP
        int mobCount = calculateMobCount();
        
        // Schedule mob spawning
        spawnMobs(mobCount);
        
        // Start round timer if enabled
        startRoundTimer();
    }
    
    private void startFinalWave() {
        // Final wave - extremely difficult or impossible
        // Forces player to use antidote
        clearActiveMobs();
        
        updateBossBar("The Curse - Final Wave (Use Antidote!)", BarColor.PURPLE);
        
        // Spawn overwhelming number of strong mobs
        spawnMobs(50); // Overwhelming number
    }
    
    private int calculateMobCount() {
        int baseCount = 3 + currentRound;
        
        if (plugin.getConfigManager().isScaleWithXP()) {
            int playerLevel = player.getLevel();
            baseCount += playerLevel / 5; // Add mobs based on player level
        }
        
        return Math.min(baseCount, 20); // Cap at 20 mobs per wave
    }
    
    private void spawnMobs(int count) {
        // Implementation for spawning mobs around the player
        // This will be implemented in the PlagueManager
        plugin.getPlagueManager().spawnMobsForPlague(this, count);
    }
    
    private void startRoundTimer() {
        int timeLimit = plugin.getConfigManager().getTimeLimitPerRound();
        if (timeLimit > 0) {
            timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (isActive) {
                    failRound("Time limit exceeded!");
                }
            }, timeLimit * 20L); // Convert seconds to ticks
        }
    }
    
    public void onMobKilled(Entity mob) {
        activeMobs.remove(mob);
        totalKills++;
        
        // Check if round is complete
        if (activeMobs.isEmpty() && isActive) {
            completeRound();
        }
    }
    
    private void completeRound() {
        // Cancel timeout task
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
        
        // First round completion gives antidote
        if (currentRound == 1) {
            hasAntidote = true;
        }
        
        // Spawn reward chest
        plugin.getPlagueManager().spawnRewardChest(this);
        
        // Schedule next round
        plugin.getServer().getScheduler().runTaskLater(plugin, this::nextRound, 100L); // 5 second delay
    }
    
    private void failRound(String reason) {
        // Apply poison effect
        plugin.getPlagueManager().applyPoisonPenalty(this);
        
        // End plague
        endPlague(false);
    }
    
    public void endPlague(boolean successful) {
        isActive = false;
        
        // Cancel all tasks
        if (roundTask != null) {
            roundTask.cancel();
        }
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
        
        // Remove boss bar
        if (bossBar != null) {
            bossBar.removeAll();
        }
        
        // Clear mobs
        clearActiveMobs();
        
        // Update statistics
        plugin.getLeaderboardManager().updatePlayerStats(playerId, this);
        
        // Remove from active plagues
        plugin.getPlagueManager().removePlague(playerId);
    }
    
    private void clearActiveMobs() {
        for (Entity mob : activeMobs) {
            if (mob != null && !mob.isDead()) {
                mob.remove();
            }
        }
        activeMobs.clear();
    }
    
    private void updateBossBar() {
        updateBossBar("The Curse - Round " + currentRound, BarColor.RED);
    }
    
    private void updateBossBar(String title, BarColor color) {
        if (bossBar != null) {
            bossBar.setTitle(title);
            bossBar.setColor(color);
        }
    }
    
    // Getters and setters
    public UUID getPlayerId() { return playerId; }
    public Player getPlayer() { return player; }
    public Location getStartLocation() { return startLocation; }
    public int getCurrentRound() { return currentRound; }
    public int getTotalKills() { return totalKills; }
    public long getStartTime() { return startTime; }
    public boolean isActive() { return isActive; }
    public boolean hasAntidote() { return hasAntidote; }
    public List<Entity> getActiveMobs() { return activeMobs; }
    public BossBar getBossBar() { return bossBar; }
    
    public void addMob(Entity mob) {
        activeMobs.add(mob);
    }
    
    public void setHasAntidote(boolean hasAntidote) {
        this.hasAntidote = hasAntidote;
    }
}
