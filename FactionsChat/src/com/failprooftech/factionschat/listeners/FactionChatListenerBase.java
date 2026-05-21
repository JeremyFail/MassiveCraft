package com.failprooftech.factionschat.listeners;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.chat.ChatPermissions;
import com.failprooftech.factionschat.chat.MiniMessageClickCommandBlacklist;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.util.InternalPlaceholders;
import com.failprooftech.factionschat.util.ChatTxt;

import me.clip.placeholderapi.PlaceholderAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for FactionsChat listeners that provides common functionality
 * for chat processing, color handling, and placeholder management.
 */
public abstract class FactionChatListenerBase
{
    protected static void runSync(Runnable task)
    {
        Bukkit.getScheduler().runTask(FactionsChat.instance, task);
    }

    /**
     * Regex pattern for message parsing of RGB Codes. 
     * 
     * <p>
     * Supports three formats:
     * 1. Modern format: &#RRGGBB or §#RRGGBB (6-digit or 3-digit hex)
     * 2. Legacy Bukkit format: §x§R§R§G§G§B§B
     * </p>
     */
    public static final String RGB_REGEX = "(?:(?:&|§)#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})|§x((?:§[A-Fa-f0-9]){6}))";
    /**
     * Regex pattern for URL parsing.
     * 
     * <p>
     * Supports http and https protocols, allows for non-ASCII characters in the URL,
     * and supports subdomains, paths, and query parameters.
     * </p>
     */
    public static final String URL_REGEX = "(https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[\\p{L}0-9+&@#/%=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|!:,.;]*\\.[a-zA-Z]{2,6}(?:/[-a-zA-Z0-9+&@#/%=~_|!:,.;]*[\\p{L}0-9+&@#/%=~_|!:,.;]*)*)";
    /**
     * Placeholder for the message content in chat formats.
     */
    public static final String PLACEHOLDER_MESSAGE = "%MESSAGE%";
    /**
     * Placeholder for the player's display name in chat formats.
     */
    public static final String PLACEHOLDER_DISPLAY_NAME = "%DISPLAYNAME%";

    /**
     * Applies non-relational placeholders to the chat format string.
     * This includes PlaceholderAPI placeholders, built-in placeholders, and color code translation.
     * 
     * @param sender The player sending the message.
     * @param format The chat format string.
     * @param chatMode The chat mode being used.
     * @return The format with non-relational placeholders replaced and color codes translated.
     */
    protected String applyNonRelationalPlaceholders(Player sender, String format, ChatMode chatMode)
    {
        // Replace placeholders based on whether PlaceholderAPI is enabled
        if (FactionsChat.instance.isPapiEnabled())
        {
            format = PlaceholderAPI.setPlaceholders(sender, format);
        }
        else
        {
            format = InternalPlaceholders.setPlaceholders(sender, format, chatMode);
        }
        
        // Replace general placeholders (except %MESSAGE%)
        format = format.replace(PLACEHOLDER_DISPLAY_NAME, sender.getDisplayName());
        
        // Replace legacy ampersand color codes with section sign
        format = ChatTxt.parseLegacy('&', format);
        
        return format;
    }

    /**
     * Applies relational placeholders for a specific sender-recipient pair.
     * 
     * @param sender The player sending the message.
     * @param recipient The player receiving the message (can be null for console).
     * @param format The format string to process.
     * @return The format with relational placeholders replaced.
     */
    protected String applyRelationalPlaceholders(Player sender, Player recipient, String format)
    {
        if (recipient == null)
        {
            // Strip relational placeholders - there is no relation to show for console/null viewers.
            return format.replaceAll("%rel_[^%\\s]+%", "");
        }
        if (FactionsChat.instance.isPapiEnabled())
        {
            format = PlaceholderAPI.setRelationalPlaceholders(sender, recipient, format);
        }
        else
        {
            format = InternalPlaceholders.setRelationalPlaceholders(sender, recipient, format);
        }
        
        // Translate any new ampersand codes that might have been introduced
        if (format.contains("&"))
        {
            format = ChatTxt.parseLegacy('&', format);
        }
        
        return format;
    }

