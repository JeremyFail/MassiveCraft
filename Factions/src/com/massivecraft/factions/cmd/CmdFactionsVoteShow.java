package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeVote;
import com.massivecraft.factions.entity.Vote;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsVoteShow extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsVoteShow()
	{
		// Parameters
		this.addParameter(TypeVote.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Vote vote = TypeVote.get(msenderFaction).read(this.arg(), sender);

		// Clean the vote
		vote.clean();

		// Message
		message(Txt.titleize("Votes for " + vote.getName()));

		int numberOfVotes = vote.getId2Vote().size();
		for (String option : vote.getOptions())
		{
			long num = vote.getId2Vote().values().stream()
						   .filter(option::equalsIgnoreCase).count();

			double percent = ((double) num / (double) numberOfVotes) * 100;
			if (Double.isInfinite(percent)) percent = 0D;
			String str = Txt.parse("<lime>%s<i>: <i>%d/%d (<h>%.2f%%<i>)", option, num, numberOfVotes, percent);
			message(str);
		}
	}
	
}
