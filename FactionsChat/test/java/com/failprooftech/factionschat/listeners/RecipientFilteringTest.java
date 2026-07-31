package com.failprooftech.factionschat.listeners;

import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.integrations.essentials.EssentialsIntegration;
import com.failprooftech.factionschat.testsupport.MockBukkitPlugins;
import com.failprooftech.factionschat.testsupport.SettingsSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises recipient inclusion and exclusion precedence across supported chat modes.
 */
class RecipientFilteringTest
{
    private ServerMock server;
    private FactionsChat plugin;
    private SettingsSnapshot settingsSnapshot;
    private SpigotFactionChatListener listener;
    private PlayerMock sender;
    private PlayerMock recipient;
    private EssentialsIntegration previousEssentials;

    /**
     * Starts MockBukkit and captures the plugin state used by recipient routing.
     *
     * @throws Exception if the Essentials integration field cannot be accessed
     */
    @BeforeEach
    void setUp() throws Exception
    {
        server = MockBukkit.mock();
        plugin = MockBukkitPlugins.loadFactionsChat();
        settingsSnapshot = SettingsSnapshot.capture();
        Settings.localChatRange = 50;

        listener = new SpigotFactionChatListener();
        sender = server.addPlayer("Sender");
        recipient = server.addPlayer("Recipient");

        Field essentials = FactionsChat.class.getDeclaredField("essentialsIntegration");
        essentials.setAccessible(true);
        previousEssentials = (EssentialsIntegration) essentials.get(plugin);
    }

    /**
     * Restores the reflected integration and global settings, then shuts down MockBukkit.
     *
     * @throws Exception if the Essentials integration field cannot be restored
     */
    @AfterEach
    void tearDown() throws Exception
    {
        if (plugin != null && previousEssentials != null)
        {
            Field essentials = FactionsChat.class.getDeclaredField("essentialsIntegration");
            essentials.setAccessible(true);
            essentials.set(plugin, previousEssentials);
        }
        if (settingsSnapshot != null)
        {
            settingsSnapshot.restore();
        }
        MockBukkit.unmock();
    }

    /**
     * Verifies that routing always retains the sender, regardless of mode permissions.
     */
    @Test
    void senderIsNeverExcluded()
    {
        assertFalse(listener.shouldExcludeRecipient(ChatMode.GLOBAL, sender, sender));
        assertFalse(listener.shouldExcludeRecipient(ChatMode.STAFF, sender, sender));
    }

    /**
     * Verifies that a recipient's disabled-mode preference takes precedence over delivery.
     */
    @Test
    void disabledChatModeExcludesRecipient()
    {
        assertTrue(plugin.getDisabledChatManager().toggleChatMode(recipient.getUniqueId(), ChatMode.GLOBAL));
        assertTrue(listener.shouldExcludeRecipient(ChatMode.GLOBAL, sender, recipient));
    }

    /**
     * Verifies ignore filtering and the sender's ignore-bypass permission.
     */
    @Test
    void ignoreExcludesUnlessSenderHasBypass()
    {
        plugin.getIgnoreManager().addIgnore(recipient.getUniqueId(), sender.getUniqueId());

        assertTrue(listener.shouldExcludeRecipient(ChatMode.GLOBAL, sender, recipient));

        sender.addAttachment(plugin, "factions.chat.ignore.bypass", true);
        assertFalse(listener.shouldExcludeRecipient(ChatMode.GLOBAL, sender, recipient));
    }

    /**
     * Verifies that social spy enables staff delivery without overriding another recipient's ignore.
     *
     * @throws Exception if the test integration cannot be injected
     */
    @Test
    void socialSpySeesMessageDespiteIgnore() throws Exception
    {
        plugin.getIgnoreManager().addIgnore(recipient.getUniqueId(), sender.getUniqueId());
        // Ignore filtering precedes social-spy routing, so the spy must be a separate recipient.
        PlayerMock spy = server.addPlayer("Spy");
        setEssentials(player -> player.equals(spy));

        assertFalse(listener.shouldExcludeRecipient(ChatMode.STAFF, sender, spy),
            "social spy should see staff chat without staff permission");
        assertTrue(listener.shouldExcludeRecipient(ChatMode.GLOBAL, sender, recipient),
            "ignore still excludes before social-spy short-circuit for that recipient");
    }

