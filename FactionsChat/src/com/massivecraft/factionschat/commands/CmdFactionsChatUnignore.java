package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the /f c unignore {player} command.
 * Allows players to remove other players from their ignore list.
 * Admins can manage other players' ignore lists with /f c unignore {playerToManage} {playerToUnignore}
 */
public class CmdFactionsChatUnignore extends FactionsCommand
{
    
    public CmdFactionsChatUnignore()
    {
        addParameter(TypeString.get(), "player");
        addParameter(TypeString.get(), false, "target player", "targetPlayer");
        setDesc("Remove a player from your ignore list or manage another player's ignore list (admin)");
        addAliases("unignore");
    }
    
    @Override
    public void perform()
    {
        // Check basic permission
        if (!msender.getPlayer().hasPermission("factions.chat.ignore"))
        {
            msender.message(ChatColor.RED + "You don't have permission to use chat ignore commands.");
            return;
        }
        
        // Parse arguments
        String firstArg = arg();
        String secondArg = arg();
        
        // At least one argument is required
        if (firstArg == null)
        {
            msender.message(ChatColor.RED + "You must specify a player to unignore.");
            return;
        }
        
        UUID ignoringPlayerUuid;
        String targetPlayerName;
        boolean isAdminCommand = false;
        
        // Determine if this is an admin command (2 arguments) or regular command (1 argument)
        if (secondArg != null)
        {
            // Admin command: /f c unignore {playerToManage} {playerToUnignore}
            if (!msender.getPlayer().hasPermission("factions.chat.ignore.admin"))
            {
                msender.message(ChatColor.RED + "You don't have permission to manage the ignore list of other players.");
                return;
            }
            
            OfflinePlayer managedPlayer = FactionsChat.instance.getIgnoreManager().getPlayerByNameOrUuid(firstArg);
            if (managedPlayer == null || (!managedPlayer.hasPlayedBefore() && !managedPlayer.isOnline()))
            {
                msender.message(ChatColor.RED + "Player not found: " + ChatColor.LIGHT_PURPLE + firstArg);
                return;
            }

            isAdminCommand = true;
            ignoringPlayerUuid = managedPlayer.getUniqueId();
            targetPlayerName = secondArg;
        }
        else
        {
            // Regular command: /f c unignore {player}
            ignoringPlayerUuid = msender.getPlayer().getUniqueId();
            targetPlayerName = firstArg;
        }
        
        // Get the target player to unignore
        OfflinePlayer targetPlayer = FactionsChat.instance.getIgnoreManager().getPlayerByNameOrUuid(targetPlayerName);
        if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()))
        {
            msender.message(ChatColor.RED + "Player not found: " + ChatColor.LIGHT_PURPLE + targetPlayerName);
            return;
        }
        
        // Check if actually ignoring
        if (!FactionsChat.instance.getIgnoreManager().isIgnoring(ignoringPlayerUuid, targetPlayer.getUniqueId()))
        {
            if (isAdminCommand)
            {
                msender.message(ChatColor.YELLOW + firstArg + " is not ignoring " + targetPlayer.getName() + ".");
            }
            else
            {
                msender.message(ChatColor.YELLOW + "You are not ignoring " + targetPlayer.getName() + ".");
            }
            return;
        }
        
        // Remove from ignore list
        boolean removed = FactionsChat.instance.getIgnoreManager().removeIgnore(ignoringPlayerUuid, targetPlayer.getUniqueId());
        
        if (removed)
        {
            if (isAdminCommand)
            {
                msender.message(ChatColor.GREEN + "Removed " + targetPlayer.getName() + " from " + firstArg + "'s ignore list.");
            }
            else
            {
                msender.message(ChatColor.GREEN + "You are no longer ignoring " + targetPlayer.getName() + ".");
            }
        }
        else
        {
            msender.message(ChatColor.RED + "Failed to remove player from ignore list.");
        }
    }
    
    /**
     * Tab completion for unignore command
     */
    @Override
    public List<String> getTabCompletions(List<String> args, CommandSender sender)
    {
        // Use a set to avoid duplicate completions
        Set<String> completions = new HashSet<>();
        
        if (args.size() == 1)
        {
            // First argument: player to unignore (or player to manage for admins)
            String input = args.get(0).toLowerCase();

            // Add list of currently ignored players for the sender
            UUID senderUuid = null;
            if (sender instanceof Player)
            {
                senderUuid = ((Player) sender).getUniqueId();
                Set<UUID> ignoredPlayers = FactionsChat.instance.getIgnoreManager().getIgnoredPlayers(senderUuid);
                for (UUID ignoredUuid : ignoredPlayers)
                {
                    OfflinePlayer ignoredPlayer = Bukkit.getOfflinePlayer(ignoredUuid);
                    String ignoredName = ignoredPlayer.getName();
                    if (ignoredName != null && ignoredName.toLowerCase().startsWith(input))
                    {
                        completions.add(ignoredName);
                    }
                }
            }

            // For admins, also add online player names
            if (sender.hasPermission("factions.chat.ignore.admin"))
            {
                // Add all online players to the list
                for (Player onlinePlayer : Bukkit.getOnlinePlayers())
                {
                    if (onlinePlayer.getName().toLowerCase().startsWith(input))
                    {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        }
        else if (args.size() == 2 && sender.hasPermission("factions.chat.ignore.admin"))
        {
            // Second argument for admins: player to unignore
            String input = args.get(1).toLowerCase();
            
            // Add list of currently ignored players for the player being managed
            String firstArg = args.get(0);
            OfflinePlayer managedPlayer = FactionsChat.instance.getIgnoreManager().getPlayerByNameOrUuid(firstArg);
            if (managedPlayer != null && managedPlayer.hasPlayedBefore())
            {
                Set<UUID> ignoredPlayers = FactionsChat.instance.getIgnoreManager().getIgnoredPlayers(managedPlayer.getUniqueId());
                for (UUID ignoredUuid : ignoredPlayers)
                {
                    OfflinePlayer ignoredPlayer = Bukkit.getOfflinePlayer(ignoredUuid);
                    String ignoredName = ignoredPlayer.getName();
                    if (ignoredName != null && ignoredName.toLowerCase().startsWith(input))
                    {
                        completions.add(ignoredName);
                    }
                }
            }
        }
        
        // Convert set to list and return
        return completions.stream().toList();
    }
}