package com.massivecraft.creativegates.gate.mobdetect;

import com.massivecraft.massivecore.MassivePlugin;

/**
 * Platform strategy for detecting when wandering mobs enter gates.
 * <p>
 * Chosen once at runtime: Paper {@code EntityMoveEvent}, or Spigot loaded-chunk scan.
 * Implementations that reference Paper APIs are loaded via {@link Class#forName} so Spigot
 * never links them (same idea as MassiveCore dialog backends).
 * </p>
 */
public interface GateMobDetectBackend
{
	/**
	 * Optional probe: return false when required platform APIs are missing.
	 */
	interface CapabilityProbe
	{
		boolean isAvailable();
	}

	/**
	 * Starts or stops detection for this backend.
	 *
	 * @param plugin Owning plugin (event registration / scheduler).
	 * @param active {@code true} to start, {@code false} to stop.
	 */
	void setActive(MassivePlugin plugin, boolean active);

	/**
	 * Short label for logs (e.g. {@code "Paper"}, {@code "Spigot"}).
	 */
	String getName();
}
