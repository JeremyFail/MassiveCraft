package com.massivecraft.massivecore.item;

import org.bukkit.attribute.AttributeModifier;

public class ConverterToAttributeModifier extends Converter<DataAttributeModifier, AttributeModifier>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterToAttributeModifier i = new ConverterToAttributeModifier();
	public static ConverterToAttributeModifier get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public AttributeModifier convert(DataAttributeModifier x)
	{
		if (x == null) return null;
		return x.toBukkit();
	}

}
