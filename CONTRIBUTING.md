# Contributing to The Curse Plugin

Thank you for your interest in contributing to The Curse plugin! This document provides guidelines for contributing to the project.

## Environment Setup

- Java 21
- Minecraft Java Edition 1.21+
- Paper 1.21.6+ (recommended)
- Maven 3.6+
- Docker (for testing)

### Version Compatibility

- **Geyser/Floodgate**: Requires Minecraft 1.21.5+ or ViaVersion for compatibility
- **ViaVersion**: Automatically downloaded and installed for better cross-version support
- **Paper**: Latest 1.21.6 builds recommended for optimal performance

## Development Workflow

### Building the Plugin

```bash
# Build the plugin
make build

# Or use Maven directly
mvn clean package
```

The compiled JAR will be available in the `target/` directory.

### Testing

```bash
# Run unit tests
make test

# Set up test server
make setup

# Start test server
make start

# Quick development cycle
make dev
```

### Shell Script Testing

- Always test shell scripts with `bash -n` to check syntax
- Use `shellcheck` to catch common shell script issues
- Run `make lint` to check all shell scripts

## Code Standards

### Java Code

- Use `NamespacedKey` for custom recipes and items to avoid conflicts
- Use cached `ItemStack` instances for performance
- Use event-driven architecture to minimize performance overhead
- Always use configuration files for customizable options
- Follow Paper API best practices

### Console Output

All console messages must use consistent color and formatting:

- **Titles**: `NamedTextColor.GOLD` for section headers and plugin names
- **Commands**: `NamedTextColor.YELLOW` for command syntax
- **Descriptions**: `NamedTextColor.GRAY` for help text and descriptions
- **Lists**: `NamedTextColor.AQUA` for list labels, `NamedTextColor.WHITE` for list items
- **Success**: `NamedTextColor.GREEN` for successful operations
- **Errors**: `NamedTextColor.RED` for error messages

Example:
```java
sender.sendMessage(Component.text("=== The Curse Commands ===", NamedTextColor.GOLD));
sender.sendMessage(Component.text("/curse help", NamedTextColor.YELLOW)
    .append(Component.text(" - Show help message", NamedTextColor.GRAY)));
```

## Plugin Architecture

### Core Components

- **CursePlugin**: Main plugin class and initialization
- **PlagueManager**: Handles plague logic and mob spawning
- **LeaderboardManager**: Manages statistics and rankings
- **ConfigManager**: Configuration handling
- **Plague Model**: Represents individual plague instances

### Data Storage

- **Player Data**: Stored in Bukkit's Persistent Data Container (PDC)
- **Server Leaderboard**: Cached in memory and persisted to `leaderboard.yml`
- **Configuration**: YAML configuration files

## Testing Guidelines

### In-Game Testing

1. Use `make debug` for interactive testing
2. Test curse triggering with Bad Omen potions at night
3. Verify mob spawning and wave progression
4. Test reward chest spawning and loot generation
5. Verify leaderboard statistics tracking

### Commands for Testing

```bash
# Give Bad Omen potion
/give @p minecraft:potion{Potion:"minecraft:bad_omen"} 1

# Set time to night
/time set night

# Test curse commands
/curse start
/curse leaderboard
/curse stop
```

## Server Management

Use the provided Makefile commands:

- `make setup` - Prepare the server environment
- `make start` - Start the server
- `make stop` - Stop the server
- `make restart` - Restart the server
- `make status` - Check server status
- `make logs` - View server logs
- `make clean` - Clean up temporary files
- `make network` - Check network connectivity
- `make attach` - Attach to server console
- `make players` - List online players

## Docker Testing

```bash
# Build and test in Docker
make docker-test

# Or manually
docker-compose up -d
docker-compose logs -f
```

## Submitting Changes

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/your-feature-name`
3. **Make your changes** following the code standards
4. **Test your changes** thoroughly
5. **Run linting**: `make lint`
6. **Run tests**: `make test`
7. **Commit your changes**: `git commit -m "Add your feature"`
8. **Push to your fork**: `git push origin feature/your-feature-name`
9. **Create a Pull Request**

### Pull Request Guidelines

- Include a clear description of the changes
- Reference any related issues
- Include testing instructions
- Ensure all tests pass
- Follow the existing code style

## Bug Reports

When reporting bugs, please include:

- Minecraft version
- Paper version
- Plugin version
- Server logs (relevant portions)
- Steps to reproduce
- Expected vs actual behavior

## Feature Requests

For feature requests, please:

- Check existing issues first
- Provide a clear use case
- Explain how it fits with the plugin's goals
- Consider implementation complexity

## Code Review Process

All contributions will be reviewed for:

- Code quality and standards
- Performance impact
- Compatibility with target versions
- Test coverage
- Documentation updates

## License

By contributing to this project, you agree that your contributions will be licensed under the same license as the project.

## Questions?

If you have questions about contributing:

- Check the existing documentation
- Look through existing issues
- Create a new issue for discussion
- Contact the maintainers

Thank you for helping make The Curse plugin better!
