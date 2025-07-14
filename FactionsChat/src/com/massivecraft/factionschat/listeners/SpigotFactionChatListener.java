package com.massivecraft.factionschat.listeners;

import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPlayerColl;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.util.FactionsChatUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
public class SpigotFactionChatListener implements Listener
{
    /**
     * Handles the AsyncPlayerChatEvent.
     * This method processes the chat message, applies the appropriate chat mode,
     * and formats the message for each recipient.
     * 
     * @param event The AsyncPlayerChatEvent triggered through chat
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        MPlayer mSender = MPlayerColl.get().get(player);
        ChatMode chatMode = FactionsChat.qmPlayers.containsKey(player.getUniqueId()) ?
                FactionsChat.qmPlayers.remove(player.getUniqueId()) :
                FactionsChat.instance.getPlayerChatModes().getOrDefault(player.getUniqueId(), ChatMode.GLOBAL);

        Set<Player> notReceiving = new HashSet<>();
        for (Player recipient : event.getRecipients())
        {
            if (recipient.equals(player)) continue;
            
            // Skip recipients who have social spy enabled in Essentials
            if (FactionsChat.instance.getEssentialsPlugin() != null && FactionsChat.instance.getEssentialsPlugin().getUser(recipient).isSocialSpyEnabled())
            {
                continue;
            }

            MPlayer mRecipient = MPlayerColl.get().get(recipient);
            if (FactionsChatUtil.filterRecipient(chatMode, mSender, mRecipient, player, recipient))
            {
                notReceiving.add(recipient);
            }
        }
        event.getRecipients().removeAll(notReceiving);
        handleChat(player, event.getMessage(), event.getRecipients(), chatMode);

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
        String format = FactionsChat.instance.getConfig().getString("ChatFormat", "%factions_chat_prefix% &f<%rel_factions_relation_color%%factions_player_rankprefix%%factions_faction_name% &r%DISPLAYNAME%> %factions_chat_color%%MESSAGE%");
        String displayName = sender.getDisplayName();
        String originalMessage = message;

        boolean settingAllowColorCodes = FactionsChat.instance.getAllowColorCodes();
        boolean settingAllowUrl = FactionsChat.instance.getAllowUrl();
        boolean settingUnderlineUrl = FactionsChat.instance.getAllowUrlUnderline();
        boolean allowColor = settingAllowColorCodes && sender.hasPermission("factions.chat.color");
        boolean allowFormat = settingAllowColorCodes && sender.hasPermission("factions.chat.format");
        boolean allowMagic = settingAllowColorCodes && sender.hasPermission("factions.chat.magic");
        boolean allowRgb = settingAllowColorCodes && sender.hasPermission("factions.chat.rgb");
        boolean allowUrl = settingAllowUrl && sender.hasPermission("factions.chat.url");

        // Placeholder replacement and color code translation for format
        if (FactionsChat.instance.isPapiEnabled())
        {
            format = PlaceholderAPI.setPlaceholders(sender, format);
        }
        else
        {
            format = FactionsChatUtil.setPlaceholders(sender, format, chatMode);
        }
        format = format.replace("%DISPLAYNAME%", displayName);
        format = ChatColor.translateAlternateColorCodes('&', format);

        // Extract the base color from the format (after color code translation)
        ChatColor baseColor = ChatColor.WHITE;
        {
            int msgIdx = format.indexOf("%MESSAGE%");
            if (msgIdx > 0) {
                String beforeMsg = format.substring(0, msgIdx);
                int colorIdx = beforeMsg.lastIndexOf('§');
                if (colorIdx != -1 && colorIdx + 1 < beforeMsg.length()) {
                    char colorChar = beforeMsg.charAt(colorIdx + 1);
                    ChatColor chatColor = ChatColor.getByChar(colorChar);
                    if (chatColor != null && chatColor.isColor()) {
                        baseColor = chatColor;
                    }
                }
            }
        }

        // Process color/format/magic/rgb codes
        String processedMessage = processLegacyColorCodes(originalMessage, allowColor, allowFormat, allowMagic, allowRgb);
        processedMessage = processRgbColorCodes(processedMessage, allowRgb);
        // Process links (break or underline, and re-apply base color after links)
        processedMessage = processLinks(processedMessage, allowUrl, settingUnderlineUrl, baseColor);

        format = format.replace("%MESSAGE%", processedMessage);
        for (Player recipient : recipients)
        {
            String formatted = format;
            if (FactionsChat.instance.isPapiEnabled())
            {
                formatted = PlaceholderAPI.setRelationalPlaceholders(sender, recipient, format);
            }
            else
            {
                formatted = FactionsChatUtil.setRelationalPlaceholders(sender, recipient, format);
            }
            recipient.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
        }
    }

    /**
     * Processes legacy color and format codes in the message.
     * 
     * @param message The message to process
     * @param allowColor Whether to color codes are allowed
     * @param allowFormat Whether to format codes are allowed
     * @param allowMagic Whether to magic codes are allowed
     * @param allowRgb Whether to RGB codes are allowed
     * @return The processed message
     */
    private static String processLegacyColorCodes(String message, boolean allowColor, boolean allowFormat, boolean allowMagic, boolean allowRgb)
    {
        if (!allowColor)
        {
            message = message.replaceAll("&([0-9a-fA-F])", "");
        }
        if (!allowFormat)
        {
            message = message.replaceAll("&([lmnorLMNOR])", "");
        }
        if (!allowMagic)
        {
            message = message.replaceAll("&([kK])", "");
        }
        if (!allowRgb)
        {
            message = message.replaceAll("&?#([A-Fa-f0-9]{6})", "");
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Processes RGB color codes in the message.
     * 
     * @param message The message to process
     * @param allowRgb Whether to RGB codes are allowed
     * @return The processed message
     */
    private static String processRgbColorCodes(String message, boolean allowRgb)
    {
        if (!allowRgb)
        {
            return message;
        }
        // Replace all occurrences of &#RRGGBB with Bukkit's hex color format (§x§R§R§G§G§B§B)
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find())
        {
            String hex = matcher.group(1);
            StringBuilder b = new StringBuilder("§x");
            for (char c : hex.toCharArray())
            {
                b.append('§').append(c);
            }
            matcher.appendReplacement(sb, b.toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Processes links in the message.
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
        String urlRegex = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)";
        java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile(urlRegex);
        java.util.regex.Matcher matcher = urlPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        int lastEnd = 0;
        while (matcher.find())
        {
            String before = message.substring(lastEnd, matcher.start());
            String url = matcher.group(1);
            // Find the most recent color code (including §x hex) in 'before'
            String colorCode = getLastColorCode(before, baseColor);
            if (allowUrl)
            {
                // Underline the link if requested (using §n), then reset and re-apply the most recent color code
                String replacement = underline ? ChatColor.UNDERLINE + url + ChatColor.RESET + colorCode : url + colorCode;
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
            }
            else
            {
                // Remove periods to break the link
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(url.replace('.', ' ')));
            }
            lastEnd = matcher.end();
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Finds the last color code (including §x hex) in the given text, or returns the base color if none found.
     */
    private static String getLastColorCode(String text, ChatColor baseColor)
    {
        // Look for the last §x hex code
        int idx = text.lastIndexOf("§x");
        if (idx != -1 && idx + 13 <= text.length())
        {
            String hexSeq = text.substring(idx, idx + 14); // §x§R§R§G§G§B§B
            if (hexSeq.matches("§x(§[0-9a-fA-F]){6}"))
            {
                return hexSeq;
            }
        }
        // Otherwise, look for the last §[0-9a-fA-F] color code
        for (int i = text.length() - 2; i >= 0; i--)
        {
            if (text.charAt(i) == '§')
            {
                char code = text.charAt(i + 1);
                ChatColor chatColor = ChatColor.getByChar(code);
                if (chatColor != null && chatColor.isColor())
                {
                    return "§" + code;
                }
            }
        }
        // Fallback to base color
        return baseColor.toString();
    }
}
