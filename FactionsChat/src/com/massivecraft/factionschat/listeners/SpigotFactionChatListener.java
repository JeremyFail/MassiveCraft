package com.massivecraft.factionschat.listeners;

import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPlayerColl;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.util.FactionsChatUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Listens for Spigot's AsyncPlayerChatEvent and handles FactionsChat formatting and delivery.
 * Supports per-recipient filtering through manual message sending, PlaceholderAPI, and built-in tag parsing.
 * The chat format is configurable via the config file.
 *
 * This listener is only registered if the server is running Spigot (not Paper).
 */
public class SpigotFactionChatListener implements Listener
{
    /**
     * Handles the AsyncPlayerChatEvent.
     * This method processes the chat message, applies the appropriate chat mode,
     * and formats the message for each recipient.
     * 
     * @param event The AsyncPlayerChatEvent triggered through chat
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        MPlayer mSender = MPlayerColl.get().get(player);
        ChatMode chatMode = FactionsChat.qmPlayers.containsKey(player.getUniqueId()) ?
                FactionsChat.qmPlayers.remove(player.getUniqueId()) :
                FactionsChat.instance.getPlayerChatModes().getOrDefault(player.getUniqueId(), ChatMode.GLOBAL);

        Set<Player> notReceiving = new HashSet<>();
        for (Player recipient : event.getRecipients())
        {
            if (recipient.equals(player)) continue;
            
            // Skip recipients who have social spy enabled in Essentials
            if (FactionsChat.instance.getEssentialsPlugin() != null && FactionsChat.instance.getEssentialsPlugin().getUser(recipient).isSocialSpyEnabled())
            {
                continue;
            }

            MPlayer mRecipient = MPlayerColl.get().get(recipient);
            if (FactionsChatUtil.filterRecipient(chatMode, mSender, mRecipient, player, recipient))
            {
                notReceiving.add(recipient);
            }
        }
        event.getRecipients().removeAll(notReceiving);
        handleChat(player, event.getMessage(), event.getRecipients(), chatMode);

        // Event is cancelled as we handle the chat manually
        event.setCancelled(true);
    }

    /**
     * Handles the chat message formatting and sending to recipients.
     * 
     * @param sender The player sending the message
     * @param message The chat message being sent
     * @param recipients Set of players who should receive the message
     * @param chatMode The chat mode being used (e.g., GLOBAL, FACTION, ALLY, etc.)
     */
    private void handleChat(Player sender, String message, Set<Player> recipients, ChatMode chatMode)
    {
        String format = FactionsChat.instance.getConfig().getString("ChatFormat", "%factions_chat_prefix% &f<%rel_factions_relation_color%%factions_player_rankprefix%%factions_faction_name% &r%DISPLAYNAME%> %factions_chat_color%%MESSAGE%");
        String displayName = sender.getDisplayName();
        String originalMessage = message;

        if (FactionsChat.instance.isPapiEnabled())
        {
            format = PlaceholderAPI.setPlaceholders(sender, format);
        } 
        else
        {
            format = FactionsChatUtil.setPlaceholders(sender, format, chatMode);
        }
        format = format.replace("%DISPLAYNAME%", displayName)
                       .replace("%MESSAGE%", originalMessage);
        for (Player recipient : recipients)
        {
            String formatted = format;
            if (FactionsChat.instance.isPapiEnabled())
            {
                formatted = PlaceholderAPI.setRelationalPlaceholders(sender, recipient, format);
            }
            else
            {
                formatted = FactionsChatUtil.setRelationalPlaceholders(sender, recipient, format);
            }
            recipient.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
        }
    }
}
