package com.massivecraft.factions.integration.map.bluemap;

import com.massivecraft.factions.integration.map.IntegrationMap;
import com.massivecraft.massivecore.Engine;

/**
 * Integration layer between Factions and BlueMap.
 *
 * <p>
 * This class serves as the activation point for the BlueMap integration, checking if
 * BlueMap is installed and starting {@link EngineBlueMap} if so. Layer and area constants
 * are defined in {@link IntegrationMap}.
 * </p>
 *
 * <p>
 * Constants include marker set IDs (inherited as {@link IntegrationMap#FACTIONS_LAYER_TERRITORY},
 * etc.) and area marker ID prefix.
 * </p>
 */
public class IntegrationBlueMap extends IntegrationMap
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	private static IntegrationBlueMap i = new IntegrationBlueMap();
	public static IntegrationBlueMap get() { return i; }

	private IntegrationBlueMap()
	{
		this.setPluginName("BlueMap");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Engine getEngine()
	{
		return EngineBlueMap.get();
	}
}
