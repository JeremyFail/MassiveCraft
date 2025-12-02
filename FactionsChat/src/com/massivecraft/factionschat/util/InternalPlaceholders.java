package com.massivecraft.factionschat.util;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.config.Settings;
import com.massivecraft.massivecore.util.PlaceholderProcessor;

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
        MPlayer mPlayer = MPlayer.get(player);
        boolean isInFaction = mPlayer.hasFaction();
        String prefix = Settings.ChatPrefixes.getPrefix(chatMode);
        String factionName = mPlayer.getFactionName();
        String factionNameForce = mPlayer.getFaction().getName();
        String playerRank = isInFaction ? mPlayer.getRank().getName() : "";
        String playerRankPrefix = isInFaction ? mPlayer.getRank().getPrefix() : "";
        String playerRankForce = mPlayer.getRank().getName();
        String playerRankPrefixForce = mPlayer.getRank().getPrefix();
        String playerTitle = mPlayer.getTitle();
        String chatColor = Settings.TextColors.getColor(chatMode);
        
        // Use PlaceholderProcessor to handle modifiers
        return PlaceholderProcessor.processPlaceholders(format, placeholder -> {
            switch (placeholder) {
                case "factions_faction_name": 
                    return factionName;
                case "factions_faction_nameforce": 
                    return factionNameForce;
                case "factions_player_rank": 
                    return playerRank;
                case "factions_player_rankprefix": 
                    return playerRankPrefix;
                case "factions_player_rankforce": 
                    return playerRankForce;
                case "factions_player_rankprefixforce": 
                    return playerRankPrefixForce;
                case "factions_player_title": 
                    return playerTitle;
                case "factions_chat_prefix": 
                    return prefix;
                case "factions_chat_color": 
                    return chatColor;
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

        MPlayer mSender = MPlayer.get(sender);
        MPlayer mRecipient = MPlayer.get(recipient);
        Rel relation = mSender.getRelationTo(mRecipient);
        String relationColor = relation.getColor().toString();
        String relationName = relation.getName();
        
        // Use PlaceholderProcessor to handle modifiers
        return PlaceholderProcessor.processPlaceholders(format, placeholder -> {
            switch (placeholder)
            {
                case "rel_factions_relation_color": 
                    return relationColor;
                case "rel_factions_relation": 
                    return relationName;
                case "rel_factions_relation_lowercase": 
                    return relationName.toLowerCase();
                case "MESSAGE":
                case "DISPLAYNAME":
                    return null; // Don't process these - they're handled by the listeners
                default: 
                    return null; // Let other systems handle unknown placeholders
            }
        });
    }
}
