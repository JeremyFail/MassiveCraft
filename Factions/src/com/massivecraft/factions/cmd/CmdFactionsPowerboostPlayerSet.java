package com.massivecraft.factions.cmd;

public class CmdFactionsPowerboostPlayerSet extends CmdFactionsPowerboostPlayerAbstract
{
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public double calcNewPowerboost(double current, double d)
	{
		return d;
	}

}
