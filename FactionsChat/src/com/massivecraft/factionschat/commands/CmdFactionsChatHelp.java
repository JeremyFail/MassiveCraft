package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;
import com.massivecraft.massivecore.util.Txt;

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
                msender.message(Txt.parse("<b>\"<v>" + pageArg + "<b>\" is not a number."));
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
        lines.add(Txt.parse("<k>Chat Modes:"));
        lines.add(Txt.parse("<n>Use <k>/f c <mode><n> to switch modes, or type <k>:<mode> <message><n> / <k>:<letter> <message><n> in chat for a one-off message."));
        lines.add(Txt.parse("<n>Use <k>:<mode><n> alone (e.g. <k>:f<n>) to switch modes as well."));
        
        // Display available chat modes
        List<ChatMode> availableModes = ChatMode.getAvailableChatModes(player);
        for (ChatMode mode : availableModes)
        {
            String modeName = mode.name().toLowerCase();
            String alias = mode.getAlias();
            String description = mode.getDescription();
            lines.add(Txt.parse("<i>  - <v>" + modeName + "<i> (or <v>" + alias + "<i>)<white> - <i>" + description));
        }
        
        // Add subcommands if player has any permissions
        if (player.hasPermission("factions.chat.ignore") 
                || player.hasPermission("factions.chat.ignore.admin") 
                || player.hasPermission("factions.chat.toggle")
                || player.hasPermission("factions.chat.toggle.admin")
                || player.hasPermission("factions.chat.reload"))
        {
            lines.add(""); // Empty line
            lines.add(Txt.parse("<k>Subcommands:"));
            lines.add(Txt.parse("<n>Use <k>/f c <subcommand><n> to run other chat commands."));
            
            if (player.hasPermission("factions.chat.ignore.admin"))
            {
                lines.add(Txt.parse("<i>  - <v>ignore [playerToUpdate] <player><i> - <i>Add players to the ignore list for yourself or another player"));
                lines.add(Txt.parse("<i>  - <v>unignore [playerToUpdate] <player><i> - <i>Remove players from the ignore list for yourself or another player"));
                lines.add(Txt.parse("<i>  - <v>ignorelist [player]<i> - <i>View the ignore list for yourself or another player"));
            }
            else if (player.hasPermission("factions.chat.ignore"))
            {
                lines.add(Txt.parse("<i>  - <v>ignore <player><i> - <i>Add a player to your ignore list"));
                lines.add(Txt.parse("<i>  - <v>unignore <player><i> - <i>Remove a player from your ignore list"));
                lines.add(Txt.parse("<i>  - <v>ignorelist<i> - <i>View your ignore list"));
            }
            
            if (player.hasPermission("factions.chat.toggle.admin"))
            {
                lines.add(Txt.parse("<i>  - <v>toggle [player] <chatMode><i> - <i>Toggle (disable/enable) chat modes for yourself or another player"));
            }
            else if (player.hasPermission("factions.chat.toggle"))
            {
                lines.add(Txt.parse("<i>  - <v>toggle <chatMode><i> - <i>Toggle (disable/enable) specific chat modes"));
            }

            if (player.hasPermission("factions.chat.reload"))
            {
                lines.add(Txt.parse("<i>  - <v>reload<i> - <i>Reload FactionsChat configuration"));
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