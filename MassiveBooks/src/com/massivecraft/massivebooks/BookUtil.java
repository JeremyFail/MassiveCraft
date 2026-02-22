package com.massivecraft.massivebooks;

import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookUtil
{
	// -------------------------------------------- //
	// BOOK META
	// -------------------------------------------- //
	
	public static boolean hasBookMeta(ItemStack item)
	{
		if (item == null) return false;
		Material type = item.getType();
		if (type == Material.WRITTEN_BOOK) return true;
		if (type == Material.WRITABLE_BOOK) return true;
		return false;
	}
	
	public static BookMeta getBookMeta(ItemStack item)
	{
		if (item == null) return null;
		ItemMeta meta = item.getItemMeta();
		if (!(meta instanceof BookMeta)) return null;
		return (BookMeta) meta;
	}
	
	public static boolean isBookMetaEmpty(ItemStack item)
	{
		if (item == null) return true;
		BookMeta meta = getBookMeta(item);
		return isBookMetaEmpty(meta);
	}
	
	public static boolean isBookMetaEmpty(BookMeta meta)
	{
		if (meta == null) return true;
		if (meta.hasTitle()) return false;
		if (meta.hasAuthor()) return false;
		if (meta.hasPages()) return false;
		return true;
	}
	
	// -------------------------------------------- //
	// UPDATE BOOKS
	// -------------------------------------------- //
	
	// Many books
	
	public static void updateBooks(HumanEntity player)
	{
		if (player == null) return;
		updateBooks(player.getInventory());
	}
	
	public static void updateBooks(Inventory inventory)
	{
		if (inventory == null) return;
		Player viewer = null;
		if (inventory.getHolder() instanceof Player)
		{
			viewer = (Player) inventory.getHolder();
		}
		else
		{
			for (HumanEntity e : inventory.getViewers())
			{
				if (e instanceof Player) 
				{ 
					viewer = (Player) e; break;
				}
			}
		}
		boolean update = false;
		ItemStack[] contents = inventory.getContents();
		for (int i = 0; i < contents.length; i++)
		{
			ItemStack item = contents[i];
			if (updateBook(item, viewer))
			{
				inventory.setItem(i, item);
				update = true;
			}
		}
		if (update) sendInventoryContentToViewersSoon(inventory);
	}
	
	// One Book
	
	public static void updateBook(ItemFrame itemFrame)
	{
		ItemStack item = itemFrame.getItem();
		if (updateBook(item, null)) itemFrame.setItem(item);
	}
	
	public static void updateBook(Item item, Player viewer)
	{
		ItemStack stack = item.getItemStack();
		if (updateBook(stack, viewer)) item.setItemStack(stack);
	}
	
	public static boolean updateBook(ItemStack item)
	{
		return updateBook(item, null);
	}
	
	public static boolean updateBook(ItemStack item, Player viewer)
	{
		if (item == null) return false;
		if (!hasBookMeta(item)) return false;
		if (updateServerbook(item, viewer)) return true;
		return updateDisplayName(item);
	}
	
	// Saved
	
	public static boolean updateServerbook(ItemStack item, Player viewer)
	{
		if (!MConf.get().autoupdatingServerbooks) return false;
		if (item == null) return false;
		String title = getTitle(item);
		if (title == null) return false;
		MBook mbook = MBook.get(title);
		if (mbook == null) return false;
		ItemStack blueprint = mbook.getItem();
		if (blueprint == null) return false;
		if (viewer != null && MassiveBooks.get().isPapiEnabled())
			blueprint = MassiveBooks.get().processBookPlaceholdersForViewer(blueprint, viewer);
		if (item.isSimilar(blueprint)) return false;
		item.setType(blueprint.getType());
		item.setItemMeta(blueprint.getItemMeta());
		// Apply display name so serverbooks show "by X" consistently and don't flip-flop when
		// blueprint was saved without a display name (next update would run updateDisplayName and
		// then replace again, toggling the text on/off).
		if (MConf.get().autoupdatingDisplayNames) setDisplayName(item, Lang.descDisplayName(item));
		return true;
	}
	
	// DisplayName
	
	public static boolean updateDisplayName(ItemStack item)
	{
		if (!MConf.get().autoupdatingDisplayNames) return false;
		if (item == null) return false;

		String targetDisplayname = Lang.descDisplayName(item);
		return setDisplayName(item, targetDisplayname);
	}
	
	public static boolean setDisplayName(ItemStack item, String targetDisplayName)
	{
		if (item == null || targetDisplayName == null) return false;

		ItemMeta meta = InventoryUtil.createMeta(item);
		String currentDisplayName = meta.getDisplayName();
		if (MUtil.equals(currentDisplayName, targetDisplayName)) return false;

		meta.setDisplayName(targetDisplayName);
		return item.setItemMeta(meta);
	}
	
	// The awesomest trick to force-update-clients :O
	
	public static void sendInventoryContentToViewersSoon(Inventory inventory)
	{
		final Set<Player> players = new HashSet<>();
		for (HumanEntity viewer : inventory.getViewers())
		{
			if (viewer instanceof Player)
			{
				players.add((Player) viewer);
			}
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(MassiveBooks.get(), () -> {
			for (Player player : players)
			{
				InventoryUtil.update(player);
			}
		});
	}
	
	// -------------------------------------------- //
	// TITLE
	// -------------------------------------------- //
	
	public static String getTitle(ItemStack item)
	{
		BookMeta meta = getBookMeta(item);
		if (meta == null) return null;
		if (!meta.hasTitle()) return null;
		return meta.getTitle();
	}
	
	public static void setTitle(ItemStack item, String title)
	{
		BookMeta meta = getBookMeta(item);
		if (meta == null) return;

		meta.setTitle(title);
		if (!item.setItemMeta(meta)) return;
		
		updateBook(item);
	}
	
	public static boolean isTitleEquals(ItemStack item, String title)
	{
		String actualTitle = getTitle(item);
		if (actualTitle == null) return title == null;
		return actualTitle.equals(title);
	}
	
	// -------------------------------------------- //
	// AUTHOR
	// -------------------------------------------- //
	
	public static String getAuthor(ItemStack item)
	{
		BookMeta meta = getBookMeta(item);
		if (meta == null) return null;
		if (!meta.hasAuthor()) return null;
		return meta.getAuthor();
	}
	
	public static void setAuthor(ItemStack item, String author)
	{
		BookMeta meta = getBookMeta(item);
		if (meta == null) return;
		meta.setAuthor(author);
		if (!item.setItemMeta(meta)) return;
		updateDisplayName(item);
	}
	
	public static boolean isAuthorEqualsId(ItemStack item, String author)
	{
		String actualAuthor = getAuthor(item);
		if (actualAuthor == null) return author == null;
		return actualAuthor.equalsIgnoreCase(author);
	}
	
	public static boolean isAuthorEquals(ItemStack item, CommandSender author)
	{
		return isAuthorEqualsId(item, IdUtil.getName(author));
	}
	
	// -------------------------------------------- //
	// PAGES
	// -------------------------------------------- //
	
	public static List<String> getPages(ItemStack item)
	{
		BookMeta meta = getBookMeta(item);
		if (meta == null) return null;
		if (!meta.hasPages()) return null;
		return meta.getPages();
	}
	
	public static boolean setPages(ItemStack item, List<String> pages)
	{
		BookMeta meta = getBookMeta(item);
		if (meta == null) return false;

		meta.setPages(pages);
		if (!item.setItemMeta(meta)) return false;

		updateDisplayName(item);
		return true;
	}
	
	public static boolean isPagesEquals(ItemStack item, List<String> pages)
	{
		List<String> actualPages = getPages(item);
		if (actualPages == null) return pages == null;
		return actualPages.equals(pages);
	}

	// -------------------------------------------- //
	// COLOR CODES (PAPI-agnostic; always applied when processing book text)
	// -------------------------------------------- //

	/**
	 * Translate alternate color codes ({@code &}) to ChatColor in the given text.
	 * Safe to call with null or when PAPI is not installed.
	 *
	 * @param text Text that may contain {@code &} color codes.
	 * @return The text with codes translated, or null if input was null.
	 */
	public static String translateColorCodes(String text)
	{
		if (text == null || text.isEmpty()) return text;
		if (!text.contains("&")) return text;
		return ChatColor.translateAlternateColorCodes('&', text);
	}

	/**
	 * Apply {@code &} color code translation to a book's title, author, and pages.
	 * Modifies the item in place. Use when preparing book content for display (with or without PAPI).
	 *
	 * @param item A book item; must have BookMeta.
	 * @return true if the item was modified.
	 */
	public static boolean applyColorCodesToBook(ItemStack item)
	{
		if (item == null) return false;
		BookMeta meta = getBookMeta(item);
		if (meta == null) return false;
		boolean changed = false;
		if (meta.hasTitle())
		{
			String t = translateColorCodes(meta.getTitle());
			if (!meta.getTitle().equals(t)) { meta.setTitle(t); changed = true; }
		}
		if (meta.hasAuthor())
		{
			String a = translateColorCodes(meta.getAuthor());
			if (!meta.getAuthor().equals(a)) { meta.setAuthor(a); changed = true; }
		}
		if (meta.hasPages())
		{
			List<String> pages = meta.getPages();
			List<String> out = new java.util.ArrayList<>(pages.size());
			for (String page : pages)
				out.add(translateColorCodes(page));
			if (!pages.equals(out)) { meta.setPages(out); changed = true; }
		}
		if (changed) item.setItemMeta(meta);
		return changed;
	}

	/**
	 * Convert ChatColor codes ({@code §}) back to alternate form ({@code &}) so text can be edited.
	 * Use when unlocking a book so the editor shows {@code &a}, {@code &b}, etc. instead of raw formatting.
	 *
	 * @param text Text that may contain {@code §} color codes (e.g. from a signed book).
	 * @return The text with {@code §} replaced by {@code &} for color codes, or null if input was null.
	 */
	public static String stripColorCodesToAlternate(String text)
	{
		if (text == null || text.isEmpty()) return text;
		if (!text.contains("\u00A7")) return text;
		return text.replaceAll("\u00A7([0-9a-fk-or])", "&$1");
	}

	/**
	 * Replace formatted color codes with {@code &} form on a book's title, author, and pages.
	 * Use when unlocking so the writable book content is editable with familiar {@code &} codes.
	 *
	 * @param item A book item; must have BookMeta.
	 * @return true if the item was modified.
	 */
	public static boolean stripColorCodesToAlternateForBook(ItemStack item)
	{
		if (item == null) return false;
		BookMeta meta = getBookMeta(item);
		if (meta == null) return false;
		boolean changed = false;
		if (meta.hasTitle())
		{
			String t = stripColorCodesToAlternate(meta.getTitle());
			if (!meta.getTitle().equals(t)) { meta.setTitle(t); changed = true; }
		}
		if (meta.hasAuthor())
		{
			String a = stripColorCodesToAlternate(meta.getAuthor());
			if (!meta.getAuthor().equals(a)) { meta.setAuthor(a); changed = true; }
		}
		if (meta.hasPages())
		{
			List<String> pages = meta.getPages();
			List<String> out = new java.util.ArrayList<>(pages.size());
			for (String page : pages)
				out.add(stripColorCodesToAlternate(page));
			if (!pages.equals(out)) { meta.setPages(out); changed = true; }
		}
		if (changed) item.setItemMeta(meta);
		return changed;
	}
	
	// -------------------------------------------- //
	// UNLOCK & LOCK
	// -------------------------------------------- //
	
	public static void unlock(ItemStack item)
	{
		if (item == null) return;
		if (item.getType() == Material.WRITABLE_BOOK) return;

		BookMeta meta = getBookMeta(item);
		// Capture title/author before setType; changing type can clear the item's meta.
		String savedTitle = (meta != null && meta.hasTitle()) ? meta.getTitle() : null;
		String savedAuthor = (meta != null && meta.hasAuthor()) ? meta.getAuthor() : null;
		List<String> pages = getPages(item);
		item.setType(Material.WRITABLE_BOOK);
		BookMeta writableMeta = getBookMeta(item);
		if (writableMeta != null)
		{
			if (savedTitle != null) writableMeta.setTitle(stripColorCodesToAlternate(savedTitle));
			if (savedAuthor != null) writableMeta.setAuthor(stripColorCodesToAlternate(savedAuthor));
			if (pages != null)
			{
				List<String> stripped = new java.util.ArrayList<>(pages.size());
				for (String p : pages) stripped.add(stripColorCodesToAlternate(p));
				writableMeta.setPages(stripped);
			}
			item.setItemMeta(writableMeta);
		}
		else if (pages != null)
		{
			List<String> stripped = new java.util.ArrayList<>(pages.size());
			for (String p : pages) stripped.add(stripColorCodesToAlternate(p));
			setPages(item, stripped);
		}
		updateDisplayName(item);
	}
	
	public static void lock(ItemStack item)
	{
		if (item == null) return;
		if (item.getType() == Material.WRITTEN_BOOK) return;

		BookMeta meta = getBookMeta(item);
		// Keep previous title and author when locking (e.g. after unlock → edit → lock).
		String savedTitle = (meta != null && meta.hasTitle()) ? meta.getTitle() : null;
		String savedAuthor = (meta != null && meta.hasAuthor()) ? meta.getAuthor() : null;
		List<String> pages = getPages(item);
		item.setType(Material.WRITTEN_BOOK);
		BookMeta writtenMeta = getBookMeta(item);
		if (writtenMeta != null)
		{
			if (savedTitle != null) writtenMeta.setTitle(savedTitle);
			if (savedAuthor != null) writtenMeta.setAuthor(savedAuthor);
			if (pages != null) writtenMeta.setPages(pages);
			item.setItemMeta(writtenMeta);
		}
		else if (pages != null)
		{
			setPages(item, pages);
		}
		updateDisplayName(item);
	}
	
	public static boolean isLocked(ItemStack item)
	{
		if (item == null) return false;
		return item.getType() == Material.WRITTEN_BOOK;
	}
	
	public static boolean isUnlocked(ItemStack item)
	{
		if (item == null) return false;
		return item.getType() == Material.WRITABLE_BOOK;
	}
	
	// -------------------------------------------- //
	// CLEAR
	// -------------------------------------------- //
	
	public static void clear(ItemStack item)
	{
		item.setType(Material.WRITABLE_BOOK);
		item.setItemMeta(null);
	}
	
	public static boolean isCleared(ItemStack item)
	{
		return item != null && item.getType() == Material.WRITABLE_BOOK && !item.hasItemMeta();
	}
	
	// -------------------------------------------- //
	// LORE-FLAGS
	// -------------------------------------------- //
	
	public static boolean containsFlag(ItemStack item, String flag)
	{
		if (flag == null) return false;
		if (!item.hasItemMeta()) return false;

		ItemMeta meta = InventoryUtil.createMeta(item);
		List<String> lore = meta.getLore();
		return lore != null && lore.contains(flag);
	}
	
	public static void addFlag(ItemStack item, String flag)
	{
		if (flag == null) return;
		if (containsFlag(item, flag)) return;

		List<String> lore = InventoryUtil.getLore(item);
		if (lore != null)
		{
			lore.add(flag);
			InventoryUtil.setLore(item, lore);
		}
		else
		{
			InventoryUtil.setLore(item, flag);
		}
		updateDisplayName(item);
	}
	
	public static void removeFlag(ItemStack item, String flag)
	{
		if (flag == null) return;
		if (!containsFlag(item, flag)) return;

		List<String> lore = InventoryUtil.getLore(item);
		if (lore == null) return;
		
		lore.remove(flag);
		if (lore.size() == 0)
		{
			InventoryUtil.setLore(item, (Collection<String>) null);
		}
		else
		{
			InventoryUtil.setLore(item, lore);
		}
		updateDisplayName(item);
	}
	
	// -------------------------------------------- //
	// COPY PERMS
	// -------------------------------------------- //
	
	public static boolean hasCopyPerm(ItemStack item, CommandSender sender, boolean verbose)
	{
		if (BookUtil.isAuthorEquals(item, sender)) return true;
		if (!Perm.COPY_OTHER.has(sender, true)) return false;
		if (!BookUtil.containsFlag(item, Const.COPYRIGHTED)) return true;
		if (!Perm.COPY_COPYRIGHTED.has(sender, true)) return false;
		return true;
	}
	
}
