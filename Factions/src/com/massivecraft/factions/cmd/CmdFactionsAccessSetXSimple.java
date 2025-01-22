package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeMPermable;

public abstract class CmdFactionsAccessSetXSimple extends CmdFactionsAccessSetX
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsAccessSetXSimple(boolean claim)
	{
		// Super
		super(claim);
		
		// Parameters
		this.addParameter(TypeMPermable.get(), "rank/rel/player/faction");
		this.setMPermableArgIndex(0);
	}
	
}
