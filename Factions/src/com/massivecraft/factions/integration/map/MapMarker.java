package com.massivecraft.factions.integration.map;

/**
 * Shared configuration data for a point marker (e.g. faction home, warp).
 *
 * <p>
 * Point markers are single-location pins displayed on the map web interface.
 * This class stores all properties needed to create or update a point marker:
 * <ul>
 * <li>Optional marker ID (for map key when adding to a marker set; e.g. BlueMap)</li>
 * <li>Location (world, x, y, z coordinates)</li>
 * <li>Display text (label, description popup)</li>
 * <li>Icon name or address (interpretation is integration-specific)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Used by Dynmap, BlueMap and other map integrations. Each integration applies these
 * values to its own API (e.g. Dynmap {@code Marker}, BlueMap {@code POIMarker}).
 * When {@link #getId()} is null (e.g. Dynmap), the caller uses a separate key for the marker.
 * Instances are immutable.
 * </p>
 */
public class MapMarker
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	/** Optional marker ID for the map key when adding to a marker set. Null when key is supplied separately (e.g. Dynmap). */
	private final String id;
	public String getId() { return id; }

	private final String label;
	public String getLabel() { return label; }

	private final String world;
	public String getWorld() { return world; }

	private final double x;
	public double getX() { return x; }

	private final double y;
	public double getY() { return y; }

	private final double z;
	public double getZ() { return z; }

	private final String iconName;
	public String getIconName() { return iconName; }

	private final String description;
	public String getDescription() { return description; }

	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //

	/**
	 * Creates a new point marker value without an ID (caller supplies the marker key separately, e.g. Dynmap).
	 *
	 * @param label       Display label for the marker
	 * @param world       World name where the marker is located
	 * @param x           X coordinate (block or exact, integration-specific)
	 * @param y           Y coordinate (block or exact, integration-specific)
	 * @param z           Z coordinate (block or exact, integration-specific)
	 * @param iconName    Icon name or address (interpretation is integration-specific)
	 * @param description HTML description for the popup when the marker is clicked
	 */
	public MapMarker(String label, String world, double x, double y, double z, String iconName, String description)
	{
		this(null, label, world, x, y, z, iconName, description);
	}

	/**
	 * Creates a new point marker value with an ID (used as the marker key when adding to a set, e.g. BlueMap).
	 *
	 * @param id          Marker ID for the map key (must not be null when used with BlueMap)
	 * @param label       Display label for the marker
	 * @param world       World name where the marker is located
	 * @param x           X coordinate (block or exact, integration-specific)
	 * @param y           Y coordinate (block or exact, integration-specific)
	 * @param z           Z coordinate (block or exact, integration-specific)
	 * @param iconName    Icon name or address (interpretation is integration-specific)
	 * @param description HTML description for the popup when the marker is clicked
	 */
	public MapMarker(String id, String label, String world, double x, double y, double z, String iconName, String description)
	{
		this.id = id;
		this.label = label;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.iconName = iconName;
		this.description = description;
	}
}
