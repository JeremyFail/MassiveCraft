package com.failprooftech.factionschat.config;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.factions.FactionsBridge;
import com.failprooftech.factionschat.util.ChatTxt;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public static final int DEFAULT_CONFIG_VERSION = 4;
    public static final String CONFIG_FILE_NAME = "config.yml";
    public static final String BACKUP_CONFIG_FILE_NAME = "config.yml.bak";
    public static final String DEFAULT_CHAT_FORMAT = "%factions_chat_prefix|rp%&r<%rel_factions_relation_color%%factions_player_rankprefix%%factions_faction_name|rp%&r%DISPLAYNAME%&r> %factions_chat_color%%MESSAGE%";

    /**
     * Default entries when {@code ChatSettings.BlacklistedMiniMessageCommands} is absent from config.
     * Matching uses the first token of the click payload (after quotes), ignores leading slashes, and treats
     * {@code minecraft:op} the same as {@code op} when the list contains {@code /op}.
     */
    public static final List<String> DEFAULT_BLACKLISTED_MINIMESSAGE_COMMANDS = Collections.unmodifiableList(Arrays.asList(
        "/minecraft:op",
        "/minecraft:deop",
        "/minecraft:ban",
        "/minecraft:ban-ip",
        "/minecraft:pardon",
        "/minecraft:pardon-ip",
        "/minecraft:kick",
        "/minecraft:mute",
        "/minecraft:unmute",
        "/minecraft:whitelist",
        "/minecraft:gamemode",
        "/minecraft:gm",
        "/minecraft:give",
        "/minecraft:clear",
        "/minecraft:effect",
        "/minecraft:stop",
        "/minecraft:reload",
        "/minecraft:rl",
        "/op",
        "/deop",
        "/ban",
        "/ban-ip",
        "/pardon",
        "/pardon-ip",
        "/kick",
        "/mute",
        "/unmute",
        "/whitelist",
        "/gamemode",
        "/gm",
        "/give",
        "/clear",
        "/effect",
        "/stop",
        "/restart",
        "/reload",
        "/rl",
        "/sudo",
        "/lp",
        "/luckperms",
        "/pex",
        "/permissions"
    ));

    public static final String MINIMESSAGE_CLICK_BLACKLIST_DENY_MESSAGE =
        ChatTxt.parse("<b>That command is not allowed in chat.");

    // Chat settings
    public static String chatFormat;
    public static boolean allowColorCodes;
    public static boolean allowUrl;
    public static boolean allowUrlUnderline;
    public static int localChatRange;
    /** When true, Paper uses cancelled-chat delivery; when false, Paper uses a custom chat renderer (signed path). Ignored on Spigot. */
    public static boolean disableChatReporting;

    /**
     * Paper only: keep Adventure components from upstream plugins while routing on plain text; parse markup in text leaves.
     * When false, the chat body is rebuilt only from plain text (legacy behavior).
     */
    public static boolean preserveUpstreamChatComponents;

    /**
     * Leading prefix + channel token for one-off or toggle messages (configured under {@code ChatSettings.QuickChat}).
     */
    public static class QuickChat
    {
        /** Plain-text prefix that starts a quick channel token (default {@code :}). Must be non-empty ASCII-ish; no whitespace. */
        public static String prefix = ":";
        /**
         * When true, unknown/disallowed quick-chat modes show {@code Invalid chat mode or command.} and cancel the line.
         * When false, those lines are sent as normal chat on the player's current channel (full text preserved).
         */
        public static boolean errorOnInvalidMode;

        /**
         * @param chatSettings {@code ChatSettings} section from config.yml
         */
        public static void initialize(ConfigurationSection chatSettings)
        {
            ConfigurationSection section = chatSettings == null
                ? null
                : chatSettings.getConfigurationSection("QuickChat");
            prefix = normalizeQuickChatPrefix(section == null ? null : section.getString("Prefix"));
            errorOnInvalidMode = section != null && section.getBoolean("ErrorOnInvalidMode", false);
        }
    }

    /**
     * First-token command roots to block inside MiniMessage {@code <click:run_command:…>} / {@code suggest_command} tags.
     * When the config key is missing, {@link #DEFAULT_BLACKLISTED_MINIMESSAGE_COMMANDS} is used.
     */
    public static List<String> blacklistedMiniMessageCommands = new ArrayList<>(DEFAULT_BLACKLISTED_MINIMESSAGE_COMMANDS);

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
        public static void initialize(ConfigurationSection config, FactionsBridge bridge) 
        {
            String allyColor    = bridge != null ? bridge.getDefaultAllyColor()    : "§b";
            String truceColor   = bridge != null ? bridge.getDefaultTruceColor()   : "§3";
            String memberColor  = bridge != null ? bridge.getDefaultMemberColor()  : "§a";
            String enemyColor   = bridge != null ? bridge.getDefaultEnemyColor()   : "§c";
            String neutralColor = bridge != null ? bridge.getDefaultNeutralColor() : "§f";
            if (config != null)
            {
                ALLY    = normalizePrefixOrColorString(config.getString("Ally",     "§e[<fcolor>ALLY§e]§r"),    allyColor);
                TRUCE   = normalizePrefixOrColorString(config.getString("Truce",    "§e[<fcolor>TRUCE§e]§r"),   truceColor);
                FACTION = normalizePrefixOrColorString(config.getString("Faction",  "§e[<fcolor>FACTION§e]§r"), memberColor);
                ENEMY   = normalizePrefixOrColorString(config.getString("Enemy",    "§e[<fcolor>ENEMY§e]§r"),   enemyColor);
                NEUTRAL = normalizePrefixOrColorString(config.getString("Neutral",  "§e[<fcolor>NEUTRAL§e]§r"), neutralColor);
                LOCAL   = normalizePrefixOrColorString(config.getString("Local",    "§e[§rLOCAL§e]§r"),          null);
                GLOBAL  = normalizePrefixOrColorString(config.getString("Global",   "§e[§6GLOBAL§e]§r"),         null);
                STAFF   = normalizePrefixOrColorString(config.getString("Staff",    "§e[§4STAFF§e]§r"),          null);
                WORLD   = normalizePrefixOrColorString(config.getString("World",    "§e[§3WORLD§e]§r"),          null);
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
        public static void initialize(ConfigurationSection config, FactionsBridge bridge) 
        {
            String allyColor    = bridge != null ? bridge.getDefaultAllyColor()    : "§b";
            String truceColor   = bridge != null ? bridge.getDefaultTruceColor()   : "§3";
            String memberColor  = bridge != null ? bridge.getDefaultMemberColor()  : "§a";
            String neutralColor = bridge != null ? bridge.getDefaultNeutralColor() : "§f";
            String enemyColor   = bridge != null ? bridge.getDefaultEnemyColor()   : "§c";
            if (config != null)
            {
                ALLY    = normalizePrefixOrColorString(config.getString("Ally",    "<fcolor>"), allyColor);
                TRUCE   = normalizePrefixOrColorString(config.getString("Truce",   "<fcolor>"), truceColor);
                FACTION = normalizePrefixOrColorString(config.getString("Faction", "<fcolor>"), memberColor);
                NEUTRAL = normalizePrefixOrColorString(config.getString("Neutral", "<fcolor>"), neutralColor);
                ENEMY   = normalizePrefixOrColorString(config.getString("Enemy",   "<fcolor>"), enemyColor);
                LOCAL   = normalizePrefixOrColorString(config.getString("Local",   "§r"), null);
                GLOBAL  = normalizePrefixOrColorString(config.getString("Global",  "§6"), null);
                STAFF   = normalizePrefixOrColorString(config.getString("Staff",   "§4"), null);
                WORLD   = normalizePrefixOrColorString(config.getString("World",   "§3"), null);
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
    public static void load(FileConfiguration config, FactionsBridge bridge) 
    {
        // Chat settings
        chatFormat = config.getString("ChatSettings.ChatFormat", DEFAULT_CHAT_FORMAT);
        allowColorCodes = config.getBoolean("ChatSettings.AllowColorCodes", true);
        allowUrl = config.getBoolean("ChatSettings.AllowClickableLinks", true);
        allowUrlUnderline = config.getBoolean("ChatSettings.AllowClickableLinksUnderline", true);
        localChatRange = config.getInt("ChatSettings.LocalChatRange", 1000);
        disableChatReporting = config.getBoolean("ChatSettings.DisableChatReporting", false);
        preserveUpstreamChatComponents = config.getBoolean("ChatSettings.PreserveUpstreamChatComponents", true);

        QuickChat.initialize(config.getConfigurationSection("ChatSettings"));

        if (!config.contains("ChatSettings.BlacklistedMiniMessageCommands"))
        {
            blacklistedMiniMessageCommands = new ArrayList<>(DEFAULT_BLACKLISTED_MINIMESSAGE_COMMANDS);
        }
        else
        {
            blacklistedMiniMessageCommands = new ArrayList<>(config.getStringList("ChatSettings.BlacklistedMiniMessageCommands"));
        }

        // Initialize nested settings
        ChatPrefixes.initialize(config.getConfigurationSection("ChatPrefixes"), bridge);
        TextColors.initialize(config.getConfigurationSection("TextColors"), bridge);
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
        return ChatTxt.parseLegacy('&', s);
    }

    /**
     * Sanitizes {@link QuickChat#prefix}: empty, null, or whitespace-containing values fall back to {@code ":"}.
     */
    private static String normalizeQuickChatPrefix(String raw)
    {
        if (raw == null)
        {
            return ":";
        }
        String t = raw.trim();
        if (t.isEmpty())
        {
            return ":";
        }
        for (int i = 0; i < t.length(); i++)
        {
            if (Character.isWhitespace(t.charAt(i)))
            {
                return ":";
            }
        }
        final int maxLen = 32;
        if (t.length() > maxLen)
        {
            return t.substring(0, maxLen);
        }
        return t;
    }
}
