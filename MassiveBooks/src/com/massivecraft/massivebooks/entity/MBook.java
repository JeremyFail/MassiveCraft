package com.massivecraft.massivebooks.entity;

import com.massivecraft.massivebooks.BookUtil;
import com.massivecraft.massivecore.store.Entity;
import org.bukkit.inventory.ItemStack;

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
		
		return this;
	}
	
	// -------------------------------------------- //
	// VERSION
	// -------------------------------------------- //
	
	public int version = 1;
	
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
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	/** Returns a clean copy for storage/display; does not parse placeholders (raw content only). */
	public static ItemStack fixItem(ItemStack item)
	{
		if (!BookUtil.hasBookMeta(item)) return null;
		item = new ItemStack(item);
		item.setAmount(1);
		return item;
	}
	
}
