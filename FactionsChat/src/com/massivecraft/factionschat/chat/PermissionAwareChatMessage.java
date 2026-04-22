package com.massivecraft.factionschat.chat;

import com.massivecraft.factionschat.adventure.PaperAdventureChatCodec;
import com.massivecraft.factionschat.listeners.FactionChatListenerBase;
import com.massivecraft.massivecore.util.Txt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits player chat into literal spans (codes the sender is not allowed to use stay as plain
 * text, e.g. {@code &atest}) and parseable spans (passed through {@link PaperAdventureChatCodec}
 * on Paper, or legacy translation + RGB expansion on Spigot).
 *
 * <p>Literal spans use the message base {@link TextColor} on Paper so they match the rest of the body; on Spigot
 * they are prefixed with the format base {@link ChatColor}. Disallowed MiniMessage tags are escaped inside
 * {@link PaperAdventureChatCodec} as literal text components (not parsed) where applicable.</p>
 */
public final class PermissionAwareChatMessage
{
    /** Modern and Bukkit-legacy RGB forms; see {@link FactionChatListenerBase#RGB_REGEX}. */
    private static final Pattern RGB_PATTERN = Pattern.compile(FactionChatListenerBase.RGB_REGEX);

    /**
     * Bukkit legacy hex typed with {@code &} or {@code §} before translation, e.g. {@code &x&r&r&g&g&b&b}.
     * Not matched by {@link #RGB_REGEX}, so we detect it separately when RGB is disallowed.
     */
    private static final Pattern LEGACY_TYPED_HEX = Pattern.compile(
        "(?i)(?:&|§)x(?:[&§][A-Fa-f0-9]){6}");

    private PermissionAwareChatMessage()
    {
    }

    /**
     * Builds an Adventure {@link Component} for Paper (and similar) chat: disallowed legacy/RGB snippets stay as
     * literal text; allowed runs use the unified legacy + MiniMessage pipeline.
     *
     * @param raw          the player's message as typed (may contain {@code &}, {@code §}, MiniMessage, etc.)
     * @param baseColor    default color for roots without an explicit color; applied to literal spans; may be {@code null}
     * @param permissions  what the sender may use (drives which prefixes count as literal)
     * @param legacyRgb    codec for translating a parseable substring into nested components before MiniMessage merge
     * @return a single flattened component (may be {@link Component#empty()} if {@code raw} is null or empty)
     */
    public static Component toAdventureComponent(
        String raw,
        TextColor baseColor,
        ChatPermissions permissions,
        PaperAdventureChatCodec.LegacyRgbPipeline legacyRgb)
    {
        if (raw == null || raw.isEmpty())
        {
            return Component.empty();
        }
        Component out = Component.empty();
        final int n = raw.length();
        int i = 0;
        // Scan left-to-right: either emit a blocked code as plain text, or hand a maximal "parseable" run to the codec.
        while (i < n)
        {
            int literalLen = disallowedPrefixLength(raw, i, permissions);
            if (literalLen > 0)
            {
                // Keep exact characters (important for signed chat) and tint like the normal message body.
                String lit = raw.substring(i, i + literalLen);
                out = out.append(baseColor != null ? Component.text(lit, baseColor) : Component.text(lit));
                i += literalLen;
                continue;
            }
            // Extend until the next index that starts a disallowed prefix (or end of string).
            int next = i;
            while (next < n && disallowedPrefixLength(raw, next, permissions) == 0)
            {
                next++;
            }
            if (next > i)
            {
                out = out.append(PaperAdventureChatCodec.toComponent(raw.substring(i, next), baseColor, legacyRgb, permissions));
            }
            i = next;
        }
        return out;
    }

