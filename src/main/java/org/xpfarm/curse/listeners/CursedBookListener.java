package org.xpfarm.curse.listeners;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.xpfarm.curse.CursePlugin;
import org.xpfarm.curse.utils.MessageUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CursedBookListener implements Listener {
    
    private final CursePlugin plugin;
    
    public CursedBookListener(CursePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onZombiePickupCursedBook(EntityPickupItemEvent event) {
        // Check if cursed book feature is enabled
        if (!plugin.getConfigManager().isCursedBookEnabled()) {
            return;
        }
        
        Entity entity = event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        
        // Check if entity is a zombie and item is a cursed book
        if (!(entity instanceof Zombie zombie)) {
            return;
        }
        
        if (!plugin.getCursedBookManager().isCursedBook(item)) {
            return;
        }
        
        // Find the nearest player who could have dropped the book
        Player nearestPlayer = findNearestPlayer(zombie);
        if (nearestPlayer == null) {
            return;
        }
        
        // Check if player already has an active plague
        if (plugin.getPlagueManager().hasActivePlague(nearestPlayer)) {
            MessageUtil.sendMessage(nearestPlayer, Component.text("You already have an active curse!", NamedTextColor.RED));
            return;
        }
        
        // Check cooldown
        if (plugin.getCooldownManager().hasCooldown(nearestPlayer)) {
            long remainingSeconds = plugin.getCooldownManager().getRemainingCooldownSeconds(nearestPlayer);
            long remainingMinutes = remainingSeconds / 60;
            MessageUtil.sendMessage(nearestPlayer, Component.text("You must wait " + remainingMinutes + " minutes before starting another curse!", NamedTextColor.RED));
            return;
        }
        
        // Cancel the pickup event so the item is consumed
        event.setCancelled(true);
        
        // Remove the item from the world
        event.getItem().remove();
        
        // Create dramatic effect
        createCurseActivationEffect(zombie, nearestPlayer);
        
        // Start the curse after a brief delay
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean started = plugin.getPlagueManager().startPlague(nearestPlayer, false);
                if (started) {
                    MessageUtil.sendMessage(nearestPlayer, Component.text("The cursed book has awakened! The curse begins!", NamedTextColor.DARK_RED));
                } else {
                    MessageUtil.sendMessage(nearestPlayer, Component.text("The curse could not be started. Check the conditions and try again.", NamedTextColor.RED));
                }
            }
        }.runTaskLater(plugin, 40L); // 2 second delay for dramatic effect
    }
    
    /**
     * Creates dramatic visual and audio effects when the curse is activated
     * @param zombie The zombie that picked up the book
     * @param player The player who will be cursed
     */
    private void createCurseActivationEffect(Zombie zombie, Player player) {
        // Only create effects if enabled
        if (!plugin.getConfigManager().isCursedBookActivationEffectsEnabled()) {
            return;
        }
        
        // Play ominous sound to player and nearby players
        player.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);
        player.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.2f);
        
        // Create particle effects
        if (plugin.getConfigManager().isVisualEffectsEnabled()) {
            // Dark particle burst
            player.getWorld().spawnParticle(Particle.SMOKE, 
                zombie.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.WITCH, 
                zombie.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, 
                zombie.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.02);
        }
        
        // Send message to player
        MessageUtil.sendMessage(player, Component.text("The zombie's eyes glow with dark energy...", NamedTextColor.DARK_PURPLE));
        
        // Send message to nearby players
        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (!nearbyPlayer.equals(player) && 
                nearbyPlayer.getLocation().distance(zombie.getLocation()) <= 20) {
                MessageUtil.sendMessage(nearbyPlayer, Component.text(player.getName() + "'s cursed book has been consumed by a zombie!", NamedTextColor.YELLOW));
            }
        }
        
        // Remove the zombie with a puff effect
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getConfigManager().isCursedBookActivationEffectsEnabled()) {
                    zombie.getWorld().spawnParticle(Particle.POOF, 
                        zombie.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                }
                zombie.remove();
            }
        }.runTaskLater(plugin, 20L); // Remove after 1 second
    }
    
    /**
     * Finds the nearest player to the zombie within a reasonable range
     * @param zombie The zombie entity
     * @return The nearest player, or null if none found
     */
    private Player findNearestPlayer(Zombie zombie) {
        Player nearestPlayer = null;
        double maxRange = plugin.getConfigManager().getCursedBookPickupRange();
        double nearestDistance = maxRange;
        
        for (Player player : zombie.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(zombie.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }
        
        return nearestPlayer;
    }
}
