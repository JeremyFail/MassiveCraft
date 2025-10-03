package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPermable;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.util.Txt;

import java.util.List;
import java.util.stream.Collectors;

public class CmdFactionsPermViewall extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsPermViewall()
	{
		// Parameters
		this.addParameter(TypeMPermable.get(), "rank/rel/player/faction");
		this.addParameter(TypeFaction.get(), "faction", "you");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Arg: Faction
		Faction faction = this.readArgAt(1, msenderFaction);
		TypeMPermable permableType = TypeMPermable.get(faction);
		MPerm.MPermable permable = permableType.read(this.argAt(0), sender);

		// Self check
		if (permable == faction)
		{
			throw new MassiveException().addMsg("<b>A faction can't have perms for itself.");
		}

		// Create list of all applicable permables
		List<MPermable> permables = new MassiveList<>();
		permables.add(permable);

		if (permable instanceof MPlayer)
		{
			MPlayer mplayer = (MPlayer) permable;
			permables.add(mplayer.getFaction());
			permables.add(mplayer.getRank());
			permables.add(faction.getRelationTo(mplayer));
		}
		if (permable instanceof Faction)
		{
			Faction faction1 = (Faction) permable;
			permables.add(faction.getRelationTo(faction1));
		}
		if (permable instanceof Rank && !faction.hasRank((Rank) permable))
		{
			Rank rank = (Rank) permable;
			Faction faction1 = rank.getFaction();
			permables.add(faction1);
			permables.add(faction.getRelationTo(faction1));
		}

		// Find the perms they have
		List<MPerm> perms = new MassiveList<>();

		perm:
		for (MPerm mperm : MPerm.getAll())
		{
			String mpermId = mperm.getId();
			permable:
			for (MPermable mpa : permables)
			{
				if (!faction.isPermitted(mpa.getId(), mperm.getId())) continue permable;
				perms.add(mperm);
				continue perm;
			}

		}

		if (perms.isEmpty())
		{
			msg("<i>In <reset>%s <reset>%s <i>has <b>no permissions<i>.", faction.describeTo(msender), permable.getDisplayName(sender));
		}
		else
		{
			List<String> permNames = perms.stream().map(perm -> Txt.parse("<h>") + perm.getName()).collect(Collectors.toList());
			String names = Txt.implodeCommaAnd(permNames, Txt.parse("<i>"));

			// Create messages
			String permissionSingularPlural = permNames.size() == 1 ? "permission" : "permissions";
			msg("<i>In <reset>%s <reset>%s <i>has the %s: <reset>%s<i> either specifically granted to them or through rank, relation or faction membership.", faction.describeTo(msender), permable.getDisplayName(sender), permissionSingularPlural, names);
		}
	}
	
}
