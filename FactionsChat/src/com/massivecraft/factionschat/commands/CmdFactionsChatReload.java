package com.massivecraft.factionschat.commands;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factionschat.FactionsChat;

import org.bukkit.ChatColor;

/**
 * Represents the /f c reload command.
 * Allows players with permission to reload the FactionsChat configuration.
 */
public class CmdFactionsChatReload extends FactionsCommand
{
    public CmdFactionsChatReload()
    {
        setDesc("Reload the FactionsChat configuration");
        addAliases("reload");
    }
    
    @Override
    public void perform()
    {
        // Check permission
        if (!msender.getPlayer().hasPermission("factions.chat.reload"))
        {
            msender.message(ChatColor.RED + "You don't have permission to reload FactionsChat configuration.");
            return;
        }
        
        // Reload the configuration
        FactionsChat.instance.reloadConfig();
        msender.message(ChatColor.GREEN + "FactionsChat configuration reloaded successfully.");
    }
}