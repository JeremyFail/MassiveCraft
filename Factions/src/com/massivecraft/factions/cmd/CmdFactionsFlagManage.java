package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.util.FlagTableUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;

/**
 * Command to open the flag management table for a faction.
 * Displays flags with YES/NOO cells; click to toggle (when editable).
 */
public class CmdFactionsFlagManage extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsFlagManage()
	{
		this.addRequirements(RequirementHasPerm.get(Perm.FLAG_MANAGE));
		this.addParameter(TypeFaction.get(), "faction", "you").setDesc("the faction whose flags to manage");
		this.addParameter(Parameter.getPage());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void perform() throws MassiveException
	{
		final Faction faction = this.readArg(msenderFaction);
		final int page = this.readArg();
		FlagTableUtil.displayTable(sender, faction, page, true, this);
	}
}
