package com.massivecraft.factions.cmd;

public class CmdFactionsPowerboostFactionAdd extends CmdFactionsPowerboostFactionAbstract
{
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public double calcNewPowerboost(double current, double d)
	{
		return current + d;
	}

}
