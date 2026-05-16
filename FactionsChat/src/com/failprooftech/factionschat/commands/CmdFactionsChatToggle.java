package com.failprooftech.factionschat.commands;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.util.ChatTxt;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /f c toggle <mode>} - enables or disables receiving messages from a
 * specific chat channel for the sender.
 *
 * <p>Admin variant: {@code /f c toggle <player> <mode>} requires
 * {@code factions.chat.toggle.admin} and modifies another player's settings.</p>
 */
public final class CmdFactionsChatToggle implements FactionsChatSubcommand
{
    private static final Set<String> ALIASES = Collections.unmodifiableSet(
        new LinkedHashSet<>(Arrays.asList("toggle")));

    @Override
    public Set<String> getAliases() { return ALIASES; }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if (sender instanceof Player && !sender.hasPermission("factions.chat.toggle"))
        {
            sender.sendMessage(ChatTxt.parse("<b>You don't have permission to use chat toggle commands."));
            return;
        }

        if (args.length == 0)
        {
            sender.sendMessage(ChatTxt.parse("<b>You must specify a chat mode to toggle."));
            return;
        }

        String firstArg  = args[0];
        String secondArg = args.length > 1 ? args[1] : null;

        UUID    targetUuid;
        String  targetName;
        String  chatModeArg;
        boolean admin = false;

        if (secondArg != null)
        {
            // Admin form: /f c toggle <player> <chatMode>
            if (sender instanceof Player && !sender.hasPermission("factions.chat.toggle.admin"))
            {
                sender.sendMessage(ChatTxt.parse("<b>You don't have permission to manage disabled chat modes of other players."));
                return;
            }
            OfflinePlayer target = FactionsChat.instance.getDisabledChatManager().getPlayerByNameOrUuid(firstArg);
            if (target == null || (!target.hasPlayedBefore() && !target.isOnline()))
            {
                sender.sendMessage(ChatTxt.parse("<b>Player not found: <v>" + firstArg));
                return;
            }
            admin       = true;
            targetUuid  = target.getUniqueId();
            targetName  = target.getName();
            chatModeArg = secondArg;
        }
        else
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatTxt.parse("<b>You cannot toggle chat modes for the console, only for other players."));
                return;
            }
            targetUuid  = ((Player) sender).getUniqueId();
            targetName  = ((Player) sender).getName();
            chatModeArg = firstArg;
        }

        ChatMode chatMode = FactionsChatDispatcher.parseChatMode(chatModeArg);
        if (chatMode == null)
        {
            sender.sendMessage(ChatTxt.parse("<b>Invalid chat mode: <v>" + chatModeArg));
            return;
        }

        boolean disabled = FactionsChat.instance.getDisabledChatManager().toggleChatMode(targetUuid, chatMode);
        String modeName  = chatMode.name().toLowerCase();

        if (admin)
        {
            if (disabled)
            {
                sender.sendMessage(ChatTxt.parse("<b>Disabled <k>" + modeName + "<i> chat for <v>" + targetName + "<i>."));
                Player tp = Bukkit.getPlayer(targetUuid);
                if (tp != null) tp.sendMessage(ChatTxt.parse("<yellow>An admin has<b> disabled <k>" + modeName + "<i> chat for you."));
            }
            else
            {
                sender.sendMessage(ChatTxt.parse("<g>Enabled <k>" + modeName + "<i> chat for <v>" + targetName + "<i>."));
                Player tp = Bukkit.getPlayer(targetUuid);
                if (tp != null) tp.sendMessage(ChatTxt.parse("<i>An admin has<g> enabled <k>" + modeName + "<i> chat for you."));
            }
        }
        else
        {
            sender.sendMessage(disabled
                ? ChatTxt.parse("<b>Disabled <k>" + modeName + "<i> chat. You will no longer see messages from this channel.")
                : ChatTxt.parse("<g>Enabled <k>" + modeName + "<i> chat. You will now see messages from this channel."));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        List<String> completions = new ArrayList<>();
        String input = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

        if (args.length == 1)
        {
            for (ChatMode mode : ChatMode.values())
                if (mode.name().toLowerCase().startsWith(input)) completions.add(mode.name().toLowerCase());

            if (sender.hasPermission("factions.chat.toggle.admin"))
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(input)) completions.add(p.getName());
        }
        else if (args.length == 2 && sender.hasPermission("factions.chat.toggle.admin"))
        {
            for (ChatMode mode : ChatMode.values())
                if (mode.name().toLowerCase().startsWith(input)) completions.add(mode.name().toLowerCase());
        }

        return completions;
    }
}
