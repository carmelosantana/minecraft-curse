# The Curse: Zombie Siege Plugin

A dynamic survival challenge plugin for Minecraft that introduces plague-style siege events triggered by drinking a cursed "bad omen" potion at night.

## Features

- **Dynamic Wave System**: Each round gets progressively harder with more mobs and enhanced attributes
- **Night-time Trigger**: Start the curse by drinking a Bad Omen potion at night
- **Boss Bar Integration**: Visual progress tracking for each plague round
- **Reward System**: Treasure chests spawn with valuable loot after completing rounds
- **Antidote Mechanic**: Essential survival item obtained after first wave completion
- **Leaderboard System**: Track completions, kills, best times, and highest rounds
- **XP Scaling**: Difficulty scales with player experience level
- **Combat Radius**: Stay within the designated area or face consequences
- **Poison Penalty**: Failure results in deadly poisoning until antidote is consumed
- **Final Wave**: Unwinnable challenge that forces strategic antidote usage
- **Automatic Reset**: Curse resets on player death or quit/rejoin
- **Cooldown System**: Configurable cooldown period after curse reset
- **Admin Commands**: Full admin control over player curses

## Installation

1. Download the latest `curse-{version}.jar` from the releases page
2. Place the JAR file in your server's `plugins/` directory
3. Start or restart your Minecraft server
4. The plugin will create a `config.yml` file in `plugins/TheCurse/`

## Building

This plugin requires Java 21 and Maven to build.

```bash
# Clone the repository
git clone <repository-url>
cd minecraft-curse

# Build the plugin
make build

# Or use Maven directly
mvn clean package
```

The compiled JAR will be available in the `target/` directory.

## Usage

### Starting a Curse

There are two ways to start a curse:

#### Method 1: Bad Omen Potion (Original)
1. Wait for nighttime (time 12541-23031)
2. Drink a Bad Omen potion
3. Survive the waves of enhanced zombies
4. Use the antidote from the first reward chest to end the curse

#### Method 2: Cursed Book (ZP25)
1. Craft or obtain a ZP25 cursed book
2. Drop the book near a zombie at night
3. Wait for the zombie to pick up the book
4. The zombie will vanish with dramatic effects and the curse begins

### Cursed Book (ZP25)

The **ZP25 cursed book** is a special item that provides an alternative way to start the curse. This dark tome is bound with powerful magic and requires specific materials to craft.

#### Obtaining a Cursed Book

**Crafting Recipe:**
```
[Spider Eye] [Nether Star] [Spider Eye]
[Ender Pearl]   [Book]     [Ender Pearl]
[Rotten Flesh][Rotten Flesh][Rotten Flesh]
```

**Admin Command:**
- `/curse book [player]` - Give a cursed book to yourself or another player (admin only)

#### Using the Cursed Book

1. **Drop the Book**: Drop the ZP25 cursed book near any zombie
2. **Zombie Pickup**: The zombie must be within 10 blocks (configurable) and will pick up the book
3. **Activation**: The zombie vanishes with a dramatic puff effect and ominous sounds
4. **Curse Begins**: After a 2-second delay, your curse starts automatically

#### Cursed Book Features

- **Unique Identifier**: Each book has a unique name "ZP25" and custom lore
- **Same Cooldown Rules**: Follows the same cooldown mechanics as Bad Omen potions
- **Visual Effects**: Dramatic particles and sounds when activated
- **Book Consumed**: The book is permanently consumed when used
- **Night Only**: Must be used at nighttime like other curse triggers

#### Configuration

The cursed book feature can be customized in `config.yml`:

```yaml
cursedBook:
  enabled: true                    # Enable/disable cursed book feature
  pickupRange: 10                  # Range for zombie to detect book
  enableRecipe: true               # Enable crafting recipe
  enableActivationEffects: true    # Enable visual effects on activation
```

### Curse Reset Conditions

**The curse will automatically reset in the following situations:**

- **Player Death**: All progress is lost and a cooldown is applied
- **Player Quit/Rejoin**: Progress is lost and a cooldown is applied
- **Admin Reset**: Administrators can manually reset any player's curse

**After a reset, you must wait for the cooldown period (default: 5 minutes) before starting a new curse.**

### Admin Commands

Administrators can manage player curses using the following commands:

- `/curse start <player>` - Force start a curse on a specific player
- `/curse stop <player>` - Force stop a specific player's curse
- `/curse reset <player>` - Reset a player's curse and apply cooldown

### Commands

- `/curse start [player]` - Force start a curse (admin only)
- `/curse stop [player]` - Force stop current curse (admin only)
- `/curse reset [player]` - Reset a curse and apply cooldown (admin only)
- `/curse book [player]` - Give a cursed book to yourself or another player (admin only)
- `/curse leaderboard` - View curse statistics and rankings
- `/curse reload` - Reload plugin configuration (admin only)
- `/curse help` - Show available commands

### Permissions

- `curse.admin` - Full access to all commands (default: op)
- `curse.use` - Basic usage and leaderboard access (default: true)
- `curse.start` - Can force start curses (default: op)
- `curse.stop` - Can force stop curses (default: op)
- `curse.reset` - Can reset curses and apply cooldown (default: op)
- `curse.reload` - Can reload configuration (default: op)

## Configuration

The plugin creates a `config.yml` file with the following options:

```yaml
plague:
  maxRounds: 7                    # Maximum rounds before final wave
  scaleWithXP: true              # Scale difficulty with player level
  spawnRadius: 20                # Mob spawn radius around player
  timeLimitPerRound: 60          # Time limit per round (0 = no limit)
  allowTerrainDamage: false      # Allow mobs to damage terrain
  maxActivePlagues: 3            # Max simultaneous plagues on server
  visualEffects: true            # Enable particle and sound effects
  combatRadius: 30               # Maximum distance from start location
  minDistanceFromVillages: 100   # Minimum distance from villages
  resetCooldownMinutes: 5        # Cooldown after curse reset (minutes)

cursedBook:
  enabled: true                   # Enable cursed book feature
  pickupRange: 10                 # Range for zombie to detect book (blocks)
  enableRecipe: true              # Enable crafting recipe
  enableActivationEffects: true   # Enable visual effects on activation

rewards:
  chestLoot:                     # Configurable loot tables
    common: [...]
    uncommon: [...]
    rare: [...]
    legendary: [...]

leaderboard:
  enabled: true                  # Enable leaderboard system
  displayCount: 10               # Number of entries to show
```

## Development

### Server Management

Use the provided Makefile for development tasks:

```bash
make setup      # Set up development environment
make start      # Start the test server
make stop       # Stop the test server
make restart    # Restart the server
make dev        # Build, install, and restart (development cycle)
make test       # Run plugin tests
make clean      # Clean server files
```

### Testing

The plugin includes comprehensive test coverage:

```bash
# Run all tests
make test

# Or use Maven
mvn test
```

## Technical Details

- **Target**: Minecraft Java Edition 1.21+
- **Server**: Paper 1.21.6+ (recommended)
- **Java Version**: 21+
- **Dependencies**: None (standalone plugin)

## Statistics Storage

- **Player Data**: Stored in Bukkit's Persistent Data Container (PDC)
- **Server Leaderboard**: Cached in memory and persisted to `leaderboard.yml`
- **Automatic Saving**: All data is automatically saved on server shutdown
