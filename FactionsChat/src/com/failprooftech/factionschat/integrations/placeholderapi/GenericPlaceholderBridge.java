package com.failprooftech.factionschat.integrations.placeholderapi;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.factions.FactionsBridge;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
 */
public final class GenericPlaceholderBridge extends PlaceholderExpansion implements PlaceholderBridge
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
    public @NotNull String getIdentifier()
    {
        return "factionschat";
    }

    @Override
    public @NotNull String getAuthor()
    {
        List<String> authors = FactionsChat.instance.getDescription().getAuthors();
        return String.join(", ", authors);
    }

    @Override
    public @NotNull String getVersion()
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

    // -------------------------------------------- //
    // Placeholder resolution
    // -------------------------------------------- //

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params)
    {
        Player player = offlinePlayer != null && offlinePlayer.isOnline()
            ? offlinePlayer.getPlayer()
            : null;

        FactionsBridge bridge = FactionsChat.instance.getFactionsBridge();

        switch (params)
        {
            // Chat-mode placeholders — always resolvable for online players
            case "chat_prefix":
                return player != null
                    ? Settings.ChatPrefixes.getPrefix(ChatMode.getChatModeForPlayer(player))
                    : "";
            case "chat_color":
                return player != null
                    ? Settings.TextColors.getColor(ChatMode.getChatModeForPlayer(player))
                    : "";

            // Faction data placeholders — require a FactionsBridge implementation
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
    }
}
