package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeRank;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

import java.util.Collection;

public class CmdFactionsRankEditName extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsRankEditName()
	{
		// Parameters
		this.addParameter(TypeRank.get(), "rank");
		this.addParameter(TypeString.get(), "new name");
		this.addParameter(TypeFaction.get(), "faction", "you");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		String name = this.readArgAt(1);
		Faction faction = this.readArgAt(2, msenderFaction);

		// Rank if any passed.
		TypeRank typeRank = new TypeRank(faction);
		Rank rank = typeRank.read(this.argAt(0), sender);

		// Args

		CmdFactionsRankEdit.ensureAllowed(msender, faction, "rename");

		Collection<Rank> ranks = faction.getRanks().getAll();

		if (ranks.stream().map(Rank::getName).anyMatch(s -> s.equalsIgnoreCase(name)))
		{
			throw new MassiveException().addMsg("<b>There is already a rank called <h>%s<b>.", name);
		}

		String priorVisual = rank.getVisual();
		rank.setName(name);

		// Visual
		msg("<i>You renamed the rank from <reset>%s <i>to <reset>%s<i>.", priorVisual, rank.getVisual());
	}

}
