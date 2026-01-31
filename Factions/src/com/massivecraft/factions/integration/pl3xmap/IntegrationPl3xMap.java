package com.massivecraft.factions.integration.pl3xmap;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.Integration;

/**
 * Integration layer between Factions and Pl3xMap.
 *
 * <p>
 * This class serves as the activation point for the Pl3xMap integration, checking if
 * Pl3xMap is installed and starting {@link EnginePl3xMap} if so. It also defines constants
 * used throughout the Pl3xMap integration that must be accessible even when Pl3xMap is not loaded.
 * </p>
 *
 * <p>
 * Plugin name is "Pl3xMap" to match the actual plugin.
 * </p>
 *
 * <p>
 * Constants include:
 * <ul>
 * <li>Layer keys for territory, home warps, and other warps</li>
 * <li>Area marker ID prefix for territory polygons</li>
 * </ul>
 * </p>
 */
public class IntegrationPl3xMap extends Integration
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //

	public static final String FACTIONS = "factions";
	public static final String FACTIONS_ = FACTIONS + "_";

	/** Layer keys for different layer types. */
	public static final String FACTIONS_LAYER_TERRITORY = FACTIONS_ + "territory";
	public static final String FACTIONS_LAYER_HOME = FACTIONS_ + "home";
	public static final String FACTIONS_LAYER_WARPS = FACTIONS_ + "warps";

	public static final String FACTIONS_AREA = FACTIONS_ + "area";
	public static final String FACTIONS_AREA_ = FACTIONS_AREA + "_";

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
