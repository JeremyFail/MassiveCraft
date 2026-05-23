package com.failprooftech.factionschat.factions;

import com.failprooftech.factionschat.ChatMode;

import org.bukkit.entity.Player;

/**
 * Abstraction layer over Factions plugin data access.
 *
 * <p>FactionsChat's core logic calls only this interface, making it independent
 * of any specific Factions implementation.  Supply a different implementation
 * to support a different Factions plugin.</p>
 */
public interface FactionsBridge
{
    // --------------------------------------------------------------------- //
    // Faction membership
    // --------------------------------------------------------------------- //

    /** Returns {@code true} if the player is in a faction. */
    boolean isInFaction(Player player);

    /** Returns the player's faction name, or an empty string if not in one. */
    String getFactionName(Player player);

    /**
     * Returns the player's faction name, falling back to a configurable default
     * when the player is not in a faction (e.g. "Wilderness").
     */
    String getFactionNameForce(Player player);

    // --------------------------------------------------------------------- //
    // Rank / title
    // --------------------------------------------------------------------- //

    /** Returns the player's rank within their faction (e.g. "Leader"), or empty. */
    String getPlayerRank(Player player);

    /** Returns the rank prefix for the player's rank, or empty. */
    String getPlayerRankPrefix(Player player);

    /**
     * Returns the player's rank, falling back to a default when not in a
     * faction (e.g. "Member").
     */
    String getPlayerRankForce(Player player);

    /** Returns the rank prefix, with a fallback for factionless players. */
    String getPlayerRankPrefixForce(Player player);

    /** Returns any custom title set for the player within their faction. */
    String getPlayerTitle(Player player);

    // --------------------------------------------------------------------- //
    // Relations
    // --------------------------------------------------------------------- //

    /**
     * Returns the color code string that represents the relationship between
     * {@code sender} and {@code recipient} (e.g. {@code "§a"} for allies).
     */
    String getRelationColor(Player sender, Player recipient);

    /**
     * Returns the relation name between {@code sender} and {@code recipient}
     * (e.g. {@code "ALLY"}).
     */
    String getRelationName(Player sender, Player recipient);

    /**
     * Returns the relation name in lower case (convenience wrapper around
     * {@link #getRelationName}).
     */
    String getRelationNameLowercase(Player sender, Player recipient);

    // --------------------------------------------------------------------- //
    // Chat filtering
    // --------------------------------------------------------------------- //

    /**
     * Decides whether {@code recipient} should be excluded from a message sent
     * by {@code sender} in the given {@link ChatMode}.
     *
     * <p>Implementations should apply any relation-based filtering rules
     * appropriate to the chat mode (faction-only, ally-only, etc.).</p>
     *
     * @return {@code true} if the recipient should NOT receive the message
     */
    boolean shouldExcludeByFactionRelation(ChatMode chatMode, Player sender, Player recipient);

    // --------------------------------------------------------------------- //
    // Default relation colors
    // --------------------------------------------------------------------- //

    String getDefaultAllyColor();
    String getDefaultTruceColor();
    String getDefaultMemberColor();
    String getDefaultEnemyColor();
    String getDefaultNeutralColor();
}
