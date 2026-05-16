package com.failprooftech.factionschat.util;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for resolving {@code %placeholder%} tokens in format strings.
 * Supports modifier syntax: {@code %key|modifier%}.
 *
 * <p>Ported from {@code com.massivecraft.massivecore.util.PlaceholderProcessor}
 * to remove the compile-time dependency on MassiveCore.</p>
 */
public final class PlaceholderProcessor
{
    /** Matches {@code %anything%} tokens, capturing the inner content. */
    private static final Pattern PATTERN = Pattern.compile("%([^%\\s]+)%");

    private PlaceholderProcessor() {}

    // -------------------------------------------- //
    // PUBLIC API
    // -------------------------------------------- //

    /**
     * Replace all {@code %key%} (or {@code %key|modifier%}) tokens in {@code format}.
     *
     * <p>The {@code resolver} is called with the base key (no modifiers). Returning
     * {@code null} leaves the original token unchanged; any other value replaces it
     * (with any modifier applied).</p>
     *
     * @param format   the input string; returned as-is when {@code null}
     * @param resolver maps a base placeholder key to its replacement
     * @return the processed string
     */
    public static String processPlaceholders(String format, Function<String, String> resolver)
    {
        if (format == null) return null;
        Matcher m = PATTERN.matcher(format);
        StringBuffer sb = new StringBuffer();
        while (m.find())
        {
            String inner    = m.group(1);
            String[] parts  = inner.split("\\|", 2);
            String key      = parts[0];
            String modifier = parts.length > 1 ? parts[1] : null;
            String resolved = resolver.apply(key);
            if (resolved == null)
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            else
                m.appendReplacement(sb, Matcher.quoteReplacement(applyModifier(resolved, modifier)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resolve a single placeholder that may include a modifier suffix.
     *
     * <p>The {@code resolver} receives only the base key (before any {@code |}).
     * If it returns {@code null} this method returns {@code null} as well.</p>
     *
     * @param placeholder  raw placeholder text, e.g. {@code "chat_prefix|rp10"}
     * @param resolver     maps a base key to its value
     * @return the resolved (and modifier-applied) string, or {@code null}
     */
    public static String parsePlaceholderWithModifiers(String placeholder, Function<String, String> resolver)
    {
        if (placeholder == null) return null;
        String[] parts  = placeholder.split("\\|", 2);
        String key      = parts[0];
        String modifier = parts.length > 1 ? parts[1] : null;
        String resolved = resolver.apply(key);
        if (resolved == null) return null;
        return applyModifier(resolved, modifier);
    }

    // -------------------------------------------- //
    // PRIVATE HELPERS
    // -------------------------------------------- //

    /**
     * Apply padding modifiers to a resolved value.
     *
     * <p>Supported modifiers:</p>
     * <ul>
     *   <li>{@code lp<n>} – left-pad to {@code n} characters with spaces</li>
     *   <li>{@code rp<n>} – right-pad to {@code n} characters with spaces</li>
     * </ul>
     */
    private static String applyModifier(String value, String modifier)
    {
        if (modifier == null || modifier.isEmpty()) return value;
        String lower = modifier.toLowerCase();
        boolean rightPad;
        String widthStr;
        if (lower.startsWith("rp"))      { rightPad = true;  widthStr = lower.substring(2); }
        else if (lower.startsWith("lp")) { rightPad = false; widthStr = lower.substring(2); }
        else                             { return value; } // Unknown modifier - ignore
        if (widthStr.isEmpty()) return value;
        try
        {
            int width = Integer.parseInt(widthStr);
            if (value.length() >= width) return value;
            String padding = " ".repeat(width - value.length());
            return rightPad ? value + padding : padding + value;
        }
        catch (NumberFormatException e) { return value; }
    }
}
