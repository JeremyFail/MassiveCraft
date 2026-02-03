package com.massivecraft.factions.integration.map.dynmap;

import com.massivecraft.factions.integration.map.MapLayer;
import com.massivecraft.factions.integration.map.MapMarker;
import com.massivecraft.factions.integration.map.MapStyle;
import com.massivecraft.factions.integration.map.MapStyleDefaults;
import com.massivecraft.factions.integration.map.MapTerritoryData;
import com.massivecraft.factions.integration.map.MapUtil;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import java.util.List;

/**
 * Utility class for the Dynmap map integration.
 *
 * <p>
 * Converts Factions' shared map data into Dynmap API types and ensures markers stay in sync:
 * <ul>
 * <li><b>Marker sets (layers):</b> {@link MapLayer} -> Dynmap {@link org.dynmap.markers.MarkerSet}. Used for
 *     territory, home warps, and other warps. Creates or updates the set label, priority, min zoom, and visibility.</li>
 * <li><b>Point markers:</b> {@link MapMarker} -> Dynmap {@link org.dynmap.markers.Marker}. Used for faction home
 *     and warp locations. Icon names from config are resolved via {@link #getMarkerIcon}; missing icons fall back
 *     to {@link IntegrationDynmap#DYNMAP_STYLE_HOME_MARKER}.</li>
 * <li><b>Area markers (territory):</b> {@link MapTerritoryData} -> Dynmap {@link org.dynmap.markers.AreaMarker}.
 *     Only the outer boundary is used (Dynmap area markers do not support holes). Corner coordinates are block
 *     positions from chunk-corner {@link PS} via {@code getLocationX(true)} / {@code getLocationZ(true)}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * All public methods are "ensure" style: they create the Dynmap object if missing, or update it in place so
 * the map reflects current Factions data without leaving stale markers.
 * </p>
 */
public class DynmapUtil
{
	// -------------------------------------------- //
	// CONSTRUCTOR (PRIVATE)
	// -------------------------------------------- //
	
	private DynmapUtil()
	{
		// Utility class - prevent instantiation
	}

	// -------------------------------------------- //
	// LAYER (MarkerSet) FROM MapLayer
	// -------------------------------------------- //

	/**
	 * Ensures a Dynmap marker set exists and is updated with the given layer values.
	 * If the marker set doesn't exist, it is created; otherwise its properties are updated.
	 *
	 * @param api    Dynmap API for creating marker sets
	 * @param id     Unique identifier for this marker set
	 * @param values Shared layer data (label, minimum zoom, priority, hidden by default)
	 * @return The created or updated marker set, or null on failure
	 */
	public static MarkerSet ensureMarkerSetExistsAndUpdated(MarkerAPI api, String id, MapLayer values)
	{
		MarkerSet set = api.getMarkerSet(id);
		if (set == null)
		{
			set = createMarkerSet(api, id, values);
		}
		else
		{
			updateMarkerSet(set, values);
		}
		return set;
	}

	/**
	 * Creates a new Dynmap marker set with the given id and applies layer options (label, min zoom, priority, hidden).
	 *
	 * @param markerApi Dynmap API
	 * @param id        Marker set id (e.g. {@link IntegrationDynmap#FACTIONS_LAYER_TERRITORY})
	 * @param values    Shared layer config
	 * @return The new MarkerSet, or null if creation failed
	 */
	private static MarkerSet createMarkerSet(MarkerAPI markerApi, String id, MapLayer values)
	{
		MarkerSet ret = markerApi.createMarkerSet(id, values.getLabel(), null, false);
		if (ret == null) 
		{
			return null;
		}

		if (values.getMinimumZoom() > 0)
		{
			ret.setMinZoom(values.getMinimumZoom());
		}
		ret.setLayerPriority(values.getPriority());
		ret.setHideByDefault(values.isHiddenByDefault());
		return ret;
	}

