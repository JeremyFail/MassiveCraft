package com.failprooftech.factionschat.util;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.testsupport.MockBukkitPlugins;
import com.failprooftech.factionschat.util.ColonChannelChatParser.ParseType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integration coverage for {@link ColonChannelChatParser}: quick-chat colon prefixes
 * with a real MockBukkit {@link Player} (permission checks + live {@link Settings}).
 *
 * <p>Tests that flip {@link Settings.QuickChat#errorOnInvalidMode} mutate global settings
 * for the duration of that method; callers elsewhere should snapshot/restore if isolation
 * matters across suites.</p>
 */
class ColonChannelChatParserTest
{
    private ServerMock server;
    private FactionsChat plugin;
    private PlayerMock player;

    /**
     * Boots MockBukkit, loads FactionsChat, and grants the default player global/local/faction
     * chat perms so quick-mode parses are not rejected for permission reasons.
     */
    @BeforeEach
    void setUp()
    {
        server = MockBukkit.mock();
        plugin = MockBukkitPlugins.loadFactionsChat();
        player = server.addPlayer();
        player.addAttachment(plugin, "factions.chat.global", true);
        player.addAttachment(plugin, "factions.chat.local", true);
        player.addAttachment(plugin, "factions.chat.faction", true);
    }

    /** Tears down the MockBukkit server (and loaded plugins) after each test. */
    @AfterEach
    void tearDown()
    {
        MockBukkit.unmock();
    }

    /** Lines without a colon quick-chat prefix stay {@link ParseType#NONE} with the body unchanged. */
    @Test
    void plainLineIsNone()
    {
        var result = ColonChannelChatParser.parse(player, "hello world");
        assertEquals(ParseType.NONE, result.getType());
        assertEquals("hello world", result.getMessageBody());
    }

    /**
     * {@code :g} plus a message body is a one-shot quick message: mode resolved, prefix stripped.
     */
    @Test
    void quickMessageStripsPrefixAndMode()
    {
        var result = ColonChannelChatParser.parse(player, ":g hello");
        assertEquals(ParseType.QUICK_MESSAGE, result.getType());
        assertEquals(ChatMode.GLOBAL, result.getTargetMode());
        assertEquals("hello", result.getMessageBody());
    }

    /** Mode-only colon input (no message body) is a channel toggle, not a quick message. */
    @Test
    void toggleWithModeOnly()
    {
        var result = ColonChannelChatParser.parse(player, ":local");
        assertEquals(ParseType.TOGGLE, result.getType());
        assertEquals(ChatMode.LOCAL, result.getTargetMode());
    }

    /**
     * Emoticon-like tokens such as {@code :)} must not be treated as mode aliases
     * (avoids false positives from bare {@code :} + non-mode characters).
     */
    @Test
    void emoticonDoesNotParseAsMode()
    {
        var result = ColonChannelChatParser.parse(player, ":)");
        assertEquals(ParseType.NONE, result.getType());
        assertEquals(":)", result.getMessageBody());
    }

    /**
     * Unknown mode names pass through as {@link ParseType#NONE} when
     * {@link Settings.QuickChat#errorOnInvalidMode} is {@code false}.
     */
    @Test
    void unknownModeIsNoneWhenErrorsDisabled()
    {
        Settings.QuickChat.errorOnInvalidMode = false;
        var result = ColonChannelChatParser.parse(player, ":notamode hi");
        assertEquals(ParseType.NONE, result.getType());
    }

    /**
     * Unknown mode names become {@link ParseType#INVALID} (no target mode) when
     * {@link Settings.QuickChat#errorOnInvalidMode} is {@code true}.
     */
    @Test
    void unknownModeIsInvalidWhenErrorsEnabled()
    {
        Settings.QuickChat.errorOnInvalidMode = true;
        var result = ColonChannelChatParser.parse(player, ":notamode hi");
        assertEquals(ParseType.INVALID, result.getType());
        assertNull(result.getTargetMode());
    }

    /**
     * A player lacking the target channel permission yields {@link ParseType#INVALID}
     * under strict invalid-mode error handling.
     */
    @Test
    void missingPermissionIsInvalidWhenStrict()
    {
        Settings.QuickChat.errorOnInvalidMode = true;
        Player noPerm = server.addPlayer();
        var result = ColonChannelChatParser.parse(noPerm, ":g hi");
        assertEquals(ParseType.INVALID, result.getType());
    }
}
