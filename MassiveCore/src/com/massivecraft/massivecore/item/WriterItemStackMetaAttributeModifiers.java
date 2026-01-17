package com.massivecraft.massivecore.item;

import com.google.common.collect.Multimap;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

public class WriterItemStackMetaAttributeModifiers extends WriterAbstractItemStackMetaField<ItemMeta, Set<DataAttributeModifier>, Multimap<Attribute, AttributeModifier>>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final WriterItemStackMetaAttributeModifiers i = new WriterItemStackMetaAttributeModifiers();
	public static WriterItemStackMetaAttributeModifiers get() { return i; }
	public WriterItemStackMetaAttributeModifiers()
	{
		super(ItemMeta.class);
		this.setMaterial(Material.DIAMOND_SWORD);
		this.setConverterTo(ConverterToAttributeModifiers.get());
		this.setConverterFrom(ConverterFromAttributeModifiers.get());
		this.addDependencyClasses(
			WriterAttributeModifier.class
		);
	}
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //
	
	@Override
	public Set<DataAttributeModifier> getA(DataItemStack ca, ItemStack d)
	{
		return ca.getAttributeModifiers();
	}
	
	@Override
	public void setA(DataItemStack ca, Set<DataAttributeModifier> fa, ItemStack d)
	{
		ca.setAttributeModifiers(fa);
	}
	
	@Override
	public Multimap<Attribute, AttributeModifier> getB(ItemMeta cb, ItemStack d)
	{
		return cb.getAttributeModifiers();
	}
	
	@Override
	public void setB(ItemMeta cb, Multimap<Attribute, AttributeModifier> fb, ItemStack d)
	{
		cb.setAttributeModifiers(fb);
	}
	
}
