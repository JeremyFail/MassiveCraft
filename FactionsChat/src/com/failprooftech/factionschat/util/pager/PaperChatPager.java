package com.failprooftech.factionschat.util.pager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Paper-specific {@link ChatPager} that builds the header using the Adventure
 * component API so navigation arrows carry {@link ClickEvent#runCommand} actions
 * and tooltip hover text.
 *
 * <p>Content lines are deserialized from §-color-coded strings via
 * {@link LegacyComponentSerializer} so they render correctly in all Paper contexts.</p>
 */
public final class PaperChatPager implements ChatPager
{
    private static final int PER_PAGE = 8;

    @Override
    public void sendPage(CommandSender sender, List<String> lines, int page, String title, String pageCommandBase)
    {
        int totalPages = Math.max(1, (lines.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(1, Math.min(page, totalPages));

        sender.sendMessage(buildHeader(page, totalPages, title, pageCommandBase));

        int start = (page - 1) * PER_PAGE;
        int end   = Math.min(start + PER_PAGE, lines.size());
        for (int i = start; i < end; i++)
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(lines.get(i)));
    }

    // -------------------------------------------- //
    // PRIVATE HELPERS
    // -------------------------------------------- //

    private static Component buildHeader(int page, int totalPages, String title, String pageCommandBase)
    {
        Component separator = Component.text("---", NamedTextColor.GRAY);

        Component prev = page > 1
            ? Component.text(" \u25C4 ", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand(pageCommandBase + " " + (page - 1)))
                .hoverEvent(HoverEvent.showText(Component.text("Previous page", NamedTextColor.GRAY)))
            : Component.text(" \u25C4 ", NamedTextColor.DARK_GRAY);

        Component next = page < totalPages
            ? Component.text(" \u25BA ", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand(pageCommandBase + " " + (page + 1)))
                .hoverEvent(HoverEvent.showText(Component.text("Next page", NamedTextColor.GRAY)))
            : Component.text(" \u25BA ", NamedTextColor.DARK_GRAY);

        Component titleComp = Component.text(title + " ", NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD)
            .append(Component.text("(" + page + "/" + totalPages + ")", NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, false));

        return separator.append(prev).append(titleComp).append(next).append(separator);
    }
}
