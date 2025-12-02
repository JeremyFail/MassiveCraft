package com.massivecraft.factionschat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;

/**
 * Enum of chat modes (i.e. channels).
 */
public enum ChatMode 
{
    FACTION("Chat only with your faction members"),
    ALLY("Chat with your faction and allied factions"),
    TRUCE("Chat with your faction, allies, and truced factions"),
    NEUTRAL("Chat with your faction, and neutral factions"),
    ENEMY("Chat with your faction, and enemy factions"),
    LOCAL("Chat with all players within a limited radius"),
    GLOBAL("Chat with all players on the server"),
    WORLD("Chat with all players in your current world"),
    STAFF("Staff Chat for server staff members only");

    private final String description;

    /**
     * Creates a new ChatMode with the specified description.
     * 
     * @param description User-friendly description of what this chat mode does
     */
    ChatMode(String description)
    {
        this.description = description;
    }

    /**
     * Get the description for this chat mode
     * 
     * @return The user-friendly description of this chat mode
     */
    public String getDescription()
    {
        return description;
    }

    // TODO: This works fine for now, until we have chat modes that start with the same letter
    /**
     * Get the single-character alias for this chat mode
     * 
     * @return The first character of the chat mode name in lowercase
     */
    public String getAlias()
    {
        return String.valueOf(name().toLowerCase().charAt(0));
    }

    private static HashMap<String, ChatMode> BY_NAME;
    
    static 
    {
        BY_NAME = new HashMap<>();
        for (ChatMode chatMode : values())
        {
            BY_NAME.put(chatMode.name(), chatMode);
            BY_NAME.put(String.valueOf(chatMode.name().charAt(0)), chatMode);
        }
    }
    
    /**
     * Retrieves a {@link HashMap} of all chat modes (channels), where the key is the 
     * name of the chat mode and the value is the mode associated to that name.
     * 
     * This includes both full names and single-character abbreviations.
     *  
     * @return A {@link HashMap} of all chat modes.
     */
    public static HashMap<String, ChatMode> getAllChatModes()
    {
        return BY_NAME;
    }
    
    /**
     * Retrieves the {@link ChatMode} for the specified name. 
     * 
     * @param value The name of the {@link ChatMode} to retrieve.
     * @return The {@link ChatMode} with the specified name, or <code>null</code>
     * if one was not found.
     */
    public static ChatMode getChatModeByName(String value)
    {
        return getAllChatModes().get(value);
    }

    /**
     * Retrieves the {@link ChatMode} for the specified player.
     * Checks the quick chat modes first, then falls back to the player's stored chat mode.
     * 
     * @param player The player to get the chat mode for.
     * @return The {@link ChatMode} for the player, or the default chat mode if none is set.
     */
    public static ChatMode getChatModeForPlayer(Player player)
    {
        if (player == null) return ChatMode.GLOBAL; // Default to GLOBAL if player is null
        return FactionsChat.qmPlayers.containsKey(player.getUniqueId())
            ? FactionsChat.qmPlayers.get(player.getUniqueId())
            : FactionsChat.instance.getPlayerChatModes().getOrDefault(player.getUniqueId(), ChatMode.GLOBAL);
    }

    /**
     * Get all chat modes that are available to the specified player based on their permissions.
     * 
     * @param player The player to check permissions for.
     * @return A list of ChatModes the player has permission to use.
     */
    public static List<ChatMode> getAvailableChatModes(Player player)
    {
        List<ChatMode> availableModes = new ArrayList<>();
        
        if (player == null)
        {
            return availableModes; // Return empty list if player is null
        }
        
        for (ChatMode mode : values())
        {
            String permission = "factions.chat." + mode.name().toLowerCase();
            if (player.hasPermission(permission))
            {
                availableModes.add(mode);
            }
        }
        
        return availableModes;
    }
}
