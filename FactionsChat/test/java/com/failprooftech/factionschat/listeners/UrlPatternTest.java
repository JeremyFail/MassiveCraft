package com.failprooftech.factionschat.listeners;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Defines accepted URL forms and capture semantics for automatic link detection.
 */
class UrlPatternTest
{
    /**
     * Verifies that HTTP and HTTPS URLs with top-level domains are detected in message text.
     */
    @Test
    void matchesHttpsAndHttpWithTld()
    {
        assertTrue(FactionChatListenerBase.URL_PATTERN.matcher("https://test.com").find());
        assertTrue(FactionChatListenerBase.URL_PATTERN.matcher("http://example.org/path").find());
        assertTrue(FactionChatListenerBase.URL_PATTERN.matcher("see https://test.com please").find());
    }

    /**
     * Verifies that scheme-less hosts and unsupported protocols are not auto-linked.
     */
    @Test
    void rejectsBareHostAndWwwWithoutScheme()
    {
        assertFalse(FactionChatListenerBase.URL_PATTERN.matcher("www.example.com").find());
        assertFalse(FactionChatListenerBase.URL_PATTERN.matcher("example.com").find());
        assertFalse(FactionChatListenerBase.URL_PATTERN.matcher("ftp://example.com").find());
    }

    /**
     * Verifies that capture group one contains the complete URL for replacement logic.
     */
    @Test
    void capturingGroupIsFullUrl()
    {
        Matcher m = FactionChatListenerBase.URL_PATTERN.matcher("x https://test.com/a y");
        assertTrue(m.find());
        assertEquals("https://test.com/a", m.group(1));
    }
}
