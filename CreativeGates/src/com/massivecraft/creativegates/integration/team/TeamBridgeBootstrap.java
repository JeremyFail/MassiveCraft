package com.massivecraft.creativegates.integration.team;

/**
 * Refreshes {@link TeamBridgeRegistry} with priority Factions -> TeamsAPI -> FactionsBridge.
 */
public final class TeamBridgeBootstrap
{
	private TeamBridgeBootstrap()
	{
	}
	
	/**
	 * Recomputes and stores the active team bridge.
	 *
	 * @return Active bridge or null.
	 */
	public static TeamBridge refresh()
	{
		TeamBridge factions = null;
		try
		{
			if (TeamBridgeFactions.get().isAvailable()) factions = TeamBridgeFactions.get();
		}
		catch (Throwable ignored)
		{
			factions = null;
		}
		
		TeamBridge teams = null;
		try
		{
			teams = TeamBridgeTeamsApi.tryCreate();
		}
		catch (Throwable ignored)
		{
			teams = null;
		}
		
		TeamBridge bridge = null;
		try
		{
			bridge = TeamBridgeFactionsBridgePlugin.tryCreate();
		}
		catch (Throwable ignored)
		{
			bridge = null;
		}
		
		return TeamBridgeRegistry.select(factions, teams, bridge);
	}
}