    /**
     * Verifies that social spy bypasses staff and local mode permission gates.
     *
     * @throws Exception if the test integration cannot be injected
     */
    @Test
    void socialSpyOverridesModePermissionGate() throws Exception
    {
        PlayerMock spy = server.addPlayer("Spy");
        setEssentials(player -> player.equals(spy));

        assertFalse(listener.shouldExcludeRecipient(ChatMode.STAFF, sender, spy));
        assertFalse(listener.shouldExcludeRecipient(ChatMode.LOCAL, sender, spy));
    }

    /**
     * Verifies that faction-backed modes reject recipients when no factions bridge is available.
     */
    @Test
    void factionModeExcludedWithoutBridge()
    {
        assertTrue(listener.shouldExcludeRecipient(ChatMode.FACTION, sender, recipient));
        assertTrue(listener.shouldExcludeRecipient(ChatMode.ALLY, sender, recipient));
    }

    /**
     * Verifies that local delivery requires both permission and proximity.
     */
    @Test
    void localRequiresPermissionAndRange()
    {
        recipient.addAttachment(plugin, "factions.chat.local", true);
        Settings.localChatRange = 10;

        assertFalse(listener.shouldExcludeRecipient(ChatMode.LOCAL, sender, recipient));

        World world = sender.getWorld();
        recipient.teleport(new Location(world, 100, 64, 100));
        assertTrue(listener.shouldExcludeRecipient(ChatMode.LOCAL, sender, recipient));

        PlayerMock noPerm = server.addPlayer("NoLocal");
        noPerm.teleport(sender.getLocation());
        assertTrue(listener.shouldExcludeRecipient(ChatMode.LOCAL, sender, noPerm));
    }

    /**
     * Verifies that staff delivery requires the staff-chat permission.
     */
    @Test
    void staffRequiresPermission()
    {
        assertTrue(listener.shouldExcludeRecipient(ChatMode.STAFF, sender, recipient));

        recipient.addAttachment(plugin, "factions.chat.staff", true);
        assertFalse(listener.shouldExcludeRecipient(ChatMode.STAFF, sender, recipient));
    }

    /**
     * Verifies that world delivery requires permission and a shared world.
     */
    @Test
    void worldRequiresPermissionAndSameWorld()
    {
        recipient.addAttachment(plugin, "factions.chat.world", true);
        assertFalse(listener.shouldExcludeRecipient(ChatMode.WORLD, sender, recipient));

        World other = server.addSimpleWorld("other");
        recipient.teleport(new Location(other, 0, 64, 0));
        assertTrue(listener.shouldExcludeRecipient(ChatMode.WORLD, sender, recipient));

        PlayerMock noPerm = server.addPlayer("NoWorld");
        assertTrue(listener.shouldExcludeRecipient(ChatMode.WORLD, sender, noPerm));
    }

    /**
     * Verifies that global chat accepts recipients without mode-specific permission.
     */
    @Test
    void globalAllowsAnyoneWhenNotIgnored()
    {
        assertFalse(listener.shouldExcludeRecipient(ChatMode.GLOBAL, sender, recipient));
    }

    /**
     * Replaces the plugin's private Essentials integration for routing tests.
     *
     * @param integration integration behavior to inject
     * @throws Exception if the private field cannot be accessed
     */
    private void setEssentials(EssentialsIntegration integration) throws Exception
    {
        // Production exposes no mutator for this optional integration.
        Field essentials = FactionsChat.class.getDeclaredField("essentialsIntegration");
        essentials.setAccessible(true);
        essentials.set(plugin, integration);
    }
}
