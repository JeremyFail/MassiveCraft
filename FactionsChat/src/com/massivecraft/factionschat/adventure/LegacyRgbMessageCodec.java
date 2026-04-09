package com.massivecraft.factionschat.adventure;

import com.massivecraft.factionschat.listeners.FactionChatListenerBase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Legacy section codes + FactionsChat RGB patterns ({@code &#RRGGBB}, {@code §x…}), used as the fallback pipeline
 * inside {@link PaperAdventureChatCodec}.
 */
public final class LegacyRgbMessageCodec implements PaperAdventureChatCodec.LegacyRgbPipeline
{
    private final LegacyComponentSerializer serializer;

    public LegacyRgbMessageCodec(LegacyComponentSerializer serializer)
    {
        this.serializer = serializer;
    }

    /**
     * Converts a normalized expanded string to a component.
     * 
     * @param normalizedExpanded The normalized expanded string.
     * @param baseColor The base color to apply.
     * @return The component.
     */
    @Override
    public Component toComponent(String normalizedExpanded, TextColor baseColor)
    {
        if (normalizedExpanded.contains("&#") || normalizedExpanded.contains("§#") || normalizedExpanded.contains("§x"))
        {
            return processRgbColorCodes(normalizedExpanded, baseColor);
        }
        return serializer.deserialize(normalizedExpanded).colorIfAbsent(baseColor);
    }

    /**
     * Processes RGB color codes in multiple formats and converts them to Bukkit's legacy RGB format.
     * Supports modern RGB (&#RRGGBB, &#RGB), legacy modern (§#RRGGBB, §#RGB), and legacy Bukkit (§x§R§R§G§G§B§B).
     * 
     * @param message The message to process.
     * @param baseColor The base color to apply.
     * @return The component.
     */
    private Component processRgbColorCodes(String message, TextColor baseColor)
    {
        Pattern rgbPattern = Pattern.compile(FactionChatListenerBase.RGB_REGEX);
        Matcher rgbMatcher = rgbPattern.matcher(message);

        // If no RGB color codes are found, return the message with the base color applied.
        if (!rgbMatcher.find())
        {
            return serializer.deserialize(message).colorIfAbsent(baseColor);
        }

        rgbMatcher.reset();

        int lastEnd = 0;
        Component comp = Component.empty();
        TextColor currentColor = null;

        // Process each RGB color code in the message.
        while (rgbMatcher.find())
        {
            String before = message.substring(lastEnd, rgbMatcher.start());

            // If the before string is not empty, append it to the component.
            if (!before.isEmpty())
            {
                TextColor colorToUse = currentColor != null ? currentColor : baseColor;
                comp = comp.append(serializer.deserialize(before).colorIfAbsent(colorToUse));
            }

            // Get the hex color code from the RGB color code.
            String hex = null;
            if (rgbMatcher.group(1) != null)
            {
                hex = rgbMatcher.group(1);
            }
            else if (rgbMatcher.group(2) != null)
            {
                String legacyHex = rgbMatcher.group(2);
                StringBuilder hexBuilder = new StringBuilder();
                for (int i = 1; i < legacyHex.length(); i += 2)
                {
                    hexBuilder.append(legacyHex.charAt(i));
                }
                hex = hexBuilder.toString();
            }

            // If the hex color code is not null, try to convert it to a TextColor.
            if (hex != null)
            {
                try
                {
                    if (hex.length() == 3)
                    {
                        hex = "" + hex.charAt(0) + hex.charAt(0)
                            + hex.charAt(1) + hex.charAt(1)
                            + hex.charAt(2) + hex.charAt(2);
                    }
                    currentColor = TextColor.fromHexString("#" + hex);
                }
                catch (IllegalArgumentException ignored)
                {
                }
            }

            lastEnd = rgbMatcher.end();
        }

        // If there is any text after the last RGB color code, append it to the component.
        if (lastEnd < message.length())
        {
            String after = message.substring(lastEnd);
            TextColor colorToUse = currentColor != null ? currentColor : baseColor;
            comp = comp.append(serializer.deserialize(after).colorIfAbsent(colorToUse));
        }

        return comp;
    }
}
