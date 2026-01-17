package com.massivecraft.massivecore.item;

import org.bukkit.NamespacedKey;

public class ConverterFromNamespacedKeys extends ConverterList<NamespacedKey, String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterFromNamespacedKeys i = new ConverterFromNamespacedKeys();
	public static ConverterFromNamespacedKeys get() { return i; }
	public ConverterFromNamespacedKeys()
	{
		super(ConverterFromNamespacedKey.get());
	}

}
