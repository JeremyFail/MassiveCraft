package com.failprooftech.factionschat.chat;

import com.failprooftech.factionschat.config.Settings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link MiniMessageClickCommandBlacklist#findFirstBlockedPayload}.
 * <p>
 * Ensures MiniMessage {@code <click:...>} payloads that invoke dangerous commands are rejected
 * against {@link Settings#DEFAULT_BLACKLISTED_MINIMESSAGE_COMMANDS}, while harmless clicks and
 * empty inputs pass.
 */
class MiniMessageClickCommandBlacklistTest
{
    /**
     * {@code run_command:/op ...} matches the default blacklist and returns a non-null payload
     * containing an op-like token.
     */
    @Test
    void blocksRunCommandOp()
    {
        String blocked = MiniMessageClickCommandBlacklist.findFirstBlockedPayload(
            "hi <click:run_command:/op Notch>x</click>",
            Settings.DEFAULT_BLACKLISTED_MINIMESSAGE_COMMANDS);
        assertNotNull(blocked);
        assertTrueContainsOp(blocked);
    }

    /**
     * {@code suggest_command} with a blacklisted path (e.g. {@code /ban}) is also blocked.
     */
    @Test
    void blocksSuggestCommandBan()
    {
        String blocked = MiniMessageClickCommandBlacklist.findFirstBlockedPayload(
            "<click:suggest_command:'/ban Steve'>x</click>",
            Settings.DEFAULT_BLACKLISTED_MINIMESSAGE_COMMANDS);
        assertNotNull(blocked);
    }

    /**
     * Non-blacklisted click commands such as {@code /spawn} are allowed (null result).
     */
    @Test
    void allowsHarmlessClick()
    {
        assertNull(MiniMessageClickCommandBlacklist.findFirstBlockedPayload(
            "<click:run_command:/spawn>go</click>",
            Settings.DEFAULT_BLACKLISTED_MINIMESSAGE_COMMANDS));
    }

    /**
     * Null/empty message or null/empty blacklist lists are treated as safe (no blocked payload).
     */
    @Test
    void emptyInputsAreSafe()
    {
        assertNull(MiniMessageClickCommandBlacklist.findFirstBlockedPayload(null, List.of("/op")));
        assertNull(MiniMessageClickCommandBlacklist.findFirstBlockedPayload("hi", List.of()));
        assertNull(MiniMessageClickCommandBlacklist.findFirstBlockedPayload("hi", null));
    }

    /**
     * Soft assertion helper: blocked payload text should mention {@code op} case-insensitively.
     */
    private static void assertTrueContainsOp(String payload)
    {
        String lower = payload.toLowerCase();
        org.junit.jupiter.api.Assertions.assertTrue(
            lower.contains("op"),
            "expected op-like payload, got: " + payload);
    }
}
