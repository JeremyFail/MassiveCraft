package com.failprooftech.factionschat.listeners;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.adventure.AdventureChatPermissionSanitizer;
import com.failprooftech.factionschat.adventure.ChatMarkupLeafExpander;
import com.failprooftech.factionschat.adventure.ComponentLeadingPlainStripper;
import com.failprooftech.factionschat.adventure.LegacyRgbMessageCodec;
import com.failprooftech.factionschat.adventure.PaperAdventureChatCodec;
import com.failprooftech.factionschat.chat.ChatPermissions;
import com.failprooftech.factionschat.chat.PermissionAwareChatMessage;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.util.ColonChannelChatParser;
import com.failprooftech.factionschat.util.ColonChannelChatParser.ParseType;
import com.failprooftech.factionschat.util.ChatTxt;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.awt.Color;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Listens for Paper's AsyncChatEvent and handles FactionsChat formatting.
 *
 * <p>When {@link Settings#disableChatReporting} is false, uses a custom {@link io.papermc.paper.chat.ChatRenderer} and
 * viewer filtering so messages stay on the signed chat path. When true, cancels the event and
 * delivers formatted chat as plugin messages, which disables chat reporting.
 * 
 * This listener is only registered if the server is running Paper.
 *
 * <p>Chat is rendered with {@link io.papermc.paper.chat.ChatRenderer} as {@code header + message body}
 * (no {@code chat.type.text} wrapper - that key is {@code <%s> %s} in en_us and adds unwanted outer
 * brackets around an already-decorated header).</p>
 *
 * <p>The message body uses upstream Adventure components when {@link Settings#preserveUpstreamChatComponents} is true:
 * plain text is still used for routing (colon channels); literal markup in text leaves is parsed like flat chat.
 * When that setting is false, the body is rebuilt only from plain text via {@link #processMessageForSender}.
 * Secure chat compares the displayed
 * message to what the client signed; re-encoding on the server can make some clients show a "message modified"
 * indicator-that tradeoff is expected if you want full formatting on the line.</p>
 *
 * <p>Format strings (prefix, channel color before {@code %MESSAGE%}) use the same unified codec.</p>
 */
public class PaperFactionChatListener extends FactionChatListenerBase implements Listener
{
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final LegacyRgbMessageCodec legacyRgbCodec = new LegacyRgbMessageCodec(serializer);
    private static final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    /**
     * Handles the AsyncChatEvent.
     * This method processes the chat message, applies the appropriate chat mode,
     * and either uses Paper's signed chat renderer or cancels and sends plugin messages
     * (see {@link Settings#disableChatReporting}).
     * 
     * @param event The AsyncChatEvent triggered through chat.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event)
    {
        Player sender = event.getPlayer();
        String plainMessage = plainSerializer.serialize(event.message());

        // Parse the chat message for colon channel prefixes
        ColonChannelChatParser.ParseResult colon = ColonChannelChatParser.parse(sender, plainMessage);
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
                sender.sendMessage(ChatTxt.parse("<i>Chat mode set to: <k>" + mode.name().toLowerCase()));
            });
            return;
        }

        // Determine the chat mode and message text
        final ChatMode chatMode;
        final String messagePlain;
        final boolean colonQuick;
        if (colon.getType() == ParseType.QUICK_MESSAGE)
        {
            chatMode = colon.getTargetMode();
            messagePlain = colon.getMessageBody();
            colonQuick = true;
        }
        else
        {
            chatMode = ChatMode.getChatModeForPlayer(sender);
            messagePlain = colon.getMessageBody();
            colonQuick = false;
        }

        if (denyIfBlacklistedMiniMessageClick(sender, messagePlain))
        {
            event.setCancelled(true);
            return;
        }

        final ChatPermissions senderPerms = getPlayerChatPermissions(sender);

        // If chat reporting is disabled, deliver the message as plugin messages
        if (Settings.disableChatReporting)
        {
            deliverLegacyCancelled(event, sender, chatMode, messagePlain, colonQuick, senderPerms);
            return;
        }

        // Deliver the message using the signed renderer
        deliverWithSignedRenderer(event, sender, chatMode, messagePlain, colonQuick, senderPerms);
    }

    private void deliverLegacyCancelled(AsyncChatEvent event, Player sender, ChatMode chatMode, String messagePlain, boolean colonQuick, ChatPermissions senderPerms)
    {
        event.setCancelled(true);

        try
        {
            // Set the chat mode placeholder override if this is a colon quick message
            if (colonQuick)
            {
                FactionsChat.setChatModePlaceholderOverride(chatMode);
            }

            // Apply general placeholders to the chat format (this is the same for all recipients)
            String preParsedFormat = applyNonRelationalPlaceholders(sender, Settings.chatFormat, chatMode);
            TextColor baseColor = getBaseColorFromFormat(preParsedFormat);

            final String plainFullLine = plainSerializer.serialize(event.message());
            Component processedMessageComponent = resolveProcessedMessageBody(
                sender, event.message(), plainFullLine, messagePlain, colonQuick, baseColor, chatMode, senderPerms);

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
                    Component finalMessage = formatMessageForRecipient(sender, preParsedFormat, processedMessageComponent, player, baseColor, chatMode);
                    audience.sendMessage(finalMessage);
                }
            }

            // Always send to console (console should see all chat messages) on the main thread
            final Component consoleMessage = formatMessageForRecipient(sender, preParsedFormat, processedMessageComponent, null, baseColor, chatMode);
            logFormattedChatToConsoleSync(consoleMessage);
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
     * Delivers chat without cancelling the event, using a {@code chat.type.text} translatable so Paper's
     * {@link io.papermc.paper.event.player.AsyncChatEvent} pipeline keeps player signing / reporting semantics.
     * Falls back to a single component tree if the config format omits {@link #PLACEHOLDER_MESSAGE}.
     *
     * @param event The AsyncChatEvent triggered through chat.
     * @param sender The player sending the message.
     * @param chatMode The chat mode being used.
     * @param messagePlain The plain text message (body only when {@code colonQuick}).
     * @param colonQuick Whether the chat message is a colon channel quick message.
     */
    private void deliverWithSignedRenderer(AsyncChatEvent event, Player sender, ChatMode chatMode, String messagePlain, boolean colonQuick, ChatPermissions senderPerms)
    {
        final String fullFormat = Settings.chatFormat;
        final int messagePlaceholderIndex = fullFormat.indexOf(PLACEHOLDER_MESSAGE);

        // When the format has no %MESSAGE%, fall back to embedding the body in a single component tree
        // (signing behavior may not match vanilla in that edge case).
        if (messagePlaceholderIndex < 0)
        {
            deliverWithSignedRendererLegacyTree(event, sender, chatMode, messagePlain, colonQuick, fullFormat, senderPerms);
            return;
        }

        final String formatBeforeMessage = fullFormat.substring(0, messagePlaceholderIndex);
        final String formatAfterMessage = fullFormat.substring(messagePlaceholderIndex + PLACEHOLDER_MESSAGE.length());

        final String preBeforeNonRel;
        final String preAfterNonRel;
        try
        {
            // Set the chat mode placeholder override if this is a colon quick message
            if (colonQuick)
            {
                FactionsChat.setChatModePlaceholderOverride(chatMode);
            }

            // Expand placeholders only for the header/suffix segments (message content is the signed argument)
            preBeforeNonRel = applyNonRelationalPlaceholders(sender, formatBeforeMessage, chatMode);
            preAfterNonRel = formatAfterMessage.isEmpty()
                ? ""
                : applyNonRelationalPlaceholders(sender, formatAfterMessage, chatMode);
        }
        finally
        {
            // Clear the chat mode placeholder override
            if (colonQuick)
            {
                FactionsChat.clearChatModePlaceholderOverride();
            }
        }

        // Base message color: last color before %MESSAGE% in the full template (placeholder keeps index valid)
        final TextColor baseColor = getBaseColorFromFormat(
            preBeforeNonRel + PLACEHOLDER_MESSAGE + formatAfterMessage);

        final String plainFullLine = plainSerializer.serialize(event.message());

        // Check if event.message() was modified by an upstream plugin (e.g. EssentialsChat parsing &codes).
        // A plain message from the client would be a single TextComponent with the full text.
        // If it has children or color, another plugin already modified it.
        final boolean upstreamModified = event.message().children().size() > 0
            || event.message().style().color() != null;

        // Determine whether the message body needs to be rebuilt from scratch (markup codes present,
        // or preserveUpstreamChatComponents is disabled). When false (plain text, upstream path), we
        // use the signed messageComponent argument from the renderer callback so Paper can preserve
        // the player's chat-signing / reporting status. When true the body is rebuilt from plain text
        // and is inherently unsigned — that's an acceptable tradeoff for colour-formatted messages.
        final boolean messageBodyTransformed = !Settings.preserveUpstreamChatComponents
            || ChatMarkupLeafExpander.mightContainParsableMarkup(messagePlain);

        final Component messageBodyFinal = resolveProcessedMessageBody(
            sender, event.message(), plainFullLine, messagePlain, colonQuick, baseColor, chatMode, senderPerms);

        // Remove recipients who should not receive the message
        event.viewers().removeIf(audience ->
        {
            if (!(audience instanceof Player))
            {
                return false;
            }
            Player recipient = (Player) audience;
            if (recipient.equals(sender))
            {
                return false;
            }
            return shouldExcludeRecipient(chatMode, sender, recipient);
        });

        final String preBeforeFinal = preBeforeNonRel;
        final String preAfterFinal = preAfterNonRel;
        final TextColor baseColorFinal = baseColor;

        event.renderer((source, sourceDisplayName, messageComponent, viewer) ->
        {
            Player recipientPlayer = viewer instanceof Player ? (Player) viewer : null;
            // Plain text: wrap the signed messageComponent with baseColor so the message
            // inherits the channel tint while keeping the signed reference intact for Paper.
            // Markup messages: use the pre-processed body (signed ref is lost, but colours work).
            Component body = messageBodyTransformed
                ? messageBodyFinal
                : (baseColorFinal != null
                    ? Component.empty().color(baseColorFinal).append(messageComponent)
                    : messageComponent);
            return buildFormattedChatLine(source, recipientPlayer, preBeforeFinal, preAfterFinal, body);
        });

        // Console receives the same line via the renderer (Console is usually a viewer); do not log again.
    }

    /**
     * Fallback when {@link Settings#chatFormat} does not contain {@link #PLACEHOLDER_MESSAGE}:
     * one component tree per viewer (signing may not match vanilla).
     */
    private void deliverWithSignedRendererLegacyTree(AsyncChatEvent event, Player sender, ChatMode chatMode, String messagePlain, boolean colonQuick, String fullFormat, ChatPermissions senderPerms)
    {
        String preParsedFormat;
        try
        {
            if (colonQuick)
            {
                FactionsChat.setChatModePlaceholderOverride(chatMode);
            }

            // Apply general placeholders to the chat format (this is the same for all recipients)
            preParsedFormat = applyNonRelationalPlaceholders(sender, fullFormat, chatMode);
        }
        finally
        {
            if (colonQuick)
            {
                FactionsChat.clearChatModePlaceholderOverride();
            }
        }

        // Extract the base color from the chat format
        final TextColor baseColor = getBaseColorFromFormat(preParsedFormat);
        final String plainFullLine = plainSerializer.serialize(event.message());
        final Component messageBodyFinal = resolveProcessedMessageBody(
            sender, event.message(), plainFullLine, messagePlain, colonQuick, baseColor, chatMode, senderPerms);

        // Remove recipients who should not receive the message
        event.viewers().removeIf(audience ->
        {
            if (!(audience instanceof Player))
            {
                return false;
            }
            Player recipient = (Player) audience;
            if (recipient.equals(sender))
            {
                return false;
            }
            return shouldExcludeRecipient(chatMode, sender, recipient);
        });

        // Set the renderer to format the message for each recipient
        final String preFinal = preParsedFormat;
        final ChatMode modeFinal = chatMode;

        // Format the message for each recipient
        event.renderer((source, sourceDisplayName, messageComponent, viewer) ->
        {
            if (viewer instanceof Player)
            {
                return formatMessageForRecipient(source, preFinal, messageBodyFinal, (Player) viewer, baseColor, modeFinal);
            }
            return formatMessageForRecipient(source, preFinal, messageBodyFinal, null, baseColor, modeFinal);
        });

        // Console uses the same renderer when present in viewers.
    }

    /**
     * Per-viewer chat line: parsed header (+ optional suffix) and message body, without extra {@code chat.type.text} brackets.
     */
    private Component buildFormattedChatLine(
        Player source,
        Player recipientOrNull,
        String preBefore,
        String preAfter,
        Component messageBody)
    {
        String headerRaw = applyRelationalPlaceholders(source, recipientOrNull, preBefore);
        Component header = formatExpandedFormatToComponent(headerRaw);
        Component line = Component.empty().append(header).append(messageBody);
        if (preAfter != null && !preAfter.isEmpty())
        {
            String afterRaw = applyRelationalPlaceholders(source, recipientOrNull, preAfter);
            line = line.append(formatExpandedFormatToComponent(afterRaw));
        }
        return line;
    }

    private void logFormattedChatToConsoleSync(Component line)
    {
        // CommandSender in this dependency set only exposes String sendMessage; use legacy text for the console.
        final String legacy = serializer.serialize(line);
        runSync(() -> Bukkit.getConsoleSender().sendMessage(legacy));
    }

    /**
     * Chat format segment (header/suffix / full template): no root default tint so {@code §r} is true white and
     * colors before {@code %MESSAGE%} stay on the serialized string (see {@link PaperAdventureChatCodec}).
     */
    private Component formatExpandedFormatToComponent(String expandedFormat)
    {
        return PaperAdventureChatCodec.toComponent(expandedFormat, null, legacyRgbCodec);
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

        // Convert legacy Bukkit named color (see BaseColorResult#legacyColor) to TextColor
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

        // Full format string -> component; root default null so prefix §r and § before %MESSAGE% behave correctly.
        Component processedFormatComponent = PaperAdventureChatCodec.toComponent(
            preParsedFormat,
            null,
            legacyRgbCodec);

        return replaceComponentPlaceholder(processedFormatComponent, PLACEHOLDER_MESSAGE, processedMessageComponent);
    }

    /**
     * Processes the message content for the sender (color codes, permissions, etc.).
     * This is done once per chat message and reused for all recipients since 
     * the message content itself doesn't change per recipient.
     * 
     * <p><b>NOTE:</b>Reset ({@code &r}, {@code §r}, {@code <reset>}) - Legacy runs use {@link com.massivecraft.factionschat.adventure.LegacyRgbMessageCodec}
     * and Adventure's legacy deserializer, where section reset is vanilla reset (following text is not the same as
     * the channel "base" {@code TextColor}; it is not force-tinted back to {@code baseColor} on each span). MiniMessage
     * {@code <r>}/{@code <reset>} segments are then merged; {@link com.massivecraft.factionschat.adventure.LegacyMiniMessageMerger} may apply
     * {@code colorIfAbsent(baseColor)} to deserialized roots. Spigot's string path therefore does not 1:1 match Paper's
     * tree here for every reset case.</p>
     * 
     * @param sender The player sending the message.
     * @param originalMessage The original message content from the player.
     * @param baseColor The base color to apply to the message.
     * @param chatMode The chat mode being used (e.g., GLOBAL, FACTION, ALLY, etc.).
     * @return A Component with the processed message ready for sending.
     */
    private Component processMessageForSender(Player sender, String originalMessage, TextColor baseColor, ChatMode chatMode, ChatPermissions permissions)
    {
        // Disallowed codes stay literal (avoids stripping & changing signed chat); allowed runs use the unified codec.
        Component messageComponent = PermissionAwareChatMessage.toAdventureComponent(
            originalMessage,
            baseColor,
            permissions,
            legacyRgbCodec);

        // - - - - - URL Processing - - - - -
        if (permissions.allowUrl)
        {
            messageComponent = processLinksInComponent(messageComponent, permissions.underlineUrl);
        }

        return AdventureChatPermissionSanitizer.sanitize(messageComponent, permissions, baseColor);
    }

    /**
     * Builds the per-line message body: either preserves {@code paperMessage} (minus a colon-prefix when {@code colonQuick})
     * and parses markup in text leaves, or flattens to plain text only when {@link Settings#preserveUpstreamChatComponents}
     * is false or the component prefix strip fails.
     *
     * <p>After leaf expansion, {@link #applyChannelBaseColorWhereAbsent(Component, TextColor)} reapplies the format-derived
     * channel {@code baseColor} on nodes that have no explicit color - matching the old all-string path where
     * {@link PermissionAwareChatMessage} always passed {@code baseColor} into the codec.</p>
     */
    private Component resolveProcessedMessageBody(
        Player sender,
        Component paperMessage,
        String plainFullLine,
        String bodyPlain,
        boolean colonQuick,
        TextColor baseColor,
        ChatMode chatMode,
        ChatPermissions senderPerms)
    {
        if (!Settings.preserveUpstreamChatComponents)
        {
            return processMessageForSender(sender, bodyPlain, baseColor, chatMode, senderPerms);
        }
        if (bodyPlain == null)
        {
            return Component.empty();
        }
        Component body = paperMessage;
        if (colonQuick)
        {
            if (!plainFullLine.endsWith(bodyPlain))
            {
                body = null;
            }
            else
            {
                String prefix = plainFullLine.substring(0, plainFullLine.length() - bodyPlain.length());
                body = ComponentLeadingPlainStripper.stripPrefix(paperMessage, prefix);
            }
        }
        if (body == null)
        {
            return processMessageForSender(sender, bodyPlain, baseColor, chatMode, senderPerms);
        }
        body = ChatMarkupLeafExpander.expand(body, baseColor, senderPerms, legacyRgbCodec);
        body = applyChannelBaseColorWhereAbsent(body, baseColor);
        if (senderPerms.allowUrl)
        {
            body = processLinksInComponent(body, senderPerms.underlineUrl);
        }
        return AdventureChatPermissionSanitizer.sanitize(body, senderPerms, baseColor);
    }

    /**
     * Applies {@link Component#colorIfAbsent(TextColor)} depth-first so unchanged upstream leaves (no markup)
     * pick up the chat-format channel tint. Parsed spans that already resolved an explicit color are left unchanged.
     *
     * @param component subtree after {@link ChatMarkupLeafExpander#expand}
     * @param baseColor color taken from the segment before {@code %MESSAGE%} in the format string; may be {@code null}
     * @return tree with default tint filled in where Adventure had no color set
     */
    private Component applyChannelBaseColorWhereAbsent(Component component, TextColor baseColor)
    {
        if (baseColor == null)
        {
            return component;
        }
        // If this node already has an explicit color, its children inherit from it —
        // stop here. Recursing would force baseColor onto colorless children that should
        // inherit their parent's color (e.g. a TextComponent inside <yellow>…</yellow>),
        // which would visually override the yellow with baseColor.
        if (component.color() != null)
        {
            return component;
        }
        Component self = component.colorIfAbsent(baseColor);
        if (self.children().isEmpty())
        {
            return self;
        }
        List<Component> mapped = self.children().stream()
            .map(c -> applyChannelBaseColorWhereAbsent(c, baseColor))
            .collect(Collectors.toList());
        return self.children(mapped);
    }

    /**
     * Processes links in a Component and makes them clickable while preserving all formatting.
     * This works directly with Components to avoid serialization issues that lose RGB color fidelity.
     *
     * @param input The input Component containing potential links.
     * @param underline Whether to underline the links.
     * @return A Component with clickable links and preserved formatting.
     */
    private Component processLinksInComponent(Component input, boolean underline)
    {
        // Recursively process all text components to find and replace URLs
        return processComponentForLinks(input, underline, 0);
    }

    /**
     * Recursively processes a Component tree to find and replace URLs with clickable links.
     * This preserves all formatting (color, bold, italic, etc.) while making URLs clickable.
     *
     * @param component The component to process.
     * @param underline Whether URLs should be underlined.
     * @param depth Current recursion depth (for logging).
     * @return A new component with URLs processed.
     */
    private Component processComponentForLinks(Component component, boolean underline, int depth)
    {
        // If this is a TextComponent, process its content for URLs
        if (component instanceof TextComponent)
        {
            TextComponent textComponent = (TextComponent) component;
            String content = textComponent.content();

            if (content != null && !content.isEmpty())
            {
                Pattern urlPattern = Pattern.compile(URL_REGEX);
                Matcher matcher = urlPattern.matcher(content);

                if (matcher.find())
                {
                    // URLs found - need to split the component
                    Component result = Component.empty();
                    int lastEnd = 0;
                    
                    // Reset matcher to process all URLs
                    matcher.reset();
                    while (matcher.find())
                    {
                        String url = matcher.group(1);
                        
                        // Add text before URL (if any)
                        if (matcher.start() > lastEnd)
                        {
                            String beforeUrl = content.substring(lastEnd, matcher.start());
                            result = result.append(Component.text(beforeUrl).style(textComponent.style()));
                        }
                        
                        // Create clickable URL component with preserved style
                        Component urlComponent = Component.text(url)
                            .style(textComponent.style())
                            .clickEvent(ClickEvent.openUrl(url));
                        
                        // Add underline if requested
                        if (underline)
                        {
                            urlComponent = urlComponent.decorate(TextDecoration.UNDERLINED);
                        }
                        
                        result = result.append(urlComponent);
                        lastEnd = matcher.end();
                    }

                    // Add any remaining text after the last URL
                    if (lastEnd < content.length())
                    {
                        String afterUrl = content.substring(lastEnd);
                        result = result.append(Component.text(afterUrl).style(textComponent.style()));
                    }
                    
                    // Process children recursively and add them to result
                    for (Component child : textComponent.children())
                    {
                        result = result.append(processComponentForLinks(child, underline, depth + 1));
                    }

                    return result;
                }
            }
        }

        // No URLs found in this TextComponent, or it's not a TextComponent
        // Process children recursively and return component with processed children
        if (component.children().isEmpty())
        {
            return component; // component with no URLs
        }
        
        // Process all children recursively
        return component.children(
            component.children().stream()
                .map(child -> processComponentForLinks(child, underline, depth + 1))
                .collect(Collectors.toList())
        );
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
        return component.children().isEmpty()
            ? component
            : component.children(component.children().stream()
                .map(child -> replaceComponentPlaceholder(child, placeholder, replacement))
                .collect(Collectors.toList()));
    }
}
