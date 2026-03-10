package com.massivecraft.massivebooks.integration.placeholderapi;

import com.massivecraft.massivebooks.BookUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes placeholders in text and books using PlaceholderAPI.
 * Only called when the PlaceholderAPI integration is active.
 * <p>
 * Placeholders are not parsed when a book is signed; they are stored raw.
 * They are parsed when a book is loaded/given/updated for the current viewer (the player
 * reading or holding the book). So e.g. %player_name% shows the viewer's name;
 * when the book is given to another player, that player sees their own values when it updates.
 * Relational placeholders use (viewer, author) when the author is online.
 */
public final class PlaceholderAPIProcessor
{
	private PlaceholderAPIProcessor() {}

	/**
	 * Process text for the current viewer. All placeholders use the viewer (current reader) as context.
	 * Relational placeholders use (viewer, author) when author is online.
	 *
	 * @param viewer       The player viewing/reading the book (placeholder context).
	 * @param authorPlayer The author if online, or null (used only for relational placeholders).
	 * @param text         The raw text that may contain placeholders.
	 * @return The processed text.
	 */
	public static String processForViewer(Player viewer, Player authorPlayer, String text)
	{
		if (text == null || text.isEmpty()) return text;

		String result = PlaceholderAPI.setPlaceholders(viewer, text);
		if (authorPlayer != null)
		{
			result = PlaceholderAPI.setRelationalPlaceholders(viewer, authorPlayer, result);
		}
		return BookUtil.translateColorCodes(result);
	}

	/**
	 * Take a raw book (with unparsed placeholders) and return a clone with placeholders
	 * parsed for the given viewer. All placeholders use the viewer (current reader) as context.
	 * Author is resolved only for relational placeholders (viewer -> author when author is online).
	 *
	 * @param raw    The raw book item (e.g. from MBook).
	 * @param viewer The player who will see the book (placeholder context).
	 * @return A new ItemStack with title, author, and pages processed for this viewer.
	 */
	public static ItemStack processBookForViewer(ItemStack raw, Player viewer)
	{
		if (raw == null || viewer == null) return raw == null ? null : raw.clone();
		ItemStack out = raw.clone();
		BookMeta meta = BookUtil.getBookMeta(out);
		if (meta == null) return out;

		String authorName = meta.hasAuthor() ? meta.getAuthor() : null;
		Player authorPlayer = (authorName != null && !authorName.isEmpty())
			? Bukkit.getPlayerExact(authorName)
			: null;

		// Process the title, author, and pages for the current viewer
		if (meta.hasTitle())
		{
			meta.setTitle(processForViewer(viewer, authorPlayer, meta.getTitle()));
		}
		if (meta.hasAuthor())
		{
			meta.setAuthor(processForViewer(viewer, authorPlayer, meta.getAuthor()));
		}
		if (meta.hasPages())
		{
			List<String> pages = meta.getPages();
			List<String> processed = new ArrayList<>(pages.size());
			for (String page : pages)
			{
				processed.add(processForViewer(viewer, authorPlayer, page));
			}
			meta.setPages(processed);
		}

		out.setItemMeta(meta);
		BookUtil.applyColorCodesToBook(out);
		return out;
	}
}
