package com.massivecraft.factions.integration.map.pl3xmap;

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
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.image.IconImage;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.registry.IconRegistry;
import net.pl3x.map.core.world.World;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World.Environment;

import com.massivecraft.factions.integration.map.MapIconUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Engine that handles the integration between Factions and Pl3xMap.
 *
 * <p>
 * This engine runs asynchronously every 15 seconds to update faction territory displays
 * on the Pl3xMap web interface. It creates polygon shapes for claimed chunks (with holes),
 * displays faction information in popups, and applies custom styling per faction.
 * </p>
 *
 * <p>
 * Pl3xMap layers are registered per world. The engine builds territory and warp data
 * asynchronously, then schedules a synchronous update on the main thread to apply markers.
 * </p>
 *
 * <p>
 * <strong>Thread safety:</strong>
 * <ul>
 * <li>Territory and warp data building runs asynchronously</li>
 * <li>Pl3xMap API updates run synchronously on the main thread</li>
 * </ul>
 * </p>
 *
 * <p>
 * Uses shared map config ({@link MConf} map* settings) and shared utilities
 * ({@link MapUtil}, {@link MapStyle}, {@link TerritoryPolygonBuilder#getPolygonWithHoles}).
 * </p>
 *
 * @see <a href="https://granny.github.io/Pl3xMap/">Pl3xMap API</a>
 */
public class EnginePl3xMap extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EnginePl3xMap i = new EnginePl3xMap();
	public static EnginePl3xMap get() { return i; }

	private EnginePl3xMap()
	{
		this.setSync(false);
		this.setPeriod(15 * 20L);
	}

	/** Per-world layer holders: world name -> territory, home, and warps SimpleLayers. */
	private final Map<String, WorldLayers> worldLayers = new ConcurrentHashMap<>();

	private static final class WorldLayers
	{
		final SimpleLayer territoryLayer;
		final SimpleLayer homeLayer;
		final SimpleLayer warpsLayer;

		WorldLayers(SimpleLayer territoryLayer, SimpleLayer homeLayer, SimpleLayer warpsLayer)
		{
			this.territoryLayer = territoryLayer;
			this.homeLayer = homeLayer;
			this.warpsLayer = warpsLayer;
		}
	}

	/**
	 * Engine tick: when Pl3xMap integration is enabled, builds territory and warp data asynchronously
	 * then schedules a sync task to apply markers on the main thread. When disabled, removes all layers.
	 */
	@Override
	public void run()
	{
		if (!MConf.get().pl3xmapEnabled)
		{
			removeAllLayers();
			return;
		}
		try
		{
			Pl3xMap api = Pl3xMap.api();
			if (!api.isEnabled()) return;
			perform(api);
		}
		catch (Throwable t)
		{
			// Pl3xMap not loaded or API not available
		}
	}

	/**
	 * Builds territory shapes, home warps, and other warps off the main thread, then schedules
	 * a single sync task to push all markers to Pl3xMap (API must be used on main thread).
	 *
	 * @param api Pl3xMap API instance (must be enabled)
	 */
	private void perform(Pl3xMap api)
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
				Pl3xMap a = Pl3xMap.api();
				if (a.isEnabled())
					updatePl3xMap(a, territoryByWorld, homeWarps, otherWarps);
			}
			catch (Throwable ignored) { }
			logTimeSpent("Sync", syncBefore);
		});
	}

	/**
	 * Resolves the Pl3xMap World for the given Bukkit world.
	 * Tries world name first, then normalized (lowercase, spaces to underscores), then vanilla dimension key.
	 *
	 * @param api         Pl3xMap API (for world registry)
	 * @param bukkitWorld Bukkit world to resolve
	 * @return Pl3xMap World if found and enabled, otherwise null
	 */
	private World getPl3xWorld(Pl3xMap api, org.bukkit.World bukkitWorld)
	{
		if (bukkitWorld == null) return null;
		String name = bukkitWorld.getName();
		World world = api.getWorldRegistry().get(name);
		if (world != null && world.isEnabled()) return world;

		String normalized = name.toLowerCase().replace(" ", "_").replace("/", "_");
		world = api.getWorldRegistry().get(normalized);
		if (world != null && world.isEnabled()) return world;

		// Try dimension-style name for vanilla
		String dim = dimensionValue(bukkitWorld.getEnvironment());
		if (dim != null)
		{
			world = api.getWorldRegistry().get(dim);
			if (world != null && world.isEnabled()) return world;
		}
		return null;
	}

	/**
	 * Returns the Pl3xMap dimension string for a Bukkit world environment (e.g. for vanilla world keys).
	 *
	 * @param env Bukkit world environment (NORMAL, NETHER, THE_END)
	 * @return Dimension value used in Pl3xMap world registry, or null if unknown
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
	 * Applies pre-built territory and warp data to Pl3xMap layers. Called synchronously on the main thread.
	 * For each Bukkit world that has an enabled Pl3xMap world, clears and repopulates the three layers
	 * (territory, home, warps) from the given maps.
	 *
	 * @param api               Pl3xMap API instance
	 * @param territoryByWorld  Territory polygons by world name, then by marker id
	 * @param homeWarps         Home warp markers by world name
	 * @param otherWarps        Non-home warp markers by world name
	 */
	private void updatePl3xMap(Pl3xMap api, Map<String, Map<String, MapTerritoryData>> territoryByWorld,
	                          Map<String, List<MapMarker>> homeWarps, Map<String, List<MapMarker>> otherWarps)
	{
		ensureFactionsIconsRegistered(api);
		MConf conf = MConf.get();

		for (org.bukkit.World bukkitWorld : Bukkit.getWorlds())
		{
			String worldName = bukkitWorld.getName();
			World pl3xWorld = getPl3xWorld(api, bukkitWorld);
			if (pl3xWorld == null) continue;

			WorldLayers layers = ensureWorldLayers(pl3xWorld, worldName);

			// Pre-built data for this world (from async build phase).
			Map<String, MapTerritoryData> worldTerritory = territoryByWorld.getOrDefault(worldName, Collections.emptyMap());
			List<MapMarker> worldHomeWarps = homeWarps.getOrDefault(worldName, Collections.emptyList());
			List<MapMarker> worldOtherWarps = otherWarps.getOrDefault(worldName, Collections.emptyList());

			// Territory layer: clear and repopulate with polygon markers (one per contiguous region).
			layers.territoryLayer.clearMarkers();
			for (Entry<String, MapTerritoryData> e : worldTerritory.entrySet())
			{
				Marker<?> marker = Pl3xMapUtil.toPolygon(e.getKey(), e.getValue());
				if (marker != null)
					layers.territoryLayer.addMarker(marker);
			}

			// Home warp layer: clear and add home markers only if enabled in config.
			layers.homeLayer.clearMarkers();
			if (conf.mapShowHomeWarp)
			{
				for (MapMarker mv : worldHomeWarps)
					layers.homeLayer.addMarker(Pl3xMapUtil.toIcon(mv));
			}

			// Other warps layer: clear and add non-home warp markers only if enabled in config.
			layers.warpsLayer.clearMarkers();
			if (conf.mapShowOtherWarps)
			{
				for (MapMarker mv : worldOtherWarps)
					layers.warpsLayer.addMarker(Pl3xMapUtil.toIcon(mv));
			}
		}
	}

	/**
	 * Ensures Factions icon keys from config (mapWarpHomeIcon, mapWarpOtherIcon) are registered
	 * with Pl3xMap's IconRegistry. Pl3xMap only displays icon markers whose image key is registered;
	 * without this, home/warp layers would show no icons. We register only the keys currently
	 * configured so admins can choose any icon name and get a generated marker image. Called on the
	 * main thread from {@link #updatePl3xMap}.
	 *
	 * @param api Pl3xMap API instance
	 */
	private void ensureFactionsIconsRegistered(Pl3xMap api)
	{
		Set<String> keysToRegister = MapIconUtil.getConfiguredWarpIconKeys(MConf.get());
		IconRegistry registry = api.getIconRegistry();
		for (String key : keysToRegister)
		{
			if (registry.has(key)) continue;
			IconImage iconImage = Pl3xMapUtil.createIconImage(key);
			if (iconImage != null)
			{
				try { registry.register(key, iconImage); }
				catch (Throwable t) { /* already registered or disk error */ }
			}
		}
	}

	/**
	 * Ensures the three Pl3xMap layers (territory, home, warps) exist for the given Pl3xMap world.
	 * Creates and registers SimpleLayers on first use; returns cached instance thereafter.
	 *
	 * @param pl3xWorld Pl3xMap world to register layers on
	 * @param worldName Bukkit world name (used as cache key and in layer key suffix)
	 * @return The layer holders for this world
	 */
	private WorldLayers ensureWorldLayers(World pl3xWorld, String worldName)
	{
		WorldLayers layers = worldLayers.get(worldName);
		if (layers != null) return layers;

		MConf conf = MConf.get();
		// Sanitize world name for use in Pl3xMap layer keys (allowed chars only).
		String safeWorld = Pl3xMapUtil.sanitizeKey(worldName);

		// Unique layer keys per world so each Pl3xMap world has its own territory/home/warps layers.
		String territoryKey = IntegrationPl3xMap.FACTIONS_LAYER_TERRITORY + "_" + safeWorld;
		String homeKey = IntegrationPl3xMap.FACTIONS_LAYER_HOME + "_" + safeWorld;
		String warpsKey = IntegrationPl3xMap.FACTIONS_LAYER_WARPS + "_" + safeWorld;

		// Territory layer: claims as polygons; label and visibility from shared config.
		SimpleLayer territoryLayer = new SimpleLayer(territoryKey, () -> conf.mapLayerName);
		territoryLayer.setDefaultHidden(conf.mapLayerHiddenByDefault);
		territoryLayer.setPriority(conf.mapLayerPriority);
		territoryLayer.setZIndex(conf.mapLayerPriority);
		territoryLayer.setShowControls(true);

		// Home warp layer: faction homes only.
		SimpleLayer homeLayer = new SimpleLayer(homeKey, () -> conf.mapLayerNameHome);
		homeLayer.setDefaultHidden(conf.mapLayerHiddenByDefaultHome);
		homeLayer.setPriority(conf.mapLayerPriorityHome);
		homeLayer.setZIndex(conf.mapLayerPriorityHome);
		homeLayer.setShowControls(true);

		// Other warps layer: non-home faction warps.
		SimpleLayer warpsLayer = new SimpleLayer(warpsKey, () -> conf.mapLayerNameWarps);
		warpsLayer.setDefaultHidden(conf.mapLayerHiddenByDefaultWarps);
		warpsLayer.setPriority(conf.mapLayerPriorityWarps);
		warpsLayer.setZIndex(conf.mapLayerPriorityWarps);
		warpsLayer.setShowControls(true);

		pl3xWorld.getLayerRegistry().register(territoryLayer);
		pl3xWorld.getLayerRegistry().register(homeLayer);
		pl3xWorld.getLayerRegistry().register(warpsLayer);

		WorldLayers wl = new WorldLayers(territoryLayer, homeLayer, warpsLayer);
		worldLayers.put(worldName, wl);
		return wl;
	}

	/**
	 * Unregisters all Factions layers from Pl3xMap and clears the per-world cache.
	 * Called when Pl3xMap integration is disabled in config. Runs the actual unregister on the main thread.
	 */
	private void removeAllLayers()
	{
		try
		{
			Pl3xMap api = Pl3xMap.api();
			// Pl3xMap API requires main thread; copy key set to avoid CME when removing during iteration.
			Bukkit.getScheduler().scheduleSyncDelayedTask(Factions.get(), () -> {
				for (String worldName : new ArrayList<>(worldLayers.keySet()))
				{
					org.bukkit.World bukkitWorld = Bukkit.getWorld(worldName);
					if (bukkitWorld == null) continue;
					World pl3xWorld = getPl3xWorld(api, bukkitWorld);
					if (pl3xWorld == null) continue;
					WorldLayers layers = worldLayers.get(worldName);
					if (layers == null) continue;
					// Unregister all three Factions layers for this world.
					pl3xWorld.getLayerRegistry().unregister(layers.territoryLayer.getKey());
					pl3xWorld.getLayerRegistry().unregister(layers.homeLayer.getKey());
					pl3xWorld.getLayerRegistry().unregister(layers.warpsLayer.getKey());
					worldLayers.remove(worldName);
				}
			});
		}
		catch (Throwable ignored) { }
	}

	/**
	 * Builds territory polygon data for all faction claims, grouped by world. For each world and faction,
	 * gets the set of claimed chunks, splits them into contiguous regions (flood-fill), converts each region
	 * to an outer polygon plus holes, and stores one {@link MapTerritoryData} per polygon.
	 *
	 * @return Map: world name -> (marker id -> territory data)
	 */
	private Map<String, Map<String, MapTerritoryData>> buildTerritoryShapes()
	{
		// Board: world -> (faction -> set of chunk PS).
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

				// Split this faction's chunks into contiguous regions; each region becomes one polygon (with holes).
				Set<PS> remaining = new MassiveSet<>(chunks);
				int markerIdx = 0;

				while (!remaining.isEmpty())
				{
					// Flood-fill from one chunk to get all 4-adjacent chunks in this region.
					Set<PS> polygonChunks = new MassiveSet<>();
					Iterator<PS> it = remaining.iterator();
					PS start = it.next();
					floodFill(remaining, polygonChunks, start);

					// Build outer boundary and holes (unclaimed areas inside the boundary).
					List<List<PS>> polygonWithHoles = TerritoryPolygonBuilder.getPolygonWithHoles(polygonChunks, false);
					if (polygonWithHoles.isEmpty()) continue;

					List<PS> outer = polygonWithHoles.get(0);
					List<List<PS>> holes = polygonWithHoles.size() > 1 ? polygonWithHoles.subList(1, polygonWithHoles.size()) : Collections.emptyList();

					// Unique marker id: world + faction id + index (faction may have multiple disjoint regions).
					String markerId = IntegrationPl3xMap.FACTIONS_AREA_ + world + "__" + faction.getId() + "__" + markerIdx++;
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
	 * Flood-fills from {@code startChunk} using 4-direction adjacency (no diagonals). Moves all contiguous
	 * chunks from {@code source} into {@code destination}. Used to split a faction's chunk set into
	 * separate contiguous regions, each of which becomes one polygon (possibly with holes) for the map.
	 *
	 * @param source      Set of chunk coordinates to search (modified: contiguous chunk set is removed)
	 * @param destination Set that receives the contiguous chunk set
	 * @param startChunk  Starting chunk for the fill
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
			// Enqueue the four cardinal neighbours (same world implied by PS).
			Stream.of(
				PS.valueOf(next.getChunkX() + 1, next.getChunkZ()),
				PS.valueOf(next.getChunkX() - 1, next.getChunkZ()),
				PS.valueOf(next.getChunkX(), next.getChunkZ() + 1),
				PS.valueOf(next.getChunkX(), next.getChunkZ() - 1)
			).filter(source::contains).forEach(stack::push);
		}
	}

	/**
	 * Collects all faction home warps as map markers, grouped by world name. Only warps named "home"
	 * (case-insensitive) are included; visibility follows map config.
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
				// Only warps named "home" (case-insensitive) go on the home layer.
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
	 * Collects all non-home faction warps as map markers, grouped by world name. Excludes warps named
	 * "home"; visibility follows map config.
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
				// Exclude home warps; they are on the home layer.
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
	 * Resolves the map style (line/fill color, opacity, etc.) for a faction's territory. Order:
	 * (1) admin override by faction id or name, (2) faction colors from /f color if enabled, (3) default style.
	 *
	 * @param faction The faction to get style for
	 * @return The style to use for this faction's polygons
	 */
	private MapStyle getStyle(Faction faction)
	{
		// (1) Admin override by faction id or name (mapFactionStyleOverrides).
		Map<String, MapStyle> styles = MConf.get().mapFactionStyleOverrides;
		MapStyle adminOverride = MapStyle.coalesce(styles.get(faction.getId()), styles.get(faction.getName()));
		if (adminOverride != null) return adminOverride;

		// (2) Faction colors from /f color if mapUseFactionColors is enabled.
		if (MConf.get().mapUseFactionColors)
		{
			String primary = faction.hasPrimaryColor() ? faction.getPrimaryColor() : null;
			String secondary = faction.hasSecondaryColor() ? faction.getSecondaryColor() : null;
			return new MapStyle().withLineColor(secondary).withFillColor(primary);
		}

		// (3) Global default style from config.
		return MConf.get().mapDefaultStyle;
	}

	/**
	 * Logs Pl3xMap update phase duration to the Factions log when {@link MConf#pl3xmapLogTimeSpent} is true.
	 *
	 * @param name  Phase label (e.g. "Build", "Sync")
	 * @param start Start time in milliseconds (e.g. from {@link System#currentTimeMillis()})
	 */
	private static void logTimeSpent(String name, long start)
	{
		if (!MConf.get().pl3xmapLogTimeSpent) return;
		long duration = System.currentTimeMillis() - start;
		Factions.get().log(Txt.parse("<i>Pl3xMap %s took <h>%dms<i>.", name, duration));
	}

	/**
	 * Logs a severe error message to the Factions log (red). Used by shared code that cannot depend on a specific map engine.
	 *
	 * @param msg The error message to log
	 */
	public static void logSevere(String msg)
	{
		Factions.get().log(ChatColor.RED + msg);
	}
}
