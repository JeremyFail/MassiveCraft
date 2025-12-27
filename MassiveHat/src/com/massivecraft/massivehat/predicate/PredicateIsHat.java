package com.massivecraft.massivehat.predicate;

import com.massivecraft.massivecore.predicate.Predicate;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivehat.entity.MConf;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PredicateIsHat implements Predicate<ItemStack>
{
	// -------------------------------------------- //
	// INSTANCES
	// -------------------------------------------- //
	
	// See if it is a hat, excluding normal helmets
	private static final PredicateIsHat iHat = new PredicateIsHat(false);
	public static PredicateIsHat getHat() { return iHat; }
	
	// See if it is a hat, including normal helmets
	private static final PredicateIsHat iHeadwear = new PredicateIsHat(true);
	public static PredicateIsHat getHeadwear() { return iHeadwear; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private boolean includeHelmets;
	public boolean includesHelmets() { return this.includeHelmets; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public PredicateIsHat(boolean includeHelmets)
	{
		this.includeHelmets = includeHelmets;
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	// With "hat" we mean something that is not usually equippable but should be.
	@Override
	public boolean apply(ItemStack itemStack)
	{
		// Nothingness should never be equipped
		if (InventoryUtil.isNothing(itemStack)) return false;
		
		Material material = itemStack.getType();
		
		// Is it a normal helmet?
		if (MConf.get().normalHelmets.contains(material)) return this.includesHelmets();
		
		// Is it a block?
		if (material.isBlock())
		{
			return MConf.get().blockHats.contains(material);
		}
		else
		{
			return MConf.get().itemHats.contains(material);
		}
	}
	
}
