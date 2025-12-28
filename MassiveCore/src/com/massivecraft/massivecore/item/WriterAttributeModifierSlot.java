package com.massivecraft.massivecore.item;

import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

public class WriterAttributeModifierSlot extends WriterAbstractAttributeModifier<EquipmentSlot, EquipmentSlot>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final WriterAttributeModifierSlot i = new WriterAttributeModifierSlot();
	public static WriterAttributeModifierSlot get() { return i; }
	public WriterAttributeModifierSlot()
	{
		super("slot");
	}
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //
	
	@Override
	public EquipmentSlot getA(DataAttributeModifier ca, Object d)
	{
		return ca.getSlot();
	}
	
	@Override
	public void setA(DataAttributeModifier ca, EquipmentSlot fa, Object d)
	{
		ca.setSlot(fa);
	}
	
	@Override
	public EquipmentSlot getB(AttributeModifier cb, Object d)
	{
		return cb.getSlot();
	}
	
}
