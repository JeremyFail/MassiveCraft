package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqFactionWarpsEnabled;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.command.Visibility;

public class FactionsCommandWarp extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public FactionsCommandWarp()
	{
		this.addRequirements(ReqFactionWarpsEnabled.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Visibility getVisibility()
	{
		return MConf.get().warpsEnabled ? super.getVisibility() : Visibility.INVISIBLE;
	}
	
}
