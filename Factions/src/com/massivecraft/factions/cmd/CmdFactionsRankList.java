package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;

import java.util.Comparator;
import java.util.List;

public class CmdFactionsRankList extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsRankList()
	{
		// Parameters
		this.addParameter(Parameter.getPage());
		this.addParameter(TypeFaction.get(), "faction", "you");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		final int page = this.readArg();
		Faction faction = this.readArg(msenderFaction);

		List<Rank> ranks = faction.getRanks().getAll(Comparator.comparingInt(Rank::getPriority).reversed());

		String title = "Rank list for " + faction.describeTo(msender);
		Pager<Rank> pager = new Pager<>(this, title, page, ranks, (Stringifier<Rank>) (r, i) -> r.getVisual());
		pager.message();
	}
	
}
