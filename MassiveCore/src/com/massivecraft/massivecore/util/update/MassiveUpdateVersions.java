package com.massivecraft.massivecore.util.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Build-time and runtime constants for the MassiveCraft suite update checker, plus Git tag normalization.
 * <p>
 * The public releases URL and repository web URL are supplied by Maven-filtered {@code massivecraft.properties}
 * on the classpath (from MassiveSuper {@code massiveReleasesUrl} and {@code massiveRepositoryUrl}), with
 * hard-coded fallbacks if the file is missing or filtering did not run. The GitHub API {@code owner/repo} slug
 * is derived from {@code repository.url} so it stays in sync with the POM without a persisted config field.
 */
public final class MassiveUpdateVersions
{

	/**
	 * Used when {@code massivecraft.properties} is absent or still contains an unexpanded Maven placeholder.
	 */
	private static final String FALLBACK_RELEASES_URL = "https://github.com/JeremyFail/MassiveCraft/releases";

	private static final String FALLBACK_GITHUB_OWNER_REPO = "JeremyFail/MassiveCraft";

	private static final Properties BUILD_PROPS = loadBuildProps();

	/**
	 * Human-facing downloads / releases page (console and in-game messages, PluginUpdateChecker download hint).
	 */
	public static final String DEFAULT_RELEASES_URL = readUrl("releases.url", FALLBACK_RELEASES_URL);

	/**
	 * GitHub repository slug {@code owner/repo} for the Releases API (from filtered {@code repository.url}).
	 */
	public static final String DEFAULT_GITHUB_OWNER_REPO = readGitHubOwnerRepo();

	/**
	 * Non-op players with this permission may receive join-time update notices when configured in
	 * {@link com.massivecraft.massivecore.entity.MassiveCoreMConf}.
	 */
	public static final String NOTIFY_PERMISSION = "massivecore.update.notify";

	/**
	 * Bukkit plugin names (as in each {@code plugin.yml}) checked against the latest GitHub release tag.
	 */
	public static final String[] SUITE_PLUGIN_NAMES = {
		"MassiveCore",
		"Factions",
		"FactionsChat",
		"CreativeGates",
		"MassiveBooks",
		"MassiveHat"
	};

	/** Prevents instantiation. */
	private MassiveUpdateVersions()
	{
	}

	/**
	 * Loads {@code massivecraft.properties} from the classpath, or returns an empty {@link Properties} if missing
	 * or unreadable (callers use {@link #readUrl} fallbacks).
	 */
	private static Properties loadBuildProps()
	{
		Properties p = new Properties();
		try (InputStream in = MassiveUpdateVersions.class.getClassLoader().getResourceAsStream("massivecraft.properties"))
		{
			if (in != null) p.load(in);
		}
		catch (IOException ignored)
		{
			// readUrl / readGitHubOwnerRepo use fallbacks
		}
		return p;
	}

	/**
	 * Reads a string property; treats null, blank, or unexpanded Maven placeholders ({@code ${...}}) as unusable.
	 *
	 * @param key property key in {@code massivecraft.properties}
	 * @param fallback value when the property is missing or unusable
	 * @return trimmed property value or {@code fallback}
	 */
	private static String readUrl(String key, String fallback)
	{
		String u = BUILD_PROPS.getProperty(key);
		if (u == null) return fallback;

		u = u.trim();
		if (u.isEmpty() || u.contains("${")) return fallback;
		
		return u;
	}

	/**
	 * Resolves the GitHub API slug from {@code repository.url}, or {@link #FALLBACK_GITHUB_OWNER_REPO} when
	 * the URL is empty or does not parse.
	 */
	private static String readGitHubOwnerRepo()
	{
		String repoUrl = readUrl("repository.url", "");
		if (repoUrl.isEmpty()) return FALLBACK_GITHUB_OWNER_REPO;
		String parsed = ownerRepoFromGitHubWebUrl(repoUrl);
		return (parsed != null && ! parsed.isEmpty()) ? parsed : FALLBACK_GITHUB_OWNER_REPO;
	}

	/**
	 * Parses {@code https://github.com/owner/repo} or {@code .../repo.git} into {@code owner/repo}.
	 * <p>
	 * Package-private for unit tests; production code uses {@link #DEFAULT_GITHUB_OWNER_REPO}.
	 *
	 * @param url repository web URL; may be {@code null}
	 * @return slug, or {@code null} if the URL is not a usable {@code github.com} path
	 */
	static String ownerRepoFromGitHubWebUrl(String url)
	{
		if (url == null) return null;

		String normalized = url.trim();
		if (normalized.isEmpty()) return null;

		// Strip trailing slashes and optional .git (clone URLs).
		while (normalized.endsWith("/"))
		{
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		if (normalized.endsWith(".git"))
		{
			normalized = normalized.substring(0, normalized.length() - ".git".length());
		}

		// Everything after "github.com/" must start with owner/repo; ignore extra path segments.
		final String githubHostPath = "github.com/";
		int hostStart = normalized.indexOf(githubHostPath);
		if (hostStart < 0) return null;

		String afterHost = normalized.substring(hostStart + githubHostPath.length());

		// Query and fragment are not part of owner or repo names.
		int query = afterHost.indexOf('?');
		if (query >= 0) afterHost = afterHost.substring(0, query);
		int fragment = afterHost.indexOf('#');
		if (fragment >= 0) afterHost = afterHost.substring(0, fragment);

		String[] segments = afterHost.split("/");
		if (segments.length < 2) return null;

		String owner = segments[0];
		String repo = segments[1];
		if (owner.isEmpty() || repo.isEmpty()) return null;

		return owner + "/" + repo;
	}

	/**
	 * Normalizes a Git {@code tag_name} for comparison to {@code plugin.yml} {@code version} strings.
	 * <p>
	 * Strips a leading {@code refs/tags/} prefix and a leading {@code v} when followed by a digit
	 * (e.g. {@code v3.4.0} -> {@code 3.4.0}).
	 *
	 * @param tag raw tag from the GitHub API or similar; may be {@code null}
	 * @return normalized string, or empty if {@code tag} is null/blank after trim
	 */
	public static String normalizeReleaseTag(String tag)
	{
		if (tag == null) return "";
		String t = tag.trim();
		if (t.startsWith("refs/tags/"))
		{
			t = t.substring("refs/tags/".length()).trim();
		}
		if (t.length() > 1 && t.charAt(0) == 'v' && Character.isDigit(t.charAt(1)))
		{
			return t.substring(1).trim();
		}
		return t;
	}

}
