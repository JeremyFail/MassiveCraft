package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeRank;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeInteger;

import java.util.Collection;

public class CmdFactionsRankEditPriority extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsRankEditPriority()
	{
		// Parameters
		this.addParameter(TypeRank.get(), "rank");
		this.addParameter(TypeInteger.get(), "new priority");
		this.addParameter(TypeFaction.get(), "faction", "you");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Integer priority = this.readArgAt(1);
		Faction faction = this.readArgAt(2, msenderFaction);

		// Rank if any passed.
		TypeRank typeRank = new TypeRank(faction);
		Rank rank = typeRank.read(this.argAt(0), sender);

		CmdFactionsRankEdit.ensureAllowed(msender, faction, "edit");

		Collection<Rank> ranks = faction.getRanks().getAll();

		if (ranks.stream().map(Rank::getPriority).anyMatch(s -> s.equals(priority)))
		{
			throw new MassiveException().addMsg("<b>There is already a rank with the priority <h>%s<b>.", priority);
		}

		if (rank.isLeader())
		{
			Rank below = rank.getRankBelow();
			if (below.getPriority() > priority)
			{
				throw new MassiveException().addMsg("<b>The leader rank must have the highest priority.");
			}
		}
		else
		{
			if (priority >= faction.getLeaderRank().getPriority())
			{
				throw new MassiveException().addMsg("<b>No rank can have higher priority than the leader rank.");
			}
		}

		int priorPriority = rank.getPriority();
		rank.setPriority(priority);

		// Visual
		msg("<i>You changed the priority of <reset>%s <i>from <h>%s <i>to <h>%s<i>.", rank.getVisual(), priorPriority, priority);
	}
	
}
