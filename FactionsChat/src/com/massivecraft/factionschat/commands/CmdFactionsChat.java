package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.TypeChatMode;
import com.massivecraft.factionschat.listeners.FactionChatListener;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

import org.bukkit.ChatColor;

/**
 * Represents the <code>/f c {channel}</code> command.
 * Supports switching chat channels, as well as sending a single message to a channel.
 */
public class CmdFactionsChat extends FactionsCommand
{
    public CmdFactionsChat()
    {
        addParameter(TypeChatMode.getInstance());
        addParameter(TypeString.get(), true);
        setDesc("Switches chat modes or sends a quick message to a channel");
        addAliases("chat", "c");
    }

    @Override
    public void perform()
    {
        // Check for reload subcommand
        String firstArg = arg();
        if (firstArg != null && firstArg.equalsIgnoreCase("reload"))
        {
            if (!msender.getPlayer().hasPermission("factionschat.reload"))
            {
                msender.message(ChatColor.RED + "Invalid chat mode: " + arg());
                return;
            }
            FactionsChat.instance.reloadConfig();
            msender.message(ChatColor.YELLOW + "FactionsChat configuration reloaded.");
            return;
        }

        // Normal chat mode switching or quick message sending
        ChatMode chatMode;
        try 
        {
            chatMode = readArg();
        } 
        catch (MassiveException e) 
        {
            msender.message(ChatColor.YELLOW + "Invalid chat mode: " + arg());
            return;
        }

        if (msender.getFaction().equals(FactionColl.get().getNone()) && 
            (chatMode == ChatMode.FACTION || chatMode == ChatMode.ALLY || chatMode == ChatMode.TRUCE || chatMode == ChatMode.ENEMY)) 
        {
            msender.message(ChatColor.YELLOW + "You are not in a faction");
            return;
        }

        if (chatMode == null) 
        {
            msender.message(ChatColor.YELLOW + "Invalid argument: " + arg());
            return;
        }

        if (!msender.getPlayer().hasPermission("factionschat." + chatMode.name().toLowerCase())) 
        {
            msender.message(ChatColor.YELLOW + "You don't have permission for the following chat mode: " + chatMode.name().toLowerCase());
            return;
        }
        
        // If the player is sending a quick message (not switching to the channel)
        try
        {
            String msg = readArg();
            FactionChatListener.qmPlayers.put(msender.getPlayer().getUniqueId(), chatMode);
            msender.getPlayer().chat(msg);
        }
        catch (MassiveException e)
        {
            FactionsChat.instance.getPlayerChatModes().put(msender.getUuid(), chatMode);
            msender.message(ChatColor.YELLOW + "Chatmode set: " + chatMode.name());
            FactionsChat.instance.saveChatModesFile();
        }
    }
}
