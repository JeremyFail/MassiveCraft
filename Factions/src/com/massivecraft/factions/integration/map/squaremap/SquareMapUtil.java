package com.massivecraft.factions.integration.map.squaremap;

import com.massivecraft.factions.integration.map.MapMarker;
import com.massivecraft.factions.integration.map.MapStyle;
import com.massivecraft.factions.integration.map.MapStyleDefaults;
import com.massivecraft.factions.integration.map.MapTerritoryData;
import com.massivecraft.factions.integration.map.MapUtil;
import com.massivecraft.massivecore.ps.PS;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.api.marker.Polygon;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts shared map data to SquareMap markers.
 *
 * <p>
 * Handles creation of {@link Polygon} instances (from {@link MapTerritoryData}) and
 * {@link xyz.jpenilla.squaremap.api.marker.Icon} instances (from {@link MapMarker}) for territory
 * shapes and warp markers. Icon names from config are passed as {@link Key} for SquareMap's icon registry.
 * </p>
 */
public final class SquareMapUtil
{
	private SquareMapUtil() {}

	private static final int DEFAULT_ICON_SIZE = 32;

	/**
	 * Converts a hex color string to {@link java.awt.Color}.
	 */
	private static Color hexToColor(String hex)
	{
		int rgb = MapStyle.getColorAsInt(hex);
		return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
	}

	/**
	 * Converts shared territory data to a SquareMap Polygon marker (outer boundary with optional holes).
	 * Outer and holes are chunk-corner {@link PS} lists; we convert to block (x,z) for SquareMap's
	 * 2D plane. Style (line/fill color, opacity, weight) and description tooltip are applied via {@link MarkerOptions}.
	 *
	 * @param data Shared territory data (label, description, outer, holes, style)
	 * @return A new Polygon marker with MarkerOptions for stroke/fill, or null if outer has fewer than 3 points
	 */
	public static Marker toPolygon(MapTerritoryData data)
	{
		List<PS> outer = data.getOuter();
		if (outer == null || outer.size() < 3) 
		{
			return null;
		}

		// Main polygon: PS chunk corners -> Point(x, z). getLocationX(true) = block corner (e.g. chunk * 16)
		List<Point> mainPoints = new ArrayList<>();
		for (PS ps : outer)
		{
			mainPoints.add(Point.of(ps.getLocationX(true), ps.getLocationZ(true)));
		}

		// Holes (unclaimed areas inside the boundary): same conversion, each hole is a list of points
		List<List<Point>> negativeSpace = new ArrayList<>();
		for (List<PS> hole : data.getHoles())
		{
			if (hole == null || hole.size() < 3) 
			{
				continue;
			}

			List<Point> holePoints = new ArrayList<>();
			for (PS ps : hole)
			{
				holePoints.add(Point.of(ps.getLocationX(true), ps.getLocationZ(true)));
			}
			negativeSpace.add(holePoints);
		}

		// Style: opacity and weight from style or defaults (null-safe)
		MapStyle style = data.getStyle();
		double lineOpacity = style != null ? style.getLineOpacity() : MapStyleDefaults.DEFAULT_LINE_OPACITY;
		double fillOpacity = style != null ? style.getFillOpacity() : MapStyleDefaults.DEFAULT_FILL_OPACITY;
		int lineWeight = style != null ? style.getLineWeight() : MapStyleDefaults.DEFAULT_LINE_WEIGHT;

		// Colors: resolved via MapUtil (style override -> config default -> fallback hex)
		String lineColorHex = MapUtil.getResolvedLineColor(style);
		String fillColorHex = MapUtil.getResolvedFillColor(style);

		MarkerOptions options = MarkerOptions.builder()
			.stroke(true)
			.strokeColor(hexToColor(lineColorHex))
			.strokeWeight(lineWeight)
			.strokeOpacity(lineOpacity)
			.fill(true)
			.fillColor(hexToColor(fillColorHex))
			.fillOpacity(fillOpacity)
			.clickTooltip(data.getDescription())
			.build();

		Polygon polygon = Polygon.polygon(mainPoints, negativeSpace);
		return polygon.markerOptions(options);
	}

	/**
	 * Converts shared marker data to a SquareMap Icon marker.
	 * Uses the icon name from config as the image key (SquareMap icon registry).
	 *
	 * @param values Shared marker data (label, world, position, icon, description)
	 * @return A new Icon marker ready to add to a layer
	 */
	public static Marker toIcon(MapMarker values)
	{
		Point point = Point.of(values.getX(), values.getZ());
		String iconName = values.getIconName();
		if (iconName == null || iconName.trim().isEmpty()) 
		{
			iconName = "redflag";
		}
		Key imageKey = Key.of(iconName.trim().toLowerCase().replace(" ", "_"));

		Marker marker = Marker.icon(point, imageKey, DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE);
		if (values.getDescription() != null && !values.getDescription().isEmpty())
		{
			marker = marker.markerOptions(marker.markerOptions().asBuilder().clickTooltip(values.getDescription()).build());
		}
		return marker;
	}

	/**
	 * Sanitizes a string for use as a SquareMap Key (allowed: [a-zA-Z0-9._-]).
	 *
	 * @param id Raw id (e.g. marker id with underscores)
	 * @return Sanitized key string
	 */
	public static String sanitizeKey(String id)
	{
		if (id == null) 
		{
			return "unknown";
		}
		return id.replaceAll("[^a-zA-Z0-9._-]", "_");
	}
}
