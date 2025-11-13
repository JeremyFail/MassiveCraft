package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.TypeChatMode;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the <code>/f c {channel}</code> command.
 * Supports switching chat channels, as well as sending a single message to a channel.
 */
public class CmdFactionsChat extends FactionsCommand
{
    // Child command instances for manual routing
    private final CmdFactionsChatHelp helpCommand = new CmdFactionsChatHelp();
    private final CmdFactionsChatIgnore ignoreCommand = new CmdFactionsChatIgnore();
    private final CmdFactionsChatUnignore unignoreCommand = new CmdFactionsChatUnignore();
    private final CmdFactionsChatIgnoreList ignoreListCommand = new CmdFactionsChatIgnoreList();
    private final CmdFactionsChatReload reloadCommand = new CmdFactionsChatReload();
    
    public CmdFactionsChat()
    {
        addParameter(TypeChatMode.getInstance(), false, "chat mode");
        addParameter(TypeString.get(), false, "message", "message", true);
        setDesc("Switches chat modes, sends a quick message to a channel, or manages other faction chat settings");
        addAliases("chat", "c");
        
        // Subcommands will be handled manually through the perform() method custom routing system
    }

    @Override
    public void perform()
    {
        // Get the first argument, which should be a chat mode
        String firstArg = arg();
        
        // If no arguments provided, show current chat mode and help info
        if (firstArg == null)
        {
            ChatMode currentMode = ChatMode.getChatModeForPlayer(msender.getPlayer());
            msender.message(ChatColor.YELLOW + "Current chat mode: " + ChatColor.AQUA + currentMode.name().toLowerCase());
            msender.message(ChatColor.GRAY + "Use " + ChatColor.LIGHT_PURPLE + "/f c <mode>" + ChatColor.GRAY + " to switch modes.");
            msender.message(ChatColor.GRAY + "Use " + ChatColor.AQUA + "/f c help" + ChatColor.GRAY + " to see all available commands and modes.");
            return;
        }

        // Check if it's a subcommand first
        if (isSubcommand(firstArg))
        {
            routeToSubcommand(firstArg);
            return;
        }

        // Try to parse as chat mode
        ChatMode chatMode = TypeChatMode.getInstance().read(firstArg, msender.getPlayer());
        if (chatMode == null)
        {
            msender.message(ChatColor.RED + "Invalid chat mode or command: " + ChatColor.LIGHT_PURPLE + firstArg);
            msender.message(ChatColor.GRAY + "Use " + ChatColor.AQUA + "/f c help" + ChatColor.GRAY + " to see available commands and modes.");
            return;
        }

        // For faction-related chat modes, check if the player is in a faction
        if (msender.getFaction().isNone() && 
            (chatMode == ChatMode.FACTION || chatMode == ChatMode.ALLY || chatMode == ChatMode.TRUCE || chatMode == ChatMode.ENEMY)) 
        {
            msender.message(ChatColor.RED + "Cannot switch to that chat mode as you are not in a faction");
            return;
        }

        // Validate permissions for the chat mode
        if (!msender.getPlayer().hasPermission("factions.chat." + chatMode.name().toLowerCase())) 
        {
            msender.message(ChatColor.RED + "Invalid chat mode or command: " + ChatColor.LIGHT_PURPLE + firstArg);
            return;
        }
        
        // If the player is sending a quick message (not switching to the channel)
        String msg = arg();
        if (msg != null)
        {
            FactionsChat.qmPlayers.put(msender.getPlayer().getUniqueId(), chatMode);
            msender.getPlayer().chat(msg);
        }
        // Otherwise, switch the chat mode
        else
        {
            FactionsChat.instance.getPlayerChatModes().put(msender.getUuid(), chatMode);
            msender.message(ChatColor.YELLOW + "Chat mode set to: " + ChatColor.AQUA + chatMode.name().toLowerCase());
        }
    }
    
    /**
     * Check if the given argument is a subcommand
     */
    private boolean isSubcommand(String arg)
    {
        if (arg == null) return false;
        
        String lower = arg.toLowerCase();
        return (lower.equals("help") || lower.equals("h") || lower.equals("?")) ||
               (lower.equals("ignore")) ||
               (lower.equals("unignore")) ||
               (lower.equals("ignorelist")) ||
               (lower.equals("reload"));
    }
    
