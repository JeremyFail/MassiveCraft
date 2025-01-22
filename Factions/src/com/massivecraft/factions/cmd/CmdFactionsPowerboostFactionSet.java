package com.massivecraft.factions.cmd;

public class CmdFactionsPowerboostFactionSet extends CmdFactionsPowerboostFactionAbstract
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
