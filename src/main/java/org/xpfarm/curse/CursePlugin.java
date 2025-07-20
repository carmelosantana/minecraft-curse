package org.xpfarm.curse;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.Material;
import org.xpfarm.curse.commands.CurseCommand;
import org.xpfarm.curse.listeners.PlayerListener;
import org.xpfarm.curse.listeners.PotionListener;
import org.xpfarm.curse.listeners.CursedBookListener;
import org.xpfarm.curse.managers.PlagueManager;
import org.xpfarm.curse.managers.LeaderboardManager;
import org.xpfarm.curse.managers.ConfigManager;
import org.xpfarm.curse.managers.CooldownManager;
import org.xpfarm.curse.managers.HUDManager;
import org.xpfarm.curse.managers.CursedBookManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CursePlugin extends JavaPlugin {
    
    private static CursePlugin instance;
    private PlagueManager plagueManager;
    private LeaderboardManager leaderboardManager;
    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private HUDManager hudManager;
    private CursedBookManager cursedBookManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        configManager = new ConfigManager(this);
        cooldownManager = new CooldownManager(this);
        hudManager = new HUDManager(this);
        cursedBookManager = new CursedBookManager(this);
        plagueManager = new PlagueManager(this);
        leaderboardManager = new LeaderboardManager(this);
        
        // Register commands
        getCommand("curse").setExecutor(new CurseCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new PotionListener(this), this);
        getServer().getPluginManager().registerEvents(new CursedBookListener(this), this);
        
        // Load configuration
        configManager.loadConfig();
        
        // Register cursed book recipe if enabled
        if (configManager.isCursedBookEnabled() && configManager.isCursedBookRecipeEnabled()) {
            registerCursedBookRecipe();
        }
        
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
        // Stop all HUD tasks
        if (hudManager != null) {
            hudManager.stopAllHUDs();
        }
        
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
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public HUDManager getHUDManager() {
        return hudManager;
    }
    
    public CursedBookManager getCursedBookManager() {
        return cursedBookManager;
    }
    
    /**
     * Registers the custom recipe for the cursed book
     */
    private void registerCursedBookRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(cursedBookManager.getCursedBookRecipeKey(), cursedBookManager.createCursedBook());
        
        // Define the recipe pattern
        // Row 1: Spider Eye, Nether Star, Spider Eye
        // Row 2: Ender Pearl, Book, Ender Pearl
        // Row 3: Rotten Flesh, Rotten Flesh, Rotten Flesh
        recipe.shape("ESE", "PBP", "FFF");
        recipe.setIngredient('E', Material.SPIDER_EYE);
        recipe.setIngredient('S', Material.NETHER_STAR);
        recipe.setIngredient('P', Material.ENDER_PEARL);
        recipe.setIngredient('B', Material.BOOK);
        recipe.setIngredient('F', Material.ROTTEN_FLESH);
        
        getServer().addRecipe(recipe);
        
        getLogger().info("Cursed book recipe registered successfully!");
    }
    
    public void reloadPlugin() {
        // Reload configuration
        configManager.loadConfig();
        
        // Reload leaderboard
        leaderboardManager.loadLeaderboard();
        
        getLogger().info("Plugin configuration reloaded!");
    }
}
