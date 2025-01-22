package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeRank;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

public class CmdFactionsRankEditPrefix extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsRankEditPrefix()
	{
		// Parameters
		this.addParameter(TypeRank.get(), "rank");
		this.addParameter(TypeString.get(), "new prefix");
		this.addParameter(TypeFaction.get(), "faction", "you");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		String prefix = this.readArgAt(1);
		Faction faction = this.readArgAt(2, msenderFaction);

		// Rank if any passed.
		TypeRank typeRank = new TypeRank(faction);
		Rank rank = typeRank.read(this.argAt(0), sender);

		// Args

		CmdFactionsRankEdit.ensureAllowed(msender, faction, "edit");

		String priorPrefix = rank.getPrefix();
		rank.setPrefix(prefix);

		// Visual
		msg("<i>You changed the prefix of <reset>%s <i>from <h>%s <i>to <h>%s<i>.", rank.getVisual(), priorPrefix, prefix);
	}

}
