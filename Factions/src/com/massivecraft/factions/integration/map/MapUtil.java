package com.massivecraft.factions.integration.map;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.massivecore.apachecommons.StringEscapeUtils;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.util.TimeDiffUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared utilities for map plugin integrations (Dynmap, BlueMap, etc.).
 *
 * <p>
 * Contains helper methods for:
 * <ul>
 * <li>Default line and fill colors from {@link MConf} (mapDefaultLineColor, mapDefaultFillColor, defaultFactionPrimaryColor, defaultFactionSecondaryColor)</li>
 * <li>HTML formatting for description popups (tables, player lists, boolean colors, placeholder replacement)</li>
 * <li>Building the full faction description HTML from {@link MConf#mapDescriptionWindowFormat} ({@link #getFactionDescriptionHtml})</li>
 * <li>Faction visibility checks using shared {@link MConf#mapVisibleFactions} and {@link MConf#mapHiddenFactions}</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class cannot be instantiated.
 * </p>
 */
public final class MapUtil
{
	// -------------------------------------------- //
	// CONSTRUCTOR (PRIVATE)
	// -------------------------------------------- //

	private MapUtil()
	{
		// Utility class - prevent instantiation
	}

	// -------------------------------------------- //
	// COLOR CONFIGURATION
	// -------------------------------------------- //

	/**
	 * Gets the default line color for territory outlines following the configuration hierarchy:
	 * <ol>
	 * <li>{@link MConf#mapDefaultLineColor} (if set and valid hex)</li>
	 * <li>{@link MConf#defaultFactionSecondaryColor} (if valid)</li>
	 * <li>{@link MapStyle#DEFAULT_LINE_COLOR}</li>
	 * </ol>
	 *
	 * @return A valid hex color string (e.g. "#00FF00")
	 */
	public static String getDefaultLineColor()
	{
		MConf conf = MConf.get();
		if (conf.mapDefaultLineColor != null && !conf.mapDefaultLineColor.trim().isEmpty())
		{
			String color = conf.mapDefaultLineColor.trim();
			if (color.matches("^#[0-9A-Fa-f]{6}$")) return color;
		}
		if (conf.defaultFactionSecondaryColor != null && !conf.defaultFactionSecondaryColor.trim().isEmpty())
		{
			String color = conf.defaultFactionSecondaryColor.trim();
			if (color.matches("^#[0-9A-Fa-f]{6}$")) return color;
		}
		return MapStyleDefaults.DEFAULT_LINE_COLOR;
	}

	/**
	 * Gets the default fill color for territory interiors following the configuration hierarchy:
	 * <ol>
	 * <li>{@link MConf#mapDefaultFillColor} (if set and valid hex)</li>
	 * <li>{@link MConf#defaultFactionPrimaryColor} (if valid)</li>
	 * <li>{@link MapStyle#DEFAULT_FILL_COLOR}</li>
	 * </ol>
	 *
	 * @return A valid hex color string (e.g. "#00FF00")
	 */
	public static String getDefaultFillColor()
	{
		MConf conf = MConf.get();
		if (conf.mapDefaultFillColor != null && !conf.mapDefaultFillColor.trim().isEmpty())
		{
			String color = conf.mapDefaultFillColor.trim();
			if (color.matches("^#[0-9A-Fa-f]{6}$")) return color;
		}
		if (conf.defaultFactionPrimaryColor != null && !conf.defaultFactionPrimaryColor.trim().isEmpty())
		{
			String color = conf.defaultFactionPrimaryColor.trim();
			if (color.matches("^#[0-9A-Fa-f]{6}$")) return color;
		}
		return MapStyleDefaults.DEFAULT_FILL_COLOR;
	}

	/**
	 * Resolves the line color for territory styling: style's line color if set, otherwise default.
	 *
	 * @param style Map style (may be null)
	 * @return A valid hex color string for the line/outline
	 */
	public static String getResolvedLineColor(MapStyle style)
	{
		if (style != null && style.getLineColor() != null && !style.getLineColor().trim().isEmpty())
			return style.getLineColor().trim();
		return getDefaultLineColor();
	}

	/**
	 * Resolves the fill color for territory styling: style's fill color if set, otherwise default.
	 *
	 * @param style Map style (may be null)
	 * @return A valid hex color string for the fill
	 */
	public static String getResolvedFillColor(MapStyle style)
	{
		if (style != null && style.getFillColor() != null && !style.getFillColor().trim().isEmpty())
			return style.getFillColor().trim();
		return getDefaultFillColor();
	}

	// -------------------------------------------- //
	// HTML FORMATTING
	// -------------------------------------------- //

	/**
	 * Formats a collection of strings into an HTML-style table with the specified number of columns.
	 * Uses " | " between cells and "&lt;br&gt;" after each row.
	 *
	 * @param strings Collection of strings to format
	 * @param cols    Number of columns per row
	 * @return Formatted string (e.g. for flag tables in description popups)
	 */
	public static String getHtmlAsciTable(Collection<String> strings, int cols)
	{
		StringBuilder ret = new StringBuilder();
		int count = 0;
		for (Iterator<String> iter = strings.iterator(); iter.hasNext(); )
		{
			ret.append(iter.next());
			count++;
			if (iter.hasNext())
				ret.append(count % cols == 0 ? "<br>" : " | ");
		}
		return ret.toString();
	}

	/**
	 * Converts a list of players into a comma- and dot-separated HTML-safe string.
	 *
	 * @param mplayers List of players (may be null or empty)
	 * @return Comma- and dot-separated player names, HTML-escaped
	 */
	public static String getHtmlPlayerString(List<MPlayer> mplayers)
	{
		List<String> names = mplayers.stream().map(MapUtil::getHtmlPlayerName).collect(Collectors.toList());
		return Txt.implodeCommaAndDot(names);
	}

	/**
	 * Gets the HTML-escaped name of a player for use in description popups.
	 *
	 * @param mplayer The player (can be null)
	 * @return HTML-escaped player name, or "none" if null
	 */
	public static String getHtmlPlayerName(MPlayer mplayer)
	{
		if (mplayer == null) return "none";
		return StringEscapeUtils.escapeHtml(mplayer.getName());
	}

	/**
	 * Wraps a string in an HTML span with color based on boolean value (green for true, red for false).
	 * Respects {@link MConf#mapUseDarkModeColors} for brighter colors in dark mode.
	 *
	 * @param string The string to wrap
	 * @param bool   The boolean determining color
	 * @return HTML span element with colored text
	 */
	public static String calcBoolcolor(String string, boolean bool)
	{
		if (MConf.get().mapUseDarkModeColors)
			return "<span style=\"color: " + (bool ? "#00FF00" : "#FF0000") + ";\">" + string + "</span>";
		return "<span style=\"color: " + (bool ? "#008000" : "#800000") + ";\">" + string + "</span>";
	}

	/**
	 * Replaces a placeholder in HTML with an HTML-escaped value.
	 * Strips Minecraft color codes from the replacement before escaping.
	 *
	 * @param ret     The HTML string containing placeholders (e.g. "%name%")
	 * @param target  The placeholder name without % symbols
	 * @param replace The replacement value (will be HTML-escaped)
	 * @return Updated HTML string
	 * @throws NullPointerException if any argument is null
	 */
	public static String addToHtml(String ret, String target, String replace)
	{
		if (ret == null || target == null || replace == null) throw new NullPointerException();
		target = "%" + target + "%";
		replace = ChatColor.stripColor(replace);
		replace = StringEscapeUtils.escapeHtml(replace);
		return ret.replace(target, replace);
	}

	/**
	 * Builds the HTML description for a faction for use in map integration popups (Dynmap, BlueMap, SquareMap).
	 * Uses {@link MConf#mapDescriptionWindowFormat} and replaces placeholders: name, description, motd, age,
	 * money, flags (per-flag and table/map), players, power, claims.
	 *
	 * @param faction The faction to describe (must not be null)
	 * @return HTML string for the description popup, wrapped in a div with class "regioninfo"
	 * @throws NullPointerException if faction is null
	 */
	public static String getFactionDescriptionHtml(Faction faction)
	{
		if (faction == null) throw new NullPointerException("faction");
		String ret = "<div class=\"regioninfo\">" + MConf.get().mapDescriptionWindowFormat + "</div>";

		// Name
		ret = addToHtml(ret, "name", faction.getName());

		// Description
		String description = faction.getDescriptionDesc();
		ret = addToHtml(ret, "description", description);

		// MOTD
		String motd = faction.getMotd();
		if (motd != null) ret = addToHtml(ret, "motd", motd);

		// Age
		long ageMillis = faction.getAge();
		LinkedHashMap<TimeUnit, Long> ageUnitcounts = TimeDiffUtil.limit(TimeDiffUtil.unitcounts(ageMillis, TimeUnit.getAllButMillisSecondsAndMinutes()), 3);
		String age = TimeDiffUtil.formatedVerboose(ageUnitcounts);
		ret = addToHtml(ret, "age", age);

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
			String flagTable = getHtmlAsciTable(flagTableParts, cols);
			ret = ret.replace("%flags.table" + cols + "%", flagTable);
		}

		// Players
		List<MPlayer> playersList = faction.getMPlayers();
		String playersCount = String.valueOf(playersList.size());
		String players = getHtmlPlayerString(playersList);

		MPlayer playersLeaderObject = faction.getLeader();
		String playersLeader = getHtmlPlayerName(playersLeaderObject);

		DecimalFormat df = new DecimalFormat("#.##");

		ret = ret.replace("%players%", players);
		ret = ret.replace("%players.count%", playersCount);
		ret = ret.replace("%players.leader%", playersLeader);
		ret = ret.replace("%power%", df.format(faction.getPower()));
		ret = ret.replace("%maxpower%", df.format(faction.getPowerMax()));
		ret = ret.replace("%claims%", String.valueOf(faction.getLandCount()));

		return ret;
	}

	// -------------------------------------------- //
	// VISIBILITY
	// -------------------------------------------- //

	/**
	 * Checks whether a faction should be shown on the map for the given world.
	 * Uses shared {@link MConf#mapVisibleFactions} (whitelist; if non-empty, faction or world must match)
	 * and {@link MConf#mapHiddenFactions} (blacklist; if matched, faction is hidden).
	 * Faction can be matched by ID, name, or "world:&lt;worldname&gt;" for world-level visibility.
	 *
	 * @param factionId   Faction UUID (must not be null)
	 * @param factionName Faction name (must not be null)
	 * @param world       World name (must not be null)
	 * @return true if the faction should be visible on the map
	 * @throws NullPointerException if any argument is null
	 */
	public static boolean isFactionVisible(String factionId, String factionName, String world)
	{
		if (factionId == null || factionName == null || world == null) throw new NullPointerException();
		Set<String> ids = com.massivecraft.massivecore.util.MUtil.set(factionId, factionName, "world:" + world);
		Set<String> visible = MConf.get().mapVisibleFactions;
		Set<String> hidden = MConf.get().mapHiddenFactions;
		if (!visible.isEmpty() && visible.stream().noneMatch(ids::contains))
			return false;
		if (!hidden.isEmpty() && hidden.stream().anyMatch(ids::contains))
			return false;
		return true;
	}
}
