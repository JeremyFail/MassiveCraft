package com.massivecraft.creativegates.integration.team;

import com.massivecraft.massivecore.Integration;

/**
 * Soft FactionsBridge plugin integration for FACTION scope as lowest priority.
 */
public class IntegrationFactionsBridge extends Integration
{
	private static IntegrationFactionsBridge i = new IntegrationFactionsBridge();
	public static IntegrationFactionsBridge get() { return i; }
	private IntegrationFactionsBridge()
	{
		this.setPluginName("FactionsBridge");
	}
	
	@Override
	public void setIntegrationActiveInner(boolean active)
	{
		TeamBridgeBootstrap.refresh();
	}
}
