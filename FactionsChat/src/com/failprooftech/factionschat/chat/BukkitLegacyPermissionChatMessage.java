package com.failprooftech.factionschat.chat;

import com.failprooftech.factionschat.util.ChatTxt;

import java.util.regex.Matcher;

/**
 * Spigot-only string pipeline for permission-aware chat (legacy {@code §} and RGB).
 */
public final class BukkitLegacyPermissionChatMessage
{
    /**
     * Private constructor to prevent instantiation.
     */
    private BukkitLegacyPermissionChatMessage()
    {
    }

    /**
     * Builds a legacy {@code §}-style string: disallowed code spans stay literal (with base color), allowed spans get
     * {@link ChatColor#translateAlternateColorCodes(char, String)} and optional RGB expansion.
     * 
     * @param raw The raw message to process.
     * @param permissions The ChatPermissions object containing permission flags.
     * @param literalLegacyPrefix § string for body tint before each literal/disallowed span; may be empty
     * @return The processed message.
     */
    public static String toBukkitLegacyString(String raw, ChatPermissions permissions, String literalLegacyPrefix)
    {
        if (raw == null || raw.isEmpty())
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        final int n = raw.length();

        // Scan left-to-right: either emit a blocked code as plain text, or hand a maximal "parseable" run to the codec.
        int i = 0;
        while (i < n)
        {
            int literalLen = ChatMessageDisallowedPrefix.disallowedPrefixLength(raw, i, permissions);
            if (literalLen > 0)
            {
                if (literalLegacyPrefix != null && !literalLegacyPrefix.isEmpty())
                {
                    sb.append(literalLegacyPrefix);
                }
                sb.append(raw, i, i + literalLen);
                i += literalLen;
                continue;
            }

            // Extend until the next index that starts a disallowed prefix (or end of string).
            int next = i;
            while (next < n && ChatMessageDisallowedPrefix.disallowedPrefixLength(raw, next, permissions) == 0)
            {
                next++;
            }
            if (next > i)
            {
                String parseable = raw.substring(i, next);
                String translated = ChatTxt.parseLegacy('&', parseable);
                sb.append(applyRgbForSpigot(translated, permissions.allowRgb));
            }
            i = next;
        }
        return sb.toString();
    }

    /**
     * Applies RGB color codes to the message.
     * 
     * @param message The message to process.
     * @param allowRgb Whether to allow RGB color codes.
     * @return The message with RGB color codes applied.
     */
    private static String applyRgbForSpigot(String message, boolean allowRgb)
    {
        if (!allowRgb)
        {
            return message;
        }

        // Process RGB color codes.
        Matcher matcher = ChatMessageDisallowedPrefix.RGB_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find())
        {
            String hex = null;
            if (matcher.group(1) != null)
            {
                hex = matcher.group(1);
            }
            else if (matcher.group(2) != null)
            {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            if (hex != null)
            {
                if (hex.length() == 3)
                {
                    hex = "" + hex.charAt(0) + hex.charAt(0)
                        + hex.charAt(1) + hex.charAt(1)
                        + hex.charAt(2) + hex.charAt(2);
                }
                StringBuilder bukkit = new StringBuilder("§x");
                for (char c : hex.toCharArray())
                {
                    bukkit.append('§').append(c);
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(bukkit.toString()));
            }
        }
        // Append the remaining unmatched part of the message.
        matcher.appendTail(sb);
        return sb.toString();
    }
}
