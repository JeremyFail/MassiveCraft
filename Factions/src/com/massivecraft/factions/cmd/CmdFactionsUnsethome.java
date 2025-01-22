package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Visibility;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.util.MUtil;

import java.util.List;

public class CmdFactionsUnsethome extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsUnsethome()
	{
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());

		// Parameters
		this.addParameter(TypeFaction.get(), "faction", "you");

		// Visibility
		this.setVisibility(Visibility.INVISIBLE);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		List<String> args = MUtil.list(MConf.get().warpsHomeName, this.argAt(0));
		CmdFactions.get().cmdFactionsWarp.cmdFactionWarpRemove.execute(me, args);
	}
	
}
