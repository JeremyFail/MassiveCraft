package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

public class AreaMarkerValues
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private final String label;
	public String getLabel() { return label; }
	public AreaMarkerValues withLabel(String label) { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	private final String world;
	public String getWorld() { return world; }
	public AreaMarkerValues withWorld(String world) { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	private final PS[] corners;
	public PS[] getCorners() { return this.corners; }
	public AreaMarkerValues withCorners() { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	private final String description;
	public String getDescription() { return description; }
	public AreaMarkerValues withDescription(String description) { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }
	
	private final int lineColor;
	public int getLineColor() { return lineColor; }
	public AreaMarkerValues withLineColor(int lineColor) { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	private final double lineOpacity;
	public double getLineOpacity() { return lineOpacity; }
	public AreaMarkerValues withLineOpacity(double lineOpacity) { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	private final int lineWeight;
	public int getLineWeight() { return lineWeight; }
	public AreaMarkerValues withLineWright(int lineWeight) { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	private final int fillColor;
	public int getFillColor() { return fillColor; }
	public AreaMarkerValues withFillColor(int fillColor) { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	private final double fillOpacity;
	public double getFillOpacity() { return fillOpacity; }
	public AreaMarkerValues withFillOpacity(double fillOpacity) { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }
	
	private final boolean boost;
	public boolean isBoost() { return boost; }
	public AreaMarkerValues withBoost(boolean boost) { return new AreaMarkerValues(label, world, corners, description, lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, boost); }

	public AreaMarkerValues withStyle(DynmapStyle style)
	{
		return new AreaMarkerValues(label, world, corners, description, style);
	}

	// Caches
	private final double[] x;
	private final double[] z;

	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //

	public AreaMarkerValues(String label, String world, PS[] corners, String description, DynmapStyle style)
	{
		this(label, world, corners, description, style.getLineColor(), style.getLineOpacity(), style.getLineWeight(), style.getFillColor(), style.getFillOpacity(), style.getBoost());
	}

	public AreaMarkerValues(String label, String world, PS[] corners, String description, int lineColor, double lineOpacity, int lineWeight, int fillColor, double fillOpacity, boolean boost)
	{
		this.label = label;
		this.world = world;
		this.corners = corners;
		this.description = description;
		this.lineColor = lineColor;
		this.lineOpacity = lineOpacity;
		this.lineWeight = lineWeight;
		this.fillColor = fillColor;
		this.fillOpacity = fillOpacity;
		this.boost = boost;

		int sz = corners.length;
		x = new double[sz];
		z = new double[sz];

		for (int i = 0; i < sz; i++)
		{
			PS ps = corners[i];
			x[i] = ps.getLocationX(true);
			z[i] = ps.getLocationZ(true);
		}
	}

	// -------------------------------------------- //
	// MASTER
	// -------------------------------------------- //

	public AreaMarker ensureExistsAndUpdated(AreaMarker areaMarker, MarkerAPI markerApi, MarkerSet markerset, String markerId)
	{
		// NOTE: I remove from the map created just in the beginning of this method.
		// NOTE: That way what is left at the end will be outdated markers to remove.
		if (areaMarker == null)
		{
			areaMarker = create(markerApi, markerset, markerId);
		}
		else
		{
			update(markerApi, markerset, areaMarker);
		}

		if (areaMarker == null)
		{
			EngineDynmap.logSevere("Could not get/create the area marker " + markerId);
		}

		return areaMarker;
	}

	// -------------------------------------------- //
	// CREATE
	// -------------------------------------------- //
	
	public AreaMarker create(MarkerAPI markerApi, MarkerSet markerset, String markerId)
	{
		AreaMarker ret = markerset.createAreaMarker(
			markerId,
			this.getLabel(),
			false,
			this.getWorld(),
			this.x,
			this.z,
			false // not persistent
		);
		
		if (ret == null) return null;
		
		// Description
		ret.setDescription(this.getDescription());
		
		// Line Style
		ret.setLineStyle(this.getLineWeight(), this.getLineOpacity(), this.getLineColor());
		
		// Fill Style
		ret.setFillStyle(this.getFillOpacity(), this.getFillColor());
		
		// Boost Flag
		ret.setBoostFlag(this.isBoost());
		
		return ret;
	}
	
	// -------------------------------------------- //
	// UPDATE
	// -------------------------------------------- //
	
	public void update(MarkerAPI markerApi, MarkerSet markerset, AreaMarker marker)
	{
		// Corner Locations
		if (!equals(marker, this.x, this.z))
		{
			marker.setCornerLocations(this.x, this.z);			
		}
		
		// Label
		MUtil.setIfDifferent(this.getLabel(), marker::getLabel, marker::setLabel);
		
		// Description
		MUtil.setIfDifferent(this.getDescription(), marker::getDescription, marker::setDescription);
		
		// Line Style
		if
		(
			!MUtil.equals(marker.getLineWeight(), this.lineWeight)
			||
			!MUtil.equals(marker.getLineOpacity(), this.lineOpacity)
			||
			!MUtil.equals(marker.getLineColor(), this.lineColor)
		)
		{
			marker.setLineStyle(this.lineWeight, this.lineOpacity, this.lineColor);
		}
		
		// Fill Style
		if
		(
			!MUtil.equals(marker.getFillOpacity(), this.fillOpacity)
			||
			!MUtil.equals(marker.getFillColor(), this.fillColor)
		)
		{
			marker.setFillStyle(this.fillOpacity, this.fillColor);
		}
		
		// Boost Flag
		MUtil.setIfDifferent(this.isBoost(), marker::getBoostFlag, marker::setBoostFlag);
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	public static boolean equals(AreaMarker marker, double[] x, double[] z)
	{
		int length = marker.getCornerCount();
		
		if (x.length != length) return false;
		if (z.length != length) return false;
		
		for (int i = 0; i < length; i++)
		{
			if (marker.getCornerX(i) != x[i]) return false;
			if (marker.getCornerZ(i) != z[i]) return false;
		}
		
		return true;
	}
	
}