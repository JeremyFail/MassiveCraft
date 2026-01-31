package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.map.MapLayer;
import com.massivecraft.factions.integration.map.MapMarker;
import com.massivecraft.factions.integration.map.MapStyle;
import com.massivecraft.factions.integration.map.MapTerritoryData;
import com.massivecraft.factions.integration.map.MapUtil;
import com.massivecraft.factions.integration.map.TerritoryPolygonBuilder;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.TimeDiffUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * EngineDynmap handles the integration between Factions and Dynmap.
 * 
 * <p>
 * This engine runs asynchronously every 15 seconds to update faction territory displays
 * on the Dynmap web interface. It creates visual polygon boundaries around claimed chunks,
 * displays faction information in popups, and applies custom styling per faction.
 * </p>
 * 
 * <p>
 * <strong>Thread Safety:</strong>
 * <ul>
 * <li>Polygon generation (createAreas) runs asynchronously to avoid blocking the main thread</li>
 * <li>Dynmap API calls (updateFactionsDynmap) run synchronously on the main thread as required by Dynmap</li>
 * </ul>
 * </p>
 */
public class EngineDynmap extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EngineDynmap i = new EngineDynmap();
	public static EngineDynmap get() { return i; }
	private EngineDynmap()
	{
		// Async
		this.setSync(false);

		// Every 15 seconds
		this.setPeriod(15 * 20L);
	}

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private DynmapAPI dynmapApi;
	private MarkerAPI markerApi;
	
	// Separate marker sets for different layer types
	private MarkerSet markersetTerritory;
	private MarkerSet markersetHome;
	private MarkerSet markersetWarps;

	// -------------------------------------------- //
	// RUN: UPDATE
	// -------------------------------------------- //
	
	// Thread Safe / Asynchronous: Yes
	@Override
	public void run()
	{
		// Is Dynmap enabled?
		if (MConf.get().dynmapEnabled)
		{
			this.perform();
		}
		else
		{
			this.disable();
		}
	}

	/**
	 * Performs the asynchronous portion of the Dynmap update.
	 * 
	 * <p>
	 * This method runs on an async thread and generates all polygon data for faction territories.
	 * It's safe to run on the main thread but not recommended as it can be CPU intensive for
	 * servers with large numbers of claimed chunks.
	 * </p>
	 * 
	 * <p>
	 * Process:
	 * <ol>
	 * <li>Generate area marker data (polygons) for all factions</li>
	 * <li>Schedule synchronous Dynmap API update on main thread</li>
	 * </ol>
	 * </p>
	 */
	public void perform()
	{
		long before = System.currentTimeMillis();

		// Generate area markers asynchronously (CPU intensive)
		final Map<String, MapTerritoryData> areas = createAreas();

		logTimeSpent("Async", before);

		// Shedule non thread safe sync at the end!
		Bukkit.getScheduler().scheduleSyncDelayedTask(Factions.get(), () -> this.updateFactionsDynmap(areas));
	}

	/**
	 * Updates all Dynmap markers on the main thread.
	 * 
	 * <p>
	 * This method must run synchronously on the main thread as required by the Dynmap API.
	 * It updates territory markers, home warp markers, and other warp markers based on
	 * configuration settings.
	 * </p>
	 * 
	 * @param areas Pre-computed territory data from async processing
	 */
	public void updateFactionsDynmap(Map<String, MapTerritoryData> areas)
	{
		long before = System.currentTimeMillis();

		if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("async");

		if (!fetchDynmapAPI()) return;

		// Update territory layer
		if (!updateLayerTerritory(createLayerTerritory())) return;
		updateAreas(areas);
		
		// Update home warp layer if enabled
		if (MConf.get().mapShowHomeWarp)
		{
			if (updateLayerHome(createLayerHome()))
			{
				Map<String, MapMarker> homeWarps = createHomeWarps();
				updateHomeWarps(homeWarps);
			}
		}
		else
		{
			// Cleanup home layer if disabled
			if (this.markersetHome != null)
			{
				this.markersetHome.deleteMarkerSet();
				this.markersetHome = null;
			}
		}
		
		// Update other warps layer if enabled
		if (MConf.get().mapShowOtherWarps)
		{
			if (updateLayerWarps(createLayerWarps()))
			{
				Map<String, MapMarker> otherWarps = createOtherWarps();
				updateOtherWarps(otherWarps);
			}
		}
		else
		{
			// Cleanup warps layer if disabled
			if (this.markersetWarps != null)
			{
				this.markersetWarps.deleteMarkerSet();
				this.markersetWarps = null;
			}
		}
		
		logTimeSpent("Sync", before);
	}

	/**
	 * Disables the Dynmap integration by removing all marker sets.
	 * Called when Dynmap integration is disabled in configuration.
	 */
	public void disable()
	{
		if (this.markersetTerritory != null)
		{
			this.markersetTerritory.deleteMarkerSet();
			this.markersetTerritory = null;
		}
		if (this.markersetHome != null)
		{
			this.markersetHome.deleteMarkerSet();
			this.markersetHome = null;
		}
		if (this.markersetWarps != null)
		{
			this.markersetWarps.deleteMarkerSet();
			this.markersetWarps = null;
		}
	}
	
	// Thread Safe / Asynchronous: Yes
	public static void logTimeSpent(String name, long start)
	{
		if (!MConf.get().dynmapLogTimeSpent) return;
		long end = System.currentTimeMillis();
		long duration = end-start;

		String message = Txt.parse("<i>Dynmap %s took <h>%dms<i>.", name, duration);
		Factions.get().log(message);
	}

	// -------------------------------------------- //
	// API
	// -------------------------------------------- //
	
	// Thread Safe / Asynchronous: No
	public boolean fetchDynmapAPI()
	{
		// Get DynmapAPI
		this.dynmapApi = (DynmapAPI) Bukkit.getPluginManager().getPlugin("dynmap");
		if (this.dynmapApi == null)
		{
			logSevere("Could not access the DynmapAPI.");
			return false;
		}
		
		// Get MarkerAPI
		this.markerApi = this.dynmapApi.getMarkerAPI();
		if (this.markerApi == null)
		{
			logSevere("Could not access the MarkerAPI.");
			return false;
		}
		
		return true;
	}
	
	// -------------------------------------------- //
	// UPDATE: LAYERS
	// -------------------------------------------- //
	
	// ========== TERRITORY LAYER ==========
	
	// Thread Safe / Asynchronous: Yes
	public MapLayer createLayerTerritory()
	{
		return new MapLayer(
			MConf.get().mapLayerName,
			MConf.get().mapLayerMinimumZoom,
			MConf.get().mapLayerPriority,
			MConf.get().mapLayerHiddenByDefault
		);
	}

	// Thread Safe / Asynchronous: No
	public boolean updateLayerTerritory(MapLayer temp)
	{
		this.markersetTerritory = DynmapUtil.ensureMarkerSetExistsAndUpdated(this.markerApi, IntegrationDynmap.FACTIONS_MARKERSET_TERRITORY, temp);
		return this.markersetTerritory != null;
	}
	
	// ========== HOME WARP LAYER ==========
	
	// Thread Safe / Asynchronous: Yes
	public MapLayer createLayerHome()
	{
		return new MapLayer(
			MConf.get().mapLayerNameHome,
			MConf.get().mapLayerMinimumZoomHome,
			MConf.get().mapLayerPriorityHome,
			MConf.get().mapLayerHiddenByDefaultHome
		);
	}

	// Thread Safe / Asynchronous: No
	public boolean updateLayerHome(MapLayer temp)
	{
		this.markersetHome = DynmapUtil.ensureMarkerSetExistsAndUpdated(this.markerApi, IntegrationDynmap.FACTIONS_MARKERSET_HOME, temp);
		return this.markersetHome != null;
	}
	
	// ========== OTHER WARPS LAYER ==========
	
	// Thread Safe / Asynchronous: Yes
	public MapLayer createLayerWarps()
	{
		return new MapLayer(
			MConf.get().mapLayerNameWarps,
			MConf.get().mapLayerMinimumZoomWarps,
			MConf.get().mapLayerPriorityWarps,
			MConf.get().mapLayerHiddenByDefaultWarps
		);
	}

	// Thread Safe / Asynchronous: No
	public boolean updateLayerWarps(MapLayer temp)
	{
		this.markersetWarps = DynmapUtil.ensureMarkerSetExistsAndUpdated(this.markerApi, IntegrationDynmap.FACTIONS_MARKERSET_WARPS, temp);
		return this.markersetWarps != null;
	}

	// -------------------------------------------- //
	// UPDATE: HOME WARPS
	// -------------------------------------------- //
	
	// Thread Safe: YES
	public Map<String, MapMarker> createHomeWarps()
	{
		Map<String, MapMarker> ret = new MassiveMap<>();

		for (Faction faction : FactionColl.get().getAll())
		{
			for (Warp warp : faction.getWarps().getAll())
			{
				if (!"home".equalsIgnoreCase(warp.getName())) continue;

				PS location = warp.getLocation();
				if (location == null) continue;

				String world = location.getWorld();
				if (world == null) continue;

				if (!isVisible(faction, world)) continue;

				String label = faction.getName() + " - Home";
				String description = "<b>" + faction.getName() + "</b><br/>Home";

				MapMarker marker = new MapMarker(
					label,
					world,
					location.getLocationX(),
					location.getLocationY(),
					location.getLocationZ(),
					MConf.get().mapWarpHomeIcon,
					description
				);

				String markerId = "factions_home_" + faction.getId();
				ret.put(markerId, marker);
			}
		}

		return ret;
	}
	
	/**
	 * Updates home warp markers on the Dynmap.
	 * Removes old markers that no longer exist and creates/updates current markers.
	 *
	 * @param values Map of marker IDs to shared marker values for all current home warps
	 */
	// Thread Safe: NO
	public void updateHomeWarps(Map<String, MapMarker> values)
	{
		this.markersetHome.getMarkers().stream()
			.filter(m -> m.getMarkerID().startsWith("factions_home_"))
			.filter(m -> !values.containsKey(m.getMarkerID()))
			.forEach(Marker::deleteMarker);

		Map<String, Marker> markers = getHomeMarkerMap(this.markersetHome);

		values.forEach((markerId, value) ->
			DynmapUtil.ensurePointMarkerExistsAndUpdated(value, markers.get(markerId), this.markerApi, this.markersetHome, markerId));
	}
	
	/**
	 * Creates a map of home warp marker IDs to Marker objects for quick lookup.
	 * 
	 * @param markerSet The marker set containing home warp markers
	 * @return Map of marker IDs to Marker objects
	 */
	private static Map<String, Marker> getHomeMarkerMap(MarkerSet markerSet)
	{
		return markerSet.getMarkers().stream()
			.filter(m -> m.getMarkerID().startsWith("factions_home_"))
			.collect(Collectors.toMap(Marker::getMarkerID, m -> m));
	}
	
	// -------------------------------------------- //
	// UPDATE: OTHER WARPS
	// -------------------------------------------- //
	
	// Thread Safe: YES
	public Map<String, MapMarker> createOtherWarps()
	{
		Map<String, MapMarker> ret = new MassiveMap<>();

		for (Faction faction : FactionColl.get().getAll())
		{
			for (Warp warp : faction.getWarps().getAll())
			{
				if ("home".equalsIgnoreCase(warp.getName())) continue;

				PS location = warp.getLocation();
				if (location == null) continue;

				String world = location.getWorld();
				if (world == null) continue;

				if (!isVisible(faction, world)) continue;

				String label = faction.getName() + " - " + warp.getName();
				String description = "<b>" + faction.getName() + "</b><br/>Warp: " + warp.getName();

				MapMarker marker = new MapMarker(
					label,
					world,
					location.getLocationX(),
					location.getLocationY(),
					location.getLocationZ(),
					MConf.get().mapWarpOtherIcon,
					description
				);

				String markerId = "factions_warp_" + faction.getId() + "_" + warp.getId();
				ret.put(markerId, marker);
			}
		}

		return ret;
	}
	
	/**
	 * Updates non-home warp markers on the Dynmap.
	 * Removes old markers that no longer exist and creates/updates current markers.
	 *
	 * @param values Map of marker IDs to shared marker values for all current non-home warps
	 */
	// Thread Safe: NO
	public void updateOtherWarps(Map<String, MapMarker> values)
	{
		this.markersetWarps.getMarkers().stream()
			.filter(m -> m.getMarkerID().startsWith("factions_warp_"))
			.filter(m -> !values.containsKey(m.getMarkerID()))
			.forEach(Marker::deleteMarker);

		Map<String, Marker> markers = getOtherWarpsMarkerMap(this.markersetWarps);

		values.forEach((markerId, value) ->
			DynmapUtil.ensurePointMarkerExistsAndUpdated(value, markers.get(markerId), this.markerApi, this.markersetWarps, markerId));
	}
	
	/**
	 * Creates a map of non-home warp marker IDs to Marker objects for quick lookup.
	 * 
	 * @param markerSet The marker set containing non-home warp markers
	 * @return Map of marker IDs to Marker objects
	 */
	private static Map<String, Marker> getOtherWarpsMarkerMap(MarkerSet markerSet)
	{
		return markerSet.getMarkers().stream()
			.filter(m -> m.getMarkerID().startsWith("factions_warp_"))
			.collect(Collectors.toMap(Marker::getMarkerID, m -> m));
	}
	
	// -------------------------------------------- //
	// UPDATE: AREAS
	// -------------------------------------------- //
	
	// Thread Safe: YES
	public Map<String, MapTerritoryData> createAreas()
	{
		Map<String, Map<Faction, Set<PS>>> worldFactionChunks = BoardColl.get().getWorldToFactionToChunks(false);
		return createAreas(worldFactionChunks);

	}
	
	// Thread Safe: YES
	public Map<String, MapTerritoryData> createAreas(Map<String, Map<Faction, Set<PS>>> worldFactionChunks)
	{
		// For each world create the areas
		return worldFactionChunks.entrySet().stream()
			.map(this::createAreas)
			// And combine all of those into a single map:
			.map(Map::entrySet)
			.flatMap(Set::stream)
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	/**
	 * Creates area markers for a world and its factions (convenience method).
	 * 
	 * @param superEntry Entry containing world name and faction-to-chunks mapping
	 * @return Map of marker IDs to territory data
	 */
	public Map<String, MapTerritoryData> createAreas(Entry<String, Map<Faction, Set<PS>>> superEntry)
	{
		return createAreas(superEntry.getKey(), superEntry.getValue());
	}

	/**
	 * Creates area markers for all factions in a world.
	 * 
	 * @param world The world name
	 * @param map Mapping of factions to their claimed chunks
	 * @return Map of marker IDs to territory data
	 */
	public Map<String, MapTerritoryData> createAreas(String world, Map<Faction, Set<PS>> map)
	{
		// For each entry convert it into the appropriate map (with method below)
		return map.entrySet().stream()
			.map(e -> createAreas(world, e))
			// And combine all of those into a single map:
			.map(Map::entrySet)
			.flatMap(Set::stream)
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	/**
	 * Creates area markers for a faction in a world (convenience method).
	 * 
	 * @param world The world name
	 * @param entry Entry containing faction and its claimed chunks
	 * @return Map of marker IDs to territory data
	 */
	public Map<String, MapTerritoryData> createAreas(String world, Entry<Faction, Set<PS>> entry)
	{
		return createAreas(world, entry.getKey(), entry.getValue());
	}

	public Map<String, MapTerritoryData> createAreas(String world, Faction faction, Set<PS> chunks)
	{
		// If the faction is visible ...
		if (!isVisible(faction, world)) return Collections.emptyMap();

		// ... and has any chunks ...
		if (chunks.isEmpty()) return Collections.emptyMap();

		Map<String, MapTerritoryData> ret = new MassiveMap<>();

		// Get info
		String description = getDescription(faction);
		MapStyle style = this.getStyle(faction);
		
		// Here we start of with all chunks
		// This field is slowly cleared when the chunks are grouped into polygons
		Set<PS> allChunksSource = new MassiveSet<>(chunks);

		while (!allChunksSource.isEmpty())
		{
			// Create the polygon using flood fill
			Set<PS> polygonChunks = new MassiveSet<>();
			Iterator<PS> it = allChunksSource.iterator();
			PS startChunk = it.next();
			floodFillTarget(allChunksSource, polygonChunks, startChunk);

			// Build single polygon with etch-a-sketch (outer + holes merged into one outline).
			// Dynmap AreaMarker does not support holes, so we use buildPolygon which traces
			// cutouts into the outline; BlueMap uses getPolygonWithHoles elsewhere for native holes.
			List<PS> outer = TerritoryPolygonBuilder.buildContiguousPolygon(polygonChunks);
			if (outer.isEmpty()) continue;

			// Build information for specific area (holes empty for Dynmap; outer has cutouts)
			String markerId = calcMarkerId(world, faction);
			MapTerritoryData data = new MapTerritoryData(faction.getName(), world, description, outer, Collections.emptyList(), style);
			ret.put(markerId, data);
		}
		
		return ret;
	}

	// This markerIndex handles the case where a faction has multiple disconnected territories in a single world
	private int markerIdx = 0;
	private String lastPartialMarkerId = "";
	
	/**
	 * Calculates a unique marker ID for a faction's territory in a world.
	 * Increments counter for multiple disconnected territories.
	 * 
	 * @param world The world name
	 * @param faction The faction
	 * @return Unique marker ID string
	 */
	public String calcMarkerId(String world, Faction faction)
	{
		// Calc current partial
		String partial = IntegrationDynmap.FACTIONS_AREA_ + world + "__" + faction.getId() + "__";

		// If different than last time, then reset the counter
		if (!partial.equals(lastPartialMarkerId)) markerIdx = 0;

		this.lastPartialMarkerId = partial;

		return partial + markerIdx++;
	}
	
	/**
	 * Updates territory area markers on the Dynmap.
	 * Removes old markers that no longer exist and creates/updates current markers.
	 * 
	 * @param values Map of marker IDs to territory data for all current territories
	 */
	// Thread Safe: NO
	public void updateAreas(Map<String, MapTerritoryData> values)
	{
		// Cleanup old markers
		this.markersetTerritory.getAreaMarkers().stream() // Get current markers
			.filter(am -> !values.containsKey(am.getMarkerID())) // That are not in the new map
			.forEach(AreaMarker::deleteMarker); // and delete them


		// Map Current
		Map<String, AreaMarker> markers = getMarkerMap(this.markersetTerritory);

		// Loop New
		values.forEach((markerId, value) ->
			DynmapUtil.ensureAreaMarkerExistsAndUpdated(value, markers.get(markerId), this.markerApi, this.markersetTerritory, markerId));
	}

	/**
	 * Creates a map of area marker IDs to AreaMarker objects for quick lookup.
	 * 
	 * @param markerSet The marker set containing area markers
	 * @return Map of marker IDs to AreaMarker objects
	 */
	private static Map<String, AreaMarker> getMarkerMap(MarkerSet markerSet)
	{
		return markerSet.getAreaMarkers().stream().collect(Collectors.toMap(AreaMarker::getMarkerID, m->m));
	}
	
	// -------------------------------------------- //
	// UTIL & SHARED
	// -------------------------------------------- //
	
	// Thread Safe / Asynchronous: Yes
	private String getDescription(Faction faction)
	{
		String ret = "<div class=\"regioninfo\">" + MConf.get().mapDescriptionWindowFormat + "</div>";

		// Name
		String name = faction.getName();
		ret = MapUtil.addToHtml(ret, "name", name);

		// Description
		String description = faction.getDescriptionDesc();
		ret = MapUtil.addToHtml(ret, "description", description);

		// MOTD
		String motd = faction.getMotd();
		if (motd != null) ret = MapUtil.addToHtml(ret, "motd", motd);

		// Age
		long ageMillis = faction.getAge();
		LinkedHashMap<TimeUnit, Long> ageUnitcounts = TimeDiffUtil.limit(TimeDiffUtil.unitcounts(ageMillis, TimeUnit.getAllButMillisSecondsAndMinutes()), 3);
		String age = TimeDiffUtil.formatedVerboose(ageUnitcounts);
		ret = MapUtil.addToHtml(ret, "age", age);

		// Money
		String money;
		if (Econ.isEnabled() && MConf.get().mapShowMoneyInDescription)
		{
			money = faction.isNormal() ? Money.format(Econ.getMoney(faction)) : "N/A";
		}
		else
		{
			money = "unavailable";
		}
		ret = MapUtil.addToHtml(ret, "money", money);

		// Flags
		Map<MFlag, Boolean> flags = MFlag.getAll().stream()
			.filter(MFlag::isVisible)
			.collect(Collectors.toMap(m -> m, faction::getFlag));

		List<String> flagMapParts = new MassiveList<>();
		List<String> flagTableParts = new MassiveList<>();

		for (Entry<MFlag, Boolean> entry : flags.entrySet())
		{
			String flagName = entry.getKey().getName();
			boolean value = entry.getValue();

			String bool = String.valueOf(value);
			String color = MapUtil.calcBoolcolor(flagName, value);
			String boolcolor = MapUtil.calcBoolcolor(String.valueOf(value), value);

			ret = ret.replace("%" + flagName + ".bool%", bool);
			ret = ret.replace("%" + flagName + ".color%", color);
			ret = ret.replace("%" + flagName + ".boolcolor%", boolcolor);

			flagMapParts.add(flagName + ": " + boolcolor);
			flagTableParts.add(color);
		}

		String flagMap = Txt.implode(flagMapParts, "<br>\n");
		ret = ret.replace("%flags.map%", flagMap);

		for (int cols = 1; cols <= 10; cols++)
		{
			String flagTable = MapUtil.getHtmlAsciTable(flagTableParts, cols);
			ret = ret.replace("%flags.table" + cols + "%", flagTable);
		}

		// Players
		List<MPlayer> playersList = faction.getMPlayers();
		String playersCount = String.valueOf(playersList.size());
		String players = MapUtil.getHtmlPlayerString(playersList);

		MPlayer playersLeaderObject = faction.getLeader();
		String playersLeader = MapUtil.getHtmlPlayerName(playersLeaderObject);

		DecimalFormat df = new DecimalFormat("#.##");

		ret = ret.replace("%players%", players);
		ret = ret.replace("%players.count%", playersCount);
		ret = ret.replace("%players.leader%", playersLeader);
		ret = ret.replace("%power%", df.format(faction.getPower()));
		ret = ret.replace("%maxpower%", df.format(faction.getPowerMax()));
		ret = ret.replace("%claims%", String.valueOf(faction.getLandCount()));

		return ret;
	}

	// Thread Safe / Asynchronous: Yes
	private boolean isVisible(Faction faction, String world)
	{
		if (faction == null) throw new NullPointerException("faction");
		if (world == null) throw new NullPointerException("world");
		return MapUtil.isFactionVisible(faction.getId(), faction.getName(), world);
	}

	// Thread Safe / Asynchronous: Yes
	public MapStyle getStyle(Faction faction)
	{
		Map<String, MapStyle> styles = MConf.get().mapFactionStyleOverrides;

		// Priority 1: Admin override by faction ID or name
		MapStyle adminOverride = MapStyle.coalesce(
			styles.get(faction.getId()),
			styles.get(faction.getName())
		);
		if (adminOverride != null) return adminOverride;

		// Priority 2: Faction colors (if enabled)
		if (MConf.get().mapUseFactionColors)
		{
			String primaryColor = faction.hasPrimaryColor() ? faction.getPrimaryColor() : null;
			String secondaryColor = faction.hasSecondaryColor() ? faction.getSecondaryColor() : null;
			return new MapStyle().withLineColor(secondaryColor).withFillColor(primaryColor);
		}

		// Priority 3: Default style
		return MConf.get().mapDefaultStyle;
	}

	/**
	 * Logs a severe error message to the Factions log in red.
	 * 
	 * @param msg The error message to log
	 */
	public static void logSevere(String msg)
	{
		String message = ChatColor.RED.toString() + msg;
		Factions.get().log(message);
	}
	
	enum Direction
	{
		XPLUS, ZPLUS, XMINUS, ZMINUS

		;

		/**
		 * Gets the chunk position adjacent to the given position in this direction.
		 * 
		 * @param ps The starting position
		 * @return The adjacent position
		 */
		public PS adjacent(PS ps)
		{
			switch (this)
			{
				case XPLUS: return PS.valueOf(ps.getChunkX() + 1, ps.getChunkZ());
				case ZPLUS: return PS.valueOf(ps.getChunkX(), ps.getChunkZ() + 1);
				case XMINUS: return PS.valueOf(ps.getChunkX() - 1, ps.getChunkZ());
				case ZMINUS: return PS.valueOf(ps.getChunkX(), ps.getChunkZ() - 1);
			}
			throw new RuntimeException("say what");
		}

		/**
		 * Gets the corner position for a chunk boundary in this direction.
		 * 
		 * @param ps The chunk position
		 * @return The corner position
		 */
		public PS getCorner(PS ps)
		{
			switch (this)
			{
				case XPLUS: return PS.valueOf(ps.getChunkX() + 1, ps.getChunkZ());
				case ZPLUS: return PS.valueOf(ps.getChunkX() + 1, ps.getChunkZ() + 1);
				case XMINUS: return PS.valueOf(ps.getChunkX(), ps.getChunkZ() + 1);
				case ZMINUS: return PS.valueOf(ps.getChunkX(), ps.getChunkZ());
			}
			throw new RuntimeException("say what");
		}

		/**
		 * Rotates this direction 90 degrees clockwise.
		 * 
		 * @return The direction after turning right
		 */
		public Direction turnRight()
		{
			return values()[(this.ordinal() + 1) % values().length];
		}

		/**
		 * Rotates this direction 180 degrees.
		 * 
		 * @return The opposite direction
		 */
		public Direction turnAround()
		{
			return this.turnRight().turnRight();
		}

		/**
		 * Rotates this direction 90 degrees counterclockwise.
		 * 
		 * @return The direction after turning left
		 */
		public Direction turnLeft()
		{
			return this.turnRight().turnRight().turnRight();
		}
	}

	/**
	 * Performs a flood fill to find all contiguous chunks starting from a given chunk.
	 * 
	 * <p>
	 * This separates large claimed territories into individual polygons. Chunks are removed
	 * from the source collection and added to the destination as they're discovered, ensuring
	 * each chunk is only processed once.
	 * </p>
	 * 
	 * @param source Collection of remaining unclaimed chunks (modified by removal)
	 * @param destination Collection to store discovered contiguous chunks (modified by addition)
	 * @param startChunk Starting point for the flood fill
	 */
	private void floodFillTarget(Collection<PS> source, Collection<PS> destination, PS startChunk)
	{
		// Create the deque
		ArrayDeque<PS> stack = new ArrayDeque<>();
		stack.push(startChunk);

		// And for each item in the queue
		while (!stack.isEmpty())
		{
			PS next = stack.pop();

			// If it is in the source
			// Remove it from there to avoid double-counting (and endless recursion)
			if (!source.remove(next)) continue;

			// Add to destination
			destination.add(next);

			// And look in adjacent chunks that are within the source
			Stream.of(Direction.values())
				.map(d -> d.adjacent(next))
				.filter(source::contains)
				.forEach(stack::push);
		}
	}

}
