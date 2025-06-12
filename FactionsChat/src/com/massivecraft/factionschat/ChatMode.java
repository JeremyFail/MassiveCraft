package com.massivecraft.factionschat;

import java.util.HashMap;

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

    public static HashMap<String, ChatMode> getAllChatModes()
    {
        return BY_NAME;
    }

    public static ChatMode getChatModeByName(String value)
    {
        return getAllChatModes().get(value);
    }
}
