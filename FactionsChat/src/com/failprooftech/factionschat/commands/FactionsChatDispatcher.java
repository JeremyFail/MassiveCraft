package com.failprooftech.factionschat.commands;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.util.pager.ChatPager;
import com.failprooftech.factionschat.util.pager.PaperChatPager;
import com.failprooftech.factionschat.util.pager.SpigotChatPager;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Routes {@code /f c} invocations to the correct {@link FactionsChatSubcommand}.
 *
 * <p>Called by all registrar implementations. Registrars do not need to know 
 * which subcommand handles which token.</p>
 */
public final class FactionsChatDispatcher
{
    /**
     * All named subcommands in declaration order.
     * The switch/status command is the implicit fallback and is not in this list.
     */
    private static final List<FactionsChatSubcommand> SUBCOMMANDS = Arrays.asList(
        new CmdFactionsChatHelp(),
        new CmdFactionsChatReload(),
        new CmdFactionsChatIgnore(),
        new CmdFactionsChatUnignore(),
        new CmdFactionsChatIgnoreList(),
        new CmdFactionsChatToggle()
    );

    /** Fallback when no subcommand alias matches - shows status or switches mode. */
    private static final CmdFactionsChatSwitch SWITCH_CMD = new CmdFactionsChatSwitch();

    /** Pager chosen at class-load time based on the running server software. */
    private static final ChatPager PAGER;
    static
    {
        boolean paper;
        try   { Class.forName("io.papermc.paper.event.player.AsyncChatEvent"); paper = true; }
        catch (ClassNotFoundException e) { paper = false; }
        PAGER = paper ? new PaperChatPager() : new SpigotChatPager();
    }

    private FactionsChatDispatcher() {}

    // -------------------------------------------------------------------- //
    // Public API used by the registrars
    // -------------------------------------------------------------------- //

    /**
     * Dispatch a {@code /f c} invocation.
     *
     * @param sender the command sender
     * @param args   tokens after the {@code "c"} / {@code "chat"} token; may be empty
     */
    public static void dispatch(CommandSender sender, String[] args)
    {
        if (args.length > 0)
        {
            String lower = args[0].toLowerCase();
            String[] rest = Arrays.copyOfRange(args, 1, args.length);

            for (FactionsChatSubcommand cmd : SUBCOMMANDS)
            {
                if (cmd.getAliases().contains(lower))
                {
                    cmd.execute(sender, rest);
                    return;
                }
            }
        }

        // Not a named subcommand - treat as a mode switch (or status display)
        SWITCH_CMD.execute(sender, args);
    }

    /**
     * Produce tab-completion candidates for {@code /f c <args>}.
     *
     * @param sender the command sender
     * @param args   tokens after the {@code "c"} / {@code "chat"} token; may be empty
     * @return mutable list of candidates; never {@code null}
     */
    public static List<String> tabComplete(CommandSender sender, String[] args)
    {
        List<String> completions = new ArrayList<>();

        if (args.length <= 1)
        {
            // First token: emit all subcommand aliases, then mode names from the switch handler
            for (FactionsChatSubcommand cmd : SUBCOMMANDS)
                for (String alias : cmd.getAliases())
                    completions.add(alias);

            completions.addAll(SWITCH_CMD.tabComplete(sender, args));
            return completions;
        }

        // Deeper tokens: delegate to the matched subcommand
        String sub = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        for (FactionsChatSubcommand cmd : SUBCOMMANDS)
        {
            if (cmd.getAliases().contains(sub))
                return cmd.tabComplete(sender, rest);
        }

        return completions;
    }

    // -------------------------------------------------------------------- //
    // Shared helpers - accessible package-wide by all subcommand classes
    // -------------------------------------------------------------------- //

    /**
     * Parse a {@link ChatMode} from a user-supplied name or alias.
     * Public because {@link com.failprooftech.factionschat.util.ColonChannelChatParser}
     * also calls this.
     *
     * @param input raw user string
     * @return matching mode, or {@code null} if none found
     */
    public static ChatMode parseChatMode(String input)
    {
        if (input == null) return null;
        for (ChatMode mode : ChatMode.values())
        {
            if (mode.name().equalsIgnoreCase(input) || mode.getAlias().equalsIgnoreCase(input))
                return mode;
        }
        return null;
    }

    /**
     * Send a paginated list of lines to a sender with clickable navigation arrows.
     *
     * @param sender          the recipient
     * @param lines           all lines to paginate (pre §-formatted)
     * @param page            requested page (1-based, clamped automatically)
     * @param title           header text shown in the border
     * @param pageCommandBase the slash-command prefix used by navigation arrows;
     *                        the page number is appended automatically
     *                        (e.g. {@code "/f c help"})
     */
    static void sendPage(CommandSender sender, List<String> lines, int page, String title, String pageCommandBase)
    {
        PAGER.sendPage(sender, lines, page, title, pageCommandBase);
    }
}
