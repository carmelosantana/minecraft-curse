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
    private long roundStartTime; // Track when current round started for timer
    private boolean isActive;
    private boolean hasAntidote;
    private int initialMobCount; // Track initial mobs for health bar progress
    private boolean isOutsideArea; // Track if player is outside cursed area
    private boolean hasBeenWarned; // Track if player has been warned about leaving
    private long lastWarningTime; // Track last warning time to prevent spam
    
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
        this.roundStartTime = this.startTime; // Initialize round timer
        this.isActive = true;
        this.hasAntidote = false;
        
        this.activeMobs = new ArrayList<>();
        
        initializeBossBar();
        
        // Start HUD display for all nearby players
        plugin.getHUDManager().startHUD(player, this);
        plugin.getHUDManager().updateHUDForAllNearbyPlayers(this);
    }
    
    private void initializeBossBar() {
        bossBar = plugin.getServer().createBossBar(
            "The Curse - Round " + currentRound,
            BarColor.RED,
            BarStyle.SEGMENTED_10
        );
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
        
        // Add all players within curse radius to see the boss bar
        updateBossBarVisibility();
    }
    
    public void nextRound() {
        currentRound++;
        updateBossBar();
        updateBossBarVisibility();
        
        // Update HUD for all nearby players
        plugin.getHUDManager().updateHUDForAllNearbyPlayers(this);
        
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
        
        // Reset round timer
        this.roundStartTime = System.currentTimeMillis();
        
        // Calculate mob count and types based on round and player XP
        int mobCount = calculateMobCount();
        
        // Schedule mob spawning
        spawnMobs(mobCount);
        
        // Reset health bar to full and update visibility
        if (bossBar != null) {
            bossBar.setProgress(1.0);
            updateBossBarVisibility();
        }
        
        // Update HUD for all nearby players
        plugin.getHUDManager().updateHUDForAllNearbyPlayers(this);
        
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
        
        // Update health progress as mobs are killed
        updateHealthProgress();
        
        // Update HUD for all nearby players to reflect new kill count and remaining mobs
        plugin.getHUDManager().updateHUDForAllNearbyPlayers(this);
        
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
            // Update HUD to show antidote availability
            plugin.getHUDManager().updateHUDForAllNearbyPlayers(this);
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
        
        // Stop HUD display for all nearby players
        for (org.bukkit.entity.Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.getWorld().equals(startLocation.getWorld()) &&
                onlinePlayer.getLocation().distance(startLocation) <= plugin.getConfigManager().getCombatRadius()) {
                plugin.getHUDManager().stopHUD(onlinePlayer);
            }
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
    
    public void updateBossBarVisibility() {
        if (bossBar == null) return;
        
        int curseRadius = plugin.getConfigManager().getCombatRadius();
        
        // Remove all players first to refresh the list
        bossBar.removeAll();
        
        // Add all players within curse radius
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.getWorld().equals(startLocation.getWorld()) &&
                onlinePlayer.getLocation().distance(startLocation) <= curseRadius) {
                bossBar.addPlayer(onlinePlayer);
            }
        }
    }
    
    public void updateHealthProgress() {
        if (bossBar == null || !isActive) return;
        
        // Remove any dead mobs from the list first
        activeMobs.removeIf(mob -> mob == null || mob.isDead());
        
        int aliveMobs = activeMobs.size();
        
        // Calculate progress (1 = all alive, 0 = all dead)
        double progress = initialMobCount == 0 ? 0.0 : ((double) aliveMobs / initialMobCount);
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        
        // Update boss bar visibility to ensure all nearby players can see it
        updateBossBarVisibility();
    }
    
    // Getters and setters
    public UUID getPlayerId() { return playerId; }
    public Player getPlayer() { return player; }
    public Location getStartLocation() { return startLocation; }
    public int getCurrentRound() { return currentRound; }
    public int getTotalKills() { return totalKills; }
    public long getStartTime() { return startTime; }
    public long getRoundStartTime() { return roundStartTime; }
    public boolean isActive() { return isActive; }
    public boolean hasAntidote() { return hasAntidote; }
    public List<Entity> getActiveMobs() { return activeMobs; }
    public BossBar getBossBar() { return bossBar; }
    public int getInitialMobCount() { return initialMobCount; }
    
    public void addMob(Entity mob) {
        activeMobs.add(mob);
    }
    
    public void setHasAntidote(boolean hasAntidote) {
        this.hasAntidote = hasAntidote;
    }
    
    public void setInitialMobCount(int count) {
        this.initialMobCount = count;
    }
    
    public boolean isOutsideArea() {
        return isOutsideArea;
    }
    
    public void setOutsideArea(boolean outsideArea) {
        this.isOutsideArea = outsideArea;
    }
    
    public boolean hasBeenWarned() {
        return hasBeenWarned;
    }
    
    public void setHasBeenWarned(boolean hasBeenWarned) {
        this.hasBeenWarned = hasBeenWarned;
    }
    
    public long getLastWarningTime() {
        return lastWarningTime;
    }
    
    public void setLastWarningTime(long lastWarningTime) {
        this.lastWarningTime = lastWarningTime;
    }
}
