package com.massivecraft.massivecore.item;

import org.bukkit.FireworkEffect;

public class ConverterFromFireworkEffectType extends Converter<FireworkEffect.Type, String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterFromFireworkEffectType i = new ConverterFromFireworkEffectType();
	public static ConverterFromFireworkEffectType get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String convert(FireworkEffect.Type x)
	{
		if (x == null) return null;
		return x.name();
	}

}
