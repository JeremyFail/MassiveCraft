package com.failprooftech.factionschat.commands;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.util.ChatTxt;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Default {@code /f c} handler - shows the player's current chat mode or
 * switches to the specified mode.
 *
 * <p>This command has no alias; it is the fallback invoked by
 * {@link FactionsChatDispatcher} when no named subcommand alias matches the
 * first argument (or when no argument is provided at all).</p>
 */
public final class CmdFactionsChatSwitch implements FactionsChatSubcommand
{
    @Override
    public Set<String> getAliases()
    {
        return Collections.emptySet(); // fallback - no keyword triggers this
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        String modeArg = args.length > 0 ? args[0] : null;

        if (!(sender instanceof Player))
        {
            if (modeArg == null)
                sender.sendMessage(ChatTxt.parse("<b>Only players have chat modes."));
            else
                sender.sendMessage(ChatTxt.parse("<b>You cannot switch chat modes as the console, only players can do that."));
            return;
        }

        Player player = (Player) sender;

        if (modeArg == null)
        {
            ChatMode current = ChatMode.getChatModeForPlayer(player);
            sender.sendMessage(ChatTxt.parse("<n>Current chat mode: <k>" + current.name().toLowerCase()));
            sender.sendMessage(ChatTxt.parse("<n>Use <k>/f c <mode><n> to switch modes, or <k>"
                    + Settings.QuickChat.prefix + "<mode><n> / <k>" + Settings.QuickChat.prefix
                    + "<letter><n> in chat for a one-off message or toggle."));
            sender.sendMessage(ChatTxt.parse("<n>Use <k>/f c help<n> to see all available commands and modes."));
            return;
        }

        ChatMode chatMode = FactionsChatDispatcher.parseChatMode(modeArg);
        if (chatMode == null)
        {
            sender.sendMessage(ChatTxt.parse("<b>Invalid chat mode or command: <v>" + modeArg));
            sender.sendMessage(ChatTxt.parse("<n>Use <k>/f c help<n> to see available commands and modes."));
            return;
        }

        boolean isFactionMode = chatMode == ChatMode.FACTION || chatMode == ChatMode.ALLY
                             || chatMode == ChatMode.TRUCE   || chatMode == ChatMode.ENEMY;

        if (isFactionMode
                && FactionsChat.instance.getFactionsBridge() != null
                && !FactionsChat.instance.getFactionsBridge().isInFaction(player))
        {
            sender.sendMessage(ChatTxt.parse("<b>Cannot switch to that chat mode as you are not in a faction."));
            return;
        }

        if (!player.hasPermission("factions.chat." + chatMode.name().toLowerCase()))
        {
            sender.sendMessage(ChatTxt.parse("<b>Invalid chat mode or command: <v>" + modeArg));
            return;
        }

        FactionsChat.instance.getPlayerChatModes().put(player.getUniqueId(), chatMode);
        sender.sendMessage(ChatTxt.parse("<i>Chat mode set to: <k>" + chatMode.name().toLowerCase()));
    }

    /** Returns mode name completions - used by the dispatcher for first-token completion. */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player)) return completions;

        String input = args.length > 0 ? args[0].toLowerCase() : "";
        for (ChatMode mode : ChatMode.getAvailableChatModes((Player) sender))
        {
            String name  = mode.name().toLowerCase();
            String alias = mode.getAlias();
            if (name.startsWith(input))                           completions.add(name);
            if (!name.equals(alias) && alias.startsWith(input))  completions.add(alias);
        }
        return completions;
    }
}
