package org.xpfarm.curse.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.xpfarm.curse.CursePlugin;
import org.xpfarm.curse.utils.MessageUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CurseCommand implements CommandExecutor, TabCompleter {
    
    private final CursePlugin plugin;
    
    public CurseCommand(CursePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "book":
                return handleBook(sender, args);
            case "leaderboard":
            case "lb":
                return handleLeaderboard(sender, args);
            case "reload":
                return handleReload(sender);
            case "help":
            default:
                showHelp(sender);
                return true;
        }
    }
    
    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("curse.admin")) {
            MessageUtil.sendMessage(sender, Component.text("You don't have permission to force start a curse!", NamedTextColor.RED));
            return true;
        }
        
        // Check if targeting another player
        if (args.length > 1) {
            // Admin starting curse on another player
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendMessage(sender, Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }
            
            // Check if target already has an active plague
            if (plugin.getPlagueManager().hasActivePlague(target)) {
                MessageUtil.sendMessage(sender, Component.text(target.getName() + " already has an active curse!", NamedTextColor.RED));
                return true;
            }
            
            // Start the plague, bypassing cooldown
            boolean started = plugin.getPlagueManager().startPlague(target, true);
            if (started) {
                MessageUtil.sendMessage(sender, Component.text("Started curse for " + target.getName() + "!", NamedTextColor.GREEN));
                MessageUtil.sendMessage(target, Component.text("An admin has started a curse on you!", NamedTextColor.GOLD));
            } else {
                MessageUtil.sendMessage(sender, Component.text("Failed to start curse for " + target.getName() + ". Check conditions and try again.", NamedTextColor.RED));
            }
            return true;
        }
        
        // Self-start (original behavior)
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, Component.text("Only players can start a curse on themselves!", NamedTextColor.RED));
            return true;
        }
        
        // Check if player already has an active plague
        if (plugin.getPlagueManager().hasActivePlague(player)) {
            MessageUtil.sendMessage(sender, Component.text("You already have an active curse!", NamedTextColor.RED));
            return true;
        }
        
        // Check cooldown
        if (plugin.getCooldownManager().hasCooldown(player)) {
            long remainingSeconds = plugin.getCooldownManager().getRemainingCooldownSeconds(player);
            long remainingMinutes = remainingSeconds / 60;
            MessageUtil.sendMessage(sender, Component.text("You must wait " + remainingMinutes + " minutes before starting another curse!", NamedTextColor.RED));
            return true;
        }
        
        // Start the plague
        boolean started = plugin.getPlagueManager().startPlague(player, true); // Bypass cooldown for admin command
        if (started) {
            MessageUtil.sendMessage(sender, Component.text("The curse has begun!", NamedTextColor.GOLD));
        } else {
            MessageUtil.sendMessage(sender, Component.text("Failed to start curse. Check conditions and try again.", NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("curse.admin")) {
            MessageUtil.sendMessage(sender, Component.text("You don't have permission to stop a curse!", NamedTextColor.RED));
            return true;
        }
        
        // Check if targeting another player
        if (args.length > 1) {
            // Admin stopping curse on another player
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendMessage(sender, Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }
            
            // Stop the plague
            boolean stopped = plugin.getPlagueManager().stopPlague(target);
            if (stopped) {
                MessageUtil.sendMessage(sender, Component.text("Stopped curse for " + target.getName() + "!", NamedTextColor.GREEN));
                MessageUtil.sendMessage(target, Component.text("An admin has stopped your curse!", NamedTextColor.YELLOW));
            } else {
                MessageUtil.sendMessage(sender, Component.text(target.getName() + " doesn't have an active curse!", NamedTextColor.RED));
            }
            return true;
        }
        
        // Self-stop (original behavior)
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, Component.text("Only players can stop their own curse!", NamedTextColor.RED));
            return true;
        }
        
        // Stop the plague
        boolean stopped = plugin.getPlagueManager().stopPlague(player);
        if (stopped) {
            MessageUtil.sendMessage(sender, Component.text("The curse has been lifted!", NamedTextColor.GREEN));
        } else {
            MessageUtil.sendMessage(sender, Component.text("You don't have an active curse!", NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("curse.admin")) {
            MessageUtil.sendMessage(sender, Component.text("You don't have permission to reset a curse!", NamedTextColor.RED));
            return true;
        }
        
        // Check if targeting another player
        if (args.length > 1) {
            // Admin resetting curse on another player
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendMessage(sender, Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }
            
            // Reset the plague and cooldown
            plugin.getPlagueManager().resetPlague(target, true);
            MessageUtil.sendMessage(sender, Component.text("Reset curse for " + target.getName() + "!", NamedTextColor.GREEN));
            MessageUtil.sendMessage(target, Component.text("An admin has reset your curse!", NamedTextColor.YELLOW));
            return true;
        }
        
        // Self-reset
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, Component.text("Only players can reset their own curse!", NamedTextColor.RED));
            return true;
        }
        
        // Reset the plague
        plugin.getPlagueManager().resetPlague(player, true);
        MessageUtil.sendMessage(sender, Component.text("Your curse has been reset!", NamedTextColor.GREEN));
        
        return true;
    }
    
    private boolean handleLeaderboard(CommandSender sender, String[] args) {
        if (!sender.hasPermission("curse.use")) {
            MessageUtil.sendMessage(sender, Component.text("You don't have permission to view the leaderboard!", NamedTextColor.RED));
            return true;
        }
        
        plugin.getLeaderboardManager().displayLeaderboard(sender);
        return true;
    }
    
    private boolean handleBook(CommandSender sender, String[] args) {
        if (!sender.hasPermission("curse.admin")) {
            MessageUtil.sendMessage(sender, Component.text("You don't have permission to give cursed books!", NamedTextColor.RED));
            return true;
        }
        
        // Determine target player
        Player target;
        if (args.length > 1) {
            // Admin giving book to another player
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendMessage(sender, Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }
        } else {
            // Self-give (only if sender is a player)
            if (!(sender instanceof Player)) {
                MessageUtil.sendMessage(sender, Component.text("Console must specify a target player!", NamedTextColor.RED));
                MessageUtil.sendMessage(sender, Component.text("Usage: /curse book <player>", NamedTextColor.YELLOW));
                return true;
            }
            target = (Player) sender;
        }
        
        // Check if player has inventory space
        if (target.getInventory().firstEmpty() == -1) {
            MessageUtil.sendMessage(sender, Component.text("Player's inventory is full!", NamedTextColor.RED));
            return true;
        }
        
        // Give the cursed book
        target.getInventory().addItem(plugin.getCursedBookManager().createCursedBook());
        
        // Send messages
        MessageUtil.sendMessage(target, Component.text("You have received a ", NamedTextColor.GRAY)
            .append(Component.text("ZP25", NamedTextColor.DARK_RED))
            .append(Component.text(" cursed book!", NamedTextColor.GRAY)));
        
        if (!target.equals(sender)) {
            MessageUtil.sendMessage(sender, Component.text("Given cursed book to " + target.getName() + "!", NamedTextColor.GREEN));
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("curse.reload")) {
            MessageUtil.sendMessage(sender, Component.text("You don't have permission to reload the plugin!", NamedTextColor.RED));
            return true;
        }
        
        plugin.reloadPlugin();
        MessageUtil.sendMessage(sender, Component.text("Plugin configuration reloaded!", NamedTextColor.GREEN));
        return true;
    }
    
    private void showHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, Component.text("=== The Curse Commands ===", NamedTextColor.GOLD));
        MessageUtil.sendMessage(sender, Component.text("/curse start [player]", NamedTextColor.YELLOW)
            .append(Component.text(" - Start a curse (admin)", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse stop [player]", NamedTextColor.YELLOW)
            .append(Component.text(" - Stop a curse (admin)", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse reset [player]", NamedTextColor.YELLOW)
            .append(Component.text(" - Reset a curse and set cooldown (admin)", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse book [player]", NamedTextColor.YELLOW)
            .append(Component.text(" - Give a cursed book (admin)", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse leaderboard", NamedTextColor.YELLOW)
            .append(Component.text(" - View curse statistics", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse reload", NamedTextColor.YELLOW)
            .append(Component.text(" - Reload plugin configuration (admin)", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse help", NamedTextColor.YELLOW)
            .append(Component.text(" - Show this help message", NamedTextColor.GRAY)));
        
        MessageUtil.sendMessage(sender, Component.text(""));
        MessageUtil.sendMessage(sender, Component.text("To start a curse naturally:", NamedTextColor.AQUA));
        MessageUtil.sendMessage(sender, Component.text("• Drink a Bad Omen potion at night", NamedTextColor.WHITE));
        MessageUtil.sendMessage(sender, Component.text("• Drop ZP25 book near a zombie", NamedTextColor.WHITE));
        MessageUtil.sendMessage(sender, Component.text("• Craft ZP25 with custom recipe", NamedTextColor.WHITE));
        MessageUtil.sendMessage(sender, Component.text(""));
        MessageUtil.sendMessage(sender, Component.text("Note: Curse resets on death or quit/rejoin!", NamedTextColor.RED));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> subCommands = Arrays.asList("start", "stop", "reset", "book", "leaderboard", "reload", "help");
            
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
            return completions;
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("start") || subCommand.equals("stop") || subCommand.equals("reset") || subCommand.equals("book")) {
                // Tab complete player names for admin commands
                List<String> completions = new ArrayList<>();
                String partial = args[1].toLowerCase();
                
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
                return completions;
            }
        }
        
        return new ArrayList<>();
    }
}
