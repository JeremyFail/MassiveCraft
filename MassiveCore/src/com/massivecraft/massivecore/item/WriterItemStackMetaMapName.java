package com.massivecraft.massivecore.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class WriterItemStackMetaMapName extends WriterAbstractItemStackMetaField<MapMeta, String, String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final WriterItemStackMetaMapName i = new WriterItemStackMetaMapName();
	public static WriterItemStackMetaMapName get() { return i; }
	
	public WriterItemStackMetaMapName()
	{
		super(MapMeta.class);
		this.setMaterial(Material.FILLED_MAP);
	}
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //

	@Override
	public String getA(DataItemStack ca, ItemStack d)
	{
		return ca.getMapName();
	}

	@Override
	public void setA(DataItemStack ca, String fa, ItemStack d)
	{
		ca.setMapName(fa);
	}

	@Override
	public String getB(MapMeta cb, ItemStack d)
	{
		return cb.hasLocationName() ? cb.getLocationName() : null;
	}

	@Override
	public void setB(MapMeta cb, String fb, ItemStack d)
	{
		cb.setLocationName(fb);
	}
	
}
