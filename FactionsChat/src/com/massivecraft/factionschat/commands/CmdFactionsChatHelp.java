package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the /f c help command.
 * Displays available chat modes and subcommands with descriptions.
 */
public class CmdFactionsChatHelp extends FactionsCommand
{

    public CmdFactionsChatHelp()
    {
        addParameter(Parameter.getPage());
        setDesc("Display help for chat commands and modes");
        addAliases("help", "h", "?");
    }
    
    @Override
    public void perform()
    {
        // Default implementation - create a basic pager
        // This is used when called directly (not through parent)
        int pageNum = 1;
        String pageArg = arg();
        
        if (pageArg != null)
        {
            try
            {
                pageNum = Integer.parseInt(pageArg);
            }
            catch (NumberFormatException e)
            {
                msender.message(ChatColor.RED + "\"" + ChatColor.LIGHT_PURPLE + pageArg + ChatColor.RED + "\" is not a number.");
                return;
            }
        }
        
        // Create basic pager without navigation
        final Pager<String> pager = new Pager<String>();
        pager.setTitle("Help for command \"chat\"");  
        pager.setNumber(pageNum);
        pager.setMsonifier((Stringifier<String>) (line, index) -> line);
        pager.setSender(sender);
        pager.setCommand(null);
        
        performWithPager(pager);
    }
    
    /**
     * Perform help display with provided pager (called from parent command)
     */
    public void performWithPager(Pager<String> pager)
    {
        // Build help content as list of strings
        List<String> helpLines = buildHelpContent();
        
        // Configure the pager with our content
        pager.setMsonifier((Stringifier<String>) (line, index) -> line);
        pager.setItems(helpLines);
        pager.message();
    }
    
    /**
     * Build the help content as a list of strings for paging
     */
    private List<String> buildHelpContent()
    {
        List<String> lines = new ArrayList<>();
        Player player = msender.getPlayer();
        
        // Chat Modes Section
        lines.add(ChatColor.AQUA + "Chat Modes:");
        lines.add(ChatColor.GRAY + "Use " + ChatColor.DARK_AQUA + "/f c <mode>" + ChatColor.GRAY 
            + " to switch modes or " + ChatColor.DARK_AQUA + "/f c <mode> <message>" + ChatColor.GRAY 
            + " for quick messages.");
        
        // Display available chat modes
        List<ChatMode> availableModes = ChatMode.getAvailableChatModes(player);
        for (ChatMode mode : availableModes)
        {
            String modeName = mode.name().toLowerCase();
            String alias = mode.getAlias();
            String description = mode.getDescription();
            lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + modeName + ChatColor.GRAY + 
                " (or " + ChatColor.LIGHT_PURPLE + alias + ChatColor.GRAY + ")" + ChatColor.WHITE + " - " + ChatColor.YELLOW + description);
        }
        
        // Add subcommands if player has any permissions
        if (player.hasPermission("factions.chat.ignore") 
                || player.hasPermission("factions.chat.ignore.admin") 
                || player.hasPermission("factions.chat.toggle")
                || player.hasPermission("factions.chat.toggle.admin")
                || player.hasPermission("factions.chat.reload"))
        {
            lines.add(""); // Empty line
            lines.add(ChatColor.AQUA + "Subcommands:");
            lines.add(ChatColor.GRAY + "Use " + ChatColor.DARK_AQUA + "/f c <subcommand>" 
                    + ChatColor.GRAY + " to run other chat commands.");
            
            if (player.hasPermission("factions.chat.ignore.admin"))
            {
                lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "ignore [playerToUpdate] <player>"
                        + ChatColor.WHITE + " - " + ChatColor.YELLOW + "Add players to the ignore list for yourself or another player");
                lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "unignore [playerToUpdate] <player>"
                        + ChatColor.WHITE + " - " + ChatColor.YELLOW + "Remove players from the ignore list for yourself or another player");
                lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "ignorelist [player]"
                        + ChatColor.WHITE + " - " + ChatColor.YELLOW + "View the ignore list for yourself or another player");
            }
            else if (player.hasPermission("factions.chat.ignore"))
            {
                lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "ignore <player>"
                        + ChatColor.WHITE + " - " + ChatColor.YELLOW + "Add a player to your ignore list");
                lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "unignore <player>"
                        + ChatColor.WHITE + " - " + ChatColor.YELLOW + "Remove a player from your ignore list");
                lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "ignorelist"
                        + ChatColor.WHITE + " - " + ChatColor.YELLOW + "View your ignore list");
            }
            
            if (player.hasPermission("factions.chat.toggle.admin"))
            {
                lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "toggle [player] <chatMode>"
                        + ChatColor.WHITE + " - " + ChatColor.YELLOW + "Toggle (disable/enable) chat modes for yourself or another player");
            }
            else if (player.hasPermission("factions.chat.toggle"))
            {
                lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "toggle <chatMode>"
                        + ChatColor.WHITE + " - " + ChatColor.YELLOW + "Toggle (disable/enable) specific chat modes");
            }

            if (player.hasPermission("factions.chat.reload"))
            {
                lines.add(ChatColor.YELLOW + "  - " + ChatColor.LIGHT_PURPLE + "reload"
                        + ChatColor.WHITE + " - " + ChatColor.YELLOW + "Reload FactionsChat configuration");
            }
        }
        
        return lines;
    }
    
    /**
     * Tab completion for help command
     */
    @Override
    public List<String> getTabCompletions(List<String> args, CommandSender sender)
    {
        List<String> completions = new ArrayList<>();
        
        if (args.size() == 1)
        {
            String input = args.get(0).toLowerCase();
            
            // Show page numbers
            if (input.isEmpty() || input.matches("^\\d.*"))
            {
                for (int i = 1; i <= 2; i++)
                {
                    String pageNum = String.valueOf(i);
                    if (pageNum.startsWith(input))
                    {
                        completions.add(pageNum);
                    }
                }
            }
        }
        
        return completions;
    }

}