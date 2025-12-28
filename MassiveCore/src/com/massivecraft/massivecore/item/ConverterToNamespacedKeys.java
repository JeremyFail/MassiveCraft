package com.massivecraft.massivecore.item;

import org.bukkit.NamespacedKey;

public class ConverterToNamespacedKeys extends ConverterList<String, NamespacedKey>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterToNamespacedKeys i = new ConverterToNamespacedKeys();
	public static ConverterToNamespacedKeys get() { return i; }
	public ConverterToNamespacedKeys()
	{
		super(ConverterToNamespacedKey.get());
	}

}