    /**
     * Extracts the base color from the chat format string by finding the last color code before %MESSAGE%.
     * Supports legacy color codes, modern RGB, and legacy Bukkit RGB formats.
     *
     * <p>If the segment before {@link #PLACEHOLDER_MESSAGE} ends (ignoring trailing spaces) with {@code &r} or
     * {@code §r}, the body base is always white (e.g. {@code %factions_chat_color%} = reset for local). That runs
     * before RGB/legacy so an earlier {@code §d} (relation) or a hex false positive in the display name does not win.
     * Otherwise, a reverse scan treats trailing {@code §r} as white and can pick an earlier relation color if no
     * reset at the end.</p>
     *
     * @param format The chat format string.
     * @return BaseColorResult containing both legacy and RGB color information.
     */
    protected BaseColorResult extractBaseColorFromFormat(String format)
    {
        int msgIdx = format.indexOf("%MESSAGE%");
        if (msgIdx <= 0)
        {
            return new BaseColorResult(ChatColor.WHITE);
        }
        
        String beforeMsg = format.substring(0, msgIdx);

        // If the line ends with &r/§r immediately before %MESSAGE% (e.g. %factions_chat_color% = reset for local
        // chat), the body should read as default/white - do not use an earlier §d from relation color, or an RGB
        // match in the player name, as the effective base.
        if (endsWithResetBeforeMessagePlaceholder(beforeMsg))
        {
            return new BaseColorResult(ChatColor.WHITE);
        }
        
        // Find all RGB color codes (both modern and legacy formats)
        Pattern rgbPattern = Pattern.compile(RGB_REGEX);
        Matcher rgbMatcher = rgbPattern.matcher(beforeMsg);
        int lastRgbEnd = -1;
        String lastHexCode = null;
        
        while (rgbMatcher.find())
        {
            lastRgbEnd = rgbMatcher.end();
            
            // Check which group matched (modern vs legacy format)
            if (rgbMatcher.group(1) != null)
            {
                // Modern format: &#RRGGBB or §#RRGGBB
                lastHexCode = rgbMatcher.group(1);
            }
            else if (rgbMatcher.group(2) != null)
            {
                // Legacy Bukkit format: §x§R§R§G§G§B§B
                String legacyHex = rgbMatcher.group(2);
                // Extract hex digits from §R§R§G§G§B§B format
                StringBuilder hex = new StringBuilder();
                for (int i = 1; i < legacyHex.length(); i += 2)
                {
                    hex.append(legacyHex.charAt(i));
                }
                lastHexCode = hex.toString();
            }
        }
        
        // Last § + qualifier before MESSAGE (scan backward). §r RESET must win over earlier §d etc. from relation color.
        int lastLegacyColorIdx = -1;
        ChatColor legacyColor = null;
        for (int i = beforeMsg.length() - 2; i >= 0; i--)
        {
            if (beforeMsg.charAt(i) == '\u00A7' && i + 1 < beforeMsg.length())
            {
                // §x§R§R§G§G§B§B uses § before each hex digit; those are not legacy §5 / §a codes.
                if (isSectionInLegacyBukkitRgb(beforeMsg, i))
                {
                    continue;
                }
                char colorChar = beforeMsg.charAt(i + 1);
                ChatColor chatColor = ChatColor.getByChar(colorChar);
                if (chatColor == ChatColor.RESET)
                {
                    lastLegacyColorIdx = i + 2;
                    legacyColor = ChatColor.WHITE;
                    break;
                }
                if (chatColor != null && chatColor.isColor())
                {
                    lastLegacyColorIdx = i + 2;
                    legacyColor = chatColor;
                    break;
                }
            }
        }
        
        // Prefer RGB when it ends at or after the last legacy code (§x tail §5 must not win as §5 purple).
        if (lastRgbEnd >= lastLegacyColorIdx && lastHexCode != null)
        {
            // RGB color code is more recent
            try
            {
                // Convert 3-digit hex to 6-digit if needed
                if (lastHexCode.length() == 3)
                {
                    lastHexCode = "" + lastHexCode.charAt(0) + lastHexCode.charAt(0) + 
                                     lastHexCode.charAt(1) + lastHexCode.charAt(1) + 
                                     lastHexCode.charAt(2) + lastHexCode.charAt(2);
                }
                BaseColorResult result = new BaseColorResult(lastHexCode);
                return result;
            }
            catch (Exception e)
            {
                // Invalid hex code, fall back to legacy or default
            }
        }
        
        // Use legacy color or default
        ChatColor finalColor = legacyColor != null ? legacyColor : ChatColor.WHITE;
        return new BaseColorResult(finalColor);
    }

