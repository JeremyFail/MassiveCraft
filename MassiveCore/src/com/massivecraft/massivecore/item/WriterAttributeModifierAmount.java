package com.massivecraft.massivecore.item;

import org.bukkit.attribute.AttributeModifier;

public class WriterAttributeModifierAmount extends WriterAbstractAttributeModifier<Double, Double>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final WriterAttributeModifierAmount i = new WriterAttributeModifierAmount();
	public static WriterAttributeModifierAmount get() { return i; }
	public WriterAttributeModifierAmount()
	{
		super("amount");
	}
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //
	
	@Override
	public Double getA(DataAttributeModifier ca, Object d)
	{
		return ca.getAmount();
	}
	
	@Override
	public void setA(DataAttributeModifier ca, Double fa, Object d)
	{
		ca.setAmount(fa);
	}
	
	@Override
	public Double getB(AttributeModifier cb, Object d)
	{
		return cb.getAmount();
	}
	
}
