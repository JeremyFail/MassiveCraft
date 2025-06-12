package com.massivecraft.factionschat;

public class ChatPrefixes 
{
    public static String ALLY = "[ALLY]";
    public static String TRUCE = "[TRUCE]";
    public static String FACTION = "[FACTION]";
    public static String NEUTRAL = "[NEUTRAL]";
    public static String ENEMY = "[ENEMY]";
    public static String LOCAL = "[LOCAL]";
    public static String GLOBAL = "[GLOBAL]";
    public static String STAFF = "[STAFF]";
    public static String WORLD = "[WORLD]";

    public static String getPrefix(ChatMode chatMode) 
    {
        switch (chatMode) 
        {
            case ALLY: return ALLY;
            case TRUCE: return TRUCE;
            case FACTION: return FACTION;
            case LOCAL: return LOCAL;
            case GLOBAL: return GLOBAL;
            case STAFF: return STAFF;
            case ENEMY: return ENEMY;
            case NEUTRAL: return NEUTRAL;
            case WORLD: return WORLD;
            default: return "";
        }
    }
}

