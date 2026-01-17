package com.massivecraft.massivecore.item;

import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;


public abstract class WriterAbstractAttributeModifier<FA, FB> extends WriterAbstractReflect<DataAttributeModifier, AttributeModifier, DataAttributeModifier, AttributeModifier, FA, FB>
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public WriterAbstractAttributeModifier(String fieldName)
	{
		super(AttributeModifier.class, fieldName);
	}
	
	public WriterAbstractAttributeModifier()
	{
		this(null); 
	}
	
	// -------------------------------------------- //
	// CREATE
	// -------------------------------------------- //
	
	@Override
	public DataAttributeModifier createOA()
	{
		return new DataAttributeModifier();
	}
	
	@Override
	public AttributeModifier createOB()
	{
		return new AttributeModifier(
			"TEST",
			0d,
			Operation.ADD_NUMBER
		);
	}
	
}
