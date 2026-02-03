package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeFactionColor;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsPrimaryColorChange;
import com.massivecraft.factions.event.EventFactionsSecondaryColorChange;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.mixin.MixinDisplayName;

/**
 * Command to set a faction's primary or secondary color.
 * Accepts hex color codes in the format #RRGGBB (e.g., #FF0000 for red).
 * Pass null or empty string to reset to default color.
 * Admins can specify a target faction to set its color.
 */
public class CmdFactionsColorSet extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsColorSet()
	{
		// Parameters
		this.addParameter(TypeFactionColor.get(), "primary|secondary").setDesc("which color to set (primary or secondary)");
		this.addParameter(TypeString.get(), "color").setDesc("hex format #RRGGBB, or nothing to reset");
		this.addParameter(null, TypeFaction.get(), "faction", "you").setDesc("the faction to set the color for (defaults to your own)");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void perform() throws MassiveException
	{
		String which = (String) this.readArg();
		String newColor = (String) this.readArg();
		Faction faction = this.readArg(msenderFaction);

		if (!MPerm.getPermColor().has(msender, faction, true)) return;

		if (newColor != null && !newColor.isEmpty())
		{
			if (!newColor.matches("^#[0-9A-Fa-f]{6}$"))
			{
				msg("<b>Invalid color format. Use hex format like <h>#00FF00<b> (#RRGGBB) or use <h>/f color reset %s<b> to reset to default.", which);
				return;
			}
			newColor = newColor.toUpperCase();
		}
		else
		{
			newColor = null;
		}

		boolean primary = "primary".equals(which);
		if (primary)
		{
			EventFactionsPrimaryColorChange event = new EventFactionsPrimaryColorChange(sender, faction, newColor);
			event.run();
			if (event.isCancelled()) return;
			newColor = event.getNewColor();
			faction.setPrimaryColor(newColor);
		}
		else
		{
			EventFactionsSecondaryColorChange event = new EventFactionsSecondaryColorChange(sender, faction, newColor);
			event.run();
			if (event.isCancelled()) return;
			newColor = event.getNewColor();
			faction.setSecondaryColor(newColor);
		}

		String colorDisplay = (newColor == null) ? "default" : newColor;
		String whichDisplay = primary ? "primary" : "secondary";

		msg("<i>You set the %s color for <h>%s<i> to: <h>%s", whichDisplay, faction.getName(msender), colorDisplay);

		for (MPlayer follower : faction.getMPlayers())
		{
			if (follower.equals(msender)) continue;
			follower.msg("<i>%s <i>set your faction %s color to: <h>%s", MixinDisplayName.get().getDisplayName(sender, follower), whichDisplay, colorDisplay);
		}
	}
}