    /**
     * Route to the appropriate subcommand
     */
    private void routeToSubcommand(String subcommand)
    {
        // Set up execution context for child commands
        String lower = subcommand.toLowerCase();
        
        try
        {
            
            if (helpCommand.getAliases().contains(lower))
            {
                setupChildCommand(helpCommand);
                helpCommand.perform();
            }
            else if (ignoreCommand.getAliases().contains(lower))
            {
                setupChildCommand(ignoreCommand);
                ignoreCommand.perform();
            }
            else if (unignoreCommand.getAliases().contains(lower))
            {
                setupChildCommand(unignoreCommand);
                unignoreCommand.perform();
            }
            else if (ignoreListCommand.getAliases().contains(lower))
            {
                setupChildCommand(ignoreListCommand);
                ignoreListCommand.perform();
            }
            else if (reloadCommand.getAliases().contains(lower))
            {
                setupChildCommand(reloadCommand);
                reloadCommand.perform();
            }
            else
            {
                msender.message(ChatColor.RED + "Invalid subcommand \"" + ChatColor.LIGHT_PURPLE + subcommand + ChatColor.RED + "\".");
                msender.message(ChatColor.GRAY + "Use " + ChatColor.AQUA + "/f c help" + ChatColor.GRAY + " to see all available subcommands and modes.");
            }
        }
        catch (Exception e)
        {
            msender.message(ChatColor.RED + "An unexpected error occurred executing subcommand \"" + ChatColor.LIGHT_PURPLE + subcommand + ChatColor.RED + "\".");
            e.printStackTrace();
        }
    }
    
    /**
     * Set up execution context for child commands
     */
    private void setupChildCommand(FactionsCommand childCommand)
    {
        // Create a new mutable list to avoid unmodifiable list issues
        List<String> allArgs = this.getArgs();
        List<String> childArgs = new ArrayList<>();
        
        // TODO: DEBUG - Check args being passed to child command
        System.out.println("Setting up child command: " + childCommand.getClass().getSimpleName());
        System.out.println("All args: " + allArgs);

        // Copy all args except the first one (subcommand name)
        for (int i = 1; i < allArgs.size(); i++)
        {
            childArgs.add(allArgs.get(i));
        }
        
        // Set up child command context
        childCommand.setArgs(childArgs);
        childCommand.sender = this.sender;
        childCommand.msender = this.msender;
        childCommand.me = this.me;
        childCommand.senderIsConsole = this.senderIsConsole;
        childCommand.nextArg = 0;
    }
    
    @Override
    protected List<String> getTabCompletionsArg(List<String> args, CommandSender sender)
    {
        if (sender == null || !(sender instanceof Player)) return new ArrayList<>();

        // If we're completing the first argument, show chat modes and subcommands
        if (args.size() == 1)
        {
            List<String> completions = new ArrayList<>();
            String input = args.get(0).toLowerCase();
            
            // Add chat modes that the player has permission for
            if (sender instanceof Player)
            {
                completions.addAll(TypeChatMode.getInstance().getTabList(sender, args.get(0)));
            }

            // Add subcommand aliases based on permissions
            for (String alias : helpCommand.getAliases())
            {
                if (alias.startsWith(input)) completions.add(alias);
            }

            if (sender.hasPermission("factions.chat.ignore") || sender.hasPermission("factions.chat.ignore.admin"))
            {
                for (String alias : ignoreCommand.getAliases())
                {
                    if (alias.startsWith(input)) completions.add(alias);
                }
                for (String alias : unignoreCommand.getAliases())
                {
                    if (alias.startsWith(input)) completions.add(alias);
                }
                for (String alias : ignoreListCommand.getAliases())
                {
                    if (alias.startsWith(input)) completions.add(alias);
                }
            }

            if (sender.hasPermission("factions.chat.reload"))
            {
                for (String alias : reloadCommand.getAliases())
                {
                    if (alias.startsWith(input)) completions.add(alias);
                }
            }
            
            return completions;
        }
        
        // For subsequent arguments, delegate to appropriate child command or use default behavior
        if (args.size() > 1)
        {
            String subcommand = args.get(0).toLowerCase();
            
            // Create a modified args list for the child command
            List<String> childArgs = new ArrayList<>(args.subList(1, args.size()));
            
            if (sender.hasPermission("factions.chat.ignore") || sender.hasPermission("factions.chat.ignore.admin"))
            {
                if (ignoreCommand.getAliases().contains(subcommand))
                {
                    return ignoreCommand.getTabCompletions(childArgs, sender);
                }
                else if (unignoreCommand.getAliases().contains(subcommand))
                {
                    return unignoreCommand.getTabCompletions(childArgs, sender);
                }
                else if (ignoreListCommand.getAliases().contains(subcommand))
                {
                    return ignoreListCommand.getTabCompletions(childArgs, sender);
                }
            }
        }

        // Return empty list if no completions found
        return new ArrayList<>();
    }
}
