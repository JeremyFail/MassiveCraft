package com.massivecraft.factionschat.util;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.ChatPrefixes;
import com.massivecraft.factionschat.TextColors;
import org.bukkit.entity.Player;

/**
 * Internal placeholder utility class for FactionsChat.
 * This class is only used if external placeholder plugins are not available.
 */
public class InternalPlaceholders 
{
    /**
     * Parses non-relational (single player) tags in the format string.
     */
    public static String setPlaceholders(Player player, String format, ChatMode chatMode) {
        MPlayer mPlayer = MPlayer.get(player);
        boolean isInFaction = mPlayer.hasFaction();
        String prefix = ChatPrefixes.getPrefix(chatMode);
        String factionName = mPlayer.getFactionName();
        String factionNameForce = mPlayer.getFaction().getName();
        String playerRank = isInFaction ? mPlayer.getRank().getName() : "";
        String playerRankPrefix = isInFaction ? mPlayer.getRank().getPrefix() : "";
        String playerRankForce = mPlayer.getRank().getName();
        String playerRankPrefixForce = mPlayer.getRank().getPrefix();
        String playerTitle = mPlayer.getTitle();
        String chatColor = TextColors.getColor(chatMode);
        
        return format
                .replace("%factions_faction_name%", factionName)
                .replace("%factions_faction_nameforce%", factionNameForce)
                .replace("%factions_player_rank%", playerRank)
                .replace("%factions_player_rankprefix%", playerRankPrefix)
                .replace("%factions_player_rankforce%", playerRankForce)
                .replace("%factions_player_rankprefixforce%", playerRankPrefixForce)
                .replace("%factions_player_title%", playerTitle)
                .replace("%factions_chat_prefix%", prefix)
                .replace("%factions_chat_color%", chatColor);
    }

    /**
     * Parses relational tags in the format string for sender/recipient pairs.
     */
    public static String setRelationalPlaceholders(Player sender, Player recipient, String format) {
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
        
        return format
                .replace("%rel_factions_relation_color%", relationColor)
                .replace("%rel_factions_relation%", relationName)
                .replace("%rel_factions_relation_lowercase%", relationName.toLowerCase());
    }
}
