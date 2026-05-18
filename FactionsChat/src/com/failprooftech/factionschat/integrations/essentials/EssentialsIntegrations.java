package com.failprooftech.factionschat.integrations.essentials;

import org.bukkit.plugin.Plugin;

/**
 * Bootstraps the Essentials integration.
 * <p>
 * This class is used to bootstrap the Essentials integration.
 * It is used to determine if a player is social-spying on another player.
 * 
 * @see EssentialsIntegration
 * @see EssentialsIntegrationLive
 * @see EssentialsIntegrationNoop
 */
public final class EssentialsIntegrations
{
	/**
	 * Private constructor to prevent instantiation.
	 */
	private EssentialsIntegrations()
	{

	}

	/**
	 * Bootstraps the Essentials integration.
	 * 
	 * @param essentialsPlugin The Essentials plugin instance.
	 * @return The Essentials integration instance.
	 */
	public static EssentialsIntegration bootstrap(final Plugin essentialsPlugin)
	{
		if (essentialsPlugin == null || !essentialsPlugin.isEnabled())
		{
			return EssentialsIntegrationNoop.INSTANCE;
		}
		try
		{
			return EssentialsIntegrationLive.tryCreate(essentialsPlugin);
		}
		catch (final Throwable ignored)
		{
			return EssentialsIntegrationNoop.INSTANCE;
		}
	}
}
