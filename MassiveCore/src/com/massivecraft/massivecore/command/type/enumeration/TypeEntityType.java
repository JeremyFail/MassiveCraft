package com.massivecraft.massivecore.command.type.enumeration;

import org.bukkit.entity.EntityType;

public class TypeEntityType extends TypeEnum<EntityType>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TypeEntityType i = new TypeEntityType();
	public static TypeEntityType get() { return i; }
	public TypeEntityType()
	{
		super(EntityType.class);
	}

}
