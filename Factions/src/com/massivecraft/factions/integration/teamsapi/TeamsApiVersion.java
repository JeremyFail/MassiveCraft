package com.massivecraft.factions.integration.teamsapi;

import com.skyblockexp.teamsapi.api.TeamsAPI;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TeamsAPI semantic-version gate for MassiveCraft Factions provider integration.
 * <p>
 * Factions registers TeamsAPI providers only when the runtime library reports
 * {@link #MINIMUM_API_VERSION} or newer (role prefix and {@link com.skyblockexp.teamsapi.model.TeamRoleDefinition}
 * support). Older API jars load without error, but provider registration is skipped after a single console message.
 */
public final class TeamsApiVersion
{
	/** Minimum TeamsAPI {@link TeamsAPI#API_VERSION} required for provider registration. */
	public static final String MINIMUM_API_VERSION = "2.4.0";

	/** Declared to block subclassing and instantiation of this utility holder. */
	private TeamsApiVersion()
	{

	}

	/**
	 * Checks if the runtime TeamsAPI API version is at least the minimum required version.
	 * 
	 * @return {@code true} when {@link TeamsAPI#API_VERSION} at runtime is at least {@link #MINIMUM_API_VERSION}
	 */
	public static boolean isRuntimeSupported()
	{
		return isAtLeast(readRuntimeApiVersion(), MINIMUM_API_VERSION);
	}

	/**
	 * Verifies runtime API version and logs a single {@link Level#SEVERE} message when it is too old.
	 * <p>
	 * Callers should treat {@code false} as "do not register TeamsAPI providers"; the Factions plugin itself
	 * continues to load.
	 *
	 * @param logger Factions plugin logger; may be {@code null} to suppress logging
	 * @return {@code true} when the runtime version is supported
	 */
	public static boolean logAndCheckRuntimeSupported(final Logger logger)
	{
		final String runtime = readRuntimeApiVersion();
		if (isAtLeast(runtime, MINIMUM_API_VERSION))
		{
			return true;
		}
		if (logger != null)
		{
			logger.log(
				Level.SEVERE,
				"TeamsAPI integration requires API version " + MINIMUM_API_VERSION
					+ " or newer (role prefix support). Found " + runtime
					+ ". Factions will not register as a TeamsAPI provider until TeamsAPI is updated."
			);
		}
		return false;
	}

	/**
	 * Reads {@link TeamsAPI#API_VERSION} from the class loader that loaded this utility.
	 * <p>
	 * On servers without TeamsAPI, or when the constant cannot be resolved, returns an empty string so
	 * comparisons fail closed (treated as unsupported).
	 *
	 * @return trimmed API version string, or {@code ""} when unavailable
	 */
	static String readRuntimeApiVersion()
	{
		try
		{
			final String version = TeamsAPI.API_VERSION;
			return version != null ? version.trim() : "";
		}
		catch (final Throwable ignored)
		{
			return "";
		}
	}

	/**
	 * Checks if a version is at least a minimum version.
	 * 
	 * @param version version string to test (may be null or empty)
	 * @param minimum inclusive minimum version
	 * @return {@code true} when {@code version} is greater than or equal to {@code minimum} by MAJOR.MINOR.PATCH
	 */
	static boolean isAtLeast(final String version, final String minimum)
	{
		return compareSemver(version, minimum) >= 0;
	}

	/**
	 * Compares two version strings using MAJOR.MINOR.PATCH numeric ordering.
	 * <p>
	 * Pre-release suffixes (text after the first {@code -}) are ignored so {@code 2.4.0-SNAPSHOT} compares as
	 * {@code 2.4.0}. Missing segments are treated as zero ({@code 2.4} equals {@code 2.4.0}).
	 *
	 * @param left  first version
	 * @param right second version
	 * @return negative if {@code left} is older, positive if newer, zero if equal under the rules above
	 */
	static int compareSemver(final String left, final String right)
	{
		final int[] l = parseSemver(left);
		final int[] r = parseSemver(right);
		for (int i = 0; i < 3; i++)
		{
			final int diff = l[i] - r[i];
			if (diff != 0)
			{
				return diff;
			}
		}
		return 0;
	}

	/**
	 * Parses the leading {@code MAJOR.MINOR.PATCH} triple from a version string.
	 * <p>
	 * Non-numeric trailing characters within a segment (e.g. {@code 1rc1}) stop digit accumulation at the first
	 * non-digit, matching common lenient Maven-style version strings on plugin jars.
	 *
	 * @param version raw version from {@link TeamsAPI#API_VERSION} or similar; may be null
	 * @return int array of length three; never {@code null}
	 */
	private static int[] parseSemver(final String version)
	{
		final int[] parts = new int[] {0, 0, 0};
		if (version == null || version.isEmpty())
		{
			return parts;
		}
		String core = version.trim();
		// 2.4.0-SNAPSHOT -> 2.4.0 for comparison purposes only
		final int dash = core.indexOf('-');
		if (dash >= 0)
		{
			core = core.substring(0, dash);
		}
		final String[] split = core.split("\\.");
		for (int i = 0; i < split.length && i < 3; i++)
		{
			parts[i] = parseSegment(split[i]);
		}
		return parts;
	}

	/**
	 * Parses the leading decimal integer from a single semver segment.
	 *
	 * @param segment one dot-separated component (e.g. {@code "4"} from {@code 2.4.0})
	 * @return parsed value, or {@code 0} for null/empty segments
	 */
	private static int parseSegment(final String segment)
	{
		if (segment == null || segment.isEmpty())
		{
			return 0;
		}
		int value = 0;
		for (int i = 0; i < segment.length(); i++)
		{
			final char c = segment.charAt(i);
			if (c < '0' || c > '9')
			{
				// e.g. "10b" -> 10; "b10" -> 0
				break;
			}
			value = value * 10 + (c - '0');
		}
		return value;
	}
}
