package org.xpfarm.curse.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.xpfarm.curse.CursePlugin;

public class CursedBookListener implements Listener {

    private final CursePlugin plugin;

    public CursedBookListener(CursePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if this is a right-click action
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player is holding an item
        if (item == null) {
            return;
        }

        // Check if the item is a cursed book (either old or new mechanic)
        boolean isLegacyBook = plugin.getCursedBookManager().isCursedBook(item);
        boolean isMechanicBook = false;

        // Check if it's a book for any mechanic
        for (String mechanicId : plugin.getCursedMechanicManager().getMechanicIds()) {
            var mechanic = plugin.getCursedMechanicManager().getMechanic(mechanicId);
            if (mechanic != null) {
                ItemStack mechanicBook = mechanic.createCursedBook();
                if (mechanicBook != null && mechanicBook.hasItemMeta() && item.hasItemMeta()) {
                    if (mechanicBook.getItemMeta().getPersistentDataContainer()
                            .equals(item.getItemMeta().getPersistentDataContainer())) {
                        isMechanicBook = true;
                        break;
                    }
                }
            }
        }

        if (!isLegacyBook && !isMechanicBook) {
            return;
        }

        // Cancel the event to prevent other interactions
        event.setCancelled(true);

        // Try to activate using new mechanics system first
        if (plugin.getCursedMechanicManager().activateMechanic(player, item)) {
            return; // Successfully activated via new system
        }

        // Fallback to legacy cursed book system
        plugin.getCursedBookManager().activateCursedBook(player, item);
    }
}