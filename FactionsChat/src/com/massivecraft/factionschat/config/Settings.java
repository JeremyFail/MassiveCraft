package com.massivecraft.factionschat.config;

import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factionschat.ChatMode;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Static settings class that caches configuration values in memory
 * for better performance instead of parsing YAML on every access.
 * 
 * This class should never be instantiated directly - it is meant
 * to be used as a static utility for accessing configuration values.
 */
public class Settings 
{
    // Configuration file constants
    public static final int DEFAULT_CONFIG_VERSION = 2;
    public static final String CONFIG_FILE_NAME = "config.yml";
    public static final String BACKUP_CONFIG_FILE_NAME = "config.yml.bak";
    public static final String DEFAULT_CHAT_FORMAT = "%factions_chat_prefix|rp%&r<%rel_factions_relation_color%%factions_player_rankprefix%%factions_faction_name|rp%&r%DISPLAYNAME%&r> %factions_chat_color%%MESSAGE%";

    // Chat settings
    public static String chatFormat;
    public static boolean allowColorCodes;
    public static boolean allowUrl;
    public static boolean allowUrlUnderline;
    public static int localChatRange;

    /**
     * Chat prefixes for each chat mode
     */
    public static class ChatPrefixes 
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
         * Initializes chat prefixes from configuration
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
         * Retrieves the prefix for the specified ChatMode.
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

    /**
     * Text colors for each chat mode
     */
    public static class TextColors 
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
         * Initializes text colors from configuration
         * 
         * @param config The configuration section containing text color settings.
         */
        public static void initialize(ConfigurationSection config) 
        {
            if (config != null)
            {
                ALLY = config.getString("Ally", "<fcolor>").replace("<fcolor>", MConf.get().colorAlly.toString());
                TRUCE = config.getString("Truce", "<fcolor>").replace("<fcolor>", MConf.get().colorTruce.toString());
                FACTION = config.getString("Faction", "<fcolor>").replace("<fcolor>", MConf.get().colorMember.toString());
                NEUTRAL = config.getString("Neutral", "<fcolor>").replace("<fcolor>", MConf.get().colorNeutral.toString());
                ENEMY = config.getString("Enemy", "<fcolor>").replace("<fcolor>", MConf.get().colorEnemy.toString());
                LOCAL = config.getString("Local", "§r");
                GLOBAL = config.getString("Global", "§6");
                STAFF = config.getString("Staff", "§4");
                WORLD = config.getString("World", "§3");
            }
        }

        /**
         * Retrieves the color code string for the specified ChatMode.
         * 
         * @param chatMode The chat mode for which to retrieve the color code.
         * @return The color code string associated with the given chat mode.
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
                default: return "§r";
            }
        }
    }

    /**
     * Load all configuration values from the config file into memory.
     * This should be called during plugin initialization and config reloads.
     * 
     * @param config The FileConfiguration object representing the plugin's config.yml.
     */
    public static void load(FileConfiguration config) 
    {
        // Chat settings
        chatFormat = config.getString("ChatSettings.ChatFormat", DEFAULT_CHAT_FORMAT);
        allowColorCodes = config.getBoolean("ChatSettings.AllowColorCodes", true);
        allowUrl = config.getBoolean("ChatSettings.AllowClickableLinks", true);
        allowUrlUnderline = config.getBoolean("ChatSettings.AllowClickableLinksUnderline", true);
        localChatRange = config.getInt("ChatSettings.LocalChatRange", 1000);

        // Initialize nested settings
        ChatPrefixes.initialize(config.getConfigurationSection("ChatPrefixes"));
        TextColors.initialize(config.getConfigurationSection("TextColors"));
    }
}
