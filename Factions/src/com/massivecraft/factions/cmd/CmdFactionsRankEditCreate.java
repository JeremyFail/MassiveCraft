package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeInteger;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.command.type.primitive.TypeStringParsed;

import java.util.Collection;

public class CmdFactionsRankEditCreate extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsRankEditCreate()
	{
		// Parameters
		this.addParameter(TypeString.get(), "name");
		this.addParameter(TypeInteger.get(), "priority");
		this.addParameter("", TypeStringParsed.get(), "prefix", "none");
		this.addParameter(TypeFaction.get(), "faction", "you");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		String name = this.readArg();
		Integer priority = this.readArg();
		String prefix = this.readArg();
		Faction faction = this.readArg(msenderFaction);

		CmdFactionsRankEdit.ensureAllowed(msender, faction, "create");

		Collection<Rank> ranks = faction.getRanks().getAll();

		if (ranks.stream().map(Rank::getName).anyMatch(s -> s.equalsIgnoreCase(name)))
		{
			throw new MassiveException().addMsg("<b>There is already a rank called <h>%s<b>.", name);
		}
		if (ranks.stream().map(Rank::getPriority).anyMatch(i -> i.equals(priority)))
		{
			throw new MassiveException().addMsg("<b>There is already a rank with priority <h>%s<b>.", priority);
		}
		if (priority > faction.getLeaderRank().getPriority())
		{
			throw new MassiveException().addMsg("<b>You can't create a rank of higher priority than the leader rank.");
		}

		Rank rank = new Rank(name, priority, prefix);

		faction.getRanks().attach(rank);

		// Inform
		msg("<i>You created the rank <reset>%s<i>.", rank.getVisual());
		msg("<i>You might want to change its permissions:");
		CmdFactions.get().cmdFactionsPerm.getTemplate(false, true, sender).messageOne(msender);
	}

}
