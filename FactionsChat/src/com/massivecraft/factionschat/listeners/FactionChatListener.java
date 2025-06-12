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

public class FactionChatListener implements Listener 
{
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

