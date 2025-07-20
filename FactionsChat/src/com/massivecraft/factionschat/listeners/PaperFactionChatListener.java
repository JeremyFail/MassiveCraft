package com.massivecraft.factionschat.listeners;

import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Listens for Paper's AsyncChatEvent and handles FactionsChat formatting.
 * 
 * This cancels the original chat event and sends custom formatted messages
 * as server messages which allows per-recipient, per-message formatting
 * using Placeholder API or built-in tags, using the format string from the config.
 *
 * This listener is only registered if the server is running Paper.
 */
public class PaperFactionChatListener extends BaseFactionChatListener implements Listener
{    
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    /**
     * Handles the AsyncChatEvent.
     * This method processes the chat message, applies the appropriate chat mode,
     * and sends formatted messages to appropriate recipients as server messages.
     * 
     * @param event The AsyncChatEvent triggered through chat
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncChat(AsyncChatEvent event)
    {
        Player sender = event.getPlayer();
        
        // Cancel the original chat event - we'll send our own formatted messages
        event.setCancelled(true);
        
        // Use quick chat mode if present, otherwise persistent
        final ChatMode chatMode = determinePlayerChatMode(sender);

        // Apply general placeholders to the chat format (this is the same for all recipients)
        String preParsedFormat = applyNonRelationalPlaceholders(sender, FactionsChat.instance.getChatFormat(), chatMode);
        TextColor baseColor = getBaseColorFromFormat(preParsedFormat);
        
        // Process the message to apply any formatting (this is the same for all recipients)
        Component processedMessageComponent = processMessageForSender(sender, serializer.serialize(event.message()), baseColor, chatMode);
        
        // Filter and send to viewers
        for (Audience audience : event.viewers())
        {
            if (!(audience instanceof Player))
            {
                continue; // Skip non-player audiences (console isn't usually in viewers anyway)
            }

            Player player = (Player) audience;
            
            // Send the formatted message if this player should receive it
            if (!shouldExcludeRecipient(chatMode, sender, player))
            {
                audience.sendMessage(formatMessageForRecipient(sender, preParsedFormat, processedMessageComponent, player, baseColor, chatMode));
            }
        }
        
        // Always send to console (console should see all chat messages)
        Component consoleMessage = formatMessageForRecipient(sender, preParsedFormat, processedMessageComponent, null, baseColor, chatMode);
        Bukkit.getConsoleSender().sendMessage(serializer.serialize(consoleMessage));
    }

    /**
     * Extracts the base color from the chat format string and converts to TextColor.
     * This method looks for the last color code (legacy or RGB) before the %MESSAGE% placeholder.
     *
     * @param format The chat format string.
     * @return The base color extracted from the format. Defaults to white if no color is found.
     */
    private TextColor getBaseColorFromFormat(String format)
    {
        BaseColorResult result = extractBaseColorFromFormat(format);
        
        if (result.isRgb)
        {
            try
            {
                return TextColor.fromHexString("#" + result.hexCode);
            }
            catch (IllegalArgumentException ignored)
            {
                // Invalid hex code, fall back to legacy
            }
        }
        
        // Convert legacy ChatColor to TextColor
        Color awtColor = result.legacyColor.asBungee().getColor();
        return TextColor.color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
    }

    /**
     * Formats a message for a specific recipient using the chat format and permissions.
     * The message component is already processed and consistent for all recipients.
     * 
     * @param sender The player sending the message.
     * @param format The pre-parsed chat format string (non-relational placeholders already replaced).
     * @param processedMessageComponent The processed message component for the sender.
     * @param recipient The player receiving the message.
     * @param baseColor The base color to apply to the message.
     * @param chatMode The chat mode being used (e.g., GLOBAL, FACTION, ALLY, etc.).
     */
    private Component formatMessageForRecipient(Player sender, String preParsedFormat, Component processedMessageComponent, Player recipient, TextColor baseColor, ChatMode chatMode)
    {
        // Replace placeholders based on whether PlaceholderAPI is enabled
        preParsedFormat = applyRelationalPlaceholders(sender, recipient, preParsedFormat);

        // Process the entire format string into a component (handling RGB codes if present)
        Component processedFormatComponent;
        if (preParsedFormat.contains("&#") || preParsedFormat.contains("§#") || preParsedFormat.contains("§x"))
        {
            processedFormatComponent = processRgbColorCodes(preParsedFormat, baseColor);
        }
        else
        {
            processedFormatComponent = serializer.deserialize(preParsedFormat);
        }

        // Replace the placeholder with the actual message component
        return replaceComponentPlaceholder(processedFormatComponent, PLACEHOLDER_MESSAGE, processedMessageComponent);
    }

