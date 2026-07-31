package com.failprooftech.factionschat.chat;

import com.failprooftech.factionschat.testsupport.ChatPermissionsFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ChatMessageDisallowedPrefix#disallowedPrefixLength}.
 * <p>
 * The scanner decides whether a legacy/RGB markup prefix at an offset is literal (return length)
 * or left for later parsers (return {@code 0}). Shared by Paper and Spigot permission-aware chat.
 */
class ChatMessageDisallowedPrefixTest
{
    /**
     * {@code &a} is treated as a disallowed 2-char prefix when color is denied, and parseable
     * (length 0) when color is allowed.
     */
    @Test
    void colorCodeLiteralWhenColorDenied()
    {
        assertEquals(2, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            "&ahi", 0, ChatPermissionsFixtures.urlOnly(false, false)));
        assertEquals(0, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            "&ahi", 0, ChatPermissionsFixtures.colorOnly()));
    }

    /**
     * Bold ({@code &l}) requires format; magic ({@code &k}) requires magic — format alone is not enough.
     */
    @Test
    void formatAndMagicAreSeparateGates()
    {
        ChatPermissions noFormat = ChatPermissionsFixtures.colorOnly();
        assertEquals(2, ChatMessageDisallowedPrefix.disallowedPrefixLength("&lbold", 0, noFormat));
        assertEquals(2, ChatMessageDisallowedPrefix.disallowedPrefixLength("&kmagic", 0, noFormat));

        ChatPermissions formatOk = ChatPermissionsFixtures.colorFormatOnly();
        assertEquals(0, ChatMessageDisallowedPrefix.disallowedPrefixLength("&lbold", 0, formatOk));
        assertEquals(2, ChatMessageDisallowedPrefix.disallowedPrefixLength("&kmagic", 0, formatOk));
    }

    /**
     * Reset ({@code &r}) is gated by format permission, same as other format codes.
     */
    @Test
    void resetIsGatedByAllowFormat()
    {
        assertEquals(2, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            "&r", 0, ChatPermissionsFixtures.colorOnly()));
        assertEquals(0, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            "&r", 0, ChatPermissionsFixtures.colorFormatOnly()));
    }

    /**
     * Modern {@code &#rrggbb} is an 8-char disallowed prefix when RGB is denied; fully allowed
     * when all color permissions are granted.
     */
    @Test
    void modernRgbLiteralWhenRgbDenied()
    {
        String rgb = "&#ff00aatext";
        assertEquals(8, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            rgb, 0, ChatPermissionsFixtures.colorOnly()));
        assertEquals(0, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            rgb, 0, ChatPermissionsFixtures.allAllowed()));
    }

    /**
     * Typed legacy hex ({@code &x&r&r&g&g&b&b}) is a 14-char disallowed prefix when RGB is denied.
     */
    @Test
    void typedLegacyHexLiteralWhenRgbDenied()
    {
        String typed = "&x&f&f&0&0&a&atext";
        assertEquals(14, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            typed, 0, ChatPermissionsFixtures.colorOnly()));
    }

    /**
     * Bare {@code &#} / {@code §#} are always marked parseable (length 0); validity is deferred
     * to RGB_REGEX / the codec even when the remainder is malformed.
     */
    @Test
    void sectionHashAlwaysParseableMarker()
    {
        // &# / §# returns 0 even when malformed — RGB_REGEX / codec decide later.
        assertEquals(0, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            "&#", 0, ChatPermissionsFixtures.urlOnly(false, false)));
        assertEquals(0, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            "§#", 0, ChatPermissionsFixtures.urlOnly(false, false)));
    }

    /**
     * A trailing {@code &} with no code letter is not a disallowed markup prefix.
     */
    @Test
    void trailingAmpersandIsNotADisallowedPrefix()
    {
        assertEquals(0, ChatMessageDisallowedPrefix.disallowedPrefixLength(
            "ends&", 4, ChatPermissionsFixtures.urlOnly(false, false)));
    }
}
