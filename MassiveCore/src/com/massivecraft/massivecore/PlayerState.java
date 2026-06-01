package com.massivecraft.massivecore;

import com.massivecraft.massivecore.engine.EngineMassiveCorePlayerState;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * This enumeration is used to keep track of where a player currently is within the login --> join --> play --> leave cycle.
 * Bukkit does not provide this information so we forge it as well as we can by listening to LOWEST on a few different events.
 * 
 * When is this information useful?
 * For example you may want to handle events differently depending on whether the player actually is online yet.
 * Say you want to store last teleport position by logging it on the player teleport event.
 * During the login and join phase the server teleports the player around to get the player in position.
 * So for such a "last tp position" system you may want to ignore any teleports other than during player state JOINED.
 * 
 * EngineMassiveCorePlayerState takes care of updating the information.
 * <p>
 * Platform note: {@code LOGSYNC} is only set on Spigot ({@code PlayerLoginEvent}). On Paper the cycle
 * skips directly from {@code LOGASYNC} to {@code JOINING} at early {@code PlayerJoinEvent}.
 */
public enum PlayerState
{
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //
	
	LOGASYNC, // During AsyncPlayerPreLoginEvent
	LOGSYNC, // During PlayerLoginEvent on Spigot (skipped on Paper; LOGASYNC proceeds directly to JOINING)
	JOINING, // During PlayerJoinEvent (LoginPipelineSpigotListener on Spigot; LoginPipelinePaperListener on Paper)
	JOINED, // Regular situation. The player is online and playing.
	LEAVING, // From the start of EventMassiveCorePlayerLeave till the player actually disconnects.
	LEFT, // The player is fully disconnected and offline.
	
	// END OF LIST
	;
	
	// -------------------------------------------- //
	// STATIC
	// -------------------------------------------- //
	
	public static PlayerState get(UUID id)
	{
		if (id == null) throw new NullPointerException("id");
		return EngineMassiveCorePlayerState.get().getState(id);
	}
	
	public static PlayerState get(Player player)
	{
		if (player == null) throw new NullPointerException("player");		
		return EngineMassiveCorePlayerState.get().getState(player);
	}
	
}
