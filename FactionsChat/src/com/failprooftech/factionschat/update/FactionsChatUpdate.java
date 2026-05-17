package com.failprooftech.factionschat.update;

import com.failprooftech.factionschat.FactionsChat;
import org.bukkit.Bukkit;

/**
 * Public entry point for FactionsChat standalone update checks against GitHub Releases.
 * <p>
 * Only activated when FactionsChat is running without MassiveCraft Factions + MassiveCore,
 * since in that integrated case MassiveCore's suite checker already covers FactionsChat.
 * <p>
 * Work is delegated to a {@link FactionsChatUpdateBackend} created by {@link FactionsChatUpdateBackends}:
 * either the real implementation (when built with the bundle profile and PluginUpdateChecker shaded) or a no-op.
 * <p>
 * The first run is scheduled with delay {@code 0} ticks so it executes after every plugin has finished enabling.
 */
public final class FactionsChatUpdate
{

	/** Lazily created on first scheduled run; cleared on {@link #shutdown()}. */
	private static FactionsChatUpdateBackend backend;

	private FactionsChatUpdate()
	{
	}

	/**
	 * Schedules the update check on the next server tick.
	 * <p>
	 * Uses delay {@code 0}: runs at the first game tick after startup, when all plugins are already enabled.
	 *
	 * @param plugin FactionsChat instance (owns the scheduler task and listener registration in the bundled backend)
	 */
	public static void scheduleAfterPluginsEnabled(FactionsChat plugin)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
		{
			// One backend for the JVM lifetime of this enable cycle; implementation picked once (bundled vs no-op).
			if (backend == null) backend = FactionsChatUpdateBackends.create();
			backend.run(plugin);
		}, 0L);
	}

	/**
	 * Stops repeating checks and clears the backend reference. Called from {@link FactionsChat#onDisable()}.
	 */
	public static void shutdown()
	{
		if (backend != null) backend.shutdown();
		backend = null;
	}

}
