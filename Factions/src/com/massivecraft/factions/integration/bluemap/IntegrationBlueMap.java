package com.massivecraft.factions.integration.bluemap;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.Integration;

/**
 * Integration layer between Factions and BlueMap.
 *
 * <p>
 * This class serves as the activation point for the BlueMap integration, checking if
 * BlueMap is installed and starting {@link EngineBlueMap} if so. It also defines constants
 * used throughout the BlueMap integration that must be accessible even when BlueMap is not loaded.
 * </p>
 *
 * <p>
 * Constants include:
 * <ul>
 * <li>Marker set IDs for territory, home warps, and other warps layers</li>
 * <li>Area marker ID prefix for territory polygons</li>
 * </ul>
 * </p>
 */
public class IntegrationBlueMap extends Integration
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //

	public static final String FACTIONS = "factions";
	public static final String FACTIONS_ = FACTIONS + "_";

	/** Marker set IDs for different layer types. */
	public static final String FACTIONS_MARKERSET_TERRITORY = FACTIONS_ + "territory";
	public static final String FACTIONS_MARKERSET_HOME = FACTIONS_ + "home";
	public static final String FACTIONS_MARKERSET_WARPS = FACTIONS_ + "warps";

	public static final String FACTIONS_AREA = FACTIONS_ + "area";
	public static final String FACTIONS_AREA_ = FACTIONS_AREA + "_";

	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static IntegrationBlueMap i = new IntegrationBlueMap();
	public static IntegrationBlueMap get() { return i; }

	private IntegrationBlueMap()
	{
		this.setPluginName("bluemap");
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