	/**
	 * Updates an existing marker set so its label, min zoom, priority, and hidden-by-default match the shared layer config.
	 * Only calls setters when the value actually changed (via {@link MUtil#setIfDifferent}).
	 */
	private static void updateMarkerSet(MarkerSet markerset, MapLayer values)
	{
		if (values.getMinimumZoom() > 0)
		{
			MUtil.setIfDifferent(values.getMinimumZoom(), markerset::getMinZoom, markerset::setMinZoom);
		}
		MUtil.setIfDifferent(values.getLabel(), markerset::getMarkerSetLabel, markerset::setMarkerSetLabel);
		MUtil.setIfDifferent(values.getPriority(), markerset::getLayerPriority, markerset::setLayerPriority);
		MUtil.setIfDifferent(values.isHiddenByDefault(), markerset::getHideByDefault, markerset::setHideByDefault);
	}

	// -------------------------------------------- //
	// POINT MARKER FROM MapMarker
	// -------------------------------------------- //

	/**
	 * Ensures a Dynmap point marker exists and is updated from shared marker values.
	 * If the existing marker is null, a new marker is created; otherwise it is updated in place.
	 *
	 * @param values    Shared marker data (label, world, position, icon, description)
	 * @param existing  Existing Dynmap marker, or null to create
	 * @param markerApi Dynmap API
	 * @param markerSet Marker set to add to when creating
	 * @param markerId  Marker ID for creation and lookup
	 * @return The created or updated marker
	 */
	public static Marker ensurePointMarkerExistsAndUpdated(MapMarker values, Marker existing, MarkerAPI markerApi, MarkerSet markerSet, String markerId)
	{
		if (existing == null)
		{
			return createPointMarker(values, markerApi, markerSet, markerId);
		}
		updatePointMarker(values, markerApi, existing);
		return existing;
	}

	/**
	 * Creates a new Dynmap point marker for a warp/home location. Uses world, block position, label, icon, and description.
	 *
	 * @param values    Shared marker data (from engine's buildHomeWarps / buildOtherWarps)
	 * @param markerApi Dynmap API
	 * @param markerset Marker set (layer) to add the marker to
	 * @param markerId  Unique id for this marker (e.g. factions_home_&lt;factionId&gt;)
	 * @return The new Marker, or null if creation failed
	 */
	private static Marker createPointMarker(MapMarker values, MarkerAPI markerApi, MarkerSet markerset, String markerId)
	{
		Marker ret = markerset.createMarker(
			markerId,
			values.getLabel(),
			values.getWorld(),
			values.getX(),
			values.getY(),
			values.getZ(),
			getMarkerIcon(markerApi, values.getIconName()),
			false
		);
		if (ret == null) 
		{
			return null;
		}
		ret.setDescription(values.getDescription());
		return ret;
	}

	/**
	 * Updates an existing point marker so location, label, icon, and description match the shared marker data.
	 * Location is updated only when it changed; other fields use setIfDifferent to avoid redundant API calls.
	 */
	private static void updatePointMarker(MapMarker values, MarkerAPI markerApi, Marker marker)
	{
		// Dynmap markers are identified by id; update world/position if they changed (e.g. warp moved)
		if (!MUtil.equals(marker.getWorld(), values.getWorld())
			|| marker.getX() != values.getX()
			|| marker.getY() != values.getY()
			|| marker.getZ() != values.getZ())
		{
			marker.setLocation(values.getWorld(), values.getX(), values.getY(), values.getZ());
		}
		MUtil.setIfDifferent(values.getLabel(), marker::getLabel, marker::setLabel);
		MarkerIcon icon = getMarkerIcon(markerApi, values.getIconName());
		MUtil.setIfDifferent(icon, marker::getMarkerIcon, marker::setMarkerIcon);
		MUtil.setIfDifferent(values.getDescription(), marker::getDescription, marker::setDescription);
	}

