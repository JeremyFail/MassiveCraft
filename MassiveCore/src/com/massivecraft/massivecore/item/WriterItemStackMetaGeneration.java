package com.massivecraft.massivecore.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;

public class WriterItemStackMetaGeneration extends WriterAbstractItemStackMetaField<BookMeta, Generation, Generation>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final WriterItemStackMetaGeneration i = new WriterItemStackMetaGeneration();
	public static WriterItemStackMetaGeneration get() { return i; }
	
	public WriterItemStackMetaGeneration()
	{
		super(BookMeta.class);
		this.setMaterial(Material.WRITTEN_BOOK);
	}
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //

	@Override
	public Generation getA(DataItemStack ca, ItemStack d)
	{
		return ca.getGeneration();
	}

	@Override
	public void setA(DataItemStack ca, Generation fa, ItemStack d)
	{
		ca.setGeneration(fa);
	}

	@Override
	public Generation getB(BookMeta cb, ItemStack d)
	{
		return cb.getGeneration();
	}

	@Override
	public void setB(BookMeta cb, Generation fb, ItemStack d)
	{
		cb.setGeneration(fb);
	}
	
}
