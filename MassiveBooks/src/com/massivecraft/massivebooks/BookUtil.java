package com.massivecraft.massivebooks;

import com.massivecraft.massivebooks.entity.BookType;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivebooks.entity.MBookColl;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
	// BOOK METADATA (PDC): bookId, bookType
	// -------------------------------------------- //

	private static NamespacedKey keyBookId() { return new NamespacedKey(MassiveBooks.get(), "book_id"); }
	private static NamespacedKey keyBookType() { return new NamespacedKey(MassiveBooks.get(), "book_type"); }

	public static UUID getBookId(ItemStack item)
	{
		if (item == null || !item.hasItemMeta()) return null;
		ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.getPersistentDataContainer().has(keyBookId(), PersistentDataType.STRING)) return null;
		String s = meta.getPersistentDataContainer().get(keyBookId(), PersistentDataType.STRING);
		if (s == null || s.isEmpty()) return null;
		try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
	}

	public static void setBookId(ItemStack item, UUID bookId)
	{
		if (item == null) return;
		ItemMeta meta = InventoryUtil.createMeta(item);
		if (bookId == null) meta.getPersistentDataContainer().remove(keyBookId());
		else meta.getPersistentDataContainer().set(keyBookId(), PersistentDataType.STRING, bookId.toString());
		item.setItemMeta(meta);
	}

	public static BookType getBookType(ItemStack item)
	{
		if (item == null || !item.hasItemMeta()) return null;
		ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.getPersistentDataContainer().has(keyBookType(), PersistentDataType.STRING)) return null;
		String s = meta.getPersistentDataContainer().get(keyBookType(), PersistentDataType.STRING);
		if (s == null) return null;
		try { return BookType.valueOf(s); } catch (IllegalArgumentException e) { return null; }
	}

	public static void setBookType(ItemStack item, BookType type)
	{
		if (item == null) return;
		ItemMeta meta = InventoryUtil.createMeta(item);
		if (type == null) meta.getPersistentDataContainer().remove(keyBookType());
		else meta.getPersistentDataContainer().set(keyBookType(), PersistentDataType.STRING, type.name());
		item.setItemMeta(meta);
	}

	/** Remove bookId and bookType from item PDC (e.g. so they are not stored in MBook template). */
	public static void removeBookMetadata(ItemStack item)
	{
		if (item == null || !item.hasItemMeta()) return;
		ItemMeta meta = item.getItemMeta();
		meta.getPersistentDataContainer().remove(keyBookId());
		meta.getPersistentDataContainer().remove(keyBookType());
		item.setItemMeta(meta);
	}

	/** Remove the book-type lore line (e.g. "Server Book") from the item. Call while item still has bookType set so we know which line to remove. Does not touch copyrighted. */
	public static void removeBookTypeLoreLine(ItemStack item)
	{
		if (item == null) return;

		BookType type = getBookType(item);
		if (type == null) return;

		List<String> lore = InventoryUtil.getLore(item);
		if (lore == null || lore.isEmpty()) return;
		
		String friendlyName = type.getFriendlyName();
		lore = new ArrayList<>(lore);
		lore.removeIf(line -> line != null && ChatColor.stripColor(line).trim().equals(friendlyName));
		if (lore.isEmpty())
		{
			InventoryUtil.setLore(item, (Collection<String>) null);
		}
		else
		{
			InventoryUtil.setLore(item, lore);
		}
	}

	/** Clear server-book identity from item when the saved book was deleted (bookId, bookType, type lore). Keeps copyrighted status. */
	public static void clearServerbookMetadataFromItem(ItemStack item)
	{
		if (item == null) return;
		removeBookTypeLoreLine(item);
		setBookId(item, null);
		setBookType(item, null);
	}

	// -------------------------------------------- //
	// UNLOCK TITLE/AUTHOR (PDC) – writable books don't support title/author in BookMeta
	// -------------------------------------------- //

	private static NamespacedKey keyUnlockTitle() { return new NamespacedKey(MassiveBooks.get(), "unlock_title"); }
	private static NamespacedKey keyUnlockAuthor() { return new NamespacedKey(MassiveBooks.get(), "unlock_author"); }

	public static String getUnlockTitle(ItemStack item)
	{
		if (item == null || !item.hasItemMeta()) return null;

		ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.getPersistentDataContainer().has(keyUnlockTitle(), PersistentDataType.STRING)) return null;

		return meta.getPersistentDataContainer().get(keyUnlockTitle(), PersistentDataType.STRING);
	}

	public static String getUnlockAuthor(ItemStack item)
	{
		if (item == null || !item.hasItemMeta()) return null;

		ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.getPersistentDataContainer().has(keyUnlockAuthor(), PersistentDataType.STRING)) return null;

		return meta.getPersistentDataContainer().get(keyUnlockAuthor(), PersistentDataType.STRING);
	}

	private static void setUnlockTitleAuthor(ItemStack item, String title, String author)
	{
		if (item == null) return;
		
		// NOTE: We store the unlock title and author in the item PDC since writable books don't support BookMeta title/author
		ItemMeta meta = InventoryUtil.createMeta(item);
		if (title != null) 
		{
			meta.getPersistentDataContainer().set(keyUnlockTitle(), PersistentDataType.STRING, title);
		}
		else 
		{
			meta.getPersistentDataContainer().remove(keyUnlockTitle());
		}

		if (author != null)
		{
			meta.getPersistentDataContainer().set(keyUnlockAuthor(), PersistentDataType.STRING, author);
		}
		else 
		{
			meta.getPersistentDataContainer().remove(keyUnlockAuthor());
		}

		item.setItemMeta(meta);
	}

	private static void clearUnlockTitleAuthor(ItemStack item)
	{
		if (item == null || !item.hasItemMeta()) return;

		ItemMeta meta = item.getItemMeta();
		meta.getPersistentDataContainer().remove(keyUnlockTitle());
		meta.getPersistentDataContainer().remove(keyUnlockAuthor());
		item.setItemMeta(meta);
	}

	public static boolean isServerBook(ItemStack item)
	{
		return getBookType(item) == BookType.SERVER_BOOK;
	}

	/** If showBookTypeAsLore is enabled, append the type's friendly name as a lore line (not saved in book data). Removes any existing type line first. */
	public static void applyBookTypeLore(ItemStack item)
	{
		if (item == null) return;
		BookType type = getBookType(item);
		if (type == null) return;
		if (!MConf.get().showBookTypeAsLore) return;

		List<String> lore = InventoryUtil.getLore(item);
		String friendlyName = type.getFriendlyName();
		// Remove any existing line that is the type's friendly name (strip color for comparison)
		if (lore != null)
		{
			lore = new ArrayList<>(lore);
			lore.removeIf(line -> line != null && ChatColor.stripColor(line).trim().equals(friendlyName));
		}
		else
		{
			lore = new ArrayList<>();
		}
		lore.add(ChatColor.GOLD + friendlyName);
		InventoryUtil.setLore(item, lore);
	}

	/** Apply MBook identity and display to a book item (bookId, bookType, copyrighted, display name, type lore). Use when giving/loading or after setting content from an MBook. */
	public static void applyMBookMetadata(ItemStack item, MBook mbook)
	{
		if (item == null || mbook == null) return;
		setBookId(item, mbook.getBookId());
		setBookType(item, mbook.getBookType());
		if (mbook.isCopyrighted())
		{
			addFlag(item, Const.COPYRIGHTED);
		}
		else
		{
			removeFlag(item, Const.COPYRIGHTED);
		}
		updateDisplayName(item);
		applyBookTypeLore(item);
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

	// Saved (server books): by ID first, then legacy by title

	public static boolean updateServerbook(ItemStack item, Player viewer)
	{
		if (!MConf.get().autoupdatingServerbooks) return false;
		if (item == null) return false;

		// New format: look up by bookId on the item
		UUID bookId = getBookId(item);
		if (bookId != null)
		{
			MBook mbook = MBookColl.get().getById(bookId);
			if (mbook != null)
			{
				applyServerbookContent(item, mbook, viewer);
				return true;
			}
			// Saved book was deleted: clear stale server-book metadata (keep copyrighted)
			clearServerbookMetadataFromItem(item);
			return false;
		}

		// Legacy: look up by title (easy to remove later)
		return updateServerbookLegacy(item, viewer);
	}

	/** Legacy serverbook update by title. Remove when dropping support for old-format books. */
	public static boolean updateServerbookLegacy(ItemStack item, Player viewer)
	{
		String title = getTitle(item);
		if (title == null) return false;
		MBook mbook = MBook.get(title);
		if (mbook == null)
		{
			// Had server-book metadata but no saved book (e.g. deleted); clear it (keep copyrighted)
			if (getBookId(item) != null || getBookType(item) != null)
			{
				clearServerbookMetadataFromItem(item);
			}
			return false;
		}
		applyServerbookContent(item, mbook, viewer);
		return true;
	}

	/** Apply saved serverbook content to item (content + bookId/bookType + display name + type lore). */
	public static void applyServerbookContent(ItemStack item, MBook mbook, Player viewer)
	{
		ItemStack blueprint = mbook.getItem();
		if (blueprint == null) return;

		// Process placeholders for the viewer if PAPI is enabled
		if (viewer != null && MassiveBooks.get().isPapiEnabled())
		{
			blueprint = MassiveBooks.get().processBookPlaceholdersForViewer(blueprint.clone(), viewer);
		}

		item.setType(blueprint.getType());
		item.setItemMeta(blueprint.getItemMeta());
		applyMBookMetadata(item, mbook);
	}

	/** If item is a legacy serverbook (title matches saved, no bookId), upgrade it in place to new format and apply latest content. Used when loading from item frame. */
	public static void upgradeLegacyServerbook(ItemStack item, Player viewer)
	{
		if (item == null || !hasBookMeta(item)) return;
		if (getBookId(item) != null) return; // already new format
		updateServerbookLegacy(item, viewer);
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
		if (meta != null && meta.hasTitle()) return meta.getTitle();
		// Unlocked (writable) books don't store title in BookMeta; use PDC if set
		if (item != null && item.getType() == Material.WRITABLE_BOOK) return getUnlockTitle(item);
		return null;
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
		if (meta != null && meta.hasAuthor()) return meta.getAuthor();
		// Unlocked (writable) books don't store author in BookMeta; use PDC if set
		if (item != null && item.getType() == Material.WRITABLE_BOOK) return getUnlockAuthor(item);
		return null;
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
		// Capture title/author before setType; writable books don't support them in BookMeta, so we store in PDC.
		String savedTitle = (meta != null && meta.hasTitle()) ? stripColorCodesToAlternate(meta.getTitle()) : null;
		String savedAuthor = (meta != null && meta.hasAuthor()) ? stripColorCodesToAlternate(meta.getAuthor()) : null;
		List<String> pages = getPages(item);
		item.setType(Material.WRITABLE_BOOK);
		BookMeta writableMeta = getBookMeta(item);
		if (writableMeta != null)
		{
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
		setUnlockTitleAuthor(item, savedTitle, savedAuthor);
		updateDisplayName(item);
	}
	
	public static void lock(ItemStack item)
	{
		if (item == null) return;
		if (item.getType() == Material.WRITTEN_BOOK) return;

		BookMeta meta = getBookMeta(item);
		// Keep previous title and author when locking (e.g. after unlock → edit → lock). Writable books store them in PDC.
		String savedTitle = (meta != null && meta.hasTitle()) ? meta.getTitle() : getUnlockTitle(item);
		String savedAuthor = (meta != null && meta.hasAuthor()) ? meta.getAuthor() : getUnlockAuthor(item);
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
		clearUnlockTitleAuthor(item);
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

	/** Remove the COPYRIGHTED lore line only (no display name update). Used when building the MBook template so copyrighted is stored as entity field, not in lore. */
	public static void removeCopyrightedFromLoreOnly(ItemStack item)
	{
		if (item == null || !containsFlag(item, Const.COPYRIGHTED)) return;
		List<String> lore = InventoryUtil.getLore(item);
		if (lore == null) return;
		lore = new ArrayList<>(lore);
		lore.remove(Const.COPYRIGHTED);
		if (lore.isEmpty())
		{
			InventoryUtil.setLore(item, (Collection<String>) null);
		}
		else
		{
			InventoryUtil.setLore(item, lore);
		}
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
