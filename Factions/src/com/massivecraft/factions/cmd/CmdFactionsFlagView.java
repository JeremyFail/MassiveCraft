package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqFlagViewOrShow;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.util.FlagTableUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;

public class CmdFactionsFlagView extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsFlagView()
	{
		this.setAliases("show");
		this.addRequirements(ReqFlagViewOrShow.get());
		this.addParameter(TypeFaction.get(), "faction", "you").setDesc("the faction to view flags for");
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
		FlagTableUtil.displayTable(sender, faction, page, false, this);
	}
	
}
