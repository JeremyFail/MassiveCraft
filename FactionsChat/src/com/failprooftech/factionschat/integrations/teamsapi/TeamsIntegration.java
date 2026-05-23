package com.failprooftech.factionschat.integrations.teamsapi;

import com.failprooftech.factionschat.factions.FactionsBridge;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Contract for integrating with the TeamsAPI.
 * <p>
 * This interface is used to create a bridge between the TeamsAPI and the FactionsChat plugin.
 * 
 * @see TeamsIntegrationLive
 * @see TeamsIntegrationNoop
 */
public interface TeamsIntegration
{
	Optional<FactionsBridge> createBridge(Logger logger);
}
