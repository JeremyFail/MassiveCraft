package com.massivecraft.massivecore.dialog;

import com.massivecraft.massivecore.dialog.backend.ChestGuiMDialogBackend;
import com.massivecraft.massivecore.dialog.backend.MDialogBackend;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDialog;
import com.massivecraft.massivecore.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Single entry point for MassiveCore dialogs.
 * <p>
 * Plugins build an {@link MDialogSpec} (usually via {@link #builder()}) and call {@link #open}.
 * This class selects a runtime backend once and caches it:
 * </p>
 * <ol>
 *   <li>Paper Dialog API when Minecraft is 1.21.6+ and Paper classes are present</li>
 *   <li>Spigot / Bungee Dialog when those APIs are present</li>
 *   <li>{@link ChestGuiMDialogBackend} otherwise</li>
 * </ol>
 * Dialog backend classes are loaded with {@link Class#forName} so older servers never link them.
 */
public final class MDialog
{
	/** Fully-qualified Paper backend class name (loaded reflectively). */
	private static final String PAPER_BACKEND = "com.massivecraft.massivecore.dialog.backend.PaperMDialogBackend";
	
	/** Fully-qualified Spigot/Bungee backend class name (loaded reflectively). */
	private static final String SPIGOT_BACKEND = "com.massivecraft.massivecore.dialog.backend.SpigotMDialogBackend";
	
	/** Cached backend chosen for this JVM; never re-probed after first success. */
	private static volatile MDialogBackend cachedBackend;
	
	/**
	 * Prevents instantiation; use static helpers only.
	 */
	private MDialog()
	{
	}
	
	/**
	 * Starts a new fluent dialog builder.
	 *
	 * @return Empty builder.
	 */
	public static MDialogBuilder builder()
	{
		return new MDialogBuilder();
	}
	
	/**
	 * Opens a dialog for the player using the resolved backend.
	 * <p>
	 * Registers an {@link MDialogSession} before showing UI so clicks can be routed.
	 * </p>
	 *
	 * @param player Viewer; ignored if null.
	 * @param spec Dialog definition; ignored if null.
	 */
	public static void open(Player player, MDialogSpec spec)
	{
		if (player == null || spec == null) return;
		// Track session first so backends can complete against EngineMassiveCoreDialog.
		MDialogSession session = new MDialogSession(player.getUniqueId(), spec);
		EngineMassiveCoreDialog.get().put(session);
		resolveBackend().open(player, spec, session);
	}
	
	/**
	 * Builds the builder then opens the resulting spec.
	 *
	 * @param player Viewer.
	 * @param builder Spec builder; ignored if null.
	 */
	public static void open(Player player, MDialogBuilder builder)
	{
		if (builder == null) return;
		open(player, builder.build());
	}
	
	/**
	 * Returns the cached backend, resolving and caching on first call.
	 *
	 * @return Non-null backend (at worst ChestGui).
	 */
	private static MDialogBackend resolveBackend()
	{
		MDialogBackend cached = cachedBackend;
		if (cached != null) return cached;
		
		synchronized (MDialog.class)
		{
			// Double-check after taking the lock.
			if (cachedBackend != null) return cachedBackend;
			
			// Vanilla dialogs exist from 1.21.6; older versions always use ChestGui.
			if (ReflectionUtil.isAtLeastMinecraft(1, 21, 6))
			{
				MDialogBackend paper = tryLoad(PAPER_BACKEND);
				if (paper != null)
				{
					cachedBackend = paper;
					return cachedBackend;
				}
				
				MDialogBackend spigot = tryLoad(SPIGOT_BACKEND);
				if (spigot != null)
				{
					cachedBackend = spigot;
					return cachedBackend;
				}
			}
			
			cachedBackend = new ChestGuiMDialogBackend();
			return cachedBackend;
		}
	}
	
	/**
	 * Instantiates a backend by class name and runs its capability probe when present.
	 *
	 * @param className Fully-qualified backend class.
	 * @return Backend instance, or null if missing / unavailable / wrong type.
	 */
	private static MDialogBackend tryLoad(String className)
	{
		try
		{
			Class<?> clazz = Class.forName(className);
			Object instance = clazz.getDeclaredConstructor().newInstance();
			if (!(instance instanceof MDialogBackend)) return null;
			MDialogBackend backend = (MDialogBackend) instance;
			// Optional probe: Paper/Spigot backends refuse to load without their APIs.
			if (backend instanceof MDialogBackend.CapabilityProbe && !((MDialogBackend.CapabilityProbe) backend).isAvailable())
			{
				return null;
			}
			return backend;
		}
		catch (Throwable t)
		{
			Bukkit.getLogger().log(Level.FINE, "MassiveCore dialog backend unavailable: " + className, t);
			return null;
		}
	}
}
