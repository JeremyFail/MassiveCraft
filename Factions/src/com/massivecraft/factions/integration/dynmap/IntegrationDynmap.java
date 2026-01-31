package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.Integration;

/**
 * Integration layer between Factions and Dynmap.
 * 
 * <p>
 * This class serves as the activation point for the Dynmap integration, checking if
 * Dynmap is installed and starting the EngineDynmap if so. It also defines constants
 * used throughout the Dynmap integration that must be accessible even when Dynmap is not loaded.
 * </p>
 * 
 * <p>
 * Constants include marker set IDs, area prefixes, Dynmap-specific defaults (e.g. home marker icon),
 * and blocks per chunk. Generic map style defaults (colors, opacity, weight) are in
 * {@link com.massivecraft.factions.integration.map.MapStyleDefaults}.
 * </p>
 */
public class IntegrationDynmap extends Integration
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //

	// Constants must be here rather than in EngineDynmap.
	// MConf relies on DynmapStyle which relies on these constants
	// and we must be able to load MConf without EngineDynmap.
	public final static int BLOCKS_PER_CHUNK = 16;

	public final static String FACTIONS = "factions";
	public final static String FACTIONS_ = FACTIONS + "_";

	// Marker set IDs for different layer types
	public final static String FACTIONS_MARKERSET = FACTIONS_ + "markerset";
	public final static String FACTIONS_MARKERSET_TERRITORY = FACTIONS_ + "territory";
	public final static String FACTIONS_MARKERSET_HOME = FACTIONS_ + "home";
	public final static String FACTIONS_MARKERSET_WARPS = FACTIONS_ + "warps";

	public final static String FACTIONS_AREA = FACTIONS_ + "area";
	public final static String FACTIONS_AREA_ = FACTIONS_AREA + "_";

	/** 
	 * Dynmap icon ID for home markers. 
	 * Style defaults (colors, opacity, weight, boost) are in 
	 * {@link com.massivecraft.factions.integration.map.MapStyleDefaults}. 
	 **/
	public final static String DYNMAP_STYLE_HOME_MARKER = "greenflag";

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
