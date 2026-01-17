package com.massivecraft.massivecore.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

public class WriterItemStackMetaLodestoneTracked extends WriterAbstractItemStackMetaField<CompassMeta, Boolean, Boolean>
{
	
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static final WriterItemStackMetaLodestoneTracked i = new WriterItemStackMetaLodestoneTracked();
	public static WriterItemStackMetaLodestoneTracked get() { return i; }
	
	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //
	
	public WriterItemStackMetaLodestoneTracked()
	{
		super(CompassMeta.class);
		this.setMaterial(Material.COMPASS);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Boolean getA(DataItemStack ca, ItemStack d) { return ca.isLodestoneTracked(); }
	
	@Override
	public void setA(DataItemStack ca, Boolean fa, ItemStack d) { ca.setLodestoneTracked(fa); }
	
	@Override
	public Boolean getB(CompassMeta cb, ItemStack d) { return cb.isLodestoneTracked(); }
	
	@Override
	public void setB(CompassMeta cb, Boolean fb, ItemStack d) { cb.setLodestoneTracked(fb); }
	
}
