package com.failprooftech.factionschat.util.pager;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Strategy for sending a paginated list of lines to a {@link CommandSender}.
 *
 * <p>Implementations differ in whether they emit clickable navigation arrows
 * (Paper Adventure API vs Spigot BungeeCord chat API).</p>
 */
public interface ChatPager
{
    /**
     * Send one page of {@code lines} to {@code sender}, with a header that includes
     * clickable {@literal ◀} / {@literal ▶} navigation arrows when running on a
     * client that supports it.
     *
     * @param sender          the recipient
     * @param lines           all content lines (pre §-formatted); split across pages
     * @param page            requested page number (1-based; clamped automatically)
     * @param title           text shown in the page header
     * @param pageCommandBase the slash-command prefix to run when a nav arrow is clicked;
     *                        the page number is appended automatically (e.g. {@code "/f c help"})
     */
    void sendPage(CommandSender sender, List<String> lines, int page, String title, String pageCommandBase);
}
