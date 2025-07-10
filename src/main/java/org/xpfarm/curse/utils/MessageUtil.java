package org.xpfarm.curse.utils;

import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;

public class MessageUtil {
    
    /**
     * Send a message to a command sender (player or console)
     */
    public static void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(message);
    }
    
    /**
     * Send a message to a command sender with no prefix
     */
    public static void sendRawMessage(CommandSender sender, Component message) {
        sender.sendMessage(message);
    }
}
