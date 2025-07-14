package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.TypeChatMode;
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
        addParameter(TypeChatMode.getInstance(), "chat mode");
        addParameter(TypeString.get(), false, "message", "message", true);
        setDesc("Switches chat modes or sends a quick message to a channel");
        addAliases("chat", "c");
    }

    @Override
    public void perform()
    {
        // Get the first argument, which is the chat mode or reload command
        String firstArg = arg();
        if (firstArg == null)
        {
            msender.message(ChatColor.RED + "Invalid chat mode: " + ChatColor.DARK_AQUA + firstArg);
        }

        // Check for reload subcommand
        if (firstArg != null && firstArg.equalsIgnoreCase("reload"))
        {
            if (!msender.getPlayer().hasPermission("factions.chat.reload"))
            {
                msender.message(ChatColor.RED + "Invalid chat mode: " + ChatColor.DARK_AQUA + firstArg);
                return;
            }
            FactionsChat.instance.reloadConfig();
            msender.message(ChatColor.YELLOW + "FactionsChat configuration reloaded.");
            return;
        }

        // Normal chat mode switching or quick message sending
        ChatMode chatMode = null;
        try
        {
            chatMode = TypeChatMode.getInstance().read(firstArg.toUpperCase());
        }
        catch (MassiveException e)
        {
            msender.message(ChatColor.RED + "Invalid chat mode: " + ChatColor.DARK_AQUA + firstArg);
            return;
        }

        // If the chat mode was not retrieved
        if (chatMode == null)
        {
            msender.message(ChatColor.RED + "Invalid chat mode: " + ChatColor.DARK_AQUA + firstArg);
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
            msender.message(ChatColor.RED + "Invalid chat mode: " + ChatColor.DARK_AQUA + firstArg);
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
            msender.message(ChatColor.YELLOW + "Chatmode set: " + ChatColor.DARK_AQUA + chatMode.name());
            FactionsChat.instance.saveChatModesFile();
        }
    }
}
