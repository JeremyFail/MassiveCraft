package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;

public abstract class CmdFactionsPowerboostFactionAbstract extends CmdFactionsPowerboostAbstract
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsPowerboostFactionAbstract()
	{
		super(TypeFaction.get(), "faction");
	}

}
