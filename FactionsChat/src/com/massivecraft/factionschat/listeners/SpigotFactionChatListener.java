package com.massivecraft.factionschat.listeners;

import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.chat.ChatPermissions;
import com.massivecraft.factionschat.chat.PermissionAwareChatMessage;
import com.massivecraft.factionschat.config.Settings;
import com.massivecraft.factionschat.util.ColonChannelChatParser;
import com.massivecraft.factionschat.util.ColonChannelChatParser.ParseType;

import org.bukkit.Bukkit;
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
 * 
 * Supports per-recipient filtering, PlaceholderAPI, and built-in tag parsing.
 * The chat format is configurable via the config file.
 * 
 * {@link Settings#disableChatReporting} does not change behavior here (Spigot has no Paper-style
 * signed chat renderer); it is documented at startup when false and the server is not Paper.
 * 
 * This listener is only registered if the server is running Spigot (not Paper).
 */
public class SpigotFactionChatListener extends FactionChatListenerBase implements Listener
{
    /**
     * Handles the AsyncPlayerChatEvent.
     * This method processes the chat message, applies the appropriate chat mode,
     * and formats the message for each recipient.
     * 
     * @param event The AsyncPlayerChatEvent triggered through chat.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event)
    {
        Player sender = event.getPlayer();
        String raw = event.getMessage();

        // Parse the chat message for colon channel prefixes
        ColonChannelChatParser.ParseResult colon = ColonChannelChatParser.parse(sender, raw);
        if (colon.getType() == ParseType.INVALID)
        {
            event.setCancelled(true);
            final String err = colon.getInvalidReason();
            runSync(() -> sender.sendMessage(err));
            return;
        }
        if (colon.getType() == ParseType.TOGGLE)
        {
            event.setCancelled(true);
            final ChatMode mode = colon.getTargetMode();
            runSync(() ->
            {
                FactionsChat.instance.getPlayerChatModes().put(sender.getUniqueId(), mode);
                sender.sendMessage(ChatColor.YELLOW + "Chat mode set to: " + ChatColor.AQUA + mode.name().toLowerCase());
            });
            return;
        }

        // Determine the chat mode and message text
        final ChatMode chatMode;
        final String messageText;
        final boolean colonQuick;
        if (colon.getType() == ParseType.QUICK_MESSAGE)
        {
            chatMode = colon.getTargetMode();
            messageText = colon.getMessageBody();
            colonQuick = true;
        }
        else
        {
            chatMode = ChatMode.getChatModeForPlayer(sender);
            messageText = colon.getMessageBody();
            colonQuick = false;
        }

        if (denyIfBlacklistedMiniMessageClick(sender, messageText))
        {
            event.setCancelled(true);
            return;
        }

        // Filter out recipients who should not receive the message
        Set<Player> notReceiving = new HashSet<>();
        for (Player recipient : event.getRecipients())
        {
            if (shouldExcludeRecipient(chatMode, sender, recipient))
            {
                notReceiving.add(recipient);
            }
        }

        // Remove the recipients who should not receive the message
        event.getRecipients().removeAll(notReceiving);
        
        // Event is cancelled as we are handling the chat ourselves
        event.setCancelled(true);
        handleChat(sender, messageText, event.getRecipients(), chatMode, colonQuick);
    }

    /**
     * Handles the chat message formatting and sending to recipients.
     * 
     * @param sender The player sending the message.
     * @param message The chat message being sent.
     * @param recipients Set of players who should receive the message.
     * @param chatMode The chat mode being used (e.g., GLOBAL, FACTION, ALLY, etc.).
     * @param colonQuick Whether the chat message is a colon channel quick message.
     */
    private void handleChat(Player sender, String message, Set<Player> recipients, ChatMode chatMode, boolean colonQuick)
    {
        try
        {
            if (colonQuick)
            {
                FactionsChat.setChatModePlaceholderOverride(chatMode);
            }

            ChatPermissions permissions = getPlayerChatPermissions(sender);

            // Apply non-relational placeholders to the format
            String format = applyNonRelationalPlaceholders(sender, Settings.chatFormat, chatMode);

            // Extract base color from format
            BaseColorResult baseColorResult = extractBaseColorFromFormat(format);
            ChatColor baseColor = baseColorResult.legacyColor;

            // Disallowed codes stay literal; allowed spans get & -> § and RGB expansion like before.
            String processedMessage = PermissionAwareChatMessage.toBukkitLegacyString(message, permissions, baseColor);
            processedMessage = processLinks(processedMessage, permissions, baseColor);

            // Replace %MESSAGE% placeholder
            format = format.replace(PLACEHOLDER_MESSAGE, processedMessage);

            // Send to each recipient with relational placeholders
            for (Player recipient : recipients)
            {
                String personalizedFormat = applyRelationalPlaceholders(sender, recipient, format);
                recipient.sendMessage(personalizedFormat);
            }

            // Always send to console (console should see all chat messages)
            String consoleFormat = applyRelationalPlaceholders(sender, null, format);
            Bukkit.getConsoleSender().sendMessage(consoleFormat);
        }
        finally
        {
            // Clear the chat mode placeholder override
            if (colonQuick)
            {
                FactionsChat.clearChatModePlaceholderOverride();
            }
        }
    }

