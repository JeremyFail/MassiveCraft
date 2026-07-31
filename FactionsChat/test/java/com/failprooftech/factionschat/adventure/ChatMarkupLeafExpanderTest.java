package com.failprooftech.factionschat.adventure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the lightweight markup pre-check used to identify component leaves
 * that may require legacy or MiniMessage parsing.
 */
class ChatMarkupLeafExpanderTest
{
    /**
     * Verifies that legacy color codes and MiniMessage tags are recognized as parsable markup.
     */
    @Test
    void detectsLegacyAndMiniMessage()
    {
        assertTrue(ChatMarkupLeafExpander.mightContainParsableMarkup("&atest"));
        assertTrue(ChatMarkupLeafExpander.mightContainParsableMarkup("§bhi"));
        assertTrue(ChatMarkupLeafExpander.mightContainParsableMarkup("<green>test"));
        assertTrue(ChatMarkupLeafExpander.mightContainParsableMarkup("&atest https://test.com"));
    }

    /**
     * Verifies that plain text, bare URLs, and empty input do not trigger markup parsing.
     */
    @Test
    void plainProseIsNegative()
    {
        assertFalse(ChatMarkupLeafExpander.mightContainParsableMarkup("hello world"));
        assertFalse(ChatMarkupLeafExpander.mightContainParsableMarkup("https://test.com"));
        assertFalse(ChatMarkupLeafExpander.mightContainParsableMarkup(""));
    }
}
