package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.TypeChatMode;
import com.massivecraft.massivecore.MassiveException;

import org.bukkit.ChatColor;

public class CmdFactionsChat extends FactionsCommand
{
    public CmdFactionsChat()
    {
        addParameter(TypeChatMode.getInstance());
        setDesc("Switches chat mode");
        addAliases("chat", "c");
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

        if (msender.getFaction().equals(FactionColl.get().getNone()) && 
            (chatMode == ChatMode.FACTION || chatMode == ChatMode.ALLY || chatMode == ChatMode.TRUCE || chatMode == ChatMode.ENEMY)) {
            msender.message(ChatColor.YELLOW + "You are not in a faction");
            return;
        }

        if (chatMode == null) {
            msender.message(ChatColor.YELLOW + "Invalid argument: " + arg());
            return;
        }

        if (!msender.getPlayer().hasPermission("factionschat." + chatMode.name().toLowerCase())) {
            msender.message(ChatColor.YELLOW + "You don't have permission for the following chat mode: " + chatMode.name().toLowerCase());
            return;
        }

        FactionsChat.instance.getPlayerChatModes().put(msender.getUuid(), chatMode);
        msender.message(ChatColor.YELLOW + "Chatmode set: " + chatMode.name());
        FactionsChat.instance.saveChatModesFile();
    }
}
