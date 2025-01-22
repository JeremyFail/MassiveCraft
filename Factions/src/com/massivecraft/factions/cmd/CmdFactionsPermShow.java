package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPerm;
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

		for (String permitted : permittedIds)
		{
			permables.add(MPerm.idToMPermable(permitted));
		}

		String removeString = Txt.parse(" of ") + faction.getDisplayName(msender);
		List<String> permableList = permables.stream()
				.map(permable -> permable.getDisplayName(msender))
				.map(s -> s.replace(removeString, ""))
				.collect(Collectors.toList());
		String permableNames = Txt.implodeCommaAnd(permableList, Txt.parse("<i>"));

		// Create messages
		msg("<i>In <reset>%s <i>permission <reset>%s <i>is granted to <reset>%s<i>.", faction.describeTo(msender), mperm.getDesc(true, false), permableNames);
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
