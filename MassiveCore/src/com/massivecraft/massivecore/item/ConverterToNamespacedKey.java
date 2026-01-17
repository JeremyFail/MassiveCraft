package com.massivecraft.massivecore.item;

import org.bukkit.NamespacedKey;

public class ConverterToNamespacedKey extends Converter<String, NamespacedKey>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterToNamespacedKey i = new ConverterToNamespacedKey();
	public static ConverterToNamespacedKey get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@SuppressWarnings("deprecation")
	@Override
	public NamespacedKey convert(String x)
	{
		if (x == null) return null;
		String[] parts = x.toLowerCase().split(":");
		if (parts.length == 1) return NamespacedKey.minecraft(parts[0]);
		if (parts[0].equals(NamespacedKey.MINECRAFT)) return NamespacedKey.minecraft(parts[1]);
		return new NamespacedKey(parts[0], parts[1]);
	}

}
