package com.massivecraft.factions.integration.placeholderapi;

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
}
