package com.massivecraft.factionschat.integrations;

import com.massivecraft.factions.integration.placeholderapi.PlaceholderExpander;
import com.massivecraft.factions.integration.placeholderapi.PlaceholderFactions;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.config.Settings;
import com.massivecraft.massivecore.util.PlaceholderProcessor;

import org.bukkit.entity.Player;

/**
 * PlaceholderAPI integration for FactionsChat. This implements PlaceholderExpander
 * to inject chat-related placeholders into the base Factions placeholder integration,
 * avoiding conflicts when PlaceholderAPI re-registers expansions.
 * 
 * <p>
 * This expander adds chat mode placeholders to the existing "factions" identifier,
 * keeping all placeholders under one namespace while allowing FactionsChat to extend
 * the functionality without creating plugin dependencies.
 */
public class PlaceholderFactionsChat implements PlaceholderExpander
{
    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //
    
    private static PlaceholderFactionsChat i = new PlaceholderFactionsChat();
    public static PlaceholderFactionsChat get() { return i; }
    
    // -------------------------------------------- //
    // ACTIVATE & DEACTIVATE
    // -------------------------------------------- //
    
    /**
     * Register this expander with the Factions PlaceholderAPI integration.
     * This should be called when FactionsChat enables.
     */
    public void activate()
    {
        PlaceholderFactions.addExpander(this);
    }
    
    /**
     * Unregister this expander from the Factions PlaceholderAPI integration.
     * This should be called when FactionsChat disables.
     */
    public void deactivate()
    {
        PlaceholderFactions.removeExpander(this);
    }
    
    // -------------------------------------------- //
    // OVERRIDE: PlaceholderExpander
    // -------------------------------------------- //
    
    @Override
    public String getExpanderVersion()
    {
        return FactionsChat.instance.getName() + " " + FactionsChat.instance.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String placeholder)
    {
        // Invalid placeholder
        if (placeholder == null) return null;

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
                    return null; // Unknown placeholder - let Factions handle it
            }
        });
    }
}
