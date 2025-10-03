package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.ps.PS;

import java.util.Collections;
import java.util.Set;


public class CmdFactionsAccessSetOne extends CmdFactionsAccessSetXSimple
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsAccessSetOne(boolean claim)
	{
		// Super
		super(claim);
		
		// Aliases
		this.addAliases("one");

		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
		Perm perm = claim ? Perm.ACCESS_GRANT_ONE : Perm.ACCESS_DENY_ONE;
		this.addRequirements(RequirementHasPerm.get(perm));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Set<PS> getChunks()
	{
		final PS chunk = PS.valueOf(me.getLocation()).getChunk(true);
		return Collections.singleton(chunk);
	}
	
}
