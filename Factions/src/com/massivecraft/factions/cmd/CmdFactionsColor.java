package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsColorChange;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.mixin.MixinDisplayName;

/**
 * Command to set a faction's custom color.
 * Accepts hex color codes in the format #RRGGBB (e.g., #FF0000 for red).
 * 
 * <p>
 * Pass null or empty string to reset to default color.
 * Admins can specify a target faction to set its color.
 */
public class CmdFactionsColor extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsColor()
	{
		// Parameters
		this.addParameter(TypeString.get(), "color").setDesc("the faction color (hex format #RRGGBB, or nothing to reset)");
		this.addParameter(null, TypeFaction.get(), "faction", "you").setDesc("the faction to set the color for (defaults to your own)");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{	
		// Args
		String newColor = this.readArg();
		Faction faction = this.readArg(msenderFaction);
		
		// MPerm
		if (!MPerm.getPermColor().has(msender, faction, true)) return;
		
		// Validate color format if not null/empty
		if (newColor != null && !newColor.isEmpty())
		{
			// Check hex format #RRGGBB
			if (!newColor.matches("^#[0-9A-Fa-f]{6}$"))
			{
				msg("<b>Invalid color format. Use hex format like <h>#FF0000<b> or leave empty to reset.");
				return;
			}
			// Normalize to uppercase
			newColor = newColor.toUpperCase();
		}
		else
		{
			// Empty string means reset to null
			newColor = null;
		}
		
		// Event
		EventFactionsColorChange event = new EventFactionsColorChange(sender, faction, newColor);
		event.run();
		if (event.isCancelled()) return;
		newColor = event.getNewColor();

		// Apply
		faction.setColor(newColor);
		
		// Inform
		String colorDisplay = (newColor == null) ? "default" : newColor;
		
		// Inform sender
		msg("<i>You set the color for <h>%s<i> to: <h>%s", faction.getName(msender), colorDisplay);
		
		// Inform other faction members
		for (MPlayer follower : faction.getMPlayers())
		{
			if (follower.equals(msender)) continue;
			follower.msg("<i>%s <i>set your faction color to: %s", MixinDisplayName.get().getDisplayName(sender, follower), colorDisplay);
		}
	}
	
}
