package org.xpfarm.curse.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.xpfarm.curse.CursePlugin;
import org.xpfarm.curse.models.Plague;
import org.xpfarm.curse.utils.MessageUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlagueManager {
    
    private final CursePlugin plugin;
    private final Map<UUID, Plague> activePlagues;
    private final Random random;
    
    public PlagueManager(CursePlugin plugin) {
        this.plugin = plugin;
        this.activePlagues = new ConcurrentHashMap<>();
        this.random = new Random();
    }
    
    public boolean startPlague(Player player) {
        // Check if player already has active plague
        if (hasActivePlague(player)) {
            return false;
        }
        
        // Check maximum active plagues
        if (activePlagues.size() >= plugin.getConfigManager().getMaxActivePlagues()) {
            MessageUtil.sendMessage(player, Component.text("Too many active curses on the server!", NamedTextColor.RED));
            return false;
        }
        
        // Check if it's night time
        World world = player.getWorld();
        long time = world.getTime();
        if (time < 12541 || time > 23031) { // Not night time
            MessageUtil.sendMessage(player, Component.text("The curse can only be started at night!", NamedTextColor.RED));
            return false;
        }
        
        // Check distance from villages (if configured)
        if (!isValidLocation(player.getLocation())) {
            MessageUtil.sendMessage(player, Component.text("You're too close to a village to start the curse!", NamedTextColor.RED));
            return false;
        }
        
        // Create and start plague
        Plague plague = new Plague(player, plugin);
        activePlagues.put(player.getUniqueId(), plague);
        
        // Apply visual effects
        if (plugin.getConfigManager().isVisualEffectsEnabled()) {
            applyPlagueAura(player);
        }
        
        // Send messages
        MessageUtil.sendMessage(player, Component.text("The curse has begun! Survive the waves!", NamedTextColor.GOLD));
        MessageUtil.sendMessage(player, Component.text("Kill all mobs in each round to progress!", NamedTextColor.YELLOW));
        
        // Start first wave
        plague.spawnNextWave();
        
        // Start monitoring task
        startPlagueMonitoring(plague);
        
        return true;
    }
    
    public boolean stopPlague(Player player) {
        Plague plague = activePlagues.get(player.getUniqueId());
        if (plague == null) {
            return false;
        }
        
        plague.endPlague(false);
        return true;
    }
    
    public void stopAllPlagues() {
        for (Plague plague : activePlagues.values()) {
            plague.endPlague(false);
        }
        activePlagues.clear();
    }
    
    public boolean hasActivePlague(Player player) {
        return activePlagues.containsKey(player.getUniqueId());
    }
    
    public Plague getPlague(Player player) {
        return activePlagues.get(player.getUniqueId());
    }
    
    public void removePlague(UUID playerId) {
        activePlagues.remove(playerId);
    }
    
    public void spawnMobsForPlague(Plague plague, int count) {
        Player player = plague.getPlayer();
        Location center = player.getLocation();
        int spawnRadius = plugin.getConfigManager().getSpawnRadius();
        
        // Set initial mob count for health bar progress tracking
        plague.setInitialMobCount(count);
        
        for (int i = 0; i < count; i++) {
            // Calculate spawn location around player
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = spawnRadius + random.nextInt(10);
            
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;
            double y = center.getWorld().getHighestBlockYAt((int) x, (int) z) + 1;
            
            Location spawnLoc = new Location(center.getWorld(), x, y, z);
            
            // Spawn zombie with enhanced attributes
            Zombie zombie = spawnEnhancedZombie(spawnLoc, plague.getCurrentRound());
            if (zombie != null) {
                plague.addMob(zombie);
                
                // Set target to plague player
                zombie.setTarget(player);
                
                if (plugin.getConfigManager().isLogMobSpawns()) {
                    plugin.getLogger().info("Spawned enhanced zombie for " + player.getName() + " at round " + plague.getCurrentRound());
                }
            }
        }
        
        // Update health progress after all mobs are spawned
        plague.updateHealthProgress();
    }
    
    private Zombie spawnEnhancedZombie(Location location, int round) {
        World world = location.getWorld();
        if (world == null) return null;
        
        Zombie zombie = (Zombie) world.spawnEntity(location, EntityType.ZOMBIE);
        
        // Scale zombie based on round
        double healthMultiplier = 1.0 + (round * 0.5);
        zombie.setMaxHealth(zombie.getMaxHealth() * healthMultiplier);
        zombie.setHealth(zombie.getMaxHealth());
        
        // Add speed based on round
        if (round > 2) {
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, round - 2));
        }
        
        // Add armor for higher rounds
        if (round > 3) {
            giveZombieArmor(zombie, round);
        }
        
        // Prevent despawning
        zombie.setPersistent(true);
        
        // Custom name
        zombie.customName(Component.text("Cursed Zombie", NamedTextColor.RED));
        zombie.setCustomNameVisible(true);
        
        return zombie;
    }
    
    private void giveZombieArmor(Zombie zombie, int round) {
        zombie.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        if (round > 4) {
            zombie.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        }
        if (round > 5) {
            zombie.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            zombie.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
        }
        if (round > 6) {
            zombie.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        }
    }
    
    public void spawnRewardChest(Plague plague) {
        Player player = plague.getPlayer();
        Location chestLoc = player.getLocation().clone().add(0, 0, 2);
        
        // Find suitable location for chest
        Block block = chestLoc.getWorld().getHighestBlockAt(chestLoc);
        block = block.getRelative(0, 1, 0);
        
        block.setType(Material.CHEST);
        Chest chest = (Chest) block.getState();
        
        // Fill chest with rewards
        fillRewardChest(chest.getInventory(), plague);
        
        // Add particle effects if enabled
        if (plugin.getConfigManager().isVisualEffectsEnabled()) {
            addChestParticles(block.getLocation());
        }
        
        MessageUtil.sendMessage(player, Component.text("A reward chest has appeared!", NamedTextColor.GREEN));
    }
    
    private void fillRewardChest(Inventory inventory, Plague plague) {
        // Add antidote if player completed first round and doesn't have one
        if (plague.getCurrentRound() == 1 || !plague.hasAntidote()) {
            ItemStack antidote = createAntidotePotion();
            inventory.addItem(antidote);
            plague.setHasAntidote(true);
        }
        
        // Add random loot based on round
        int lootCount = 2 + random.nextInt(3); // 2-4 items
        for (int i = 0; i < lootCount; i++) {
            ItemStack loot = generateRandomLoot(plague.getCurrentRound());
            if (loot != null) {
                inventory.addItem(loot);
            }
        }
        
        // Add undo potion for higher rounds (rare)
        if (plague.getCurrentRound() > 3 && random.nextDouble() < 0.1) { // 10% chance
            ItemStack undoPotion = createUndoPotion();
            inventory.addItem(undoPotion);
        }
    }
    
    private ItemStack createAntidotePotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        
        meta.setDisplayName("§aCurse Antidote"); // Use legacy color codes
        List<String> lore = Arrays.asList(
            "§7Ends the current curse",
            "§eUse wisely!"
        );
        meta.setLore(lore);
        
        // Create custom potion using NamespacedKey
        NamespacedKey key = new NamespacedKey(plugin, "curse_antidote");
        meta.setBasePotionType(PotionType.HEALING); // Base type for appearance
        
        potion.setItemMeta(meta);
        return potion;
    }
    
    private ItemStack createUndoPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        
        meta.setDisplayName("§9Undo Potion"); // Use legacy color codes
        List<String> lore = Arrays.asList(
            "§7Reverses damage from last round",
            "§6Very rare!"
        );
        meta.setLore(lore);
        
        meta.setBasePotionType(PotionType.HEALING);
        
        potion.setItemMeta(meta);
        return potion;
    }
    
    public static ItemStack createCurseTriggerPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        
        meta.setDisplayName("§4Curse Trigger"); // Use legacy color codes for compatibility
        List<String> lore = Arrays.asList(
            "§7Drink at night to start The Curse",
            "§cBeware the undead plague!"
        );
        meta.setLore(lore);
        
        meta.setBasePotionType(PotionType.AWKWARD); // Base type for appearance
        meta.addCustomEffect(new PotionEffect(PotionEffectType.BAD_OMEN, 1, 0), true);
        
        potion.setItemMeta(meta);
        return potion;
    }
    
    private ItemStack generateRandomLoot(int round) {
        List<Material> commonLoot = Arrays.asList(
            Material.BREAD, Material.COOKED_BEEF, Material.GOLDEN_APPLE
        );
        
        List<Material> uncommonLoot = Arrays.asList(
            Material.ENCHANTED_BOOK, Material.DIAMOND, Material.IRON_INGOT
        );
        
        List<Material> rareLoot = Arrays.asList(
            Material.TOTEM_OF_UNDYING, Material.NETHERITE_INGOT, Material.DRAGON_EGG
        );
        
        double rand = random.nextDouble();
        Material material;
        
        if (rand < 0.6) { // 60% common
            material = commonLoot.get(random.nextInt(commonLoot.size()));
        } else if (rand < 0.85) { // 25% uncommon
            material = uncommonLoot.get(random.nextInt(uncommonLoot.size()));
        } else { // 15% rare
            material = rareLoot.get(random.nextInt(rareLoot.size()));
        }
        
        return new ItemStack(material, 1);
    }
    
    public void applyPoisonPenalty(Plague plague) {
        Player player = plague.getPlayer();
        
        // Apply deadly poison
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, Integer.MAX_VALUE, 1));
        
        MessageUtil.sendMessage(player, Component.text("You have been cursed with deadly poison!", NamedTextColor.RED));
        MessageUtil.sendMessage(player, Component.text("Find and drink the antidote to survive!", NamedTextColor.YELLOW));
    }
    
    private void applyPlagueAura(Player player) {
        // Apply visual aura effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true));
    }
    
    private void addChestParticles(Location location) {
        // Schedule particle effects
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks++ > 100) { // 5 seconds
                    cancel();
                    return;
                }
                
                // Spawn particles around chest
                location.getWorld().spawnParticle(
                    org.bukkit.Particle.ENCHANT,
                    location.clone().add(0.5, 1, 0.5),
                    10, 0.5, 0.5, 0.5, 0.1
                );
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    
    private boolean isValidLocation(Location location) {
        // Check distance from villages if configured
        int minDistance = plugin.getConfigManager().getMinDistanceFromVillages();
        if (minDistance > 0) {
            // Simple implementation - can be enhanced to check for actual villages
            return true; // For now, allow all locations
        }
        
        return true;
    }
    
    private void startPlagueMonitoring(Plague plague) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plague.isActive()) {
                    cancel();
                    return;
                }
                
                Player player = plague.getPlayer();
                if (!player.isOnline()) {
                    plague.endPlague(false);
                    cancel();
                    return;
                }
                
                // Update boss bar visibility for all players in radius
                plague.updateBossBarVisibility();
                
                // Check if player left combat radius
                int combatRadius = plugin.getConfigManager().getCombatRadius();
                if (player.getLocation().distance(plague.getStartLocation()) > combatRadius) {
                    MessageUtil.sendMessage(player, Component.text("You left the curse area! The curse intensifies!", NamedTextColor.RED));
                    applyPoisonPenalty(plague);
                    plague.endPlague(false);
                    cancel();
                    return;
                }
                
                // Check if it's daylight and player doesn't have antidote
                World world = player.getWorld();
                long time = world.getTime();
                if ((time >= 23031 || time <= 12541) && !plague.hasAntidote()) {
                    MessageUtil.sendMessage(player, Component.text("Daylight arrived without the antidote! The curse consumes you!", NamedTextColor.RED));
                    applyPoisonPenalty(plague);
                    plague.endPlague(false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
    }
}
