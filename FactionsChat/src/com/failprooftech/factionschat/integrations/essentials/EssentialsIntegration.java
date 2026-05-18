package com.failprooftech.factionschat.integrations.essentials;

import org.bukkit.entity.Player;

/**
 * Optional EssentialsX wiring; default is {@link EssentialsIntegrationNoop}.
 * 
 * <p>
 * This interface provides a contract for integrating with EssentialsX.
 * It is used to determine if a player is social-spying on another player.
 * 
 * @see EssentialsIntegrationLive
 * @see EssentialsIntegrationNoop
 */
public interface EssentialsIntegration
{
	boolean isSocialSpy(Player recipient);
}
