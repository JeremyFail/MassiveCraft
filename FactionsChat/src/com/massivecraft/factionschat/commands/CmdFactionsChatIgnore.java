package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.util.Txt;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the /f c ignore {player} command.
 * Allows players to add other players to their ignore list.
 * Admins can manage other players' ignore lists with /f c ignore {playerToManage} {playerToIgnore}
 */
public class CmdFactionsChatIgnore extends FactionsCommand
{
    
    public CmdFactionsChatIgnore()
    {
        addParameter(TypeString.get(), "player");
        addParameter(TypeString.get(), false, "target player", "targetPlayer");
        setDesc("Add a player to your ignore list");
        addAliases("ignore");
    }
    
    @Override
    public void perform()
    {
        // Check basic permission
        if (msender.isPlayer() && !msender.getPlayer().hasPermission("factions.chat.ignore"))
        {
            msender.message(Txt.parse("<b>You don't have permission to use chat ignore commands."));
            return;
        }
        
        // Parse arguments
        String firstArg = arg();
        String secondArg = arg();
        
        // At least one argument is required
        if (firstArg == null)
        {
            msender.message(Txt.parse("<b>You must specify a player to ignore."));
            return;
        }
        
        UUID ignoringPlayerUuid;
        String targetPlayerName;
        boolean isAdminCommand = false;
        
        // Determine if this is an admin command (2 arguments) or regular command (1 argument)
        if (secondArg != null)
        {
            // Admin command: /f c ignore {playerToManage} {playerToIgnore}
            if (!msender.getPlayer().hasPermission("factions.chat.ignore.admin"))
            {
                msender.message(Txt.parse("<b>You don't have permission to manage the ignore list of other players."));
                return;
            }
            
            OfflinePlayer managedPlayer = FactionsChat.instance.getIgnoreManager().getPlayerByNameOrUuid(firstArg);
            if (managedPlayer == null || (!managedPlayer.hasPlayedBefore() && !managedPlayer.isOnline()))
            {
                msender.message(Txt.parse("<b>Player not found: <v>" + firstArg));
                return;
            }
            
            isAdminCommand = true;
            ignoringPlayerUuid = managedPlayer.getUniqueId();
            targetPlayerName = secondArg;
        }
        // Regular command: /f c ignore {player}
        else
        {
            // Console cannot ignore players, only manage other players' ignore lists
            if (msender.isConsole())
            {
                msender.message(Txt.parse("<b>You cannot ignore players from the console, only manage other players' ignore lists."));
                return;
            }

            ignoringPlayerUuid = msender.getPlayer().getUniqueId();
            targetPlayerName = firstArg;
        }
        
        // Get the target player to ignore
        OfflinePlayer targetPlayer = FactionsChat.instance.getIgnoreManager().getPlayerByNameOrUuid(targetPlayerName);
        if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()))
        {
            msender.message(Txt.parse("<b>Player not found: <v>" + targetPlayerName));
            return;
        }
        
        // Can't ignore yourself
        if (ignoringPlayerUuid.equals(targetPlayer.getUniqueId()))
        {
            if (isAdminCommand)
            {
                msender.message(Txt.parse("<b>A player cannot ignore themselves."));
            }
            else
            {
                msender.message(Txt.parse("<b>You cannot ignore yourself."));
            }
            return;
        }

        // Can't ignore players with bypass permission
        if (targetPlayer.isOnline() && targetPlayer.getPlayer().hasPermission("factions.chat.ignore.bypass"))
        {
            if (isAdminCommand)
            {
                msender.message(Txt.parse("<b>You cannot add ") + targetPlayer.getName() + Txt.parse("<b> to the ignore list because they have ignore bypass permission."));
            }
            else
            {
                msender.message(Txt.parse("<b>You cannot ignore ") + targetPlayer.getName() + Txt.parse("<b>."));
            }
            return;
        }
        
        // Check if already ignoring
        if (FactionsChat.instance.getIgnoreManager().isIgnoring(ignoringPlayerUuid, targetPlayer.getUniqueId()))
        {
            if (isAdminCommand)
            {
                msender.message(Txt.parse("<b>") + firstArg + Txt.parse("<b> is already ignoring ") + targetPlayer.getName() + Txt.parse("<b>."));
            }
            else
            {
                msender.message(Txt.parse("<b>You are already ignoring ") + targetPlayer.getName() + Txt.parse("<b>."));
            }
            return;
        }
        
        // Add to ignore list
        FactionsChat.instance.getIgnoreManager().addIgnore(ignoringPlayerUuid, targetPlayer.getUniqueId());
        
        if (isAdminCommand)
        {
            msender.message(Txt.parse("<g>Added ") + targetPlayer.getName() + Txt.parse("<g> to ") + firstArg + Txt.parse("<g>'s ignore list."));
        }
        else
        {
            msender.message(Txt.parse("<g>You are now ignoring ") + targetPlayer.getName() + Txt.parse("<g>."));
        }
    }
    
    /**
     * Tab completion for ignore command
     */
    @Override
    public List<String> getTabCompletions(List<String> args, CommandSender sender)
    {
        List<String> completions = new ArrayList<>();
        
        if (args.size() == 1)
        {
            // First argument: player to ignore (or player to manage for admins)
            String input = args.get(0).toLowerCase();
            
            // Get online players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            {
                if (onlinePlayer.getName().toLowerCase().startsWith(input))
                {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        else if (args.size() == 2 && sender.hasPermission("factions.chat.ignore.admin"))
        {
            // Second argument for admins: target player to ignore
            String input = args.get(1).toLowerCase();
            
            // Get online players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            {
                if (onlinePlayer.getName().toLowerCase().startsWith(input))
                {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        
        return completions;
    }
}