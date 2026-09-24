package com.failprooftech.factionschat.integrations.discordsrv;

/**
 * Contract for integrating with DiscordSRV.
 * <p>
 * Exposes staff-channel bindings and activity state without tying callers to {@code github.scarsz.discordsrv.DiscordSRV}.
 * Default installation uses {@link DiscordSRVIntegrationLive}; servers without DiscordSRV, or with {@code DiscordSRV.enabled} false, keep {@link DiscordSRVIntegrationNoop}.
 *
 * @see DiscordSRVIntegrationLive
 * @see DiscordSRVIntegrationNoop
 */
public interface DiscordSRVIntegration
{
	/**
	 * @return {@code true} when DiscordSRV is installed, this integration is wired, and DiscordSRV is still enabled
	 *         (false if DiscordSRV later self-disables, e.g. missing bot token)
	 */
	boolean isActive();

	/**
	 * Discord channel id registered under DiscordSRV's {@code staff} alias (see {@link #setStaffChannelBinding}).
	 *
	 * @return bound id, or {@code null} if unset
	 */
	String getStaffChannelBinding();

	void setStaffChannelBinding(String discordChannelId);

	/**
	 * Asks DiscordSRV to process a Minecraft chat line for Discord relay.
	 * <p>
	 * Used when FactionsChat cancels the Bukkit/Paper chat event so DiscordSRV's own listener
	 * would otherwise skip it (Spigot always cancels; Paper cancels when {@code DisableChatReporting}
	 * is true). Fires DiscordSRV's {@code GameChatMessagePreProcessEvent}, where FactionsChat routes
	 * global / staff / other channels.
	 *
	 * @param player     the speaking player
	 * @param rawMessage the original chat line (may include colon quick-chat prefixes)
	 */
	void processGameChat(org.bukkit.entity.Player player, String rawMessage);
}
