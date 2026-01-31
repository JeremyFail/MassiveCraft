package com.massivecraft.factions.integration.squaremap;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.Integration;

/**
 * Integration layer between Factions and SquareMap.
 *
 * <p>
 * This class serves as the activation point for the SquareMap integration, checking if
 * SquareMap is installed and starting {@link EngineSquareMap} if so. It also defines constants
 * used throughout the SquareMap integration that must be accessible even when SquareMap is not loaded.
 * </p>
 *
 * <p>
 * Plugin name is lowercase "squaremap" to match the actual plugin.
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
public class IntegrationSquareMap extends Integration
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
