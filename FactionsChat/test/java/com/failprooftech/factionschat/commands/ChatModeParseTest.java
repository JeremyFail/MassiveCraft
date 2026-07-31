package com.failprooftech.factionschat.commands;

import com.failprooftech.factionschat.ChatMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for chat-mode string parsing and faction-data requirements.
 * <p>
 * Covers {@link FactionsChatDispatcher#parseChatMode}, {@link ChatMode#requiresFactionData()},
 * and the case-sensitive {@link ChatMode#getChatModeByName} lookup map.
 */
class ChatModeParseTest
{
    /**
     * Dispatcher accepts full names, single-letter aliases, and legacy {@code public}/{@code p}
     * for global; lookup is case-insensitive for these tokens.
     */
    @Test
    void parseChatModeAcceptsNameAliasAndPublicLegacy()
    {
        assertEquals(ChatMode.GLOBAL, FactionsChatDispatcher.parseChatMode("global"));
        assertEquals(ChatMode.GLOBAL, FactionsChatDispatcher.parseChatMode("g"));
        assertEquals(ChatMode.GLOBAL, FactionsChatDispatcher.parseChatMode("public"));
        assertEquals(ChatMode.GLOBAL, FactionsChatDispatcher.parseChatMode("p"));
        assertEquals(ChatMode.FACTION, FactionsChatDispatcher.parseChatMode("f"));
        assertEquals(ChatMode.LOCAL, FactionsChatDispatcher.parseChatMode("LOCAL"));
    }

    /**
     * Null, empty, and unrecognized tokens return {@code null}.
     */
    @Test
    void parseChatModeRejectsUnknown()
    {
        assertNull(FactionsChatDispatcher.parseChatMode(null));
        assertNull(FactionsChatDispatcher.parseChatMode(""));
        assertNull(FactionsChatDispatcher.parseChatMode("nope"));
    }

    /**
     * Faction and ally modes require faction data; global, local, and staff do not.
     */
    @Test
    void factionModesRequireFactionData()
    {
        assertTrue(ChatMode.FACTION.requiresFactionData());
        assertTrue(ChatMode.ALLY.requiresFactionData());
        assertTrue(!ChatMode.GLOBAL.requiresFactionData());
        assertTrue(!ChatMode.LOCAL.requiresFactionData());
        assertTrue(!ChatMode.STAFF.requiresFactionData());
    }

    /**
     * {@link ChatMode#getChatModeByName} keys are {@code enum.name()} (uppercase) and single-letter
     * aliases only — lowercase full names are rejected.
     */
    @Test
    void getChatModeByName()
    {
        // BY_NAME keys are enum name() (uppercase) and single-letter aliases
        assertEquals(ChatMode.STAFF, ChatMode.getChatModeByName("STAFF"));
        assertEquals(ChatMode.STAFF, ChatMode.getChatModeByName("S"));
        assertNull(ChatMode.getChatModeByName("staff"));
        assertNull(ChatMode.getChatModeByName("missing"));
    }
}
