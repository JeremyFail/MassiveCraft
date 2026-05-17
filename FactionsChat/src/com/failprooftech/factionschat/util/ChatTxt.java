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
    // Tag to § color-code mapping (matches MassiveCore Txt)
    // ------------------------------------------------------------------ //

    private static final Map<String, String> TAGS = new HashMap<>();

    static
    {
		// Color by name (Minecraft color code in parentheses: §0-§f, §k, §l, §m, §n, §o, §r)
		TAGS.put("<empty>", "");
		TAGS.put("<black>", "\u00A70");         // §0 black
		TAGS.put("<navy>", "\u00A71");          // §1 dark blue
		TAGS.put("<green>", "\u00A72");         // §2 dark green
		TAGS.put("<teal>", "\u00A73");          // §3 dark aqua
		TAGS.put("<red>", "\u00A74");           // §4 dark red
		TAGS.put("<purple>", "\u00A75");        // §5 dark purple
		TAGS.put("<gold>", "\u00A76");          // §6 gold
		TAGS.put("<orange>", "\u00A76");        // §6 gold (alias)
		TAGS.put("<silver>", "\u00A77");        // §7 gray (light gray)
		TAGS.put("<gray>", "\u00A78");          // §8 dark gray
		TAGS.put("<grey>", "\u00A78");          // §8 dark gray (alias)
		TAGS.put("<blue>", "\u00A79");          // §9 blue
		TAGS.put("<lime>", "\u00A7a");          // §a green (bright green)
		TAGS.put("<aqua>", "\u00A7b");          // §b aqua (cyan)
		TAGS.put("<rose>", "\u00A7c");          // §c red (bright red)
		TAGS.put("<pink>", "\u00A7d");          // §d light purple (magenta)
		TAGS.put("<yellow>", "\u00A7e");        // §e yellow
		TAGS.put("<white>", "\u00A7f");         // §f white
		TAGS.put("<magic>", "\u00A7k");         // §k obfuscated (random chars)
		TAGS.put("<bold>", "\u00A7l");          // §l bold
		TAGS.put("<strong>", "\u00A7l");        // §l bold (alias)
		TAGS.put("<strike>", "\u00A7m");        // §m strikethrough
		TAGS.put("<strikethrough>", "\u00A7m"); // §m strikethrough
		TAGS.put("<under>", "\u00A7n");         // §n underline
		TAGS.put("<underline>", "\u00A7n");     // §n underline (alias)
		TAGS.put("<italic>", "\u00A7o");        // §o italic
		TAGS.put("<em>", "\u00A7o");            // §o italic (alias)
		TAGS.put("<reset>", "\u00A7r");         // §r reset

		// Color by semantic functionality (reuse §2, §6, §7, §e, §a, §c, §b, §d, §3)
		TAGS.put("<l>", "\u00A72");             // §2 dark green (logo)
		TAGS.put("<logo>", "\u00A72");
		TAGS.put("<a>", "\u00A76");             // §6 gold (art)
		TAGS.put("<art>", "\u00A76");
		TAGS.put("<n>", "\u00A77");             // §7 gray (notice)
		TAGS.put("<notice>", "\u00A77");
		TAGS.put("<i>", "\u00A7e");             // §e yellow (info)
		TAGS.put("<info>", "\u00A7e");
		TAGS.put("<g>", "\u00A7a");             // §a green (good)
		TAGS.put("<good>", "\u00A7a");
		TAGS.put("<b>", "\u00A7c");             // §c red (bad)
		TAGS.put("<bad>", "\u00A7c");

		TAGS.put("<k>", "\u00A7b");             // §b aqua (key)
		TAGS.put("<key>", "\u00A7b");

		TAGS.put("<v>", "\u00A7d");             // §d light purple (value/highlight)
		TAGS.put("<value>", "\u00A7d");
		TAGS.put("<h>", "\u00A7d");
		TAGS.put("<highlight>", "\u00A7d");

		TAGS.put("<c>", "\u00A7b");             // §b aqua (command)
		TAGS.put("<command>", "\u00A7b");
		TAGS.put("<p>", "\u00A73");             // §3 dark aqua (parameter)
		TAGS.put("<parameter>", "\u00A73");
		TAGS.put("&&", "&");                    // escape ampersand
		TAGS.put("§§", "§");                    // escape section sign
    }

    private ChatTxt() {}

    // ------------------------------------------------------------------ //
    // Public API
    // ------------------------------------------------------------------ //

    /**
     * Translate MassiveCore-style tags (e.g. {@code <g>}, {@code <b>}) and
     * optional printf-style format specifiers into a colored legacy string.
     *
     * @param string the template string
     * @param args   optional printf arguments
     * @return colored legacy string
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
     * Translate alternate color codes (e.g. {@code &a}) to § codes.
     *
     * @param altColorChar the alternate color character (usually {@code '&'})
     * @param textToParse  the raw string
     * @return translated string
     */
    public static String parseLegacy(char altColorChar, String textToParse)
    {
        if (textToParse == null) return "";
        return ChatColor.translateAlternateColorCodes(altColorChar, textToParse);
    }

    /**
     * Strip all § color / formatting codes from a string.
     *
     * @param input the colored string
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
