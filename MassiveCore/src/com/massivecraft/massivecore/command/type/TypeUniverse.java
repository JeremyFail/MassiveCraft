package com.massivecraft.massivecore.command.type;

import com.massivecraft.massivecore.entity.Multiverse;

import java.util.Collection;

public class TypeUniverse extends TypeAbstractChoice<String>
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	protected Multiverse multiverse = null;
	
	public Multiverse getMultiverse()
	{
		return this.multiverse;
	}
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	public static TypeUniverse get(Multiverse multiverse) { return new TypeUniverse(multiverse); }

	public TypeUniverse(Multiverse multiverse) { super(String.class); this.multiverse = multiverse; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Collection<String> getAll()
	{
		return this.getMultiverse().getUniverses();
	}

}
