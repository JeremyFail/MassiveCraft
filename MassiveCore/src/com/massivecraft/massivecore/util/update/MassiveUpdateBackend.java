package com.massivecraft.massivecore.util.update;

import com.massivecraft.massivecore.MassiveCore;

/**
 * Strategy for running MassiveCraft suite GitHub update checks.
 * <p>
 * Implementations:
 * <ul>
 *   <li>{@link MassiveUpdateBackendNoop} - bundled checker class not in the JAR; logs once and does nothing.</li>
 *   <li>{@code MassiveUpdateBackendBundled} - lives under {@code update-checker-bundled/java}; uses PluginUpdateChecker.</li>
 * </ul>
 */
public interface MassiveUpdateBackend
{

	/**
	 * Starts or refreshes update checking for this enable cycle (async fetch, console output, optional join notices).
	 *
	 * @param plugin MassiveCore plugin instance
	 */
	void run(MassiveCore plugin);

	/**
	 * Cancels scheduled checker tasks and releases resources; safe to call when the plugin disables.
	 */
	void shutdown();

}
