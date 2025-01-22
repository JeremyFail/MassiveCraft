package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeMPlayer;

public abstract class CmdFactionsPowerboostPlayerAbstract extends CmdFactionsPowerboostAbstract
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsPowerboostPlayerAbstract()
	{
		super(TypeMPlayer.get(), "player");
	}

}
