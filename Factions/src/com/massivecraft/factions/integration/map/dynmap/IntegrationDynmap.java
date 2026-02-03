package com.massivecraft.factions.integration.map.dynmap;

import com.massivecraft.factions.integration.map.IntegrationMap;
import com.massivecraft.massivecore.Engine;

/**
 * Integration layer between Factions and Dynmap.
 *
 * <p>
 * This class serves as the activation point for the Dynmap integration, checking if
 * Dynmap is installed and starting {@link EngineDynmap} if so. Layer and area constants
 * are defined in {@link IntegrationMap}. Dynmap-specific constants (blocks per chunk,
 * home marker icon) remain here.
 * </p>
 *
 * <p>
 * Style defaults (colors, opacity, weight) are in
 * {@link com.massivecraft.factions.integration.map.MapStyleDefaults}.
 * </p>
 */
public class IntegrationDynmap extends IntegrationMap
{
	// -------------------------------------------- //
	// DYNMAP-SPECIFIC CONSTANTS
	// -------------------------------------------- //

	// MConf relies on DynmapStyle which relies on these constants;
	// we must be able to load MConf without EngineDynmap.
	public static final int BLOCKS_PER_CHUNK = 16;

	/** Marker set ID used by Dynmap API (same value as {@link #FACTIONS_LAYER_TERRITORY} etc.). */
	public static final String FACTIONS_MARKERSET = FACTIONS_ + "markerset";

	/**
	 * Dynmap icon ID for home markers. Style defaults (colors, opacity, weight, boost) are in
	 * {@link com.massivecraft.factions.integration.map.MapStyleDefaults}.
	 */
	public static final String DYNMAP_STYLE_HOME_MARKER = "greenflag";

	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static IntegrationDynmap i = new IntegrationDynmap();
	public static IntegrationDynmap get() { return i; }

	private IntegrationDynmap()
	{
		this.setPluginName("dynmap");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Engine getEngine()
	{
		return EngineDynmap.get();
	}
}
