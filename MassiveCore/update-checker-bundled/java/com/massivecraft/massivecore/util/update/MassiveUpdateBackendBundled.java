package com.massivecraft.massivecore.util.update;

import com.failprooftech.pluginupdatechecker.PluginUpdateChecker;
import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.entity.MassiveCoreMConf;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * {@link MassiveUpdateBackend} that embeds {@link PluginUpdateChecker} for one GitHub Releases API call,
 * then compares the latest tag against every installed suite plugin version.
 * <p>
 * This class is compiled only when the {@code bundle-update-checker} Maven profile is active; after packaging,
 * library bytecode is relocated under {@code com.massivecraft.massivecore.lib.pluginupdatechecker}.
 * <p>
 * Built-in PluginUpdateChecker player/console notifications are disabled; this class owns console banners and
 * optional join messages so a single check covers the whole monorepo.
 */
public final class MassiveUpdateBackendBundled implements MassiveUpdateBackend, Listener
{

	/** Single checker bound to MassiveCore; drives async HTTP and repeating schedule. */
	private PluginUpdateChecker checker;

	private boolean listenerRegistered;

	/** Latest normalized tag from the last successful fetch; shown in join messages. */
	private volatile String lastRemoteNormalized = "";

	/**
	 * Human-readable lines {@code "PluginName version"} for plugins older than {@link #lastRemoteNormalized};
	 * empty means no join notice.
	 */
	private volatile List<String> lastOutdatedLines = Collections.emptyList();

	/**
	 * Default constructor for reflective instantiation from {@link MassiveUpdateBackends}.
	 */
	public MassiveUpdateBackendBundled()
	{
	}

	@Override
	public void run(MassiveCore plugin)
	{
		if ( ! MassiveCoreMConf.get().updateCheckEnabled) return;

		// Replace any previous checker (e.g. /reload) so we do not stack repeating tasks.
		if (checker != null)
		{
			checker.cancelScheduledChecks();
		}

		String slug = MassiveUpdateVersions.DEFAULT_GITHUB_OWNER_REPO;
		if (slug == null || slug.isEmpty())
		{
			plugin.getLogger().warning("Update check is enabled but repository URL did not yield owner/repo; skipping.");
			return;
		}

		// Library compares MassiveCore's jar version by default; we only use supplier + callbacks for suite logic.
		checker = new PluginUpdateChecker(plugin, slug);
		// We aggregate messaging ourselves (one GitHub response, many plugins).
		checker.setNotifyRequestorsOnCheck(false);
		checker.setNotifyOperatorsOnJoin(false);
		checker.setDownloadUrl(MassiveUpdateVersions.DEFAULT_RELEASES_URL);
		checker.setOnSuccess(this::onFetchSuccess);
		checker.setOnFailure(ex -> plugin.getLogger().log(Level.WARNING, "MassiveCraft update check failed.", ex));

		// Silent initial check (no built-in requestors); suite results handled in onFetchSuccess.
		checker.checkNow(Collections.emptyList());

		long repeatHours = MassiveCoreMConf.get().updateCheckRepeatHours;
		if (repeatHours > 0L)
		{
			// Library schedules silent repeats; first repeat fires after one interval, not immediately.
			checker.scheduleRepeating(repeatHours, TimeUnit.HOURS);
		}

		if ( ! listenerRegistered)
		{
			Bukkit.getPluginManager().registerEvents(this, plugin);
			listenerRegistered = true;
		}
	}

	@Override
	public void shutdown()
	{
		if (checker != null)
		{
			checker.cancelScheduledChecks();
			checker = null;
		}
		listenerRegistered = false;
		lastOutdatedLines = Collections.emptyList();
		lastRemoteNormalized = "";
	}

	/**
	 * Main-thread callback after GitHub returned a tag: compare each installed suite plugin with Maven-style ordering.
	 *
	 * @param requestors unused; PluginUpdateChecker API passes requestors from {@code checkNow}; we use silent checks
	 * @param latestVersionRaw {@code tag_name} from the first release in the API response
	 */
	private void onFetchSuccess(@SuppressWarnings("unused") Collection<CommandSender> requestors, String latestVersionRaw)
	{
		String remote = MassiveUpdateVersions.normalizeReleaseTag(latestVersionRaw);
		lastRemoteNormalized = remote;

		List<String> outdated = new ArrayList<>();
		for (String pluginName : MassiveUpdateVersions.SUITE_PLUGIN_NAMES)
		{
			Plugin pl = Bukkit.getPluginManager().getPlugin(pluginName);
			if (pl == null || ! pl.isEnabled()) continue;

			String local = pl.getDescription().getVersion();
			if (local == null) local = "";
			local = local.trim();
			if (local.isEmpty()) continue;

			if (PluginUpdateChecker.isRemoteVersionNewer(local, remote))
			{
				outdated.add(pluginName + " " + local);
			}
		}

		lastOutdatedLines = outdated.isEmpty()
			? Collections.emptyList()
			: Collections.unmodifiableList(new ArrayList<>(outdated));

		if (outdated.isEmpty()) return;

		MassiveCore core = MassiveCore.get();
		String border = repeat('*', 60);
		// Same path as Integration (MassivePlugin#log -> MixinMessage) so prefix and colors match other MassiveCore lines.
		core.log(Txt.parse("<i>%s", border));
		core.log(Txt.parse("<pink>[MassiveCraft]<i> Newer release <g>%s<i> is available on GitHub.", remote));
		core.log(Txt.parse("<n>The following installed plugins are older than the latest release:"));
		for (String line : outdated)
		{
			core.log(Txt.parse("<n>  - <h>%s", line));
		}
		core.log(Txt.parse("<g>Download:<n> %s", MassiveUpdateVersions.DEFAULT_RELEASES_URL));
		core.log(Txt.parse("<g>Update all plugins to the latest version."));
		core.log(Txt.parse("<i>%s", border));
	}

	/**
	 * Repeats a character a given number of times.
	 * Used for the border in the console output.
	 * 
	 * @param ch The character to repeat.
	 * @param count The number of times to repeat the character.
	 * @return A string with the character repeated the given number of times.
	 */
	private static String repeat(char ch, int count)
	{
		char[] buf = new char[count];
		java.util.Arrays.fill(buf, ch);
		return new String(buf);
	}

	/**
	 * Notifies eligible joining players while an outdated suite state is remembered from the last successful check.
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		if (lastOutdatedLines.isEmpty()) return;

		Player player = event.getPlayer();
		MassiveCoreMConf conf = MassiveCoreMConf.get();
		boolean allowOp = conf.updateCheckNotifyOperatorsOnJoin && player.isOp();
		boolean allowPerm = conf.updateCheckNotifyPermissionHoldersOnJoin
			&& player.hasPermission(MassiveUpdateVersions.NOTIFY_PERMISSION);
		if ( ! allowOp && ! allowPerm) return;

		player.sendMessage(Txt.parse("<pink>[MassiveCraft]<b> Updates available<b>: release <aqua>" 
			+ lastRemoteNormalized + "<b> is newer than one or more installed plugins. See console "
			+ "for details. Go to <g>" + MassiveUpdateVersions.DEFAULT_RELEASES_URL + "<b> to "
			+ "download now."));
	}

}
