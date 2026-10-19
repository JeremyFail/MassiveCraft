package com.massivecraft.creativegates.cmd.requirement;

import com.massivecraft.creativegates.integration.team.TeamBridge;
import com.massivecraft.creativegates.integration.team.TeamBridgeRegistry;
import com.massivecraft.creativegates.util.FactionTerm;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementAbstract;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.command.CommandSender;

/**
 * Requires an active Factions / TeamsAPI / FactionsBridge integration.
 */
public class RequirementTeamBridge extends RequirementAbstract
{
	private static final long serialVersionUID = 1L;
	
	private static final RequirementTeamBridge i = new RequirementTeamBridge();
	public static RequirementTeamBridge get() { return i; }
	
	@Override
	public boolean apply(CommandSender sender, MassiveCommand command)
	{
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		return bridge != null && bridge.isAvailable();
	}
	
	@Override
	public String createErrorMessage(CommandSender sender, MassiveCommand command)
	{
		return Txt.parse("<b>%s integration is not available.", FactionTerm.capital());
	}
}
