package com.massivecraft.factions.cmd.req;

import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementAbstract;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.command.CommandSender;

public class ReqFactionWarpsEnabled extends RequirementAbstract
{
	// -------------------------------------------- //
	// SERIALIZABLE
	// -------------------------------------------- //
	
	private static final long serialVersionUID = 1L;
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static ReqFactionWarpsEnabled i = new ReqFactionWarpsEnabled();
	public static ReqFactionWarpsEnabled get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public boolean apply(CommandSender sender, MassiveCommand command)
	{
		return MConf.get().warpsEnabled;
	}
	
	@Override
	public String createErrorMessage(CommandSender sender, MassiveCommand command)
	{
		return Txt.parse("<b>Warps must be enabled on the server to %s.", getDesc(command));
	}
	
}
