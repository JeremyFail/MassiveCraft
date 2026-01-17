package com.massivecraft.massivecore.item;

import com.google.common.collect.Multimap;
import com.massivecraft.massivecore.collections.MassiveSet;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.Map.Entry;
import java.util.Set;

public class ConverterFromAttributeModifiers extends Converter<Multimap<Attribute, AttributeModifier>, Set<DataAttributeModifier>>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final ConverterFromAttributeModifiers i = new ConverterFromAttributeModifiers();
	public static ConverterFromAttributeModifiers get() {return i;}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Set<DataAttributeModifier> convert(Multimap<Attribute, AttributeModifier> multimap)
	{
		// Null
		if (multimap == null) return null;
		
		// Create
		Set<DataAttributeModifier> set = new MassiveSet<>();
		
		// Fill
		for (Entry<Attribute, AttributeModifier> entry : multimap.entries())
		{
			DataAttributeModifier dataAttributeModifier;
			AttributeModifier attributeModifier = entry.getValue();
			try
			{
				dataAttributeModifier = ConverterFromAttributeModifier.get().convert(attributeModifier).setAttribute(entry.getKey());
			}
			catch (Throwable t)
			{
				t.printStackTrace();
				continue;
			}
			
			set.add(dataAttributeModifier);
		}
		
		// Return
		return set;
	}
	
}
