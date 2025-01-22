package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeVote;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.Vote;
import com.massivecraft.factions.event.EventFactionsVoteRemove;
import com.massivecraft.massivecore.MassiveException;

public class CmdFactionsVoteRemove extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsVoteRemove()
	{
		// Parameters
		this.addParameter(TypeVote.get(), "vote");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Vote vote = TypeVote.get(msenderFaction).read(this.arg(), sender);
		
		// Any and MPerm
		if ( ! MPerm.getPermCreateVote().has(msender, msenderFaction, true)) return;
		
		// Event
		EventFactionsVoteRemove event = new EventFactionsVoteRemove(sender, msenderFaction, vote);
		event.run();
		if (event.isCancelled()) return;
		vote = event.getVote();

		// Apply
		vote.detach();
		
		// Inform
		msender.msg("%s<i> removed the vote <h>%s <i>from your faction.", msender.describeTo(msenderFaction, true), vote.getName());
	}
	
}
