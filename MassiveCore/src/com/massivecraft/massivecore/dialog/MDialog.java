package com.massivecraft.massivecore.dialog;

import com.massivecraft.massivecore.dialog.backend.ChestGuiMDialogBackend;
import com.massivecraft.massivecore.dialog.backend.MDialogBackend;
import com.massivecraft.massivecore.dialog.backend.PaperMDialogBackend;
import com.massivecraft.massivecore.dialog.backend.SpigotMDialogBackend;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDialog;
import com.massivecraft.massivecore.util.ReflectionUtil;
import org.bukkit.entity.Player;

/**
 * Single entry point for MassiveCore dialogs.
 * <p>
 * Plugins build an {@link MDialogSpec} (usually via {@link #builder()}) and call {@link #open}.
 * This class selects a runtime backend once and caches it:
 * </p>
 * <ol>
 *   <li>Paper Dialog API when Minecraft is 1.21.6+ and Paper dialog classes are present</li>
 *   <li>Spigot / Bungee Dialog when those APIs are present</li>
 *   <li>{@link ChestGuiMDialogBackend} otherwise</li>
 * </ol>
 * Availability is probed with {@link ReflectionUtil#classExists(String)} on platform APIs only;
 * backends are then constructed with {@code new} so Spigot never links Paper types (and vice versa).
 */
public final class MDialog
{
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
	 * Whether Paper or Spigot native Dialog APIs are available (not ChestGui).
	 * <p>
	 * Callers that want a different UX when dialogs are missing (e.g. chat tables)
	 * should check this before {@link #open}. ChestGui remains the default backend
	 * for unconditional {@link #open} calls.
	 * </p>
	 *
	 * @return True if the resolved backend is a native Dialog implementation.
	 */
	public static boolean isNativeDialogAvailable()
	{
		return !(resolveBackend() instanceof ChestGuiMDialogBackend);
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
			if (cachedBackend != null) return cachedBackend;
			
			// Vanilla dialogs exist from 1.21.6; older versions always use ChestGui.
			if (ReflectionUtil.isAtLeastMinecraft(1, 21, 6))
			{
				if (isPaperDialogApiPresent())
				{
					cachedBackend = new PaperMDialogBackend();
					return cachedBackend;
				}
				if (isSpigotDialogApiPresent())
				{
					cachedBackend = new SpigotMDialogBackend();
					return cachedBackend;
				}
			}
			
			cachedBackend = new ChestGuiMDialogBackend();
			return cachedBackend;
		}
	}
	
	/**
	 * Probe only - does not load {@link PaperMDialogBackend}.
	 *
	 * @return True when Paper Dialog API types exist on the classpath.
	 */
	private static boolean isPaperDialogApiPresent()
	{
		return ReflectionUtil.classExists("io.papermc.paper.dialog.Dialog")
			&& ReflectionUtil.classExists("io.papermc.paper.registry.data.dialog.type.DialogType");
	}
	
	/**
	 * Probe only - does not load {@link SpigotMDialogBackend}.
	 *
	 * @return True when Spigot Bungee Dialog + custom-click types exist.
	 */
	private static boolean isSpigotDialogApiPresent()
	{
		return ReflectionUtil.classExists("net.md_5.bungee.api.dialog.MultiActionDialog")
			&& ReflectionUtil.classExists("org.bukkit.event.player.PlayerCustomClickEvent");
	}
}
