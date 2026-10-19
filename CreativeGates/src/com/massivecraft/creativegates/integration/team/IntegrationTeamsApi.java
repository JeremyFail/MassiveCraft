package com.massivecraft.creativegates.integration.team;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.Integration;

/**
 * Soft TeamsAPI integration for FACTION scope when MassiveCraft Factions is absent.
 */
public class IntegrationTeamsApi extends Integration
{
	private static IntegrationTeamsApi i = new IntegrationTeamsApi();
	public static IntegrationTeamsApi get() { return i; }
	private IntegrationTeamsApi()
	{
		this.setPluginName("TeamsAPI");
	}
	
	@Override
	public Engine getEngine()
	{
		return EngineTeamsApiDisband.get();
	}
	
	@Override
	public void setIntegrationActiveInner(boolean active)
	{
		TeamBridgeBootstrap.refresh();
	}
}
