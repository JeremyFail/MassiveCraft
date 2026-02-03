package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeFactionColor;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.MassiveException;

/**
 * Command to view a faction's primary or secondary color.
 */
public class CmdFactionsColorShow extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsColorShow()
	{
		// Parameters
		this.addParameter(TypeFactionColor.get(), "primary|secondary").setDesc("which color to show (primary or secondary)");
		this.addParameter(null, TypeFaction.get(), "faction", "you").setDesc("the faction to view (defaults to your own)");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void perform() throws MassiveException
	{
		String which = (String) this.readArg();
		Faction faction = this.readArg(msenderFaction);

		boolean primary = "primary".equals(which);
		String color;
		boolean isCustom;
		String whichDisplay;

		if (primary)
		{
			color = faction.getPrimaryColor();
			isCustom = faction.hasPrimaryColor();
			whichDisplay = "primary";
		}
		else
		{
			color = faction.getSecondaryColor();
			isCustom = faction.hasSecondaryColor();
			whichDisplay = "secondary";
		}

		if (isCustom)
		{
			msg("<i>The %s color for <h>%s<i> is: <h>%s", whichDisplay, faction.getName(msender), color);
		}
		else
		{
			msg("<i>The %s color for <h>%s<i> is: <h>%s <i>(default)", whichDisplay, faction.getName(msender), color);
		}
	}
}
