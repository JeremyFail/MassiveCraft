package com.failprooftech.factionschat.util;

import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lightweight drop-in for MassiveCore's {@code Txt} utility, limited to the
 * subset of methods FactionsChat actually uses.
 *
 * <p>No external dependencies - safe to use regardless of which Factions plugin
 * or framework is installed.</p>
 */
public final class ChatTxt
{
    // ------------------------------------------------------------------ //
    // Tag → § colour-code mapping (matches MassiveCore Txt)
    // ------------------------------------------------------------------ //

    private static final Map<String, String> TAGS = new HashMap<>();

    static
    {
        // Bad / Good / Info / Key / Value / Highlight
        TAGS.put("<b>",      "§c");
        TAGS.put("<g>",      "§a");
        TAGS.put("<i>",      "§e");
        TAGS.put("<k>",      "§b");
        TAGS.put("<v>",      "§d");
        TAGS.put("<h>",      "§d");

        // Named colours
        TAGS.put("<black>",       "§0");
        TAGS.put("<navy>",        "§1");
        TAGS.put("<darkgreen>",   "§2");
        TAGS.put("<teal>",        "§3");
        TAGS.put("<darkred>",     "§4");
        TAGS.put("<purple>",      "§5");
        TAGS.put("<gold>",        "§6");
        TAGS.put("<silver>",      "§7");
        TAGS.put("<gray>",        "§8");
        TAGS.put("<grey>",        "§8");
        TAGS.put("<blue>",        "§9");
        TAGS.put("<green>",       "§a");
        TAGS.put("<aqua>",        "§b");
        TAGS.put("<red>",         "§c");
        TAGS.put("<pink>",        "§d");
        TAGS.put("<lightpurple>", "§d");
        TAGS.put("<yellow>",      "§e");
        TAGS.put("<white>",       "§f");

        // Formatting
        TAGS.put("<em>",    "§o");
        TAGS.put("<it>",    "§o");
        TAGS.put("<bold>",  "§l");
        TAGS.put("<strike>","§m");
        TAGS.put("<under>", "§n");
        TAGS.put("<magic>", "§k");
        TAGS.put("<reset>", "§r");
        TAGS.put("<r>",     "§r");
        TAGS.put("<n>",     "§f");

        // Neutral (plain white)
        TAGS.put("<neutral>", "§f");
        TAGS.put("<a>",       "§f");
    }

    private ChatTxt() {}

    // ------------------------------------------------------------------ //
    // Public API
    // ------------------------------------------------------------------ //

    /**
     * Translate MassiveCore-style tags (e.g. {@code <g>}, {@code <b>}) and
     * optional printf-style format specifiers into a coloured legacy string.
     *
     * @param string the template string
     * @param args   optional printf arguments
     * @return coloured legacy string
     */
    public static String parse(String string, Object... args)
    {
        if (string == null) return "";
        if (args != null && args.length > 0)
            string = String.format(string, args);
        for (Map.Entry<String, String> e : TAGS.entrySet())
            string = string.replace(e.getKey(), e.getValue());
        return string;
    }

    /**
     * Translate alternate colour codes (e.g. {@code &a}) to § codes.
     *
     * @param altColorChar the alternate colour character (usually {@code '&'})
     * @param textToParse  the raw string
     * @return translated string
     */
    public static String parseLegacy(char altColorChar, String textToParse)
    {
        if (textToParse == null) return "";
        return ChatColor.translateAlternateColorCodes(altColorChar, textToParse);
    }

    /**
     * Strip all § colour / formatting codes from a string.
     *
     * @param input the coloured string
     * @return plain-text string
     */
    public static String stripColorLegacy(String input)
    {
        if (input == null) return "";
        return ChatColor.stripColor(input);
    }

    // ------------------------------------------------------------------ //
    // Regex pattern for splitting on newlines (matches MassiveCore Txt)
    // ------------------------------------------------------------------ //

    public static final Pattern PATTERN_NEWLINE = Pattern.compile("\n");
}
