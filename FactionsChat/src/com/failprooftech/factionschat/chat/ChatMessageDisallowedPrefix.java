package com.failprooftech.factionschat.chat;

import com.failprooftech.factionschat.listeners.FactionChatListenerBase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Literal vs parseable scan for player chat. Used by both Paper and Spigot for 
 * permission-aware chat.
 */
final class ChatMessageDisallowedPrefix
{
    /** Modern and Bukkit-legacy RGB forms; see {@link FactionChatListenerBase#RGB_REGEX}. */
    static final Pattern RGB_PATTERN = Pattern.compile(FactionChatListenerBase.RGB_REGEX);

    /**
     * Bukkit legacy hex typed with {@code &} or {@code §} before translation, e.g. {@code &x&r&r&g&g&b&b}.
     * Not matched by {@link FactionChatListenerBase#RGB_REGEX}, so we detect it separately when RGB is disallowed.
     */
    private static final Pattern LEGACY_TYPED_HEX = Pattern.compile(
        "(?i)(?:&|§)x(?:[&§][A-Fa-f0-9]){6}");

    private ChatMessageDisallowedPrefix()
    {
    }

    /**
     * If a disallowed code starts at {@code i}, returns how many characters belong to that literal run; otherwise {@code 0}
     * so callers may pass the substring through normal parsing/translation.
     */
    static int disallowedPrefixLength(String s, int i, ChatPermissions p)
    {
        final int n = s.length();
        if (i >= n)
        {
            return 0;
        }
        Matcher rgb = RGB_PATTERN.matcher(s);
        rgb.region(i, n);
        if (rgb.lookingAt())
        {
            return p.allowRgb ? 0 : (rgb.end() - i);
        }
        if (!p.allowRgb)
        {
            Matcher legacyHex = LEGACY_TYPED_HEX.matcher(s);
            legacyHex.region(i, n);
            if (legacyHex.lookingAt())
            {
                return legacyHex.end() - i;
            }
        }
        char c0 = s.charAt(i);
        if ((c0 == '&' || c0 == '§') && i + 1 < n)
        {
            char c1 = s.charAt(i + 1);
            if (c1 == '#')
            {
                return 0;
            }
            if (isMagicChar(c1) && !p.allowMagic)
            {
                return 2;
            }
            if (isClassicFormatChar(c1) && !p.allowFormat)
            {
                return 2;
            }
            if (isLegacy16ColorChar(c1) && !p.allowColor)
            {
                return 2;
            }
        }
        return 0;
    }

    private static boolean isLegacy16ColorChar(char c)
    {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isMagicChar(char c)
    {
        return c == 'k' || c == 'K';
    }

    private static boolean isClassicFormatChar(char c)
    {
        return c == 'l' || c == 'L'
            || c == 'm' || c == 'M'
            || c == 'n' || c == 'N'
            || c == 'o' || c == 'O'
            || c == 'r' || c == 'R';
    }
}
