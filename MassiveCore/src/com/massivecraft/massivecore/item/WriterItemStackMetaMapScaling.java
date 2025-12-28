package com.massivecraft.massivecore.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class WriterItemStackMetaMapScaling extends WriterAbstractItemStackMetaField<MapMeta, Boolean, Boolean>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final WriterItemStackMetaMapScaling i = new WriterItemStackMetaMapScaling();
	public static WriterItemStackMetaMapScaling get() { return i; }
	public WriterItemStackMetaMapScaling()
	{
		super(MapMeta.class);
		this.setMaterial(Material.FILLED_MAP);
	}
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //

	@Override
	public Boolean getA(DataItemStack ca, ItemStack d)
	{
		return ca.isScaling();
	}

	@Override
	public void setA(DataItemStack ca, Boolean fa, ItemStack d)
	{
		ca.setScaling(fa);
	}

	@Override
	public Boolean getB(MapMeta cb, ItemStack d)
	{
		return cb.isScaling();
	}

	@Override
	public void setB(MapMeta cb, Boolean fb, ItemStack d)
	{
		cb.setScaling(fb);
	}
	
}
