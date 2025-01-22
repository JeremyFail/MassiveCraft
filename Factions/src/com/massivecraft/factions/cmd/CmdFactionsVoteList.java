package com.massivecraft.factions.cmd;

import com.massivecraft.factions.entity.Vote;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsVoteList extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsVoteList()
	{
		// Parameters
		this.addParameter(Parameter.getPage());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		int idx = this.readArg();

		Pager<Vote> pager = new Pager<>(this, "Faction Votes", idx, msenderFaction.getVotes().getAll());
		pager.setMsonifier((Stringifier<Vote>)  (vote, i) ->
		{
			String options = Txt.implodeCommaAndDot(vote.getOptions(), Txt.parse("<teal>%s"), Txt.parse("<i>, "), Txt.parse(" <i>and "), "");
			return Txt.parse("<lime>%s<i>: %s", vote.getName(), options);
		});

		pager.message();
	}
	
}
