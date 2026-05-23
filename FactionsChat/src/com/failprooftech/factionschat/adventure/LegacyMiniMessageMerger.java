package com.failprooftech.factionschat.adventure;

import com.failprooftech.factionschat.adventure.PaperAdventureChatCodec.LegacyRgbPipeline;
import com.failprooftech.factionschat.chat.ChatPermissions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Locale;
import java.util.Set;

/**
 * Merges a normalized string (legacy {@code §} / hex + raw MiniMessage {@code <tags>}) into one MiniMessage source
 * string: legacy-only runs become {@link Component}s via the RGB pipeline, are serialized back to MiniMessage, and
 * are concatenated with literal tag chunks unchanged.
 *
 * <p>Player chat with {@link ChatPermissions}: use {@link #mergeAndDeserializeWithLiteralDisallowedTags(String, TextColor, LegacyRgbPipeline, MiniMessage, ChatPermissions)}
 * so tags the sender cannot use become plain-text {@link Component} slices (no backslash injection into MiniMessage).</p>
 *
 * @see <a href="https://docs.papermc.io/adventure/minimessage/format">Paper MiniMessage format</a>
 */
public final class LegacyMiniMessageMerger
{
    /**
     * Decoration + reset + newline tags (including aliases {@code em}, {@code obf}, {@code br}) per Paper docs.
     */
    private static final Set<String> MINIMESSAGE_FORMAT_TAGS = Set.of(
        "bold", "b", "italic", "i", "em", "underlined", "u", "strikethrough", "st",
        "obfuscated", "obf", "reset", "r", "newline", "br");

    /** Minecraft named color tag names (case-insensitive after extraction). */
    private static final Set<String> MINIMESSAGE_NAMED_COLORS = Set.of(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
        "gold", "gray", "grey", "dark_gray", "dark_grey", "blue", "green", "aqua",
        "red", "light_purple", "yellow", "white");

    private LegacyMiniMessageMerger()
    {
    }

    /**
     * Same walk as {@link #mergeLegacySegmentsIntoMiniMessage} but tags forbidden by {@code p} are appended as
     * literal {@link Component#text(String)} (optionally with {@code baseColor}) instead of being parsed, so players
     * see {@code <red>test} with no visible escape character.
     *
     * @param normalized         string with {@code &} already translated to {@code §}
     * @param baseColor          applied to deserialized chunks via {@code colorIfAbsent}, and to literal tag text
     * @param legacyRgbPipeline  legacy + hex pipeline for non-tag segments
     * @param miniMessage        serializer + parser (e.g. {@link MiniMessage#miniMessage()})
     * @param p                  sender permissions
     * @return concatenation of parsed and literal parts
     */
    public static Component mergeAndDeserializeWithLiteralDisallowedTags(
        String normalized,
        TextColor baseColor,
        LegacyRgbPipeline legacyRgbPipeline,
        MiniMessage miniMessage,
        ChatPermissions p)
    {
        if (normalized == null || normalized.isEmpty())
        {
            return Component.empty();
        }
        Component result = Component.empty();
        StringBuilder mmBuffer = new StringBuilder();
        int i = 0;
        final int n = normalized.length();
        while (i < n)
        {
            if (isMiniMessageTagStart(normalized, i))
            {
                int end = indexOfClosingAngleBracket(normalized, i);
                if (end > i)
                {
                    String fullTag = normalized.substring(i, end + 1);
                    // A real MiniMessage tag never contains § - if it does, the < came from plain
                    // text (e.g. a display name wrapped in format brackets like <§bNick>).
                    // Fall through to the legacy pipeline so the § codes are converted properly
                    // instead of landing raw in the MiniMessage buffer and causing ParsingExceptionImpl.
                    if (fullTag.indexOf('§') < 0)
                    {
                        if (shouldEscapeMiniMessageTag(fullTag, p))
                        {
                            result = appendDeserializedMmBuffer(result, mmBuffer, miniMessage, baseColor);
                            result = result.append(literalTagComponent(fullTag, baseColor));
                            i = end + 1;
                            continue;
                        }
                        mmBuffer.append(fullTag);
                        i = end + 1;
                        continue;
                    }
                }
            }
            int nextTag = nextPotentialTagIndex(normalized, i + 1);
            String textSeg = normalized.substring(i, nextTag);
            if (!textSeg.isEmpty())
            {
                Component legacyPart = legacyRgbPipeline.toComponent(textSeg, null);
                mmBuffer.append(miniMessage.serialize(legacyPart));
            }
            i = nextTag;
        }
        result = appendDeserializedMmBuffer(result, mmBuffer, miniMessage, baseColor);
        return result;
    }

    private static Component literalTagComponent(String fullTag, TextColor baseColor)
    {
        return baseColor != null ? Component.text(fullTag, baseColor) : Component.text(fullTag);
    }

