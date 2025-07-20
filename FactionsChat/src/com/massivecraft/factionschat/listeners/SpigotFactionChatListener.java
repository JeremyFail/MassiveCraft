package com.massivecraft.factionschat.listeners;

import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for Spigot's AsyncPlayerChatEvent and handles FactionsChat formatting and delivery.
 * Supports per-recipient filtering through manual message sending, PlaceholderAPI, and built-in tag parsing.
 * The chat format is configurable via the config file.
 *
 * This listener is only registered if the server is running Spigot (not Paper).
 */
public class SpigotFactionChatListener extends BaseFactionChatListener implements Listener
{
    /**
     * Handles the AsyncPlayerChatEvent.
     * This method processes the chat message, applies the appropriate chat mode,
     * and formats the message for each recipient.
     * 
     * @param event The AsyncPlayerChatEvent triggered through chat
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(AsyncPlayerChatEvent event)
    {
        Player sender = event.getPlayer();
        
        // Use quick chat mode if present, otherwise persistent
        final ChatMode chatMode = determinePlayerChatMode(sender);

        // Filter recipients based on chat mode
        Set<Player> notReceiving = new HashSet<>();
        for (Player recipient : event.getRecipients())
        {
            if (shouldExcludeRecipient(chatMode, sender, recipient))
            {
                notReceiving.add(recipient);
            }
        }
        event.getRecipients().removeAll(notReceiving);
        
        handleChat(sender, event.getMessage(), event.getRecipients(), chatMode);

        // Event is cancelled as we handle the chat manually
        event.setCancelled(true);
    }

    /**
     * Handles the chat message formatting and sending to recipients.
     * 
     * @param sender The player sending the message
     * @param message The chat message being sent
     * @param recipients Set of players who should receive the message
     * @param chatMode The chat mode being used (e.g., GLOBAL, FACTION, ALLY, etc.)
     */
    private void handleChat(Player sender, String message, Set<Player> recipients, ChatMode chatMode)
    {
        // Get player permissions
        ChatPermissions permissions = getPlayerChatPermissions(sender);
        
        // Apply non-relational placeholders to the format
        String format = applyNonRelationalPlaceholders(sender, FactionsChat.instance.getChatFormat(), chatMode);
        
        // Extract base color from format
        BaseColorResult baseColorResult = extractBaseColorFromFormat(format);
        ChatColor baseColor = baseColorResult.legacyColor;

        // Process the message content
        String processedMessage = stripColorFormatCodes(message, permissions.allowColor, permissions.allowFormat, permissions.allowMagic, permissions.allowRgb);
        processedMessage = processRgbColorCodes(processedMessage, permissions.allowRgb);
        processedMessage = processLinks(processedMessage, permissions.allowUrl, permissions.underlineUrl, baseColor);

        // Replace %MESSAGE% placeholder
        format = format.replace(PLACEHOLDER_MESSAGE, processedMessage);
        
        // Send to each recipient with relational placeholders
        for (Player recipient : recipients)
        {
            String personalizedFormat = applyRelationalPlaceholders(sender, recipient, format);
            recipient.sendMessage(personalizedFormat);
        }
    }

