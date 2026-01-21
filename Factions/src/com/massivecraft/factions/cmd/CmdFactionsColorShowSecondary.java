package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.MassiveException;

/**
 * Command to view a faction's secondary color.
 */
public class CmdFactionsColorShowSecondary extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsColorShowSecondary()
	{
		// Aliases
		this.addAliases("s");
		
		// Parameters
		this.addParameter(null, TypeFaction.get(), "faction", "you").setDesc("the faction to view (defaults to your own)");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{	
		// Args
		Faction faction = this.readArg(msenderFaction);
		
		// Get the color
		String secondaryColor = faction.getSecondaryColor();
		boolean isCustom = faction.hasSecondaryColor();
		
		// Display
		if (isCustom)
		{
			msg("<i>The secondary color for <h>%s<i> is: <h>%s", faction.getName(msender), secondaryColor);
		}
		else
		{
			msg("<i>The secondary color for <h>%s<i> is: <h>%s <i>(default)", faction.getName(msender), secondaryColor);
		}
	}
	
}
