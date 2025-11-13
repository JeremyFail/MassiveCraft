package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.ChatMode;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Represents the /f c help command.
 * Displays available chat modes and subcommands with descriptions.
 */
public class CmdFactionsChatHelp extends FactionsCommand
{
    public CmdFactionsChatHelp()
    {
        setDesc("Display help for chat commands and modes");
        addAliases("help", "h", "?");
    }
    
    @Override
    public void perform()
    {
        msender.message(ChatColor.GOLD + "___________.[ " + ChatColor.DARK_GREEN + "Help for command \"chat\"" + ChatColor.GOLD + " ].___________");
        
        // Chat Modes Section
        msender.message(ChatColor.YELLOW + "Chat Modes:");
        msender.message(ChatColor.GRAY + "Use " + ChatColor.AQUA + "/f c <mode>" + ChatColor.GRAY 
            + " to switch modes or " + ChatColor.AQUA + "/f c <mode> <message>" + ChatColor.GRAY 
            + " for quick messages.");
        
        // Display available chat modes using the helper method
        List<ChatMode> availableModes = ChatMode.getAvailableChatModes(msender.getPlayer());
        for (ChatMode mode : availableModes)
        {
            String modeName = mode.name().toLowerCase();
            String alias = mode.getAlias();
            String description = mode.getDescription();
            msender.message(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + modeName + ChatColor.GRAY + 
                " (or " + ChatColor.LIGHT_PURPLE + alias + ChatColor.GRAY + ")" + ChatColor.WHITE + " - " + ChatColor.AQUA + description);
        }
        
        msender.message("");
        
        // Subcommands Section
        Player player = msender.getPlayer();
        if (player.hasPermission("factions.chat.ignore") 
                || player.hasPermission("factions.chat.ignore.admin") 
                || player.hasPermission("factions.chat.reload"))
        {
            msender.message(ChatColor.YELLOW + "Subcommands:");
            msender.message(ChatColor.GRAY + "Use " + ChatColor.LIGHT_PURPLE + "/f c <subcommand>" 
                    + ChatColor.GRAY + " to run other chat commands.");
            if (player.hasPermission("factions.chat.ignore.admin"))
            {
                msender.message(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "ignore [playerToUpdate] <player>"
                        + ChatColor.WHITE + " - " + ChatColor.AQUA + "Add players to the ignore list of yourself or another player");
                msender.message(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "unignore [playerToUpdate] <player>"
                        + ChatColor.WHITE + " - " + ChatColor.AQUA + "Remove players from the ignore list of yourself or another player");
                msender.message(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "ignorelist [player]"
                        + ChatColor.WHITE + " - " + ChatColor.AQUA + "View the ignored list of yourself or another player");
            }
            else if (player.hasPermission("factions.chat.ignore"))
            {
                msender.message(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "ignore <player>"
                        + ChatColor.WHITE + " - " + ChatColor.AQUA + "Add a player to your ignore list");
                msender.message(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "unignore <player>"
                        + ChatColor.WHITE + " - " + ChatColor.AQUA + "Remove a player from your ignore list");
                msender.message(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "ignorelist"
                        + ChatColor.WHITE + " - " + ChatColor.AQUA + "View your ignore list");
            }

            if (player.hasPermission("factions.chat.reload"))
            {
                msender.message(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "reload"
                        + ChatColor.WHITE + " - " + ChatColor.AQUA + "Reload FactionsChat configuration");
            }
        }
    }
    

}