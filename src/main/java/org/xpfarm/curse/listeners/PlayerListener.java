package org.xpfarm.curse.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.xpfarm.curse.CursePlugin;
import org.xpfarm.curse.models.Plague;
import org.xpfarm.curse.utils.MessageUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    
    private final CursePlugin plugin;
    private final Map<UUID, Long> lastQuitTime;
    
    public PlayerListener(CursePlugin plugin) {
        this.plugin = plugin;
        this.lastQuitTime = new HashMap<>();
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Check if player has active curse
        if (plugin.getPlagueManager().hasActivePlague(player)) {
            // Reset the curse and set cooldown
            plugin.getPlagueManager().resetPlague(player, true);
            
            MessageUtil.sendMessage(player, Component.text("Your curse has been reset due to death!", NamedTextColor.RED));
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Track quit time if player has active curse
        if (plugin.getPlagueManager().hasActivePlague(player)) {
            lastQuitTime.put(player.getUniqueId(), System.currentTimeMillis());
            // End the plague but don't set cooldown yet (wait for rejoin)
            plugin.getPlagueManager().stopPlague(player);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Check if player quit with active curse
        if (lastQuitTime.containsKey(playerId)) {
            // Set cooldown for rejoining after quitting with curse
            plugin.getCooldownManager().setCooldown(player);
            lastQuitTime.remove(playerId);
            
            MessageUtil.sendMessage(player, Component.text("Your curse was reset due to leaving the server. You must wait before starting another one.", NamedTextColor.YELLOW));
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
                if (event.getEntity().getKiller() instanceof Player) {
                    event.setDroppedExp(event.getDroppedExp() * 2); // Double XP
                }
                
                break;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Early return if no active plague to improve performance
        if (!plugin.getPlagueManager().hasActivePlague(player)) {
            return;
        }
        
        // Check if player has active plague and moved significantly
        Plague plague = plugin.getPlagueManager().getPlague(player);
        if (plague != null && plague.isActive()) {
            // Distance check is handled in PlagueManager monitoring task
            // This event can be used for other movement-based features if needed
        }
    }
}
