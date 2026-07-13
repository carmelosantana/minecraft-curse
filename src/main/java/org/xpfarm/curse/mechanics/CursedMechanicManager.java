package org.xpfarm.curse.mechanics;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.xpfarm.curse.CursePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manager for all cursed mechanics in The Curse plugin.
 * Handles registration, activation, and lifecycle management of different cursed mechanics.
 */
public class CursedMechanicManager {

    private final CursePlugin plugin;
    private final Map<String, CursedMechanic> mechanics;

    public CursedMechanicManager(CursePlugin plugin) {
        this.plugin = plugin;
        this.mechanics = new HashMap<>();

        // Register available mechanics
        registerMechanics();
    }

    /**
     * Registers all available cursed mechanics
     */
    private void registerMechanics() {
        // Register ZP25 - Zombie Plague (existing mechanic)
        registerMechanic(new ZombieRoyaleMechanic(plugin));

        // Register CS25P - Cursed Spawn (new mechanic)
        registerMechanic(new CursedSpawnMechanic(plugin));
    }

    /**
     * Registers a cursed mechanic with the manager
     * @param mechanic The mechanic to register
     */
    public void registerMechanic(CursedMechanic mechanic) {
        mechanics.put(mechanic.getMechanicId(), mechanic);
        plugin.getLogger().info("Registered cursed mechanic: " + mechanic.getMechanicId() + " (" + mechanic.getName() + ")");
    }

    /**
     * Gets a cursed mechanic by its ID
     * @param mechanicId The mechanic ID
     * @return The mechanic, or null if not found
     */
    public CursedMechanic getMechanic(String mechanicId) {
        return mechanics.get(mechanicId);
    }

    /**
     * Gets all registered mechanic IDs
     * @return Set of mechanic IDs
     */
    public Set<String> getMechanicIds() {
        return mechanics.keySet();
    }

    /**
     * Gets all registered mechanics
     * @return Map of mechanic ID to mechanic instance
     */
    public Map<String, CursedMechanic> getAllMechanics() {
        return new HashMap<>(mechanics);
    }

    /**
     * Attempts to activate a cursed mechanic based on the book item
     * @param player The player activating the mechanic
     * @param item The cursed book item
     * @return true if a mechanic was activated
     */
    public boolean activateMechanic(Player player, ItemStack item) {
        // Try to determine which mechanic this book belongs to
        for (CursedMechanic mechanic : mechanics.values()) {
            if (mechanic.isEnabled() && isMechanicBook(item, mechanic)) {
                return mechanic.activate(player, item);
            }
        }
        return false;
    }

    /**
     * Checks if an item is a cursed book for a specific mechanic
     * @param item The item to check
     * @param mechanic The mechanic to check against
     * @return true if the item is a book for this mechanic
     */
    private boolean isMechanicBook(ItemStack item, CursedMechanic mechanic) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemStack mechanicBook = mechanic.createCursedBook();
        if (mechanicBook == null || !mechanicBook.hasItemMeta()) {
            return false;
        }

        // Compare the keys that would be set by each mechanic
        var itemPdc = item.getItemMeta().getPersistentDataContainer();
        var mechanicPdc = mechanicBook.getItemMeta().getPersistentDataContainer();

        // Check if the item has any of the mechanic's persistent data keys
        for (var key : mechanicPdc.getKeys()) {
            if (itemPdc.has(key) && itemPdc.get(key, PersistentDataType.STRING)
                    .equals(mechanicPdc.get(key, PersistentDataType.STRING))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if any mechanic has the player as active
     * @param player The player to check
     * @return true if the player has any active mechanic
     */
    public boolean hasActiveMechanic(Player player) {
        return mechanics.values().stream()
                .anyMatch(mechanic -> mechanic.hasActiveMechanic(player));
    }

    /**
     * Ends all active mechanics for a player
     * @param player The player whose mechanics should end
     */
    public void endAllMechanics(Player player) {
        mechanics.values().forEach(mechanic -> mechanic.endMechanic(player));
    }

    /**
     * Gets the active mechanic for a player (if any)
     * @param player The player to check
     * @return The active mechanic, or null if none
     */
    public CursedMechanic getActiveMechanic(Player player) {
        return mechanics.values().stream()
                .filter(mechanic -> mechanic.hasActiveMechanic(player))
                .findFirst()
                .orElse(null);
    }
}
