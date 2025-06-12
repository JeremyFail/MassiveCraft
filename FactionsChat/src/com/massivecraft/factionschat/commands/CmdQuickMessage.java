package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.TypeChatMode;
import com.massivecraft.factionschat.listeners.FactionChatListener;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

import org.bukkit.ChatColor;

public class CmdQuickMessage extends FactionsCommand
{
    public CmdQuickMessage()
    {
        addParameter(TypeChatMode.getInstance());
        addParameter(TypeString.get(), true);
        addAliases("quickmessage", "qm");
    }

    @Override
    public void perform()
    {
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
                
        String msg;
        try
        {
            msg = readArg();
        }
        catch (MassiveException e)
        {
            msender.message(ChatColor.YELLOW + "Invalid argument: " + arg());
            return;
        }

        if (chatMode == null)
        {
            msender.message("Invalid chat mode: " + chatMode);
            return;
        }

        if (!msender.getPlayer().hasPermission("factionschat." + chatMode.name().toLowerCase()))
        {
            msender.message(ChatColor.YELLOW + "You don't have permission to use " + chatMode.name());
            return;
        }

        FactionChatListener.qmPlayers.put(msender.getPlayer().getUniqueId(), chatMode);
        msender.getPlayer().chat(msg);
    }
}
