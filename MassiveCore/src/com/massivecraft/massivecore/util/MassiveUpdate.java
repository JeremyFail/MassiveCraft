package com.massivecraft.massivecore.util;

import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.entity.MassiveCoreMConf;
import com.massivecraft.massivecore.util.update.MassiveUpdateBackend;
import com.massivecraft.massivecore.util.update.MassiveUpdateBackends;
import org.bukkit.Bukkit;

/**
 * Public entry point for MassiveCraft suite update checks against GitHub Releases.
 * <p>
 * {@link MassiveCore} calls this from {@code onEnablePost} and {@code onDisable}. Work is delegated to a
 * {@link MassiveUpdateBackend} created by {@link com.massivecraft.massivecore.util.update.MassiveUpdateBackends}:
 * either the real implementation (when built with the bundle profile and PluginUpdateChecker shaded) or a no-op.
 * <p>
 * The first run is scheduled with delay {@code 0} ticks so it executes after every plugin has finished enabling.
 */
public final class MassiveUpdate
{

	/** Lazily created on first scheduled run; cleared on {@link #shutdown()}. */
	private static MassiveUpdateBackend backend;

	private MassiveUpdate()
	{
	}

	/**
	 * Schedules the suite update check on the next server tick if {@link MassiveCoreMConf#updateCheckEnabled} is true.
	 * <p>
	 * Uses delay {@code 0}: runs at the first game tick after startup, when all plugins are already enabled.
	 *
	 * @param plugin MassiveCore instance (owns the scheduler task and listener registration in the bundled backend)
	 */
	public static void scheduleAfterPluginsEnabled(MassiveCore plugin)
	{
		if ( ! MassiveCoreMConf.get().updateCheckEnabled) return;

		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
		{
			// One backend for the JVM lifetime of this enable cycle; implementation picked once (bundled vs no-op).
			if (backend == null) backend = MassiveUpdateBackends.create();
			backend.run(plugin);
		}, 0L);
	}

	/**
	 * Stops repeating checks and clears the backend reference. Called from {@link MassiveCore#onDisable()}.
	 */
	public static void shutdown()
	{
		if (backend != null) backend.shutdown();
		backend = null;
	}

}
