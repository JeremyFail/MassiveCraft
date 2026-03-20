package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPermable;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.factions.util.PermTableUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Set;

public class CmdFactionsPermView extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsPermView()
	{
		this.addParameter(TypeMPermable.get(), "rank/rel/player/faction");
		this.addParameter(TypeFaction.get(), "faction", "you");
		this.addParameter(Parameter.getPage());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Same as viewall/manage: with 2 args, the second can be faction or page; with 3 args, faction then page.
		Faction faction;
		int page;
		int n = this.getArgs().size();
		if (n >= 3)
		{
			faction = this.readArgAt(1, msenderFaction);
			page = this.readArgAt(2, 1);
		}
		else if (n == 2)
		{
			String arg1 = this.argAt(1);
			try
			{
				page = Math.max(1, Integer.parseInt(arg1));
				faction = msenderFaction;
			}
			catch (NumberFormatException e)
			{
				faction = this.readArgAt(1, msenderFaction);
				page = 1;
			}
		}
		else
		{
			faction = msenderFaction;
			page = 1;
		}

		TypeMPermable permableType = TypeMPermable.get(faction);
		MPermable permable = permableType.read(this.argAt(0), sender);

		if (permable == faction)
		{
			throw new MassiveException().addMsg("<b>A faction can't have perms for itself.");
		}

		// Direct perms only: explicitly set on this permable (not inherited). Include non-visible only in override or console.
		List<MPerm> directPerms = new MassiveList<>();
		boolean showAllPerms = senderIsConsole || msender.isOverriding();
		for (MPerm mperm : MPerm.getAll())
		{
			Set<String> permittedIds = faction.getPerms().get(mperm.getId());
			if (permittedIds == null || !permittedIds.contains(permable.getId()))
			{
				continue;
			}
			if (!mperm.isVisible() && !showAllPerms)
			{
				continue;
			}
			directPerms.add(mperm);
		}

		if (directPerms.isEmpty())
		{
			msg("<i>In <reset>%s <reset>%s <i>specifically has <b>no permissions<i>.", faction.describeTo(msender), permable.getDisplayName(sender));
			if (permable instanceof MPlayer)
			{
				MPlayer mplayer = (MPlayer) permable;
				msg("<i>They may have other permissions through their faction membership, rank or relation to <reset>%s<i>.", faction.describeTo(msender));
				List<Mson> msons = new MassiveList<>();
				if (mplayer.getFaction() != faction) msons.add(Mson.parse("<command>[faction]").command(this, mplayer.getFaction().getName(), faction.getName()));
				msons.add(Mson.parse("<command>[rank]").command(this, mplayer.getFaction().getName() + "-" + mplayer.getRank().getName(), faction.getName()));
				if (mplayer.getFaction() != faction) msons.add(Mson.parse("<command>[relation]").command(this, faction.getRelationTo(mplayer).toString(), faction.getName()));
				message(Mson.mson(Mson.mson("Commands: ").color(ChatColor.YELLOW), Mson.implode(msons, Mson.SPACE)));
			}
			if (permable instanceof Faction)
			{
				Faction faction1 = (Faction) permable;
				msg("<i>They may have other permissions through their relation to <reset>%s<i>.", faction.describeTo(msender));
				Mson msonRelation = Mson.parse("<command>[relation]").command(this, faction.getRelationTo(faction1).toString(), faction.getName());
				message(Mson.mson(Mson.mson("Commands: ").color(ChatColor.YELLOW), msonRelation));
			}
			if (permable instanceof Rank && !faction.hasRank((Rank) permable))
			{
				Rank rank = (Rank) permable;
				msg("<i>They may have other permissions through their faction membership or relation to <reset>%s<i>.", faction.describeTo(msender));
				Mson msonFaction = Mson.parse("<command>[faction]").command(this, rank.getFaction().getName(), faction.getName());
				Mson msonRelation = Mson.parse("<command>[relation]").command(this, faction.getRelationTo(rank.getFaction()).toString(), faction.getName());
				message(Mson.mson(Mson.mson("Commands: ").color(ChatColor.YELLOW), Mson.implode(MUtil.list(msonFaction, msonRelation), Mson.SPACE)));
			}
			msg("<i>To view all perms held by %s <i>type:", permable.getDisplayName(sender));
			String targetArg0 = PermTableUtil.getTargetArgForPermable(permable, faction);
			String factionName0 = faction.getName();
			Mson viewallLink0 = CmdFactions.get().cmdFactionsPerm.cmdFactionsPermViewall.getTemplateWithArgs(sender, MUtil.list(targetArg0, factionName0));
			message(viewallLink0.command(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermViewall, targetArg0, factionName0).tooltipParse("<aqua>Click to run command"));
		}
		else
		{
			// Show table with only direct perms (view-only, no click); pagination runs view
			PermTableUtil.displayTable(sender, permable, faction, page, false, directPerms, this);
			msg("<i>To view all perms held by %s <i>type:", permable.getDisplayName(sender));
			String linkTargetArg = PermTableUtil.getTargetArgForPermable(permable, faction);
			String factionName = faction.getName();
			Mson viewallLink = CmdFactions.get().cmdFactionsPerm.cmdFactionsPermViewall.getTemplateWithArgs(sender, MUtil.list(linkTargetArg, factionName));
			message(viewallLink.command(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermViewall, linkTargetArg, factionName).tooltipParse("<aqua>Click to run command"));
		}
	}
	
}
