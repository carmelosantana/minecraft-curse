package org.xpfarm.curse.managers;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.xpfarm.curse.CursePlugin;
import org.xpfarm.curse.models.Plague;
import org.xpfarm.curse.utils.MessageUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LeaderboardManager {
    
    private final CursePlugin plugin;
    private final File leaderboardFile;
    private FileConfiguration leaderboardConfig;
    
    // Persistent Data Container keys
    private final NamespacedKey totalCompletionsKey;
    private final NamespacedKey totalKillsKey;
    private final NamespacedKey bestTimeKey;
    private final NamespacedKey highestRoundKey;
    
    public LeaderboardManager(CursePlugin plugin) {
        this.plugin = plugin;
        this.leaderboardFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        
        // Initialize PDC keys
        this.totalCompletionsKey = new NamespacedKey(plugin, "curse_total_completions");
        this.totalKillsKey = new NamespacedKey(plugin, "curse_total_kills");
        this.bestTimeKey = new NamespacedKey(plugin, "curse_best_time");
        this.highestRoundKey = new NamespacedKey(plugin, "curse_highest_round");
    }
    
    public void loadLeaderboard() {
        if (!leaderboardFile.exists()) {
            plugin.saveResource("leaderboard.yml", false);
        }
        
        leaderboardConfig = YamlConfiguration.loadConfiguration(leaderboardFile);
    }
    
    public void saveLeaderboard() {
        try {
            leaderboardConfig.save(leaderboardFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save leaderboard file: " + e.getMessage());
        }
    }
    
    public void updatePlayerStats(UUID playerId, Plague plague) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) return;
        
        // Update player's persistent data
        updatePlayerPersistentData(player, plague);
        
        // Update server-wide leaderboard
        updateServerLeaderboard(player, plague);
    }
    
    private void updatePlayerPersistentData(Player player, Plague plague) {
        var pdc = player.getPersistentDataContainer();
        
        // Total completions
        int completions = pdc.getOrDefault(totalCompletionsKey, PersistentDataType.INTEGER, 0);
        if (plague.getCurrentRound() > 1) { // Only count if they completed at least one round
            completions++;
            pdc.set(totalCompletionsKey, PersistentDataType.INTEGER, completions);
        }
        
        // Total kills
        int totalKills = pdc.getOrDefault(totalKillsKey, PersistentDataType.INTEGER, 0);
        totalKills += plague.getTotalKills();
        pdc.set(totalKillsKey, PersistentDataType.INTEGER, totalKills);
        
        // Best time (for completed plagues)
        if (plague.getCurrentRound() > 1) {
            long currentTime = System.currentTimeMillis() - plague.getStartTime();
            long bestTime = pdc.getOrDefault(bestTimeKey, PersistentDataType.LONG, Long.MAX_VALUE);
            if (currentTime < bestTime) {
                pdc.set(bestTimeKey, PersistentDataType.LONG, currentTime);
            }
        }
        
        // Highest round reached
        int highestRound = pdc.getOrDefault(highestRoundKey, PersistentDataType.INTEGER, 0);
        if (plague.getCurrentRound() > highestRound) {
            pdc.set(highestRoundKey, PersistentDataType.INTEGER, plague.getCurrentRound());
        }
    }
    
    private void updateServerLeaderboard(Player player, Plague plague) {
        String playerName = player.getName();
        String path = "players." + playerName;
        
        // Update global stats
        int globalCompletions = leaderboardConfig.getInt("global.totalCompletions", 0);
        int globalKills = leaderboardConfig.getInt("global.totalKills", 0);
        
        if (plague.getCurrentRound() > 1) {
            globalCompletions++;
        }
        globalKills += plague.getTotalKills();
        
        leaderboardConfig.set("global.totalCompletions", globalCompletions);
        leaderboardConfig.set("global.totalKills", globalKills);
        
        // Update player-specific stats in leaderboard
        int playerCompletions = leaderboardConfig.getInt(path + ".completions", 0);
        int playerKills = leaderboardConfig.getInt(path + ".kills", 0);
        
        if (plague.getCurrentRound() > 1) {
            playerCompletions++;
        }
        playerKills += plague.getTotalKills();
        
        leaderboardConfig.set(path + ".completions", playerCompletions);
        leaderboardConfig.set(path + ".kills", playerKills);
        
        // Update best time if completed
        if (plague.getCurrentRound() > 1) {
            long currentTime = System.currentTimeMillis() - plague.getStartTime();
            long bestTime = leaderboardConfig.getLong(path + ".bestTime", Long.MAX_VALUE);
            if (currentTime < bestTime) {
                leaderboardConfig.set(path + ".bestTime", currentTime);
            }
        }
        
        // Update highest round
        int highestRound = leaderboardConfig.getInt(path + ".highestRound", 0);
        if (plague.getCurrentRound() > highestRound) {
            leaderboardConfig.set(path + ".highestRound", plague.getCurrentRound());
        }
        
        // Save changes
        saveLeaderboard();
    }
    
    public void displayLeaderboard(CommandSender sender) {
        if (!plugin.getConfigManager().isLeaderboardEnabled()) {
            MessageUtil.sendMessage(sender, Component.text("Leaderboard is disabled!", NamedTextColor.RED));
            return;
        }
        
        MessageUtil.sendMessage(sender, Component.text("=== The Curse Leaderboard ===", NamedTextColor.GOLD));
        MessageUtil.sendMessage(sender, Component.text(""));
        
        // Global stats
        int globalCompletions = leaderboardConfig.getInt("global.totalCompletions", 0);
        int globalKills = leaderboardConfig.getInt("global.totalKills", 0);
        
        MessageUtil.sendMessage(sender, Component.text("Global Statistics:", NamedTextColor.AQUA));
        MessageUtil.sendMessage(sender, Component.text("Total Completions: ", NamedTextColor.GRAY)
            .append(Component.text(globalCompletions, NamedTextColor.WHITE)));
        MessageUtil.sendMessage(sender, Component.text("Total Kills: ", NamedTextColor.GRAY)
            .append(Component.text(globalKills, NamedTextColor.WHITE)));
        MessageUtil.sendMessage(sender, Component.text(""));
        
        // Top players by completions
        displayTopPlayers(sender, "completions", "Most Completions");
        MessageUtil.sendMessage(sender, Component.text(""));
        
        // Top players by kills
        displayTopPlayers(sender, "kills", "Most Kills");
        MessageUtil.sendMessage(sender, Component.text(""));
        
        // Best times
        displayBestTimes(sender);
        
        // Player's personal stats if sender is a player
        if (sender instanceof Player player) {
            MessageUtil.sendMessage(sender, Component.text(""));
            displayPersonalStats(player);
        }
    }
    
    private void displayTopPlayers(CommandSender sender, String statType, String title) {
        MessageUtil.sendMessage(sender, Component.text(title + ":", NamedTextColor.AQUA));
        
        if (!leaderboardConfig.contains("players")) {
            MessageUtil.sendMessage(sender, Component.text("No data available yet!", NamedTextColor.GRAY));
            return;
        }
        
        // Get all players and their stats
        List<Map.Entry<String, Integer>> playerStats = new ArrayList<>();
        
        for (String playerName : leaderboardConfig.getConfigurationSection("players").getKeys(false)) {
            int value = leaderboardConfig.getInt("players." + playerName + "." + statType, 0);
            playerStats.add(new AbstractMap.SimpleEntry<>(playerName, value));
        }
        
        // Sort by value (descending)
        playerStats.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Display top entries
        int displayCount = Math.min(plugin.getConfigManager().getLeaderboardDisplayCount(), playerStats.size());
        for (int i = 0; i < displayCount; i++) {
            Map.Entry<String, Integer> entry = playerStats.get(i);
            MessageUtil.sendMessage(sender, Component.text((i + 1) + ". ", NamedTextColor.YELLOW)
                .append(Component.text(entry.getKey(), NamedTextColor.WHITE))
                .append(Component.text(" - " + entry.getValue(), NamedTextColor.GRAY)));
        }
    }
    
    private void displayBestTimes(CommandSender sender) {
        MessageUtil.sendMessage(sender, Component.text("Best Completion Times:", NamedTextColor.AQUA));
        
        if (!leaderboardConfig.contains("players")) {
            MessageUtil.sendMessage(sender, Component.text("No data available yet!", NamedTextColor.GRAY));
            return;
        }
        
        // Get all players and their best times
        List<Map.Entry<String, Long>> playerTimes = new ArrayList<>();
        
        for (String playerName : leaderboardConfig.getConfigurationSection("players").getKeys(false)) {
            long bestTime = leaderboardConfig.getLong("players." + playerName + ".bestTime", Long.MAX_VALUE);
            if (bestTime != Long.MAX_VALUE) {
                playerTimes.add(new AbstractMap.SimpleEntry<>(playerName, bestTime));
            }
        }
        
        // Sort by time (ascending)
        playerTimes.sort(Map.Entry.comparingByValue());
        
        // Display top entries
        int displayCount = Math.min(plugin.getConfigManager().getLeaderboardDisplayCount(), playerTimes.size());
        for (int i = 0; i < displayCount; i++) {
            Map.Entry<String, Long> entry = playerTimes.get(i);
            String timeStr = formatTime(entry.getValue());
            MessageUtil.sendMessage(sender, Component.text((i + 1) + ". ", NamedTextColor.YELLOW)
                .append(Component.text(entry.getKey(), NamedTextColor.WHITE))
                .append(Component.text(" - " + timeStr, NamedTextColor.GRAY)));
        }
    }
    
    private void displayPersonalStats(Player player) {
        MessageUtil.sendMessage(player, Component.text("Your Statistics:", NamedTextColor.AQUA));
        
        var pdc = player.getPersistentDataContainer();
        
        int completions = pdc.getOrDefault(totalCompletionsKey, PersistentDataType.INTEGER, 0);
        int kills = pdc.getOrDefault(totalKillsKey, PersistentDataType.INTEGER, 0);
        long bestTime = pdc.getOrDefault(bestTimeKey, PersistentDataType.LONG, Long.MAX_VALUE);
        int highestRound = pdc.getOrDefault(highestRoundKey, PersistentDataType.INTEGER, 0);
        
        MessageUtil.sendMessage(player, Component.text("Completions: ", NamedTextColor.GRAY)
            .append(Component.text(completions, NamedTextColor.WHITE)));
        MessageUtil.sendMessage(player, Component.text("Total Kills: ", NamedTextColor.GRAY)
            .append(Component.text(kills, NamedTextColor.WHITE)));
        MessageUtil.sendMessage(player, Component.text("Highest Round: ", NamedTextColor.GRAY)
            .append(Component.text(highestRound, NamedTextColor.WHITE)));
        
        if (bestTime != Long.MAX_VALUE) {
            MessageUtil.sendMessage(player, Component.text("Best Time: ", NamedTextColor.GRAY)
                .append(Component.text(formatTime(bestTime), NamedTextColor.WHITE)));
        } else {
            MessageUtil.sendMessage(player, Component.text("Best Time: ", NamedTextColor.GRAY)
                .append(Component.text("None yet", NamedTextColor.WHITE)));
        }
    }
    
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
