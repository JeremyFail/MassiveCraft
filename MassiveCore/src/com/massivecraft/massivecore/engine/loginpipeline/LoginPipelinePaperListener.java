package com.massivecraft.massivecore.engine.loginpipeline;

import com.massivecraft.massivecore.PlayerState;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDatabase;
import com.massivecraft.massivecore.engine.EngineMassiveCorePlayerState;
import com.massivecraft.massivecore.util.IdUtil;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.profile.PlayerProfile;

import java.util.UUID;

/**
 * Paper login pipeline: replaces deprecated {@code PlayerLoginEvent} handlers.
 * <p>
 * Paper maps former "login LOWEST" work to early {@code PlayerJoinEvent} and the async prelogin /
 * connection-validation phases. All steps in {@link #joinPipelineLowest} run in one handler so store
 * hydration always completes before {@code JOINING} is set (ordering that Spigot got for free via
 * separate login and join events).
 * <p>
 * {@code LOGSYNC} is not used on Paper; players transition {@code LOGASYNC} → {@code JOINING} directly.
 * <p>
 * Only registered when {@link LoginPipeline#supportsModernPaperLogin()} is true (Paper 1.21.7+).
 * Older Paper versions fall back to {@link LoginPipelineSpigotListener}; see {@link LoginPipelineMode#PAPER_LEGACY}.
 */
public class LoginPipelinePaperListener implements Listener
{
	// -------------------------------------------- //
	// DATABASE + SENDER REFERENCES + JOINING
	// -------------------------------------------- //
	// PlayerJoinEvent: LOWEST (replaces PlayerLoginEvent on Paper)
	
	/**
	 * Single LOWEST join handler: hydrate store, wire sender refs, then set {@code JOINING}.
	 * Must stay in this order. Spigot sets {@code JOINING} in {@link LoginPipelineSpigotListener#joining}.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void joinPipelineLowest(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		EngineMassiveCoreDatabase.get().hydrateStoreOnJoin(player);
		EngineMassiveCoreDatabase.setSenderReferences(player, player);
		EngineMassiveCorePlayerState.get().setState(player, PlayerState.JOINING, false, null);
	}
	
	// -------------------------------------------- //
	// ID UTIL
	// -------------------------------------------- //
	// AsyncPlayerPreLoginEvent: LOWEST (earlier than Spigot's PlayerLoginEvent slot)
	
	/**
	 * Updates id/name maps from UUID and name before a {@code Player} instance exists.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void idUtilPreLoginLowest(AsyncPlayerPreLoginEvent event)
	{
		IdUtil.declareExistenceFromPreLogin(event.getUniqueId(), event.getName());
	}
	
	// -------------------------------------------- //
	// VALIDATE LOGIN
	// -------------------------------------------- //
	// PlayerConnectionValidateLoginEvent: MONITOR
	
	/**
	 * Cleans up connection data when Paper denies login after async prelogin allowed the connection.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void validateLoginMonitor(PlayerConnectionValidateLoginEvent event)
	{
		if (event.getKickMessage() == null) return;
		
		UUID id = getConnectionUuid(event);
		if (id == null) return;
		
		EngineMassiveCoreDatabase.clearConnectionDataSoon(id.toString());
		EngineMassiveCorePlayerState.get().setState(id, PlayerState.LEFT, false, PlayerState.LOGASYNC);
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	private static UUID getConnectionUuid(PlayerConnectionValidateLoginEvent event)
	{
		if ( ! (event.getConnection() instanceof PlayerLoginConnection loginConnection)) return null;
		
		PlayerProfile profile = loginConnection.getAuthenticatedProfile();
		if (profile == null) profile = loginConnection.getUnsafeProfile();
		if (profile == null) return null;
		
		return profile.getUniqueId();
	}
	
}
