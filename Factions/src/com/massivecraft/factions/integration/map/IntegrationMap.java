package com.massivecraft.factions.integration.map;

import com.massivecraft.massivecore.Integration;

/**
 * Base integration for map plugins (Pl3xMap, SquareMap, Dynmap, BlueMap).
 *
 * <p>
 * Holds shared layer/marker constants so all map integrations use the same keys.
 * Subclasses set the plugin name and return their engine.
 * </p>
 *
 * <p>
 * Constants:
 * <ul>
 * <li>{@link #FACTIONS}, {@link #FACTIONS_} – prefix</li>
 * <li>{@link #FACTIONS_LAYER_TERRITORY}, {@link #FACTIONS_LAYER_HOME}, {@link #FACTIONS_LAYER_WARPS} – layer/marker set keys</li>
 * <li>{@link #FACTIONS_AREA}, {@link #FACTIONS_AREA_} – territory area marker ID prefix</li>
 * </ul>
 * </p>
 */
public abstract class IntegrationMap extends Integration
{
	// -------------------------------------------- //
	// SHARED CONSTANTS (same values for all map integrations)
	// -------------------------------------------- //

	public static final String FACTIONS = "factions";
	public static final String FACTIONS_ = FACTIONS + "_";

	/** Layer/marker set key for territory (claimed chunks). */
	public static final String FACTIONS_LAYER_TERRITORY = FACTIONS_ + "territory";
	/** Layer/marker set key for faction home warps. */
	public static final String FACTIONS_LAYER_HOME = FACTIONS_ + "home";
	/** Layer/marker set key for other faction warps. */
	public static final String FACTIONS_LAYER_WARPS = FACTIONS_ + "warps";

	public static final String FACTIONS_AREA = FACTIONS_ + "area";
	public static final String FACTIONS_AREA_ = FACTIONS_AREA + "_";
}
