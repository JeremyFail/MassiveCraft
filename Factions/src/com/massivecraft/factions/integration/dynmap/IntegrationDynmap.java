package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.factions.integration.worldguard.EngineWorldGuard;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.Integration;

public class IntegrationDynmap extends Integration
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //

	// Constants must be here rather than in EngineDynmap.
	// MConf relies on DynmapStyle which relies on these constants
	// and we must be able to load MConf without EngineDynmap.
	public final static int BLOCKS_PER_CHUNK = 16;

	public final static String FACTIONS = "factions";
	public final static String FACTIONS_ = FACTIONS + "_";

	public final static String FACTIONS_MARKERSET = FACTIONS_ + "markerset";

	public final static String FACTIONS_AREA = FACTIONS_ + "area";
	public final static String FACTIONS_AREA_ = FACTIONS_AREA + "_";

	public final static transient String DYNMAP_STYLE_LINE_COLOR = "#00FF00";
	public final static transient double DYNMAP_STYLE_LINE_OPACITY = 0.8D;
	public final static transient int DYNMAP_STYLE_LINE_WEIGHT = 3;
	public final static transient String DYNMAP_STYLE_FILL_COLOR = "#00FF00";
	public final static transient double DYNMAP_STYLE_FILL_OPACITY = 0.35D;
	public final static transient String DYNMAP_STYLE_HOME_MARKER = "greenflag";
	public final static transient boolean DYNMAP_STYLE_BOOST = false;

	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static IntegrationDynmap i = new IntegrationDynmap();
	public static IntegrationDynmap get() { return i; }
	private IntegrationDynmap()
	{
		this.setPluginName("dynmap");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Engine getEngine()
	{
		return EngineDynmap.get();
	}
	
}
