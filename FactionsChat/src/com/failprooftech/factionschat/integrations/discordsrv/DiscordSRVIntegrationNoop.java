package com.failprooftech.factionschat.integrations.discordsrv;

/**
 * Implementation of {@link DiscordSRVIntegration} that does nothing.
 * <p>
 * Used when DiscordSRV is not installed, {@code DiscordSRV.enabled} is false, or activation fails.
 *
 * @see DiscordSRVIntegration
 * @see DiscordSRVIntegrationLive
 */
public enum DiscordSRVIntegrationNoop implements DiscordSRVIntegration
{
	INSTANCE;

	@Override
	public boolean isActive()
	{
		return false;
	}

	@Override
	public String getStaffChannelBinding()
	{
		return null;
	}

	@Override
	public void setStaffChannelBinding(final String discordChannelId)
	{

	}
}
