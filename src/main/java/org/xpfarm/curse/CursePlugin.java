package org.xpfarm.curse;

import org.bukkit.plugin.java.JavaPlugin;
import org.xpfarm.curse.commands.CurseCommand;
import org.xpfarm.curse.listeners.PlayerListener;
import org.xpfarm.curse.listeners.PotionListener;
import org.xpfarm.curse.managers.PlagueManager;
import org.xpfarm.curse.managers.LeaderboardManager;
import org.xpfarm.curse.managers.ConfigManager;
import org.xpfarm.curse.utils.MessageUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CursePlugin extends JavaPlugin {
    
    private static CursePlugin instance;
    private PlagueManager plagueManager;
    private LeaderboardManager leaderboardManager;
    private ConfigManager configManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        configManager = new ConfigManager(this);
        plagueManager = new PlagueManager(this);
        leaderboardManager = new LeaderboardManager(this);
        
        // Register commands
        getCommand("curse").setExecutor(new CurseCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new PotionListener(this), this);
        
        // Load configuration
        configManager.loadConfig();
        
        // Initialize leaderboard
        leaderboardManager.loadLeaderboard();
        
        getLogger().info("The Curse plugin has been enabled!");
        
        // Send startup message to console
        getServer().getConsoleSender().sendMessage(
            Component.text("=== The Curse Plugin ===", NamedTextColor.GOLD)
        );
        getServer().getConsoleSender().sendMessage(
            Component.text("Plugin enabled successfully!", NamedTextColor.GREEN)
        );
    }
    
    @Override
    public void onDisable() {
        // Stop all active plagues
        if (plagueManager != null) {
            plagueManager.stopAllPlagues();
        }
        
        // Save leaderboard
        if (leaderboardManager != null) {
            leaderboardManager.saveLeaderboard();
        }
        
        getLogger().info("The Curse plugin has been disabled!");
        
        // Send shutdown message to console
        getServer().getConsoleSender().sendMessage(
            Component.text("The Curse Plugin disabled", NamedTextColor.YELLOW)
        );
    }
    
    public static CursePlugin getInstance() {
        return instance;
    }
    
    public PlagueManager getPlagueManager() {
        return plagueManager;
    }
    
    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public void reloadPlugin() {
        // Reload configuration
        configManager.loadConfig();
        
        // Reload leaderboard
        leaderboardManager.loadLeaderboard();
        
        getLogger().info("Plugin configuration reloaded!");
    }
}
