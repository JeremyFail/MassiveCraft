package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFactionColor;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.util.MUtil;

import java.util.List;

/**
 * Alias for resetting a faction color: runs "f color set primary ''" or
 * "f color set secondary ''" with empty color to reset to default.
 */
public class CmdFactionsColorReset extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsColorReset()
	{
		// Parameters
		this.addParameter(TypeFactionColor.get(), "primary|secondary").setDesc("which color to reset (primary or secondary)");
		this.addParameter(null, TypeFaction.get(), "faction", "you").setDesc("the faction to reset the color for (defaults to your own)");

		// Description
		this.setDesc("reset a faction color to the default color");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void perform() throws MassiveException
	{
		String which = (String) this.readArg();
		Faction faction = this.readArg(msenderFaction);

		// Build args: primary|secondary, empty color to reset, faction id
		List<String> args = MUtil.list(which, "", faction.getId());

		CmdFactionsColor colorParent = (CmdFactionsColor) getParent();
		colorParent.cmdFactionsColorSet.execute(sender, args);
	}

}
