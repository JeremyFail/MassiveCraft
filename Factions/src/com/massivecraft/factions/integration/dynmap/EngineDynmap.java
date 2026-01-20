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
import com.massivecraft.massivecore.apachecommons.StringEscapeUtils;
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
 * EngineDynmap handles the integration between Factions and the Dynmap plugin.
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
 * The base idea was based on mikeprimms plugin Dynmap-Factions, but has been modified for better accuracy
 * (supporting holes and multiple disconnected territories).
 * <ul>
 * <li>Uses flood fill to group contiguous claimed chunks into separate polygons</li>
 * <li>Traces polygon outlines using a right-hand rule wall-following algorithm</li>
 * <li>Handles multiple disconnected territories per faction by creating separate markers</li>
 * <li>Unclaimed chunks within claimed territory naturally appear as holes in the visualization</li>
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

	public Map<String, AreaMarkerValues> createAreas(Entry<String, Map<Faction, Set<PS>>> superEntry)
	{
		return createAreas(superEntry.getKey(), superEntry.getValue());
	}

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
				finalPolygon = createPolygonWithSlits(polygonWithHoles);
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
		
		// 4. Group unclaimed chunks into separate holes using flood fill
		while (!potentialHoles.isEmpty())
		{
			Set<PS> hole = new MassiveSet<>();
			PS start = potentialHoles.iterator().next();
			floodFillUnclaimed(potentialHoles, hole, start);
			
			// Check if this hole touches the outer boundary (not a real hole)
			if (!touchesBoundary(hole, minX, maxX, minZ, maxZ))
			{
				// Trace this hole's boundary (counterclockwise)
				List<PS> holeBoundary = getLineListForHole(hole, polygonChunks);
				result.add(holeBoundary);
			}
		}
		
		return result;
	}
	
	/**
	 * Creates a single polygon with slits connecting to holes.
	 * 
	 * <p>
	 * This technique creates a "corridor" from the outer boundary to each hole,
	 * allowing the polygon to wrap around the hole and return, forming a single
	 * contiguous polygon. The result is a proper visual representation of holes
	 * with only thin visual artifacts where the slits are.
	 * </p>
	 * 
	 * <p>
	 * Algorithm:
	 * <ol>
	 * <li>Start with outer boundary</li>
	 * <li>For each hole, find closest point on outer boundary to hole</li>
	 * <li>Insert a path: boundary → hole → around hole → back to boundary</li>
	 * <li>Continue with rest of boundary</li>
	 * </ol>
	 * </p>
	 * 
	 * @param polygonWithHoles List where [0] is outer boundary, [1+] are holes
	 * @return Single polygon with holes connected via slits
	 */
	private static List<PS> createPolygonWithSlits(List<List<PS>> polygonWithHoles)
	{
		List<PS> outer = polygonWithHoles.get(0);
		
		// If no holes, just return the outer boundary
		if (polygonWithHoles.size() == 1)
		{
			return new MassiveList<>(outer);
		}
		
		// For each hole, find the closest connection points
		List<SlitConnection> connections = new MassiveList<>();
		
		for (int holeIdx = 1; holeIdx < polygonWithHoles.size(); holeIdx++)
		{
			List<PS> hole = polygonWithHoles.get(holeIdx);
			if (hole.isEmpty()) continue;
			
			// Find closest point pair between outer boundary and this hole
			SlitConnection connection = findClosestPoints(outer, hole);
			connection.holeIndex = holeIdx;
			connections.add(connection);
		}
		
		// Sort connections by their position on the outer boundary
		// This ensures we insert them in order as we traverse the boundary
		connections.sort((a, b) -> Integer.compare(a.outerIndex, b.outerIndex));
		
		// Build the result by walking the outer boundary and inserting slits
		List<PS> result = new MassiveList<>();
		int connectionIdx = 0;
		
		for (int i = 0; i < outer.size(); i++)
		{
			result.add(outer.get(i));
			
			// Check if we need to insert a slit after this point
			while (connectionIdx < connections.size() && connections.get(connectionIdx).outerIndex == i)
			{
				SlitConnection conn = connections.get(connectionIdx);
				List<PS> hole = polygonWithHoles.get(conn.holeIndex);
				
				// Create the slit: current point → hole point → around hole → back
				result.add(conn.holePoint);
				
				// Add the hole, starting from the connection point
				// We need to rotate the hole so it starts at the connection point
				List<PS> rotatedHole = rotateToStart(hole, conn.holePointIndex);
				result.addAll(rotatedHole);
				
				// Close back to the connection point
				result.add(conn.holePoint);
				
				// Return to the outer boundary point
				result.add(conn.outerPoint);
				
				connectionIdx++;
			}
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
	 * Finds the closest point pair between an outer boundary and a hole.
	 * 
	 * @param outer The outer boundary polygon
	 * @param hole The hole polygon
	 * @return Connection information with the closest points and their indices
	 */
	private static SlitConnection findClosestPoints(List<PS> outer, List<PS> hole)
	{
		double minDistance = Double.MAX_VALUE;
		SlitConnection bestConnection = new SlitConnection();
		
		for (int outerIdx = 0; outerIdx < outer.size(); outerIdx++)
		{
			PS outerPoint = outer.get(outerIdx);
			
			for (int holeIdx = 0; holeIdx < hole.size(); holeIdx++)
			{
				PS holePoint = hole.get(holeIdx);
				
				double distance = calculateDistance(outerPoint, holePoint);
				
				if (distance < minDistance)
				{
					minDistance = distance;
					bestConnection.outerPoint = outerPoint;
					bestConnection.outerIndex = outerIdx;
					bestConnection.holePoint = holePoint;
					bestConnection.holePointIndex = holeIdx;
				}
			}
		}
		
		return bestConnection;
	}
	
	/**
	 * Calculates the Euclidean distance between two PS points.
	 * 
	 * @param a First point
	 * @param b Second point
	 * @return Distance between the points
	 */
	private static double calculateDistance(PS a, PS b)
	{
		double dx = a.getChunkX() - b.getChunkX();
		double dz = a.getChunkZ() - b.getChunkZ();
		return Math.sqrt(dx * dx + dz * dz);
	}
	
	/**
	 * Helper class to store information about a slit connection between boundary and hole.
	 */
	private static class SlitConnection
	{
		PS outerPoint;
		int outerIndex;
		PS holePoint;
		int holePointIndex;
		int holeIndex;
	}
	
	/**
	 * Flood fill for unclaimed chunks.
	 * 
	 * @param source Collection of remaining unclaimed chunks (modified by removal)
	 * @param destination Collection to store discovered contiguous unclaimed chunks
	 * @param start Starting point for the flood fill
	 */
	private static void floodFillUnclaimed(Set<PS> source, Set<PS> destination, PS start)
	{
		ArrayDeque<PS> stack = new ArrayDeque<>();
		stack.push(start);
		
		while (!stack.isEmpty())
		{
			PS next = stack.pop();
			if (!source.remove(next)) continue;
			
			destination.add(next);
			
			// Check all 4 adjacent chunks
			for (Direction dir : Direction.values())
			{
				PS adjacent = dir.adjacent(next);
				if (source.contains(adjacent))
				{
					stack.push(adjacent);
				}
			}
		}
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

	// This markerIndex, is if a faction has several claims in a single world
	private int markerIdx = 0;
	private String lastPartialMarkerId = "";
	public String calcMarkerId(String world, Faction faction)
	{
		// Calc current partial
		String partial = IntegrationDynmap.FACTIONS_AREA_ + world + "__" + faction.getId() + "__";

		// If different than last time, then reset the counter
		if (!partial.equals(lastPartialMarkerId)) markerIdx = 0;

		this.lastPartialMarkerId = partial;

		return partial + markerIdx++;
	}
	
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
		String ret = "<div class=\"regioninfo\">" + MConf.get().dynmapFactionDescription + "</div>";
		
		// Name
		String name = faction.getName();
		ret = addToHtml(ret, "name", name);
		
		// Description
		String description = faction.getDescriptionDesc();
		ret = addToHtml(ret, "description", description);

		// MOTD (probably shouldn't be shown but if the server owner specifies it, I don't care)
		String motd = faction.getMotd();
		if (motd != null) ret = addToHtml(ret, "motd", motd);
		
		// Age
		long ageMillis = faction.getAge();
		LinkedHashMap<TimeUnit, Long> ageUnitcounts = TimeDiffUtil.limit(TimeDiffUtil.unitcounts(ageMillis, TimeUnit.getAllButMillisSecondsAndMinutes()), 3);
		String age = TimeDiffUtil.formatedVerboose(ageUnitcounts);
		ret = addToHtml(ret, "age", age);
		
		// Money
		String money = "unavailable";
		if (Econ.isEnabled() && MConf.get().dynmapShowMoneyInDescription)
		{
			money = Money.format(Econ.getMoney(faction));
		}
		ret = addToHtml(ret, "money", money);
		
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
			String color = calcBoolcolor(flagName, value);
			String boolcolor = calcBoolcolor(String.valueOf(value), value);
			
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
			String flagTable = getHtmlAsciTable(flagTableParts, cols);
			ret = ret.replace("%flags.table" + cols + "%", flagTable);
		}
		
		// Players
		List<MPlayer> playersList = faction.getMPlayers();
		String playersCount = String.valueOf(playersList.size());
		String players = getHtmlPlayerString(playersList);
		
		MPlayer playersLeaderObject = faction.getLeader();
		String playersLeader = getHtmlPlayerName(playersLeaderObject);
		
		ret = ret.replace("%players%", players);
		ret = ret.replace("%players.count%", playersCount);
		ret = ret.replace("%players.leader%", playersLeader);
		
		return ret;
	}

	public static String getHtmlAsciTable(Collection<String> strings, final int cols)
	{
		StringBuilder ret = new StringBuilder();
		
		int count = 0;
		for (Iterator<String> iter = strings.iterator(); iter.hasNext();)
		{
			String string = iter.next();
			count++;
			
			ret.append(string);

			if (iter.hasNext())
			{
				boolean lineBreak = count % cols == 0;
				ret.append(lineBreak ? "<br>" : " | ");
			}
		}
		
		return ret.toString();
	}
	
	public static String getHtmlPlayerString(List<MPlayer> mplayers)
	{
		List<String> names = mplayers.stream().map(EngineDynmap::getHtmlPlayerName).collect(Collectors.toList());
		return Txt.implodeCommaAndDot(names);
	}
	
	public static String getHtmlPlayerName(MPlayer mplayer)
	{
		if (mplayer == null) return "none";
		return StringEscapeUtils.escapeHtml(mplayer.getName());
	}
	
	public static String calcBoolcolor(String string, boolean bool)
	{
		return "<span style=\"color: " + (bool ? "#008000" : "#800000") + ";\">" + string + "</span>";
	}

	public static String addToHtml(String ret, String target, String replace)
	{
		if (ret == null) throw new NullPointerException("ret");
		if (target == null) throw new NullPointerException("target");
		if (replace == null) throw new NullPointerException("replace");

		target = "%" + target + "%";
		replace = ChatColor.stripColor(replace);
		replace = StringEscapeUtils.escapeHtml(replace);
		return ret.replace(target, replace);
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
		Map<String, DynmapStyle> styles = MConf.get().dynmapFactionStyles;
		
		// Priority 1: Check for admin override by faction ID or name
		DynmapStyle adminOverride = DynmapStyle.coalesce(
			styles.get(faction.getId()),
			styles.get(faction.getName())
		);
		if (adminOverride != null) return adminOverride;
		
		// Priority 2: Check for faction custom color (if enabled and set)
		if (MConf.get().dynmapUseFactionColors && faction.hasColor())
		{
			String color = faction.getColor();
			return new DynmapStyle().withLineColor(color).withFillColor(color);
		}
		
		// Priority 3: Use default style which will resolve colors via MConf.getDynmapColorForStyle()
		return MConf.get().dynmapDefaultStyle;
	}

	public static void logSevere(String msg)
	{
		String message = ChatColor.RED.toString() + msg;
		Factions.get().log(message);
	}
	
	enum Direction
	{
		XPLUS, ZPLUS, XMINUS, ZMINUS

		;

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

		public Direction turnRight()
		{
			return values()[(this.ordinal() + 1) % values().length];
		}

		public Direction turnAround()
		{
			return this.turnRight().turnRight();
		}

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
