package com.failprooftech.factionschat.update;

import com.failprooftech.factionschat.FactionsChat;

/**
 * No-op backend when {@link FactionsChatUpdateBackendBundled} was not compiled into the JAR
 * (build without the {@code bundle-update-checker} profile / no PluginUpdateChecker artifact).
 * <p>
 * Avoids failing at runtime; informs the console once that checks are skipped.
 */
public final class FactionsChatUpdateBackendNoop implements FactionsChatUpdateBackend
{

	/** Ensures the informational log is printed at most once per class loader (typical server process). */
	private static boolean skippedMessageLogged;

	@Override
	public void run(FactionsChat plugin)
	{
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
