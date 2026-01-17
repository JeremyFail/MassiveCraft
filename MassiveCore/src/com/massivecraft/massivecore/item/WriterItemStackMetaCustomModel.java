package com.massivecraft.massivecore.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class WriterItemStackMetaCustomModel extends WriterAbstractItemStackMetaField<ItemMeta, Integer, Integer>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final WriterItemStackMetaCustomModel i = new WriterItemStackMetaCustomModel();
	public static WriterItemStackMetaCustomModel get() { return i; }
	public WriterItemStackMetaCustomModel()
	{
		super(ItemMeta.class);
	}
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //

	@Override
	public Integer getA(DataItemStack ca, ItemStack d)
	{
		return ca.getCustomModel();
	}

	@Override
	public void setA(DataItemStack ca, Integer fa, ItemStack d)
	{
		ca.setCustomModel(fa);
	}

	@Override
	public Integer getB(ItemMeta cb, ItemStack d)
	{
		if (!cb.hasCustomModelData()) return null;
		return cb.getCustomModelData();
	}

	@Override
	public void setB(ItemMeta cb, Integer fb, ItemStack d)
	{
		cb.setCustomModelData(fb);
	}
	
}
