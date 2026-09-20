package com.massivecraft.creativegates.gate.mobdetect;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.massivecore.MassivePlugin;
import org.bukkit.Bukkit;

import java.util.logging.Level;

/**
 * Resolves and activates the mob-gate detection backend (Paper event vs Spigot scan).
 */
public final class GateMobDetect
{
	private static final String PAPER_BACKEND =
		"com.massivecraft.creativegates.gate.mobdetect.PaperGateMobDetectBackend";

	private static volatile GateMobDetectBackend cachedBackend;
	private static boolean active;

	private GateMobDetect() { }

	/**
	 * Starts or stops the resolved detection backend.
	 */
	public static void setActive(boolean active)
	{
		MassivePlugin plugin = CreativeGates.get();
		if (plugin == null) return;

		GateMobDetectBackend backend = resolveBackend();
		if (GateMobDetect.active == active) return;

		backend.setActive(plugin, active);
		GateMobDetect.active = active;
	}

	/**
	 * Returns the cached backend, resolving on first use.
	 */
	public static GateMobDetectBackend resolveBackend()
	{
		GateMobDetectBackend cached = cachedBackend;
		if (cached != null) return cached;

		synchronized (GateMobDetect.class)
		{
			if (cachedBackend != null) return cachedBackend;

			GateMobDetectBackend paper = tryLoad(PAPER_BACKEND);
			if (paper != null)
			{
				cachedBackend = paper;
				return cachedBackend;
			}

			cachedBackend = new SpigotGateMobDetectBackend();
			return cachedBackend;
		}
	}

	private static GateMobDetectBackend tryLoad(String className)
	{
		try
		{
			Class<?> clazz = Class.forName(className);
			Object instance = clazz.getDeclaredConstructor().newInstance();
			if (!(instance instanceof GateMobDetectBackend)) return null;
			GateMobDetectBackend backend = (GateMobDetectBackend) instance;
			if (backend instanceof GateMobDetectBackend.CapabilityProbe
				&& !((GateMobDetectBackend.CapabilityProbe) backend).isAvailable())
			{
				return null;
			}
			return backend;
		}
		catch (Throwable t)
		{
			Bukkit.getLogger().log(Level.FINE, "CreativeGates mob-detect backend unavailable: " + className, t);
			return null;
		}
	}
}
