package com.massivecraft.massivecore.item;

import org.bukkit.NamespacedKey;

public class ConverterFromNamespacedKey extends Converter<NamespacedKey, String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterFromNamespacedKey i = new ConverterFromNamespacedKey();
	public static ConverterFromNamespacedKey get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public String convert(NamespacedKey x)
	{
		if (x == null) return null;
		return x.toString();
	}

}
