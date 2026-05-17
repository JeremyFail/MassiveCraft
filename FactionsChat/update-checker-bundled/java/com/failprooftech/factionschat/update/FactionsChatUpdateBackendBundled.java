package com.failprooftech.factionschat.update;

import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.util.ChatTxt;
import com.failprooftech.pluginupdatechecker.PluginUpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * {@link FactionsChatUpdateBackend} that embeds {@link PluginUpdateChecker} for a GitHub Releases API call,
 * comparing the latest release tag against the installed FactionsChat version.
 * <p>
 * This class is compiled only when the {@code bundle-update-checker} Maven profile is active; after packaging,
 * library bytecode is relocated under {@code com.failprooftech.factionschat.lib.pluginupdatechecker}.
 * <p>
 * Only active in standalone mode (when FactionsChat is not integrated with MassiveCraft Factions + MassiveCore);
 * in that integrated case MassiveCore's own suite checker already covers FactionsChat.
 * <p>
 * Built-in PluginUpdateChecker player/console notifications are disabled; this class owns all console banners
 * and join messages.
 */
public final class FactionsChatUpdateBackendBundled implements FactionsChatUpdateBackend, Listener
{

	private static final String GITHUB_OWNER_REPO = "JeremyFail/FactionsChat";
	private static final String RELEASES_URL = "https://github.com/JeremyFail/FactionsChat/releases";
	private static final String NOTIFY_PERMISSION = "factionschat.update.notify";
	private static final long REPEAT_HOURS = 24L;

	/** Single checker bound to FactionsChat; drives async HTTP and repeating schedule. */
	private PluginUpdateChecker checker;

	private boolean listenerRegistered;

	/** Latest normalized tag from the last successful fetch; shown in join messages. */
	private volatile String lastRemoteNormalized = "";

	/** Whether an update was found in the last successful fetch. */
	private volatile boolean lastCheckFoundUpdate = false;

	/**
	 * Default constructor for reflective instantiation from {@link FactionsChatUpdateBackends}.
	 */
	public FactionsChatUpdateBackendBundled()
	{
	}

	@Override
	public void run(FactionsChat plugin)
	{
		// Replace any previous checker (e.g. /reload) so we do not stack repeating tasks.
		if (checker != null)
		{
			checker.cancelScheduledChecks();
		}

		// Library compares FactionsChat's jar version by default; we only use callbacks for our own output.
		checker = new PluginUpdateChecker(plugin, GITHUB_OWNER_REPO);
		checker.setNotifyRequestorsOnCheck(false);
		checker.setNotifyOperatorsOnJoin(false);
		checker.setDownloadUrl(RELEASES_URL);
		checker.setOnSuccess(this::onFetchSuccess);
		// Silent failure: if the API is unreachable or returns no releases, treat as up-to-date.
		checker.setOnFailure(ex -> { /* silent */ });

		// Silent initial check; results handled in onFetchSuccess.
		checker.checkNow(Collections.emptyList());

		// Schedule repeating check every 24 hours.
		checker.scheduleRepeating(REPEAT_HOURS, TimeUnit.HOURS);

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
		lastCheckFoundUpdate = false;
		lastRemoteNormalized = "";
	}

	/**
	 * Main-thread callback after GitHub returned a tag: compare FactionsChat's installed version
	 * against the latest release using Maven-style ordering.
	 *
	 * @param requestors unused; PluginUpdateChecker API passes requestors from {@code checkNow}; we use silent checks
	 * @param latestVersionRaw {@code tag_name} from the first release in the API response
	 */
	private void onFetchSuccess(@SuppressWarnings("unused") java.util.Collection<org.bukkit.command.CommandSender> requestors, String latestVersionRaw)
	{
		if (latestVersionRaw == null || latestVersionRaw.trim().isEmpty())
		{
			// No releases found - treat as up-to-date, silent.
			lastCheckFoundUpdate = false;
			lastRemoteNormalized = "";
			return;
		}

		String remote = normalizeReleaseTag(latestVersionRaw);
		lastRemoteNormalized = remote;

		org.bukkit.plugin.Plugin pl = Bukkit.getPluginManager().getPlugin("FactionsChat");
		if (pl == null || ! pl.isEnabled())
		{
			lastCheckFoundUpdate = false;
			return;
		}

		String local = pl.getDescription().getVersion();
		if (local == null) local = "";
		local = local.trim();

		if (local.isEmpty() || ! PluginUpdateChecker.isRemoteVersionNewer(local, remote))
		{
			lastCheckFoundUpdate = false;
			return;
		}

		lastCheckFoundUpdate = true;

		String border = repeat('*', 60);
		FactionsChat plugin = FactionsChat.instance;
		plugin.getLogger().info(ChatTxt.parse("<i>%s", border));
		plugin.getLogger().info(ChatTxt.parse("<pink>[FactionsChat]<i> Newer release <g>%s<i> is available on GitHub.", remote));
		plugin.getLogger().info(ChatTxt.parse("<n>FactionsChat <h>%s<n> is older than the latest release.", local));
		plugin.getLogger().info(ChatTxt.parse("<g>Download:<a> %s", RELEASES_URL));
		plugin.getLogger().info(ChatTxt.parse("<g>Update FactionsChat to the latest version."));
		plugin.getLogger().info(ChatTxt.parse("<i>%s", border));
	}

	/**
	 * Notifies eligible joining players while an outdated state is remembered from the last successful check.
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		if ( ! lastCheckFoundUpdate) return;

		Player player = event.getPlayer();
		boolean allowOp = player.isOp();
		boolean allowPerm = player.hasPermission(NOTIFY_PERMISSION);
		if ( ! allowOp && ! allowPerm) return;

		player.sendMessage(ChatTxt.parse("<pink>[FactionsChat]<b> An update is available<b>: release <aqua>"
			+ lastRemoteNormalized + "<b> is newer than the installed version. Go to <g>"
			+ RELEASES_URL + "<b> to download."));
	}

	/**
	 * Normalizes a Git {@code tag_name} for comparison to {@code plugin.yml} {@code version} strings.
	 * Strips a leading {@code refs/tags/} prefix and a leading {@code v} when followed by a digit
	 * (e.g. {@code v3.4.0} → {@code 3.4.0}).
	 */
	private static String normalizeReleaseTag(String tag)
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

	private static String repeat(char ch, int count)
	{
		char[] buf = new char[count];
		java.util.Arrays.fill(buf, ch);
		return new String(buf);
	}

}