    /**
     * Builds a legacy {@code §}-style string for Spigot chat: same literal vs parseable split as
     * {@link #toAdventureComponent(String, TextColor, ChatPermissions, PaperAdventureChatCodec.LegacyRgbPipeline)}.
     * Parseable spans get {@link ChatColor#translateAlternateColorCodes(char, String)} and optional RGB expansion.
     *
     * @param raw                     the player's message as typed
     * @param permissions             what the sender may use
     * @param messageBaseLegacyColor  color from the chat format to prefix each literal run (may be {@code null})
     * @return the processed message body, or an empty string if {@code raw} is null or empty
     */
    public static String toBukkitLegacyString(String raw, ChatPermissions permissions, ChatColor messageBaseLegacyColor)
    {
        if (raw == null || raw.isEmpty())
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        final int n = raw.length();
        int i = 0;
        while (i < n)
        {
            int literalLen = disallowedPrefixLength(raw, i, permissions);
            if (literalLen > 0)
            {
                // Re-apply base format color so literal "&c" etc. does not look like default/white vs parsed text.
                if (messageBaseLegacyColor != null)
                {
                    sb.append(messageBaseLegacyColor.toString());
                }
                sb.append(raw, i, i + literalLen);
                i += literalLen;
                continue;
            }
            int next = i;
            while (next < n && disallowedPrefixLength(raw, next, permissions) == 0)
            {
                next++;
            }
            if (next > i)
            {
                String parseable = raw.substring(i, next);
                String translated = Txt.parseLegacy('&', parseable);
                sb.append(applyRgbForSpigot(translated, permissions.allowRgb));
            }
            i = next;
        }
        return sb.toString();
    }

    /**
     * If a disallowed code starts at {@code i}, returns how many characters belong to that literal run; otherwise {@code 0}
     * so callers may pass the substring through normal parsing/translation.
     *
     * @param s the full raw message
     * @param i current index in {@code s}
     * @param p sender permissions
     * @return length of literal prefix at {@code i}, or {@code 0} if parsing may continue here
     */
    private static int disallowedPrefixLength(String s, int i, ChatPermissions p)
    {
        final int n = s.length();
        if (i >= n)
        {
            return 0;
        }
        // &#RRGGBB / §#… / §x§R§R… - full span is literal when RGB is off.
        Matcher rgb = RGB_PATTERN.matcher(s);
        rgb.region(i, n);
        if (rgb.lookingAt())
        {
            return p.allowRgb ? 0 : (rgb.end() - i);
        }
        // &x&r&r… typed form: only relevant when modern hex is disallowed (already handled above for §x variants via RGB_REGEX).
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
        // Single legacy code unit: & or § plus one qualifier (not #, which begins modern hex after translate).
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

    /**
     * @param c second character of a two-char legacy color code
     * @return true if {@code c} is a Minecraft 16-color legacy digit/letter (not reset {@code r})
     */
    private static boolean isLegacy16ColorChar(char c)
    {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * @param c character after {@code &} or {@code §}
     * @return true if {@code c} is the obfuscated (magic) code
     */
    private static boolean isMagicChar(char c)
    {
        return c == 'k' || c == 'K';
    }

    /**
     * @param c character after {@code &} or {@code §}
     * @return true if {@code c} is bold/italic/underline/strikethrough/reset per Bukkit legacy
     */
    private static boolean isClassicFormatChar(char c)
    {
        return c == 'l' || c == 'L'
            || c == 'm' || c == 'M'
            || c == 'n' || c == 'N'
            || c == 'o' || c == 'O'
            || c == 'r' || c == 'R';
    }

    /**
     * Converts {@code &#RRGGBB} / {@code §#…} segments in an already-§-normalized string into Bukkit {@code §x§R§R…} form.
     * Leaves existing §x runs untouched.
     *
     * @param message  segment that may contain RGB patterns
     * @param allowRgb if false, returns {@code message} unchanged
     * @return message with hex patterns expanded when allowed
     */
    private static String applyRgbForSpigot(String message, boolean allowRgb)
    {
        if (!allowRgb)
        {
            return message;
        }
        Matcher matcher = RGB_PATTERN.matcher(message);
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
        matcher.appendTail(sb);
        return sb.toString();
    }
}
