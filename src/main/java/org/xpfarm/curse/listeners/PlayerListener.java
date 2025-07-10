package org.xpfarm.curse.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.xpfarm.curse.CursePlugin;
import org.xpfarm.curse.models.Plague;

public class PlayerListener implements Listener {
    
    private final CursePlugin plugin;
    
    public PlayerListener(CursePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // End any active plague when player leaves
        if (plugin.getPlagueManager().hasActivePlague(player)) {
            plugin.getPlagueManager().stopPlague(player);
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        
        // Check if this entity is part of any active plague
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Plague plague = plugin.getPlagueManager().getPlague(player);
            if (plague != null && plague.getActiveMobs().contains(entity)) {
                // This mob was part of the plague
                plague.onMobKilled(entity);
                
                // Give XP bonus for curse mobs
                if (event.getEntity().getKiller() instanceof Player killer) {
                    event.setDroppedExp(event.getDroppedExp() * 2); // Double XP
                }
                
                break;
            }
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has active plague and moved significantly
        if (plugin.getPlagueManager().hasActivePlague(player)) {
            Plague plague = plugin.getPlagueManager().getPlague(player);
            if (plague != null && plague.isActive()) {
                // Distance check is handled in PlagueManager monitoring task
                // This event can be used for other movement-based features if needed
            }
        }
    }
}
