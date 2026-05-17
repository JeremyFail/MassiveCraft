package com.failprooftech.factionschat.util.pager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Paper-specific {@link ChatPager} that builds the header using the Adventure
 * component API so navigation arrows carry {@link ClickEvent#runCommand} actions
 * and tooltip hover text.
 *
 * <p>Visual style mirrors MassiveCraft's pager:
 * {@code §6_____.[ §2Title §6[<] §61§6/§63§6[>] §6].§6_____}</p>
 *
 * <p>Content lines are deserialized from §-color-coded strings via
 * {@link LegacyComponentSerializer} so they render correctly in all Paper contexts.</p>
 */
public final class PaperChatPager implements ChatPager
{
    private static final int PER_PAGE = 8;

    /** Repeated underscore line used on either side of the title - matches MassiveCraft's 52-char line. */
    private static final String TITLE_LINE = "_".repeat(52);

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
        // Center section: .[ Title [<] page/total [>] ].
        Component dot      = Component.text(".").color(NamedTextColor.GOLD);
        Component bracketL = Component.text("[ ").color(NamedTextColor.GOLD);
        Component bracketR = Component.text(" ].").color(NamedTextColor.GOLD);

        Component titleComp = Component.text(title + " ").color(NamedTextColor.DARK_GREEN);

        Component prev = buildNavArrow("[<] ", page > 1,
                pageCommandBase + " " + (page - 1), "Previous page");

        Component pageNum = Component.text(String.valueOf(page)).color(NamedTextColor.GOLD);
        Component slash   = Component.text("/").color(NamedTextColor.GOLD);
        Component total;
        if (page < totalPages)
        {
            total = Component.text(String.valueOf(totalPages)).color(NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand(pageCommandBase + " " + totalPages))
                    .hoverEvent(HoverEvent.showText(Component.text("Jump to last page").color(NamedTextColor.GRAY)));
        }
        else
        {
            total = Component.text(String.valueOf(totalPages)).color(NamedTextColor.GOLD);
        }
        Component next = buildNavArrow(" [>]", page < totalPages,
                pageCommandBase + " " + (page + 1), "Next page");

        Component center = dot
                .append(bracketL)
                .append(titleComp)
                .append(prev)
                .append(pageNum)
                .append(slash)
                .append(total)
                .append(next)
                .append(bracketR);

        // Calculate how many underscores fit on each side
        int centerLen = ".[ ".length() + title.length() + 1
                + "[<] ".length() + String.valueOf(page).length()
                + "/".length() + String.valueOf(totalPages).length()
                + " [>]".length() + " ].".length();
        int pivot    = TITLE_LINE.length() / 2;
        int eatLeft  = (centerLen / 2) - (-1); // titleizeBalance = -1
        int eatRight = (centerLen - eatLeft) + (-1);

        if (eatLeft < pivot)
        {
            return Component.text(TITLE_LINE.substring(0, pivot - eatLeft)).color(NamedTextColor.GOLD)
                    .append(center)
                    .append(Component.text(TITLE_LINE.substring(pivot + eatRight)).color(NamedTextColor.GOLD));
        }
        return center;
    }

    /**
     * Builds a {@code [<] } or {@code [>]} navigation component.
     * Active arrows are aqua with a click command; inactive arrows are gray.
     */
    private static Component buildNavArrow(String label, boolean active, String command, String tooltip)
    {
        if (active)
        {
            return Component.text(label).color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand(command))
                    .hoverEvent(HoverEvent.showText(Component.text(tooltip).color(NamedTextColor.GRAY)));
        }
        return Component.text(label).color(NamedTextColor.GRAY);
    }
}
