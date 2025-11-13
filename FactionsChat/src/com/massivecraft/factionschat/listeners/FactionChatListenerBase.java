package com.massivecraft.factionschat.listeners;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.config.Settings;
import com.massivecraft.factionschat.util.InternalPlaceholders;

import me.clip.placeholderapi.PlaceholderAPI;

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
    /**
     * Regex pattern for message parsing of RGB Codes. 
     * 
     * Supports three formats:
     * 1. Modern format: &#RRGGBB or §#RRGGBB (6-digit or 3-digit hex)
     * 2. Legacy Bukkit format: §x§R§R§G§G§B§B
     */
    public static final String RGB_REGEX = "(?:(?:&|§)#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})|§x((?:§[A-Fa-f0-9]){6}))";
    /**
     * Regex pattern for URL parsing.
     * 
     * Supports http and https protocols, allows for non-ASCII characters in the URL,
     * and supports subdomains, paths, and query parameters.
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
        format = ChatColor.translateAlternateColorCodes('&', format);
        
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
            format = ChatColor.translateAlternateColorCodes('&', format);
        }
        
        return format;
    }

    /**
     * Extracts the base color from the chat format string by finding the last color code before %MESSAGE%.
     * Supports legacy color codes, modern RGB, and legacy Bukkit RGB formats.
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
        
        // Also look for legacy color codes (§[0-9a-fA-F])
        int lastLegacyColorIdx = -1;
        ChatColor legacyColor = null;
        for (int i = beforeMsg.length() - 2; i >= 0; i--)
        {
            if (beforeMsg.charAt(i) == '\u00A7' && i + 1 < beforeMsg.length())
            {
                char colorChar = beforeMsg.charAt(i + 1);
                ChatColor chatColor = ChatColor.getByChar(colorChar);
                if (chatColor != null && chatColor.isColor())
                {
                    lastLegacyColorIdx = i + 2; // Position after the color code
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
     * Strips disallowed color and formatting codes from a message.
     * 
     * @param message The message to process.
     * @param permissions The ChatPermissions object containing permission flags.
     * @return The processed message with disallowed codes removed.
     */
    protected String stripColorFormatCodes(String message, ChatPermissions permissions)
    {
        // Strip RGB codes if not allowed
        if (!permissions.allowRgb)
        {
            // Strip all RGB formats: modern (&#RRGGBB) and legacy Bukkit (§x§R§R§G§G§B§B)
            message = message.replaceAll(RGB_REGEX, "");
        }

        if (!permissions.allowColor)
        {
            // Strip legacy color codes, but avoid touching RGB hex codes if they're allowed
            if (permissions.allowRgb)
            {
                message = message.replaceAll("&(?!#)([0-9a-fA-F])", "");
            }
            else
            {
                // RGB already stripped above, safe to use simple regex
                message = message.replaceAll("&([0-9a-fA-F])", "");
            }
        }

        if (!permissions.allowFormat)
        {
            message = message.replaceAll("&([lmnorLMNOR])", "");
        }

        if (!permissions.allowMagic)
        {
            message = message.replaceAll("&([kK])", "");
        }
        
        // Final translation step
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Gets permission settings for a player's chat capabilities.
     * 
     * @param sender The player to check permissions for.
     * @return ChatPermissions object containing all permission flags.
     */
    protected ChatPermissions getPlayerChatPermissions(Player sender)
    {
        boolean settingAllowColorCodes = Settings.allowColorCodes;
        boolean settingAllowUrl = Settings.allowUrl;
        boolean settingUnderlineUrl = Settings.allowUrlUnderline;
        
        ChatPermissions perms = new ChatPermissions(
            settingAllowColorCodes && sender.hasPermission("factions.chat.color"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.format"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.magic"),
            settingAllowColorCodes && sender.hasPermission("factions.chat.rgb"),
            settingAllowUrl && sender.hasPermission("factions.chat.url"),
            settingUnderlineUrl
        );
        
        return perms;
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
        
        // Check if the recipient is ignoring the sender
        if (FactionsChat.instance.getIgnoreManager().isIgnoring(recipient.getUniqueId(), sender.getUniqueId()))
        {
            return true;
        }
        
        MPlayer mSender = MPlayer.get(sender);
        MPlayer mRecipient = MPlayer.get(recipient);
        
        return filterRecipient(chatMode, mSender, mRecipient, sender, recipient);
    }

    /**
     * Whether the recipient should be excluded from receiving the message based on the chat mode and permissions.
     * 
     * @param chatMode The chat mode being used (e.g., GLOBAL, FACTION, ALLY, etc.).
     * @param mSender The MPlayer object of the sender.
     * @param mRecipient The MPlayer object of the recipient.
     * @param sender The Player object of the sender.
     * @param recipient The Player object of the recipient.
     * 
     * @return Whether the recipient should be excluded from receiving the message.
     */
    public static boolean filterRecipient(ChatMode chatMode, MPlayer mSender, MPlayer mRecipient, Player sender, Player recipient)
    {
        // Always include recipients who have social spy enabled in Essentials
        if (FactionsChat.instance.getEssentialsPlugin() != null && 
            FactionsChat.instance.getEssentialsPlugin().getUser(recipient).isSocialSpyEnabled())
        {
            return false;
        }
        
        switch (chatMode)
        {
            case TRUCE:
                return !recipient.hasPermission("factions.chat.truce")
                        || (mSender.getRelationTo(mRecipient) != Rel.TRUCE && mSender.getRelationTo(mRecipient) != Rel.FACTION);
            case ALLY:
                return !recipient.hasPermission("factions.chat.ally")
                        || (mSender.getRelationTo(mRecipient) != Rel.ALLY && mSender.getRelationTo(mRecipient) != Rel.FACTION);
            case FACTION:
                return !recipient.hasPermission("factions.chat.faction") 
                        || mSender.getRelationTo(mRecipient) != Rel.FACTION;
            case ENEMY:
                return !recipient.hasPermission("factions.chat.enemy")
                        || (mSender.getRelationTo(mRecipient) != Rel.ENEMY && mSender.getRelationTo(mRecipient) != Rel.FACTION);
            case NEUTRAL:
                return !recipient.hasPermission("factions.chat.neutral")
                        || (mSender.getRelationTo(mRecipient) != Rel.NEUTRAL && mSender.getRelationTo(mRecipient) != Rel.FACTION);
            case LOCAL:
                return !recipient.hasPermission("factions.chat.local")
                        || sender.getLocation().toVector().subtract(recipient.getLocation().toVector()).length() > Settings.localChatRange;
            case STAFF:
                return !recipient.hasPermission("factions.chat.staff");
            case WORLD:
                return !recipient.hasPermission("factions.chat.world") 
                        || !recipient.getWorld().equals(sender.getWorld());
            default:
                return false;
        }
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
    }

    /**
     * Container class for chat permissions to avoid passing many boolean parameters.
     */
    protected static class ChatPermissions
    {
        public final boolean allowColor;
        public final boolean allowFormat;
        public final boolean allowMagic;
        public final boolean allowRgb;
        public final boolean allowUrl;
        public final boolean underlineUrl;
        
        public ChatPermissions(boolean allowColor, boolean allowFormat, boolean allowMagic, 
                             boolean allowRgb, boolean allowUrl, boolean underlineUrl)
        {
            this.allowColor = allowColor;
            this.allowFormat = allowFormat;
            this.allowMagic = allowMagic;
            this.allowRgb = allowRgb;
            this.allowUrl = allowUrl;
            this.underlineUrl = underlineUrl;
        }
    }
}
