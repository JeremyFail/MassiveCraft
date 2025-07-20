package com.massivecraft.factionschat;

import java.util.HashMap;

import org.bukkit.entity.Player;

/**
 * Enum of chat modes (i.e. channels).
 */
public enum ChatMode 
{
    FACTION,
    ALLY,
    TRUCE,
    NEUTRAL,
    ENEMY,
    LOCAL,
    GLOBAL,
    WORLD,
    STAFF;

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
     * name of the chat mode and the value is the one-char version of that name.
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
}
