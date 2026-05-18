package com.failprooftech.factionschat.integrations.essentials;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Implementation of {@link EssentialsIntegration} that uses the EssentialsX plugin.
 * 
 * <p>
 * This class is used to determine if a player is social-spying on another player.
 * 
 * @see EssentialsIntegration
 * @see EssentialsIntegrationNoop
 */
final class EssentialsIntegrationLive implements EssentialsIntegration
{
	private final Essentials essentials;

	/**
	 * Creates a new EssentialsIntegrationLive instance.
	 * 
	 * @param essentials The Essentials plugin instance.
	 */
	private EssentialsIntegrationLive(final Essentials essentials)
	{
		this.essentials = essentials;
	}

	/**
	 * Tries to create an EssentialsIntegrationLive instance.
	 * 
	 * @param plugin The plugin instance to create the EssentialsIntegrationLive instance from.
	 * @return The EssentialsIntegrationLive instance, or null if the plugin is not an Essentials plugin.
	 */
	static EssentialsIntegration tryCreate(final Plugin plugin)
	{
		if (!(plugin instanceof Essentials))
		{
			return null;
		}
		return new EssentialsIntegrationLive((Essentials) plugin);
	}

	@Override
	public boolean isSocialSpy(final Player recipient)
	{
		if (recipient == null)
		{
			return false;
		}
		final User user = this.essentials.getUser(recipient);
		return user != null && user.isSocialSpyEnabled();
	}
}
