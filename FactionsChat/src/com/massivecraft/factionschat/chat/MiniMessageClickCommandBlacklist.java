package com.massivecraft.factionschat.chat;

import java.util.List;
import java.util.Locale;

/**
 * Detects MiniMessage {@code <click:run_command:…>} / {@code <click:suggest_command:…>} payloads in a raw
 * chat string and tests them against a configurable blacklist (normalized command roots).
 */
public final class MiniMessageClickCommandBlacklist
{
    private MiniMessageClickCommandBlacklist()
    {
    }

    /**
     * Finds the first blocked command payload in a player's message.
     * 
     * @param raw       player message as stored for parsing (Paper: plain-serialized line; may contain literal {@code <…>} tags)
     * @param blacklist entries such as {@code /op} or {@code ban}; leading slashes are ignored for matching
     * @return the first blacklisted command payload found (for logging), or {@code null} if none
     */
    public static String findFirstBlockedPayload(String raw, List<String> blacklist)
    {
        if (raw == null || raw.isEmpty() || blacklist == null || blacklist.isEmpty())
        {
            return null;
        }
        final int n = raw.length();
        for (int i = 0; i < n; i++)
        {
            if (!isPotentialMiniMessageOpen(raw, i))
            {
                continue;
            }
            int end = indexOfClosingAngleBracket(raw, i);
            if (end <= i)
            {
                continue;
            }
            String fullTag = raw.substring(i, end + 1);
            if (fullTag.length() < 2 || fullTag.charAt(0) != '<')
            {
                continue;
            }
            // Closing tags like </click> are not interactive.
            if (fullTag.charAt(1) == '/')
            {
                continue;
            }
            String inner = fullTag.substring(1, fullTag.length() - 1);
            if (!startsWithIgnoreCase(inner, "click:"))
            {
                continue;
            }
            String payload = extractRunOrSuggestPayload(inner);
            if (payload != null && isBlocked(payload, blacklist))
            {
                return payload.trim();
            }
        }
        return null;
    }

    /**
     * Checks if the character at the given index is a potential MiniMessage open tag.
     * 
     * @param s The string to check.
     * @param idx The index to check.
     * @return True if the character at the given index is a potential MiniMessage open tag, false otherwise.
     */
    private static boolean isPotentialMiniMessageOpen(String s, int idx)
    {
        if (s.charAt(idx) != '<')
        {
            return false;
        }
        if (idx + 1 >= s.length())
        {
            return false;
        }
        char c = s.charAt(idx + 1);
        return Character.isLetter(c) || c == '/' || c == '#' || c == '!';
    }

    /**
     * Closing {@code >} for a tag, respecting quotes inside arguments.
     * 
     * @param s The string to check.
     * @param openIdx The index of the opening angle bracket.
     * @return The index of the closing angle bracket, or -1 if not found.
     */
    private static int indexOfClosingAngleBracket(String s, int openIdx)
    {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int j = openIdx + 1; j < s.length(); j++)
        {
            char c = s.charAt(j);
            if (c == '\'' && !inDouble)
            {
                inSingle = !inSingle;
            }
            else if (c == '"' && !inSingle)
            {
                inDouble = !inDouble;
            }
            else if (c == '>' && !inSingle && !inDouble)
            {
                return j;
            }
        }
        return -1;
    }

    /**
     * Extracts the run or suggest payload from a MiniMessage tag.
     * 
     * @param innerNoBrackets The inner part of the MiniMessage tag, without the brackets.
     * @return The run or suggest payload, or null if not found.
     */
    private static String extractRunOrSuggestPayload(String innerNoBrackets)
    {
        int rc = indexOfIgnoreCase(innerNoBrackets, "run_command");
        int sc = indexOfIgnoreCase(innerNoBrackets, "suggest_command");
        int use = -1;
        int kwLen = 0;
        if (rc >= 0 && (sc < 0 || rc <= sc))
        {
            use = rc;
            kwLen = "run_command".length();
        }
        else if (sc >= 0)
        {
            use = sc;
            kwLen = "suggest_command".length();
        }
        if (use < 0)
        {
            return null;
        }
        int pos = use + kwLen;
        while (pos < innerNoBrackets.length() && Character.isWhitespace(innerNoBrackets.charAt(pos)))
        {
            pos++;
        }
        if (pos >= innerNoBrackets.length() || innerNoBrackets.charAt(pos) != ':')
        {
            return null;
        }
        pos++;
        while (pos < innerNoBrackets.length() && Character.isWhitespace(innerNoBrackets.charAt(pos)))
        {
            pos++;
        }
        if (pos >= innerNoBrackets.length())
        {
            return "";
        }
        char q = innerNoBrackets.charAt(pos);
        if (q == '\'' || q == '"')
        {
            int end = innerNoBrackets.indexOf(q, pos + 1);
            if (end < 0)
            {
                return innerNoBrackets.substring(pos + 1);
            }
            return innerNoBrackets.substring(pos + 1, end);
        }
        return innerNoBrackets.substring(pos).trim();
    }

    /**
     * Checks if a command payload is blocked.
     * 
     * @param commandPayload The command payload to check.
     * @param blacklist The blacklist to check against.
     * @return True if the command payload is blocked, false otherwise.
     */
    private static boolean isBlocked(String commandPayload, List<String> blacklist)
    {
        String trimmed = commandPayload.trim();
        if (trimmed.isEmpty())
        {
            return false;
        }
        String firstToken = trimmed.split("\\s+", 2)[0];
        String normalizedToken = normalizeCommandRoot(firstToken);
        if (normalizedToken.isEmpty())
        {
            return false;
        }
        for (String entry : blacklist)
        {
            if (entry == null || entry.isEmpty())
            {
                continue;
            }
            String entryRoot = normalizeCommandRoot(entry.trim());
            if (entryRoot.isEmpty())
            {
                continue;
            }
            if (normalizedToken.equals(entryRoot))
            {
                return true;
            }
            if (normalizedToken.contains(":"))
            {
                String afterColon = normalizedToken.substring(normalizedToken.lastIndexOf(':') + 1);
                if (afterColon.equals(entryRoot))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Lowercase, strip leading slashes so {@code /op} and {@code op} match the same root.
     * 
     * @param token The token to normalize.
     * @return The normalized token.
     */
    private static String normalizeCommandRoot(String token)
    {
        String t = token.trim().toLowerCase(Locale.ROOT);
        while (t.startsWith("/"))
        {
            t = t.substring(1);
        }
        return t;
    }

    /**
     * Finds the index of the first occurrence of the needle in the haystack, ignoring case.
     * 
     * @param haystack The haystack to search in.
     * @param needle The needle to search for.
     * @return The index of the first occurrence of the needle in the haystack, ignoring case.
     */
    private static int indexOfIgnoreCase(String haystack, String needle)
    {
        return haystack.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));
    }

    /**
     * Checks if a string starts with a given prefix, ignoring case.
     * 
     * @param s The string to check.
     * @param prefix The prefix to check for.
     * @return True if the string starts with the prefix, ignoring case.
     */
    private static boolean startsWithIgnoreCase(String s, String prefix)
    {
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
