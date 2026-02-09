package com.massivecraft.factions.integration.placeholderapi;

import com.massivecraft.massivecore.Integration;

public class IntegrationPlaceholderAPI extends Integration
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static IntegrationPlaceholderAPI i = new IntegrationPlaceholderAPI();
	public static IntegrationPlaceholderAPI get() { return i; }
	private IntegrationPlaceholderAPI()
	{
		this.setPluginName("PlaceholderAPI");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void setIntegrationActiveInner(boolean active)
	{
		if (active) PlaceholderFactions.get().register();
		else PlaceholderFactions.get().unregister();
	}

	public static void ensureRegistered()
	{
		if (PlaceholderFactions.get().isRegistered()) return;
		PlaceholderFactions.get().register();
	}
}
