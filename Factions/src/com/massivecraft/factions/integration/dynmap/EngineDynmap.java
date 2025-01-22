package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
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
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private MarkerSet markerset;

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

	public void perform()
	{
		long before = System.currentTimeMillis();

		// We do what we can here.
		// You /can/ run this method from the main server thread but it's not recommended at all.
		// This method is supposed to be run async to avoid locking the main server thread.
		//final Map<String, TempMarker> homes = createHomes();
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

		// createLayer() is thread safe but it makes use of fields set in fetchDynmapAPI() so we must have it after.
		if (!updateLayer(createLayer())) return;

		updateAreas(areas);

		logTimeSpent("Sync", before);
	}

	public void disable()
	{
		if (this.markerset != null)
		{
			this.markerset.deleteMarkerSet();
			this.markerset = null;
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
	// UPDATE: Layer
	// -------------------------------------------- //
	
	// Thread Safe / Asynchronous: Yes
	public LayerValues createLayer()
	{
		return new LayerValues(
			MConf.get().dynmapLayerName,
			MConf.get().dynmapLayerMinimumZoom,
			MConf.get().dynmapLayerPriority,
			MConf.get().dynmapLayerHiddenByDefault
		);
	}
	
	// Thread Safe / Asynchronous: No
	public boolean updateLayer(LayerValues temp)
	{
		this.markerset = temp.ensureExistsAndUpdated(this.markerApi, IntegrationDynmap.FACTIONS_MARKERSET);
		return this.markerset != null;
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
			Iterator<PS> it = allChunksSource.iterator();
			PS somePs = it.next();

			List<PS> linelist = new MassiveList<>();
			// Create the polygon
			Set<PS> polygonChunks = new MassiveSet<>();
			floodFillTarget(allChunksSource, polygonChunks, somePs);

			List<PS> polygonCorners = new MassiveList<>();
			for (PS chunk : polygonChunks)
			{
				for (Direction d : Direction.values())
				{
					polygonCorners.add(d.getCorner(chunk));
				}
			}

			Set<PS> points = new MassiveSet<>();
			for (PS corner : polygonCorners)
			{
				if (points.contains(corner))
					points.remove(corner);
				else
					points.add(corner);
			}

			Map<PS, PS> edges_h = new MassiveMap<>();
			Map<PS, PS> edges_v = new MassiveMap<>();

			List<PS> sorted_x = new MassiveList<>(points);
			Collections.sort(sorted_x, this::xThenZ);
			List<PS> sorted_z = new MassiveList<>(points);
			Collections.sort(sorted_z, this::zThenX);

			// Create horizontal edges
			for (int i = 0; i < points.size();)
			{
				int curr_z = sorted_z.get(i).getChunkZ();
				while (i < points.size() && curr_z == sorted_z.get(i+1).getChunkZ())
				{
					edges_h.put(sorted_z.get(i), sorted_z.get(i+1));
					edges_h.put(sorted_z.get(i+1), sorted_z.get(i));
					i += 2;
				}
			}

			// Create vertical edges
			for (int i = 0; i < points.size();)
			{
				int curr_x = sorted_x.get(i).getChunkX();
				while (i < points.size() && curr_x == sorted_x.get(i+1).getChunkX())
				{
					edges_v.put(sorted_x.get(i), sorted_x.get(i+1));
					edges_v.put(sorted_x.get(i+1), sorted_x.get(i));
					i += 2;
				}
			}

			//List<PS> linelist = getLineList(polygonChunks);


			linelist = new MassiveList<>(new LinkedHashSet<>(linelist));

			for (Direction d : Direction.values())
			{
				linelist.add(d.getCorner(somePs));
			}

			// Build information for specific area
			String markerId = calcMarkerId(world, faction);
			AreaMarkerValues values = new AreaMarkerValues(faction.getName(), world, linelist.toArray(new PS[]{}), description, style);
			ret.put(markerId, values);
		}
		
		return ret;
	}

	private int zThenX(PS a, PS b)
	{
		if (a.getChunkZ() < b.getChunkZ() || (a.getChunkZ().equals(b.getChunkZ()) && a.getChunkX() < b.getChunkX()))
			return -1;
		else if (a.equals(b))
			return 0;
		else
			return 1;
	}

	private int xThenZ(PS a, PS b)
	{
		if (a.getChunkX() < b.getChunkX() || (a.getChunkX().equals(b.getChunkX()) && a.getChunkZ() < b.getChunkZ()))
			return -1;
		else if (a.equals(b))
			return 0;
		else
			return 1;
	}

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

	// XPLUS, ZPLUS, XMINUS, ZMINUS
	private static List<PS> getLineList(Set<PS> polygonChunks)
	{
		PS minimumChunk = getMinimum(polygonChunks);

		//final int initialX = minimumChunk.getChunkX();
		//final int initialZ = minimumChunk.getChunkZ();
		//int currentX = initialX;
		//int currentZ = initialZ;

		PS currentChunk = minimumChunk;

		Direction direction = Direction.XPLUS;
		List<PS> linelist = new MassiveList<>();

		linelist.add(minimumChunk); // Add start point
		while ((!currentChunk.equals(minimumChunk)) || (direction != Direction.ZMINUS))
		{
			PS adjacent = direction.adjacent(currentChunk);
			PS corner = direction.getCorner(currentChunk);
			// If the adjacent chunk is not present

			if (!polygonChunks.contains(adjacent))
			{ // Right turn?
				linelist.add(corner); // Finish line
				direction = direction.turnRight(); // Change direction
			}

			// If the chunk left of the adjacent is not present
			else if (!polygonChunks.contains(direction.turnLeft().adjacent(adjacent)))
			{ // Straight?
				currentChunk = adjacent;
			}

			else
			{ // Left turn
				linelist.add(corner); // Finish line
				direction = direction.turnLeft();

				// Left turn of adjacent
				currentChunk = direction.adjacent(adjacent);
			}
		}

		return linelist;
	}

	// IS CLAIMED

	private static boolean isSoutheastClaimed(PS ps, Collection<PS> polygon)
	{
		return polygon.contains(PS.valueOf(ps.getChunkX() - 1, ps.getChunkZ() + 1));
	}

	private static boolean isNortheastClaimed(PS ps, Collection<PS> polygon)
	{
		return polygon.contains(PS.valueOf(ps.getChunkX() + 1, ps.getChunkZ() + 1));
	}

	private static boolean isSouthwestClaimed(PS ps, Collection<PS> polygon)
	{
		return polygon.contains(PS.valueOf(ps.getChunkX() - 1, ps.getChunkZ() - 1));
	}

	private static boolean isNorthwestClaimed(PS ps, Collection<PS> polygon)
	{
		return polygon.contains(PS.valueOf(ps.getChunkX() + 1, ps.getChunkZ() - 1));
	}

	// GET CHUNKS

	private static PS getNortheastPS(PS ps)
	{
		return PS.valueOf(ps.getChunkX() + 1, ps.getChunkZ() + 1);
	}

	private static PS getSoutheastPS(PS ps)
	{
		return PS.valueOf(ps.getChunkX(), ps.getChunkZ() + 1);
	}

	private static PS getSouthwestPS(PS ps)
	{
		return PS.valueOf(ps.getChunkX(), ps.getChunkZ());
	}

	private static PS getNorthwestPS(PS ps)
	{
		return PS.valueOf(ps.getChunkX() + 1, ps.getChunkZ());
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
		this.markerset.getAreaMarkers().stream() // Get current markers
			.filter(am -> !values.containsKey(am.getMarkerID())) // That are not in the new map
			.forEach(AreaMarker::deleteMarker); // and delete them


		// Map Current
		Map<String, AreaMarker> markers = getMarkerMap(this.markerset);

		// Loop New
		values.forEach((markerId, value) ->
						  value.ensureExistsAndUpdated(markers.get(markerId), this.markerApi, this.markerset, markerId));

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
		String age = TimeDiffUtil.formatedVerboose(ageUnitcounts, "");
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

		return DynmapStyle.coalesce(
			styles.get(faction.getId()),
			styles.get(faction.getName()),
			MConf.get().dynmapDefaultStyle
		);
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
