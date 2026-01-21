package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.MassiveException;

/**
 * Command to view a faction's primary color.
 */
public class CmdFactionsColorShowPrimary extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsColorShowPrimary()
	{
		// Aliases
		this.addAliases("p");
		
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
		String primaryColor = faction.getPrimaryColor();
		boolean isCustom = faction.hasPrimaryColor();
		
		// Display
		if (isCustom)
		{
			msg("<i>The primary color for <h>%s<i> is: <h>%s", faction.getName(msender), primaryColor);
		}
		else
		{
			msg("<i>The primary color for <h>%s<i> is: <h>%s <i>(default)", faction.getName(msender), primaryColor);
		}
	}
	
}
