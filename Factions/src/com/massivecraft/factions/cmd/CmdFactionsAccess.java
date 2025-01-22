package com.massivecraft.factions.cmd;

import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;

public class CmdFactionsAccess extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdFactionsAccessView cmdFactionsAccessView = new CmdFactionsAccessView();
	public CmdFactionsAccessGrant cmdFactionsAccessGrant = new CmdFactionsAccessGrant();
	public CmdFactionsAccessDeny cmdFactionsAccessDeny = new CmdFactionsAccessDeny();
	public CmdFactionsAccessInspect cmdFactionsAccessInspect = new CmdFactionsAccessInspect();
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsAccess()
	{
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
	}
	
}