    /**
     * Checks if the section at the given index is a legacy Bukkit RGB color code.
     * 
     * @param text The text to check.
     * @param sectionIndex The index of the section to check.
     * @return {@code true} if the section is a legacy Bukkit RGB color code, {@code false} otherwise.
     */
    private static boolean isSectionInLegacyBukkitRgb(final String text, final int sectionIndex)
    {
        if (sectionIndex < 0 || sectionIndex >= text.length() || text.charAt(sectionIndex) != '\u00A7')
        {
            return false;
        }
        if (sectionIndex + 1 >= text.length())
        {
            return false;
        }
        final char digit = text.charAt(sectionIndex + 1);
        if (!isHexDigit(digit))
        {
            return false;
        }
        final int xStart = text.lastIndexOf("\u00A7x", sectionIndex);
        if (xStart < 0)
        {
            return false;
        }
        final int rgbStart = xStart + 2;
        final int rgbEnd = rgbStart + 12;
        if (sectionIndex < rgbStart || sectionIndex >= rgbEnd || rgbEnd > text.length())
        {
            return false;
        }
        for (int p = rgbStart; p < rgbEnd; p += 2)
        {
            if (text.charAt(p) != '\u00A7' || !isHexDigit(text.charAt(p + 1)))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the character is a valid hexadecimal digit.
     * @param c The character to check.
     * @return {@code true} if the character is a valid hexadecimal digit, {@code false} otherwise.
     */
    private static boolean isHexDigit(final char c)
    {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Checks if the string ends with a legacy reset code.
     * @param beforeMsg The string to check.
     * @return {@code true} if the string ends with a legacy reset code, {@code false} otherwise.
     * @return
     */
    private static boolean endsWithResetBeforeMessagePlaceholder(String beforeMsg)
    {
        if (beforeMsg == null)
        {
            return false;
        }
        String t = beforeMsg.stripTrailing();
        int n = t.length();
        if (n < 2)
        {
            return false;
        }
        char b = t.charAt(n - 1);
        if (b != 'r' && b != 'R')
        {
            return false;
        }
        char a = t.charAt(n - 2);
        return a == '§' || a == '&';
    }

    /** Trailing MiniMessage color/reset tags (e.g. {@code <reset>}, {@code <gold>}) used as channel tint before {@code %MESSAGE%}. */
    private static final Pattern TRAILING_MINIMESSAGE_FORMAT_SUFFIX = Pattern.compile(
        "<(?:reset|r|[a-z_]{2,}|#[0-9a-fA-F]{6}|![^>]+)>\\s*$",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern TRAILING_MODERN_RGB_SUFFIX = Pattern.compile(
        "(?:§|&)#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})\\s*$");

    private static final Pattern TRAILING_LEGACY_CODE_SUFFIX = Pattern.compile(
        "(?:§|&)([0-9a-fk-or])\\s*$",
        Pattern.CASE_INSENSITIVE);
    
    /**
     * Strips trailing legacy/RGB/MiniMessage format codes from the expanded header segment (the format
     * before {@link #PLACEHOLDER_MESSAGE}) so it can be parsed on its own for display.
     *
     * <p>On Paper's signed-chat path the header and message body are parsed separately. When
     * {@code %factions_chat_color%} sits immediately before {@code %MESSAGE%}, the expanded header ends with a
     * dangling {@code §} color (e.g. {@code > §r}) with no following text. Deserializing that tail attaches the
     * active color to the header root and recolors earlier reset spans such as literal {@code <} / {@code >}.</p>
     *
     * <p>Whitespace before the stripped channel color (e.g. {@code "> §6"} in {@code "> %factions_chat_color%})
     * is preserved so a separating space before the message body still appears in chat.</p>
     *
     * <p>Channel tint for the message body still comes from {@link #extractBaseColorFromFormat} on the full
     * pre-message segment; only the visible header parse should omit this suffix.</p>
     * @param expandedBeforeMessage expanded format before {@code %MESSAGE%} (placeholders resolved, {@code &} → {@code §})
     * @return header text safe to pass to the format codec without a trailing message-only color tail
     */
    protected static String stripTrailingFormatCodesForHeaderParse(String expandedBeforeMessage)
    {
        if (expandedBeforeMessage == null || expandedBeforeMessage.isEmpty())
        {
            return expandedBeforeMessage;
        }
        String s = expandedBeforeMessage;
        boolean stripped;
        do
        {
            stripped = false;
            if (s.isEmpty())
            {
                return s;
            }

            Matcher trailingMm = TRAILING_MINIMESSAGE_FORMAT_SUFFIX.matcher(s);
            if (trailingMm.find())
            {
                s = s.substring(0, trailingMm.start());
                stripped = true;
                continue;
            }

            int rgbSuffixLen = trailingLegacyBukkitRgbSuffixLength(s);
            if (rgbSuffixLen > 0)
            {
                s = s.substring(0, s.length() - rgbSuffixLen);
                stripped = true;
                continue;
            }

            Matcher modernRgb = TRAILING_MODERN_RGB_SUFFIX.matcher(s);
            if (modernRgb.find())
            {
                s = s.substring(0, modernRgb.start());
                stripped = true;
                continue;
            }

            Matcher legacyCode = TRAILING_LEGACY_CODE_SUFFIX.matcher(s);
            if (legacyCode.find())
            {
                char code = legacyCode.group(1).charAt(0);
                if (ChatColor.getByChar(code) != null)
                {
                    s = s.substring(0, legacyCode.start());
                    stripped = true;
                }
            }
        }
        while (stripped);
        return s;
    }

    /**
     * Calculates the length of a trailing legacy Bukkit RGB format code.
     * @param text The text to check.
     * @return The length of the trailing legacy Bukkit RGB format code.
     */
    private static int trailingLegacyBukkitRgbSuffixLength(String text)
    {
        if (text == null || text.length() < 14)
        {
            return 0;
        }
        int start = text.length() - 14;
        if (text.startsWith("\u00A7x", start) && isSectionInLegacyBukkitRgb(text, start))
        {
            return 14;
        }
        return 0;
    }
    
    /**
     * When the message contains a MiniMessage {@code <click:run_command:…>} / {@code suggest_command:…>} payload
     * matching {@link Settings#blacklistedMiniMessageCommands}, cancels processing and notifies the sender on the main thread.
     *
     * @param sender       chat sender
     * @param messageBody  text that will be parsed as the chat body (after {@code :channel} quick prefix handling)
     * @return {@code true} if the message must not be sent
     */
    protected boolean denyIfBlacklistedMiniMessageClick(Player sender, String messageBody)
    {
        if (messageBody == null || messageBody.isEmpty()
            || Settings.blacklistedMiniMessageCommands == null
            || Settings.blacklistedMiniMessageCommands.isEmpty())
        {
            return false;
        }
        if (sender.hasPermission("factions.chat.click.bypass"))
        {
            return false;
        }
        String blockedPayload = MiniMessageClickCommandBlacklist.findFirstBlockedPayload(
            messageBody, Settings.blacklistedMiniMessageCommands);
        if (blockedPayload == null)
        {
            return false;
        }
        FactionsChat.instance.getLogger().warning(
            sender.getName() + " (" + sender.getUniqueId()
                + ") attempted chat with a blacklisted MiniMessage click command. Payload: " + blockedPayload);
        runSync(() -> sender.sendMessage(Settings.MINIMESSAGE_CLICK_BLACKLIST_DENY_MESSAGE));
        return true;
    }

    /**
     * Gets the chat permissions for a player.
     * @param sender The player to get the chat permissions for.
     * @return The chat permissions for the player.
     */
    protected ChatPermissions getPlayerChatPermissions(Player sender)
    {
        boolean settingAllowColorCodes = Settings.allowColorCodes;
        boolean settingAllowUrl = Settings.allowUrl;
        boolean settingUnderlineUrl = Settings.allowUrlUnderline;
        
        return new ChatPermissions(
            settingAllowColorCodes && sender.hasPermission("factions.chat.color"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.format"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.magic"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.rgb"),
            settingAllowUrl && sender.hasPermission("factions.chat.url"),
            settingUnderlineUrl,
            sender.hasPermission("factions.chat.hover"),
            sender.hasPermission("factions.chat.click"),
            sender.hasPermission("factions.chat.insert"),
            sender.hasPermission("factions.chat.keybind"),
            sender.hasPermission("factions.chat.translatable"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.rainbow"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.gradient"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.transition"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.font"),
            sender.hasPermission("factions.chat.selector"),
            sender.hasPermission("factions.chat.score"),
            sender.hasPermission("factions.chat.nbt"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.pride"),
            sender.hasPermission("factions.chat.sprite"),
            sender.hasPermission("factions.chat.head"));
    }

    /**
     * Checks if a recipient should be excluded from receiving a message based on chat mode and social spy.
     * 
     * @param chatMode The chat mode being used.
     * @param sender The player sending the message.
     * @param recipient The potential recipient.
     * @return true if the recipient should be excluded, false if they should receive the message.
     */
    protected boolean shouldExcludeRecipient(ChatMode chatMode, Player sender, Player recipient)
    {
        if (recipient.equals(sender))
        {
            return false; // Always include the sender
        }
        
        // Check if the recipient has disabled this chat mode
        if (FactionsChat.instance.getDisabledChatManager().isChatModeDisabled(recipient.getUniqueId(), chatMode))
        {
            return true;
        }
        
        // Check if the recipient is ignoring the sender and the sender is not bypassing ignores
        if (FactionsChat.instance.getIgnoreManager().isIgnoring(recipient.getUniqueId(), sender.getUniqueId()) 
                && !sender.hasPermission("factions.chat.ignore.bypass"))
        {
            return true;
        }
        
        // Essentials social-spy recipients always see the message
        if (FactionsChat.instance.getEssentialsIntegration().isSocialSpy(recipient))
        {
            return false;
        }

        // Fallback when faction modes leak without a bridge (normally normalized to GLOBAL earlier).
        if (FactionsChat.instance.getFactionsBridge() == null && chatMode.requiresFactionData())
        {
            return true;
        }

        switch (chatMode)
        {
            case LOCAL:
                return !recipient.hasPermission("factions.chat.local")
                        || sender.getLocation().toVector().subtract(recipient.getLocation().toVector()).length() > Settings.localChatRange;
            case STAFF:
                return !recipient.hasPermission("factions.chat.staff");
            case WORLD:
                return !recipient.hasPermission("factions.chat.world")
                        || !recipient.getWorld().equals(sender.getWorld());
            default:
                break;
        }

        // Delegate faction-relation filtering to the bridge (null bridge = no faction plugin support)
        if (FactionsChat.instance.getFactionsBridge() != null)
        {
            return FactionsChat.instance.getFactionsBridge().shouldExcludeByFactionRelation(chatMode, sender, recipient);
        }

        return false;
    }

    /**
     * Represents the result of extracting a base color from a format string.
     * Contains both legacy ChatColor and hex string representations.
     */
    protected static class BaseColorResult
    {
        public final ChatColor legacyColor;
        public final String hexCode; // 6-digit hex without #
        public final boolean isRgb;

        /**
         * Constructs a new BaseColorResult with a legacy color.
         * @param legacyColor The legacy color to use.
         */
        public BaseColorResult(ChatColor legacyColor)
        {
            this.legacyColor = legacyColor;
            this.hexCode = null;
            this.isRgb = false;
        }

        /**
         * Constructs a new BaseColorResult with a hex code.
         * @param hexCode The hex code to use.
         */
        public BaseColorResult(String hexCode)
        {
            this.legacyColor = ChatColor.WHITE; // fallback
            this.hexCode = hexCode;
            this.isRgb = true;
        }

        /**
         * Gets the legacy prefix string for the base color.
         * @return The legacy prefix string for the base color.
         */
        public String toLegacyPrefixString()
        {
            if (isRgb && hexCode != null && !hexCode.isEmpty())
            {
                String hex = hexCode;
                if (hex.length() == 3)
                {
                    hex = "" + hex.charAt(0) + hex.charAt(0)
                        + hex.charAt(1) + hex.charAt(1)
                        + hex.charAt(2) + hex.charAt(2);
                }
                if (hex.length() != 6)
                {
                    return legacyColor != null ? legacyColor.toString() : "";
                }
                StringBuilder sb = new StringBuilder("§x");
                for (char c : hex.toCharArray())
                {
                    sb.append('§').append(Character.toLowerCase(c));
                }
                return sb.toString();
            }
            return legacyColor != null ? legacyColor.toString() : "";
        }
    }

}
