package com.failprooftech.factionschat.commands;

import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.util.ChatTxt;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code /f c reload} - reloads the FactionsChat configuration from disk.
 * Requires the {@code factions.chat.reload} permission.
 */
public final class CmdFactionsChatReload implements FactionsChatSubcommand
{
    private static final Set<String> ALIASES = Collections.unmodifiableSet(
        new LinkedHashSet<>(Arrays.asList("reload")));

    @Override
    public Set<String> getAliases() { return ALIASES; }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if (sender instanceof Player && !sender.hasPermission("factions.chat.reload"))
        {
            sender.sendMessage(ChatTxt.parse("<b>You don't have permission to reload FactionsChat configuration."));
            return;
        }

        FactionsChat.instance.reloadConfig();
        sender.sendMessage(ChatTxt.parse("<g>FactionsChat configuration reloaded successfully."));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        return Collections.emptyList();
    }
}