    /**
     * Processes the message content for the sender (color codes, permissions, etc.).
     * This is done once per chat message and reused for all recipients since 
     * the message content itself doesn't change per recipient.
     * 
     * @param sender The player sending the message.
     * @param originalMessage The original message content from the player.
     * @param baseColor The base color to apply to the message.
     * @param chatMode The chat mode being used (e.g., GLOBAL, FACTION, ALLY, etc.).
     * @return A Component with the processed message ready for sending.
     */
    private Component processMessageForSender(Player sender, String originalMessage, TextColor baseColor, ChatMode chatMode)
    {
        // Get player permissions
        ChatPermissions permissions = getPlayerChatPermissions(sender);

        // Remove disallowed codes and parse legacy color/format codes in the original message
        String processedMessage = stripColorFormatCodes(originalMessage, permissions.allowColor, permissions.allowFormat, permissions.allowMagic, permissions.allowRgb);

        // - - - - - RGB Color Code Processing - - - - -
        Component messageComponent;
        if (permissions.allowRgb && (processedMessage.contains("&#") || processedMessage.contains("§#") || processedMessage.contains("§x")))
        {
            messageComponent = processRgbColorCodes(processedMessage, baseColor);
        }
        else
        {
            messageComponent = Component.text(processedMessage).color(baseColor);
        }

        // - - - - - Clickable URL Processing - - - - -
        if (permissions.allowUrl)
        {
            messageComponent = processLinks(messageComponent, permissions.underlineUrl, baseColor);
        }

        return messageComponent;
    }

    /**
     * Processes links in the input Component and makes them clickable.
     * After each link, reapplies the most recent color (legacy or RGB) found before the link.
     *
     * @param input The input Component containing potential links.
     * @param underline Whether to underline the links.
     * @param baseColor The base color to apply if no color code is found.
     * @return A Component with clickable links.
     */
    private Component processLinks(Component input, boolean underline, TextColor baseColor)
    {
        Pattern urlPattern = Pattern.compile(URL_REGEX);
        // Use the serializer to get the correct plain text with color codes
        String inputStr = LegacyComponentSerializer.legacySection().serialize(input);
        Matcher matcher = urlPattern.matcher(inputStr);
        int lastEnd = 0;
        Component comp = Component.empty();
        while (matcher.find())
        {
            String before = inputStr.substring(lastEnd, matcher.start());
            // Find the most recent color code (legacy or RGB) in 'before'
            TextColor lastColor = getLastTextColor(before, baseColor);
            if (!before.isEmpty())
            {
                comp = comp.append(Component.text(before).color(lastColor));
            }
            String url = matcher.group(1);
            Component urlComponent = Component.text(url).color(lastColor)
                .clickEvent(ClickEvent.openUrl(url));
            if (underline)
            {
                urlComponent = urlComponent.decorate(TextDecoration.UNDERLINED);
            }
            comp = comp.append(urlComponent);
            lastEnd = matcher.end();
        }
        if (lastEnd < inputStr.length())
        {
            // Find the most recent color code in the last segment
            TextColor lastColor = getLastTextColor(inputStr.substring(0, lastEnd), baseColor);
            comp = comp.append(Component.text(inputStr.substring(lastEnd)).color(lastColor));
        }
        return comp;
    }

    /**
     * Finds the last color code (legacy or RGB) in the given text, or returns the base color if none found.
     * Supports modern RGB (&#RRGGBB), legacy Bukkit RGB (§x§R§R§G§G§B§B), and legacy color codes (§[0-9a-fA-F]).
     * 
     * @param text The text to search for color codes.
     * @param baseColor The base color to fall back to if no color code is found.
     * @return The last found TextColor, or the base color if no color code is found.
     */
    private TextColor getLastTextColor(String text, TextColor baseColor)
    {
        TextColor lastColor = null;
        int lastColorPosition = -1;

        // Look for RGB color codes using the comprehensive regex pattern
        Pattern rgbPattern = Pattern.compile(RGB_REGEX);
        Matcher rgbMatcher = rgbPattern.matcher(text);
        
        while (rgbMatcher.find())
        {
            try
            {
                String hex = null;
                
                // Check which group matched (modern vs legacy format)
                if (rgbMatcher.group(1) != null)
                {
                    // Modern format: &#RRGGBB or §#RRGGBB
                    hex = rgbMatcher.group(1);
                }
                else if (rgbMatcher.group(2) != null)
                {
                    // Legacy Bukkit format: §x§R§R§G§G§B§B
                    String legacyHex = rgbMatcher.group(2);
                    // Extract hex digits from §R§R§G§G§B§B format
                    StringBuilder hexBuilder = new StringBuilder();
                    for (int i = 1; i < legacyHex.length(); i += 2)
                    {
                        hexBuilder.append(legacyHex.charAt(i));
                    }
                    hex = hexBuilder.toString();
                }
                
                if (hex != null)
                {
                    // Convert 3-digit hex to 6-digit hex if needed
                    if (hex.length() == 3)
                    {
                        hex = "" + hex.charAt(0) + hex.charAt(0) + 
                                  hex.charAt(1) + hex.charAt(1) + 
                                  hex.charAt(2) + hex.charAt(2);
                    }
                    
                    TextColor color = TextColor.fromHexString("#" + hex);
                    if (rgbMatcher.end() > lastColorPosition)
                    {
                        lastColor = color;
                        lastColorPosition = rgbMatcher.end();
                    }
                }
            }
            catch (Exception ignored) 
            {
                // Invalid hex code, continue searching
            }
        }
        
        // Look for legacy color codes (§[0-9a-fA-F])
        for (int i = text.length() - 2; i >= 0; i--)
        {
            if (text.charAt(i) == '§' && i + 1 < text.length())
            {
                char code = text.charAt(i + 1);
                ChatColor chatColor = ChatColor.getByChar(code);
                if (chatColor != null && chatColor.isColor())
                {
                    // Check if this legacy color code is more recent than any RGB code found
                    if (i + 2 > lastColorPosition)
                    {
                        java.awt.Color awtColor = chatColor.asBungee().getColor();
                        lastColor = TextColor.color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                    }
                    break; // We found the most recent legacy color, stop searching
                }
            }
        }
        
        // Return the most recent color found, or base color if none
        return lastColor != null ? lastColor : baseColor;
    }

