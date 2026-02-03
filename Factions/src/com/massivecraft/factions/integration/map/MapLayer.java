package com.massivecraft.factions.integration.map;

/**
 * Shared configuration data for a map plugin layer (e.g. territory, home warps, other warps).
 *
 * <p>
 * A layer is a collection of related markers that can be toggled on/off together in the
 * map web interface. This class stores the data common to all map integrations:
 * <ul>
 * <li>Display label shown in the layer control</li>
 * <li>Minimum zoom level at which markers appear</li>
 * <li>Rendering/sort priority relative to other layers</li>
 * <li>Whether the layer is hidden by default</li>
 * </ul>
 * </p>
 *
 * <p>
 * Used by Dynmap, BlueMap and other map integrations. Each integration applies these
 * values to its own API (e.g. Dynmap {@code MarkerSet}, BlueMap {@code MarkerSet}).
 * Instances are immutable; use the {@code withXXX()} methods to create modified copies.
 * </p>
 */
public class MapLayer
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private final String label;
	public String getLabel() { return label; }
	public MapLayer withLabel(String label) { return new MapLayer(label, minimumZoom, priority, hiddenByDefault); }

	private final int minimumZoom;
	public int getMinimumZoom() { return minimumZoom; }
	public MapLayer withMinimumZoom(int minimumZoom) { return new MapLayer(label, minimumZoom, priority, hiddenByDefault); }

	private final int priority;
	public int getPriority() { return priority; }
	public MapLayer withPriority(int priority) { return new MapLayer(label, minimumZoom, priority, hiddenByDefault); }

	private final boolean hiddenByDefault;
	public boolean isHiddenByDefault() { return hiddenByDefault; }
	public MapLayer withHiddenByDefault(boolean hiddenByDefault) { return new MapLayer(label, minimumZoom, priority, hiddenByDefault); }

	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //

	/**
	 * Creates a new layer value with the given display and visibility settings.
	 *
	 * @param label           Display label shown in the layer control
	 * @param minimumZoom     Minimum zoom level at which markers appear (0 = always visible)
	 * @param priority        Sort/rendering priority (lower = earlier in lists)
	 * @param hiddenByDefault Whether the layer is hidden by default in the web interface
	 */
	public MapLayer(String label, int minimumZoom, int priority, boolean hiddenByDefault)
	{
		this.label = label;
		this.minimumZoom = minimumZoom;
		this.priority = priority;
		this.hiddenByDefault = hiddenByDefault;
	}
}
