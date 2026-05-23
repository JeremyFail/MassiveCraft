package com.failprooftech.factionschat.integrations.essentials;

import org.bukkit.entity.Player;

/**
 * Implementation of {@link EssentialsIntegration} that does nothing.
 * <p>
 * Used when EssentialsX is not installed, {@code Essentials.enabled} is false, or activation fails.
 * 
 * @see EssentialsIntegration
 * @see EssentialsIntegrationLive
 */
public enum EssentialsIntegrationNoop implements EssentialsIntegration
{
	INSTANCE;

	@Override
	public boolean isSocialSpy(final Player recipient)
	{
		return false;
	}
}
