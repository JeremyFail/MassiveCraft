package com.failprooftech.factionschat.integrations.teamsapi;

import com.failprooftech.factionschat.factions.FactionsBridge;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Implementation of {@link TeamsIntegration} that does nothing.
 * <p>
 * This class is used when the Teams API is not installed.
 * 
 * @see TeamsIntegration
 * @see TeamsIntegrationLive
 */
public enum TeamsIntegrationNoop implements TeamsIntegration
{
	INSTANCE;

	@Override
	public Optional<FactionsBridge> createBridge(final Logger logger)
	{
		return Optional.empty();
	}
}
