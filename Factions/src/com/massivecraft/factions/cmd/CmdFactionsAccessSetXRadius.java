package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeMPermable;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeInteger;


public abstract class CmdFactionsAccessSetXRadius extends CmdFactionsAccessSetX
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsAccessSetXRadius(boolean claim)
	{
		// Super
		super(claim);
		
		// Parameters
		this.addParameter(TypeInteger.get(), "radius");

		this.addParameter(TypeMPermable.get(), "rank/rel/faction/player");
		this.setMPermableArgIndex(1);

	}
	
	// -------------------------------------------- //
	// EXTRAS
	// -------------------------------------------- //
	
	public Integer getRadius() throws MassiveException
	{
		int radius = this.readArgAt(0);
		
		// Radius Claim Min
		if (radius < 1)
		{
			throw new MassiveException().setMsg("<b>If you specify a radius, it must be at least 1.");
		}
		
		// Radius Claim Max
		if (radius > MConf.get().setRadiusMax && ! msender.isOverriding())
		{
			throw new MassiveException().setMsg("<b>The maximum radius allowed is <h>%s<b>.", MConf.get().setRadiusMax);
		}
		
		return radius;
	}
	
}
