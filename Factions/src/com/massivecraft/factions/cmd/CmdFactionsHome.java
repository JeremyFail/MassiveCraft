package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.util.MUtil;

import java.util.List;

public class CmdFactionsHome extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	// Alias for the new format of using warps "/f warp go home" command.
	public CmdFactionsHome()
	{
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());

		// Parameters
		this.addParameter(TypeFaction.get(), "faction", "you");

		// Description
		this.setDesc("alias to teleport to the faction home warp");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		List<String> args = MUtil.list(MConf.get().warpsHomeName, this.argAt(0));
		CmdFactions.get().cmdFactionsWarp.cmdFactionsWarpGo.execute(me, args);
	}
	
}
