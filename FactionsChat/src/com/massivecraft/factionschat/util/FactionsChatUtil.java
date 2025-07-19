package com.massivecraft.factionschat.util;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.ChatPrefixes;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.TextColors;
import org.bukkit.entity.Player;

public class FactionsChatUtil 
{
    public static final String DEFAULT_CHAT_FORMAT = "%factions_chat_prefix% &f<%rel_factions_relation_color%%factions_player_rankprefix%%factions_faction_name% &r%DISPLAYNAME%> %factions_chat_color%%MESSAGE%";
    
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

    /**
     * Whether the recipient should be excluded from receiving the message based on the chat mode and permissions.
     * 
     * @param chatMode The chat mode being used (e.g., GLOBAL, FACTION, ALLY, etc.).
     * @param mSender The MPlayer object of the sender.
     * @param mRecipient The MPlayer object of the recipient.
     * @param sender The Player object of the sender.
     * @param recipient The Player object of the recipient.
     * 
     * @return Whether the recipient should be excluded from receiving the message.
     */
    public static boolean filterRecipient(ChatMode chatMode, MPlayer mSender, MPlayer mRecipient, Player sender, Player recipient)
    {
        switch (chatMode)
        {
            case TRUCE:
                return !recipient.hasPermission("factions.chat.truce")
                        || (mSender.getRelationTo(mRecipient) != Rel.TRUCE && mSender.getRelationTo(mRecipient) != Rel.FACTION);
            case ALLY:
                return !recipient.hasPermission("factions.chat.ally")
                        || (mSender.getRelationTo(mRecipient) != Rel.ALLY && mSender.getRelationTo(mRecipient) != Rel.FACTION);
            case FACTION:
                return !recipient.hasPermission("factions.chat.faction") 
                        || mSender.getRelationTo(mRecipient) != Rel.FACTION;
            case ENEMY:
                return !recipient.hasPermission("factions.chat.enemy")
                        || (mSender.getRelationTo(mRecipient) != Rel.ENEMY && mSender.getRelationTo(mRecipient) != Rel.FACTION);
            case NEUTRAL:
                return !recipient.hasPermission("factions.chat.neutral")
                        || (mSender.getRelationTo(mRecipient) != Rel.NEUTRAL && mSender.getRelationTo(mRecipient) != Rel.FACTION);
            case LOCAL:
                return !recipient.hasPermission("factions.chat.local")
                        || sender.getLocation().toVector().subtract(recipient.getLocation().toVector()).length() > FactionsChat.instance.getLocalChatRange();
            case STAFF:
                return !recipient.hasPermission("factions.chat.staff");
            case WORLD:
                return !recipient.hasPermission("factions.chat.world") 
                        || !recipient.getWorld().equals(sender.getWorld());
            default:
                return false;
        }
    }
}
