package com.failprooftech.factionschat.integrations.discordsrv;

import com.failprooftech.factionschat.listeners.DiscordSRVPaperListener;
import com.failprooftech.factionschat.listeners.DiscordSRVSpigotListener;

import github.scarsz.discordsrv.DiscordSRV;

import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Bootstraps the DiscordSRV integration.
 * <p>
 * Subscribes FactionsChat's API listeners when DiscordSRV is present, {@code DiscordSRV.enabled} is true, and wiring succeeds;
 * returns {@link DiscordSRVIntegrationLive} in that case. The caller skips bootstrap entirely when the config flag is false.
 * otherwise returns {@link DiscordSRVIntegrationNoop}. The caller (typically {@code FactionsChat}) keeps the instance,
 * like {@link com.failprooftech.factionschat.integrations.essentials.EssentialsIntegrations}.
 *
 * @see DiscordSRVIntegration
 * @see DiscordSRVIntegrationLive
 * @see DiscordSRVIntegrationNoop
 */
public final class DiscordSRVIntegrations
{
	/**
	 * Private constructor to prevent instantiation.
	 */
	private DiscordSRVIntegrations()
	{

	}

	/**
	 * Builds the DiscordSRV integration for this server run.
	 *
	 * @param discordPlugin   Bukkit {@code DiscordSRV} plugin instance, or {@code null}
	 * @param paper           {@code true} to subscribe the Paper listener implementation
	 * @param staffChannelId  Discord channel id from config for the {@code staff} alias
	 * @param logger          plugin logger for startup messages (may be {@code null})
	 * @return live integration when DiscordSRV is enabled and wiring succeeds; otherwise {@link DiscordSRVIntegrationNoop#INSTANCE}
	 */
	public static DiscordSRVIntegration bootstrap(final Plugin discordPlugin, final boolean paper, final String staffChannelId,
			final Logger logger)
	{
		if (discordPlugin == null || !discordPlugin.isEnabled())
		{
			return DiscordSRVIntegrationNoop.INSTANCE;
		}
		try
		{
			final DiscordSRV srv = (DiscordSRV) discordPlugin;
			final DiscordSRVIntegrationLive live = new DiscordSRVIntegrationLive(srv);
			DiscordSRV.api.subscribe(paper ? new DiscordSRVPaperListener() : new DiscordSRVSpigotListener());
			live.setStaffChannelBinding(staffChannelId);
			if (logger != null)
			{
				logger.info("DiscordSRV detected - integration enabled. Registered staff channel: " + staffChannelId);
			}
			return live;
		}
		catch (final Throwable ignored)
		{
			return DiscordSRVIntegrationNoop.INSTANCE;
		}
	}
}
