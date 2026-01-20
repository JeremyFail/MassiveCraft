package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.massivecore.util.MUtil;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

/**
 * Represents configuration for a Dynmap marker layer/set.
 * 
 * <p>
 * A marker set is a collection of related markers (in this case, all faction territory markers)
 * that can be toggled on/off together in the Dynmap interface. This class stores:
 * <ul>
 * <li>Display label shown in the layer control</li>
 * <li>Minimum zoom level at which markers appear</li>
 * <li>Rendering priority relative to other layers</li>
 * <li>Whether the layer is hidden by default</li>
 * </ul>
 * </p>
 * 
 * Instances are immutable - use the withXXX() methods to create modified copies.
 */
public class LayerValues
{

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private final String label;
	public String getLabel() { return label; }
	public LayerValues withLabel(String label) { return new LayerValues(label, minimumZoom, priority, hiddenByDefault); }

	private final int minimumZoom;
	public int getMinimumZoom() { return minimumZoom; }
	public LayerValues withMinimumZoom(int minimumZoom) { return new LayerValues(label, minimumZoom, priority, hiddenByDefault); }

	private final int priority;
	public int getPriority() { return priority; }
	public LayerValues withPriority(int priority) { return new LayerValues(label, minimumZoom, priority, hiddenByDefault); }

	private final boolean hiddenByDefault;
	public boolean isHiddenByDefault() { return hiddenByDefault; }
	public LayerValues withHidenByDefault(boolean hideByDefault) { return new LayerValues(label, minimumZoom, priority, hideByDefault); }

	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //

	public LayerValues(String label, int minimumZoom, int priority, boolean hideByDefault)
	{
		this.label = label;
		this.minimumZoom = minimumZoom;
		this.priority = priority;
		this.hiddenByDefault = hideByDefault;
	}

	// -------------------------------------------- //
	// MASTER
	// -------------------------------------------- //

	/**
	 * Ensures a Dynmap marker set exists and is updated with current values.
	 * 
	 * <p>
	 * If the marker set doesn't exist, it will be created. If it does exist, its properties
	 * will be updated to match this object's values.
	 * </p>
	 * 
	 * @param api Dynmap API for creating marker sets
	 * @param id Unique identifier for this marker set
	 * @return The created or updated marker set, or null on failure
	 */
	public MarkerSet ensureExistsAndUpdated(MarkerAPI api, String id)
	{
		MarkerSet set = api.getMarkerSet(id);
		if (set == null)
		{
			set = this.create(api, id);
		}
		else
		{
			this.update(set);
		}

		if (set == null)
		{
			EngineDynmap.logSevere("Could not create the Faction Markerset/Layer");
		}

		return set;
	}

	// -------------------------------------------- //
	// CREATE
	// -------------------------------------------- //

	public MarkerSet create(MarkerAPI markerApi, String id)
	{
		MarkerSet ret = markerApi.createMarkerSet(id, this.label, null, false); // ("null, false" at the end means "all icons allowed, not perisistent")
		if (ret == null) return null;

		// Minimum Zoom
		if (this.minimumZoom > 0)
		{
			ret.setMinZoom(this.getMinimumZoom());
		}

		// Priority
		ret.setLayerPriority(this.getPriority());

		// Hide by Default
		ret.setHideByDefault(this.isHiddenByDefault());
		return ret;
	}

	// -------------------------------------------- //
	// UPDATE
	// -------------------------------------------- //

	public void update(MarkerSet markerset)
	{
		// Minimum Zoom
		if (this.minimumZoom > 0)
		{
			MUtil.setIfDifferent(this.getMinimumZoom(), markerset::getMinZoom, markerset::setMinZoom);
		}

		// Set other values
		MUtil.setIfDifferent(this.getLabel(), markerset::getMarkerSetLabel, markerset::setMarkerSetLabel);
		MUtil.setIfDifferent(this.getPriority(), markerset::getLayerPriority, markerset::setLayerPriority);
		MUtil.setIfDifferent(this.isHiddenByDefault(), markerset::getHideByDefault, markerset::setHideByDefault);
	}

}
