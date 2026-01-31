package com.massivecraft.factions.integration.dynmap;

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
 * Utility class for Dynmap integration.
 * Applies shared {@link MapLayer}, {@link MapMarker}, and {@link MapTerritoryData}
 * to the Dynmap API: ensures marker sets (layers), point markers, and area markers exist and are updated.
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

	private static MarkerSet createMarkerSet(MarkerAPI markerApi, String id, MapLayer values)
	{
		MarkerSet ret = markerApi.createMarkerSet(id, values.getLabel(), null, false);
		if (ret == null) return null;
		if (values.getMinimumZoom() > 0)
		{
			ret.setMinZoom(values.getMinimumZoom());
		}
		ret.setLayerPriority(values.getPriority());
		ret.setHideByDefault(values.isHiddenByDefault());
		return ret;
	}

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
		if (ret == null) return null;
		ret.setDescription(values.getDescription());
		return ret;
	}

	private static void updatePointMarker(MapMarker values, MarkerAPI markerApi, Marker marker)
	{
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
	 * Resolves an icon name to a Dynmap MarkerIcon, falling back to the default home marker icon if not found.
	 *
	 * @param markerApi Dynmap API
	 * @param iconName  Icon name from config (e.g. "redflag")
	 * @return The MarkerIcon to use
	 */
	public static MarkerIcon getMarkerIcon(MarkerAPI markerApi, String iconName)
	{
		MarkerIcon ret = markerApi.getMarkerIcon(iconName);
		if (ret == null) ret = markerApi.getMarkerIcon(IntegrationDynmap.DYNMAP_STYLE_HOME_MARKER);
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
		if (ret == null) return null;

		ret.setDescription(data.getDescription());
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

	private static double[] getOuterX(List<PS> outer)
	{
		if (outer == null || outer.isEmpty()) return new double[0];
		double[] x = new double[outer.size()];
		for (int i = 0; i < outer.size(); i++)
			x[i] = outer.get(i).getLocationX(true);
		return x;
	}

	private static double[] getOuterZ(List<PS> outer)
	{
		if (outer == null || outer.isEmpty()) return new double[0];
		double[] z = new double[outer.size()];
		for (int i = 0; i < outer.size(); i++)
			z[i] = outer.get(i).getLocationZ(true);
		return z;
	}

	private static boolean areaMarkerCornersEqual(AreaMarker marker, double[] x, double[] z)
	{
		int length = marker.getCornerCount();
		if (x.length != length || z.length != length) return false;
		for (int i = 0; i < length; i++)
		{
			if (marker.getCornerX(i) != x[i] || marker.getCornerZ(i) != z[i]) return false;
		}
		return true;
	}
}
