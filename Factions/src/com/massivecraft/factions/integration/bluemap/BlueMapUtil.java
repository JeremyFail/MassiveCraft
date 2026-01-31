package com.massivecraft.factions.integration.bluemap;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.massivecraft.factions.integration.map.MapMarker;
import com.massivecraft.factions.integration.map.MapStyle;
import com.massivecraft.factions.integration.map.MapStyleDefaults;
import com.massivecraft.factions.integration.map.MapTerritoryData;
import com.massivecraft.factions.integration.map.MapUtil;
import com.massivecraft.massivecore.ps.PS;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.POIMarker;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts shared map data to BlueMap markers.
 *
 * <p>
 * Handles creation of {@link POIMarker} instances (from {@link MapMarker}) and
 * {@link ExtrudeMarker} instances (from {@link MapTerritoryData}) for territory shapes.
 * Icon names from config are resolved to BlueMap asset paths when possible.
 * </p>
 */
public final class BlueMapUtil
{
	private BlueMapUtil() {}

	/** Default anchor for 32x32 icons (center). */
	private static final int DEFAULT_ICON_ANCHOR = 16;

	/** Scale factor to inset polygon (0.998 = 0.2% inward) to reduce z-fighting when adjacent territory borders overlap in 3D. */
	private static final double SHAPE_INSET_SCALE = 0.998;

	/** Scale factor to outset holes (1.002 = 0.2% outward). Inner-faction borders sit on the hole edge; outsetting the hole 
	 * creates a gap so the outer territory's hole border and the inner faction's outer border no longer z-fight. */
	private static final double SHAPE_HOLE_OUTSET_SCALE = 1.002;

	/** Epsilon for detecting shared corners (hole-hole or hole-outer). Vertices within this distance are nudged so the triangulator 
	 * does not create spurious triangles. Use 0.1 so diagonally adjacent holes (~0.04 apart after outset) are separated. */
	private static final double DUPLICATE_VERTEX_EPSILON = 0.1;
	/** Nudge factor: duplicate vertices move this fraction toward the hole centroid (0.01 = 1% inward) so the gap is visible 
	 * to the triangulator. */
	private static final double DUPLICATE_VERTEX_NUDGE = 0.01;

	/**
	 * Converts shared marker data to a BlueMap POIMarker.
	 * Sets label, position, detail (description), and icon when {@link MapMarker#getIconName()} is present.
	 *
	 * @param values Shared marker data (label, world, position, icon, description)
	 * @return A new POIMarker ready to add to a marker set
	 */
	public static POIMarker toPOIMarker(MapMarker values)
	{
		Vector3d position = new Vector3d(values.getX(), values.getY(), values.getZ());
		POIMarker marker = new POIMarker(values.getLabel(), position);
		marker.setDetail(values.getDescription());

		String iconName = values.getIconName();
		if (iconName != null && !iconName.trim().isEmpty())
		{
			String iconAddress = resolveIconAddress(iconName.trim());
			if (iconAddress != null)
			{
				marker.setIcon(iconAddress, new Vector2i(DEFAULT_ICON_ANCHOR, DEFAULT_ICON_ANCHOR));
			}
		}

		return marker;
	}

	/**
	 * Resolves a config icon name to a BlueMap icon address.
	 * BlueMap expects relative paths such as "assets/poi.svg".
	 * If the name already looks like a path (contains '/' or has an image extension), it is returned as-is.
	 * Otherwise "assets/" + name + ".png" is used so built-in or custom icons in that folder work.
	 *
	 * @param iconName Icon name from config (e.g. "redflag") or path (e.g. "assets/custom.png")
	 * @return Resolved address for BlueMap, or null if iconName is null/empty
	 */
	public static String resolveIconAddress(String iconName)
	{
		if (iconName == null || iconName.isEmpty()) return "assets/poi.svg";
		if (iconName.contains("/") || iconName.endsWith(".png") || iconName.endsWith(".svg") || iconName.endsWith(".jpg"))
			return iconName;
		return "assets/" + iconName + ".png";
	}

	// -------------------------------------------- //
	// EXTRUDE MARKER (TERRITORY) FROM MapTerritoryData
	// -------------------------------------------- //

	/**
	 * Creates a BlueMap Color from a hex string and opacity (0–1).
	 * BlueMap supports alpha; use this for transparent fill and solid or semi-transparent lines.
	 */
	private static Color colorWithAlpha(String hex, double opacity)
	{
		int rgb = MapStyle.getColorAsInt(hex);
		float a = (float) Math.max(0, Math.min(1, opacity));
		return new Color(rgb, a);
	}

