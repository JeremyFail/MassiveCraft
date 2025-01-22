package com.massivecraft.factions.cmd;

import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;

public class CmdFactionsAccessDeny extends CmdFactionsAccessAbstract
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	public CmdFactionsAccessSetOne cmdFactionsAccessDenyOne = new CmdFactionsAccessSetOne(false);
	public CmdFactionsAccessSetSquare cmdFactionsAccessDenySquare = new CmdFactionsAccessSetSquare(false);
	public CmdFactionsAccessSetCircle cmdFactionsAccessDenyCircle = new CmdFactionsAccessSetCircle(false);
	public CmdFactionsAccessSetFill cmdFactionsAccessDenyFill = new CmdFactionsAccessSetFill(false);

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsAccessDeny()
	{
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
	}
	
}
