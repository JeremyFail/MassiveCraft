package com.failprooftech.factionschat.util.pager;

import com.failprooftech.factionschat.util.ChatTxt;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Spigot-specific {@link ChatPager} that uses the BungeeCord chat API
 * ({@link TextComponent} / {@link ClickEvent}) to render clickable navigation
 * arrows for online players. Console output falls back to plain text.
 */
public final class SpigotChatPager implements ChatPager
{
    private static final int PER_PAGE = 8;

    @Override
    public void sendPage(CommandSender sender, List<String> lines, int page, String title, String pageCommandBase)
    {
        int totalPages = Math.max(1, (lines.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(1, Math.min(page, totalPages));

        if (sender instanceof Player)
            ((Player) sender).spigot().sendMessage(buildHeader(page, totalPages, title, pageCommandBase));
        else
            sender.sendMessage(ChatTxt.parse("<k>--- " + title + " (" + page + "/" + totalPages + ") ---"));

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
        TextComponent root = new TextComponent();
        root.addExtra(new TextComponent(ChatColor.GRAY + "---"));

        if (page > 1)
        {
            TextComponent prev = new TextComponent(ChatColor.YELLOW + " \u25C4 ");
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                pageCommandBase + " " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{new TextComponent("Previous page")}));
            root.addExtra(prev);
        }
        else
        {
            root.addExtra(new TextComponent(ChatColor.DARK_GRAY + " \u25C4 "));
        }

        TextComponent titleComp = new TextComponent(
            ChatColor.GOLD + "" + ChatColor.BOLD + title + " "
                + ChatColor.YELLOW + "(" + page + "/" + totalPages + ")");
        root.addExtra(titleComp);

        if (page < totalPages)
        {
            TextComponent next = new TextComponent(ChatColor.YELLOW + " \u25BA ");
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                pageCommandBase + " " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{new TextComponent("Next page")}));
            root.addExtra(next);
        }
        else
        {
            root.addExtra(new TextComponent(ChatColor.DARK_GRAY + " \u25BA "));
        }

        root.addExtra(new TextComponent(ChatColor.GRAY + "---"));
        return new BaseComponent[]{root};
    }
}
