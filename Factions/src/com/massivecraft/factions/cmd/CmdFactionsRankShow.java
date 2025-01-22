package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeMPlayer;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsRankShow extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsRankShow()
	{
		// Parameters
		this.addParameter(TypeMPlayer.get(), "player");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		MPlayer target = this.readArg();
		Rank rank = target.getRank();

		// Damn you grammar, causing all these checks.
		String targetName = target.describeTo(msender, true);
		String isAre = (target == msender) ? "are" : "is"; // "you are" or "he is"

		String theAan = (rank.isLeader()) ? "the" : Txt.aan(rank.getName()); // "a member", "an officer" or "the leader"
		String rankName = rank.getName().toLowerCase();
		String ofIn = (rank.isLeader()) ? "of" : "in"; // "member in" or "leader of"
		String factionName = target.getFaction().describeTo(msender, true);
		if (target.getFaction() == msenderFaction)
		{
			// Having the "Y" in "Your faction" being uppercase in the middle of a sentence makes no sense.
			factionName = factionName.toLowerCase();
		}
		if (target.getFaction().isNone())
		{
			// Wilderness aka none doesn't use ranks
			msg("%s <i>%s factionless", targetName, isAre);
		}
		else
		{
			// Derp	is a member in Faction
			msg("%s <i>%s %s <h>%s <i>%s %s<i>.", targetName, isAre, theAan, rankName, ofIn, factionName);
		}
	}
	
}
