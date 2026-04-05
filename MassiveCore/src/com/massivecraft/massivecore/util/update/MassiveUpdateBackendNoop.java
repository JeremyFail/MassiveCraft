package com.massivecraft.massivecore.util.update;

import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.entity.MassiveCoreMConf;

/**
 * No-op backend when {@link MassiveUpdateBackendBundled} was not compiled into the JAR
 * (build without the {@code bundle-update-checker} profile / no PluginUpdateChecker artifact).
 * <p>
 * Avoids failing at runtime; informs the console once that checks are skipped.
 */
public final class MassiveUpdateBackendNoop implements MassiveUpdateBackend
{

	/** Ensures the informational log is printed at most once per class loader (typical server process). */
	private static boolean skippedMessageLogged;

	@Override
	public void run(MassiveCore plugin)
	{
		if ( ! MassiveCoreMConf.get().updateCheckEnabled) return;

		if ( ! skippedMessageLogged)
		{
			skippedMessageLogged = true;
			plugin.getLogger().info("Update checker not bundled (build without -Pbundle-update-checker); skipping GitHub update checks.");
		}
	}

	@Override
	public void shutdown()
	{
		// No tasks or listeners registered.
	}

}
