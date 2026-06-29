package com.massivecraft.massivecore.engine.loginpipeline;

import com.massivecraft.massivecore.PlayerState;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDatabase;
import com.massivecraft.massivecore.engine.EngineMassiveCorePlayerState;
import com.massivecraft.massivecore.util.IdUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Spigot login pipeline: handles work that Paper moved off deprecated {@link PlayerLoginEvent}.
 * <p>
 * Event ordering on Spigot (all within the classic login → join window):
 * <ol>
 *   <li>{@code AsyncPlayerPreLoginEvent} — async DB prefetch (in {@code EngineMassiveCoreDatabase})</li>
 *   <li>{@code PlayerLoginEvent LOWEST} — store hydration, sender refs, IP cache, {@code LOGSYNC}, early IdUtil</li>
 *   <li>{@code PlayerJoinEvent LOWEST} — {@code JOINING}</li>
 * </ol>
 * This listener is registered on Spigot and on Paper 1.21.4–1.21.6 (soft-support fallback).
 * It is not registered on Paper 1.21.7+, which uses {@link LoginPipelinePaperListener} instead.
 */
@SuppressWarnings("deprecation")
public class LoginPipelineSpigotListener implements Listener
{
	// -------------------------------------------- //
	// DATABASE + SENDER REFERENCES
	// -------------------------------------------- //
	// PlayerLoginEvent LOWEST and MONITOR
	
	/**
	 * Hydrates in-memory store entries, wires {@code SenderColl} sender references, and caches the
	 * connection IP. Runs at LOWEST so downstream login handlers see synced data and live refs.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void loginLowest(PlayerLoginEvent event)
	{
		Player player = event.getPlayer();
		EngineMassiveCoreDatabase.get().hydrateStoreOnJoin(player);
		EngineMassiveCoreDatabase.setSenderReferences(player, player);
		EngineMassiveCoreDatabase.cacheConnectionAddress(player.getUniqueId().toString(), event.getAddress());
	}
	
	/**
	 * Clears sender refs when login is denied. Uses {@code getResult()} rather than {@code isOnline()}
	 * because some platforms report successfully logged in players as offline for several ticks.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void loginMonitor(PlayerLoginEvent event)
	{
		if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) return;
		EngineMassiveCoreDatabase.setSenderReferencesSoon(event.getPlayer(), null);
	}
	
	// -------------------------------------------- //
	// PLAYER STATE
	// -------------------------------------------- //
	// PlayerLoginEvent: LOGSYNC. PlayerJoinEvent: JOINING.
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void logsync(PlayerLoginEvent event)
	{
		Player player = event.getPlayer();
		EngineMassiveCorePlayerState.get().setState(player, PlayerState.LOGSYNC, false, null);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void logsyncMonitor(PlayerLoginEvent event)
	{
		if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) return;
		
		Player player = event.getPlayer();
		EngineMassiveCorePlayerState.get().setState(player, PlayerState.LEFT, true, PlayerState.LOGSYNC);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void joining(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		EngineMassiveCorePlayerState.get().setState(player, PlayerState.JOINING, false, null);
	}
	
	// -------------------------------------------- //
	// ID UTIL
	// -------------------------------------------- //
	// PlayerLoginEvent: LOWEST
	
	/**
	 * Declares id/name existence as early as possible during login (before {@code PlayerJoinEvent}).
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void idUtilLoginLowest(PlayerLoginEvent event)
	{
		IdUtil.declareExistenceFromLogin(event.getPlayer());
	}
	
}
