package com.failprooftech.factionschat.commands;

import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.util.ChatTxt;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code /f c help [page]} - displays a paginated help listing of all
 * {@code /f c} subcommands and available chat modes.
 */
public final class CmdFactionsChatHelp implements FactionsChatSubcommand
{
    private static final Set<String> ALIASES = Collections.unmodifiableSet(
        new LinkedHashSet<>(Arrays.asList("help", "h", "?")));

    @Override
    public Set<String> getAliases() { return ALIASES; }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        int page = 1;
        if (args.length > 0)
        {
            try { page = Integer.parseInt(args[0]); }
            catch (NumberFormatException e)
            {
                sender.sendMessage(ChatTxt.parse("<b>\"<v>" + args[0] + "<b>\" is not a valid page number."));
                return;
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add(ChatTxt.parse("<k>/f c                <n>Show current chat mode and usage"));
        lines.add(ChatTxt.parse("<k>/f c <mode>         <n>Switch to a chat mode"));
        lines.add(ChatTxt.parse("<k>/f c help           <n>Show this help page"));
        lines.add(ChatTxt.parse("<k>/f c toggle <mode>  <n>Toggle a chat mode on/off"));
        lines.add(ChatTxt.parse("<k>/f c ignore <player><n>Ignore a player's chat messages"));
        lines.add(ChatTxt.parse("<k>/f c unignore <player><n>Unignore a player"));
        lines.add(ChatTxt.parse("<k>/f c ignorelist     <n>View your ignore list"));
        lines.add(ChatTxt.parse("<k>/f c reload         <n>Reload configuration (admin)"));
        lines.add(ChatTxt.parse("<i>Modes: <k>global<i>, <k>local<i>, <k>faction<i>, <k>ally<i>, <k>truce<i>, <k>enemy<i>, <k>staff"));
        lines.add(ChatTxt.parse("<i>Quick-chat: prefix a message with <k>"
                + Settings.QuickChat.prefix + "<mode><i> to send without switching."));

        FactionsChatDispatcher.sendPage(sender, lines, page, "Help for /f c", "/f c help");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        return Collections.emptyList(); // no useful completions for a page number
    }
}
