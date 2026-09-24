package com.failprooftech.factionschat.integrations.discordsrv;

import github.scarsz.discordsrv.DiscordSRV;

/**
 * Implementation of {@link DiscordSRVIntegration} backed by the DiscordSRV plugin.
 * <p>
 * Reads and writes the {@code staff} channel alias via DiscordSRV's channel map.
 * All calls no-op when DiscordSRV has disabled itself (e.g. missing bot token), so
 * FactionsChat does not touch a closed plugin classloader.
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
		return this.plugin.isEnabled();
	}

	@Override
	public String getStaffChannelBinding()
	{
		if (!this.plugin.isEnabled())
		{
			return null;
		}
		return this.plugin.getChannels().get("staff");
	}

	@Override
	public void setStaffChannelBinding(final String discordChannelId)
	{
		if (!this.plugin.isEnabled())
		{
			return;
		}
		this.plugin.getChannels().put("staff", discordChannelId);
	}

	@Override
	public void processGameChat(final org.bukkit.entity.Player player, final String rawMessage)
	{
		if (player == null || rawMessage == null || !this.plugin.isEnabled())
		{
			return;
		}
		// cancelled=false: we already delivered in-game ourselves; DiscordSRV should still relay.
		this.plugin.processChatMessage(player, rawMessage, null, false);
	}
}
