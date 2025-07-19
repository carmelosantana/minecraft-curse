package org.xpfarm.curse.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.xpfarm.curse.CursePlugin;
import org.xpfarm.curse.models.Plague;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HUDManager {
    
    private final CursePlugin plugin;
    private final Map<UUID, BukkitTask> activeTasks;
    
    public HUDManager(CursePlugin plugin) {
        this.plugin = plugin;
        this.activeTasks = new HashMap<>();
    }
    
    /**
     * Start displaying HUD for a player with an active plague
     */
    public void startHUD(Player player, Plague plague) {
        if (player == null || plague == null || !plugin.getConfigManager().isHUDEnabled()) return;
        
        // Stop existing HUD task for this player
        stopHUD(player);
        
        // Create and start new HUD task
        int updateInterval = plugin.getConfigManager().getHUDUpdateInterval();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plague.isActive() || !player.isOnline()) {
                    this.cancel();
                    activeTasks.remove(player.getUniqueId());
                    return;
                }
                
                updateHUDDisplay(player, plague);
            }
        }.runTaskTimer(plugin, 0L, updateInterval); // Use configurable update interval
        
        activeTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Stop displaying HUD for a player
     */
    public void stopHUD(Player player) {
        if (player == null) return;
        
        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        
        // Clear the action bar
        player.sendActionBar(Component.empty());
    }
    
    /**
     * Update HUD display for all players within the plague radius
     */
    public void updateHUDForAllNearbyPlayers(Plague plague) {
        if (plague == null || !plague.isActive() || !plugin.getConfigManager().isHUDEnabled()) return;
        
        Player plagueOwner = plague.getPlayer();
        if (plagueOwner == null || !plagueOwner.isOnline()) return;
        
        int radius = plugin.getConfigManager().getCombatRadius();
        
        // Update HUD for all players within radius
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(plagueOwner.getWorld()) &&
                player.getLocation().distance(plagueOwner.getLocation()) <= radius) {
                
                // Start or update HUD for this player
                if (!activeTasks.containsKey(player.getUniqueId())) {
                    startHUD(player, plague);
                }
            } else {
                // Player is outside radius, stop their HUD
                stopHUD(player);
            }
        }
    }
    
    /**
     * Update the HUD display for a specific player
     */
    private void updateHUDDisplay(Player player, Plague plague) {
        Component hudComponent = buildHUDComponent(plague);
        player.sendActionBar(hudComponent);
    }
    
    /**
     * Build the HUD component with current plague information
     */
    private Component buildHUDComponent(Plague plague) {
        Component result = Component.text("▌ ", NamedTextColor.BLACK); // Background separator
        
        // Round information
        Component roundInfo = Component.text("Round: ", NamedTextColor.GRAY)
            .append(Component.text(plague.getCurrentRound(), getRoundColor(plague.getCurrentRound())))
            .append(Component.text("/" + plugin.getConfigManager().getMaxRounds(), NamedTextColor.DARK_GRAY));
        
        result = result.append(roundInfo);
        
        // Kill count (if enabled)
        if (plugin.getConfigManager().isShowKills()) {
            Component killInfo = Component.text(" | Kills: ", NamedTextColor.GRAY)
                .append(Component.text(plague.getTotalKills(), NamedTextColor.WHITE));
            result = result.append(killInfo);
        }
        
        // Remaining mobs in current round (if enabled)
        if (plugin.getConfigManager().isShowRemainingMobs()) {
            int remainingMobs = plague.getActiveMobs().size();
            Component mobInfo = Component.text(" | Remaining: ", NamedTextColor.GRAY)
                .append(Component.text(remainingMobs, remainingMobs > 0 ? NamedTextColor.RED : NamedTextColor.GREEN));
            result = result.append(mobInfo);
        }
        
        // Timer information (if enabled and round has time limit)
        if (plugin.getConfigManager().isShowTimer()) {
            int timeLimit = plugin.getConfigManager().getTimeLimitPerRound();
            if (timeLimit > 0) {
                long elapsedTime = (System.currentTimeMillis() - plague.getRoundStartTime()) / 1000;
                long remainingTime = Math.max(0, timeLimit - elapsedTime);
                
                if (remainingTime > 0) {
                    Component timerInfo = Component.text(" | Time: ", NamedTextColor.GRAY)
                        .append(Component.text(formatTime(remainingTime), getTimerColor(remainingTime, timeLimit)));
                    result = result.append(timerInfo);
                }
            }
        }
        
        // Antidote status (if enabled)
        if (plugin.getConfigManager().isShowAntidoteStatus() && plague.hasAntidote()) {
            Component antidoteInfo = Component.text(" | ", NamedTextColor.GRAY)
                .append(Component.text("ANTIDOTE AVAILABLE", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true));
            result = result.append(antidoteInfo);
        }
        
        // Add closing separator
        result = result.append(Component.text(" ▐", NamedTextColor.BLACK));
        
        return result;
    }
    
    /**
     * Get color for round number based on difficulty
     */
    private NamedTextColor getRoundColor(int round) {
        if (round <= 2) return NamedTextColor.GREEN;
        if (round <= 4) return NamedTextColor.YELLOW;
        if (round <= 6) return NamedTextColor.GOLD;
        return NamedTextColor.RED;
    }
    
    /**
     * Get color for timer based on remaining time
     */
    private NamedTextColor getTimerColor(long remainingTime, int totalTime) {
        double percentage = (double) remainingTime / totalTime;
        if (percentage > 0.5) return NamedTextColor.GREEN;
        if (percentage > 0.25) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }
    
    /**
     * Format time in MM:SS format
     */
    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
    
    /**
     * Stop all active HUD tasks
     */
    public void stopAllHUDs() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        
        // Clear action bars for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(Component.empty());
        }
    }
    
    /**
     * Check if player has active HUD
     */
    public boolean hasActiveHUD(Player player) {
        return activeTasks.containsKey(player.getUniqueId());
    }
}