    /**
     * Processes RGB color codes in multiple formats and converts them to TextColor components.
     * Supports modern RGB (&#RRGGBB), legacy Bukkit RGB (§x§R§R§G§G§B§B), and 3-digit hex codes.
     *
     * @param message The message containing RGB color codes.
     * @param baseColor The base color to use if no RGB code is present.
     * @return A Component with the RGB colors applied.
     */
    private Component processRgbColorCodes(String message, TextColor baseColor)
    {
        Pattern rgbPattern = Pattern.compile(RGB_REGEX);
        Matcher rgbMatcher = rgbPattern.matcher(message);
        int lastEnd = 0;
        Component comp = Component.empty();
        TextColor currentColor = null;
        
        // Iterate through all matches of the RGB pattern
        while (rgbMatcher.find())
        {
            // Get the substring before the current match and append it to the component with the current color
            String before = message.substring(lastEnd, rgbMatcher.start());
            if (!before.isEmpty())
            {
                if (currentColor != null)
                {
                    comp = comp.append(Component.text(before).color(currentColor));
                }
                else
                {
                    comp = comp.append(Component.text(before).color(baseColor));
                }
            }
            
            // Extract the hex color code based on which format matched
            String hex = null;
            if (rgbMatcher.group(1) != null)
            {
                // Modern format: &#RRGGBB or §#RRGGBB
                hex = rgbMatcher.group(1);
            }
            else if (rgbMatcher.group(2) != null)
            {
                // Legacy Bukkit format: §x§R§R§G§G§B§B
                String legacyHex = rgbMatcher.group(2);
                // Extract hex digits from §R§R§G§G§B§B format
                StringBuilder hexBuilder = new StringBuilder();
                for (int i = 1; i < legacyHex.length(); i += 2)
                {
                    hexBuilder.append(legacyHex.charAt(i));
                }
                hex = hexBuilder.toString();
            }
            
            if (hex != null)
            {
                try
                {
                    // Convert 3-digit hex to 6-digit hex if needed
                    if (hex.length() == 3)
                    {
                        hex = "" + hex.charAt(0) + hex.charAt(0) + 
                                  hex.charAt(1) + hex.charAt(1) + 
                                  hex.charAt(2) + hex.charAt(2);
                    }
                    currentColor = TextColor.fromHexString("#" + hex);
                }
                catch (IllegalArgumentException ignored)
                {
                    // Invalid hex code, keep current color
                }
            }
            
            lastEnd = rgbMatcher.end();
        }
        
        // Append any remaining text after the last RGB code
        if (lastEnd < message.length())
        {
            String after = message.substring(lastEnd);
            if (currentColor != null)
            {
                comp = comp.append(Component.text(after).color(currentColor));
            }
            else
            {
                comp = comp.append(Component.text(after).color(baseColor));
            }
        }
        
        return comp;
    }

    /**
     * Replaces a text placeholder in a Component with another Component.
     * This method recursively walks through the component tree to find and replace text placeholders.
     * 
     * @param component The component to search in.
     * @param placeholder The text placeholder to replace.
     * @param replacement The component to replace the placeholder with.
     * @return A new Component with the placeholder replaced.
     */
    private Component replaceComponentPlaceholder(Component component, String placeholder, Component replacement)
    {
        // Check if this is a text component containing our placeholder
        if (component instanceof TextComponent)
        {
            TextComponent textComponent = (TextComponent) component;
            String content = textComponent.content();
            
            if (content.contains(placeholder))
            {
                // Split the text around the placeholder
                String[] parts = content.split(Pattern.quote(placeholder), -1);
                Component result = Component.empty();
                
                for (int i = 0; i < parts.length; i++)
                {
                    if (!parts[i].isEmpty())
                    {
                        result = result.append(Component.text(parts[i]).style(textComponent.style()));
                    }
                    if (i < parts.length - 1)
                    {
                        result = result.append(replacement);
                    }
                }
                
                // Preserve any children components
                for (Component child : textComponent.children())
                {
                    result = result.append(replaceComponentPlaceholder(child, placeholder, replacement));
                }
                
                return result;
            }
        }
        
        // If no placeholder found in this component, check children
        Component result = component.children().isEmpty() 
            ? component 
            : component.children(component.children().stream()
                .map(child -> replaceComponentPlaceholder(child, placeholder, replacement))
                .collect(Collectors.toList()));
        
        return result;
    }
}
