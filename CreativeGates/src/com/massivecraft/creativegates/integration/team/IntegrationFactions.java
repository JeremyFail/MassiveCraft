package com.massivecraft.creativegates.integration.team;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.Integration;

/**
 * Soft MassiveCraft Factions integration (disband cleanup + bridge priority).
 */
public class IntegrationFactions extends Integration
{
	private static IntegrationFactions i = new IntegrationFactions();
	public static IntegrationFactions get() { return i; }
	private IntegrationFactions()
	{
		this.setPluginName("Factions");
	}
	
	@Override
	public Engine getEngine()
	{
		return EngineFactionsDisband.get();
	}
	
	@Override
	public void setIntegrationActiveInner(boolean active)
	{
		TeamBridgeBootstrap.refresh();
	}
}
