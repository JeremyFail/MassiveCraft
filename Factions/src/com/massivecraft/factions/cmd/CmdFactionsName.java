package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeFactionNameLenient;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.event.EventFactionsNameChange;
import com.massivecraft.massivecore.MassiveException;

public class CmdFactionsName extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsName()
	{
		// Parameters
		this.addParameter(TypeFactionNameLenient.get(), "new name").setDesc("the new name of the faction");
		this.addParameter(TypeFaction.get(), "faction", "you").setDesc("the faction whose name to change");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		String newName = this.readArg();
		Faction faction = this.readArg(msenderFaction);
		
		// MPerm
		if ( ! MPerm.getPermName().has(msender, faction, true)) return;
		
		// Event
		EventFactionsNameChange event = new EventFactionsNameChange(sender, faction, newName);
		event.run();
		if (event.isCancelled()) return;
		newName = event.getNewName();

		// Apply
		faction.setName(newName);

		// Inform
		faction.msg("%s<i> changed your faction name to %s", msender.describeTo(faction, true), faction.getName(faction));
		if (msenderFaction != faction)
		{
			msg("<i>You changed the faction name to %s", faction.getName(msender));
		}
	}
	
}
