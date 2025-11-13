package com.massivecraft.factionschat.integrations;

import com.massivecraft.factions.integration.placeholderapi.PlaceholderFactions;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.config.Settings;
import com.massivecraft.massivecore.util.PlaceholderProcessor;

import org.bukkit.entity.Player;

/**
 * PlaceholderAPI Expansion for FactionsChat, this extends the 
 * Factions Placeholder Expansion to add the chat-related placeholders
 * to the already supported list of Factions placeholders, thereby 
 * providing player-specific chat mode information.
 */
public class PlaceholderFactionsChat extends PlaceholderFactions
{
    @Override
    public String getVersion()
    {
        return super.getVersion() + " with FactionsChat " + FactionsChat.instance.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String placeholder)
    {
        // Invalid placeholder
        if (placeholder == null) return null;

        // Call the parent method to handle any existing faction placeholders
        String factionsPlaceholder = super.onPlaceholderRequest(player, placeholder);
        if (factionsPlaceholder != null) return factionsPlaceholder;

        // If the player is null, we will return an empty string for chat-specific placeholders
        boolean isNull = player == null;

        // Handle the chat-specific placeholders
        // Use PlaceholderProcessor to handle modifiers like |rp, |lp, etc.
        return PlaceholderProcessor.parsePlaceholderWithModifiers(placeholder, basePlaceholder -> {
            switch (basePlaceholder)
            {
                // The prefix for the chat mode the player is currently using
                case "chat_prefix":
                    return !isNull ? Settings.ChatPrefixes.getPrefix(ChatMode.getChatModeForPlayer(player)) : "";

                // The color for the chat mode the player is currently using
                case "chat_color":
                    return !isNull ? Settings.TextColors.getColor(ChatMode.getChatModeForPlayer(player)) : "";
                
                default:
                    return null; // Unknown placeholder
            }
        });
    }
}
