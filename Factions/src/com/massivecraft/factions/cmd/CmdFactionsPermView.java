package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPermable;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public class CmdFactionsPermView extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsPermView()
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

		if (permable == faction)
		{
			throw new MassiveException().addMsg("<b>A faction can't have perms for itself.");
		}

		List<MPerm> perms = new MassiveList<>();

		for (MPerm mperm : MPerm.getAll())
		{
			if (faction.isPermitted(permable.getId(), mperm.getId())) perms.add(mperm);
		}

		if (perms.isEmpty())
		{
			msg("<i>In <reset>%s <reset>%s <i>specifically has <b>no permissions<i>.", faction.describeTo(msender), permable.getDisplayName(sender));
		}
		else
		{
			List<String> permNames = perms.stream().map(perm -> Txt.parse("<h>") + perm.getName()).collect(Collectors.toList());
			String names = Txt.implodeCommaAnd(permNames, Txt.parse("<i>"));

			// Create messages
			String permissionSingularPlural = permNames.size() == 1 ? "permission" : "permissions";
			msg("<i>In <reset>%s <reset>%s <i>specifically has the %s: <reset>%s<i>.", faction.describeTo(msender), permable.getDisplayName(sender), permissionSingularPlural, names);
		}
		if (permable instanceof MPlayer)
		{
			MPlayer mplayer = (MPlayer) permable;
			msg("<i>They may have other permissions through their faction membership, rank or relation to <reset>%s<i>.", faction.describeTo(msender));

			List<Mson> msons = new MassiveList<>();

			if (mplayer.getFaction() != faction) msons.add(Mson.parse("<command>[faction]").command(this, mplayer.getFaction().getName(), faction.getName()));
			msons.add(Mson.parse("<command>[rank]").command(this, mplayer.getFaction().getName() + "-" + mplayer.getRank().getName(), faction.getName()));
			if (mplayer.getFaction() != faction) msons.add(Mson.parse("<command>[relation]").command(this, faction.getRelationTo(mplayer).toString(), faction.getName()));
			Mson msons2 = Mson.implode(msons, Mson.SPACE);
			message(mson(mson("Commands: ").color(ChatColor.YELLOW), msons2));
		}
		if (permable instanceof Faction)
		{
			Faction faction1 = (Faction) permable;
			msg("<i>They may have other permissions through their relation to <reset>%s<i>.", faction.describeTo(msender));
			Mson msonRelation = Mson.parse("<command>[relation]").command(this, faction.getRelationTo(faction1).toString(), faction.getName());
			Mson msons = Mson.implode(MUtil.list(msonRelation), Mson.SPACE);
			message(mson(mson("Commands: ").color(ChatColor.YELLOW), msons));
		}
		if (permable instanceof Rank && !faction.hasRank((Rank) permable))
		{
			Rank rank = (Rank) permable;
			msg("<i>They may have other permissions thorugh their faction membership or relation to <reset>%s<i>.", faction.describeTo(msender));
			Mson msonFaction = Mson.parse("<command>[faction]").command(this, rank.getFaction().getName(), faction.getName());
			Mson msonRelation = Mson.parse("<command>[relation]").command(this, faction.getRelationTo(rank.getFaction()).toString(), faction.getName());
			Mson msons = Mson.implode(MUtil.list(msonFaction, msonRelation), Mson.SPACE);
			message(mson(mson("Commands: ").color(ChatColor.YELLOW), msons));
		}
		msg("<i>To view all perms held by %s <i>type:", permable.getDisplayName(sender));
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermViewall.getTemplateWithArgs(sender, MUtil.list(permable.getName(), faction.getName())));
	}
	
}
