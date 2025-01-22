package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeRank;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.util.Txt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CmdFactionsRankEditDelete extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsRankEditDelete()
	{
		// Parameters
		this.addParameter(TypeString.get(), "rank");
		this.addParameter(TypeFaction.get(), "faction", "you");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Faction faction = this.readArgAt(1, msenderFaction);

		// Rank if any passed.
		TypeRank typeRank = new TypeRank(faction);
		Rank rank = typeRank.read(this.argAt(0), sender);

		CmdFactionsRankEdit.ensureAllowed(msender, faction, "delete");

		Collection<Rank> ranks = faction.getRanks().getAll();
		if (ranks.size() <= 2)
		{
			throw new MassiveException().addMsg("<b>A faction must have at least two ranks.");
		}

		List<MPlayer> mplayers = faction.getMPlayersWhereRank(rank);
		if (!mplayers.isEmpty())
		{
			int count = mplayers.size();
			List<String> names = mplayers.stream().map(m -> m.getDisplayName(sender)).collect(Collectors.toList());
			String namesDesc = Txt.implodeCommaAnd(names, Txt.parse("<i>"));
			String rankRanks = count == 1 ? "rank" : "ranks";
			throw new MassiveException().addMsg("<b>This rank is held by <h>%s <b>change their %s first.", namesDesc, rankRanks);
		}

		String visual = rank.getVisual();
		faction.getRanks().detachEntity(rank);

		// Inform
		msg("<i>You deleted the rank <reset>%s<i>.", visual);
	}
	
}
