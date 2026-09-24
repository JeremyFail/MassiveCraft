package com.failprooftech.factionschat;

import com.failprooftech.factionschat.testsupport.MockBukkitPlugins;
import org.bukkit.permissions.PermissionAttachment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Standalone MockBukkit smoke tests for {@link FactionsChat} without a Factions soft-depend.
 * <p>
 * Verifies enablement, command registration, mode resolution fallbacks, and permission-gated
 * available modes when the Factions bridge is absent.
 */
class FactionsChatPluginLoadTest
{
    private ServerMock server;
    private FactionsChat plugin;

    /** Boots MockBukkit and loads FactionsChat in isolation (no Factions plugin). */
    @BeforeEach
    void setUp()
    {
        server = MockBukkit.mock();
        plugin = MockBukkitPlugins.loadFactionsChat();
    }

    /** Tears down the MockBukkit server after each test. */
    @AfterEach
    void tearDown()
    {
        MockBukkit.unmock();
    }

    /**
     * Plugin enables, bridge stays null, and the {@code /chat} command is registered with the
     * expected prefix.
     */
    @Test
    void enablesInStandaloneWithoutFactionsBridge()
    {
        assertTrue(plugin.isEnabled());
        assertNull(plugin.getFactionsBridge());
        assertEquals("/chat", plugin.getChatCommandPrefix());
        assertNotNull(plugin.getCommand("chat"));
    }

    /**
     * Without a bridge, faction-scoped modes collapse to {@link ChatMode#GLOBAL}; non-faction
     * modes pass through unchanged.
     */
    @Test
    void resolveEffectiveChatModeFallsBackToGlobalWithoutBridge()
    {
        assertEquals(ChatMode.GLOBAL, FactionsChat.resolveEffectiveChatMode(ChatMode.FACTION));
        assertEquals(ChatMode.LOCAL, FactionsChat.resolveEffectiveChatMode(ChatMode.LOCAL));
        assertEquals(ChatMode.GLOBAL, FactionsChat.resolveEffectiveChatMode(ChatMode.GLOBAL));
    }

    /**
     * New players default to global; writing into the plugin's mode map is reflected by
     * {@link ChatMode#getChatModeForPlayer}.
     */
    @Test
    void playerChatModeDefaultsAndStores()
    {
        PlayerMock player = server.addPlayer();
        assertEquals(ChatMode.GLOBAL, ChatMode.getChatModeForPlayer(player));

        plugin.getPlayerChatModes().put(player.getUniqueId(), ChatMode.LOCAL);
        assertEquals(ChatMode.LOCAL, ChatMode.getChatModeForPlayer(player));
    }

    /**
     * Global/local appear when permitted; faction is omitted without a bridge even if the
     * faction chat permission is granted.
     */
    @Test
    void availableModesRespectPermissionsWithoutFactionBridge()
    {
        PlayerMock player = server.addPlayer();
        PermissionAttachment att = player.addAttachment(plugin);
        att.setPermission("factions.chat.global", true);
        att.setPermission("factions.chat.local", true);
        att.setPermission("factions.chat.faction", true);

        var available = ChatMode.getAvailableChatModes(player);
        assertTrue(available.contains(ChatMode.GLOBAL));
        assertTrue(available.contains(ChatMode.LOCAL));
        // Faction channel needs bridge + membership path; without bridge it should be omitted.
        assertFalse(available.contains(ChatMode.FACTION));
    }
}
