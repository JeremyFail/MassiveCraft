package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPerm;
import com.massivecraft.factions.cmd.type.TypeMPermable;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.util.Txt;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CmdFactionsPermShow extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsPermShow()
	{
		// Parameters
		this.addParameter(TypeMPerm.get(), "perm");
		this.addParameter(TypeFaction.get(), "faction", "you");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Arg: Faction
		MPerm mperm = this.readArg();
		Faction faction = this.readArg(msenderFaction);

		Set<String> permittedIds = faction.getPerms().get(mperm.getId());
		List<MPermable> permables = new MassiveList<>();

		if (permittedIds != null && !permittedIds.isEmpty())
		{
			for (String permitted : permittedIds)
			{
				MPermable mPermable = TypeMPermable.get(faction).read(permitted, sender);
				if (mPermable == null) continue;
				permables.add(mPermable);
			}
		}

		// If no one has this permission, inform the sender
		if (permables.isEmpty())
		{
			msg(
				"<i>In <reset>%s<i> permission <reset>%s<i> is not currently granted to anyone.", 
				faction.describeTo(msender), 
				mperm.getDesc(true, false)
			);
			return;
		}

		// Otherwise, create messages
		msg(
			"<i>In <reset>%s <i>permission <reset>%s <i>is granted to <reset>%s<i>.",
			faction.describeTo(msender),
			mperm.getDesc(true, false),
			permablesToDisplayString(permables, me)
		);
	}

	@Deprecated
	public static MPerm.MPermable idToMPermable(String id)
	{
		return MPerm.idToMPermable(id);
	}

	public static String permablesToDisplayString(Collection<MPermable> permables, Object watcherObject)
	{
		MPlayer mplayer = MPlayer.get(watcherObject);
		Faction faction = mplayer.getFaction();

		String removeString;
		if (faction.isNone()) removeString = "";
		else removeString = Txt.parse(" of ") + faction.getDisplayName(mplayer);

		List<String> permableList = permables.stream()
				.map(permable -> permable.getDisplayName(mplayer))
				.map(s -> s.replace(removeString, ""))
				.collect(Collectors.toList());

		return Txt.implodeCommaAnd(permableList, Txt.parse("<i>"));
	}
	
}
