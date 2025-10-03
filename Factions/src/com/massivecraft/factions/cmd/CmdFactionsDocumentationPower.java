package com.massivecraft.factions.cmd;

import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.MassiveException;

public class CmdFactionsDocumentationPower extends FactionsCommandDocumentation
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsDocumentationPower()
	{

	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		msgDoc("All players  have an amount of power ranging from <h>%.2f <i>to <h>%.2f<i>.", MConf.get().powerMin, MConf.get().powerMax);
		msgDoc("The power of a faction is equal to the combined power of all it's members.");
		msgDoc("Your power is <h>%.2f<i>", msender.getPower());
		msgDoc("Your faction's power is <h>%.2f<i>", msenderFaction.getPower());
		msgDoc("The amount of chunks a faction can claim is the amount power it has.");
		msgDoc("For every hour you are online you gain <h>%.2f <i>power.", MConf.get().powerPerHour);
		msgDoc("Every time you die you power is decreased by <h>%.2f <i>.", MConf.get().powerPerDeath*-1);
		if (!MConf.get().canLeaveWithNegativePower && MConf.get().powerMin < 0)
		{
			msgDoc("You can't leave a faction if your power is negative.");
		}

	}
	
}
