package com.massivecraft.factions.integration.placeholderapi;

import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Interface for plugins that want to add additional placeholders to the Factions PlaceholderAPI integration.
 * This allows other plugins (like FactionsChat) to extend the Factions placeholder set without
 * creating dependency issues or causing conflicts when PlaceholderAPI re-registers expansions.
 * 
 * <p>
 * Implementations should handle their specific placeholders and return null for unknown ones,
 * allowing the chain to continue to the base Factions placeholders.
 */
public interface PlaceholderExpander
{
	/**
	 * Process a placeholder request for additional placeholders not handled by base Factions.
	 * 
	 * @param player The player for which the placeholder is being requested
	 * @param placeholder The placeholder identifier (without % symbols or prefix)
	 * @return The placeholder value, or null if this expander doesn't handle this placeholder
	 */
	String onPlaceholderRequest(Player player, String placeholder);

	/**
	 * Process a placeholder request when the player may be offline (e.g. books, signs).
	 * Default implementation returns null so base Factions handles it via MPlayer.
	 * Override to support offline for your placeholders (e.g. return "" when offline if not applicable).
	 *
	 * @param player The offline player for which the placeholder is being requested
	 * @param placeholder The placeholder identifier (without % symbols or prefix)
	 * @return The placeholder value, or null if this expander doesn't handle this placeholder
	 */
	default String onPlaceholderRequest(OfflinePlayer player, String placeholder)
	{
		return null;
	}
	
	/**
	 * Process a relational placeholder request for additional placeholders not handled by base Factions.
	 * 
	 * @param player1 The first player in the relation
	 * @param player2 The second player in the relation
	 * @param placeholder The placeholder identifier (without % symbols or prefix)
	 * @return The placeholder value, or null if this expander doesn't handle this placeholder
	 */
	default String onPlaceholderRequest(Player player1, Player player2, String placeholder)
	{
		return null;
	}
	
	/**
	 * Get the version string for this expander.
	 * This will be appended to the base Factions version in PlaceholderAPI's expansion list.
	 * 
	 * @return Version string for this expander
	 */
	String getExpanderVersion();

	/**
	 * Get the author string for this expander.
	 * This will be appended to the base Factions author in PlaceholderAPI's expansion list.
	 * 
	 * If null, the author is assumed to be the same as the base Factions author.
	 * 
	 * @return Author string for this expander
	 */
	default String getAuthor()
	{
		return null;
	}

	/**
	 * Get the placeholders supported by this expander.
	 * 
	 * @return List of placeholders supported by this expander.
	 */
	List<String> getPlaceholders();
}
