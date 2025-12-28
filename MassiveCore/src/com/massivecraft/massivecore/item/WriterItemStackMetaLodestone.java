package com.massivecraft.massivecore.item;

import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class WriterItemStackMetaLodestone extends WriterAbstractItemStackMetaField<CompassMeta, PS, PS>
{
	
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static final WriterItemStackMetaLodestone i = new WriterItemStackMetaLodestone();
	public static WriterItemStackMetaLodestone get()
	{
		return i;
	}
	
	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //
	
	public WriterItemStackMetaLodestone()
	{
		super(CompassMeta.class);
		this.setMaterial(Material.COMPASS);
	}
	
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //

	public static NamespacedKey NSK_LODESTONE_WORLD = new NamespacedKey(MassiveCore.get(), "lodestone_world");
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	private String getRootWorldName() {
		return Bukkit.getWorlds().get(0).getName();
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public PS getA(DataItemStack ca, ItemStack d) {
		return ca.getLodestone();
	}
	
	@Override
	public void setA(DataItemStack ca, PS fa, ItemStack d) {
		ca.setLodestone(fa);
	}
	
	@Override
	public PS getB(CompassMeta cb, ItemStack d) {
		if (!cb.hasLodestone()) return null;
		
		PS ps = PS.valueOf(cb.getLodestone());
		if (ps == null) return null;
		
		PersistentDataContainer pdc = cb.getPersistentDataContainer();
		String targetWorld = pdc.getOrDefault(NSK_LODESTONE_WORLD, PersistentDataType.STRING, ps.getWorld());
		
		if (ps.getWorld().equals(getRootWorldName())) return ps.withWorld(targetWorld);
		return ps;
	}
	
	@Override
	public void setB(CompassMeta cb, PS fb, ItemStack d) {
		if (fb == null) {
			cb.setLodestone(null);
			return;
		}
		
		// Write the PS's world to the
		cb.getPersistentDataContainer().set(NSK_LODESTONE_WORLD, PersistentDataType.STRING, fb.getWorld());
		if (Bukkit.getWorld(fb.getWorld()) == null) fb = fb.withWorld(getRootWorldName());
		
		cb.setLodestone(fb.asBukkitLocation());
	}
	
}
