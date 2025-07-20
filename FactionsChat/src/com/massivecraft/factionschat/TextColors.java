package com.massivecraft.factionschat;

import com.massivecraft.factions.entity.MConf;

import org.bukkit.configuration.ConfigurationSection;

/**
 * The text colors for the various chat channels.
 */
public class TextColors 
{
    public static String ALLY;
    public static String TRUCE;
    public static String FACTION;
    public static String NEUTRAL;
    public static String ENEMY;
    public static String LOCAL;
    public static String GLOBAL;
    public static String STAFF;
    public static String WORLD;

    /**
     * Initializes the text colors for the various chat channels from the
     * specified config settings.
     * 
     * @param cfg The {@link ConfigurationSection} containing the text color configurations.
     */
    public static void initialize(ConfigurationSection cfg) 
    {
        ALLY = cfg.getString("Ally", "<fcolor>").replace("<fcolor>", MConf.get().colorAlly.toString());
        TRUCE = cfg.getString("Truce", "<fcolor>").replace("<fcolor>", MConf.get().colorTruce.toString());
        FACTION = cfg.getString("Faction", "<fcolor>").replace("<fcolor>", MConf.get().colorMember.toString());
        NEUTRAL = cfg.getString("Neutral", "<fcolor>").replace("<fcolor>", MConf.get().colorNeutral.toString());
        ENEMY = cfg.getString("Enemy", "<fcolor>").replace("<fcolor>", MConf.get().colorEnemy.toString());
        LOCAL = cfg.getString("Local", "§r");
        GLOBAL = cfg.getString("Global", "§6");
        STAFF = cfg.getString("Staff", "§4");
        WORLD = cfg.getString("World", "§3");
    }

    /**
     * Retrieves the color code string for the specified {@link ChatMode}.
     * 
     * @param chatMode The {@link ChatMode} to retrieve the color string for.
     * @return The color string for the specified {@link ChatMode}.
     */
    public static String getColor(ChatMode chatMode) 
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
