package org.xpfarm.curse.managers;

import org.bukkit.entity.Player;
import org.xpfarm.curse.CursePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    
    private final CursePlugin plugin;
    private final Map<UUID, Long> cooldowns;
    
    public CooldownManager(CursePlugin plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
    }
    
    public void setCooldown(Player player) {
        setCooldown(player.getUniqueId());
    }
    
    public void setCooldown(UUID playerId) {
        long cooldownTime = System.currentTimeMillis() + (plugin.getConfigManager().getResetCooldownMinutes() * 60 * 1000L);
        cooldowns.put(playerId, cooldownTime);
    }
    
    public boolean hasCooldown(Player player) {
        return hasCooldown(player.getUniqueId());
    }
    
    public boolean hasCooldown(UUID playerId) {
        Long cooldownTime = cooldowns.get(playerId);
        if (cooldownTime == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= cooldownTime) {
            cooldowns.remove(playerId);
            return false;
        }
        
        return true;
    }
    
    public long getRemainingCooldownSeconds(Player player) {
        return getRemainingCooldownSeconds(player.getUniqueId());
    }
    
    public long getRemainingCooldownSeconds(UUID playerId) {
        Long cooldownTime = cooldowns.get(playerId);
        if (cooldownTime == null) {
            return 0;
        }
        
        long remaining = cooldownTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
    
    public void removeCooldown(Player player) {
        removeCooldown(player.getUniqueId());
    }
    
    public void removeCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
    
    public void clearAllCooldowns() {
        cooldowns.clear();
    }
}