    /**
     * Processes RGB color codes in multiple formats and converts them to Bukkit's legacy RGB format.
     * Supports modern RGB (&#RRGGBB, &#RGB), legacy modern (§#RRGGBB, §#RGB), and legacy Bukkit (§x§R§R§G§G§B§B).
     * 
     * @param message The message to process
     * @param allowRgb Whether RGB codes are allowed
     * @return The processed message with RGB codes converted to §x§R§R§G§G§B§B format
     */
    private static String processRgbColorCodes(String message, boolean allowRgb)
    {
        if (!allowRgb)
        {
            return message;
        }
        
        // Use the comprehensive regex to find all RGB formats
        Pattern pattern = Pattern.compile(RGB_REGEX);
        Matcher matcher = pattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find())
        {
            String hex = null;
            
            // Check which group matched (modern vs legacy format)
            if (matcher.group(1) != null)
            {
                // Modern format: &#RRGGBB or §#RRGGBB
                hex = matcher.group(1);
            }
            else if (matcher.group(2) != null)
            {
                // Legacy Bukkit format: §x§R§R§G§G§B§B - already in correct format
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
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
                
                // Convert to Bukkit's hex color format (§x§R§R§G§G§B§B)
                StringBuilder bukkit = new StringBuilder("§x");
                for (char c : hex.toCharArray())
                {
                    bukkit.append('§').append(c);
                }
                matcher.appendReplacement(sb, bukkit.toString());
            }
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Processes links in the message for Spigot's string-based chat system.
     * Ensures links are underlined if allowed, and re-applies the most recent color code after each link.
     *
     * @param message The message to process
     * @param allowUrl Whether URLs are allowed
     * @param underline Whether to underline the links
     * @param baseColor The base ChatColor to use if no color code is found
     * @return The processed message
     */
    private static String processLinks(String message, boolean allowUrl, boolean underline, ChatColor baseColor)
    {
        if (!allowUrl)
        {
            // Break links by removing periods
            Pattern urlPattern = Pattern.compile(URL_REGEX);
            Matcher matcher = urlPattern.matcher(message);
            StringBuffer sb = new StringBuffer();
            while (matcher.find())
            {
                String url = matcher.group(1);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(url.replace('.', ' ')));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
        
        Pattern urlPattern = Pattern.compile(URL_REGEX);
        Matcher matcher = urlPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        int lastEnd = 0;
        
        while (matcher.find())
        {
            String before = message.substring(lastEnd, matcher.start());
            String url = matcher.group(1);
            
            // Find the most recent color code (including §x hex) in 'before'
            String colorCode = getLastColorCodeString(before, baseColor);
            
            // Underline the link if requested (using §n), then reset and re-apply the most recent color code
            String replacement = underline ? ChatColor.UNDERLINE + url + ChatColor.RESET + colorCode : url + colorCode;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            lastEnd = matcher.end();
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Finds the last color code (legacy or RGB) in the given text as a string for Spigot.
     * Supports modern RGB (&#RRGGBB), legacy Bukkit RGB (§x§R§R§G§G§B§B), and legacy color codes (§[0-9a-fA-F]).
     */
    private static String getLastColorCodeString(String text, ChatColor baseColor)
    {
        String lastColorCode = null;
        int lastColorPosition = -1;

        // Look for RGB color codes using the comprehensive regex
        Pattern rgbPattern = Pattern.compile(RGB_REGEX);
        Matcher rgbMatcher = rgbPattern.matcher(text);
        
        while (rgbMatcher.find())
        {
            String hex = null;
            
            // Check which group matched (modern vs legacy format)
            if (rgbMatcher.group(1) != null)
            {
                // Modern format: &#RRGGBB or §#RRGGBB
                hex = rgbMatcher.group(1);
                
                // Convert 3-digit hex to 6-digit hex if needed
                if (hex.length() == 3)
                {
                    hex = "" + hex.charAt(0) + hex.charAt(0) + 
                              hex.charAt(1) + hex.charAt(1) + 
                              hex.charAt(2) + hex.charAt(2);
                }
                
                // Convert to Bukkit's hex color format (§x§R§R§G§G§B§B)
                StringBuilder bukkit = new StringBuilder("§x");
                for (char c : hex.toCharArray())
                {
                    bukkit.append('§').append(c);
                }
                
                if (rgbMatcher.end() > lastColorPosition)
                {
                    lastColorCode = bukkit.toString();
                    lastColorPosition = rgbMatcher.end();
                }
            }
            else if (rgbMatcher.group(2) != null)
            {
                // Legacy Bukkit format: §x§R§R§G§G§B§B - already in correct format
                if (rgbMatcher.end() > lastColorPosition)
                {
                    lastColorCode = rgbMatcher.group(0); // Full match including §x
                    lastColorPosition = rgbMatcher.end();
                }
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
                        lastColorCode = "§" + code;
                    }
                    break; // We found the most recent legacy color, stop searching
                }
            }
        }
        
        // Return the most recent color found, or base color if none
        return lastColorCode != null ? lastColorCode : baseColor.toString();
    }
}
