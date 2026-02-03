package com.massivecraft.factions.integration.map;

/**
 * Default constant values for {@link MapStyle}.
 *
 * <p>
 * Kept in a separate class so that Gson never reflects on them when deserializing
 * {@link MapStyle} (e.g. from {@link com.massivecraft.factions.entity.MConf}).
 * MapStyle instance fields are the only ones serialized; these defaults are referenced
 * by MapStyle getters and by utilities that need fallback values.
 * </p>
 */
public final class MapStyleDefaults
{
	private MapStyleDefaults() {}

	public static final String DEFAULT_LINE_COLOR = "#00FF00";
	public static final double DEFAULT_LINE_OPACITY = 0.8D;
	public static final int DEFAULT_LINE_WEIGHT = 3;
	public static final String DEFAULT_FILL_COLOR = "#00FF00";
	public static final double DEFAULT_FILL_OPACITY = 0.35D;
	public static final boolean DEFAULT_BOOST = false;
}
