package com.massivecraft.massivebooks.cmd.type;

import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivebooks.entity.MBookColl;
import com.massivecraft.massivecore.command.type.store.TypeEntity;

public class TypeMBook extends TypeEntity<MBook>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final TypeMBook i = new TypeMBook();
	public static TypeMBook get() { return i; }
	public TypeMBook()
	{
		super(MBookColl.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String getName()
	{
		return "saved book";
	}

}
