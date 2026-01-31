package com.massivecraft.factions.integration.bluemap;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.map.MapMarker;
import com.massivecraft.factions.integration.map.MapStyle;
import com.massivecraft.factions.integration.map.MapTerritoryData;
import com.massivecraft.factions.integration.map.MapUtil;
import com.massivecraft.factions.integration.map.TerritoryPolygonBuilder;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.TimeDiffUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import com.massivecraft.massivecore.util.Txt;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
 * Engine that handles the integration between Factions and BlueMap.
 *
 * <p>
 * This engine runs asynchronously every 15 seconds to update faction territory displays
 * on the BlueMap web interface. It creates 3D extruded shapes for claimed chunks (full
 * world height), uses native hole support for unclaimed holes within faction land,
 * displays faction information in popups, and applies custom styling per faction.
 * </p>
 *
 * <p>
 * BlueMap markers are not persistent; they are re-created each time BlueMap loads and
 * on each periodic run. The engine registers {@link BlueMapAPI#onEnable} so markers are
 * re-created when BlueMap loads or reloads.
 * </p>
 *
 * <p>
 * <strong>Thread safety:</strong>
 * <ul>
 * <li>Territory and warp data building runs asynchronously to avoid blocking the main thread</li>
 * <li>BlueMap API updates run synchronously on the main thread (scheduled from the async run)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Uses shared map config ({@link MConf} map* settings) and shared utilities
 * ({@link MapUtil}, {@link MapStyle}, {@link TerritoryPolygonBuilder#getPolygonWithHoles}).
 * </p>
 */
public class EngineBlueMap extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EngineBlueMap i = new EngineBlueMap();
	public static EngineBlueMap get() { return i; }

	private EngineBlueMap()
	{
		this.setSync(false);
		this.setPeriod(15 * 20L);
		// Re-create markers when BlueMap loads or reloads (markers are not persistent)
		BlueMapAPI.onEnable(api -> perform(api));
	}

	@Override
	public void run()
	{
		if (!MConf.get().bluemapEnabled)
		{
			BlueMapAPI.getInstance().ifPresent(this::removeMarkerSets);
			return;
		}
		BlueMapAPI.getInstance().ifPresent(this::perform);
	}

	/**
	 * Performs the BlueMap update: builds territory shapes, home warps, and other warps
	 * asynchronously, then schedules a synchronous update on the main thread to apply
	 * markers to all BlueMap worlds and maps.
	 *
	 * @param api The BlueMap API instance (must not be null)
	 */
	public void perform(BlueMapAPI api)
	{
		long before = System.currentTimeMillis();

		final Map<String, Map<String, MapTerritoryData>> territoryByWorld = buildTerritoryShapes();
		final Map<String, List<MapMarker>> homeWarps = buildHomeWarps();
		final Map<String, List<MapMarker>> otherWarps = buildOtherWarps();

		logTimeSpent("Build", before);

		Bukkit.getScheduler().scheduleSyncDelayedTask(Factions.get(), () -> {
			long syncBefore = System.currentTimeMillis();
			BlueMapAPI.getInstance().ifPresent(a -> updateBlueMap(a, territoryByWorld, homeWarps, otherWarps));
			logTimeSpent("Sync", syncBefore);
		});
	}

	/**
	 * Updates the BlueMap with the given territory, home warps, and other warps.
	 * @param api The BlueMap API instance
	 * @param territoryByWorld The territory data by world
	 * @param homeWarps The home warps data
	 * @param otherWarps The other warps data
	 */
	private void updateBlueMap(BlueMapAPI api, Map<String, Map<String, MapTerritoryData>> territoryByWorld,
	                           Map<String, List<MapMarker>> homeWarps, Map<String, List<MapMarker>> otherWarps)
	{
		MConf conf = MConf.get();
		for (de.bluecolored.bluemap.api.BlueMapWorld bmWorld : api.getWorlds())
		{
			String worldId = bmWorld.getId();
			// Resolve data world key: our data is keyed by Board/warp world name; BlueMap world id may differ (case, format).
			String dataWorldKey = resolveDataWorldKey(worldId, territoryByWorld);
			World world = Bukkit.getWorld(worldId);
			if (world == null && dataWorldKey != null)
				world = Bukkit.getWorld(dataWorldKey);

			// Get the minimum and maximum Y levels for the world
			int minY = world != null ? world.getMinHeight() : -64;
			int worldMaxY = world != null ? world.getMaxHeight() : 320;
			// Cap the maximum Y level to the world's max height
			int maxY = Math.min(worldMaxY, MConf.get().mapTerritoryMaxY);

			for (de.bluecolored.bluemap.api.BlueMapMap map : bmWorld.getMaps())
			{
				ensureMarkerSets(map);

				String lookupKey = dataWorldKey != null ? dataWorldKey : worldId;
				Map<String, MapTerritoryData> worldTerritory = territoryByWorld.getOrDefault(lookupKey, Collections.emptyMap());
				List<MapMarker> worldHomeWarps = homeWarps.getOrDefault(lookupKey, Collections.emptyList());
				List<MapMarker> worldOtherWarps = otherWarps.getOrDefault(lookupKey, Collections.emptyList());

				// Territory
				MarkerSet territorySet = map.getMarkerSets().get(IntegrationBlueMap.FACTIONS_MARKERSET_TERRITORY);
				if (territorySet != null)
				{
					territorySet.getMarkers().clear();
					for (Entry<String, MapTerritoryData> e : worldTerritory.entrySet())
					{
						ExtrudeMarker marker = BlueMapUtil.toExtrudeMarker(e.getValue(), minY, maxY);
						if (marker != null)
						{
							territorySet.getMarkers().put(e.getKey(), marker);
						}
					}
				}

				// Home warps
				if (conf.mapShowHomeWarp)
				{
					MarkerSet homeSet = map.getMarkerSets().get(IntegrationBlueMap.FACTIONS_MARKERSET_HOME);
					if (homeSet != null)
					{
						homeSet.getMarkers().clear();
						for (MapMarker mv : worldHomeWarps)
						{
							homeSet.getMarkers().put(mv.getId(), BlueMapUtil.toPOIMarker(mv));
						}
					}
				}

				// Other warps
				if (conf.mapShowOtherWarps)
				{
					MarkerSet warpsSet = map.getMarkerSets().get(IntegrationBlueMap.FACTIONS_MARKERSET_WARPS);
					if (warpsSet != null)
					{
						warpsSet.getMarkers().clear();
						for (MapMarker mv : worldOtherWarps)
						{
							warpsSet.getMarkers().put(mv.getId(), BlueMapUtil.toPOIMarker(mv));
						}
					}
				}
			}
		}
	}

	/**
	 * Resolves BlueMap world id to our data world key (Board id / warp world name).
	 * BlueMap uses ids like "world#minecraft:overworld"; our data is keyed by Bukkit world name (e.g. "world").
	 * Tries exact match, then the world name part before "#", then case-insensitive match, then Bukkit world name.
	 */
	private static String resolveDataWorldKey(String blueMapWorldId, Map<String, Map<String, MapTerritoryData>> territoryByWorld)
	{
		if (blueMapWorldId == null || territoryByWorld == null) return blueMapWorldId;
		if (territoryByWorld.containsKey(blueMapWorldId)) return blueMapWorldId;
		// BlueMap world id is often "worldName#minecraft:dimension" — try the world name part
		String worldNamePart = blueMapWorldId.contains("#") ? blueMapWorldId.substring(0, blueMapWorldId.indexOf('#')) : blueMapWorldId;
		if (territoryByWorld.containsKey(worldNamePart)) return worldNamePart;
		for (String key : territoryByWorld.keySet())
		{
			if (key != null && key.equalsIgnoreCase(blueMapWorldId)) return key;
			if (key != null && key.equalsIgnoreCase(worldNamePart)) return key;
		}
		World bukkitWorld = Bukkit.getWorld(blueMapWorldId);
		if (bukkitWorld == null) bukkitWorld = Bukkit.getWorld(worldNamePart);
		if (bukkitWorld != null && territoryByWorld.containsKey(bukkitWorld.getName())) return bukkitWorld.getName();
		for (World w : Bukkit.getWorlds())
		{
			if ((blueMapWorldId.equals(w.getName()) || worldNamePart.equals(w.getName())) && territoryByWorld.containsKey(w.getName()))
			{
				return w.getName();
			}
		}
		return blueMapWorldId;
	}

	/**
	 * Ensures the marker sets for the given map exist.
	 * 
	 * @param map The BlueMap map
	 */
	private void ensureMarkerSets(de.bluecolored.bluemap.api.BlueMapMap map)
	{
		MConf conf = MConf.get();
		Map<String, MarkerSet> sets = map.getMarkerSets();

		if (!sets.containsKey(IntegrationBlueMap.FACTIONS_MARKERSET_TERRITORY))
		{
			MarkerSet territory = new MarkerSet(conf.mapLayerName, true, conf.mapLayerHiddenByDefault);
			territory.setSorting(conf.mapLayerPriority);
			sets.put(IntegrationBlueMap.FACTIONS_MARKERSET_TERRITORY, territory);
		}

		if (conf.mapShowHomeWarp && !sets.containsKey(IntegrationBlueMap.FACTIONS_MARKERSET_HOME))
		{
			MarkerSet home = new MarkerSet(conf.mapLayerNameHome, true, conf.mapLayerHiddenByDefaultHome);
			home.setSorting(conf.mapLayerPriorityHome);
			sets.put(IntegrationBlueMap.FACTIONS_MARKERSET_HOME, home);
		}

		if (conf.mapShowOtherWarps && !sets.containsKey(IntegrationBlueMap.FACTIONS_MARKERSET_WARPS))
		{
			MarkerSet warps = new MarkerSet(conf.mapLayerNameWarps, true, conf.mapLayerHiddenByDefaultWarps);
			warps.setSorting(conf.mapLayerPriorityWarps);
			sets.put(IntegrationBlueMap.FACTIONS_MARKERSET_WARPS, warps);
		}
	}

	/**
	 * Removes all marker sets from the given BlueMap API instance.
	 * 
	 * @param api The BlueMap API instance
	 */
	private void removeMarkerSets(BlueMapAPI api)
	{
		for (de.bluecolored.bluemap.api.BlueMapWorld bmWorld : api.getWorlds())
		{
			for (de.bluecolored.bluemap.api.BlueMapMap map : bmWorld.getMaps())
			{
				map.getMarkerSets().remove(IntegrationBlueMap.FACTIONS_MARKERSET_TERRITORY);
				map.getMarkerSets().remove(IntegrationBlueMap.FACTIONS_MARKERSET_HOME);
				map.getMarkerSets().remove(IntegrationBlueMap.FACTIONS_MARKERSET_WARPS);
			}
		}
	}

	/**
	 * Builds the territory shapes for all factions.
	 * 
	 * @return The territory shapes by world
	 */
	private Map<String, Map<String, MapTerritoryData>> buildTerritoryShapes()
	{
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

				String description = buildDescription(faction);
				MapStyle style = getStyle(faction);

				Set<PS> remaining = new MassiveSet<>(chunks);
				int markerIdx = 0;

				while (!remaining.isEmpty())
				{
					Set<PS> polygonChunks = new MassiveSet<>();
					Iterator<PS> it = remaining.iterator();
					PS start = it.next();
					floodFill(remaining, polygonChunks, start);

					// Do not combine diagonally adjacent holes so BlueMap gets separate hole boundaries (avoids triangulation artifacts).
					List<List<PS>> polygonWithHoles = TerritoryPolygonBuilder.getPolygonWithHoles(polygonChunks, false);
					if (polygonWithHoles.isEmpty()) continue;

					List<PS> outer = polygonWithHoles.get(0);
					List<List<PS>> holes = polygonWithHoles.size() > 1 ? polygonWithHoles.subList(1, polygonWithHoles.size()) : Collections.emptyList();

					String markerId = IntegrationBlueMap.FACTIONS_AREA_ + world + "__" + faction.getId() + "__" + markerIdx++;
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
	 * Performs a flood fill algorithm to find all contiguous chunks in the given source set.
	 * 
	 * @param source The source set of chunks
	 * @param destination The destination set of chunks
	 * @param startChunk The starting chunk
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
			Stream.of(
				PS.valueOf(next.getChunkX() + 1, next.getChunkZ()),
				PS.valueOf(next.getChunkX() - 1, next.getChunkZ()),
				PS.valueOf(next.getChunkX(), next.getChunkZ() + 1),
				PS.valueOf(next.getChunkX(), next.getChunkZ() - 1)
			).filter(source::contains).forEach(stack::push);
		}
	}

	/**
	 * Builds the home warps for all factions.
	 * 
	 * @return The home warps by world
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
	 * Builds the other warps for all factions.
	 * 
	 * @return The other warps by world
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
	 * Builds the description for the given faction.
	 * 
	 * @param faction The faction
	 * @return The description
	 */
	private String buildDescription(Faction faction)
	{
		String ret = "<div class=\"regioninfo\">" + MConf.get().mapDescriptionWindowFormat + "</div>";
		ret = MapUtil.addToHtml(ret, "name", faction.getName());
		ret = MapUtil.addToHtml(ret, "description", faction.getDescriptionDesc());
		String motd = faction.getMotd();
		if (motd != null) ret = MapUtil.addToHtml(ret, "motd", motd);

		LinkedHashMap<TimeUnit, Long> ageUnitcounts = TimeDiffUtil.limit(TimeDiffUtil.unitcounts(faction.getAge(), TimeUnit.getAllButMillisSecondsAndMinutes()), 3);
		ret = MapUtil.addToHtml(ret, "age", TimeDiffUtil.formatedVerboose(ageUnitcounts));

		String money;
		if (Econ.isEnabled() && MConf.get().mapShowMoneyInDescription)
			money = faction.isNormal() ? Money.format(Econ.getMoney(faction)) : "N/A";
		else
			money = "unavailable";
		ret = MapUtil.addToHtml(ret, "money", money);

		Map<com.massivecraft.factions.entity.MFlag, Boolean> flags = com.massivecraft.factions.entity.MFlag.getAll().stream()
			.filter(com.massivecraft.factions.entity.MFlag::isVisible)
			.collect(Collectors.toMap(m -> m, faction::getFlag));
		List<String> flagTableParts = new ArrayList<>();
		for (Entry<com.massivecraft.factions.entity.MFlag, Boolean> entry : flags.entrySet())
		{
			String flagName = entry.getKey().getName();
			boolean value = entry.getValue();
			ret = ret.replace("%" + flagName + ".bool%", String.valueOf(value));
			ret = ret.replace("%" + flagName + ".color%", MapUtil.calcBoolcolor(flagName, value));
			ret = ret.replace("%" + flagName + ".boolcolor%", MapUtil.calcBoolcolor(String.valueOf(value), value));
			flagTableParts.add(MapUtil.calcBoolcolor(flagName, value));
		}
		ret = ret.replace("%flags.map%", String.join("<br>\n", flagTableParts));
		for (int cols = 1; cols <= 10; cols++)
			ret = ret.replace("%flags.table" + cols + "%", MapUtil.getHtmlAsciTable(flagTableParts, cols));

		List<com.massivecraft.factions.entity.MPlayer> playersList = faction.getMPlayers();
		ret = ret.replace("%players%", MapUtil.getHtmlPlayerString(playersList));
		ret = ret.replace("%players.count%", String.valueOf(playersList.size()));
		ret = ret.replace("%players.leader%", MapUtil.getHtmlPlayerName(faction.getLeader()));
		DecimalFormat df = new DecimalFormat("#.##");
		ret = ret.replace("%power%", df.format(faction.getPower()));
		ret = ret.replace("%maxpower%", df.format(faction.getPowerMax()));
		ret = ret.replace("%claims%", String.valueOf(faction.getLandCount()));
		return ret;
	}

	/**
	 * Gets the style for the given faction.
	 * 
	 * @param faction The faction
	 * @return The style for the given faction
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
	 * Logs BlueMap update timing to the Factions log when {@link MConf#bluemapLogTimeSpent} is true.
	 *
	 * @param name  Label for the phase (e.g. "Build", "Sync")
	 * @param start Start time in milliseconds (e.g. from {@link System#currentTimeMillis()})
	 */
	private static void logTimeSpent(String name, long start)
	{
		if (!MConf.get().bluemapLogTimeSpent) return;
		long duration = System.currentTimeMillis() - start;
		Factions.get().log(Txt.parse("<i>BlueMap %s took <h>%dms<i>.", name, duration));
	}

	/**
	 * Logs a severe error message to the Factions log in red.
	 *
	 * @param msg The error message to log
	 */
	public static void logSevere(String msg)
	{
		Factions.get().log(ChatColor.RED + msg);
	}
}
