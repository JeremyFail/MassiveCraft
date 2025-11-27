package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;
import com.massivecraft.massivecore.util.Txt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the /f c ignorelist command.
 * Shows a player's ignore list with pagination.
 * Admins can view other players' ignore lists with /f c ignorelist {player} [page]
 */
public class CmdFactionsChatIgnoreList extends FactionsCommand
{
    
    public CmdFactionsChatIgnoreList()
    {
        addParameter(TypeString.get(), false, "player");
        addParameter(Parameter.getPage());
        setDesc("View your ignore list");
        addAliases("ignorelist", "ignored");
    }
    
    @Override
    public void perform()
    {
        // Check basic permission
        if (!msender.getPlayer().hasPermission("factions.chat.ignore")) {
            msender.message(ChatColor.RED + "You don't have permission to use chat ignore commands.");
            return;
        }
        
        // Parse arguments to get page number
        String firstArg = arg();
        String secondArg = arg();
        
        int pageNum = 1;
        String pageToParse = null;

        // Assume running as admin command if both args are present
        if (firstArg != null && secondArg != null)
        {
            pageToParse = secondArg;
        }
        else if (firstArg != null)
        {
            // Could be either other player name or page number
            try
            {
                pageNum = Integer.parseInt(firstArg);
            }
            catch (NumberFormatException e)
            {
                // It's a player name, not a page number
            }
        }

        // Parse page number if provided - defaults to 1 if not provided
        if (pageToParse != null)
        {
            try
            {
                pageNum = Integer.parseInt(pageToParse);
            }
            catch (NumberFormatException e)
            {
                msender.message(ChatColor.RED + "\"" + ChatColor.LIGHT_PURPLE + pageToParse + ChatColor.RED + "\" is not a number.");
                return;
            }
        }
        
        // Create basic pager without navigation (for direct usage)
        final Pager<UUID> pager = new Pager<UUID>();
        pager.setTitle("Ignore List");  
        pager.setNumber(pageNum);
        pager.setSender(sender);
        pager.setCommand(null); // Disable navigation for direct usage
        
        performWithPager(pager);
    }
    
    /**
     * Perform ignorelist display with provided pager (called from parent command)
     */
    public void performWithPager(Pager<UUID> pager)
    {
        // Use the existing logic but with the provided pager
        // Extract the argument parsing from the original perform method
        String firstArg = arg();
        String secondArg = arg();
        
        String otherPlayerName = null;
        String targetPlayerName = null;
        UUID targetPlayerUuid = null;
        boolean isAdminCommand = false;

        // Assume running as admin command if both args are present
        if (firstArg != null && secondArg != null)
        {
            otherPlayerName = firstArg;
            isAdminCommand = true;
        }
        else if (firstArg != null)
        {
            // Could be either other player name or page number
            try
            {
                Integer.parseInt(firstArg); // Just test if it's a number
                // If we get here, it's a page number, no player specified
            }
            catch (NumberFormatException e)
            {
                otherPlayerName = firstArg;
            }
        }

        // Admin command: /f c ignorelist {player} [page]
        if (otherPlayerName != null)
        {
            // Validate admin permission
            if (!msender.getPlayer().hasPermission("factions.chat.ignore.admin"))
            {
                msender.message(ChatColor.RED + "You don't have permission to view other players' ignore lists.");
                return;
            }

            // Parse the target player
            OfflinePlayer targetPlayer = FactionsChat.instance.getIgnoreManager().getPlayerByNameOrUuid(otherPlayerName);
            if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()))
            {
                msender.message(ChatColor.RED + "Player not found: " + ChatColor.LIGHT_PURPLE + otherPlayerName);
                return;
            }

            isAdminCommand = true;
            targetPlayerUuid = targetPlayer.getUniqueId();
            targetPlayerName = targetPlayer.getName();
        }
        else
        {
            // Regular command: /f c ignorelist [page]
            targetPlayerUuid = msender.getPlayer().getUniqueId();
            targetPlayerName = msender.getPlayer().getName();
        }
        
        final CommandSender sender = this.sender;
        final UUID finalTargetUuid = targetPlayerUuid;
        final String finalTargetName = targetPlayerName;
        final boolean finalIsAdminCommand = isAdminCommand;
        
        // Update pager title based on whether it's an admin command
        String title = finalIsAdminCommand ? finalTargetName + "'s Ignore List" : "Your Ignore List";
        pager.setTitle(title);
        pager.setMsonifier((Stringifier<UUID>) (ignoredUuid, index) -> {
            OfflinePlayer ignoredPlayer = Bukkit.getOfflinePlayer(ignoredUuid);
            String playerName = ignoredPlayer.getName();
            if (playerName == null)
            {
                playerName = ignoredUuid.toString(); // Fallback to UUID if name is null
            }
            
            String status = ignoredPlayer.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.GRAY + "Offline";
            return Txt.parse("<i>%s <i>- %s", playerName, status);
        });
        
        // Run asynchronously to avoid blocking the main thread during player name lookups
        Bukkit.getScheduler().runTaskAsynchronously(FactionsChat.instance, () -> {
            Set<UUID> ignoredPlayers = FactionsChat.instance.getIgnoreManager().getIgnoredPlayers(finalTargetUuid);
            
            if (ignoredPlayers.isEmpty())
            {
                // Schedule back to main thread to send message
                Bukkit.getScheduler().runTask(FactionsChat.instance, () -> {
                    if (finalIsAdminCommand)
                    {
                        sender.sendMessage(ChatColor.YELLOW + finalTargetName + " is not ignoring anyone.");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.YELLOW + "You are not ignoring anyone.");
                    }
                });
                return;
            }
            
            // Convert Set to List for pager
            List<UUID> ignoredPlayersList = new ArrayList<>(ignoredPlayers);
            pager.setItems(ignoredPlayersList);
            
            // Schedule back to main thread to send pager message
            Bukkit.getScheduler().runTask(FactionsChat.instance, pager::message);
        });
    }
    
    /**
     * Tab completion for ignorelist command
     */
    @Override
    public List<String> getTabCompletions(List<String> args, CommandSender sender)
    {
        List<String> completions = new ArrayList<>();
        
        if (args.size() == 1)
        {
            String input = args.get(0).toLowerCase();
            
            // Only tab complete for admins - show player names
            if (sender.hasPermission("factions.chat.ignore.admin"))
            {
                // Add online player names
                for (Player onlinePlayer : Bukkit.getOnlinePlayers())
                {
                    if (onlinePlayer.getName().toLowerCase().startsWith(input))
                    {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}