    private static Component appendDeserializedMmBuffer(
        Component result,
        StringBuilder mmBuffer,
        MiniMessage miniMessage,
        TextColor baseColor)
    {
        if (mmBuffer.length() == 0)
        {
            return result;
        }
        Component parsed = miniMessage.deserialize(mmBuffer.toString());
        mmBuffer.setLength(0);
        return result.append(applyRootDefaultColor(parsed, baseColor));
    }

    private static Component applyRootDefaultColor(Component component, TextColor baseColor)
    {
        if (baseColor == null)
        {
            return component;
        }
        return component.colorIfAbsent(baseColor);
    }

    /**
     * Alternates between (a) verbatim MiniMessage tag regions and (b) legacy-only text. Each legacy region is
     * converted with {@link LegacyRgbPipeline#toComponent(String, net.kyori.adventure.text.format.TextColor)}
     * (base color {@code null} so upstream color tags are not overridden), then appended as serialized MiniMessage.
     *
     * @param normalized          string with {@code &} already translated to {@code §}; may mix §-codes and {@code <tags>}
     * @param legacyRgbPipeline   legacy + hex -> component pipeline
     * @param serializer          MiniMessage instance used to serialize legacy components into tag form
     * @return one concatenated MiniMessage source string safe for {@link MiniMessage#deserialize(String)}
     */
    public static String mergeLegacySegmentsIntoMiniMessage(
        String normalized,
        LegacyRgbPipeline legacyRgbPipeline,
        MiniMessage serializer)
    {
        if (normalized == null || normalized.isEmpty())
        {
            return "";
        }

        StringBuilder out = new StringBuilder();
        int i = 0;
        final int n = normalized.length();
        while (i < n)
        {
            if (isMiniMessageTagStart(normalized, i))
            {
                int end = indexOfClosingAngleBracket(normalized, i);
                if (end > i)
                {
                    String candidate = normalized.substring(i, end + 1);
                    // A real MiniMessage tag never contains § - if it does, the < came from plain
                    // text (e.g. a display name wrapped in format brackets like <§bNick>).
                    // Fall through to the legacy pipeline so the § codes are converted properly
                    // instead of landing raw in the MiniMessage buffer and causing ParsingExceptionImpl.
                    if (candidate.indexOf('§') < 0)
                    {
                        // Verbatim MiniMessage source; used for trusted / config paths without permission filtering.
                        out.append(candidate);
                        i = end + 1;
                        continue;
                    }
                }
            }
            int nextTag = nextPotentialTagIndex(normalized, i + 1);
            String textSeg = normalized.substring(i, nextTag);
            if (!textSeg.isEmpty())
            {
                // Do not pass chat base color: would recolor runs that should stay uncolored until a <yellow> etc. applies.
                Component legacyPart = legacyRgbPipeline.toComponent(textSeg, null);
                out.append(serializer.serialize(legacyPart));
            }
            i = nextTag;
        }
        return out.toString();
    }

    /**
     * Finds the next index {@code j >= from} where a (possibly escaped) MiniMessage tag begins, or {@code s.length()}
     * if none.
     *
     * @param s    full merged string
     * @param from first index to consider (often {@code i + 1} after failing to close a tag at {@code i})
     * @return index of next tag start, or end of string
     */
    private static int nextPotentialTagIndex(String s, int from)
    {
        for (int j = from; j < s.length(); j++)
        {
            if (isMiniMessageTagStart(s, j))
            {
                return j;
            }
        }
        return s.length();
    }

