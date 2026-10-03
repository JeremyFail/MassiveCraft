package com.massivecraft.massivecore.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for converting between millisecond durations, {@link TimeUnit} breakdowns,
 * and human-readable time-diff strings (parse and format).
 */
public class TimeDiffUtil
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	/**
	 * Matches a full time-diff string made of one or more count+unit parts (e.g. {@code 1d2h30m}).
	 */
	public final static Pattern patternFull = Pattern.compile("^(?:([^a-zA-Z]+)([a-zA-Z]*))+$");
	
	/**
	 * Matches a single count+unit part within a time-diff string.
	 */
	public final static Pattern patternPart = Pattern.compile("([^a-zA-Z]+)([a-zA-Z]*)");
	
	// -------------------------------------------- //
	// MILLIS
	// -------------------------------------------- //
	
	/**
	 * Converts a single unit count to milliseconds.
	 *
	 * @param timeUnit the unit
	 * @param count how many of that unit
	 * @return {@code timeUnit.millis * count}
	 */
	public static long millis(TimeUnit timeUnit, long count)
	{
		return timeUnit.millis*count;
	}
	
	/**
	 * Converts one of the given unit to milliseconds.
	 *
	 * @param timeUnit the unit
	 * @return milliseconds for a count of {@code 1}
	 */
	public static long millis(TimeUnit timeUnit)
	{
		return millis(timeUnit, 1);
	}
	
	/**
	 * Sums a unit-count map to milliseconds, optionally scaled by {@code count}.
	 *
	 * @param unitcounts map of unit to amount
	 * @param count multiplier applied to every amount
	 * @return total milliseconds
	 */
	public static long millis(Map<TimeUnit, Long> unitcounts, long count)
	{
		long ret = 0;
		for (Entry<TimeUnit, Long> entry : unitcounts.entrySet())
		{
			ret += millis(entry.getKey(), entry.getValue()*count);
		}
		return ret;
	}
	
	/**
	 * Sums a unit-count map to milliseconds.
	 *
	 * @param unitcounts map of unit to amount
	 * @return total milliseconds
	 */
	public static long millis(Map<TimeUnit, Long> unitcounts)
	{
		return millis(unitcounts, 1);
	}
	
	/**
	 * Parses a time-diff string and converts it to milliseconds, scaled by {@code count}.
	 *
	 * @param formated time-diff text (e.g. {@code 1d2h})
	 * @param count multiplier applied after parsing
	 * @return total milliseconds
	 * @throws Exception if the string cannot be parsed
	 */
	public static long millis(String formated, long count) throws Exception
	{
		Map<TimeUnit, Long> unitcount = unitcounts(formated);
		return millis(unitcount, count);
	}
	
	/**
	 * Parses a time-diff string and converts it to milliseconds.
	 *
	 * @param formated time-diff text (e.g. {@code 1d2h})
	 * @return total milliseconds
	 * @throws Exception if the string cannot be parsed
	 */
	public static long millis(String formated) throws Exception
	{
		return millis(formated, 1);
	}
	
	// -------------------------------------------- //
	// UNITCOUNT
	// -------------------------------------------- //
	
	/**
	 * Parses a compact time-diff string into a unit-count map.
	 *
	 * @param formated time-diff text (e.g. {@code 1d2h30m}); {@code "0"} yields an empty map
	 * @return ordered map of {@link TimeUnit} to count
	 * @throws Exception if the string is null, malformed, or contains unknown/duplicate units
	 */
	public static LinkedHashMap<TimeUnit, Long> unitcounts(String formated) throws Exception
	{
		if (formated == null) throw new NullPointerException("The string can't be null.");
		
		Matcher matcherFull = patternFull.matcher(formated);
		if (!matcherFull.matches()) throw new NullPointerException("Invalid time diff format.");
		
		LinkedHashMap<TimeUnit, Long> ret = new LinkedHashMap<>();
		if (formated.equals("0")) return ret;
		
		Matcher matcherPart = patternPart.matcher(formated);
		while (matcherPart.find())
		{
			// Parse the count
			String countString = matcherPart.group(1);
			String countStringFixed = countString.replaceAll("[+\\s]", "");
			long count = 0;
			try
			{
				count = Long.parseLong(countStringFixed);
			}
			catch (Exception e)
			{
				throw new Exception("\""+countString+"\" is not a valid integer.");
			}
			
			// Parse the time unit
			String unitString = matcherPart.group(2);
			TimeUnit unit = TimeUnit.get(unitString);
			if (unit == null)
			{
				throw new Exception("\""+unitString+"\" is not a valid time unit.");
			}
			
			// Add to the return map
			if (ret.put(unit, count) != null)
			{
				throw new Exception("Multiple "+unit.singularName+" entries is not allowed.");
			}
		}
		
		return ret;
	}
	
	/**
	 * Breaks a millisecond duration into whole units from the given set (largest first).
	 * The sign of {@code millis} is ignored. Remainder smaller than the smallest unit is dropped.
	 *
	 * @param millis duration in milliseconds
	 * @param units units to use, typically largest-to-smallest (as in {@link TimeUnit#getAll()})
	 * @return map of units that had a non-zero whole count
	 */
	public static LinkedHashMap<TimeUnit, Long> unitcounts(long millis, TreeSet<TimeUnit> units)
	{
		// Create non-negative millis decumulator
		long millisLeft = Math.abs(millis);
		
		LinkedHashMap<TimeUnit, Long> ret = new LinkedHashMap<>();
		
		for (TimeUnit unit : units)
		{
			long count = (long) Math.floor(millisLeft / unit.millis);
			if (count < 1) continue;
			millisLeft -= unit.millis*count;
			ret.put(unit, count);
		}
		
		return ret;
	}
	
	/**
	 * Breaks a millisecond duration into all registered {@link TimeUnit}s.
	 *
	 * @param millis duration in milliseconds
	 * @return map of units that had a non-zero whole count
	 */
	public static LinkedHashMap<TimeUnit, Long> unitcounts(long millis)
	{
		return unitcounts(millis, TimeUnit.getAll());
	}
	
	/**
	 * Keeps only the first {@code limit} entries of a unit-count map (iteration order).
	 *
	 * @param unitcounts source map
	 * @param limit maximum number of entries to retain
	 * @return a new map with at most {@code limit} entries
	 */
	public static LinkedHashMap<TimeUnit, Long> limit(LinkedHashMap<TimeUnit, Long> unitcounts, int limit)
	{
		LinkedHashMap<TimeUnit, Long> ret = new LinkedHashMap<>();
		
		Iterator<Entry<TimeUnit, Long>> iter = unitcounts.entrySet().iterator();
		int i = 0;
		while (iter.hasNext() && i < limit)
		{
			Entry<TimeUnit, Long> entry = iter.next();
			ret.put(entry.getKey(), entry.getValue());
			i++;
		}
		
		return ret;
	}
	
	// -------------------------------------------- //
	// FORMAT
	// -------------------------------------------- //
	
	/**
	 * Verbose single-entry format: colored count and full unit name (e.g. {@code 5 minutes}).
	 * Format args: {@code %1$d} count, {@code %2$s} short unit, {@code %3$s} full name.
	 */
	public static final String FORMAT_ENTRY_VERBOOSE = Txt.parse("<v>%1$d <k>%3$s");
	
	/**
	 * Verbose list comma separator; {@code %s} is replaced with a parsed color code.
	 */
	public static final String FORMAT_COMMA_VERBOOSE = "%s, ";
	
	/**
	 * Verbose list “and” separator; {@code %s} is replaced with a parsed color code.
	 */
	public static final String FORMAT_AND_VERBOOSE = " %sand ";
	
	/**
	 * Minimal single-entry format: colored count and short unit (e.g. {@code 5min}).
	 * Format args: {@code %1$d} count, {@code %2$s} short unit, {@code %3$s} full name.
	 */
	public static final String FORMAT_ENTRY_MINIMAL = Txt.parse("<v>%1$d<k>%2$s");
	
	/**
	 * Minimal list comma separator (no extra text between entries beyond the color arg).
	 */
	public static final String FORMAT_COMMA_MINIMAL = "%s";
	
	/**
	 * Minimal list “and” separator (no extra text between entries beyond the color arg).
	 */
	public static final String FORMAT_AND_MINIMAL = "%s";
	
	/**
	 * Formats one unit count with a {@link String#format} entry pattern.
	 *
	 * @param unit the time unit
	 * @param count how many of that unit
	 * @param formatEntry pattern with args count, short unit, full name
	 * @return formatted entry string
	 */
	public static String formated(TimeUnit unit, long count, String formatEntry)
	{
		return String.format(formatEntry, count, unit.getUnitString(count), unit.getNameString(count));
	}
	
	/**
	 * Formats a unit-count map as a comma/and list using the given separators and color.
	 * An empty map formats as {@code 0} seconds with {@code entryFormat}.
	 *
	 * @param unitcounts map of unit to count
	 * @param entryFormat per-entry {@link String#format} pattern
	 * @param commaFormat comma separator pattern ({@code %s} = color)
	 * @param andFormat and separator pattern ({@code %s} = color)
	 * @param color MassiveCore color tag (e.g. {@code <i>}) passed through {@link Txt#parse(String)}
	 * @return formatted duration list
	 */
	public static String formated(Map<TimeUnit, Long> unitcounts, String entryFormat, String commaFormat, String andFormat, String color)
	{
		String comma = String.format(commaFormat, Txt.parse(color));
		String and = String.format(andFormat, Txt.parse(color));
		
		if (unitcounts.isEmpty())
		{
			return formated(TimeUnit.SECOND, 0, entryFormat);
		}
		
		List<String> parts = new ArrayList<>();
		for (Entry<TimeUnit, Long> entry : unitcounts.entrySet())
		{
			parts.add(formated(entry.getKey(), entry.getValue(), entryFormat));
		}
		return Txt.implodeCommaAnd(parts, comma, and);
	}
	
	/**
	 * Formats one unit count in verbose style.
	 *
	 * @param unit the time unit
	 * @param count how many of that unit
	 * @return verbose entry (e.g. colored {@code 5 minutes})
	 */
	public static String formatedVerboose(TimeUnit unit, long count)
	{
		return formated(unit, count, FORMAT_ENTRY_VERBOOSE);
	}
	
	/**
	 * Formats a unit-count map in verbose style with the given separator color.
	 *
	 * @param unitcounts map of unit to count
	 * @param color MassiveCore color tag for commas/and
	 * @return verbose list string
	 */
	public static String formatedVerboose(Map<TimeUnit, Long> unitcounts, String color)
	{
		return formated(unitcounts, FORMAT_ENTRY_VERBOOSE, FORMAT_COMMA_VERBOOSE, FORMAT_AND_VERBOOSE, color);
	}
	
	/**
	 * Formats a unit-count map in verbose style with default {@code <i>} separator color.
	 *
	 * @param unitcounts map of unit to count
	 * @return verbose list string
	 */
	public static String formatedVerboose(Map<TimeUnit, Long> unitcounts)
	{
		return formatedVerboose(unitcounts, "<i>");
	}
	
	/**
	 * Formats one unit count in minimal style.
	 *
	 * @param unit the time unit
	 * @param count how many of that unit
	 * @return minimal entry (e.g. colored {@code 5min})
	 */
	public static String formatedMinimal(TimeUnit unit, long count)
	{
		return formated(unit, count, FORMAT_ENTRY_MINIMAL);
	}
	
	/**
	 * Formats a unit-count map in minimal style with the given separator color.
	 *
	 * @param unitcounts map of unit to count
	 * @param color MassiveCore color tag for separators
	 * @return minimal list string
	 */
	public static String formatedMinimal(Map<TimeUnit, Long> unitcounts, String color)
	{
		return formated(unitcounts, FORMAT_ENTRY_MINIMAL, FORMAT_COMMA_MINIMAL, FORMAT_AND_MINIMAL, color);
	}
	
	/**
	 * Formats a unit-count map in minimal style with no separator color.
	 *
	 * @param unitcounts map of unit to count
	 * @return minimal list string
	 */
	public static String formatedMinimal(Map<TimeUnit, Long> unitcounts)
	{
		return formatedMinimal(unitcounts, "");
	}
	
	// -------------------------------------------- //
	// COUNTDOWN / COOLDOWN DISPLAY
	// -------------------------------------------- //
	
	/**
	 * Formats a remaining duration for cooldown / “please wait” messages.
	 * <p>
	 * Milliseconds are rounded <b>up</b> to whole seconds. Zero-count units are omitted.
	 * Uses hours, minutes, and seconds only (e.g. {@code 2 hours 5 minutes 1 second},
	 * {@code 5 minutes 30 seconds}, or {@code 45 seconds}).
	 * </p>
	 *
	 * @param millis remaining duration in milliseconds (sign ignored)
	 * @return plain human-readable duration string (no MassiveCore color tags)
	 */
	public static String formattedCountdown(long millis)
	{
		long abs = Math.abs(millis);
		// Round up to the next whole second so a partial second still shows as at least 1 second
		long ceilMillis = abs == 0L ? 0L
			: ((abs + TimeUnit.MILLIS_PER_SECOND - 1L) / TimeUnit.MILLIS_PER_SECOND) * TimeUnit.MILLIS_PER_SECOND;
		
		LinkedHashMap<TimeUnit, Long> unitcounts = unitcounts(
			ceilMillis,
			TimeUnit.getSpecific(TimeUnit.HOUR, TimeUnit.MINUTE, TimeUnit.SECOND)
		);
		
		if (unitcounts.isEmpty())
		{
			return "0 seconds";
		}
		
		List<String> parts = new ArrayList<>();
		for (Entry<TimeUnit, Long> entry : unitcounts.entrySet())
		{
			long count = entry.getValue();
			parts.add(count + " " + entry.getKey().getNameString(count));
		}
		return String.join(" ", parts);
	}
	
}
