package com.failprooftech.factionschat.integrations.teamsapi;

/**
 * This class is used to register the Teams integration.
 * 
 * It is used to hold the active {@link TeamsIntegration}. Another plugin may call {@link #register(TeamsIntegration)} before
 * FactionsChat enables if it supplies a custom bridge factory.
 * 
 * @see TeamsIntegration
 * @see TeamsIntegrationLive
 * @see TeamsIntegrationNoop
 */
public final class TeamsIntegrationRegistry
{
	/**
	 * The active {@link TeamsIntegration}.
	 */
	private static volatile TeamsIntegration integration = TeamsIntegrationNoop.INSTANCE;

	/**
	 * Private constructor to prevent instantiation.
	 */
	private TeamsIntegrationRegistry()
	{

	}

	/**
	 * Registers a new {@link TeamsIntegration}.
	 * 
	 * @param integration The {@link TeamsIntegration} to register.
	 */
	public static void register(final TeamsIntegration integration)
	{
		if (integration != null)
		{
			TeamsIntegrationRegistry.integration = integration;
		}
	}

	/**
	 * Gets the active {@link TeamsIntegration}.
	 * 
	 * @return The active {@link TeamsIntegration}.
	 */
	public static TeamsIntegration get()
	{
		return TeamsIntegrationRegistry.integration;
	}
}
