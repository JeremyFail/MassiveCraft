package com.massivecraft.factions.integration.map;

import com.massivecraft.factions.entity.MConf;

/**
 * Shared styling configuration for faction territory on map plugins (line/fill color, opacity, weight, etc.).
 *
 * <p>
 * This class uses a coalesce pattern where null values fall back to:
 * <ol>
 * <li>The default style defined in {@link MConf#mapDefaultStyle}</li>
 * <li>Hard-coded constants in {@link MapStyleDefaults}</li>
 * </ol>
 * This allows server administrators to define a global default style, override styles
 * per-faction via {@link MConf#mapFactionStyleOverrides}, and leave properties null to inherit.
 * </p>
 *
 * <p>
 * Used by Dynmap, BlueMap and other map integrations. Instances are immutable; use the
 * {@code withXXX()} methods to create modified copies. Default constants are in {@link MapStyleDefaults} (separate class to avoid Gson reflecting on them when deserializing MapStyle).
 * integration-specific defaults (e.g. Dynmap icon names) remain in the integration.
 * </p>
 */
public class MapStyle
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	public final String lineColor;
	public String getLineColor() { return coalesce(this.lineColor, MapUtil.getDefaultLineColor()); }
	public MapStyle withLineColor(String lineColor) { return new MapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	public final Double lineOpacity;
	public double getLineOpacity() { return coalesce(this.lineOpacity, MConf.get().mapDefaultStyle != null ? MConf.get().mapDefaultStyle.lineOpacity : null, MapStyleDefaults.DEFAULT_LINE_OPACITY); }
	public MapStyle withLineOpacity(Double lineOpacity) { return new MapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	public final Integer lineWeight;
	public int getLineWeight() { return coalesce(this.lineWeight, MConf.get().mapDefaultStyle != null ? MConf.get().mapDefaultStyle.lineWeight : null, MapStyleDefaults.DEFAULT_LINE_WEIGHT); }
	public MapStyle withLineWeight(Integer lineWeight) { return new MapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	public final String fillColor;
	public String getFillColor() { return coalesce(this.fillColor, MapUtil.getDefaultFillColor()); }
	public MapStyle withFillColor(String fillColor) { return new MapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	public final Double fillOpacity;
	public double getFillOpacity() { return coalesce(this.fillOpacity, MConf.get().mapDefaultStyle != null ? MConf.get().mapDefaultStyle.fillOpacity : null, MapStyleDefaults.DEFAULT_FILL_OPACITY); }
	public MapStyle withFillOpacity(Double fillOpacity) { return new MapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	public final Boolean boost;
	public boolean getBoost() { return coalesce(this.boost, MConf.get().mapDefaultStyle != null ? MConf.get().mapDefaultStyle.boost : null, MapStyleDefaults.DEFAULT_BOOST); }
	public MapStyle withBoost(Boolean boost) { return new MapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //

	/** Creates a new style with all values null (resolved from config/defaults via getters). */
	public MapStyle()
	{
		this(null, null, null, null, null, null);
	}

	/**
	 * Creates a new style with the given values. Null values are resolved from
	 * {@link MConf#mapDefaultStyle} or constants via the getters.
	 *
	 * @param lineColor   Hex line color (e.g. "#00FF00"), or null to use default
	 * @param lineOpacity Line opacity 0.0–1.0, or null
	 * @param lineWeight  Line weight in pixels, or null
	 * @param fillColor   Hex fill color, or null to use default
	 * @param fillOpacity Fill opacity 0.0–1.0, or null
	 * @param boost       Whether to boost rendering, or null
	 */
	public MapStyle(String lineColor, Double lineOpacity, Integer lineWeight, String fillColor, Double fillOpacity, Boolean boost)
	{
		this.lineColor = lineColor;
		this.lineOpacity = lineOpacity;
		this.lineWeight = lineWeight;
		this.fillColor = fillColor;
		this.fillOpacity = fillOpacity;
		this.boost = boost;
	}

	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //

	/**
	 * Returns the first non-null value from the provided arguments.
	 *
	 * @param items Values to check in order of priority
	 * @param <T>   Type of values
	 * @return First non-null value, or null if all are null
	 */
	@SafeVarargs
	public static <T> T coalesce(T... items)
	{
		for (T item : items)
		{
			if (item != null) return item;
		}
		return null;
	}

	/**
	 * Converts a hex color string (e.g. "#00FF00") to an integer RGB value for APIs that require it.
	 *
	 * @param hex Hex color string starting with '#'
	 * @return RGB color as integer, or 0x00FF00 (green) if parsing fails
	 */
	public static int getColorAsInt(String hex)
	{
		int ret = 0x00FF00;
		try
		{
			if (hex != null && hex.startsWith("#") && hex.length() >= 7)
				ret = Integer.parseInt(hex.substring(1), 16);
		}
		catch (NumberFormatException ignored) { }
		return ret;
	}
}
