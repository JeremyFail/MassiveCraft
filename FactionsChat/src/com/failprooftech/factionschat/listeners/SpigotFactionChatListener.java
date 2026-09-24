package com.failprooftech.factionschat.listeners;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.chat.ChatPermissions;
import com.failprooftech.factionschat.chat.BukkitLegacyPermissionChatMessage;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.util.ColonChannelChatParser;
import com.failprooftech.factionschat.util.ColonChannelChatParser.ParseType;
import com.failprooftech.factionschat.util.ChatTxt;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
            runSync(() -> sendLegacy(sender, err));
            return;
        }
        if (colon.getType() == ParseType.TOGGLE)
        {
            event.setCancelled(true);
            final ChatMode mode = colon.getTargetMode();
            runSync(() ->
            {
                FactionsChat.instance.getPlayerChatModes().put(sender.getUniqueId(), mode);
                sendLegacy(sender, ChatTxt.parse("<i>Chat mode set to: <k>" + mode.name().toLowerCase()));
            });
            return;
        }

        // Determine the chat mode and message text
        final ChatMode rawChatMode;
        final String messageText;
        final boolean colonQuick;
        if (colon.getType() == ParseType.QUICK_MESSAGE)
        {
            rawChatMode = colon.getTargetMode();
            messageText = colon.getMessageBody();
            colonQuick = true;
        }
        else
        {
            rawChatMode = ChatMode.getChatModeForPlayer(sender);
            messageText = colon.getMessageBody();
            colonQuick = false;
        }
        final ChatMode chatMode = FactionsChat.resolveEffectiveChatMode(rawChatMode);

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
        event.getRecipients().removeAll(notReceiving);
        Set<Player> recipients = new HashSet<>(event.getRecipients());

        // Cancel so vanilla does not deliver or double-log to console. DiscordSRV skips cancelled
        // chat events, so forward explicitly with the raw line (incl. colon prefixes) for channel routing.
        final String rawForDiscord = event.getMessage();
        event.setCancelled(true);
        handleChat(sender, messageText, recipients, chatMode, colonQuick);
        FactionsChat.instance.getDiscordSRVIntegration().processGameChat(sender, rawForDiscord);
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

            // Template still contains %MESSAGE% - relational placeholders and legacy & parsing must run before
            // injecting the body, or applyRelationalPlaceholders() would run ChatTxt.parseLegacy() on the full line and
            // reinterpret literal &… sequences from permission-aware processing as color codes.
            String formatTemplate = applyNonRelationalPlaceholders(sender, Settings.chatFormat, chatMode);

            BaseColorResult messageBase = extractBaseColorFromFormat(formatTemplate);
            ChatColor baseColorForLinks = messageBase.legacyColor;

            String processedMessage = BukkitLegacyPermissionChatMessage.toBukkitLegacyString(
                message, permissions, messageBase.toLegacyPrefixString());
            processedMessage = processLinks(processedMessage, permissions, baseColorForLinks);

            for (Player recipient : recipients)
            {
                String personalizedFormat = applyRelationalPlaceholders(sender, recipient, formatTemplate);
                personalizedFormat = personalizedFormat.replace(PLACEHOLDER_MESSAGE, processedMessage);
                sendLegacy(recipient, personalizedFormat, permissions.allowUrl);
            }

            // Always send to console (console should see all chat messages)
            // Strip to plain text; keep literal & if present.
            String consoleLine = applyRelationalPlaceholders(sender, null, formatTemplate).replace(PLACEHOLDER_MESSAGE, processedMessage);
            String consolePlain = stripAllLegacyForConsoleLog(consoleLine);
            Bukkit.getConsoleSender().sendMessage(consolePlain);
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
     * Sends a legacy {@code §}-coded string with no URL click attachment (notices / errors).
     *
     * @param player the recipient
     * @param legacy colored legacy string (may be null or empty)
     */
    private static void sendLegacy(Player player, String legacy)
    {
        sendLegacy(player, legacy, false);
    }

    /**
     * Sends a legacy {@code §}-coded string to a player via the Bungee chat API.
     * <p>
     * {@link TextComponent#fromLegacyText(String)} turns section-sign / hex sequences into proper chat
     * components (same approach as MassiveCore Spigot delivery). Relying on {@link Player#sendMessage(String)}
     * alone is unreliable once Adventure is on the {@code Player} type hierarchy, because plain-string
     * sends may not interpret legacy color codes.
     * <p>
     * Legacy strings cannot carry click events, so when {@code linkifyUrls} is true any {@code http(s)://}
     * spans are split out and given {@link ClickEvent.Action#OPEN_URL} after deserialization.
     *
     * @param player the recipient
     * @param legacy colored legacy string (may be null or empty)
     * @param linkifyUrls whether to attach open-URL click events to detected links
     */
    @SuppressWarnings("deprecation")
    private static void sendLegacy(Player player, String legacy, boolean linkifyUrls)
    {
        // fromLegacyText often leaves empty "extra" lists; modern Spigot rejects those.
        // Rebuild as a flat list of TextComponents (optional URL click events) with no extras.
        BaseComponent[] components = toSendableComponents(legacy != null ? legacy : "", linkifyUrls);
        player.spigot().sendMessage(components);
    }

    /**
     * Deserializes legacy text into a flat array of {@link TextComponent}s safe for modern Spigot
     * ({@code extra == null}), optionally attaching {@link ClickEvent.Action#OPEN_URL} on URL spans.
     *
     * @param legacy      section-sign / hex colored string
     * @param linkifyUrls whether to attach open-URL clicks
     * @return sendable components (never null; may be a single empty text component)
     */
    @SuppressWarnings("deprecation")
    static BaseComponent[] toSendableComponents(String legacy, boolean linkifyUrls)
    {
        BaseComponent[] raw = TextComponent.fromLegacyText(legacy != null ? legacy : "");
        List<BaseComponent> out = new ArrayList<>();
        for (BaseComponent component : raw)
        {
            flattenLegacyComponent(component, out, linkifyUrls);
        }
        if (out.isEmpty())
        {
            out.add(new TextComponent(""));
        }
        return out.toArray(new BaseComponent[0]);
    }

    /**
     * @deprecated use {@link #toSendableComponents(String, boolean)}; kept for tests that assert URL clicks
     */
    @Deprecated
    static BaseComponent[] attachUrlClickEvents(BaseComponent[] components)
    {
        if (components == null || components.length == 0)
        {
            return components != null ? components : new BaseComponent[0];
        }
        List<BaseComponent> out = new ArrayList<>();
        for (BaseComponent component : components)
        {
            flattenLegacyComponent(component, out, true);
        }
        return out.toArray(new BaseComponent[0]);
    }

    /**
     * @deprecated no longer needed when using {@link #toSendableComponents}; kept for DiscordSRV caller compatibility
     */
    @Deprecated
    static void sanitizeEmptyExtras(BaseComponent[] components)
    {
        // no-op: send path rebuilds without extras
    }

    /**
     * Flattens a Bungee component tree into sibling text pieces with no {@code extra} lists.
     *
     * @param component   source (typically from {@link TextComponent#fromLegacyText(String)})
     * @param out         destination
     * @param linkifyUrls whether to split URLs and attach open-URL clicks
     */
    private static void flattenLegacyComponent(BaseComponent component, List<BaseComponent> out, boolean linkifyUrls)
    {
        List<BaseComponent> extras = component.getExtra() != null
            ? new ArrayList<>(component.getExtra())
            : Collections.emptyList();

        if (component instanceof TextComponent)
        {
            TextComponent textComponent = (TextComponent) component;
            String text = textComponent.getText();
            if (text != null && !text.isEmpty())
            {
                if (linkifyUrls)
                {
                    Matcher matcher = URL_PATTERN.matcher(text);
                    if (matcher.find())
                    {
                        matcher.reset();
                        int lastEnd = 0;
                        while (matcher.find())
                        {
                            if (matcher.start() > lastEnd)
                            {
                                out.add(copyTextWithFormatting(textComponent, text.substring(lastEnd, matcher.start())));
                            }
                            String url = matcher.group(1);
                            TextComponent link = copyTextWithFormatting(textComponent, url);
                            link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                            out.add(link);
                            lastEnd = matcher.end();
                        }
                        if (lastEnd < text.length())
                        {
                            out.add(copyTextWithFormatting(textComponent, text.substring(lastEnd)));
                        }
                    }
                    else
                    {
                        out.add(copyTextWithFormatting(textComponent, text));
                    }
                }
                else
                {
                    out.add(copyTextWithFormatting(textComponent, text));
                }
            }
        }

        for (BaseComponent extra : extras)
        {
            flattenLegacyComponent(extra, out, linkifyUrls);
        }
    }

    /**
     * New text component with {@code text} and formatting copied from {@code styleSource}.
     * Never sets an {@code extra} list (empty lists break Spigot chat serialization).
     *
     * @param styleSource formatting donor
     * @param text        plain text content
     * @return styled text component without click/hover/extras from the donor
     */
    private static TextComponent copyTextWithFormatting(TextComponent styleSource, String text)
    {
        TextComponent copy = new TextComponent(text);
        copy.copyFormatting(styleSource);
        copy.setClickEvent(null);
        copy.setHoverEvent(null);
        // Do not call setExtra(null) - Bungee NPEs; new TextComponent starts with extra == null.
        return copy;
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
            Matcher matcher = URL_PATTERN.matcher(message);
            StringBuffer sb = new StringBuffer();
            while (matcher.find())
            {
                String url = matcher.group(1);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(url.replace('.', ' ')));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }

        Matcher matcher = URL_PATTERN.matcher(message);
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

    /**
     * Spigot console output: remove all legacy {@code §} color/format codes so logs are plain text (no intended
     * mis-parsed color for environments that strip vs. ANSI); {@link Txt#stripColorLegacy} is applied until stable.
     */
    private static String stripAllLegacyForConsoleLog(String line)
    {
        if (line == null || line.isEmpty())
        {
            return line;
        }
        String cur = line;
        String prev;
        do
        {
            prev = cur;
            cur = ChatTxt.stripColorLegacy(cur);
        }
        while (!cur.equals(prev));
        return cur;
    }
}
