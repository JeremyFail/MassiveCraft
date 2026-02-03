package com.massivecraft.factions.integration.map.pl3xmap;

import com.massivecraft.factions.integration.map.IntegrationMap;
import com.massivecraft.massivecore.Engine;

/**
 * Integration layer between Factions and Pl3xMap.
 *
 * <p>
 * This class serves as the activation point for the Pl3xMap integration, checking if
 * Pl3xMap is installed and starting {@link EnginePl3xMap} if so. Layer and area constants
 * are defined in {@link IntegrationMap}.
 * </p>
 *
 * <p>
 * Plugin name is "Pl3xMap" to match the actual plugin.
 * </p>
 */
public class IntegrationPl3xMap extends IntegrationMap
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static IntegrationPl3xMap i = new IntegrationPl3xMap();
	public static IntegrationPl3xMap get() { return i; }

	private IntegrationPl3xMap()
	{
		this.setPluginName("Pl3xMap");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Engine getEngine()
	{
		return EnginePl3xMap.get();
	}
}
