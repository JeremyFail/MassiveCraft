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
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;
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
 * 
 * <p>
 * <strong>Algorithm:</strong>
 * </p>
 * <p>
 * The base idea was based on mikeprimm's Dynmap-Factions plugin, but has been heavily modified for better 
 * accuracy, supporting holes and multiple disconnected territories.
 * <ul>
 * <li>Uses 4-directional flood fill to group contiguous claimed chunks into separate polygons</li>
 * <li>Traces polygon outlines using a right-hand rule wall-following algorithm</li>
 * <li>Detects holes using 8-directional (diagonal-aware) flood fill to handle "kiddy corner" holes</li>
 * <li>Connects holes to outer boundary using shortest perpendicular (horizontal/vertical) paths</li>
 * <li>Creates single polygons with slits using the "Etch-a-Sketch" technique</li>
 * <li>Handles multiple disconnected territories per faction by creating separate markers</li>
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
		final Map<String, AreaMarkerValues> areas = createAreas();

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
	 * @param areas Pre-computed area marker values from async processing
	 */
	public void updateFactionsDynmap(Map<String, AreaMarkerValues> areas)
	{
		long before = System.currentTimeMillis();

		if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("async");

		if (!fetchDynmapAPI()) return;

		// Update territory layer
		if (!updateLayerTerritory(createLayerTerritory())) return;
		updateAreas(areas);
		
		// Update home warp layer if enabled
		if (MConf.get().dynmapShowHomeWarp)
		{
			if (updateLayerHome(createLayerHome()))
			{
				Map<String, MarkerValues> homeWarps = createHomeWarps();
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
		if (MConf.get().dynmapShowOtherWarps)
		{
			if (updateLayerWarps(createLayerWarps()))
			{
				Map<String, MarkerValues> otherWarps = createOtherWarps();
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
	public LayerValues createLayerTerritory()
	{
		return new LayerValues(
			MConf.get().dynmapLayerName,
			MConf.get().dynmapLayerMinimumZoom,
			MConf.get().dynmapLayerPriority,
			MConf.get().dynmapLayerHiddenByDefault
		);
	}
	
	// Thread Safe / Asynchronous: No
	public boolean updateLayerTerritory(LayerValues temp)
	{
		this.markersetTerritory = temp.ensureExistsAndUpdated(this.markerApi, IntegrationDynmap.FACTIONS_MARKERSET_TERRITORY);
		return this.markersetTerritory != null;
	}
	
	// ========== HOME WARP LAYER ==========
	
	// Thread Safe / Asynchronous: Yes
	public LayerValues createLayerHome()
	{
		return new LayerValues(
			MConf.get().dynmapLayerNameHome,
			MConf.get().dynmapLayerMinimumZoomHome,
			MConf.get().dynmapLayerPriorityHome,
			MConf.get().dynmapLayerHiddenByDefaultHome
		);
	}
	
	// Thread Safe / Asynchronous: No
	public boolean updateLayerHome(LayerValues temp)
	{
		this.markersetHome = temp.ensureExistsAndUpdated(this.markerApi, IntegrationDynmap.FACTIONS_MARKERSET_HOME);
		return this.markersetHome != null;
	}
	
	// ========== OTHER WARPS LAYER ==========
	
	// Thread Safe / Asynchronous: Yes
	public LayerValues createLayerWarps()
	{
		return new LayerValues(
			MConf.get().dynmapLayerNameWarps,
			MConf.get().dynmapLayerMinimumZoomWarps,
			MConf.get().dynmapLayerPriorityWarps,
			MConf.get().dynmapLayerHiddenByDefaultWarps
		);
	}
	
	// Thread Safe / Asynchronous: No
	public boolean updateLayerWarps(LayerValues temp)
	{
		this.markersetWarps = temp.ensureExistsAndUpdated(this.markerApi, IntegrationDynmap.FACTIONS_MARKERSET_WARPS);
		return this.markersetWarps != null;
	}

	// -------------------------------------------- //
	// UPDATE: HOME WARPS
	// -------------------------------------------- //
	
	// Thread Safe: YES
	public Map<String, MarkerValues> createHomeWarps()
	{
		Map<String, MarkerValues> ret = new MassiveMap<>();
		
		// For each faction
		for (Faction faction : FactionColl.get().getAll())
		{
			// For each warp
			for (Warp warp : faction.getWarps().getAll())
			{
				// Only process home warps
				if (!"home".equalsIgnoreCase(warp.getName())) continue;
				
				PS location = warp.getLocation();
				if (location == null) continue;
				
				String world = location.getWorld();
				if (world == null) continue;
				
				// Check visibility for this world
				if (!isVisible(faction, world)) continue;
				
				// Create label
				String label = faction.getName() + " - Home";
				
				// Create description
				String description = "<b>" + faction.getName() + "</b><br/>Home";
				
				// Create marker
				MarkerValues marker = new MarkerValues(
					label,
					world,
					location.getLocationX(),
					location.getLocationY(),
					location.getLocationZ(),
					MConf.get().dynmapWarpHomeIcon,
					description
				);
				
				// Generate unique ID
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
	 * @param values Map of marker IDs to marker values for all current home warps
	 */
	// Thread Safe: NO
	public void updateHomeWarps(Map<String, MarkerValues> values)
	{
		// Cleanup old home markers
		this.markersetHome.getMarkers().stream()
			.filter(m -> m.getMarkerID().startsWith("factions_home_"))
			.filter(m -> !values.containsKey(m.getMarkerID()))
			.forEach(Marker::deleteMarker);
		
		// Map current markers
		Map<String, Marker> markers = getHomeMarkerMap(this.markersetHome);
		
		// Create or update markers
		values.forEach((markerId, value) ->
			value.ensureExistsAndUpdated(markers.get(markerId), this.markerApi, this.markersetHome, markerId));
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
	public Map<String, MarkerValues> createOtherWarps()
	{
		Map<String, MarkerValues> ret = new MassiveMap<>();
		
		// For each faction
		for (Faction faction : FactionColl.get().getAll())
		{
			// For each warp
			for (Warp warp : faction.getWarps().getAll())
			{
				// Skip home warp (only process other warps)
				if ("home".equalsIgnoreCase(warp.getName())) continue;
				
				PS location = warp.getLocation();
				if (location == null) continue;
				
				String world = location.getWorld();
				if (world == null) continue;
				
				// Check visibility for this world
				if (!isVisible(faction, world)) continue;
				
				// Create label
				String label = faction.getName() + " - " + warp.getName();
				
				// Create description
				String description = "<b>" + faction.getName() + "</b><br/>Warp: " + warp.getName();
				
				// Create marker
				MarkerValues marker = new MarkerValues(
					label,
					world,
					location.getLocationX(),
					location.getLocationY(),
					location.getLocationZ(),
					MConf.get().dynmapWarpOtherIcon,
					description
				);
				
				// Generate unique ID
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
	 * @param values Map of marker IDs to marker values for all current non-home warps
	 */
	// Thread Safe: NO
	public void updateOtherWarps(Map<String, MarkerValues> values)
	{
		// Cleanup old warp markers
		this.markersetWarps.getMarkers().stream()
			.filter(m -> m.getMarkerID().startsWith("factions_warp_"))
			.filter(m -> !values.containsKey(m.getMarkerID()))
			.forEach(Marker::deleteMarker);
		
		// Map current markers
		Map<String, Marker> markers = getOtherWarpsMarkerMap(this.markersetWarps);
		
		// Create or update markers
		values.forEach((markerId, value) ->
			value.ensureExistsAndUpdated(markers.get(markerId), this.markerApi, this.markersetWarps, markerId));
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
	public Map<String, AreaMarkerValues> createAreas()
	{
		Map<String, Map<Faction, Set<PS>>> worldFactionChunks = BoardColl.get().getWorldToFactionToChunks(false);
		return createAreas(worldFactionChunks);

	}
	
	// Thread Safe: YES
	public Map<String, AreaMarkerValues> createAreas(Map<String, Map<Faction, Set<PS>>> worldFactionChunks)
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
	 * @return Map of marker IDs to area marker values
	 */
	public Map<String, AreaMarkerValues> createAreas(Entry<String, Map<Faction, Set<PS>>> superEntry)
	{
		return createAreas(superEntry.getKey(), superEntry.getValue());
	}

	/**
	 * Creates area markers for all factions in a world.
	 * 
	 * @param world The world name
	 * @param map Mapping of factions to their claimed chunks
	 * @return Map of marker IDs to area marker values
	 */
	public Map<String, AreaMarkerValues> createAreas(String world, Map<Faction, Set<PS>> map)
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
	 * @return Map of marker IDs to area marker values
	 */
	public Map<String, AreaMarkerValues> createAreas(String world, Entry<Faction, Set<PS>> entry)
	{
		return createAreas(world, entry.getKey(), entry.getValue());
	}

	public Map<String, AreaMarkerValues> createAreas(String world, Faction faction, Set<PS> chunks)
	{
		// If the faction is visible ...
		if (!isVisible(faction, world)) return Collections.emptyMap();

		// ... and has any chunks ...
		if (chunks.isEmpty()) return Collections.emptyMap();

		Map<String, AreaMarkerValues> ret = new MassiveMap<>();

		// Get info
		String description = getDescription(faction);
		DynmapStyle style = this.getStyle(faction);
		
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

			// Get polygon with holes (outer boundary + any interior holes)
			List<List<PS>> polygonWithHoles = getPolygonWithHoles(polygonChunks);
			
			// If there are holes, connect them with slits to create a single polygon
			List<PS> finalPolygon;
			if (polygonWithHoles.size() > 1)
			{
				finalPolygon = createPolygonWithSlits(polygonWithHoles, polygonChunks);
			}
			else
			{
				finalPolygon = polygonWithHoles.get(0);
			}
			
			PS[] corners = finalPolygon.toArray(new PS[0]);

			// Build information for specific area
			String markerId = calcMarkerId(world, faction);
			AreaMarkerValues values = new AreaMarkerValues(faction.getName(), world, corners, description, style);
			ret.put(markerId, values);
		}
		
		return ret;
	}

	/**
	 * Finds the chunk with the minimum X and Z coordinates.
	 * Used as the starting point for polygon tracing.
	 * 
	 * @param pss Collection of chunks to search
	 * @return The chunk at the minimum (X, Z) position
	 */
	private static PS getMinimum(Collection<PS> pss)
	{
		int minimumX = Integer.MAX_VALUE;
		int minimumZ = Integer.MAX_VALUE;

		for (PS chunk : pss)
		{
			int chunkX = chunk.getChunkX();
			int chunkZ = chunk.getChunkZ();

			if (chunkX < minimumX)
			{
				minimumX = chunkX;
				minimumZ = chunkZ;
			}
			else if (chunkX == minimumX && chunkZ < minimumZ)
			{
				minimumZ = chunkZ;
			}
		}
		return PS.valueOf(minimumX, minimumZ);
	}

	/**
	 * Gets a polygon with holes, including the outer boundary and any interior holes.
	 * 
	 * <p>
	 * This method traces the outer boundary of claimed chunks, then searches for any
	 * unclaimed chunks completely surrounded by claimed chunks (holes). Each hole's
	 * boundary is traced counterclockwise (opposite of the outer boundary).
	 * </p>
	 * 
	 * <p>
	 * <strong>Algorithm:</strong>
	 * <ol>
	 * <li>Trace outer boundary using wall-following algorithm</li>
	 * <li>Find all unclaimed chunks within bounding box</li>
	 * <li>Group unclaimed chunks using diagonal-aware flood fill (kiddy-corner chunks = same hole)</li>
	 * <li>Filter out groups that touch the bounding box edge (these connect to the exterior)</li>
	 * <li>Trace boundary for each remaining hole group</li>
	 * </ol>
	 * </p>
	 * 
	 * @param polygonChunks Set of contiguous claimed chunks
	 * @return List where [0] is outer boundary, [1+] are holes (if any)
	 */
	private static List<List<PS>> getPolygonWithHoles(Set<PS> polygonChunks)
	{
		List<List<PS>> result = new MassiveList<>();
		
		// 1. Trace outer boundary (clockwise)
		List<PS> outerBoundary = getLineList(polygonChunks);
		result.add(outerBoundary);
		
		// 2. Find bounding box to search for holes
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		for (PS chunk : polygonChunks)
		{
			int x = chunk.getChunkX();
			int z = chunk.getChunkZ();
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (z < minZ) minZ = z;
			if (z > maxZ) maxZ = z;
		}
		
		// 3. Find all unclaimed chunks within the bounding box
		Set<PS> potentialHoles = new MassiveSet<>();
		for (int x = minX; x <= maxX; x++)
		{
			for (int z = minZ; z <= maxZ; z++)
			{
				PS chunk = PS.valueOf(x, z);
				if (!polygonChunks.contains(chunk))
				{
					potentialHoles.add(chunk);
				}
			}
		}
		
		// 4. Group unclaimed chunks into separate holes using DIAGONAL-AWARE flood fill
		// This treats kiddy-corner chunks as part of the same hole
		while (!potentialHoles.isEmpty())
		{
			Set<PS> hole = new MassiveSet<>();
			PS start = potentialHoles.iterator().next();
			floodFillDiagonal(potentialHoles, hole, start);
			
			// Check if this hole touches the bounding box edge (not a real hole - it connects to outside)
			if (touchesBoundary(hole, minX, maxX, minZ, maxZ))
			{
				continue;
			}
			
			// This is a true hole - trace its boundary
			List<PS> holeBoundary = getLineListForHole(hole, polygonChunks);
			result.add(holeBoundary);
		}
		
		return result;
	}
	
	/**
	 * Flood fill that includes diagonal neighbors (8-directional).
	 * 
	 * <p>
	 * This treats "kiddy corner" chunks as connected, so two unclaimed chunks
	 * that only touch at a corner are considered part of the same hole.
	 * </p>
	 * 
	 * @param source Collection of remaining unclaimed chunks (modified by removal)
	 * @param destination Collection to store discovered contiguous unclaimed chunks
	 * @param start Starting point for the flood fill
	 */
	private static void floodFillDiagonal(Set<PS> source, Set<PS> destination, PS start)
	{
		ArrayDeque<PS> stack = new ArrayDeque<>();
		stack.push(start);
		
		while (!stack.isEmpty())
		{
			PS next = stack.pop();
			if (!source.remove(next)) continue;
			
			destination.add(next);
			
			int x = next.getChunkX();
			int z = next.getChunkZ();
			
			// Check all 8 neighbors (orthogonal + diagonal)
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dz = -1; dz <= 1; dz++)
				{
					if (dx == 0 && dz == 0) continue; // Skip self
					
					PS adjacent = PS.valueOf(x + dx, z + dz);
					if (source.contains(adjacent))
					{
						stack.push(adjacent);
					}
				}
			}
		}
	}
	
	/**
	 * Creates a single polygon with slits connecting to holes using the "Etch-a-Sketch" algorithm.
	 * 
	 * <p>
	 * <strong>New Clean Algorithm:</strong>
	 * <ol>
	 * <li>Build a complete map of all outer boundary edges (horizontal and vertical segments)</li>
	 * <li>For each hole, find ALL its corners and check perpendicular distances to ALL outer edges</li>
	 * <li>Select the shortest perpendicular connection (must be horizontal or vertical only)</li>
	 * <li>Build the final polygon by walking the outer boundary and inserting slits at the right places</li>
	 * </ol>
	 * </p>
	 * 
	 * @param polygonWithHoles List where [0] is outer boundary, [1+] are holes
	 * @param claimedChunks Set of claimed chunks (unused in new algorithm but kept for API compatibility)
	 * @return Single polygon with holes connected via slits
	 */
	private static List<PS> createPolygonWithSlits(List<List<PS>> polygonWithHoles, Set<PS> claimedChunks)
	{
		List<PS> outer = polygonWithHoles.get(0);
		
		// If no holes, just return the outer boundary
		if (polygonWithHoles.size() == 1)
		{
			return new MassiveList<>(outer);
		}
		
		// Build lists of horizontal and vertical edges from the outer boundary
		List<Edge> outerEdges = buildEdgeList(outer);
		
		// For each hole, find the best connection point to the outer boundary
		List<SlitConnection> connections = new MassiveList<>();
		
		for (int holeIdx = 1; holeIdx < polygonWithHoles.size(); holeIdx++)
		{
			List<PS> hole = polygonWithHoles.get(holeIdx);
			if (hole.isEmpty()) continue;
			
			// Find the shortest perpendicular connection from this hole to the outer boundary
			SlitConnection connection = findShortestPerpendicularConnection(outer, outerEdges, hole);
			
			if (connection == null)
			{
				EngineDynmap.logSevere("Failed to find connection for hole at " + hole.get(0) + " - skipping hole");
				continue;
			}
			
			connection.holeIndex = holeIdx;
			connections.add(connection);
		}
		
		// Sort connections by their position on the outer boundary
		// This ensures we insert them in order as we traverse the boundary
		connections.sort((a, b) -> {
			// First compare by edge index
			int cmp = Integer.compare(a.outerEdgeIndex, b.outerEdgeIndex);
			if (cmp != 0) return cmp;
			// Then by distance along the edge from the start corner
			return Double.compare(a.distanceAlongEdge, b.distanceAlongEdge);
		});
		
		// Build the result using Etch-a-Sketch approach
		return buildPolygonWithSlits(outer, polygonWithHoles, connections);
	}
	
	/**
	 * Represents an edge (line segment) of the polygon boundary.
	 */
	private static class Edge
	{
		PS start;       // Starting corner
		PS end;         // Ending corner
		int edgeIndex;  // Index of the starting corner in the boundary list
		boolean isHorizontal; // true if horizontal (same Z), false if vertical (same X)
		
		Edge(PS start, PS end, int edgeIndex)
		{
			this.start = start;
			this.end = end;
			this.edgeIndex = edgeIndex;
			this.isHorizontal = (start.getChunkZ() == end.getChunkZ());
		}
		
		/**
		 * Checks if a perpendicular line from the given point intersects this edge.
		 * Returns the intersection point if it does, null otherwise.
		 * 
		 * @param point The point to check from
		 * @param horizontal If true, check horizontal line from point; if false, check vertical
		 * @return The intersection point on this edge, or null if no intersection
		 */
		PS getPerpendicularIntersection(PS point, boolean horizontal)
		{
			int px = point.getChunkX();
			int pz = point.getChunkZ();
			
			if (horizontal)
			{
				// We're drawing a horizontal line from the point
				// This can only intersect a vertical edge
				if (this.isHorizontal) return null;
				
				int edgeX = this.start.getChunkX();
				int minZ = Math.min(this.start.getChunkZ(), this.end.getChunkZ());
				int maxZ = Math.max(this.start.getChunkZ(), this.end.getChunkZ());
				
				// Check if point's Z is within the edge's Z range
				if (pz >= minZ && pz <= maxZ)
				{
					return PS.valueOf(edgeX, pz);
				}
			}
			else
			{
				// We're drawing a vertical line from the point
				// This can only intersect a horizontal edge
				if (!this.isHorizontal) return null;
				
				int edgeZ = this.start.getChunkZ();
				int minX = Math.min(this.start.getChunkX(), this.end.getChunkX());
				int maxX = Math.max(this.start.getChunkX(), this.end.getChunkX());
				
				// Check if point's X is within the edge's X range
				if (px >= minX && px <= maxX)
				{
					return PS.valueOf(px, edgeZ);
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Builds a list of edges from a polygon boundary.
	 */
	private static List<Edge> buildEdgeList(List<PS> boundary)
	{
		List<Edge> edges = new MassiveList<>();
		for (int i = 0; i < boundary.size(); i++)
		{
			PS start = boundary.get(i);
			PS end = boundary.get((i + 1) % boundary.size());
			edges.add(new Edge(start, end, i));
		}
		return edges;
	}
	
	/**
	 * Finds the shortest perpendicular connection from a hole to the outer boundary.
	 * 
	 * <p>
	 * For each corner of the hole, we check:
	 * <ul>
	 * <li>Horizontal line left (negative X direction)</li>
	 * <li>Horizontal line right (positive X direction)</li>
	 * <li>Vertical line up (negative Z direction)</li>
	 * <li>Vertical line down (positive Z direction)</li>
	 * </ul>
	 * We find where each of these lines first intersects an outer edge, and pick the shortest.
	 * </p>
	 */
	private static SlitConnection findShortestPerpendicularConnection(List<PS> outer, List<Edge> outerEdges, List<PS> hole)
	{
		SlitConnection best = null;
		int bestDistance = Integer.MAX_VALUE;
		
		// For each corner of the hole
		for (int holeCornerIdx = 0; holeCornerIdx < hole.size(); holeCornerIdx++)
		{
			PS holeCorner = hole.get(holeCornerIdx);
			int hx = holeCorner.getChunkX();
			int hz = holeCorner.getChunkZ();
			
			// Check all 4 directions
			for (int dir = 0; dir < 4; dir++)
			{
				boolean horizontal = (dir < 2); // 0,1 = horizontal; 2,3 = vertical
				boolean positive = (dir % 2 == 1); // 1,3 = positive direction
				
				// Find the closest edge intersection in this direction
				PS closestIntersection = null;
				int closestDistance = Integer.MAX_VALUE;
				Edge closestEdge = null;
				
				for (Edge edge : outerEdges)
				{
					PS intersection = edge.getPerpendicularIntersection(holeCorner, horizontal);
					if (intersection == null) continue;
					
					int ix = intersection.getChunkX();
					int iz = intersection.getChunkZ();
					
					// Calculate distance and check direction
					int distance;
					boolean correctDirection;
					
					if (horizontal)
					{
						distance = Math.abs(ix - hx);
						correctDirection = positive ? (ix > hx) : (ix < hx);
					}
					else
					{
						distance = Math.abs(iz - hz);
						correctDirection = positive ? (iz > hz) : (iz < hz);
					}
					
					if (correctDirection && distance > 0 && distance < closestDistance)
					{
						closestDistance = distance;
						closestIntersection = intersection;
						closestEdge = edge;
					}
				}
				
				// If we found an intersection and it's the best so far
				if (closestIntersection != null && closestDistance < bestDistance)
				{
					bestDistance = closestDistance;
					best = new SlitConnection();
					best.holePoint = holeCorner;
					best.holePointIndex = holeCornerIdx;
					best.outerPoint = closestIntersection;
					best.outerEdgeIndex = closestEdge.edgeIndex;
					
					// Calculate distance along the edge from the start corner
					if (closestEdge.isHorizontal)
					{
						best.distanceAlongEdge = Math.abs(closestIntersection.getChunkX() - closestEdge.start.getChunkX());
					}
					else
					{
						best.distanceAlongEdge = Math.abs(closestIntersection.getChunkZ() - closestEdge.start.getChunkZ());
					}
					
					// Check if the intersection point is exactly at a corner
					best.isOnEdge = !outer.contains(closestIntersection);
					if (!best.isOnEdge)
					{
						// Find the corner index
						best.outerIndex = outer.indexOf(closestIntersection);
					}
					else
					{
						best.outerIndex = closestEdge.edgeIndex;
					}
				}
			}
		}
		
		return best;
	}
	
	/**
	 * Builds the final polygon by walking the outer boundary and inserting slits to holes.
	 */
	private static List<PS> buildPolygonWithSlits(List<PS> outer, List<List<PS>> polygonWithHoles, List<SlitConnection> connections)
	{
		List<PS> result = new MassiveList<>();
		int connectionIdx = 0;
		
		for (int i = 0; i < outer.size(); i++)
		{
			PS currentCorner = outer.get(i);
			
			// Add the current corner
			result.add(currentCorner);
			
			// Process any connections that start from this corner (not on edge)
			while (connectionIdx < connections.size())
			{
				SlitConnection conn = connections.get(connectionIdx);
				
				// Check if this connection is on the current edge or at the current corner
				if (conn.outerEdgeIndex != i && conn.outerIndex != i)
				{
					break;
				}
				
				// If connection is at a corner (current corner specifically)
				if (!conn.isOnEdge && conn.outerIndex == i)
				{
					// Draw slit to hole, around hole, and back
					result.addAll(drawSlitToHole(conn, polygonWithHoles));
					connectionIdx++;
					continue;
				}
				
				// If connection is on this edge
				if (conn.isOnEdge && conn.outerEdgeIndex == i)
				{
					// Draw to the edge point
					result.add(conn.outerPoint);
					
					// Draw slit to hole, around hole, and back
					result.addAll(drawSlitToHole(conn, polygonWithHoles));
					
					// Draw back to the edge point (already there, so just continue)
					// The next corner will be added in the next iteration
					connectionIdx++;
					continue;
				}
				
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Draws a slit from the current position to a hole, around the hole, and back.
	 * Returns the points to add (not including the starting point).
	 */
	private static List<PS> drawSlitToHole(SlitConnection conn, List<List<PS>> polygonWithHoles)
	{
		List<PS> result = new MassiveList<>();
		List<PS> hole = polygonWithHoles.get(conn.holeIndex);
		
		// Draw to hole entry point
		result.add(conn.holePoint);
		
		// Trace around the hole, starting from the connection point
		List<PS> rotatedHole = rotateToStart(hole, conn.holePointIndex);
		
		// Add all hole points except the first (which we just added)
		for (int i = 1; i < rotatedHole.size(); i++)
		{
			result.add(rotatedHole.get(i));
		}
		
		// Return to the entry point to complete the hole
		result.add(conn.holePoint);
		
		// If we came from an edge point, add that too
		if (conn.isOnEdge)
		{
			result.add(conn.outerPoint);
		}
		
		return result;
	}
	
	/**
	 * Rotates a list so that the element at the specified index becomes the first element.
	 * The list is treated as circular.
	 * 
	 * @param list The list to rotate
	 * @param startIndex The index that should become the first element
	 * @return A new list with the elements rotated
	 */
	private static List<PS> rotateToStart(List<PS> list, int startIndex)
	{
		if (list.isEmpty() || startIndex == 0) return new MassiveList<>(list);
		
		List<PS> result = new MassiveList<>();
		for (int i = 0; i < list.size(); i++)
		{
			result.add(list.get((startIndex + i) % list.size()));
		}
		return result;
	}
	


	/**
	 * Helper class to store information about a slit connection between boundary and hole.
	 */
	private static class SlitConnection
	{
		PS outerPoint;
		int outerIndex; // Index of the corner (if not on edge)
		int outerEdgeIndex; // Index of the edge (for sorting when on edge)
		double distanceAlongEdge; // Distance from edge start to the connection point
		boolean isOnEdge; // True if outerPoint is on an edge, not at a corner
		PS holePoint;
		int holePointIndex;
		int holeIndex;
	}
	
	/**
	 * Check if an unclaimed area touches the bounding box edge (not a true hole).
	 * 
	 * @param chunks Unclaimed chunks to check
	 * @param minX Minimum X coordinate of bounding box
	 * @param maxX Maximum X coordinate of bounding box
	 * @param minZ Minimum Z coordinate of bounding box
	 * @param maxZ Maximum Z coordinate of bounding box
	 * @return true if the chunks touch the edge
	 */
	private static boolean touchesBoundary(Set<PS> chunks, int minX, int maxX, int minZ, int maxZ)
	{
		for (PS chunk : chunks)
		{
			int x = chunk.getChunkX();
			int z = chunk.getChunkZ();
			if (x == minX || x == maxX || z == minZ || z == maxZ)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Traces the boundary of a hole (counterclockwise).
	 * 
	 * <p>
	 * For holes, we need to trace counterclockwise (opposite of outer boundary).
	 * We use the same algorithm as getLineList but treating the hole chunks as "solid"
	 * and tracing around them, which naturally produces a counterclockwise path.
	 * </p>
	 * 
	 * @param holeChunks Unclaimed chunks forming the hole
	 * @param claimed Set of claimed chunks surrounding the hole (unused but kept for clarity)
	 * @return List of corner positions forming the hole's outline
	 */
	private static List<PS> getLineListForHole(Set<PS> holeChunks, Set<PS> claimed)
	{
		// Trace the hole boundary by treating hole chunks as the "solid" area
		// This produces a counterclockwise path around the hole
		PS minimumChunk = getMinimum(holeChunks);

		int initialX = minimumChunk.getChunkX();
		int initialZ = minimumChunk.getChunkZ();
		int currentX = initialX;
		int currentZ = initialZ;

		Direction direction = Direction.XPLUS;
		List<PS> linelist = new MassiveList<>();

		linelist.add(minimumChunk); // Add start point
		
		// Add safety counter to prevent infinite loops
		int iterations = 0;
		int maxIterations = holeChunks.size() * 4 + 100; // Reasonable upper bound
		
		while (((currentX != initialX) || (currentZ != initialZ) || (direction != Direction.ZMINUS)) && iterations < maxIterations)
		{
			iterations++;
			
			switch (direction)
			{
				case XPLUS: // Segment in X+ Direction
					if (!holeChunks.contains(PS.valueOf(currentX + 1, currentZ)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX + 1, currentZ)); // Finish line
						direction = Direction.ZPLUS; // Change direction
					}
					else if (!holeChunks.contains(PS.valueOf(currentX + 1, currentZ - 1)))
					{ // Straight?
						currentX++;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX + 1, currentZ)); // Finish line
						direction = Direction.ZMINUS;
						currentX++;
						currentZ--;
					}
					break;
				case ZPLUS: // Segment in Z+ Direction
					if (!holeChunks.contains(PS.valueOf(currentX, currentZ + 1)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX + 1, currentZ + 1)); // Finish line
						direction = Direction.XMINUS; // Change direction
					}
					else if (!holeChunks.contains(PS.valueOf(currentX + 1, currentZ + 1)))
					{ // Straight?
						currentZ++;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX + 1, currentZ + 1)); // Finish line
						direction = Direction.XPLUS;
						currentX++;
						currentZ++;
					}
					break;
				case XMINUS: // Segment in X- Direction
					if (!holeChunks.contains(PS.valueOf(currentX - 1, currentZ)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX, currentZ + 1)); // Finish line
						direction = Direction.ZMINUS; // Change direction
					}
					else if (!holeChunks.contains(PS.valueOf(currentX - 1, currentZ + 1)))
					{ // Straight?
						currentX--;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX, currentZ + 1)); // Finish line
						direction = Direction.ZPLUS;
						currentX--;
						currentZ++;
					}
					break;
				case ZMINUS: // Segment in Z- Direction
					if (!holeChunks.contains(PS.valueOf(currentX, currentZ - 1)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX, currentZ)); // Finish line
						direction = Direction.XPLUS; // Change direction
					}
					else if (!holeChunks.contains(PS.valueOf(currentX - 1, currentZ - 1)))
					{ // Straight?
						currentZ--;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX, currentZ)); // Finish line
						direction = Direction.XMINUS;
						currentX--;
						currentZ--;
					}
					break;
			}
		}
		
		if (iterations >= maxIterations)
		{
			EngineDynmap.logSevere("getLineListForHole exceeded maximum iterations - possible infinite loop detected for hole at " + minimumChunk);
		}

		return linelist;
	}
	
	/**
	 * Traces the outline of a polygon of claimed chunks using a right-hand rule wall-following algorithm.
	 * 
	 * <p>
	 * This algorithm walks clockwise around the perimeter of the claimed territory, starting from
	 * the minimum (X, Z) position. At each step, it checks three directions:
	 * <ul>
	 * <li>Right turn: Adjacent chunk in current direction is unclaimed</li>
	 * <li>Straight: Adjacent chunk is claimed, but diagonal-left is unclaimed</li>
	 * <li>Left turn: Both adjacent and diagonal-left chunks are claimed</li>
	 * </ul>
	 * </p>
	 * 
	 * <p>
	 * The result is a list of corner coordinates that form the polygon boundary, suitable for
	 * rendering on Dynmap.
	 * </p>
	 * 
	 * @param polygonChunks Set of contiguous claimed chunks to trace
	 * @return List of corner positions forming the polygon outline
	 */
	private static List<PS> getLineList(Set<PS> polygonChunks)
	{
		PS minimumChunk = getMinimum(polygonChunks);

		int initialX = minimumChunk.getChunkX();
		int initialZ = minimumChunk.getChunkZ();
		int currentX = initialX;
		int currentZ = initialZ;

		Direction direction = Direction.XPLUS;
		List<PS> linelist = new MassiveList<>();

		linelist.add(minimumChunk); // Add start point
		
		while ((currentX != initialX) || (currentZ != initialZ) || (direction != Direction.ZMINUS))
		{
			switch (direction)
			{
				case XPLUS: // Segment in X+ Direction
					if (!polygonChunks.contains(PS.valueOf(currentX + 1, currentZ)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX + 1, currentZ)); // Finish line
						direction = Direction.ZPLUS; // Change direction
					}
					else if (!polygonChunks.contains(PS.valueOf(currentX + 1, currentZ - 1)))
					{ // Straight?
						currentX++;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX + 1, currentZ)); // Finish line
						direction = Direction.ZMINUS;
						currentX++;
						currentZ--;
					}
					break;
				case ZPLUS: // Segment in Z+ Direction
					if (!polygonChunks.contains(PS.valueOf(currentX, currentZ + 1)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX + 1, currentZ + 1)); // Finish line
						direction = Direction.XMINUS; // Change direction
					}
					else if (!polygonChunks.contains(PS.valueOf(currentX + 1, currentZ + 1)))
					{ // Straight?
						currentZ++;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX + 1, currentZ + 1)); // Finish line
						direction = Direction.XPLUS;
						currentX++;
						currentZ++;
					}
					break;
				case XMINUS: // Segment in X- Direction
					if (!polygonChunks.contains(PS.valueOf(currentX - 1, currentZ)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX, currentZ + 1)); // Finish line
						direction = Direction.ZMINUS; // Change direction
					}
					else if (!polygonChunks.contains(PS.valueOf(currentX - 1, currentZ + 1)))
					{ // Straight?
						currentX--;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX, currentZ + 1)); // Finish line
						direction = Direction.ZPLUS;
						currentX--;
						currentZ++;
					}
					break;
				case ZMINUS: // Segment in Z- Direction
					if (!polygonChunks.contains(PS.valueOf(currentX, currentZ - 1)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX, currentZ)); // Finish line
						direction = Direction.XPLUS; // Change direction
					}
					else if (!polygonChunks.contains(PS.valueOf(currentX - 1, currentZ - 1)))
					{ // Straight?
						currentZ--;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX, currentZ)); // Finish line
						direction = Direction.XMINUS;
						currentX--;
						currentZ--;
					}
					break;
			}
		}

		return linelist;
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
	 * @param values Map of marker IDs to area marker values for all current territories
	 */
	// Thread Safe: NO
	public void updateAreas(Map<String, AreaMarkerValues> values)
	{
		// Cleanup old markers
		this.markersetTerritory.getAreaMarkers().stream() // Get current markers
			.filter(am -> !values.containsKey(am.getMarkerID())) // That are not in the new map
			.forEach(AreaMarker::deleteMarker); // and delete them


		// Map Current
		Map<String, AreaMarker> markers = getMarkerMap(this.markersetTerritory);

		// Loop New
		values.forEach((markerId, value) ->
						  value.ensureExistsAndUpdated(markers.get(markerId), this.markerApi, this.markersetTerritory, markerId));

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
		String ret = "<div class=\"regioninfo\">" + MConf.get().dynmapDescriptionWindowFormat + "</div>";
		
		// Name
		String name = faction.getName();
		ret = DynmapUtil.addToHtml(ret, "name", name);
		
		// Description
		String description = faction.getDescriptionDesc();
		ret = DynmapUtil.addToHtml(ret, "description", description);

		// MOTD (probably shouldn't be shown but if the server owner specifies it, I don't care)
		String motd = faction.getMotd();
		if (motd != null) ret = DynmapUtil.addToHtml(ret, "motd", motd);
		
		// Age
		long ageMillis = faction.getAge();
		LinkedHashMap<TimeUnit, Long> ageUnitcounts = TimeDiffUtil.limit(TimeDiffUtil.unitcounts(ageMillis, TimeUnit.getAllButMillisSecondsAndMinutes()), 3);
		String age = TimeDiffUtil.formatedVerboose(ageUnitcounts);
		ret = DynmapUtil.addToHtml(ret, "age", age);
		
		// Money
		String money;
		if (Econ.isEnabled() && MConf.get().dynmapShowMoneyInDescription)
		{
			if (faction.isNormal())
			{
				money = Money.format(Econ.getMoney(faction));
			}
			else
			{
				money = "N/A";
			}
		}
		else
		{
			money = "unavailable";
		}
		ret = DynmapUtil.addToHtml(ret, "money", money);
		
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
			String color = DynmapUtil.calcBoolcolor(flagName, value);
			String boolcolor = DynmapUtil.calcBoolcolor(String.valueOf(value), value);
			
			ret = ret.replace("%" + flagName + ".bool%", bool); // true
			ret = ret.replace("%" + flagName + ".color%", color); // monsters (red or green)
			ret = ret.replace("%" + flagName + ".boolcolor%", boolcolor); // true (red or green)

			flagMapParts.add(flagName + ": " + boolcolor);
			flagTableParts.add(color);
		}
		
		String flagMap = Txt.implode(flagMapParts, "<br>\n");
		ret = ret.replace("%flags.map%", flagMap);

		// The server can specify the wished number of columns
		// So we loop over the possibilities
		for (int cols = 1; cols <= 10; cols++)
		{
			String flagTable = DynmapUtil.getHtmlAsciTable(flagTableParts, cols);
			ret = ret.replace("%flags.table" + cols + "%", flagTable);
		}
		
		// Players
		List<MPlayer> playersList = faction.getMPlayers();
		String playersCount = String.valueOf(playersList.size());
		String players = DynmapUtil.getHtmlPlayerString(playersList);
		
		MPlayer playersLeaderObject = faction.getLeader();
		String playersLeader = DynmapUtil.getHtmlPlayerName(playersLeaderObject);

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

		final String factionId = faction.getId();
		final String factionName = faction.getName();
		final String worldId =  "world:" + world;

		Set<String> ids = MUtil.set(factionId, factionName, worldId);

		if (factionId == null) throw new NullPointerException("faction id");
		if (factionName == null) throw new NullPointerException("faction name");
		
		Set<String> visible = MConf.get().dynmapVisibleFactions;
		Set<String> hidden = MConf.get().dynmapHiddenFactions;

		if (!visible.isEmpty() && visible.stream().noneMatch(ids::contains))
		{
			return false;
		}

		if (!hidden.isEmpty() && hidden.stream().anyMatch(ids::contains))
		{
			return false;
		}

		return true;
	}
	
	// Thread Safe / Asynchronous: Yes
	public DynmapStyle getStyle(Faction faction)
	{
		Map<String, DynmapStyle> styles = MConf.get().dynmapFactionStyleOverrides;
		
		// Priority 1: Check for admin override by faction ID or name
		DynmapStyle adminOverride = DynmapStyle.coalesce(
			styles.get(faction.getId()),
			styles.get(faction.getName())
		);
		if (adminOverride != null) return adminOverride;
		
		// Priority 2: Check for custom faction colors (if enabled)
		if (MConf.get().dynmapUseFactionColors)
		{
			String primaryColor = null;
			String secondaryColor = null;
			if (faction.hasPrimaryColor())
			{
				primaryColor = faction.getPrimaryColor();
			}
			if (faction.hasSecondaryColor())
			{
				secondaryColor = faction.getSecondaryColor();
			}

			return new DynmapStyle().withLineColor(secondaryColor).withFillColor(primaryColor);
		}
		
		// Priority 3: Use default style
		return MConf.get().dynmapDefaultStyle;
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
