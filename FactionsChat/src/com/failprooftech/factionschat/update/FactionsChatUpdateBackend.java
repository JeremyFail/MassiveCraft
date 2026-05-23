package com.failprooftech.factionschat.update;

import com.failprooftech.factionschat.FactionsChat;

/**
 * Strategy for running FactionsChat standalone GitHub update checks.
 * <p>
 * Implementations:
 * <ul>
 *   <li>{@link FactionsChatUpdateBackendNoop} - bundled checker class not in the JAR; logs once and does nothing.</li>
 *   <li>{@code FactionsChatUpdateBackendBundled} - lives under {@code update-checker-bundled/java}; uses PluginUpdateChecker.</li>
 * </ul>
 * <p>
 * This check only runs when FactionsChat is NOT integrated with MassiveCraft Factions + MassiveCore,
 * since in that case MassiveCore's own suite update checker handles FactionsChat.
 */
public interface FactionsChatUpdateBackend
{

	/**
	 * Starts or refreshes update checking for this enable cycle (async fetch, console output, optional join notices).
	 *
	 * @param plugin FactionsChat plugin instance
	 */
	void run(FactionsChat plugin);

	/**
	 * Cancels scheduled checker tasks and releases resources; safe to call when the plugin disables.
	 */
	void shutdown();

}
