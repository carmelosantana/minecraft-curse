package org.xpfarm.curse.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.xpfarm.curse.CursePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;
import java.util.List;

public class CursedBookManager {

    private final CursePlugin plugin;
    private final NamespacedKey cursedBookKey;

    public static final String CURSED_BOOK_ID = "zp25_cursed_book";
    public static final String CURSED_BOOK_NAME = "ZP25";

    public CursedBookManager(CursePlugin plugin) {
        this.plugin = plugin;
        this.cursedBookKey = new NamespacedKey(plugin, CURSED_BOOK_ID);
    }

    /**
     * Creates a new cursed book item with custom name, lore, and persistent data
     * @return ItemStack of the cursed book
     */
    public ItemStack createCursedBook() {
        ItemStack book = new ItemStack(Material.BOOK);

        book.editMeta(meta -> {
            // Set display name
            meta.displayName(Component.text(CURSED_BOOK_NAME, NamedTextColor.DARK_RED)
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
                Component.text("• Activates curse with dramatic effect", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("", NamedTextColor.GRAY),
                Component.text("⚠ Same cooldown rules apply", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true)
            );
            meta.lore(lore);

            // Add persistent data to identify this as a cursed book
            meta.getPersistentDataContainer().set(cursedBookKey, PersistentDataType.STRING, CURSED_BOOK_ID);
        });

        return book;
    }

    /**
     * Checks if an ItemStack is a cursed book
     * @param item The item to check
     * @return true if the item is a cursed book
     */
    public boolean isCursedBook(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        return meta.getPersistentDataContainer().has(cursedBookKey, PersistentDataType.STRING);
    }

    /**
     * Activates the cursed book - creates effects and starts the curse
     * @param player The player activating the book
     * @param item The cursed book item
     * @return true if the curse was successfully started
     */
    public boolean activateCursedBook(Player player, ItemStack item) {
        // Validate the book
        if (!isCursedBook(item)) {
            return false;
        }

        // Check if player already has an active plague
        if (plugin.getPlagueManager().hasActivePlague(player)) {
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

        // Create dramatic activation effects
        createActivationEffects(player);

        // Start the plague
        boolean started = plugin.getPlagueManager().startPlague(player, false);
        if (started) {
            // Consume the book
            item.setAmount(item.getAmount() - 1);
            player.sendMessage(Component.text("The cursed book crumbles to ash as the curse awakens!", NamedTextColor.DARK_RED));
            return true;
        } else {
            player.sendMessage(Component.text("The curse could not be started. Check the conditions and try again.", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Creates dramatic visual and audio effects when the curse is activated
     * @param player The player activating the curse
     */
    private void createActivationEffects(Player player) {
        // Play ominous sounds
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.6f);

        // Create particle effects around player
        if (plugin.getConfigManager().isVisualEffectsEnabled()) {
            // Dark energy burst around player
            player.getWorld().spawnParticle(Particle.WITCH,
                player.getLocation().add(0, 1, 0), 25, 1.0, 1.0, 1.0, 0.1);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.05);
            player.getWorld().spawnParticle(Particle.SMOKE,
                player.getLocation().add(0, 1, 0), 30, 0.8, 0.8, 0.8, 0.1);

            // Create sparkle effect around player
            for (int i = 0; i < 20; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = 1.5 + Math.random() * 1.5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.random() * 2;

                player.getWorld().spawnParticle(Particle.END_ROD,
                    player.getLocation().add(x, y, z), 1, 0, 0, 0, 0);
            }
        }

        // Apply knockback effect to the player
        Vector knockback = new Vector(0, 0.5, 0);
        // Add slight random horizontal knockback
        knockback.setX((Math.random() - 0.5) * 0.3);
        knockback.setZ((Math.random() - 0.5) * 0.3);
        player.setVelocity(knockback);

        // Send message to nearby players
        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (!nearbyPlayer.equals(player) &&
                nearbyPlayer.getLocation().distance(player.getLocation()) <= 20) {
                nearbyPlayer.sendMessage(Component.text(player.getName() + " has activated a cursed book! Dark energy surges through the air!", NamedTextColor.GOLD));
            }
        }
    }

    /**
     * Gets the NamespacedKey used for identifying cursed books
     * @return NamespacedKey for cursed book identification
     */
    public NamespacedKey getCursedBookKey() {
        return cursedBookKey;
    }
}
