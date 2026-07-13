package org.xpfarm.curse.mechanics;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.xpfarm.curse.CursePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;
import java.util.List;

/**
 * ZP25 - Zombie Royale Mechanic
 * The original zombie plague mechanic that triggers waves of zombies and other monsters.
 * This wraps the existing PlagueManager functionality.
 */
public class ZombieRoyaleMechanic implements CursedMechanic {

    private final CursePlugin plugin;
    private final NamespacedKey bookKey;

    public static final String MECHANIC_ID = "ZP25";
    public static final String BOOK_ID = "zp25_cursed_book";
    public static final String MECHANIC_NAME = "Zombie Royale";

    public ZombieRoyaleMechanic(CursePlugin plugin) {
        this.plugin = plugin;
        this.bookKey = new NamespacedKey(plugin, BOOK_ID);
    }

    @Override
    public String getMechanicId() {
        return MECHANIC_ID;
    }

    @Override
    public String getName() {
        return MECHANIC_NAME;
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("mechanics.zp25.enabled", true);
    }

    @Override
    public ItemStack createCursedBook() {
        ItemStack book = new ItemStack(Material.BOOK);

        book.editMeta(meta -> {
            // Set display name
            meta.displayName(Component.text(MECHANIC_ID, NamedTextColor.DARK_RED)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

            // Set lore with curse mechanics explanation
            List<Component> lore = Arrays.asList(
                Component.text("", NamedTextColor.GRAY),
                Component.text("A cursed tome bound in darkness", NamedTextColor.DARK_PURPLE)
                    .decoration(TextDecoration.ITALIC, true),
                Component.text("", NamedTextColor.GRAY),
                Component.text("Curse Mechanics:", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true),
                Component.text("• Right-click to activate the curse", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• Must be used at night", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• Book is consumed permanently", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• Activates zombie plague waves", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("", NamedTextColor.GRAY),
                Component.text("⚠ Same cooldown rules apply", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true)
            );
            meta.lore(lore);

            // Add persistent data to identify this as a ZP25 cursed book
            meta.getPersistentDataContainer().set(bookKey, PersistentDataType.STRING, BOOK_ID);
        });

        return book;
    }

    @Override
    public boolean activate(Player player, ItemStack item) {
        // Check if this is the correct book type
        if (!isCursedBook(item)) {
            return false;
        }

        // Check if player can activate
        if (!canActivate(player)) {
            return false;
        }

        // Use existing plague manager to start the curse
        boolean started = plugin.getPlagueManager().startPlague(player, false);
        if (started) {
            // Consume the book
            item.setAmount(item.getAmount() - 1);
            player.sendMessage(Component.text("The cursed book crumbles to ash as the zombie plague awakens!", NamedTextColor.DARK_RED));
            return true;
        } else {
            player.sendMessage(Component.text("The curse could not be started. Check the conditions and try again.", NamedTextColor.RED));
            return false;
        }
    }

    @Override
    public boolean canActivate(Player player) {
        // Check if player already has active plague
        if (hasActiveMechanic(player)) {
            player.sendMessage(Component.text("You already have an active curse!", NamedTextColor.RED));
            return false;
        }

        // Check cooldown
        if (plugin.getCooldownManager().hasCooldown(player)) {
            long remainingSeconds = plugin.getCooldownManager().getRemainingCooldownSeconds(player);
            long remainingMinutes = remainingSeconds / 60;
            player.sendMessage(Component.text("You must wait " + remainingMinutes + " minutes before starting another curse!", NamedTextColor.RED));
            return false;
        }

        return true;
    }

    @Override
    public boolean endMechanic(Player player) {
        return plugin.getPlagueManager().stopPlague(player);
    }

    @Override
    public boolean hasActiveMechanic(Player player) {
        return plugin.getPlagueManager().hasActivePlague(player);
    }

    @Override
    public CursePlugin getPlugin() {
        return plugin;
    }

    /**
     * Checks if an ItemStack is a ZP25 cursed book
     * @param item The item to check
     * @return true if the item is a ZP25 cursed book
     */
    private boolean isCursedBook(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        return meta.getPersistentDataContainer().has(bookKey, PersistentDataType.STRING);
    }
}
