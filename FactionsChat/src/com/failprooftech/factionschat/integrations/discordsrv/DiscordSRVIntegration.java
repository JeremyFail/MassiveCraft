package com.failprooftech.factionschat.integrations.discordsrv;

/**
 * Contract for integrating with DiscordSRV.
 * <p>
 * Exposes staff-channel bindings and activity state without tying callers to {@code github.scarsz.discordsrv.DiscordSRV}.
 * Default installation uses {@link DiscordSRVIntegrationLive}; servers without DiscordSRV keep {@link DiscordSRVIntegrationNoop}.
 *
 * @see DiscordSRVIntegrationLive
 * @see DiscordSRVIntegrationNoop
 */
public interface DiscordSRVIntegration
{
	/**
	 * @return {@code true} when DiscordSRV is installed and this integration is active
	 */
	boolean isActive();

	/**
	 * Discord channel id registered under DiscordSRV's {@code staff} alias (see {@link #setStaffChannelBinding}).
	 *
	 * @return bound id, or {@code null} if unset
	 */
	String getStaffChannelBinding();

	void setStaffChannelBinding(String discordChannelId);
}
