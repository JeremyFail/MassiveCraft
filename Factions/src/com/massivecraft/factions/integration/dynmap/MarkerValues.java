package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.massivecore.util.MUtil;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

public class MarkerValues
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final String label;
	public String getLabel() { return label; }
	public MarkerValues withLabel(String label) { return new MarkerValues(label, world, x, y, z, iconName, description); }

	private final String world;
	public String getWorld() { return world; }
	public MarkerValues withWorld(String world) { return new MarkerValues(label, world, x, y, z, iconName, description); }

	private final double x;
	public double getX() { return x; }
	public MarkerValues withX(double x) { return new MarkerValues(label, world, x, y, z, iconName, description); }

	private final double y;
	public double getY() { return y; }
	public MarkerValues withY(double y) { return new MarkerValues(label, world, x, y, z, iconName, description); }

	private final double z;
	public double getZ() { return z; }
	public MarkerValues withZ(double z) { return new MarkerValues(label, world, x, y, z, iconName, description); }

	private final String iconName;
	public String getIconName() { return iconName; }
	public MarkerValues withIconName(String iconName) { return new MarkerValues(label, world, x, y, z, iconName, description); }

	private final String description;
	public String getDescription() { return description; }
	public MarkerValues withDescription(String description) { return new MarkerValues(label, world, x, y, z, iconName, description); }

	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //

	public MarkerValues(String label, String world, double x, double y, double z, String iconName, String description)
	{
		this.label = label;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.iconName = iconName;
		this.description = description;
	}

	// -------------------------------------------- //
	// MAKE SURE EXISTS
	// -------------------------------------------- //

	public Marker ensureExistsAndUpdated(MarkerAPI markerApi,  MarkerSet markerset, String id)
	{
		throw new UnsupportedOperationException("todo");
	}
	
	// -------------------------------------------- //
	// CREATE
	// -------------------------------------------- //
	
	public Marker create(MarkerAPI markerApi, MarkerSet markerset, String markerId)
	{
		Marker ret = markerset.createMarker(
			markerId,
			this.getLabel(),
			this.getWorld(),
			this.getX(),
			this.getY(),
			this.getZ(),
			getMarkerIcon(markerApi, this.getIconName()),
			false // not persistent
		);
		
		if (ret == null) return null;
		
		ret.setDescription(this.getDescription());
		
		return ret;
	}
	
	// -------------------------------------------- //
	// UPDATE
	// -------------------------------------------- //
	
	public void update(MarkerAPI markerApi, MarkerSet markerset, Marker marker)
	{
		if
		(
			!MUtil.equals(marker.getWorld(), this.getWorld())
			||
			marker.getX() != this.getX()
			||
			marker.getY() != this.getY()
			||
			marker.getZ() != this.getZ()
		)
		{
			marker.setLocation(
					this.getWorld(),
					this.getX(),
					this.getY(),
					this.getZ()
			);
		}

		MUtil.setIfDifferent(this.getLabel(), marker::getLabel, marker::setLabel);

		MarkerIcon icon = getMarkerIcon(markerApi, this.iconName);
		MUtil.setIfDifferent(icon, marker::getMarkerIcon, marker::setMarkerIcon);

		MUtil.setIfDifferent(this.getDescription(), marker::getDescription, marker::setDescription);
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	public static MarkerIcon getMarkerIcon(MarkerAPI markerApi, String name)
	{
		MarkerIcon ret = markerApi.getMarkerIcon(name);
		if (ret == null) ret = markerApi.getMarkerIcon(IntegrationDynmap.DYNMAP_STYLE_HOME_MARKER);
		return ret;
	}
	
}