	/**
	 * Returns true if {@code p} is within {@link #DUPLICATE_VERTEX_EPSILON} of any point in {@code others}.
	 */
	private static boolean isDuplicateVertex(Vector2d p, List<Vector2d> others)
	{
		if (others == null) return false;
		double eps = DUPLICATE_VERTEX_EPSILON;
		double px = p.getX(), pz = p.getY();
		for (Vector2d o : others)
		{
			if (Math.abs(o.getX() - px) <= eps && Math.abs(o.getY() - pz) <= eps)
				return true;
		}
		return false;
	}

	/**
	 * Nudges any vertex in {@code holePoints} that is within epsilon of a point in {@code outerPoints}
	 * or in any other hole in {@code otherHoles} toward this hole's centroid.
	 *
	 * <p><b>Why:</b> When two holes are diagonally adjacent they share a corner (same or nearly same x,z).
	 * Many triangulators treat such points as one vertex and then build spurious triangles (e.g. from
	 * hole corners to the outer boundary), causing visible artifacts. Moving shared vertices slightly
	 * inward breaks the tie so each hole has its own vertex and the triangulator produces correct geometry.</p>
	 *
	 * <p><b>Math:</b> Centroid (cx, cz) is the arithmetic mean of all hole points. For each vertex p
	 * that is a duplicate, we replace it with a point on the segment from p toward the centroid:
	 * new = centroid + (p - centroid) * (1 - nudge). So nudge=0.01 moves the point 1% of the way from
	 * p to the centroid (inward), creating a small gap without distorting the hole shape.</p>
	 */
	private static void nudgeDuplicateVertices(List<Vector2d> holePoints, List<Vector2d> outerPoints, List<List<Vector2d>> otherHoles)
	{
		if (holePoints == null || holePoints.size() < 3) return;
		// Centroid: (cx, cz) = mean of all vertices in this hole
		double cx = 0, cz = 0;
		for (Vector2d p : holePoints)
		{
			cx += p.getX();
			cz += p.getY();
		}
		cx /= holePoints.size();
		cz /= holePoints.size();
		double nudge = DUPLICATE_VERTEX_NUDGE;
		for (int i = 0; i < holePoints.size(); i++)
		{
			Vector2d p = holePoints.get(i);
			boolean dup = isDuplicateVertex(p, outerPoints);
			if (!dup && otherHoles != null)
			{
				for (List<Vector2d> other : otherHoles)
					if (other != holePoints && isDuplicateVertex(p, other)) { dup = true; break; }
			}
			if (dup)
			{
				// Move p toward centroid: new = c + (p - c) * (1 - nudge)
				double nx = cx + (p.getX() - cx) * (1 - nudge);
				double nz = cz + (p.getY() - cz) * (1 - nudge);
				holePoints.set(i, new Vector2d(nx, nz));
			}
		}
	}

	/**
	 * Scales a list of 2D polygon points relative to their centroid (uniform scaling).
	 *
	 * <p><b>Math:</b> Centroid c = (sum of x) / n, (sum of z) / n. Each point p is transformed to
	 * p' = c + (p - c) * scale. So scale &lt; 1 shrinks the polygon toward the center (inset),
	 * scale &gt; 1 grows it outward (outset), and scale = 1 leaves points unchanged.</p>
	 *
	 * <p><b>Use:</b> Inset (e.g. 0.998) on the outer boundary and outset (e.g. 1.002) on holes
	 * creates a tiny gap between adjacent territory borders so they don't occupy the same pixels
	 * in 3D view, avoiding z-fighting. Holes are outset so the outer territory's hole edge and
	 * the inner faction's outer edge no longer overlap.</p>
	 *
	 * @param points Input polygon vertices (x,z in BlueMap's horizontal plane)
	 * @param scale  Scale factor: &lt; 1 inset, &gt; 1 outset, 1 no-op
	 * @return New list of scaled points, or original list if no change
	 */
	private static List<Vector2d> scaleShapeTowardCentroid(List<Vector2d> points, double scale)
	{
		if (points == null || points.size() < 3 || scale == 1) return points;
		double cx = 0, cz = 0;
		for (Vector2d p : points)
		{
			cx += p.getX();
			cz += p.getY();
		}
		cx /= points.size();
		cz /= points.size();
		List<Vector2d> out = new ArrayList<>(points.size());
		for (Vector2d p : points)
			out.add(new Vector2d(cx + (p.getX() - cx) * scale, cz + (p.getY() - cz) * scale));
		return out;
	}

