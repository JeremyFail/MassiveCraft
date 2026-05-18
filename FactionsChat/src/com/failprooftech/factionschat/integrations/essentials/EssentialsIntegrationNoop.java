package com.failprooftech.factionschat.integrations.essentials;

import org.bukkit.entity.Player;

/**
 * Implementation of {@link EssentialsIntegration} that does nothing.
 * <p>
 * This class is used when EssentialsX is not installed.
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
