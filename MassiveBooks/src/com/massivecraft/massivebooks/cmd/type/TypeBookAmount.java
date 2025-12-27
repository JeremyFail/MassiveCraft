package com.massivecraft.massivebooks.cmd.type;

import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeInteger;
import org.bukkit.command.CommandSender;

public class TypeBookAmount extends TypeInteger
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	public static final Integer ENSURE = Integer.MIN_VALUE;
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final TypeBookAmount i = new TypeBookAmount();
	public static TypeBookAmount get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Integer read(String arg, CommandSender sender) throws MassiveException
	{
		if (arg != null && arg.toLowerCase().startsWith("e")) return ENSURE;
		
		int ret = super.read(arg, sender);
		if (ret <= 0) throw new MassiveException().addMessage(Lang.TIMES_MUST_BE_POSITIVE);
		
		return ret;
	}
	
}