	/**
	 * Converts shared territory data to a BlueMap ExtrudeMarker (3D extruded polygon).
	 *
	 * <p><b>Pipeline:</b></p>
	 * <ol>
	 * <li><b>Outer shape:</b> PS corners from {@link MapTerritoryData#getOuter()} are converted to (x,z) block
	 * coordinates (using {@code getLocationX(true)}, {@code getLocationZ(true)}), then scaled inward via
	 * {@link #scaleShapeTowardCentroid} with {@link #SHAPE_INSET_SCALE} to reduce z-fighting at borders.</li>
	 * <li><b>Holes:</b> Each hole boundary is converted to (x,z), scaled outward with {@link #SHAPE_HOLE_OUTSET_SCALE},
	 * then any vertex that is within {@link #DUPLICATE_VERTEX_EPSILON} of the outer or another hole is nudged
	 * toward that hole's centroid via {@link #nudgeDuplicateVertices} so the triangulator does not create
	 * spurious triangles at shared corners (e.g. diagonally adjacent holes).</li>
	 * <li><b>Extrusion:</b> The 2D shape (outer + holes) is extruded from minY to maxY to form the 3D prism.</li>
	 * <li><b>Style:</b> Line and fill colors use config opacity (e.g. transparent fill 0.35, solid line 0.8);
	 * line width comes from config.</li>
	 * </ol>
	 *
	 * @param data Shared territory data (label, description, outer, holes, style)
	 * @param minY Minimum Y level for extrusion (bottom of the prism)
	 * @param maxY Maximum Y level for extrusion (top of the prism; can be capped by map integration config)
	 * @return A new ExtrudeMarker, or null if outer has fewer than 3 points
	 */
	public static ExtrudeMarker toExtrudeMarker(MapTerritoryData data, int minY, int maxY)
	{
		List<PS> outer = data.getOuter();
		if (outer == null || outer.size() < 3) return null;

		MapStyle style = data.getStyle();
		double lineOpacity = style != null ? style.getLineOpacity() : MapStyleDefaults.DEFAULT_LINE_OPACITY;
		double fillOpacity = style != null ? style.getFillOpacity() : MapStyleDefaults.DEFAULT_FILL_OPACITY;
		int lineWeight = style != null ? style.getLineWeight() : MapStyleDefaults.DEFAULT_LINE_WEIGHT;

		// Outer boundary: PS -> Vector2d (block x,z), then inset toward centroid
		List<Vector2d> points = new ArrayList<>();
		for (PS ps : outer)
			points.add(new Vector2d(ps.getLocationX(true), ps.getLocationZ(true)));
		List<Vector2d> outerPoints = scaleShapeTowardCentroid(points, SHAPE_INSET_SCALE);
		Shape shape = new Shape(outerPoints);
		ExtrudeMarker marker = new ExtrudeMarker(data.getLabel(), shape, minY, maxY);
		marker.setDetail(data.getDescription());

		// Holes: each hole boundary -> Vector2d, outset, then nudge shared vertices
		List<List<Vector2d>> holePointLists = new ArrayList<>();
		for (List<PS> hole : data.getHoles())
		{
			if (hole == null || hole.size() < 3) continue;
			List<Vector2d> holePoints = new ArrayList<>();
			for (PS ps : hole)
				holePoints.add(new Vector2d(ps.getLocationX(true), ps.getLocationZ(true)));
			holePoints = scaleShapeTowardCentroid(holePoints, SHAPE_HOLE_OUTSET_SCALE);
			holePointLists.add(holePoints);
		}
		for (List<Vector2d> holePoints : holePointLists)
		{
			nudgeDuplicateVertices(holePoints, outerPoints, holePointLists);
			marker.getHoles().add(new Shape(holePoints));
		}

		// Colors with opacity; line width from config
		String lineColorHex = MapUtil.getResolvedLineColor(style);
		String fillColorHex = MapUtil.getResolvedFillColor(style);
		marker.setLineColor(colorWithAlpha(lineColorHex, lineOpacity));
		marker.setFillColor(colorWithAlpha(fillColorHex, fillOpacity));
		marker.setLineWidth(Math.max(1, lineWeight));
		return marker;
	}
}
