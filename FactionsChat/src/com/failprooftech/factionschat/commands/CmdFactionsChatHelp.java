package com.failprooftech.factionschat.commands;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.util.ChatTxt;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        FactionsChatDispatcher.sendPage(sender, buildHelpContent(sender), page, "Help for command \"chat\"",
                FactionsChat.instance.getChatCommandPrefix() + " help");
    }

    private List<String> buildHelpContent(CommandSender sender)
    {
        List<String> lines = new ArrayList<>();
        boolean isConsole = !(sender instanceof Player);
        Player player = isConsole ? null : (Player) sender;

        // ---- Modes + Quick-chat (players only) ----
        if (!isConsole)
        {
            final String cmdPx = FactionsChat.instance.getChatCommandPrefix();
            lines.add(ChatTxt.parse("<k>Chat Modes:"));
            lines.add(ChatTxt.parse("<n>Use <k>" + cmdPx + " <mode><n> to switch modes, or type <k>"
                    + Settings.QuickChat.prefix + "<mode><n> / <k>"
                    + Settings.QuickChat.prefix + "<letter><n> in chat for a one-off message."));
            lines.add(ChatTxt.parse("<n>Use <k>" + Settings.QuickChat.prefix + "<mode><n> alone to switch modes as well."));
            List<ChatMode> availableModes = ChatMode.getAvailableChatModes(player);
            for (ChatMode mode : availableModes)
            {
                lines.add(ChatTxt.parse("<i>  - <v>" + mode.name().toLowerCase()
                        + "<i> (or <v>" + mode.getAlias() + "<i>)<white> - <i>" + mode.getDescription()));
            }
            lines.add(""); // spacer before commands
        }

        // ---- Subcommands (permission-filtered) ----
        boolean hasIgnore      = isConsole || player.hasPermission("factions.chat.ignore");
        boolean hasIgnoreAdmin = isConsole || player.hasPermission("factions.chat.ignore.admin");
        boolean hasToggle      = isConsole || player.hasPermission("factions.chat.toggle");
        boolean hasToggleAdmin = isConsole || player.hasPermission("factions.chat.toggle.admin");
        boolean hasReload      = isConsole || player.hasPermission("factions.chat.reload");

        if (hasIgnore || hasIgnoreAdmin || hasToggle || hasToggleAdmin || hasReload)
        {
            lines.add(ChatTxt.parse("<k>Subcommands:"));
            lines.add(ChatTxt.parse("<n>Use <k>" + FactionsChat.instance.getChatCommandPrefix() + " <subcommand><n> to run other chat commands."));

            if (hasIgnoreAdmin)
            {
                lines.add(ChatTxt.parse("<i>  - <v>ignore [playerToUpdate] <player><i> - <i>Add players to the ignore list for "
                        + (!isConsole ? "yourself or " : "") + "another player"));
                lines.add(ChatTxt.parse("<i>  - <v>unignore [playerToUpdate] <player><i> - <i>Remove players from the ignore list for "
                        + (!isConsole ? "yourself or " : "") + "another player"));
                lines.add(ChatTxt.parse("<i>  - <v>ignorelist [player]<i> - <i>View the ignore list for "
                        + (!isConsole ? "yourself or " : "") + "another player"));
            }
            else if (hasIgnore)
            {
                lines.add(ChatTxt.parse("<i>  - <v>ignore <player><i> - <i>Add a player to "
                        + (!isConsole ? "your" : "") + " ignore list"));
                lines.add(ChatTxt.parse("<i>  - <v>unignore <player><i> - <i>Remove a player from "
                        + (!isConsole ? "your" : "") + " ignore list"));
                lines.add(ChatTxt.parse("<i>  - <v>ignorelist<i> - <i>View "
                        + (!isConsole ? "your" : "") + " ignore list"));
            }

            if (hasToggleAdmin)
            {
                lines.add(ChatTxt.parse("<i>  - <v>toggle [player] <chatMode><i> - <i>Toggle (disable/enable) chat modes for "
                        + (!isConsole ? "yourself or " : "") + "another player"));
            }
            else if (hasToggle)
            {
                lines.add(ChatTxt.parse("<i>  - <v>toggle <chatMode><i> - <i>Toggle (disable/enable) specific chat modes"));
            }

            if (hasReload)
            {
                lines.add(ChatTxt.parse("<i>  - <v>reload<i> - <i>Reload FactionsChat configuration"));
            }
        }

        return lines;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        return Collections.emptyList(); // no useful completions for a page number
    }
}
