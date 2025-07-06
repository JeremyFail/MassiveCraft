package com.massivecraft.factionschat.listeners;

import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.util.FactionsChatUtil;
import com.massivecraft.massivecore.util.MUtil;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import io.papermc.paper.event.player.AsyncChatEvent;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listens for Paper's AsyncChatEvent and sets a custom ChatRenderer for FactionsChat.
 * 
 * This allows per-recipient, per-message formatting using PlaceholderAPI or built-in tags,
 * using the format string from the config.
 *
 * This listener is only registered if the server is running Paper.
 */
public class PaperFactionChatListener implements Listener
{
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    /**
     * Handles the AsyncChatEvent.
     * This method processes the chat message, applies the appropriate chat mode,
     * and formats the message for each recipient through the ChatRenderer interface.
     * 
     * @param event The AsyncChatEvent triggered through chat
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event)
    {
        Player sender = event.getPlayer();
        MPlayer mSender = MPlayer.get(sender);
        // Use quick chat mode if present, otherwise persistent
        final ChatMode chatMode = FactionsChat.qmPlayers.containsKey(sender.getUniqueId())
            ? FactionsChat.qmPlayers.remove(sender.getUniqueId())
            : FactionsChat.instance.getPlayerChatModes().getOrDefault(sender.getUniqueId(), ChatMode.GLOBAL);

        // Remove recipients who should not receive the message based on chat mode and permissions
        event.viewers().removeIf(aud -> 
        {
            // Non-player audiences should not be filtered
            if (aud == null || MUtil.isntPlayer(aud))
            {
                return false;
            }

            // Don't filter the sender
            Player recipient = (Player) aud;
            if (recipient.equals(sender))
            {
                return false;
            }

            // Don't filter recipients who have social spy enabled in Essentials
            if (FactionsChat.instance.getEssentialsPlugin() != null && FactionsChat.instance.getEssentialsPlugin().getUser(recipient).isSocialSpyEnabled())
            {
                return false;
            }

            // Validate permissions and chat mode and remove if necessary
            MPlayer mRecipient = MPlayer.get(recipient);
            return FactionsChatUtil.filterRecipient(chatMode, mSender, mRecipient, sender, recipient);
        });

        // Ensure sender is in the viewers set - add if not present
        event.viewers().add((Audience) sender);

        // Use a lambda to capture the chatMode for this event
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            String format = FactionsChat.instance.getConfig().getString("ChatFormat", "%factions_chat_prefix% &f<%rel_factions_relation_color%%factions_player_rankprefix%%factions_faction_name% &r%DISPLAYNAME%> %factions_chat_color%%MESSAGE%");
            String displayName = source.getDisplayName();
            String originalMessage = serializer.serialize(message);
            Player recipient = viewer == null || MUtil.isntPlayer(viewer) ? null : (Player) viewer;
            
            // Replace placeholders based on whether PlaceholderAPI is enabled
            if (FactionsChat.instance.isPapiEnabled())
            {
                format = PlaceholderAPI.setPlaceholders(source, format);
                format = PlaceholderAPI.setRelationalPlaceholders(source, recipient, format);
            } 
            else
            {
                format = FactionsChatUtil.setPlaceholders(source, format, chatMode);
                format = FactionsChatUtil.setRelationalPlaceholders(source, recipient, format);
            }

            // Replace general placeholders and color codes
            format = ChatColor.translateAlternateColorCodes('&', format);
            format = format.replace("%DISPLAYNAME%", displayName)
                           .replace("%MESSAGE%", originalMessage);
            
            return serializer.deserialize(format);
        });
    }
}
