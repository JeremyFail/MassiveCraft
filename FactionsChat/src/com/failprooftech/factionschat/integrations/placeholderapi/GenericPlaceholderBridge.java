package com.failprooftech.factionschat.integrations.placeholderapi;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.factions.FactionsBridge;
import com.failprooftech.factionschat.util.PlaceholderProcessor;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Generic PlaceholderAPI expansion for FactionsChat when a non-MassiveCraft Factions
 * plugin is installed (or when MassiveCraft's expander API is unavailable).
 *
 * <p>Registers directly with PlaceholderAPI under the {@code factionschat} identifier,
 * so placeholders take the form {@code %factionschat_chat_prefix%} etc.</p>
 *
 * <p>Chat-mode placeholders are always available. Faction data placeholders
 * (rank, name, etc.) are resolved via a {@link FactionsBridge} if one is
 * configured; they return empty strings otherwise.</p>
 *
 * <p>Relational placeholders ({@code %rel_factionschat_relation_name%} and
 * {@code %rel_factionschat_relation_color%}) are also supported when a
 * {@link FactionsBridge} is present.</p>
 */
public final class GenericPlaceholderBridge extends PlaceholderExpansion implements Relational, PlaceholderBridge
{
    // -------------------------------------------- //
    // PlaceholderBridge
    // -------------------------------------------- //

    @Override
    public void activate()
    {
        this.register();
    }

    @Override
    public void deactivate()
    {
        this.unregister();
    }

    // -------------------------------------------- //
    // PlaceholderExpansion metadata
    // -------------------------------------------- //

    @Override
    public String getIdentifier()
    {
        return "factionschat";
    }

    @Override
    public String getAuthor()
    {
        return "Ymerejliaf";
    }

    @Override
    public String getVersion()
    {
        return FactionsChat.instance.getDescription().getVersion();
    }

    /**
     * Keep this expansion registered across {@code /papi reload} so callers
     * do not need to restart the server.
     */
    @Override
    public boolean persist()
    {
        return true;
    }

    /**
     * Get the placeholders supported by the FactionsChat PlaceholderAPI integration.
     * 
     * @return List of placeholders supported by the FactionsChat PlaceholderAPI integration.
     */
    @Override
    public List<String> getPlaceholders()
    {
        return List.of(
            "factionschat_chat_prefix",
            "factionschat_chat_color",
            "factionschat_faction_name",
            "factionschat_faction_nameforce",
            "factionschat_player_rank",
            "factionschat_player_rankprefix",
            "factionschat_player_rankforce",
            "factionschat_player_rankprefixforce",
            "factionschat_player_title",
            "rel_factionschat_relation_name",
            "rel_factionschat_relation_color"
        );
    }

    // -------------------------------------------- //
    // Placeholder resolution
    // -------------------------------------------- //

    /**
     * Resolves relational placeholders ({@code %rel_factionschat_relation_name%}
     * and {@code %rel_factionschat_relation_color%}) between two online players.
     *
     * <p>Both players must be non-null and a {@link FactionsBridge} must be
     * available; otherwise an empty string is returned.</p>
     */
    @Override
    public String onPlaceholderRequest(Player player1, Player player2, String placeholder)
    {
        FactionsBridge bridge = FactionsChat.instance.getFactionsBridge();

        return PlaceholderProcessor.parsePlaceholderWithModifiers(placeholder, key ->
        {
            switch (key)
            {
                case "relation_name":
                case "relation":
                    return player1 != null && player2 != null && bridge != null
                        ? bridge.getRelationName(player1, player2) : "";
                case "relation_lowercase":
                case "relation_lower":
                case "relation_name_lowercase":
                case "relation_name_lower":
                    return player1 != null && player2 != null && bridge != null
                        ? bridge.getRelationName(player1, player2).toLowerCase() : "";
                case "relation_color":
                    return player1 != null && player2 != null && bridge != null
                        ? bridge.getRelationColor(player1, player2) : "";
                default:
                    return null;
            }
        });
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params)
    {
        Player player = offlinePlayer != null && offlinePlayer.isOnline()
            ? offlinePlayer.getPlayer()
            : null;

        FactionsBridge bridge = FactionsChat.instance.getFactionsBridge();

        return PlaceholderProcessor.parsePlaceholderWithModifiers(params, key ->
        {
            switch (key)
            {
                // Chat-mode placeholders - always resolvable for online players
                case "chat_prefix":
                    return player != null
                        ? Settings.ChatPrefixes.getPrefix(ChatMode.getChatModeForPlayer(player))
                        : "";
                case "chat_color":
                    return player != null
                        ? Settings.TextColors.getColor(ChatMode.getChatModeForPlayer(player))
                        : "";

                // Faction data placeholders - require a FactionsBridge implementation
                case "faction_name":
                    return player != null && bridge != null ? bridge.getFactionName(player) : "";
                case "faction_nameforce":
                    return player != null && bridge != null ? bridge.getFactionNameForce(player) : "";
                case "player_rank":
                    return player != null && bridge != null ? bridge.getPlayerRank(player) : "";
                case "player_rankprefix":
                    return player != null && bridge != null ? bridge.getPlayerRankPrefix(player) : "";
                case "player_rankforce":
                    return player != null && bridge != null ? bridge.getPlayerRankForce(player) : "";
                case "player_rankprefixforce":
                    return player != null && bridge != null ? bridge.getPlayerRankPrefixForce(player) : "";
                case "player_title":
                    return player != null && bridge != null ? bridge.getPlayerTitle(player) : "";

                default:
                    return null; // Unknown placeholder - PAPI will leave it as-is
            }
        });
    }
}
