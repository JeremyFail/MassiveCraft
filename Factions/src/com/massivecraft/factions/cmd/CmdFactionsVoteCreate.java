package com.massivecraft.factions.cmd;

import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.Vote;
import com.massivecraft.factions.event.EventFactionsVoteAdd;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.type.container.TypeList;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.util.IdUtil;

import java.util.List;

public class CmdFactionsVoteCreate extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsVoteCreate()
	{
		// Parameters
		this.addParameter(TypeString.get(), "name");
		this.addParameter(TypeList.get(TypeString.get()), "options", true);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		String name = this.readArg();
		List<String> options = this.readArg();
		
		// MPerm
		if ( ! MPerm.getPermCreateVote().has(msender, msenderFaction, true)) return;

		if (msenderFaction.getVoteByName(name).isPresent())
		{
			throw new MassiveException().addMsg("<b>There is already a vote called <h>%s<b>.", name);
		}

		// Make sure there are no duplicated
		List<String> options2 = new MassiveList<>();
		for (String str : options)
		{
			if (options2.stream().anyMatch(str::equalsIgnoreCase)) continue;
			options2.add(str);
		}


		Vote vote = new Vote(IdUtil.getId(sender), name, options2);
		
		// Event
		EventFactionsVoteAdd event = new EventFactionsVoteAdd(sender, msenderFaction, vote);
		event.run();
		if (event.isCancelled()) return;
		vote = event.getVote();

		// Apply
		msenderFaction.addVote(vote);
		
		// Inform
		msenderFaction.msg("%s<i> added the vote <h>%s <i>to your faction. You can now use:", msender.describeTo(msenderFaction, true), vote.getName());
		msenderFaction.sendMessage(CmdFactions.get().cmdFactionsVote.cmdFactionsVoteDo.getTemplateWithArgs(null, vote.getName()));
	}
	
}