    /**
     * Heuristic tag opener: {@code <} followed by letter, {@code /}, {@code #}, or {@code !}, and not escaped by an
     * odd number of preceding backslashes (MiniMessage escape). So {@code \<tag>} is not a tag start.
     *
     * @param s   full string
     * @param idx index of {@code '<'}
     * @return {@code true} if parsing should treat this as the start of a MiniMessage tag
     */
    static boolean isMiniMessageTagStart(String s, int idx)
    {
        if (idx >= s.length() || s.charAt(idx) != '<')
        {
            return false;
        }
        int slashes = 0;
        for (int k = idx - 1; k >= 0 && s.charAt(k) == '\\'; k--)
        {
            slashes++;
        }
        if (slashes % 2 != 0)
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
     * Parses the leading tag name (after optional {@code /} and {@code !}) and decides whether the sender lacks
     * permission for that MiniMessage feature. Unknown tag names return {@code false} (lenient: leave for server parse).
     *
     * @param fullTag opening or self-closing tag including {@code <} and {@code >}, e.g. {@code <gradient:red:blue>}
     * @param p       sender permissions
     * @return {@code true} if this tag must not be parsed and should appear as literal angle-bracket text
     */
    private static boolean shouldEscapeMiniMessageTag(String fullTag, ChatPermissions p)
    {
        if (fullTag.length() < 2 || fullTag.charAt(0) != '<')
        {
            return false;
        }
        int pos = 1;
        if (pos < fullTag.length() && fullTag.charAt(pos) == '/')
        {
            pos++;
        }
        if (pos < fullTag.length() && fullTag.charAt(pos) == '!')
        {
            pos++;
        }
        int nameStart = pos;
        int nameEnd = nameStart;
        while (nameEnd < fullTag.length())
        {
            char c = fullTag.charAt(nameEnd);
            if (c == '>' || c == ':' || Character.isWhitespace(c))
            {
                break;
            }
            nameEnd++;
        }
        if (nameEnd == nameStart)
        {
            return false;
        }
        String name = fullTag.substring(nameStart, nameEnd).toLowerCase(Locale.ROOT);
        String low = fullTag.toLowerCase(Locale.ROOT);

        // --- Interactivity ---
        if ("click".equals(name))
        {
            if (low.contains("open_url"))
            {
                return !p.allowUrl;
            }
            return !p.allowClick;
        }
        if ("hover".equals(name))
        {
            return !p.allowHover;
        }
        if ("insert".equals(name))
        {
            return !p.allowInsert;
        }

        if ("key".equals(name))
        {
            return !p.allowKeybind;
        }

        // lang / tr / translate / *_or fallbacks - aliases per Paper docs.
        if ("lang".equals(name) || "tr".equals(name) || "translate".equals(name)
            || "lang_or".equals(name) || "tr_or".equals(name) || "translate_or".equals(name))
        {
            return !p.allowTranslatable;
        }

        // --- Multi-color / font ---
        if ("rainbow".equals(name))
        {
            if (!p.allowRainbow)
            {
                return true;
            }
            return !p.allowColor;
        }
        if ("gradient".equals(name))
        {
            if (!p.allowGradient || !p.allowColor)
            {
                return true;
            }
            return fullTag.indexOf('#') >= 0 && !p.allowRgb;
        }
        if ("transition".equals(name))
        {
            if (!p.allowTransition || !p.allowColor)
            {
                return true;
            }
            return fullTag.indexOf('#') >= 0 && !p.allowRgb;
        }

        if ("font".equals(name))
        {
            return !p.allowFont;
        }

        // --- Server-resolved components ---
        if ("selector".equals(name) || "sel".equals(name))
        {
            return !p.allowSelector;
        }
        if ("score".equals(name))
        {
            return !p.allowScore;
        }
        if ("nbt".equals(name) || "data".equals(name))
        {
            return !p.allowNbt;
        }

        if ("pride".equals(name))
        {
            if (!p.allowPride)
            {
                return true;
            }
            return !p.allowColor;
        }
        if ("sprite".equals(name))
        {
            return !p.allowSprite;
        }
        if ("head".equals(name))
        {
            return !p.allowHead;
        }

        if ("shadow".equals(name))
        {
            if (!p.allowColor)
            {
                return true;
            }
            return !p.allowRgb && fullTag.indexOf('#') >= 0;
        }

        if (name.startsWith("#"))
        {
            return !p.allowRgb;
        }
        if ("color".equals(name) || "c".equals(name) || "colour".equals(name))
        {
            if (!p.allowColor)
            {
                return true;
            }
            return !p.allowRgb && fullTag.indexOf('#') >= 0;
        }
        if (MINIMESSAGE_FORMAT_TAGS.contains(name))
        {
            if (("obfuscated".equals(name) || "obf".equals(name)) && !p.allowMagic)
            {
                return true;
            }
            return !p.allowFormat;
        }
        if (MINIMESSAGE_NAMED_COLORS.contains(name))
        {
            return !p.allowColor;
        }
        return false;
    }

    
    /**
     * Checks if the string contains a trusted MiniMessage tag.
     * @param normalized The string to check.
     * @return {@code true} if the string contains a trusted MiniMessage tag, {@code false} otherwise.
     */
    public static boolean containsTrustedMiniMessageTag(String normalized)
    {
        if (normalized == null || normalized.isEmpty())
        {
            return false;
        }
        // Check for trusted MiniMessage tags
        for (int i = 0; i < normalized.length(); i++)
        {
            if (!isMiniMessageTagStart(normalized, i))
            {
                continue;
            }
            int end = indexOfClosingAngleBracket(normalized, i);
            if (end <= i)
            {
                continue;
            }
            String candidate = normalized.substring(i, end + 1);
            if (candidate.indexOf('§') < 0)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the closing {@code >} for a tag starting at {@code openIdx}, ignoring {@code >} inside quoted argument
     * substrings ({@code '} and {@code "} toggled independently so MiniMessage-style arguments are covered).
     *
     * @param s       full string
     * @param openIdx index of the opening {@code '<'}
     * @return index of closing {@code '>'}, or {@code -1} if unterminated
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
}
