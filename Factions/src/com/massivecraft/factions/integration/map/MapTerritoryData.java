package com.massivecraft.factions.integration.map;

import com.massivecraft.massivecore.ps.PS;

import java.util.Collections;
import java.util.List;

/**
 * Shared data for a territory shape (polygon with optional holes) used by map integrations.
 *
 * <p>
 * Represents a single territory polygon for display on Dynmap, BlueMap, etc. Contains:
 * <ul>
 * <li>Display text (label, description popup)</li>
 * <li>World name</li>
 * <li>Outer boundary and optional hole boundaries (as lists of {@link PS} coordinates)</li>
 * <li>Styling via {@link MapStyle} (line/fill color, opacity, weight, boost)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Dynmap area markers support only a single polygon (no holes); integrations use {@link #getOuter()}
 * for the outline and ignore {@link #getHoles()} when creating Dynmap markers. BlueMap ExtrudeMarkers
 * support holes natively and use both outer and holes.
 * </p>
 *
 * <p>
 * Instances are immutable.
 * </p>
 */
public class MapTerritoryData
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private final String label;
	public String getLabel() { return label; }

	private final String world;
	public String getWorld() { return world; }

	private final String description;
	public String getDescription() { return description; }

	private final List<PS> outer;
	public List<PS> getOuter() { return outer; }

	private final List<List<PS>> holes;
	public List<List<PS>> getHoles() { return holes; }

	private final MapStyle style;
	public MapStyle getStyle() { return style; }

	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //

	/**
	 * Creates territory data with the given geometry and style.
	 *
	 * @param label       Display label for the territory
	 * @param world       World name where the territory is located
	 * @param description HTML description for the popup when the territory is clicked
	 * @param outer       Outer boundary as a list of corner coordinates (must not be null; defensively copied)
	 * @param holes       Optional hole boundaries (can be null or empty; defensively copied)
	 * @param style       Styling (line/fill color, opacity, weight, boost); can be null to use defaults via getters
	 */
	public MapTerritoryData(String label, String world, String description, List<PS> outer, List<List<PS>> holes, MapStyle style)
	{
		this.label = label;
		this.world = world;
		this.description = description;
		this.outer = outer == null ? Collections.emptyList() : Collections.unmodifiableList(outer);
		this.holes = holes == null || holes.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(holes);
		this.style = style;
	}
}
