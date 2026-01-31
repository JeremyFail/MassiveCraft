package com.massivecraft.factions.integration.pl3xmap;

import com.massivecraft.factions.integration.map.MapIconUtil;
import com.massivecraft.factions.integration.map.MapMarker;
import com.massivecraft.factions.integration.map.MapStyle;
import com.massivecraft.factions.integration.map.MapStyleDefaults;
import com.massivecraft.factions.integration.map.MapTerritoryData;
import com.massivecraft.factions.integration.map.MapUtil;
import com.massivecraft.massivecore.ps.PS;
import net.pl3x.map.core.image.IconImage;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.marker.Icon;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.marker.Polygon;
import net.pl3x.map.core.markers.marker.Polyline;
import net.pl3x.map.core.markers.option.Fill;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.markers.option.Stroke;
import net.pl3x.map.core.markers.option.Tooltip;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts shared map data to Pl3xMap markers.
 *
 * <p>
 * Handles creation of {@link Polygon} instances (from {@link MapTerritoryData}) and
 * {@link Icon} instances (from {@link MapMarker}) for territory shapes and warp markers.
 * Uses Pl3xMap's Point (x,z), Polyline (outer + holes), Options (Stroke, Fill, Tooltip).
 * </p>
 *
 * @see <a href="https://granny.github.io/Pl3xMap/">Pl3xMap API</a>
 */
public final class Pl3xMapUtil
{
	private Pl3xMapUtil() {}

	private static final int DEFAULT_ICON_SIZE = MapIconUtil.DEFAULT_MARKER_SIZE;

	/**
	 * Converts hex color and opacity to Pl3xMap ARGB integer (alpha in high byte).
	 */
	private static int toArgb(String hexRgb, double opacity)
	{
		int rgb = MapStyle.getColorAsInt(hexRgb);
		int alpha = Math.max(0, Math.min(255, (int) (opacity * 255.0)));
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}

	/**
	 * Converts shared territory data to a Pl3xMap Polygon marker (outer boundary with optional holes).
	 * Outer and holes are chunk-corner {@link PS} lists; we convert to block (x,z) for Pl3xMap's
	 * 2D plane. Style (line/fill color, opacity, weight) and description tooltip are applied via {@link Options}.
	 *
	 * @param markerKey Unique key for this marker (e.g. from {@link IntegrationPl3xMap#FACTIONS_AREA_})
	 * @param data      Shared territory data (label, description, outer, holes, style)
	 * @return A new Polygon marker with Options for stroke/fill/tooltip, or null if outer has fewer than 3 points
	 */
	public static Marker<?> toPolygon(String markerKey, MapTerritoryData data)
	{
		// Sanitize key for Pl3xMap layer/marker registry (allowed chars only).
		markerKey = sanitizeKey(markerKey);
		List<PS> outer = data.getOuter();
		if (outer == null || outer.size() < 3) return null;

		// Build outer boundary: chunk-corner PS -> Pl3xMap Point (block x,z). getLocationX(true) = block coord.
		List<Point> mainPoints = new ArrayList<>();
		for (PS ps : outer)
			mainPoints.add(Point.of(ps.getLocationX(true), ps.getLocationZ(true)));

		Polyline outerPoly = Polyline.of(markerKey + "_outer", mainPoints);

		// First polyline is outer; additional polylines are holes (unclaimed areas inside the boundary).
		List<Polyline> polylines = new ArrayList<>();
		polylines.add(outerPoly);

		int holeIdx = 0;
		for (List<PS> hole : data.getHoles())
		{
			if (hole == null || hole.size() < 3) continue;
			List<Point> holePoints = new ArrayList<>();
			for (PS ps : hole)
				holePoints.add(Point.of(ps.getLocationX(true), ps.getLocationZ(true)));
			polylines.add(Polyline.of(markerKey + "_hole_" + holeIdx++, holePoints));
		}

		Polygon polygon = Polygon.of(markerKey, polylines);

		// Resolve style: line/fill opacity and weight from style or defaults; colors from MapUtil (config/faction).
		MapStyle style = data.getStyle();
		double lineOpacity = style != null ? style.getLineOpacity() : MapStyleDefaults.DEFAULT_LINE_OPACITY;
		double fillOpacity = style != null ? style.getFillOpacity() : MapStyleDefaults.DEFAULT_FILL_OPACITY;
		int lineWeight = style != null ? style.getLineWeight() : MapStyleDefaults.DEFAULT_LINE_WEIGHT;

		String lineColorHex = MapUtil.getResolvedLineColor(style);
		String fillColorHex = MapUtil.getResolvedFillColor(style);

		// Pl3xMap Options use ARGB integers; convert hex + opacity via toArgb().
		Stroke stroke = new Stroke(lineWeight, toArgb(lineColorHex, lineOpacity));
		Fill fill = new Fill(toArgb(fillColorHex, fillOpacity));
		Tooltip tooltip = data.getDescription() != null && !data.getDescription().isEmpty()
			? new Tooltip(data.getDescription())
			: null;

		Options options = new Options(stroke, fill, tooltip, null);
		return polygon.setOptions(options);
	}

	/**
	 * Converts shared marker data to a Pl3xMap Icon marker.
	 *
	 * @param values Shared marker data (label, world, position, icon, description)
	 * @return A new Icon marker ready to add to a layer
	 */
	public static Marker<?> toIcon(MapMarker values)
	{
		String key = values.getId() != null ? values.getId() : "marker_" + values.getX() + "_" + values.getZ();
		key = sanitizeKey(key);
		Point point = Point.of(values.getX(), values.getZ());
		String iconName = values.getIconName();
		if (iconName == null || iconName.trim().isEmpty()) iconName = "redflag";
		iconName = iconName.trim().toLowerCase().replace(" ", "_");

		Icon icon = Icon.of(key, point, iconName, DEFAULT_ICON_SIZE);
		if (values.getDescription() != null && !values.getDescription().isEmpty())
			icon = icon.setOptions(new Options(null, null, new Tooltip(values.getDescription()), null));
		return icon;
	}

	/**
	 * Sanitizes a string for use as a Pl3xMap layer or marker key (safe characters).
	 *
	 * @param id Raw id (e.g. marker id with underscores)
	 * @return Sanitized key string
	 */
	public static String sanitizeKey(String id)
	{
		if (id == null) return "unknown";
		return id.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

	// -------------------------------------------- //
	// ICON REGISTRATION (Pl3xMap requires icon keys to be registered)
	// -------------------------------------------- //

	/**
	 * Creates a Pl3xMap IconImage for the given key, for use with IconRegistry.register.
	 * Uses shared {@link MapIconUtil#createMarkerImageForKey} for the image; wraps it in Pl3xMap's IconImage type.
	 *
	 * @param iconKey Icon key from config (e.g. mapWarpHomeIcon, mapWarpOtherIcon)
	 * @return IconImage to register, or null if iconKey is null or blank
	 */
	public static IconImage createIconImage(String iconKey)
	{
		if (iconKey == null || iconKey.trim().isEmpty()) return null;
		String key = iconKey.trim().toLowerCase().replace(" ", "_");
		BufferedImage img = MapIconUtil.createMarkerImageForKey(key);
		if (img == null) return null;
		return new IconImage(key, img, "png");
	}
}
