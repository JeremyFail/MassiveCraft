package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.ChunkUtil;

import java.util.Set;
import java.util.function.Predicate;

public class CmdFactionsAccessSetFill extends CmdFactionsAccessSetXSimple
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsAccessSetFill(boolean claim)
	{
		// Super
		super(claim);
		
		// Aliases
		this.addAliases("fill");
		
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
		Perm perm = claim ? Perm.ACCESS_GRANT_FILL : Perm.ACCESS_DENY_FILL;
		this.addRequirements(RequirementHasPerm.get(perm));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Set<PS> getChunks() throws MassiveException
	{
		// Common Startup
		final PS chunk = PS.valueOf(me.getLocation()).getChunk(true);
		
		// What faction (aka color) resides there?
		// NOTE: Wilderness/None is valid. 
		final Faction color = BoardColl.get().getFactionAt(chunk);
		
		// Calculate
		int max = MConf.get().setFillMax;
		Predicate<PS> matcher = ps -> BoardColl.get().getFactionAt(ps) == color;
		return ChunkUtil.getChunkArea(chunk, matcher, max);
	}

}
