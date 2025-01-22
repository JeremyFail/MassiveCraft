package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeVote;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.Vote;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

public class CmdFactionsVoteDo extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsVoteDo()
	{
		// Parameters
		this.addParameter(TypeVote.get());
		this.addParameter(TypeString.get(), "option");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Vote vote = TypeVote.get(msenderFaction).read(this.arg(), sender);
		String option = this.readArg();
		
		// Any and MPerm
		if ( ! MPerm.getPermVote().has(msender, msenderFaction, true)) return;

		if (vote.getOptions().stream().noneMatch(option::equalsIgnoreCase))
		{
			throw new MassiveException().addMsg("<b>No option in <h>%s <b>matches <h>%s<b>.", vote.getName(), option);
		}

		vote.setVote(msender, option);

		msg("<i>Succesfully voted for <h>%s <i>in <h>%s<i>.", option, vote.getName());
	}
	
}
