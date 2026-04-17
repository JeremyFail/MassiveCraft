package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.TypeChatMode;
import com.massivecraft.massivecore.util.Txt;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the <code>/f c toggle {chatMode}</code> or <code>/f c toggle {player} {chatMode}</code> command.
 * Allows players to toggle (disable/enable) specific chat modes.
 * Admins can toggle chat modes for other players.
 */
public class CmdFactionsChatToggle extends FactionsCommand
{
    public CmdFactionsChatToggle()
    {
        setAliases("toggle");
        setDesc("Toggle (disable/enable) specific chat modes");
    }

    @Override
    public void perform()
    {
        // Check basic permission
        if (!msender.getPlayer().hasPermission("factions.chat.toggle"))
        {
            msender.message(Txt.parse("<b>You don't have permission to use chat toggle commands."));
            return;
        }
        
        // Parse arguments
        String firstArg = arg();
        String secondArg = arg();
        
        // At least one argument is required
        if (firstArg == null)
        {
            msender.message(Txt.parse("<b>You must specify a chat mode to toggle."));
            return;
        }
        
        UUID targetPlayerUuid;
        String targetPlayerName;
        String chatModeArg;
        boolean isAdminCommand = false;
        
        // Determine if this is an admin command (2 arguments) or regular command (1 argument)
        if (secondArg != null)
        {
            // Admin command: /f c toggle {player} {chatMode}
            if (!msender.getPlayer().hasPermission("factions.chat.toggle.admin"))
            {
                msender.message(Txt.parse("<b>You don't have permission to manage disabled chat modes of other players."));
                return;
            }
            
            OfflinePlayer targetPlayer = FactionsChat.instance.getDisabledChatManager().getPlayerByNameOrUuid(firstArg);
            if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()))
            {
                msender.message(Txt.parse("<b>Player not found: <v>" + firstArg));
                return;
            }
            
            isAdminCommand = true;
            targetPlayerUuid = targetPlayer.getUniqueId();
            targetPlayerName = targetPlayer.getName();
            chatModeArg = secondArg;
        }
        else
        {
            // Regular command: /f c toggle {chatMode}
            targetPlayerUuid = msender.getPlayer().getUniqueId();
            targetPlayerName = msender.getPlayer().getName();
            chatModeArg = firstArg;
        }
        
        // Parse chat mode from the appropriate argument
        ChatMode chatMode = TypeChatMode.getInstance().read(chatModeArg, msender.getPlayer());
        if (chatMode == null)
        {
            msender.message(Txt.parse("<b>Invalid chat mode: <v>" + chatModeArg));
            return;
        }
        
        // Perform the toggle
        boolean isNowDisabled = FactionsChat.instance.getDisabledChatManager().toggleChatMode(targetPlayerUuid, chatMode);
        
        // Send appropriate messages
        String chatModeName = chatMode.name().toLowerCase();
        
        if (isAdminCommand)
        {
            if (isNowDisabled)
            {
                msender.message(Txt.parse("<b>Disabled <k>" + chatModeName + "<i> chat for <v>" + targetPlayerName + "<i>."));
                
                // Notify target player if online
                Player targetPlayer = Bukkit.getPlayer(targetPlayerUuid);
                if (targetPlayer != null && targetPlayer.isOnline())
                {
                    targetPlayer.sendMessage(Txt.parse("<yellow>An admin has<b> disabled <k>" + chatModeName + "<i> chat for you."));
                }
            }
            else
            {
                msender.message(Txt.parse("<g>Enabled <k>" + chatModeName + "<i> chat for <v>" + targetPlayerName + "<i>."));
                
                // Notify target player if online
                Player targetPlayer = Bukkit.getPlayer(targetPlayerUuid);
                if (targetPlayer != null && targetPlayer.isOnline())
                {
                    targetPlayer.sendMessage(Txt.parse("<i>An admin has<g> enabled <k>" + chatModeName + "<i> chat for you."));
                }
            }
        }
        else
        {
            if (isNowDisabled)
            {
                msender.message(Txt.parse("<b>Disabled <k>" + chatModeName + "<i> chat. You will no longer see messages from this channel."));
            }
            else
            {
                msender.message(Txt.parse("<g>Enabled <k>" + chatModeName + "<i> chat. You will now see messages from this channel."));
            }
        }
    }
    
    /**
     * Tab completion for toggle command
     */
    @Override
    public List<String> getTabCompletions(List<String> args, CommandSender sender)
    {
        List<String> completions = new ArrayList<>();
        
        if (args.size() == 1)
        {
            String input = args.get(0).toLowerCase();
            
            // Always suggest chat modes for first argument
            for (ChatMode chatMode : ChatMode.values())
            {
                String modeName = chatMode.name().toLowerCase();
                if (modeName.startsWith(input))
                {
                    completions.add(modeName);
                }
            }
            
            // If player has admin permission, also suggest player names
            if (sender.hasPermission("factions.chat.toggle.admin"))
            {
                for (Player player : Bukkit.getOnlinePlayers())
                {
                    String playerName = player.getName();
                    if (playerName.toLowerCase().startsWith(input))
                    {
                        completions.add(playerName);
                    }
                }
            }
        }
        else if (args.size() == 2 && sender.hasPermission("factions.chat.toggle.admin"))
        {
            // Second argument for admins: chat mode
            String input = args.get(1).toLowerCase();
            
            for (ChatMode chatMode : ChatMode.values())
            {
                String modeName = chatMode.name().toLowerCase();
                if (modeName.startsWith(input))
                {
                    completions.add(modeName);
                }
            }
        }
        
        return completions;
    }
}