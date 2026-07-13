package org.xpfarm.curse.mechanics;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.xpfarm.curse.CursePlugin;

/**
 * Base interface for all cursed mechanics in The Curse plugin.
 * Each mechanic represents a different cursed challenge that can be triggered by players.
 */
public interface CursedMechanic {

    /**
     * Gets the unique identifier for this mechanic
     * @return The mechanic ID (e.g., "ZP25", "CS25P")
     */
    String getMechanicId();

    /**
     * Gets the display name for this mechanic
     * @return The user-friendly name
     */
    String getName();

    /**
     * Checks if this mechanic is enabled in the configuration
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Creates the cursed book item for this mechanic
     * @return ItemStack representing the cursed book
     */
    ItemStack createCursedBook();

    /**
     * Activates this cursed mechanic for the given player
     * @param player The player triggering the mechanic
     * @param item The cursed book item being used
     * @return true if the mechanic was successfully activated
     */
    boolean activate(Player player, ItemStack item);

    /**
     * Checks if the player meets the conditions to activate this mechanic
     * @param player The player attempting to activate
     * @return true if conditions are met
     */
    boolean canActivate(Player player);

    /**
     * Ends the cursed mechanic for the given player (if active)
     * @param player The player whose mechanic should end
     * @return true if the mechanic was successfully ended
     */
    boolean endMechanic(Player player);

    /**
     * Checks if the player has this mechanic currently active
     * @param player The player to check
     * @return true if the mechanic is active
     */
    boolean hasActiveMechanic(Player player);

    /**
     * Gets the plugin instance
     * @return The CursePlugin instance
     */
    CursePlugin getPlugin();
}
