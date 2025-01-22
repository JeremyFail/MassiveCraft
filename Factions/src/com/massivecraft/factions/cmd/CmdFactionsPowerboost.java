package com.massivecraft.factions.cmd;

public class CmdFactionsPowerboost extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdFactionsPowerboostPlayer cmdFactionsPowerBoostPlayer = new CmdFactionsPowerboostPlayer();
	public CmdFactionsPowerboostFaction cmdFactionsPowerBoostFaction = new CmdFactionsPowerboostFaction();
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsPowerboost()
	{
		// Child
		this.addChild(this.cmdFactionsPowerBoostPlayer);
		this.addChild(this.cmdFactionsPowerBoostFaction);
	}
	
}
