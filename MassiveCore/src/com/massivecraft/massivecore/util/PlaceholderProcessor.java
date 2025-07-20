package com.massivecraft.massivecore.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utility class for processing placeholders with modifiers.
 * Supports dynamic modifiers like |lp (left padding), |rp (right padding), etc.
 * 
 * This class is designed to be used by any MassiveCraft plugin that needs
 * advanced placeholder processing with modifier support.
 */
public class PlaceholderProcessor
{    
    /**
     * Pattern to match placeholders with optional modifiers.
     * Matches: %placeholder%, %placeholder|modifier%, %placeholder|mod1|mod2%
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)((?:\\|[a-z]+)*)%");
    
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
            switch (modifier)
            {
                case "lp": // Left padding - add space to the left
                    result = " " + result;
                    break;
                case "rp": // Right padding - add space to the right
                    result = result + " ";
                    break;
                case "bp": // Both padding - add space to both sides
                    result = " " + result + " ";
                    break;
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
