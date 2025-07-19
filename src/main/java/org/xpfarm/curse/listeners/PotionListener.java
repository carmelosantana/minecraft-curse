package org.xpfarm.curse.listeners;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.xpfarm.curse.CursePlugin;
import org.xpfarm.curse.models.Plague;
import org.xpfarm.curse.utils.MessageUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PotionListener implements Listener {
    
    private final CursePlugin plugin;
    
    public PotionListener(CursePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item.getType() != Material.POTION) {
            return;
        }
        
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        // Check for Bad Omen effect or custom curse trigger potion
        if (meta.hasDisplayName()) {
            String displayName = meta.getDisplayName(); // Use legacy method for simplicity
            
            if (displayName != null && (displayName.contains("Bad Omen") || displayName.contains("Curse Trigger"))) {
                handleBadOmenPotion(player, event);
                return;
            }
        }
        
        // Also check if the potion has bad omen effect
        if (meta.hasCustomEffects()) {
            for (PotionEffect effect : meta.getCustomEffects()) {
                if (effect.getType().equals(PotionEffectType.BAD_OMEN)) {
                    handleBadOmenPotion(player, event);
                    return;
                }
            }
        }
        
        // Check for custom curse potions
        if (meta.hasDisplayName()) {
            String displayName = meta.getDisplayName(); // Use legacy method for simplicity
            
            if (displayName != null) {
                if (displayName.contains("Curse Antidote")) {
                    handleAntidotePotion(player, event);
                } else if (displayName.contains("Undo Potion")) {
                    handleUndoPotion(player, event);
                }
            }
        }
    }
    
    private void handleBadOmenPotion(Player player, PlayerItemConsumeEvent event) {
        // Check if player already has active plague
        if (plugin.getPlagueManager().hasActivePlague(player)) {
            MessageUtil.sendMessage(player, Component.text("You already have an active curse!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
        
        // Check if it's night time
        long time = player.getWorld().getTime();
        if (time < 12541 || time > 23031) { // Not night time
            MessageUtil.sendMessage(player, Component.text("The curse can only be started at night!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
        
        // Start the plague
        boolean started = plugin.getPlagueManager().startPlague(player);
        if (!started) {
            MessageUtil.sendMessage(player, Component.text("Failed to start the curse!", NamedTextColor.RED));
            event.setCancelled(true);
        } else {
            MessageUtil.sendMessage(player, Component.text("You feel the darkness taking hold...", NamedTextColor.DARK_RED));
            MessageUtil.sendMessage(player, Component.text("The curse has begun!", NamedTextColor.GOLD));
        }
    }
    
    private void handleAntidotePotion(Player player, PlayerItemConsumeEvent event) {
        Plague plague = plugin.getPlagueManager().getPlague(player);
        
        // Check what effects the player has before curing
        boolean hadPoison = player.hasPotionEffect(PotionEffectType.POISON);
        boolean hadGlowing = player.hasPotionEffect(PotionEffectType.GLOWING);
        
        // If there's an active curse
        if (plague != null) {
            if (!plague.hasAntidote()) {
                MessageUtil.sendMessage(player, Component.text("This antidote isn't yours to use yet!", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
            
            // End the plague successfully
            MessageUtil.sendMessage(player, Component.text("The antidote courses through your veins...", NamedTextColor.GREEN));
            
            // Remove curse effects
            player.removePotionEffect(PotionEffectType.POISON);
            player.removePotionEffect(PotionEffectType.GLOWING);
            
            // Provide specific feedback about what was cured
            if (hadPoison && hadGlowing) {
                MessageUtil.sendMessage(player, Component.text("The curse and poisoning effect have been cured!", NamedTextColor.GOLD));
            } else if (hadPoison) {
                MessageUtil.sendMessage(player, Component.text("The curse and poisoning effect have been cured!", NamedTextColor.GOLD));
            } else {
                MessageUtil.sendMessage(player, Component.text("The curse has been lifted!", NamedTextColor.GOLD));
            }
            
            // End plague
            plague.endPlague(true);
        } else {
            // No active curse - check if player has poison effects to cure
            if (hadPoison || hadGlowing) {
                MessageUtil.sendMessage(player, Component.text("The antidote courses through your veins...", NamedTextColor.GREEN));
                
                // Remove curse effects
                player.removePotionEffect(PotionEffectType.POISON);
                player.removePotionEffect(PotionEffectType.GLOWING);
                
                // Provide specific feedback about what was cured
                if (hadPoison && hadGlowing) {
                    MessageUtil.sendMessage(player, Component.text("The poisoning and glowing effects have been cured!", NamedTextColor.GOLD));
                } else if (hadPoison) {
                    MessageUtil.sendMessage(player, Component.text("The poisoning effect has been cured!", NamedTextColor.GOLD));
                } else if (hadGlowing) {
                    MessageUtil.sendMessage(player, Component.text("The glowing effect has been cured!", NamedTextColor.GOLD));
                }
            } else {
                MessageUtil.sendMessage(player, Component.text("You don't have any curse effects to cure!", NamedTextColor.YELLOW));
            }
        }
    }
    
    private void handleUndoPotion(Player player, PlayerItemConsumeEvent event) {
        Plague plague = plugin.getPlagueManager().getPlague(player);
        if (plague == null) {
            MessageUtil.sendMessage(player, Component.text("You don't have an active curse!", NamedTextColor.RED));
            return;
        }
        
        // Check what effects the player has before curing
        boolean hadPoison = player.hasPotionEffect(PotionEffectType.POISON);
        boolean hadWeakness = player.hasPotionEffect(PotionEffectType.WEAKNESS);
        boolean hadSlowness = player.hasPotionEffect(PotionEffectType.SLOWNESS);
        
        // Restore health and remove negative effects
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        
        // Remove negative potion effects
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        
        MessageUtil.sendMessage(player, Component.text("The undo potion restores your condition!", NamedTextColor.BLUE));
        
        // Provide specific feedback about what was cured
        if (hadPoison || hadWeakness || hadSlowness) {
            MessageUtil.sendMessage(player, Component.text("Negative effects have been cleansed!", NamedTextColor.GREEN));
        }
        
        MessageUtil.sendMessage(player, Component.text("You feel refreshed and ready to continue!", NamedTextColor.GREEN));
    }
}
