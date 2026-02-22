package com.massivecraft.massivebooks.entity;

import com.massivecraft.massivebooks.BookUtil;
import com.massivecraft.massivecore.store.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MBook extends Entity<MBook>
{
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //

	public static MBook get(Object oid)
	{
		return MBookColl.get().get(oid);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public MBook load(MBook that)
	{
		this.item = that.item;
		this.bookId = that.bookId;
		this.bookType = that.bookType;
		return this;
	}

	// -------------------------------------------- //
	// VERSION
	// -------------------------------------------- //

	public int version = 2;

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private ItemStack item = null;
	public ItemStack getItem()
	{
		return fixItem(this.item);
	}
	public void setItem(ItemStack item)
	{
		item = fixItem(item);
		this.item = item;
		this.changed();
	}

	/** Unique id for this saved book (stored as string in JSON). Links physical items to this MBook. */
	private String bookId = null;
	public UUID getBookId() { return bookId == null || bookId.isEmpty() ? null : UUID.fromString(bookId); }
	public void setBookId(UUID id) { this.bookId = id == null ? null : id.toString(); this.changed(); }

	/** Type of book (e.g. SERVER_BOOK). Used for display and future logic. */
	private BookType bookType = BookType.SERVER_BOOK;
	public BookType getBookType() { return bookType; }
	public void setBookType(BookType bookType) { this.bookType = bookType; this.changed(); }

	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //

	/** Returns a clean copy for storage/display; strips bookId/bookType from item so they are not stored in the template. */
	public static ItemStack fixItem(ItemStack item)
	{
		if (!BookUtil.hasBookMeta(item)) return null;
		item = new ItemStack(item);
		item.setAmount(1);
		BookUtil.removeBookMetadata(item);
		return item;
	}

}
