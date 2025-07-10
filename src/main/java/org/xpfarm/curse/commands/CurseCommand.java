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
        if (!sender.hasPermission("curse.start")) {
            MessageUtil.sendMessage(sender, Component.text("You don't have permission to start a curse!", NamedTextColor.RED));
            return true;
        }
        
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, Component.text("Only players can start a curse!", NamedTextColor.RED));
            return true;
        }
        
        // Check if player already has an active plague
        if (plugin.getPlagueManager().hasActivePlague(player)) {
            MessageUtil.sendMessage(sender, Component.text("You already have an active curse!", NamedTextColor.RED));
            return true;
        }
        
        // Start the plague
        boolean started = plugin.getPlagueManager().startPlague(player);
        if (started) {
            MessageUtil.sendMessage(sender, Component.text("The curse has begun!", NamedTextColor.GOLD));
        } else {
            MessageUtil.sendMessage(sender, Component.text("Failed to start curse. Check conditions and try again.", NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("curse.stop")) {
            MessageUtil.sendMessage(sender, Component.text("You don't have permission to stop a curse!", NamedTextColor.RED));
            return true;
        }
        
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, Component.text("Only players can stop their curse!", NamedTextColor.RED));
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
    
    private boolean handleLeaderboard(CommandSender sender, String[] args) {
        if (!sender.hasPermission("curse.use")) {
            MessageUtil.sendMessage(sender, Component.text("You don't have permission to view the leaderboard!", NamedTextColor.RED));
            return true;
        }
        
        plugin.getLeaderboardManager().displayLeaderboard(sender);
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
        MessageUtil.sendMessage(sender, Component.text("/curse start", NamedTextColor.YELLOW)
            .append(Component.text(" - Force start a curse (admin)", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse stop", NamedTextColor.YELLOW)
            .append(Component.text(" - Force stop current curse (admin)", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse leaderboard", NamedTextColor.YELLOW)
            .append(Component.text(" - View curse statistics", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse reload", NamedTextColor.YELLOW)
            .append(Component.text(" - Reload plugin configuration (admin)", NamedTextColor.GRAY)));
        MessageUtil.sendMessage(sender, Component.text("/curse help", NamedTextColor.YELLOW)
            .append(Component.text(" - Show this help message", NamedTextColor.GRAY)));
        
        MessageUtil.sendMessage(sender, Component.text(""));
        MessageUtil.sendMessage(sender, Component.text("To start a curse naturally:", NamedTextColor.AQUA));
        MessageUtil.sendMessage(sender, Component.text("Drink a Bad Omen potion at night!", NamedTextColor.WHITE));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> subCommands = Arrays.asList("start", "stop", "leaderboard", "reload", "help");
            
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
            return completions;
        }
        
        return new ArrayList<>();
    }
}
