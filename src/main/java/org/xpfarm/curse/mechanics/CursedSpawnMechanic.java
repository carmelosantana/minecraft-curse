package org.xpfarm.curse.mechanics;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.xpfarm.curse.models.CurseActivity;
import org.xpfarm.curse.CursePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CS25P - Cursed Spawn Mechanic (2025 Preview)
 * A dramatic cursed mechanic where an obsidian block falls from the sky,
 * creates a massive explosion, and spawns waves of baby zombies from a crater spawner.
 */
public class CursedSpawnMechanic implements CursedMechanic {

    private final CursePlugin plugin;
    private final NamespacedKey bookKey;
    private final Map<UUID, CursedSpawnSession> activeSessions;

    public static final String MECHANIC_ID = "CS25P";
    public static final String BOOK_ID = "cs25p_cursed_spawn_book";
    public static final String MECHANIC_NAME = "Cursed Spawn";

    public CursedSpawnMechanic(CursePlugin plugin) {
        this.plugin = plugin;
        this.bookKey = new NamespacedKey(plugin, BOOK_ID);
        this.activeSessions = new ConcurrentHashMap<>();
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
        return plugin.getConfigManager().getConfig().getBoolean("mechanics.cs25p.enabled", true);
    }

    @Override
    public ItemStack createCursedBook() {
        ItemStack book = new ItemStack(Material.BOOK);

        book.editMeta(meta -> {
            // Set display name
            meta.displayName(Component.text("Book of Summoning", NamedTextColor.DARK_RED)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

            // Set lore with curse mechanics explanation
            List<Component> lore = Arrays.asList(
                Component.text("", NamedTextColor.GRAY),
                Component.text("It came not from the sky, but from beyond.", NamedTextColor.DARK_PURPLE)
                    .decoration(TextDecoration.ITALIC, true),
                Component.text("The stone cracked the earth, and with it,", NamedTextColor.DARK_PURPLE)
                    .decoration(TextDecoration.ITALIC, true),
                Component.text("the spawn of curses broke free.", NamedTextColor.DARK_PURPLE)
                    .decoration(TextDecoration.ITALIC, true),
                Component.text("", NamedTextColor.GRAY),
                Component.text("Cursed Spawn Mechanics:", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true),
                Component.text("• Right-click to summon the crater", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• Obsidian falls and explodes", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• Spawner creates waves of baby zombies", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• Survive all waves for rewards", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("• Use antidote to escape early", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("", NamedTextColor.GRAY),
                Component.text("⚠ Beware what stirs in the crater's core", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, true)
            );
            meta.lore(lore);

            // Add persistent data to identify this as a CS25P cursed book
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

        // Start the cursed spawn sequence
        boolean started = startCursedSpawn(player);
        if (started) {
            // Consume the book
            item.setAmount(item.getAmount() - 1);
            player.sendMessage(Component.text("The Book of Summoning crumbles as dark forces gather above!", NamedTextColor.DARK_RED));
            return true;
        } else {
            player.sendMessage(Component.text("The summoning failed. The location is not suitable for the curse.", NamedTextColor.RED));
            return false;
        }
    }

    @Override
    public boolean canActivate(Player player) {
        // Check if player already has active mechanic
        if (hasActiveMechanic(player)) {
            player.sendMessage(Component.text("You already have an active cursed spawn!", NamedTextColor.RED));
            return false;
        }

        // Check cooldown
        if (plugin.getCooldownManager().hasCooldown(player)) {
            long remainingSeconds = plugin.getCooldownManager().getRemainingCooldownSeconds(player);
            long remainingMinutes = remainingSeconds / 60;
            player.sendMessage(Component.text("You must wait " + remainingMinutes + " minutes before starting another curse!", NamedTextColor.RED));
            return false;
        }

        // Check if location is valid for falling block
        Location loc = player.getLocation();
        if (!isValidSpawnLocation(loc)) {
            player.sendMessage(Component.text("This location is not suitable for the cursed spawn!", NamedTextColor.RED));
            return false;
        }

        return true;
    }

    @Override
    public boolean endMechanic(Player player) {
        CursedSpawnSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.cleanup();
            player.sendMessage(Component.text("The cursed spawn has been ended.", NamedTextColor.YELLOW));
            return true;
        }
        return false;
    }

    @Override
    public boolean hasActiveMechanic(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    @Override
    public CursePlugin getPlugin() {
        return plugin;
    }

    /**
     * Handle mob death for CS25P sessions
     * @param entity The entity that died
     * @param killer The player who killed the entity (can be null)
     * @return true if this was a CS25P mob and kill was credited
     */
    public boolean handleMobDeath(Entity entity, Player killer) {
        for (CursedSpawnSession session : activeSessions.values()) {
            if (session.activeMobs.contains(entity)) {
                // This mob was part of a cursed spawn

                // Only give credit if the cursed player killed it or if no specific killer
                if (killer == null || killer.getUniqueId().equals(session.player.getUniqueId())) {
                    session.onMobKilled();
                    session.activeMobs.remove(entity);
                    return true;
                } else {
                    // Someone else killed the curse mob - remove it from tracking but no credit
                    session.activeMobs.remove(entity);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Starts the cursed spawn sequence for a player
     * @param player The player who activated the mechanic
     * @return true if successfully started
     */
    private boolean startCursedSpawn(Player player) {
        Location spawnLoc = player.getLocation().clone();

        // Create new session
        CursedSpawnSession session = new CursedSpawnSession(player, spawnLoc);
        activeSessions.put(player.getUniqueId(), session);

        // Start the falling block sequence
        session.startFallingBlock();

        return true;
    }

    /**
     * Checks if a location is valid for the cursed spawn
     * @param location The location to check
     * @return true if valid
     */
    private boolean isValidSpawnLocation(Location location) {
        // Check if we have enough vertical space for falling block
        World world = location.getWorld();
        if (world == null) return false;

        // Make sure we're not too close to world height limit
        if (location.getY() > world.getMaxHeight() - 50) {
            return false;
        }

        // Make sure there's solid ground reasonably below
        Block groundBlock = world.getHighestBlockAt(location);
        return groundBlock.getY() > world.getMinHeight() + 10;
    }

    /**
     * Checks if an ItemStack is a CS25P cursed book
     * @param item The item to check
     * @return true if the item is a CS25P cursed book
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

    /**
     * Internal class to manage a cursed spawn session for a player
     */
    private class CursedSpawnSession implements CurseActivity {
        private final Player player;
        private final Location startLocation;
        private final long startTime;

        private FallingBlock fallingBlock;
        private Location craterCenter;
        private Block spawnerBlock;
        private int currentWave;
        private int maxWaves;
        private int totalKills;
        private boolean wasSuccessful;
        private Set<Entity> activeMobs;
        private BukkitTask waveTask;
        private BukkitTask monitorTask;

        public CursedSpawnSession(Player player, Location startLocation) {
            this.player = player;
            this.startLocation = startLocation.clone();
            this.startTime = System.currentTimeMillis();
            this.activeMobs = new HashSet<>();
            this.currentWave = 0;
            this.maxWaves = plugin.getConfigManager().getConfig().getInt("mechanics.cs25p.maxWaves", 5);
            this.totalKills = 0;
            this.wasSuccessful = false;
        }

        public void startFallingBlock() {
            World world = startLocation.getWorld();
            if (world == null) return;

            // Calculate falling block spawn location (high in the sky)
            Location fallLocation = startLocation.clone().add(0, 100, 0);

            // Warn nearby players
            warnNearbyPlayers();

            // Create falling obsidian block
            fallingBlock = world.spawn(fallLocation, FallingBlock.class, fb -> {
                fb.setBlockData(Material.OBSIDIAN.createBlockData());
                fb.setDropItem(false); // Don't drop item when it hits
                fb.setHurtEntities(true); // Can hurt entities
            });

            // Schedule explosion when block hits ground
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (fallingBlock == null || fallingBlock.isDead() || fallingBlock.isOnGround()) {
                        createExplosion();
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 20L, 5L); // Check every 5 ticks after 1 second delay
        }

        private void warnNearbyPlayers() {
            World world = startLocation.getWorld();
            if (world == null) return;

            // Send warning to all players within 50 blocks
            for (Player nearbyPlayer : world.getPlayers()) {
                if (nearbyPlayer.getLocation().distance(startLocation) <= 50) {
                    nearbyPlayer.sendMessage(Component.text("⚠ DANGER: Something dark approaches from above! ⚠", NamedTextColor.DARK_RED, TextDecoration.BOLD));
                    nearbyPlayer.playSound(nearbyPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.8f);
                }
            }
        }

        private void createExplosion() {
            World world = startLocation.getWorld();
            if (world == null) return;

            // Find where the block landed
            craterCenter = fallingBlock != null ? fallingBlock.getLocation() : startLocation;

            // Create massive explosion (power of 1 end crystal = ~6)
            int explosionPower = plugin.getConfigManager().getConfig().getInt("mechanics.cs25p.explosion.power", 6);
            world.createExplosion(craterCenter, explosionPower, false, true);

            // Create additional dramatic effects
            createExplosionEffects();

            // Schedule spawner creation after explosion settles
            new BukkitRunnable() {
                @Override
                public void run() {
                    createSpawner();
                }
            }.runTaskLater(plugin, 40L); // 2 seconds after explosion
        }

        private void createExplosionEffects() {
            World world = craterCenter.getWorld();
            if (world == null) return;

            // Massive particle effects
            world.spawnParticle(Particle.EXPLOSION, craterCenter, 10, 3, 3, 3, 0);
            world.spawnParticle(Particle.LARGE_SMOKE, craterCenter, 50, 5, 5, 5, 0.1);
            world.spawnParticle(Particle.LAVA, craterCenter, 30, 4, 4, 4, 0);

            // Dramatic sounds
            world.playSound(craterCenter, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            world.playSound(craterCenter, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);

            // Message to activating player
            player.sendMessage(Component.text("The obsidian crater has been carved! Beware what emerges from its depths...", NamedTextColor.DARK_PURPLE));
        }

        private void createSpawner() {
            World world = craterCenter.getWorld();
            if (world == null) return;

            // Find the lowest point in the crater to place spawner
            Location spawnerLoc = findCraterBottom();

            // Place spawner block
            spawnerBlock = world.getBlockAt(spawnerLoc);
            spawnerBlock.setType(Material.SPAWNER);

            // Configure spawner
            if (spawnerBlock.getState() instanceof CreatureSpawner spawner) {
                spawner.setSpawnedType(EntityType.ZOMBIE);
                spawner.setDelay(20); // Quick spawning
                spawner.setMinSpawnDelay(20);
                spawner.setMaxSpawnDelay(40);
                spawner.setSpawnCount(2); // Spawn 2 at a time
                spawner.setMaxNearbyEntities(10);
                spawner.setRequiredPlayerRange(32);
                spawner.update();
            }

            // Start wave system
            startWaveSystem();
        }

        private Location findCraterBottom() {
            World world = craterCenter.getWorld();
            if (world == null) return craterCenter;

            // Search downward from crater center to find the lowest solid block
            Location searchLoc = craterCenter.clone();
            for (int y = (int) craterCenter.getY(); y > world.getMinHeight(); y--) {
                searchLoc.setY(y);
                Block block = world.getBlockAt(searchLoc);
                if (block.getType().isSolid()) {
                    return searchLoc.add(0, 1, 0); // Place spawner on top of solid block
                }
            }

            return craterCenter; // Fallback
        }

        private void startWaveSystem() {
            currentWave = 1;
            player.sendMessage(Component.text("Wave " + currentWave + " begins! Survive the cursed spawn!", NamedTextColor.GOLD));

            // Start wave spawning task
            waveTask = new BukkitRunnable() {
                @Override
                public void run() {
                    spawnWave();
                }
            }.runTaskTimer(plugin, 0L, plugin.getConfigManager().getConfig().getInt("mechanics.cs25p.waveInterval", 120) * 20L);

            // Start monitoring task to check wave completion
            monitorTask = new BukkitRunnable() {
                @Override
                public void run() {
                    checkWaveCompletion();
                }
            }.runTaskTimer(plugin, 40L, 20L); // Check every second after initial delay
        }

        private void spawnWave() {
            if (currentWave > maxWaves) {
                completeSpawn();
                return;
            }

            World world = craterCenter.getWorld();
            if (world == null) return;

            int mobsPerWave = plugin.getConfigManager().getConfig().getInt("mechanics.cs25p.perWave", 6);

            // Spawn baby zombies around the spawner
            for (int i = 0; i < mobsPerWave; i++) {
                Location spawnLoc = craterCenter.clone().add(
                    (Math.random() - 0.5) * 10, // Random X within 10 blocks
                    2, // Spawn above ground
                    (Math.random() - 0.5) * 10  // Random Z within 10 blocks
                );

                Entity entity = world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
                if (entity instanceof Zombie zombie) {
                    zombie.setAge(-1000); // Set as baby zombie (negative age)
                    zombie.setTarget(player); // Target the player
                    activeMobs.add(zombie);
                }
            }

            player.sendMessage(Component.text("Wave " + currentWave + " spawned! " + mobsPerWave + " baby zombies emerge!", NamedTextColor.RED));
        }

        private void checkWaveCompletion() {
            // Remove dead mobs from tracking
            activeMobs.removeIf(Entity::isDead);

            // Check if player died
            if (!player.isOnline() || player.isDead()) {
                endSpawn(false);
                return;
            }

            // Check if all mobs are dead and we can proceed to next wave
            if (activeMobs.isEmpty() && currentWave <= maxWaves) {
                currentWave++;
                if (currentWave <= maxWaves) {
                    player.sendMessage(Component.text("Wave " + (currentWave - 1) + " cleared! Next wave incoming...", NamedTextColor.GREEN));
                    // Next wave will spawn automatically via waveTask
                }
            }
        }

        private void completeSpawn() {
            endSpawn(true);
        }

        private void endSpawn(boolean success) {
            wasSuccessful = success;
            cleanup();

            if (success) {
                player.sendMessage(Component.text("You have survived the Cursed Spawn! Claim your reward!", NamedTextColor.GOLD));
                createRewardChest();
                plugin.getCooldownManager().setCooldown(player);
            } else {
                player.sendMessage(Component.text("The Cursed Spawn has ended...", NamedTextColor.GRAY));
            }

            // Update leaderboard stats
            plugin.getLeaderboardManager().updatePlayerStats(player.getUniqueId(), this);

            // Remove from active sessions
            activeSessions.remove(player.getUniqueId());
        }

        private void createRewardChest() {
            if (craterCenter == null) return;

            World world = craterCenter.getWorld();
            if (world == null) return;

            // Place chest at crater center
            Location chestLoc = craterCenter.clone();
            Block chestBlock = world.getBlockAt(chestLoc);
            chestBlock.setType(Material.CHEST);

            // TODO: Populate chest with rewards based on config
            // This would be implemented similar to the existing reward system

            player.sendMessage(Component.text("A reward chest has appeared in the crater!", NamedTextColor.YELLOW));
        }

        // Implement CurseActivity interface methods
        @Override
        public int getCurrentRound() {
            return currentWave; // Use wave number as round equivalent
        }

        @Override
        public int getTotalKills() {
            return totalKills;
        }

        @Override
        public long getStartTime() {
            return startTime;
        }

        @Override
        public boolean isSuccessful() {
            return wasSuccessful;
        }

        // Method to track mob kills
        public void onMobKilled() {
            totalKills++;
        }

        public void cleanup() {
            // Cancel running tasks
            if (waveTask != null && !waveTask.isCancelled()) {
                waveTask.cancel();
            }
            if (monitorTask != null && !monitorTask.isCancelled()) {
                monitorTask.cancel();
            }

            // Remove spawner if it exists
            if (spawnerBlock != null && spawnerBlock.getType() == Material.SPAWNER) {
                spawnerBlock.setType(Material.AIR);
            }

            // Clean up any remaining mobs
            for (Entity mob : activeMobs) {
                if (!mob.isDead()) {
                    mob.remove();
                }
            }
            activeMobs.clear();
        }
    }
}
