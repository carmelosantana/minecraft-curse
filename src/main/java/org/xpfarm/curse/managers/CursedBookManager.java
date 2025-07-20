package org.xpfarm.curse.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
                Component.text("• Drop near a zombie to begin", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• The zombie must pick it up", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• Zombie vanishes, curse starts", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• Book is consumed permanently", NamedTextColor.RED)
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
     * Gets the NamespacedKey used for the cursed book recipe
     * @return NamespacedKey for the recipe
     */
    public NamespacedKey getCursedBookRecipeKey() {
        return new NamespacedKey(plugin, "cursed_book_recipe");
    }
    
    /**
     * Gets the NamespacedKey used for identifying cursed books
     * @return NamespacedKey for cursed book identification
     */
    public NamespacedKey getCursedBookKey() {
        return cursedBookKey;
    }
}