	/**
	 * Resolves a config icon name to a Dynmap MarkerIcon. If the name is not registered in Dynmap (e.g. typo or
	 * custom icon not in web/tiles), falls back to {@link IntegrationDynmap#DYNMAP_STYLE_HOME_MARKER} so the
	 * marker still displays.
	 *
	 * @param markerApi Dynmap API (provides icon registry)
	 * @param iconName  Icon name from config (e.g. mapWarpHomeIcon "redflag")
	 * @return The MarkerIcon to use for the point marker
	 */
	public static MarkerIcon getMarkerIcon(MarkerAPI markerApi, String iconName)
	{
		MarkerIcon ret = markerApi.getMarkerIcon(iconName);
		if (ret == null) 
		{
			ret = markerApi.getMarkerIcon(IntegrationDynmap.DYNMAP_STYLE_HOME_MARKER);
		}
		return ret;
	}

	// -------------------------------------------- //
	// AREA MARKER (TERRITORY) FROM MapTerritoryData
	// -------------------------------------------- //

	/**
	 * Ensures a Dynmap area marker exists and is updated from shared territory data.
	 * Dynmap area markers support only a single polygon (no holes); only {@link MapTerritoryData#getOuter()} is used.
	 *
	 * @param data      Shared territory data (label, world, description, outer, style)
	 * @param existing  Existing Dynmap area marker, or null to create
	 * @param markerApi Dynmap API
	 * @param markerSet Marker set to add to when creating
	 * @param markerId  Marker ID for creation and lookup
	 * @return The created or updated area marker, or null on failure
	 */
	public static AreaMarker ensureAreaMarkerExistsAndUpdated(MapTerritoryData data, AreaMarker existing, MarkerAPI markerApi, MarkerSet markerSet, String markerId)
	{
		if (existing == null)
		{
			return createAreaMarker(data, markerApi, markerSet, markerId);
		}
		updateAreaMarker(data, markerApi, existing);
		return existing;
	}

	/**
	 * Creates a new Dynmap area marker for one contiguous territory region. Uses only the outer boundary
	 * (Dynmap area markers do not support holes). Corner coordinates are block positions from chunk-corner
	 * {@link PS} via {@link #getOuterX} / {@link #getOuterZ}. Applies line/fill style from shared config or faction style.
	 *
	 * @param data      Shared territory data (outer polygon, world, label, description, style)
	 * @param markerApi Dynmap API
	 * @param markerSet Territory marker set to add to
	 * @param markerId  Unique id (e.g. factions_area_world__factionId__regionIndex)
	 * @return The new AreaMarker, or null if outer has fewer than 3 corners or creation failed
	 */
	private static AreaMarker createAreaMarker(MapTerritoryData data, MarkerAPI markerApi, MarkerSet markerSet, String markerId)
	{
		double[] x = getOuterX(data.getOuter());
		double[] z = getOuterZ(data.getOuter());
		if (x.length < 3) return null;

		AreaMarker ret = markerSet.createAreaMarker(
			markerId,
			data.getLabel(),
			false,
			data.getWorld(),
			x,
			z,
			false
		);
		if (ret == null) 
		{
			return null;
		}

		ret.setDescription(data.getDescription());
		// Line/fill colors from MapUtil (config or faction override); opacity and weight from style or defaults
		MapStyle style = data.getStyle();
		int lineColor = MapStyle.getColorAsInt(MapUtil.getResolvedLineColor(style));
		int fillColor = MapStyle.getColorAsInt(MapUtil.getResolvedFillColor(style));
		double lineOpacity = style != null ? style.getLineOpacity() : MapStyleDefaults.DEFAULT_LINE_OPACITY;
		int lineWeight = style != null ? style.getLineWeight() : MapStyleDefaults.DEFAULT_LINE_WEIGHT;
		double fillOpacity = style != null ? style.getFillOpacity() : MapStyleDefaults.DEFAULT_FILL_OPACITY;
		boolean boost = style != null && style.getBoost();

		ret.setLineStyle(lineWeight, lineOpacity, lineColor);
		ret.setFillStyle(fillOpacity, fillColor);
		ret.setBoostFlag(boost);
		return ret;
	}

