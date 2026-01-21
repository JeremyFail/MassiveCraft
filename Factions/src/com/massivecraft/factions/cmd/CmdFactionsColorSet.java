package com.massivecraft.factions.cmd;

/**
 * Parent command for setting faction colors.
 */
public class CmdFactionsColorSet extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdFactionsColorSetPrimary cmdFactionsColorSetPrimary = new CmdFactionsColorSetPrimary();
	public CmdFactionsColorSetSecondary cmdFactionsColorSetSecondary = new CmdFactionsColorSetSecondary();
	
}
