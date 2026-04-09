package com.massivecraft.factionschat.config;

import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factionschat.ChatMode;

import org.bukkit.ChatColor;
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
    public static final int DEFAULT_CONFIG_VERSION = 3;
    public static final String CONFIG_FILE_NAME = "config.yml";
    public static final String BACKUP_CONFIG_FILE_NAME = "config.yml.bak";
    public static final String DEFAULT_CHAT_FORMAT = "%factions_chat_prefix|rp%&r<%rel_factions_relation_color%%factions_player_rankprefix%%factions_faction_name|rp%&r%DISPLAYNAME%&r> %factions_chat_color%%MESSAGE%";

    // Chat settings
    public static String chatFormat;
    public static boolean allowColorCodes;
    public static boolean allowUrl;
    public static boolean allowUrlUnderline;
    public static int localChatRange;
    /** When true, Paper uses cancelled-chat delivery; when false, Paper uses a custom chat renderer (signed path). Ignored on Spigot. */
    public static boolean disableChatReporting;

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
                ALLY = normalizePrefixOrColorString(config.getString("Ally", "§e[<fcolor>ALLY§e]§r"), MConf.get().colorAlly.toString());
                TRUCE = normalizePrefixOrColorString(config.getString("Truce", "§e[<fcolor>TRUCE§e]§r"), MConf.get().colorTruce.toString());
                FACTION = normalizePrefixOrColorString(config.getString("Faction", "§e[<fcolor>FACTION§e]§r"), MConf.get().colorMember.toString());
                ENEMY = normalizePrefixOrColorString(config.getString("Enemy", "§e[<fcolor>ENEMY§e]§r"), MConf.get().colorEnemy.toString());
                NEUTRAL = normalizePrefixOrColorString(config.getString("Neutral", "§e[<fcolor>NEUTRAL§e]§r"), MConf.get().colorNeutral.toString());
                LOCAL = normalizePrefixOrColorString(config.getString("Local", "§e[§rLOCAL§e]§r"), null);
                GLOBAL = normalizePrefixOrColorString(config.getString("Global", "§e[§6GLOBAL§e]§r"), null);
                STAFF = normalizePrefixOrColorString(config.getString("Staff", "§e[§4STAFF§e]§r"), null);
                WORLD = normalizePrefixOrColorString(config.getString("World", "§e[§3WORLD§e]§r"), null);
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
                ALLY = normalizePrefixOrColorString(config.getString("Ally", "<fcolor>"), MConf.get().colorAlly.toString());
                TRUCE = normalizePrefixOrColorString(config.getString("Truce", "<fcolor>"), MConf.get().colorTruce.toString());
                FACTION = normalizePrefixOrColorString(config.getString("Faction", "<fcolor>"), MConf.get().colorMember.toString());
                NEUTRAL = normalizePrefixOrColorString(config.getString("Neutral", "<fcolor>"), MConf.get().colorNeutral.toString());
                ENEMY = normalizePrefixOrColorString(config.getString("Enemy", "<fcolor>"), MConf.get().colorEnemy.toString());
                LOCAL = normalizePrefixOrColorString(config.getString("Local", "§r"), null);
                GLOBAL = normalizePrefixOrColorString(config.getString("Global", "§6"), null);
                STAFF = normalizePrefixOrColorString(config.getString("Staff", "§4"), null);
                WORLD = normalizePrefixOrColorString(config.getString("World", "§3"), null);
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
        disableChatReporting = config.getBoolean("ChatSettings.DisableChatReporting", false);

        // Initialize nested settings
        ChatPrefixes.initialize(config.getConfigurationSection("ChatPrefixes"));
        TextColors.initialize(config.getConfigurationSection("TextColors"));
    }

    /**
     * Replaces {@code <fcolor>} when {@code fcolorReplacement} is non-null, then translates {@code &} color codes to section signs
     * (same as player chat). MiniMessage tags are left as-is for Paper's unified legacy + MiniMessage parse.
     * 
     * @param raw The raw string to normalize.
     * @param fcolorReplacement The replacement for {@code <fcolor>}.
     * @return The normalized string.
     */
    private static String normalizePrefixOrColorString(String raw, String fcolorReplacement)
    {
        if (raw == null)
        {
            return "";
        }
        String s = raw;
        if (fcolorReplacement != null)
        {
            s = s.replace("<fcolor>", fcolorReplacement);
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
