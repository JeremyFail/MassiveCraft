package com.massivecraft.creativegates.cmd.type;

import com.massivecraft.massivecore.command.type.enumeration.TypeEnum;

/**
 * Tab-completable actions for {@code /cg manage access}.
 */
public enum TypeAccessAction
{
	LIST,
	ADD,
	REMOVE,
	RESET,
	;
	
	public static final TypeEnum<TypeAccessAction> TYPE = new TypeEnum<>(TypeAccessAction.class);
}