    /**
     * Processes links in the message for Spigot's string-based chat system.
     * Ensures links are underlined if allowed, and re-applies the most recent color code after each link.
     *
     * @param message The message to process.
     * @param permissions The ChatPermissions object containing permission flags.
     * @param baseColor The base ChatColor to use if no color code is found.
     * @return The processed message.
     */
    private static String processLinks(String message, ChatPermissions permissions, ChatColor baseColor)
    {
        if (!permissions.allowUrl)
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

            // Find the most recent color code (including §x hex) and formatting codes in 'before'
            String colorAndFormatCodes = getLastColorCodeString(before, baseColor);

            // Underline the link if requested (using §n), then reset and re-apply the most recent color and formatting codes
            String replacement = permissions.underlineUrl ? ChatColor.UNDERLINE + url + ChatColor.RESET + colorAndFormatCodes : url + colorAndFormatCodes;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            lastEnd = matcher.end();
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Finds the last color code and formatting codes in the given text as a string for Spigot.
     * Supports modern RGB (&#RRGGBB), legacy Bukkit RGB (§x§R§R§G§G§B§B), legacy color codes (§[0-9a-fA-F]),
     * and formatting codes (§[lmnork]).
     * 
     * @param text The text to search for color codes.
     * @param baseColor The base ChatColor to use if no color code is found.
     * @return The last color code string, including any active formatting codes.
     */
    private static String getLastColorCodeString(String text, ChatColor baseColor)
    {
        String lastColorCode = null;
        int lastColorPosition = -1;
        StringBuilder activeFormattingCodes = new StringBuilder();

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

                // Convert 3-digit hex to 6-digit format
                if (hex.length() == 3)
                {
                    hex = "" + hex.charAt(0) + hex.charAt(0)
                        + hex.charAt(1) + hex.charAt(1)
                        + hex.charAt(2) + hex.charAt(2);
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
                // Legacy Bukkit format: §x§R§R§G§G§B§B
                if (rgbMatcher.end() > lastColorPosition)
                {
                    lastColorCode = rgbMatcher.group(0);
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
                        lastColorPosition = i + 2;
                    }
                    break; // We found the most recent legacy color, stop searching
                }
            }
        }

        // Now collect all active formatting codes that come after the last color code
        // Look for formatting codes starting from the last color position
        int searchStart = Math.max(0, lastColorPosition);
        for (int i = searchStart; i < text.length() - 1; i++)
        {
            if (text.charAt(i) == '§' && i + 1 < text.length())
            {
                char code = text.charAt(i + 1);
                ChatColor chatColor = ChatColor.getByChar(code);

                if (chatColor != null)
                {
                    if (chatColor.isFormat())
                    {
                        // This is a formatting code (bold, italic, underline, etc.)
                        String formatCode = "§" + code;
                        if (!activeFormattingCodes.toString().contains(formatCode))
                        {
                            activeFormattingCodes.append(formatCode);
                        }
                    }
                    else if (chatColor == ChatColor.RESET)
                    {
                        // Reset clears all formatting
                        activeFormattingCodes.setLength(0);
                    }
                    // Note: We don't process color codes here as we already found the last one above
                }
            }
        }

        // Build the final result: color code + formatting codes
        String finalColorCode = lastColorCode != null ? lastColorCode : baseColor.toString();
        return finalColorCode + activeFormattingCodes.toString();
    }
}
