package com.massivecraft.massivecore.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utility class for processing placeholders with modifiers.
 * Supports dynamic modifiers like |lp (left padding), |rp (right padding), |bp (both padding),
 * and counted variants |lpN, |rpN, |bpN where N is the number of spaces to add.
 * 
 * This class is designed to be used by any MassiveCraft plugin that needs
 * advanced placeholder processing with modifier support.
 */
public class PlaceholderProcessor
{
    private static final String[] SPACE_CACHE = {"", " ", "  ", "   ", "    ", "     ", "      ", "       "};

    /**
     * Pattern to match placeholders with optional modifiers.
     * Matches: %placeholder%, %placeholder|modifier%, %placeholder|mod1|mod2%
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)((?:\\|[a-z]+[0-9]*)*)%");
    
    /**
     * Processes a format string, replacing placeholders with their values and applying modifiers.
     * 
     * @param format The format string containing placeholders
     * @param resolver A function that maps placeholder names to their values
     * @return The processed format string with placeholders replaced and modifiers applied
     */
    public static String processPlaceholders(String format, Function<String, String> resolver)
    {
        if (format == null) return null;
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(format);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find())
        {
            String basePlaceholder = matcher.group(1); // e.g., "factions_faction_name"
            String modifiersString = matcher.group(2); // e.g., "|rp|lp" or null
            
            // Parse modifiers
            List<String> modifiers = parseModifiers(modifiersString);
            
            // Resolve the base placeholder value
            String value = resolver.apply(basePlaceholder);
            if (value == null)
            {
                // If resolver returns null, leave the placeholder unchanged
                // This allows other systems to handle it
                continue;
            }
            
            // Apply modifiers to the value
            String processedValue = applyModifiers(value, modifiers);
            
            // Replace in the result
            matcher.appendReplacement(result, Matcher.quoteReplacement(processedValue));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Parses a modifier string like "|rp|lp" into a list of individual modifiers.
     * 
     * @param modifiersString The modifier string (may be null or empty)
     * @return A list of modifier names (e.g., ["rp", "lp"])
     */
    private static List<String> parseModifiers(String modifiersString)
    {
        List<String> modifiers = new ArrayList<>();
        
        if (modifiersString != null && !modifiersString.isEmpty())
        {
            // Split by "|" and filter out empty strings
            String[] parts = modifiersString.split("\\|");
            for (String part : parts)
            {
                part = part.trim();
                if (!part.isEmpty())
                {
                    modifiers.add(part.toLowerCase());
                }
            }
        }
        
        return modifiers;
    }
    
    /**
     * Applies a list of modifiers to a placeholder value.
     * 
     * @param value The resolved placeholder value
     * @param modifiers The list of modifiers to apply
     * @return The value with modifiers applied
     */
    private static String applyModifiers(String value, List<String> modifiers)
    {
        // If the value is null or empty, don't apply any padding modifiers
        if (value == null || value.isEmpty())
        {
            return "";
        }
        
        String result = value;
        
        for (String modifier : modifiers)
        {
            int leftPad = parsePaddingCount(modifier, "lp");
            if (leftPad > 0)
            {
                result = spaces(leftPad) + result;
                continue;
            }

            int rightPad = parsePaddingCount(modifier, "rp");
            if (rightPad > 0)
            {
                result = result + spaces(rightPad);
                continue;
            }

            int bothPad = parsePaddingCount(modifier, "bp");
            if (bothPad > 0)
            {
                String pad = spaces(bothPad);
                result = pad + result + pad;
                continue;
            }

            switch (modifier)
            {
                case "trim": 
                    result = result.trim(); 
                    break;
                case "upper": 
                    result = result.toUpperCase(); 
                    break;
                case "lower": 
                    result = result.toLowerCase(); 
                    break;
                default:
                    // Ignore unknown modifiers
                    break;
            }
        }
        
        return result;
    }

    /**
     * Parses a padding modifier such as {@code lp}, {@code lp3}, or {@code bp12}.
     *
     * @param modifier The full modifier token
     * @param prefix The padding prefix ({@code lp}, {@code rp}, or {@code bp})
     * @return The number of spaces to add, {@code 0} for {@code prefix0} or invalid suffixes,
     *         or {@code -1} if the modifier is not a padding modifier for that prefix
     */
    private static int parsePaddingCount(String modifier, String prefix)
    {
        if (!modifier.startsWith(prefix)) return -1;

        if (modifier.length() == prefix.length()) return 1;

        try
        {
            return Integer.parseInt(modifier.substring(prefix.length()));
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    /**
     * Returns a string of {@code count} space characters.
     * Small counts are served from a cache; larger counts allocate on demand.
     * 
     * @param count The number of spaces to return.
     * @return A string of {@code count} space characters.
     */
    private static String spaces(int count)
    {
        if (count <= 0) return "";
        if (count < SPACE_CACHE.length) return SPACE_CACHE[count];

        char[] chars = new char[count];
        for (int i = 0; i < count; i++) chars[i] = ' ';
        return new String(chars);
    }
    
    /**
     * Parses placeholder with modifiers from a external request.
     * This is used in integrations where the placeholder string
     * may contain modifiers that need to be parsed and handled.
     * 
     * @param placeholder The placeholder string (e.g., "prefix|rp")
     * @param resolver A function that maps the base placeholder to its value
     * @return The processed value with modifiers applied, or null if not handled
     */
    public static String parsePlaceholderWithModifiers(String placeholder, Function<String, String> resolver)
    {
        if (placeholder == null) return null;
        
        // Split the placeholder and modifiers
        String[] parts = placeholder.split("\\|", 2);
        String basePlaceholder = parts[0];
        String modifiersString = parts.length > 1 ? "|" + parts[1] : "";
        
        // Parse modifiers
        List<String> modifiers = parseModifiers(modifiersString);
        
        // Resolve the base placeholder
        String value = resolver.apply(basePlaceholder);
        if (value == null)
        {
            return null; // Let external plugins handle unknown placeholders
        }
        
        // Apply modifiers and return
        return applyModifiers(value, modifiers);
    }
    
    /**
     * Creates a resolver function from a map of placeholder values.
     * This is a convenience method for simple use cases.
     * 
     * @param placeholderMap A map of placeholder names to their values
     * @return A resolver function that looks up values in the map
     */
    public static Function<String, String> mapResolver(Map<String, String> placeholderMap)
    {
        return placeholderMap::get;
    }
}
