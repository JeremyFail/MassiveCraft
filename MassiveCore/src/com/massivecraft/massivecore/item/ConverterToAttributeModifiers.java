package com.massivecraft.massivecore.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.Set;

public class ConverterToAttributeModifiers extends Converter<Set<DataAttributeModifier>, Multimap<Attribute, AttributeModifier>>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterToAttributeModifiers i = new ConverterToAttributeModifiers();
	public static ConverterToAttributeModifiers get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Multimap<Attribute, AttributeModifier> convert(Set<DataAttributeModifier> set)
	{
		// Null
		if (set == null) return null;
		
		// Create
		Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
		
		// Fill
		for (DataAttributeModifier dataAttributeModifier : set)
		{
			AttributeModifier attributeModifier;
			
			try
			{
				attributeModifier = ConverterToAttributeModifier.get().convert(dataAttributeModifier);
			}
			catch (Throwable t)
			{
				t.printStackTrace();
				continue;
			}
			
			multimap.put(
				dataAttributeModifier.getAttribute(),
				attributeModifier
			);
		}
		
		// Return
		return multimap;
	}

}
