package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.apachecommons.StringEscapeUtils;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for Dynmap integration.
 * Contains helper methods for HTML formatting, color management, and configuration access.
 */
public class DynmapUtil
{
	// -------------------------------------------- //
	// CONSTRUCTOR (PRIVATE)
	// -------------------------------------------- //
	
	private DynmapUtil()
	{
		// Utility class - prevent instantiation
	}
	
	// -------------------------------------------- //
	// COLOR CONFIGURATION
	// -------------------------------------------- //
	
	/**
	 * Gets the Dynmap default line color following the configuration hierarchy:
	 * <ol>
	 * <li>dynmapDefaultLineColor (if set and valid)</li>
	 * <li>defaultFactionSecondaryColor (if valid)</li>
	 * <li>Hard-coded constant from IntegrationDynmap</li>
	 * </ol>
	 * 
	 * @return A valid hex color string
	 */
	public static String getDefaultLineColor()
	{
		MConf conf = MConf.get();
		
		// Check Dynmap-specific override
		if (conf.dynmapDefaultLineColor != null && !conf.dynmapDefaultLineColor.trim().isEmpty())
		{
			String color = conf.dynmapDefaultLineColor.trim();
			if (color.matches("^#[0-9A-Fa-f]{6}$")) return color;
		}
		
		// Check generic default faction secondary color
		if (conf.defaultFactionSecondaryColor != null && !conf.defaultFactionSecondaryColor.trim().isEmpty())
		{
			String color = conf.defaultFactionSecondaryColor.trim();
			if (color.matches("^#[0-9A-Fa-f]{6}$")) return color;
		}
		
		// Fallback to hardcoded constant
		return IntegrationDynmap.DYNMAP_STYLE_LINE_COLOR;
	}
	
	/**
	 * Gets the Dynmap default fill color following the configuration hierarchy:
	 * <ol>
	 * <li>dynmapDefaultFillColor (if set and valid)</li>
	 * <li>defaultFactionPrimaryColor (if valid)</li>
	 * <li>Hard-coded constant from IntegrationDynmap</li>
	 * </ol>
	 * 
	 * @return A valid hex color string
	 */
	public static String getDefaultFillColor()
	{
		MConf conf = MConf.get();
		
		// Check Dynmap-specific override
		if (conf.dynmapDefaultFillColor != null && !conf.dynmapDefaultFillColor.trim().isEmpty())
		{
			String color = conf.dynmapDefaultFillColor.trim();
			if (color.matches("^#[0-9A-Fa-f]{6}$")) return color;
		}
		
		// Check generic default faction primary color
		if (conf.defaultFactionPrimaryColor != null && !conf.defaultFactionPrimaryColor.trim().isEmpty())
		{
			String color = conf.defaultFactionPrimaryColor.trim();
			if (color.matches("^#[0-9A-Fa-f]{6}$")) return color;
		}
		
		// Fallback to hardcoded constant
		return IntegrationDynmap.DYNMAP_STYLE_FILL_COLOR;
	}
	
	// -------------------------------------------- //
	// HTML FORMATTING
	// -------------------------------------------- //
	
	/**
	 * Formats a collection of strings into an HTML table with the specified number of columns.
	 * 
	 * @param strings Collection of strings to format
	 * @param cols Number of columns in the table
	 * @return HTML formatted string
	 */
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
	
	/**
	 * Converts a list of players into a comma-separated HTML string.
	 * 
	 * @param mplayers List of players
	 * @return Comma and dot separated player names
	 */
	public static String getHtmlPlayerString(List<MPlayer> mplayers)
	{
		List<String> names = mplayers.stream().map(DynmapUtil::getHtmlPlayerName).collect(Collectors.toList());
		return Txt.implodeCommaAndDot(names);
	}
	
	/**
	 * Gets the HTML-escaped name of a player.
	 * 
	 * @param mplayer The player (can be null)
	 * @return HTML-escaped player name or "none" if null
	 */
	public static String getHtmlPlayerName(MPlayer mplayer)
	{
		if (mplayer == null) return "none";
		return StringEscapeUtils.escapeHtml(mplayer.getName());
	}
	
	/**
	 * Wraps a string in HTML span with color based on boolean value.
	 * Green for true, red for false.
	 * 
	 * @param string The string to wrap
	 * @param bool The boolean determining color
	 * @return HTML span element with colored text
	 */
	public static String calcBoolcolor(String string, boolean bool)
	{
		// For dark mode, use brighter colors
		if (MConf.get().dynmapUseDarkModeColors)
		{
			return "<span style=\"color: " + (bool ? "#00FF00" : "#FF0000") + ";\">" + string + "</span>";
		}
		return "<span style=\"color: " + (bool ? "#008000" : "#800000") + ";\">" + string + "</span>";
	}

	/**
	 * Replaces a placeholder in HTML with an HTML-escaped value.
	 * 
	 * @param ret The HTML string containing placeholders
	 * @param target The placeholder name (without % symbols)
	 * @param replace The replacement value (will be HTML-escaped)
	 * @return Updated HTML string
	 */
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
}
