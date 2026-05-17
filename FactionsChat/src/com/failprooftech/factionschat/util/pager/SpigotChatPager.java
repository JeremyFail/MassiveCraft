package com.failprooftech.factionschat.util.pager;

import com.failprooftech.factionschat.util.ChatTxt;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Spigot-specific {@link ChatPager} that uses the BungeeCord chat API
 * ({@link TextComponent} / {@link ClickEvent}) to render clickable navigation
 * arrows for online players. Console output falls back to plain text.
 *
 * <p>Visual style mirrors MassiveCraft's pager:
 * {@code §6_____.[ §2Title §6[<] §61§6/§63§6[>] §6].§6_____}</p>
 */
public final class SpigotChatPager implements ChatPager
{
    private static final int PER_PAGE = 8;

    /** Repeated underscore line - matches MassiveCraft's 52-char line. */
    private static final String TITLE_LINE = "_".repeat(52);

    @Override
    public void sendPage(CommandSender sender, List<String> lines, int page, String title, String pageCommandBase)
    {
        int totalPages = Math.max(1, (lines.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(1, Math.min(page, totalPages));

        if (sender instanceof Player)
            ((Player) sender).spigot().sendMessage(buildHeader(page, totalPages, title, pageCommandBase));
        else
            sender.sendMessage(buildConsoleFallback(page, totalPages, title));

        int start = (page - 1) * PER_PAGE;
        int end   = Math.min(start + PER_PAGE, lines.size());
        for (int i = start; i < end; i++)
            sender.sendMessage(lines.get(i));
    }

    // -------------------------------------------- //
    // PRIVATE HELPERS
    // -------------------------------------------- //

    private static BaseComponent[] buildHeader(int page, int totalPages, String title, String pageCommandBase)
    {
        // Calculate underscore padding (mirrors MassiveCraft's titleizeMson math)
        int centerLen = ".".length() + "[ ".length() + title.length() + 1
                + "[<] ".length() + String.valueOf(page).length()
                + "/".length() + String.valueOf(totalPages).length()
                + " [>]".length() + " ].".length();
        int pivot    = TITLE_LINE.length() / 2;
        int eatLeft  = (centerLen / 2) + 1; // balance = -1
        int eatRight = (centerLen - eatLeft) - 1;

        ComponentBuilder builder = new ComponentBuilder();

        // Left underscores
        if (eatLeft < pivot)
            builder.append(TITLE_LINE.substring(0, pivot - eatLeft)).color(ChatColor.GOLD).bold(false);

        // .[
        builder.append(".").color(ChatColor.GOLD).bold(false);
        builder.append("[ ").color(ChatColor.GOLD).bold(false);

        // Title
        builder.append(title + " ").color(ChatColor.DARK_GREEN).bold(false);

        // [<] - active if not on first page
        if (page > 1)
        {
            TextComponent prev = new TextComponent("[<] ");
            prev.setColor(ChatColor.AQUA);
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    pageCommandBase + " " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Previous page").color(ChatColor.GRAY).create()));
            builder.append(prev);
        }
        else
        {
            builder.append("[<] ").color(ChatColor.GRAY).bold(false);
        }

        // page number - clickable to go to page 1 when not already there
        if (page > 1)
        {
            TextComponent pageComp = new TextComponent(String.valueOf(page));
            pageComp.setColor(ChatColor.GOLD);
            pageComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    pageCommandBase + " 1"));
            pageComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Jump to first page").color(ChatColor.GRAY).create()));
            builder.append(pageComp);
        }
        else
        {
            builder.append(String.valueOf(page)).color(ChatColor.GOLD).bold(false);
        }

        builder.append("/").color(ChatColor.GOLD).bold(false);

        // total pages - clickable to jump to last page when not already there
        if (page < totalPages)
        {
            TextComponent totalComp = new TextComponent(String.valueOf(totalPages));
            totalComp.setColor(ChatColor.GOLD);
            totalComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    pageCommandBase + " " + totalPages));
            totalComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Jump to last page").color(ChatColor.GRAY).create()));
            builder.append(totalComp);
        }
        else
        {
            builder.append(String.valueOf(totalPages)).color(ChatColor.GOLD).bold(false);
        }

        // [>] - active if not on last page
        if (page < totalPages)
        {
            TextComponent next = new TextComponent(" [>]");
            next.setColor(ChatColor.AQUA);
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    pageCommandBase + " " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Next page").color(ChatColor.GRAY).create()));
            builder.append(next);
        }
        else
        {
            builder.append(" [>]").color(ChatColor.GRAY).bold(false);
        }

        // ].
        builder.append(" ].").color(ChatColor.GOLD).bold(false);

        // Right underscores
        if (eatLeft < pivot)
            builder.append(TITLE_LINE.substring(pivot + eatRight)).color(ChatColor.GOLD).bold(false);

        return builder.create();
    }

    private static String buildConsoleFallback(int page, int totalPages, String title)
    {
        return ChatTxt.parse("<k>.[ <h>" + title + " <k>[<] <v>" + page + "<k>/<v>" + totalPages + "<k>[>] ]." );
    }
}
