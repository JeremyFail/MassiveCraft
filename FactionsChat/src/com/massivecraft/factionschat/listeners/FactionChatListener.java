package com.massivecraft.factionschat.listeners;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPlayerColl;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.ChatPrefixes;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.TextColors;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.Set;
import java.util.Map;

/**
 * The main chat listener for the FactionsChat plugin.
 */
public class FactionChatListener implements Listener 
{
    // Players sending quick messages
    public static final Map<UUID, ChatMode> qmPlayers = new HashMap<>();

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) 
    {
        MPlayer mSender = (MPlayer) MPlayerColl.get().get(event.getPlayer());
        ChatMode chatMode = qmPlayers.containsKey(event.getPlayer().getUniqueId()) ?
                qmPlayers.remove(event.getPlayer().getUniqueId()) :
                FactionsChat.instance.getPlayerChatModes().getOrDefault(event.getPlayer().getUniqueId(), ChatMode.GLOBAL);

        Set<Player> notReceiving = new HashSet<>();
        for (Player recipient : event.getRecipients()) 
        {
            if (recipient.equals(event.getPlayer())) continue;

            if (FactionsChat.instance.getEssentialsPlugin() != null && FactionsChat.instance.getEssentialsPlugin().getUser(recipient).isSocialSpyEnabled()) 
            {
                continue;
            }

            MPlayer mRecipient = (MPlayer)MPlayerColl.get().get(recipient);

            if (shouldExcludeRecipient(chatMode, mSender, mRecipient, recipient, event.getPlayer())) 
            {
                notReceiving.add(recipient);
            }
        }

        event.getRecipients().removeAll(notReceiving);
        event.setFormat(ChatPrefixes.getPrefix(chatMode) + event.getFormat());
        event.setMessage(TextColors.getColor(chatMode) + event.getMessage());

        qmPlayers.remove(event.getPlayer().getUniqueId());
    }
    
    /**
     * Checks if the specified recipient should be excluded from receiving a particular chat message.
     * Recipients will be excluded if they don't have permission for a specific chat channel, or if 
     * they don't meet the necessary relationship requirements for the player sending the message.
     * 
     * @param chatMode The {@link ChatMode} of the current message being sent. 
     * @param mSender The {@link MPlayer} representation of the sender of the current message.
     * @param mRecipient The {@link MPlayer} representation of the possible recipient of the current message.
     * @param recipient The {@link Player} representation of the possible recipient of the current message.
     * @param sender The {@link Player} representation of the sender of the current message.
     * @return True or False depending on if the recipient meets the criteria to receive the message 
     * (True if they should <strong>NOT</strong> receive the message). 
     */
    private boolean shouldExcludeRecipient(ChatMode chatMode, MPlayer mSender, MPlayer mRecipient, Player recipient, Player sender) 
    {
        switch (chatMode) 
        {
            case TRUCE:
                return (mSender.getRelationTo(mRecipient) != Rel.TRUCE && mSender.getRelationTo(mRecipient) != Rel.FACTION)
                        || !recipient.hasPermission("factionschat.truce");
            case ALLY:
                return (mSender.getRelationTo(mRecipient) != Rel.ALLY && mSender.getRelationTo(mRecipient) != Rel.FACTION)
                        || !recipient.hasPermission("factionschat.ally");
            case FACTION:
                return mSender.getRelationTo(mRecipient) != Rel.FACTION || !recipient.hasPermission("factionschat.faction");
            case ENEMY:
                return (mSender.getRelationTo(mRecipient) != Rel.ENEMY && mSender.getRelationTo(mRecipient) != Rel.FACTION)
                        || !recipient.hasPermission("factionschat.enemy");
            case NEUTRAL:
                return (mSender.getRelationTo(mRecipient) != Rel.NEUTRAL && mSender.getRelationTo(mRecipient) != Rel.FACTION)
                        || !recipient.hasPermission("factionschat.neutral");
            case LOCAL:
                return sender.getLocation().toVector().subtract(recipient.getLocation().toVector()).length() > FactionsChat.instance.getLocalChatRange()
                        || !recipient.hasPermission("factionschat.local");
            case STAFF:
                return !recipient.hasPermission("factionschat.staff");
            case WORLD:
                return !recipient.getWorld().equals(sender.getWorld()) || !recipient.hasPermission("factionschat.world");
            default:
                return false;
        }
    }
}

