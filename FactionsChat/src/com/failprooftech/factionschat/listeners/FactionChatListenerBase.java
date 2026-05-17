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
        
        // Use the color code that appears last in the string
        if (lastRgbEnd > lastLegacyColorIdx && lastHexCode != null)
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
     * True if {@code beforeMsg} (text before {@link #PLACEHOLDER_MESSAGE}) ends with a legacy reset code, ignoring
     * only trailing whitespace - i.e. the last code unit before the body is {@code &r}/{@code §r}.
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
        if (FactionsChat.instance.getEssentialsPlugin() != null &&
                FactionsChat.instance.getEssentialsPlugin().getUser(recipient).isSocialSpyEnabled())
        {
            return false;
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
     * @deprecated Use {@link #shouldExcludeRecipient(ChatMode, Player, Player)} instead.
     */
    @Deprecated
    protected boolean filterRecipient(ChatMode chatMode, Player sender, Player recipient)
    {
        return shouldExcludeRecipient(chatMode, sender, recipient);
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

        public BaseColorResult(ChatColor legacyColor)
        {
            this.legacyColor = legacyColor;
            this.hexCode = null;
            this.isRgb = false;
        }

        public BaseColorResult(String hexCode)
        {
            this.legacyColor = ChatColor.WHITE; // fallback
            this.hexCode = hexCode;
            this.isRgb = true;
        }

        /**
         * § prefix to prepend before literal/disallowed spans so they match the effective base color before
         * {@link FactionChatListenerBase#PLACEHOLDER_MESSAGE} (including {@code §x} RGB when applicable).
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
