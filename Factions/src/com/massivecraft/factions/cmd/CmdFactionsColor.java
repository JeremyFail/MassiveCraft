package com.massivecraft.factions.cmd;

/**
 * Parent command for faction color management.
 * Supports setting and viewing primary and secondary colors.
 */
public class CmdFactionsColor extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdFactionsColorSet cmdFactionsColorSet = new CmdFactionsColorSet();
	public CmdFactionsColorShow cmdFactionsColorShow = new CmdFactionsColorShow();
	public CmdFactionsColorReset cmdFactionsColorReset = new CmdFactionsColorReset();
	
}
