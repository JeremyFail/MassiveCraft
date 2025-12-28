package com.massivecraft.massivecore.item;

import org.bukkit.attribute.AttributeModifier;

public class ConverterFromAttributeModifier extends Converter<AttributeModifier, DataAttributeModifier>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterFromAttributeModifier i = new ConverterFromAttributeModifier();
	public static ConverterFromAttributeModifier get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public DataAttributeModifier convert(AttributeModifier x)
	{
		if (x == null) return null;
		return new DataAttributeModifier(x);
	}

}
