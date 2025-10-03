package com.massivecraft.massivecore.command.type.primitive;

import com.massivecraft.massivecore.command.type.TypeAbstract;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;

public class TypeStringConfirmation extends TypeAbstract<String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static TypeStringConfirmation i = new TypeStringConfirmation();
	public static TypeStringConfirmation get() { return i; }
	public TypeStringConfirmation() { super(String.class); }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public String getName()
	{
		return "confirmation text";
	}
	
	@Override
	public String read(String arg, CommandSender sender)
	{
		return arg;
	}
	
	@Override
	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		return Collections.emptySet();
	}

}
