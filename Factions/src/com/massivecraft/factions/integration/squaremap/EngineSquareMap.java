package com.massivecraft.factions.integration.squaremap;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.factions.integration.map.MapMarker;
import com.massivecraft.factions.integration.map.MapStyle;
import com.massivecraft.factions.integration.map.MapTerritoryData;
import com.massivecraft.factions.integration.map.MapUtil;
import com.massivecraft.factions.integration.map.TerritoryPolygonBuilder;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.World.Environment;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Engine that handles the integration between Factions and SquareMap.
 *
 * <p>
 * This engine runs asynchronously every 15 seconds to update faction territory displays
 * on the SquareMap web interface. It creates polygon shapes for claimed chunks (with holes),
 * displays faction information in popups, and applies custom styling per faction.
 * </p>
 *
 * <p>
 * SquareMap layers are registered per world. The engine builds territory and warp data
 * asynchronously, then schedules a synchronous update on the main thread to apply markers.
 * </p>
 *
 * <p>
 * <strong>Thread safety:</strong>
 * <ul>
 * <li>Territory and warp data building runs asynchronously</li>
 * <li>SquareMap API updates run synchronously on the main thread</li>
 * </ul>
 * </p>
 *
 * <p>
 * Uses shared map config ({@link MConf} map* settings) and shared utilities
 * ({@link MapUtil}, {@link MapStyle}, {@link TerritoryPolygonBuilder#getPolygonWithHoles}).
 * </p>
 */
public class EngineSquareMap extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EngineSquareMap i = new EngineSquareMap();
	public static EngineSquareMap get() { return i; }

	private EngineSquareMap()
	{
		this.setSync(false);
		this.setPeriod(15 * 20L);
	}

	/** Per-world layer providers: world name -> keys and SimpleLayerProviders for territory, home, and warps. */
	private final Map<String, WorldLayers> worldLayers = new ConcurrentHashMap<>();

	/** Holds the SquareMap layer keys and providers for one world's territory, home warp, and other warp layers. */
	private static final class WorldLayers
	{
		final Key territoryKey;
		final SimpleLayerProvider territoryProvider;
		final Key homeKey;
		final SimpleLayerProvider homeProvider;
		final Key warpsKey;
		final SimpleLayerProvider warpsProvider;

		WorldLayers(Key territoryKey, SimpleLayerProvider territoryProvider,
		            Key homeKey, SimpleLayerProvider homeProvider,
		            Key warpsKey, SimpleLayerProvider warpsProvider)
		{
			this.territoryKey = territoryKey;
			this.territoryProvider = territoryProvider;
			this.homeKey = homeKey;
			this.homeProvider = homeProvider;
			this.warpsKey = warpsKey;
			this.warpsProvider = warpsProvider;
		}
	}

	@Override
	public void run()
	{
		if (!MConf.get().squaremapEnabled)
		{
			removeAllLayers();
			return;
		}
		try
		{
			Squaremap api = SquaremapProvider.get();
			perform(api);
		}
		catch (IllegalStateException e)
		{
			// SquareMap API not loaded (plugin not enabled yet)
		}
	}

	/**
	 * Performs the SquareMap update: builds territory shapes, home warps, and other warps
	 * asynchronously, then schedules a synchronous update on the main thread.
	 */
	private void perform(Squaremap api)
	{
		long before = System.currentTimeMillis();

		final Map<String, Map<String, MapTerritoryData>> territoryByWorld = buildTerritoryShapes();
		final Map<String, List<MapMarker>> homeWarps = buildHomeWarps();
		final Map<String, List<MapMarker>> otherWarps = buildOtherWarps();

		logTimeSpent("Build", before);

		Bukkit.getScheduler().scheduleSyncDelayedTask(Factions.get(), () -> {
			long syncBefore = System.currentTimeMillis();
			try
			{
				Squaremap a = SquaremapProvider.get();
				updateSquareMap(a, territoryByWorld, homeWarps, otherWarps);
			}
			catch (IllegalStateException ignored) { }
			logTimeSpent("Sync", syncBefore);
		});
	}

	/**
	 * Resolves the SquareMap MapWorld for the given Bukkit world.
	 * SquareMap (Paper) may use world name (e.g. minecraft:world) or dimension key (e.g. minecraft:overworld).
	 * Tries world name first, then dimension key for vanilla dimensions.
	 */
	private Optional<MapWorld> getMapWorld(Squaremap api, World world)
	{
		String worldName = world.getName();
		// 1) Try world name (e.g. minecraft:world)
		String nameValue = worldName.toLowerCase().replace(" ", "_").replace("/", "_");
		Optional<MapWorld> opt = api.getWorldIfEnabled(WorldIdentifier.create("minecraft", nameValue));
		if (opt.isPresent()) return opt;
		// 2) Try dimension key (vanilla: overworld, the_nether, the_end)
		String dimensionValue = dimensionValue(world.getEnvironment());
		if (dimensionValue != null)
		{
			opt = api.getWorldIfEnabled(WorldIdentifier.create("minecraft", dimensionValue));
			if (opt.isPresent()) return opt;
		}
		return Optional.empty();
	}

	/**
	 * Returns the SquareMap dimension string for a Bukkit world environment.
	 * Used when resolving MapWorld by dimension key (e.g. minecraft:overworld).
	 *
	 * @param env World environment (NORMAL, NETHER, THE_END)
	 * @return Dimension value for WorldIdentifier, or null if unknown
	 */
	private static String dimensionValue(Environment env)
	{
		if (env == null) return null;
		switch (env)
		{
			case NORMAL: return "overworld";
			case NETHER: return "the_nether";
			case THE_END: return "the_end";
			default: return null;
		}
	}

	/**
	 * Applies built territory and warp data to SquareMap layers.
	 * Called synchronously on the main thread. For each Bukkit world that has an enabled
	 * MapWorld, ensures layers exist, then clears and repopulates markers from the pre-built maps.
	 *
	 * @param api SquareMap API instance
	 * @param territoryByWorld Territory polygons by world name, then by marker id
	 * @param homeWarps Home warp markers by world name
	 * @param otherWarps Non-home warp markers by world name
	 */
	private void updateSquareMap(Squaremap api, Map<String, Map<String, MapTerritoryData>> territoryByWorld,
	                             Map<String, List<MapMarker>> homeWarps, Map<String, List<MapMarker>> otherWarps)
	{
		MConf conf = MConf.get();

		for (World world : Bukkit.getWorlds())
		{
			String worldName = world.getName();
			Optional<MapWorld> opt = getMapWorld(api, world);
			if (!opt.isPresent()) continue;

			MapWorld mapWorld = opt.get();
			WorldLayers layers = ensureWorldLayers(mapWorld, worldName);

			Map<String, MapTerritoryData> worldTerritory = territoryByWorld.getOrDefault(worldName, Collections.emptyMap());
			List<MapMarker> worldHomeWarps = homeWarps.getOrDefault(worldName, Collections.emptyList());
			List<MapMarker> worldOtherWarps = otherWarps.getOrDefault(worldName, Collections.emptyList());

			// Territory layer: polygon markers for claimed chunks
			layers.territoryProvider.clearMarkers();
			for (Entry<String, MapTerritoryData> e : worldTerritory.entrySet())
			{
				Marker marker = SquareMapUtil.toPolygon(e.getValue());
				if (marker != null)
					layers.territoryProvider.addMarker(Key.of(SquareMapUtil.sanitizeKey(e.getKey())), marker);
			}

			// Home warp layer: icon markers for faction home warps (if enabled in config)
			layers.homeProvider.clearMarkers();
			if (conf.mapShowHomeWarp)
			{
				for (MapMarker mv : worldHomeWarps)
					layers.homeProvider.addMarker(Key.of(SquareMapUtil.sanitizeKey(mv.getId())), SquareMapUtil.toIcon(mv));
			}

			// Other warps layer: icon markers for non-home faction warps (if enabled in config)
			layers.warpsProvider.clearMarkers();
			if (conf.mapShowOtherWarps)
			{
				for (MapMarker mv : worldOtherWarps)
					layers.warpsProvider.addMarker(Key.of(SquareMapUtil.sanitizeKey(mv.getId())), SquareMapUtil.toIcon(mv));
			}
		}
	}

	/**
	 * Ensures the three SquareMap layers (territory, home, warps) exist for the given MapWorld.
	 * Layers are keyed per world (e.g. factions_territory_world) so multiple worlds each have
	 * their own layer set. Creates and registers providers on first use; returns cached instance thereafter.
	 *
	 * @param mapWorld SquareMap world to register layers on
	 * @param worldName Bukkit world name (used as cache key and in layer key suffix)
	 * @return The layer providers for this world
	 */
	private WorldLayers ensureWorldLayers(MapWorld mapWorld, String worldName)
	{
		WorldLayers layers = worldLayers.get(worldName);
		if (layers != null) return layers;

		MConf conf = MConf.get();
		// Sanitize world name for SquareMap Key (allowed: [a-zA-Z0-9._-])
		String safeWorld = SquareMapUtil.sanitizeKey(worldName);

		SimpleLayerProvider territoryProvider = SimpleLayerProvider.builder(conf.mapLayerName)
			.defaultHidden(conf.mapLayerHiddenByDefault)
			.showControls(true)
			.layerPriority(conf.mapLayerPriority)
			.zIndex(conf.mapLayerPriority)
			.build();

		SimpleLayerProvider homeProvider = SimpleLayerProvider.builder(conf.mapLayerNameHome)
			.defaultHidden(conf.mapLayerHiddenByDefaultHome)
			.showControls(true)
			.layerPriority(conf.mapLayerPriorityHome)
			.zIndex(conf.mapLayerPriorityHome)
			.build();

		SimpleLayerProvider warpsProvider = SimpleLayerProvider.builder(conf.mapLayerNameWarps)
			.defaultHidden(conf.mapLayerHiddenByDefaultWarps)
			.showControls(true)
			.layerPriority(conf.mapLayerPriorityWarps)
			.zIndex(conf.mapLayerPriorityWarps)
			.build();

		// Unique layer keys per world so each MapWorld has its own territory/home/warps layers
		Key territoryKey = Key.of(IntegrationSquareMap.FACTIONS_LAYER_TERRITORY + "_" + safeWorld);
		Key homeKey = Key.of(IntegrationSquareMap.FACTIONS_LAYER_HOME + "_" + safeWorld);
		Key warpsKey = Key.of(IntegrationSquareMap.FACTIONS_LAYER_WARPS + "_" + safeWorld);

		mapWorld.layerRegistry().register(territoryKey, territoryProvider);
		mapWorld.layerRegistry().register(homeKey, homeProvider);
		mapWorld.layerRegistry().register(warpsKey, warpsProvider);

		WorldLayers wl = new WorldLayers(territoryKey, territoryProvider, homeKey, homeProvider, warpsKey, warpsProvider);
		worldLayers.put(worldName, wl);
		return wl;
	}

	/**
	 * Unregisters all Factions layers from SquareMap and clears the per-world cache.
	 * Called when SquareMap integration is disabled in config. Runs the actual unregister
	 * on the main thread to satisfy the SquareMap API.
	 */
	private void removeAllLayers()
	{
		try
		{
			Squaremap api = SquaremapProvider.get();
			Bukkit.getScheduler().scheduleSyncDelayedTask(Factions.get(), () -> {
				for (String worldName : new ArrayList<>(worldLayers.keySet()))
				{
					World world = Bukkit.getWorld(worldName);
					if (world == null) continue;
					Optional<MapWorld> opt = getMapWorld(api, world);
					if (!opt.isPresent()) continue;
					WorldLayers layers = worldLayers.get(worldName);
					if (layers == null) continue;
					opt.get().layerRegistry().unregister(layers.territoryKey);
					opt.get().layerRegistry().unregister(layers.homeKey);
					opt.get().layerRegistry().unregister(layers.warpsKey);
					worldLayers.remove(worldName);
				}
			});
		}
		catch (IllegalStateException ignored) { }
	}

	/**
	 * Builds territory polygon data for all faction claims, grouped by world.
	 * For each world and faction we get the set of claimed chunks, then split them into
	 * contiguous regions (via flood-fill), convert each region to an outer polygon plus
	 * holes (unclaimed areas inside the region), and store one {@link MapTerritoryData} per polygon.
	 *
	 * @return Map: world name -> (marker id -> territory data)
	 */
	private Map<String, Map<String, MapTerritoryData>> buildTerritoryShapes()
	{
		// Board: world -> (faction -> set of chunk PS)
		Map<String, Map<Faction, Set<PS>>> worldFactionChunks = BoardColl.get().getWorldToFactionToChunks(false);
		Map<String, Map<String, MapTerritoryData>> result = new MassiveMap<>();

		for (Entry<String, Map<Faction, Set<PS>>> worldEntry : worldFactionChunks.entrySet())
		{
			String world = worldEntry.getKey();
			Map<String, MapTerritoryData> worldData = new MassiveMap<>();
			result.put(world, worldData);

			for (Entry<Faction, Set<PS>> factionEntry : worldEntry.getValue().entrySet())
			{
				Faction faction = factionEntry.getKey();
				Set<PS> chunks = factionEntry.getValue();
				if (!MapUtil.isFactionVisible(faction.getId(), faction.getName(), world) || chunks.isEmpty())
					continue;

				String description = MapUtil.getFactionDescriptionHtml(faction);
				MapStyle style = getStyle(faction);

				// Process chunks in contiguous groups: each group becomes one polygon (with holes)
				Set<PS> remaining = new MassiveSet<>(chunks);
				int markerIdx = 0;

				while (!remaining.isEmpty())
				{
					// Flood-fill from one chunk to get all 4-adjacent chunks in this region
					Set<PS> polygonChunks = new MassiveSet<>();
					Iterator<PS> it = remaining.iterator();
					PS start = it.next();
					floodFill(remaining, polygonChunks, start);

					// Build outer boundary and holes (unclaimed areas inside the boundary)
					// false = do not combine diagonal holes (SquareMap supports multiple holes)
					List<List<PS>> polygonWithHoles = TerritoryPolygonBuilder.getPolygonWithHoles(polygonChunks, false);
					if (polygonWithHoles.isEmpty()) continue;

					List<PS> outer = polygonWithHoles.get(0);
					List<List<PS>> holes = polygonWithHoles.size() > 1 ? polygonWithHoles.subList(1, polygonWithHoles.size()) : Collections.emptyList();

					// Unique marker id: world + faction id + index (faction may have multiple disjoint regions)
					String markerId = IntegrationSquareMap.FACTIONS_AREA_ + world + "__" + faction.getId() + "__" + markerIdx++;
					MapTerritoryData data = new MapTerritoryData(
						faction.getName(),
						world,
						description,
						outer,
						holes,
						style
					);
					worldData.put(markerId, data);
				}
			}
		}
		return result;
	}

	/**
	 * Flood-fills from {@code startChunk} using 4-direction adjacency (no diagonals).
	 * Moves all contiguous chunks from {@code source} into {@code destination}.
	 * Used to split a faction's chunk set into separate contiguous regions, each of
	 * which becomes one polygon (possibly with holes) for the map.
	 *
	 * @param source Set of chunk coordinates to search (modified: contiguous chunk set is removed)
	 * @param destination Set that receives the contiguous chunk set
	 * @param startChunk Starting chunk for the fill
	 */
	private void floodFill(Set<PS> source, Set<PS> destination, PS startChunk)
	{
		ArrayDeque<PS> stack = new ArrayDeque<>();
		stack.push(startChunk);
		while (!stack.isEmpty())
		{
			PS next = stack.pop();
			if (!source.remove(next)) continue;
			destination.add(next);
			// Enqueue the four cardinal neighbours (same world implied by PS)
			Stream.of(
				PS.valueOf(next.getChunkX() + 1, next.getChunkZ()),
				PS.valueOf(next.getChunkX() - 1, next.getChunkZ()),
				PS.valueOf(next.getChunkX(), next.getChunkZ() + 1),
				PS.valueOf(next.getChunkX(), next.getChunkZ() - 1)
			).filter(source::contains).forEach(stack::push);
		}
	}

	/**
	 * Collects all faction home warps as map markers, grouped by world name.
	 * Only warps named "home" (case-insensitive) are included; visibility follows map config.
	 *
	 * @return Map: world name -> list of home warp markers
	 */
	private Map<String, List<MapMarker>> buildHomeWarps()
	{
		Map<String, List<MapMarker>> byWorld = new MassiveMap<>();
		for (Faction faction : FactionColl.get().getAll())
		{
			for (Warp warp : faction.getWarps().getAll())
			{
				if (!"home".equalsIgnoreCase(warp.getName())) continue;
				PS loc = warp.getLocation();
				if (loc == null) continue;
				String world = loc.getWorld();
				if (world == null || !MapUtil.isFactionVisible(faction.getId(), faction.getName(), world)) continue;

				String label = faction.getName() + " - Home";
				String description = "<b>" + faction.getName() + "</b><br/>Home";
				String id = "factions_home_" + faction.getId();
				byWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(new MapMarker(id, label, world, loc.getLocationX(), loc.getLocationY(), loc.getLocationZ(), MConf.get().mapWarpHomeIcon, description));
			}
		}
		return byWorld;
	}

	/**
	 * Collects all non-home faction warps as map markers, grouped by world name.
	 * Excludes warps named "home"; visibility follows map config.
	 *
	 * @return Map: world name -> list of warp markers
	 */
	private Map<String, List<MapMarker>> buildOtherWarps()
	{
		Map<String, List<MapMarker>> byWorld = new MassiveMap<>();
		for (Faction faction : FactionColl.get().getAll())
		{
			for (Warp warp : faction.getWarps().getAll())
			{
				if ("home".equalsIgnoreCase(warp.getName())) continue;
				PS loc = warp.getLocation();
				if (loc == null) continue;
				String world = loc.getWorld();
				if (world == null || !MapUtil.isFactionVisible(faction.getId(), faction.getName(), world)) continue;

				String label = faction.getName() + " - " + warp.getName();
				String description = "<b>" + faction.getName() + "</b><br/>Warp: " + warp.getName();
				String id = "factions_warp_" + faction.getId() + "_" + warp.getId();
				byWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(new MapMarker(id, label, world, loc.getLocationX(), loc.getLocationY(), loc.getLocationZ(), MConf.get().mapWarpOtherIcon, description));
			}
		}
		return byWorld;
	}

	/**
	 * Resolves the map style (line/fill color, opacity, etc.) for a faction's territory.
	 * Order: (1) admin override by faction id or name, (2) faction colors from /f color if enabled,
	 * (3) default style from config.
	 *
	 * @param faction The faction to get style for
	 * @return The style to use for this faction's polygons
	 */
	private MapStyle getStyle(Faction faction)
	{
		Map<String, MapStyle> styles = MConf.get().mapFactionStyleOverrides;
		MapStyle adminOverride = MapStyle.coalesce(styles.get(faction.getId()), styles.get(faction.getName()));
		if (adminOverride != null) return adminOverride;
		if (MConf.get().mapUseFactionColors)
		{
			String primary = faction.hasPrimaryColor() ? faction.getPrimaryColor() : null;
			String secondary = faction.hasSecondaryColor() ? faction.getSecondaryColor() : null;
			return new MapStyle().withLineColor(secondary).withFillColor(primary);
		}
		return MConf.get().mapDefaultStyle;
	}

	/**
	 * Logs SquareMap update phase duration to the Factions log when {@link MConf#squaremapLogTimeSpent} is true.
	 *
	 * @param name  Phase label (e.g. "Build", "Sync")
	 * @param start Start time in milliseconds (e.g. from {@link System#currentTimeMillis()})
	 */
	private static void logTimeSpent(String name, long start)
	{
		if (!MConf.get().squaremapLogTimeSpent) return;
		long duration = System.currentTimeMillis() - start;
		Factions.get().log(Txt.parse("<i>SquareMap %s took <h>%dms<i>.", name, duration));
	}

	/**
	 * Logs a severe error message to the Factions log (red).
	 * Used by shared code (e.g. {@link TerritoryPolygonBuilder}) that cannot depend on a specific map engine.
	 *
	 * @param msg The error message to log
	 */
	public static void logSevere(String msg)
	{
		Factions.get().log(ChatColor.RED + msg);
	}
}
