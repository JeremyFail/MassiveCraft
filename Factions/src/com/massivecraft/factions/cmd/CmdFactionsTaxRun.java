package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqTaxEnabled;
import com.massivecraft.factions.task.TaskTax;
import com.massivecraft.massivecore.MassiveException;

public class CmdFactionsTaxRun extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsTaxRun()
	{
		this.addRequirements(ReqTaxEnabled.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		TaskTax.get().invoke(System.currentTimeMillis());
	}
	
}
