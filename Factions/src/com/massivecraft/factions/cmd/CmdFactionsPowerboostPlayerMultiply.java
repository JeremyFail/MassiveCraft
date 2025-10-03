package com.massivecraft.factions.cmd;

public class CmdFactionsPowerboostPlayerMultiply extends CmdFactionsPowerboostPlayerAbstract
{
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public double calcNewPowerboost(double current, double d)
	{
		return current * d;
	}

}
