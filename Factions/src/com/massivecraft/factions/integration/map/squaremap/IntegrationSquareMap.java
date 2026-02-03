package com.massivecraft.factions.integration.map.squaremap;

import com.massivecraft.factions.integration.map.IntegrationMap;
import com.massivecraft.massivecore.Engine;

/**
 * Integration layer between Factions and SquareMap.
 *
 * <p>
 * This class serves as the activation point for the SquareMap integration, checking if
 * SquareMap is installed and starting {@link EngineSquareMap} if so. Layer and area constants
 * are defined in {@link IntegrationMap}.
 * </p>
 *
 * <p>
 * Plugin name is lowercase "squaremap" to match the actual plugin.
 * </p>
 */
public class IntegrationSquareMap extends IntegrationMap
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static IntegrationSquareMap i = new IntegrationSquareMap();
	public static IntegrationSquareMap get() { return i; }

	private IntegrationSquareMap()
	{
		this.setPluginName("squaremap");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Engine getEngine()
	{
		return EngineSquareMap.get();
	}
}
