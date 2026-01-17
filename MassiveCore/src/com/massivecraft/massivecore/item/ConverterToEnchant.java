package com.massivecraft.massivecore.item;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public class ConverterToEnchant extends Converter<String, Enchantment>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterToEnchant i = new ConverterToEnchant();
	public static ConverterToEnchant get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Enchantment convert(String x)
	{
		if (x == null) return null;
		return Enchantment.getByKey(NamespacedKey.minecraft(x));
	}

}
