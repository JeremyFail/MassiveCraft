package com.massivecraft.factions.integration.teamsapi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * TeamsAPI semantic-version gate for MassiveCraft Factions provider integration.
 * <p>
 * Compares the runtime {@code TeamsAPI.API_VERSION} constant (see
 * <a href="https://ez-plugins.github.io/teams-api/api.html#teamsapi-static-facade">TeamsAPI static facade</a>)
 * against {@link #MINIMUM_API_VERSION}. Provider registration is skipped when the installed TeamsAPI plugin is older.
 */
public final class TeamsApiVersion
{
	private static final String TEAMS_API_CLASS_NAME = "com.skyblockexp.teamsapi.api.TeamsAPI";
	private static final String API_VERSION_FIELD_NAME = "API_VERSION";

	/** Minimum {@code TeamsAPI.API_VERSION} required for provider registration. */
	public static final String MINIMUM_API_VERSION = "2.4.0";

	/** Guards the version-mismatch warning so it is emitted at most once per server run. */
	private static volatile boolean versionMismatchWarned;

	/** Declared to block subclassing and instantiation of this utility holder. */
	private TeamsApiVersion()
	{

	}

	/**
	 * Checks whether the installed TeamsAPI plugin meets the minimum API version.
	 *
	 * @return {@code true} when the TeamsAPI plugin's {@code API_VERSION} is at least {@link #MINIMUM_API_VERSION}
	 */
	public static boolean isRuntimeSupported()
	{
		return isAtLeast(readRuntimeApiVersion(), MINIMUM_API_VERSION);
	}

	/**
	 * Verifies runtime API version when the TeamsAPI plugin is loaded.
	 * <p>
	 * Logs at most one warning per server run if TeamsAPI is present and enabled but below {@link #MINIMUM_API_VERSION}.
	 * If TeamsAPI is absent or disabled, returns {@code false} without logging.
	 *
	 * @param logger Factions plugin logger; may be {@code null} to suppress logging
	 * @return {@code true} when the runtime version is supported
	 */
	public static boolean logAndCheckRuntimeSupported(final Logger logger)
	{
		final Plugin teamsApiPlugin = Bukkit.getPluginManager().getPlugin("TeamsAPI");
		if (teamsApiPlugin == null || !teamsApiPlugin.isEnabled())
		{
			return false;
		}
		if (isRuntimeSupported())
		{
			versionMismatchWarned = false;
			return true;
		}
		if (logger != null && !versionMismatchWarned)
		{
			versionMismatchWarned = true;
			logger.warning(
				"TeamsAPI integration requires API version " + MINIMUM_API_VERSION
					+ " or newer. Found " + readRuntimeApiVersion() + ". Factions will not register as "
					+ "a TeamsAPI provider until TeamsAPI is updated."
			);
		}
		return false;
	}

	/**
	 * Reads {@code TeamsAPI.API_VERSION} from the enabled TeamsAPI <em>plugin</em> class loader.
	 * <p>
	 * This is the value documented on the TeamsAPI facade. A static reference to {@code TeamsAPI.API_VERSION} in
	 * Factions source is not reliable: the compiler may inline the compile-time {@code provided} teams-api constant
	 * (e.g. 2.4.0), which can differ from the TeamsAPI plugin jar on the server.
	 *
	 * @return trimmed {@code API_VERSION} from the TeamsAPI plugin, or {@code ""} when TeamsAPI is missing or disabled
	 */
	static String readRuntimeApiVersion()
	{
		final Plugin teamsApiPlugin = Bukkit.getPluginManager().getPlugin("TeamsAPI");
		if (teamsApiPlugin == null || !teamsApiPlugin.isEnabled())
		{
			return "";
		}

		final String fromApi = readApiVersionField(teamsApiPlugin.getClass().getClassLoader());
		if (!fromApi.isEmpty())
		{
			return fromApi;
		}

		return readPluginYamlVersion(teamsApiPlugin);
	}

	/**
	 * Reads {@code TeamsAPI.API_VERSION} from a specific class loader via the facade type name.
	 * <p>
	 * Used to load the constant from the TeamsAPI plugin jar rather than from Factions' compile classpath.
	 *
	 * @param loader class loader to resolve {@value #TEAMS_API_CLASS_NAME} from; {@code null} yields {@code ""}
	 * @return trimmed field value, or {@code ""} when the class or field is unavailable
	 */
	private static String readApiVersionField(final ClassLoader loader)
	{
		if (loader == null)
		{
			return "";
		}
		try
		{
			final Class<?> teamsApiClass = Class.forName(TEAMS_API_CLASS_NAME, false, loader);
			final Field field = teamsApiClass.getField(API_VERSION_FIELD_NAME);
			final Object value = field.get(null);
			return value != null ? value.toString().trim() : "";
		}
		catch (final ReflectiveOperationException ignored)
		{
			return "";
		}
	}

	/**
	 * Fallback when {@code API_VERSION} cannot be read from the TeamsAPI facade class.
	 *
	 * @param teamsApiPlugin enabled TeamsAPI plugin instance
	 * @return {@code plugin.yml} version string, trimmed; never {@code null}
	 */
	private static String readPluginYamlVersion(final Plugin teamsApiPlugin)
	{
		final String version = teamsApiPlugin.getDescription().getVersion();
		return version != null ? version.trim() : "";
	}

	/**
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
	 * @param version raw version string; may be null
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
				break;
			}
			value = value * 10 + (c - '0');
		}
		return value;
	}
}