	/**
	 * Updates an existing area marker so corners, label, description, and line/fill style match the shared
	 * territory data. Corner arrays are only written when they actually changed (avoids unnecessary Dynmap updates).
	 */
	private static void updateAreaMarker(MapTerritoryData data, MarkerAPI markerApi, AreaMarker marker)
	{
		double[] x = getOuterX(data.getOuter());
		double[] z = getOuterZ(data.getOuter());

		if (!areaMarkerCornersEqual(marker, x, z))
		{
			marker.setCornerLocations(x, z);
		}
		MUtil.setIfDifferent(data.getLabel(), marker::getLabel, marker::setLabel);
		MUtil.setIfDifferent(data.getDescription(), marker::getDescription, marker::setDescription);

		MapStyle style = data.getStyle();
		int lineColor = MapStyle.getColorAsInt(MapUtil.getResolvedLineColor(style));
		int fillColor = MapStyle.getColorAsInt(MapUtil.getResolvedFillColor(style));
		double lineOpacity = style != null ? style.getLineOpacity() : MapStyleDefaults.DEFAULT_LINE_OPACITY;
		int lineWeight = style != null ? style.getLineWeight() : MapStyleDefaults.DEFAULT_LINE_WEIGHT;
		double fillOpacity = style != null ? style.getFillOpacity() : MapStyleDefaults.DEFAULT_FILL_OPACITY;
		boolean boost = style != null && style.getBoost();

		marker.setLineStyle(lineWeight, lineOpacity, lineColor);
		marker.setFillStyle(fillOpacity, fillColor);
		marker.setBoostFlag(boost);
	}

	/**
	 * Converts the outer boundary of a territory polygon from chunk-corner {@link PS} list to block X coordinates.
	 * Uses {@code getLocationX(true)} so chunk (cx, cz) yields block position (cx*16, cz*16) at the corner.
	 *
	 * @param outer List of chunk-corner PS from {@link MapTerritoryData#getOuter()} (clockwise boundary)
	 * @return Array of X block coordinates for Dynmap area marker corners; empty if outer is null or empty
	 */
	private static double[] getOuterX(List<PS> outer)
	{
		if (outer == null || outer.isEmpty()) 
		{
			return new double[0];
		}

		double[] x = new double[outer.size()];
		for (int i = 0; i < outer.size(); i++)
		{
			x[i] = outer.get(i).getLocationX(true);
		}
		return x;
	}

	/**
	 * Converts the outer boundary of a territory polygon from chunk-corner {@link PS} list to block Z coordinates.
	 * Mirrors {@link #getOuterX}; same indices correspond to the same corner.
	 *
	 * @param outer List of chunk-corner PS from {@link MapTerritoryData#getOuter()}
	 * @return Array of Z block coordinates for Dynmap area marker corners; empty if outer is null or empty
	 */
	private static double[] getOuterZ(List<PS> outer)
	{
		if (outer == null || outer.isEmpty()) 
		{
			return new double[0];
		}

		double[] z = new double[outer.size()];
		for (int i = 0; i < outer.size(); i++)
		{
			z[i] = outer.get(i).getLocationZ(true);
		}
		return z;
	}

	/**
	 * Returns true if the area marker's current corner positions match the given x and z arrays (same length and values).
	 * Used to avoid calling {@link AreaMarker#setCornerLocations} when the polygon has not changed.
	 *
	 * @param marker Existing Dynmap area marker
	 * @param x      New corner X block coordinates
	 * @param z      New corner Z block coordinates
	 * @return true if marker corners equal (x, z) element-wise
	 */
	private static boolean areaMarkerCornersEqual(AreaMarker marker, double[] x, double[] z)
	{
		int length = marker.getCornerCount();
		if (x.length != length || z.length != length) 
		{
			return false;
		}

		// Loop through the corners and check if they are equal
		for (int i = 0; i < length; i++)
		{
			if (marker.getCornerX(i) != x[i] || marker.getCornerZ(i) != z[i]) 
			{
				return false;
			}
		}
		return true;
	}
}
