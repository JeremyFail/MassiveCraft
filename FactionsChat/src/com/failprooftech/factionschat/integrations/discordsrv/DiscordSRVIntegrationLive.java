package com.failprooftech.factionschat.integrations.discordsrv;

import github.scarsz.discordsrv.DiscordSRV;

/**
 * Implementation of {@link DiscordSRVIntegration} backed by the DiscordSRV plugin.
 * <p>
 * Reads and writes the {@code staff} channel alias via DiscordSRV's channel map.
 *
 * @see DiscordSRVIntegration
 * @see DiscordSRVIntegrationNoop
 */
final class DiscordSRVIntegrationLive implements DiscordSRVIntegration
{
	private final DiscordSRV plugin;

	/**
	 * Creates a new DiscordSRVIntegrationLive instance.
	 * 
	 * @param plugin The DiscordSRV plugin instance.
	 * 
	 * @see DiscordSRVIntegration
	 * @see DiscordSRVIntegrationLive
	 */
	DiscordSRVIntegrationLive(final DiscordSRV plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public boolean isActive()
	{
		return true;
	}

	@Override
	public String getStaffChannelBinding()
	{
		return this.plugin.getChannels().get("staff");
	}

	@Override
	public void setStaffChannelBinding(final String discordChannelId)
	{
		this.plugin.getChannels().put("staff", discordChannelId);
	}
}
