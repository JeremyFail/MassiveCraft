package com.massivecraft.factions.cmd;

import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;

public class CmdFactionsAccessGrant extends CmdFactionsAccessAbstract
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	public CmdFactionsAccessSetOne cmdFactionsAccessGrantOne = new CmdFactionsAccessSetOne(true);
	public CmdFactionsAccessSetSquare cmdFactionsAccessGrantSquare = new CmdFactionsAccessSetSquare(true);
	public CmdFactionsAccessSetCircle cmdFactionsAccessGrantCircle = new CmdFactionsAccessSetCircle(true);
	public CmdFactionsAccessSetFill cmdFactionsAccessGrantFill = new CmdFactionsAccessSetFill(true);

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsAccessGrant()
	{
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
	}
	
}
