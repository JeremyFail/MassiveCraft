package com.failprooftech.factionschat.integrations.teamsapi;

import com.failprooftech.factionschat.factions.FactionsBridge;
import com.failprooftech.factionschat.factions.TeamsApiFactionsBridge;
import com.skyblockexp.teamsapi.api.TeamsAPI;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Implementation of {@link TeamsIntegration} that uses the Teams API.
 * <p>
 * This class is used to create a bridge between the Teams API and the FactionsChat plugin.
 * 
 * @see TeamsIntegration
 * @see TeamsIntegrationNoop
 */
public final class TeamsIntegrationLive implements TeamsIntegration
{
	@Override
	public Optional<FactionsBridge> createBridge(final Logger logger)
	{
		final Optional<TeamsApiFactionsBridge> bridgeOpt = TeamsApiFactionsBridge.tryCreate();
		if (bridgeOpt.isEmpty())
		{
			return Optional.empty();
		}
		if (!TeamsAPI.isRelationAvailable())
		{
			if (logger != null)
			{
				logger.warning(
						"Teams API is present but no relation provider is registered; FactionsChat needs TeamsRelationService for ally/truce/enemy channels. "
								+ "Falling back to direct Factions integration when available.");
			}
			return Optional.empty();
		}
		return bridgeOpt.map(FactionsBridge.class::cast);
	}
}
