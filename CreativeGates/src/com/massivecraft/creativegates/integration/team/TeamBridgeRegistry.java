package com.massivecraft.creativegates.integration.team;

/**
 * Selects the active {@link TeamBridge} with priority:
 * MassiveCraft Factions, then TeamsAPI, then FactionsBridge.
 */
public final class TeamBridgeRegistry
{
	private static TeamBridge active;
	
	private TeamBridgeRegistry()
	{
	}
	
	/**
	 * @return Active bridge, or null if none available.
	 */
	public static TeamBridge getActive()
	{
		return active;
	}
	
	/**
	 * Sets the active bridge (may be null).
	 *
	 * @param bridge Bridge instance or null.
	 */
	public static void setActive(TeamBridge bridge)
	{
		active = bridge;
	}
	
	/**
	 * Picks the highest-priority available candidate and stores it.
	 *
	 * @param factions Factions bridge or null.
	 * @param teamsApi TeamsAPI bridge or null.
	 * @param factionsBridge FactionsBridge plugin bridge or null.
	 * @return Selected bridge or null.
	 */
	public static TeamBridge select(TeamBridge factions, TeamBridge teamsApi, TeamBridge factionsBridge)
	{
		if (factions != null && factions.isAvailable())
		{
			active = factions;
			return active;
		}
		if (teamsApi != null && teamsApi.isAvailable())
		{
			active = teamsApi;
			return active;
		}
		if (factionsBridge != null && factionsBridge.isAvailable())
		{
			active = factionsBridge;
			return active;
		}
		active = null;
		return null;
	}
}
