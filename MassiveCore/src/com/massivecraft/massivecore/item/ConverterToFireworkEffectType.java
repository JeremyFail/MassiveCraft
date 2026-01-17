package com.massivecraft.massivecore.item;

import org.bukkit.FireworkEffect;

public class ConverterToFireworkEffectType extends Converter<String, FireworkEffect.Type>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterToFireworkEffectType i = new ConverterToFireworkEffectType();
	public static ConverterToFireworkEffectType get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public FireworkEffect.Type convert(String x)
	{
		if (x == null) return null;
		return FireworkEffect.Type.valueOf(x);
	}

}
