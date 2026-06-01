package com.massivecraft.massivecore.engine.loginpipeline;

import com.massivecraft.massivecore.MassivePlugin;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/**
 * Registers platform-specific login pipeline listeners at runtime.
 * <p>
 * Three tiers:
 * <ol>
 *   <li><b>Spigot</b> — {@link LoginPipelineSpigotListener} ({@code PlayerLoginEvent})</li>
 *   <li><b>Paper 1.21.7+</b> — {@link LoginPipelinePaperListener} (validate-login + join pipeline)</li>
 *   <li><b>Paper 1.21.4–1.21.6</b> — soft-support fallback to {@link LoginPipelineSpigotListener}</li>
 * </ol>
 * Paper 1.21.4–1.21.6 is not actively tested; compatibility is best-effort only.
 * <p>
 * Called from {@link com.massivecraft.massivecore.MassiveCore#onEnableInner()} after engines activate,
 * and unregistered from {@link com.massivecraft.massivecore.MassiveCore#onDisable()}.
 */
public class LoginPipeline
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private static Listener platformListener = null;
	private static LoginPipelineMode mode = null;
	private static boolean registered = false;
	
	// -------------------------------------------- //
	// REGISTER
	// -------------------------------------------- //
	
	/**
	 * Registers the appropriate pipeline for the current server. Safe to call once per enable.
	 */
	public static void register(MassivePlugin plugin)
	{
		if (registered) return;
		
		if (supportsModernPaperLogin())
		{
			mode = LoginPipelineMode.PAPER_MODERN;
			platformListener = new LoginPipelinePaperListener();
		}
		else if (MUtil.isPaper())
		{
			mode = LoginPipelineMode.PAPER_LEGACY;
			platformListener = new LoginPipelineSpigotListener();
		}
		else
		{
			mode = LoginPipelineMode.SPIGOT;
			platformListener = new LoginPipelineSpigotListener();
		}
		
		Bukkit.getPluginManager().registerEvents(platformListener, plugin);
		registered = true;
	}
	
	/**
	 * Unregisters the platform listener. Called on plugin disable.
	 */
	public static void unregister()
	{
		if ( ! registered) return;
		
		if (platformListener != null)
		{
			HandlerList.unregisterAll(platformListener);
			platformListener = null;
		}
		
		mode = null;
		registered = false;
	}
	
	/** Whether {@link #register} has been called without a matching {@link #unregister}. */
	public static boolean isRegistered()
	{
		return registered;
	}
	
	/** The pipeline mode chosen at {@link #register}, or {@code null} if not registered. */
	public static LoginPipelineMode getMode()
	{
		return mode;
	}
	
	/**
	 * Whether the modern Paper pipeline ({@link LoginPipelinePaperListener}) is in use.
	 * False for Spigot and Paper 1.21.4–1.21.6 soft-support.
	 */
	public static boolean isModernPaperPipeline()
	{
		return mode == LoginPipelineMode.PAPER_MODERN;
	}
	
	/**
	 * @deprecated Use {@link #getMode()} or {@link #isModernPaperPipeline()}.
	 */
	@Deprecated
	public static boolean isPaperPipeline()
	{
		return isModernPaperPipeline();
	}
	
	/**
	 * Paper 1.21.7+ exposes {@code PlayerConnectionValidateLoginEvent}. Detected at runtime so we never load
	 * {@link LoginPipelinePaperListener} on older Paper builds (that class links against 1.21.7 API types).
	 */
	public static boolean supportsModernPaperLogin()
	{
		if ( ! MUtil.isPaper()) return false;
		
		try
		{
			Class.forName("io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent");
			return true;
		}
		catch (ClassNotFoundException e)
		{
			return false;
		}
	}
	
}
