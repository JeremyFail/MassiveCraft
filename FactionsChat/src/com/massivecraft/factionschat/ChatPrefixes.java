package com.massivecraft.factionschat;

import org.bukkit.configuration.ConfigurationSection;

import com.massivecraft.factions.entity.MConf;

/**
 * The chat prefixes for the various chat channels.
 */
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

    /**
     * Sets up the chat prefixes specified in the config.
     */
    public static void initialize(ConfigurationSection config) 
    {
        if (config != null)
        {
            ALLY = config.getString("Ally", "§e[<fcolor>ALLY§e]§r")
                    .replace("<fcolor>", MConf.get().colorAlly.toString());
            TRUCE = config.getString("Truce", "§e[<fcolor>TRUCE§e]§r")
                    .replace("<fcolor>", MConf.get().colorTruce.toString());
            FACTION = config.getString("Faction", "§e[<fcolor>FACTION§e]§r")
                    .replace("<fcolor>", MConf.get().colorMember.toString());
            ENEMY = config.getString("Enemy", "§e[<fcolor>ENEMY§e]§r")
                    .replace("<fcolor>", MConf.get().colorEnemy.toString());
            NEUTRAL = config.getString("Neutral", "§e[<fcolor>NEUTRAL§e]§r")
                    .replace("<fcolor>", MConf.get().colorNeutral.toString());
            LOCAL = config.getString("Local", "§e[§rLOCAL§e]§r");
            GLOBAL = config.getString("Global", "§e[§6GLOBAL§e]§r");
            STAFF = config.getString("Staff", "§e[§4STAFF§e]§r");
            WORLD = config.getString("World", "§e[§3WORLD§e]§r");
        }
    }
    
    /**
     * Retrieves the prefix for the specified {@link ChatMode}.
     * 
     * @param chatMode The {@link ChatMode} to retrieve the prefix for.
     * @return The prefix for the specified {@link ChatMode}, or an empty
     * string if an invalid {@link ChatMode} was specified.
     */
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

