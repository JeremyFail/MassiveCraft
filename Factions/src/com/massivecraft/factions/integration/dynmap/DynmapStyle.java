package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.factions.entity.MConf;

public class DynmapStyle
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public final String lineColor;
	public int getLineColor() { return getColor(coalesce(this.lineColor, MConf.get().dynmapDefaultStyle.lineColor, IntegrationDynmap.DYNMAP_STYLE_LINE_COLOR)); }
	public DynmapStyle withLineColor(String lineColor) { return new DynmapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, homeMarker, boost); }
	
	public final Double lineOpacity;
	public double getLineOpacity() { return coalesce(this.lineOpacity, MConf.get().dynmapDefaultStyle.lineOpacity, IntegrationDynmap.DYNMAP_STYLE_LINE_OPACITY); }
	public DynmapStyle withLineOpacity(Double lineOpacity) { return new DynmapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, homeMarker, boost); }
	
	public final Integer lineWeight;
	public int getLineWeight() { return coalesce(this.lineWeight, MConf.get().dynmapDefaultStyle.lineWeight, IntegrationDynmap.DYNMAP_STYLE_LINE_WEIGHT); }
	public DynmapStyle withLineWeight(Integer lineWeight) { return new DynmapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, homeMarker, boost); }
	
	public final String fillColor;
	public int getFillColor() { return getColor(coalesce(this.fillColor, MConf.get().dynmapDefaultStyle.fillColor, IntegrationDynmap.DYNMAP_STYLE_FILL_COLOR)); }
	public DynmapStyle withFillColor(String fillColor) { return new DynmapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, homeMarker, boost); }
	
	public final Double fillOpacity;
	public double getFillOpacity() { return coalesce(this.fillOpacity, MConf.get().dynmapDefaultStyle.fillOpacity, IntegrationDynmap.DYNMAP_STYLE_FILL_OPACITY); }
	public DynmapStyle withFillOpacity(Double fillOpacity) { return new DynmapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, homeMarker, boost); }
	
	// NOTE: We just return the string here. We do not return the resolved Dynmap MarkerIcon object.
	// The reason is we use this class in the MConf. For serialization to work Dynmap would have to be loaded and we can't require that.
	// Using dynmap is optional.
	public final String homeMarker;
	public String getHomeMarker() { return coalesce(this.homeMarker, MConf.get().dynmapDefaultStyle.homeMarker, IntegrationDynmap.DYNMAP_STYLE_HOME_MARKER); }
	public DynmapStyle withHomeMarker(String homeMarker) { return new DynmapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, homeMarker, boost); }
	
	public final Boolean boost;
	public boolean getBoost() { return coalesce(this.boost, MConf.get().dynmapDefaultStyle.boost, IntegrationDynmap.DYNMAP_STYLE_BOOST); }
	public DynmapStyle withBoost(Boolean boost) { return new DynmapStyle(lineColor, lineOpacity, lineWeight, fillColor, fillOpacity, homeMarker, boost); }

	// -------------------------------------------- //
	// CONSTRUCTOR
	// -------------------------------------------- //

	public DynmapStyle()
	{
		this(null, null, null, null, null, null, null);
	}

	public DynmapStyle(String lineColor, Double lineOpacity, Integer lineWeight, String fillColor, Double fillOpacity, String homeMarker, Boolean boost)
	{
		this.lineColor = lineColor;
		this.lineOpacity = lineOpacity;
		this.lineWeight = lineWeight;
		this.fillColor = fillColor;
		this.fillOpacity = fillOpacity;
		this.homeMarker = homeMarker;
		this.boost = boost;
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	@SafeVarargs
	public static <T> T coalesce(T... items)
	{
		for (T item : items)
		{
			if (item != null) return item;
		}
		return null;
	}
	
	public static int getColor(String string)
	{
		int ret = 0x00FF00;
		try
		{
			ret = Integer.parseInt(string.substring(1), 16);
		}
		catch (NumberFormatException nfx)
		{
			
		}
		return ret;
	}
	
}