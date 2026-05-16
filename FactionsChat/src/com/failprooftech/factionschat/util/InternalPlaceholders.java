package com.failprooftech.factionschat.util;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.factions.FactionsBridge;

import org.bukkit.entity.Player;

/**
 * Internal placeholder utility class for FactionsChat.
 * This class is only used if external placeholder plugins are not available.
 */
public class InternalPlaceholders 
{
    /**
     * Parses non-relational (single player) tags in the format string.
     * Supports modifier syntax like %placeholder|rp% for right padding.
     */
    public static String setPlaceholders(Player player, String format, ChatMode chatMode)
    {
        FactionsBridge bridge = FactionsChat.instance.getFactionsBridge();
        
        // Use PlaceholderProcessor to handle modifiers
        return PlaceholderProcessor.processPlaceholders(format, placeholder -> {
            switch (placeholder) {
                case "factions_faction_name": 
                    return bridge != null ? bridge.getFactionName(player) : "";
                case "factions_faction_nameforce": 
                    return bridge != null ? bridge.getFactionNameForce(player) : "";
                case "factions_player_rank": 
                    return bridge != null ? bridge.getPlayerRank(player) : "";
                case "factions_player_rankprefix": 
                    return bridge != null ? bridge.getPlayerRankPrefix(player) : "";
                case "factions_player_rankforce": 
                    return bridge != null ? bridge.getPlayerRankForce(player) : "";
                case "factions_player_rankprefixforce": 
                    return bridge != null ? bridge.getPlayerRankPrefixForce(player) : "";
                case "factions_player_title": 
                    return bridge != null ? bridge.getPlayerTitle(player) : "";
                case "factions_chat_prefix": 
                    return Settings.ChatPrefixes.getPrefix(chatMode);
                case "factions_chat_color": 
                    return Settings.TextColors.getColor(chatMode);
                case "MESSAGE":
                case "DISPLAYNAME":
                    return null; // Don't process these - they're handled by the listeners
                default: 
                    return null; // Let other systems handle unknown placeholders
            }
        });
    }

    /**
     * Parses relational tags in the format string for sender/recipient pairs.
     * Supports modifier syntax like %placeholder|rp% for right padding.
     */
    public static String setRelationalPlaceholders(Player sender, Player recipient, String format)
    {
        // If players are null, simply remove relational placeholders
        if (sender == null || recipient == null)
        {
            return format
                .replace("%rel_factions_relation_color%", "")
                .replace("%rel_factions_relation%", "")
                .replace("%rel_factions_relation_lowercase%", "");
        }

        FactionsBridge bridge = FactionsChat.instance.getFactionsBridge();
        
        // Use PlaceholderProcessor to handle modifiers
        return PlaceholderProcessor.processPlaceholders(format, placeholder -> {
            switch (placeholder)
            {
                case "rel_factions_relation_color": 
                    return bridge != null ? bridge.getRelationColor(sender, recipient) : "";
                case "rel_factions_relation": 
                    return bridge != null ? bridge.getRelationName(sender, recipient) : "";
                case "rel_factions_relation_lowercase": 
                    return bridge != null ? bridge.getRelationName(sender, recipient).toLowerCase() : "";
                case "MESSAGE":
                case "DISPLAYNAME":
                    return null; // Don't process these - they're handled by the listeners
                default: 
                    return null; // Let other systems handle unknown placeholders
            }
        });
    }
}
