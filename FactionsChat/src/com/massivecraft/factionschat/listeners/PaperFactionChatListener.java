package com.massivecraft.factionschat.listeners;

import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.util.FactionsChatUtil;
import com.massivecraft.massivecore.util.MUtil;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for Paper's AsyncChatEvent and sets a custom ChatRenderer for FactionsChat.
 * 
 * This allows per-recipient, per-message formatting using PlaceholderAPI or built-in tags,
 * using the format string from the config.
 *
 * This listener is only registered if the server is running Paper.
 */
public class PaperFactionChatListener implements Listener
{
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    /**
     * Handles the AsyncChatEvent.
     * This method processes the chat message, applies the appropriate chat mode,
     * and formats the message for each recipient through the ChatRenderer interface.
     * 
     * @param event The AsyncChatEvent triggered through chat
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event)
    {
        Player sender = event.getPlayer();
        MPlayer mSender = MPlayer.get(sender);
        // Use quick chat mode if present, otherwise persistent
        final ChatMode chatMode = FactionsChat.qmPlayers.containsKey(sender.getUniqueId())
            ? FactionsChat.qmPlayers.remove(sender.getUniqueId())
            : FactionsChat.instance.getPlayerChatModes().getOrDefault(sender.getUniqueId(), ChatMode.GLOBAL);

        // Remove recipients who should not receive the message based on chat mode and permissions
        event.viewers().removeIf(aud -> 
        {
            // Non-player audiences should not be filtered
            if (aud == null || MUtil.isntPlayer(aud))
            {
                return false;
            }

            // Don't filter the sender
            Player recipient = (Player) aud;
            if (recipient.equals(sender))
            {
                return false;
            }

            // Don't filter recipients who have social spy enabled in Essentials
            if (FactionsChat.instance.getEssentialsPlugin() != null && FactionsChat.instance.getEssentialsPlugin().getUser(recipient).isSocialSpyEnabled())
            {
                return false;
            }

            // Validate permissions and chat mode and remove if necessary
            MPlayer mRecipient = MPlayer.get(recipient);
            return FactionsChatUtil.filterRecipient(chatMode, mSender, mRecipient, sender, recipient);
        });

        // Ensure sender is in the viewers set - add if not present
        event.viewers().add((Audience) sender);

        // Use a lambda to capture the chatMode for this event
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            String format = FactionsChat.instance.getConfig().getString("ChatFormat", "%factions_chat_prefix% &f<%rel_factions_relation_color%%factions_player_rankprefix%%factions_faction_name% &r%DISPLAYNAME%> %factions_chat_color%%MESSAGE%");
            String displayName = source.getDisplayName();
            String originalMessage = serializer.serialize(message);
            Player recipient = viewer == null || MUtil.isntPlayer(viewer) ? null : (Player) viewer;

            // - - - - - Color/Format Processing - - - - -
            boolean settingAllowColorCodes = FactionsChat.instance.getAllowColorCodes();
            boolean settingAllowUrl = FactionsChat.instance.getAllowUrl();
            boolean settingUnderlineUrl = FactionsChat.instance.getAllowUrlUnderline();
            boolean allowColor = settingAllowColorCodes && source.hasPermission("factions.chat.color");
            boolean allowFormat = settingAllowColorCodes && source.hasPermission("factions.chat.format");
            boolean allowMagic = settingAllowColorCodes && source.hasPermission("factions.chat.magic");
            boolean allowRgb = settingAllowColorCodes && source.hasPermission("factions.chat.rgb");
            boolean allowUrl = settingAllowUrl && source.hasPermission("factions.chat.url");

            // Replace placeholders based on whether PlaceholderAPI is enabled
            if (FactionsChat.instance.isPapiEnabled())
            {
                format = PlaceholderAPI.setPlaceholders(source, format);
                format = PlaceholderAPI.setRelationalPlaceholders(source, recipient, format);
            } 
            else
            {
                format = FactionsChatUtil.setPlaceholders(source, format, chatMode);
                format = FactionsChatUtil.setRelationalPlaceholders(source, recipient, format);
            }

            // Replace general placeholders and color codes (except %MESSAGE%)
            format = ChatColor.translateAlternateColorCodes('&', format);
            format = format.replace("%DISPLAYNAME%", displayName);

            // Extract the base color from the format (after color code translation)
            TextColor baseColor = null;
            {
                // Try to extract the color code just before %MESSAGE%
                int msgIdx = format.indexOf("%MESSAGE%");
                if (msgIdx > 0) {
                    // Look backwards for the last color code (\u00A7[0-9a-fA-F])
                    String beforeMsg = format.substring(0, msgIdx);
                    int colorIdx = beforeMsg.lastIndexOf('\u00A7');
                    if (colorIdx != -1 && colorIdx + 1 < beforeMsg.length()) {
                        char colorChar = beforeMsg.charAt(colorIdx + 1);
                        ChatColor chatColor = ChatColor.getByChar(colorChar);
                        if (chatColor != null && chatColor.isColor()) {
                            java.awt.Color awtColor = chatColor.asBungee().getColor();
                            baseColor = TextColor.color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                        }
                    }
                }
            }
            if (baseColor == null) baseColor = TextColor.color(255, 255, 255); // fallback to white

            // Remove disallowed codes and parse legacy color/format codes
            String processedMessage = processLegacyColorCodes(originalMessage, allowColor, allowFormat, allowMagic, allowRgb);

            // - - - - - RGB Color Code Processing - - - - -
            Component messageComponent;
            if (allowRgb && processedMessage.contains("&#"))
            {
                messageComponent = processRgbColorCodes(processedMessage, baseColor);
            }
            else
            {
                messageComponent = Component.text(processedMessage).color(baseColor);
            }

            // - - - - - Clickable URL Processing - - - - -
            if (allowUrl)
            {
                messageComponent = processLinks(messageComponent, settingUnderlineUrl, baseColor);
            }

            // Insert the processed message component in place of %MESSAGE%
            String[] parts = format.split("%MESSAGE%", -1);
            net.kyori.adventure.text.Component result = net.kyori.adventure.text.Component.empty();
            for (int i = 0; i < parts.length; i++)
            {
                result = result.append(serializer.deserialize(parts[i]));
                if (i < parts.length - 1)
                {
                    result = result.append(messageComponent);
                }
            }
            return result;
        });
    }

    /**
     * Processes links in the input Component and makes them clickable.
     * After each link, reapplies the most recent color (legacy or RGB) found before the link.
     *
     * @param input The input Component containing potential links
     * @param underline Whether to underline the links
     * @param baseColor The base color to apply if no color code is found
     * @return A Component with clickable links
     */
    private static Component processLinks(Component input, boolean underline, TextColor baseColor)
    {
        String urlRegex = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)";
        Pattern urlPattern = Pattern.compile(urlRegex);
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
     */
    private static TextColor getLastTextColor(String text, TextColor baseColor)
    {
        // Look for the last RGB hex code (e.g., §x§R§R§G§G§B§B)
        int idx = text.lastIndexOf("§x");
        if (idx != -1 && idx + 13 < text.length())
        {
            String hexSeq = text.substring(idx, idx + 14); // §x§R§R§G§G§B§B
            if (hexSeq.matches("§x(§[0-9a-fA-F]){6}"))
            {
                StringBuilder hex = new StringBuilder();
                for (int i = 2; i < 14; i += 2)
                {
                    hex.append(hexSeq.charAt(i + 1));
                }

                try
                {
                    return TextColor.fromHexString("#" + hex.toString());
                } 
                catch (Exception ignored) { }
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
                    java.awt.Color awtColor = chatColor.asBungee().getColor();
                    return TextColor.color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                }
            }
        }
        // Fallback to base color
        return baseColor;
    }

    /**
     * Processes RGB color codes in the format &#[hex] and converts them to TextColor components.
     *
     * @param message The message containing RGB color codes
     * @param baseColor The base color to use if no RGB code is present
     * @return A Component with the RGB colors applied
     */
    private static Component processRgbColorCodes(String message, TextColor baseColor)
    {
        String rgbRegex = "&?#([A-Fa-f0-9]{6})";
        Pattern rgbPattern = Pattern.compile(rgbRegex);
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
            // Extract the hex color code and create a TextColor
            String hex = rgbMatcher.group(1);
            currentColor = TextColor.fromHexString("#" + hex);
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
     * Processes legacy color and format codes in the message.
     * 
     * @param message The message containing legacy color codes
     * @param allowColor Whether to allow color codes
     * @param allowFormat Whether to allow format codes
     * @param allowMagic Whether to allow magic codes
     * @param allowRgb Whether to allow RGB codes
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
}
