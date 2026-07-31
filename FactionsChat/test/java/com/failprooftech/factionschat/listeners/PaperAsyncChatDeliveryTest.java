package com.failprooftech.factionschat.listeners;

import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.testsupport.MockBukkitPlugins;
import com.failprooftech.factionschat.testsupport.SettingsSnapshot;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paper {@link AsyncChatEvent} delivery: cancel+plugin-message vs signed renderer, and URL/markup
 * body-transform gating (the original clickable-URL signed-path regression).
 */
class PaperAsyncChatDeliveryTest
{
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final String SENTINEL = "SIGNED_SENTINEL_BODY";

    private ServerMock server;
    private FactionsChat plugin;
    private SettingsSnapshot settingsSnapshot;
    private PaperFactionChatListener listener;
    private PlayerMock sender;
    private PlayerMock recipient;

    /**
     * Starts MockBukkit and configures a sender and recipient with global-chat access.
     */
    @BeforeEach
    void setUp()
    {
        server = MockBukkit.mock();
        plugin = MockBukkitPlugins.loadFactionsChat();
        settingsSnapshot = SettingsSnapshot.capture();

        Settings.chatFormat = "<%DISPLAYNAME%> %MESSAGE%";
        Settings.allowColorCodes = true;
        Settings.allowUrl = true;
        Settings.allowUrlUnderline = true;
        Settings.preserveUpstreamChatComponents = true;
        Settings.disableChatReporting = false;
        Settings.QuickChat.errorOnInvalidMode = false;

        listener = new PaperFactionChatListener();
        sender = server.addPlayer("Sender");
        recipient = server.addPlayer("Recipient");
        sender.addAttachment(plugin, "factions.chat.global", true);
        recipient.addAttachment(plugin, "factions.chat.global", true);
    }

    /**
     * Restores global settings before shutting down MockBukkit.
     */
    @AfterEach
    void tearDown()
    {
        if (settingsSnapshot != null)
        {
            settingsSnapshot.restore();
        }
        MockBukkit.unmock();
    }

    /**
     * Verifies that disabling chat reporting cancels Paper delivery and sends plugin-owned messages.
     */
    @Test
    void disableChatReportingCancelsAndDeliversPluginMessages()
    {
        Settings.disableChatReporting = true;
        ChatRenderer originalRenderer = ChatRenderer.defaultRenderer();
        AsyncChatEvent event = newEvent("hello world", originalRenderer);

        listener.onAsyncChat(event);

        assertTrue(event.isCancelled());
        assertSame(originalRenderer, event.renderer());
        assertNotNull(recipient.nextComponentMessage());
    }

    /**
     * Verifies that reporting-compatible delivery leaves the event active and replaces its renderer.
     */
    @Test
    void signedPathKeepsEventOpenAndInstallsCustomRenderer()
    {
        Settings.disableChatReporting = false;
        AsyncChatEvent event = newEvent("hello world");
        ChatRenderer before = event.renderer();

        listener.onAsyncChat(event);

        assertFalse(event.isCancelled());
        assertNotSame(before, event.renderer());
        assertNotSame(ChatRenderer.defaultRenderer(), event.renderer());
    }

    /**
     * Verifies that an unmodified body preserves Paper's signed message component.
     */
    @Test
    void plainBodyKeepsSignedMessageComponentWhenPreserveUpstream()
    {
        Settings.disableChatReporting = false;
        Settings.preserveUpstreamChatComponents = true;
        AsyncChatEvent event = newEvent("hello world");

        listener.onAsyncChat(event);
        // A distinct sentinel reveals whether the renderer reused Paper's signed component.
        Component rendered = renderForRecipient(event, Component.text(SENTINEL));

        assertTrue(PLAIN.serialize(rendered).contains(SENTINEL),
            "plain unsigned-safe body should wrap the signed messageComponent");
    }

    /**
     * Verifies that clickable-URL processing replaces Paper's signed message component.
     */
    @Test
    void urlInBodyOptsOutOfSignedMessageComponent()
    {
        Settings.disableChatReporting = false;
        Settings.preserveUpstreamChatComponents = true;
        Settings.allowUrl = true;
        sender.addAttachment(plugin, "factions.chat.url", true);

        String plain = "see https://example.com please";
        AsyncChatEvent event = newEvent(plain);

        listener.onAsyncChat(event);
        Component rendered = renderForRecipient(event, Component.text(SENTINEL));
        String out = PLAIN.serialize(rendered);

        assertFalse(out.contains(SENTINEL),
            "URL + allowUrl must use processed body, not the signed sentinel");
        assertTrue(out.contains("https://example.com"));
    }

    /**
     * Verifies that legacy color markup forces use of the processed message body.
     */
    @Test
    void markupInBodyOptsOutOfSignedMessageComponent()
    {
        Settings.disableChatReporting = false;
        Settings.preserveUpstreamChatComponents = true;
        sender.addAttachment(plugin, "factions.chat.color", true);

        AsyncChatEvent event = newEvent("&agreen text");

        listener.onAsyncChat(event);
        Component rendered = renderForRecipient(event, Component.text(SENTINEL));

        assertFalse(PLAIN.serialize(rendered).contains(SENTINEL),
            "legacy/MiniMessage markup must force processed body");
    }

    /**
     * Verifies that a URL cannot replace the signed body when the sender lacks URL permission.
     */
    @Test
    void urlWithoutPermissionKeepsSignedBodyWhenPreserveUpstream()
    {
        Settings.disableChatReporting = false;
        Settings.preserveUpstreamChatComponents = true;
        Settings.allowUrl = true;

        AsyncChatEvent event = newEvent("see https://example.com please");

        listener.onAsyncChat(event);
        Component rendered = renderForRecipient(event, Component.text(SENTINEL));

        assertTrue(PLAIN.serialize(rendered).contains(SENTINEL),
            "without allowUrl permission, URL alone should not transform the signed body");
    }

    /**
     * Creates an asynchronous Paper chat event using the default renderer.
     *
     * @param plain plain-text message body
     * @return event containing the configured sender and viewers
     */
    private AsyncChatEvent newEvent(String plain)
    {
        return newEvent(plain, ChatRenderer.defaultRenderer());
    }

    /**
     * Creates an asynchronous Paper chat event with a selected renderer.
     *
     * @param plain plain-text message body
     * @param renderer renderer initially attached to the event
     * @return event containing equivalent original, unsigned, and system-signed bodies
     */
    private AsyncChatEvent newEvent(String plain, ChatRenderer renderer)
    {
        Component message = Component.text(plain);
        Set<Audience> viewers = new HashSet<>();
        viewers.add(sender);
        viewers.add(recipient);
        return new AsyncChatEvent(
            true,
            sender,
            viewers,
            renderer,
            message,
            message,
            SignedMessage.system(plain, message));
    }

    /**
     * Invokes the event renderer as the configured recipient.
     *
     * @param event event whose renderer is under test
     * @param messageComponent Paper-provided signed message component
     * @return component produced for the recipient
     */
    private Component renderForRecipient(AsyncChatEvent event, Component messageComponent)
    {
        return event.renderer().render(
            sender,
            Component.text(sender.getName()),
            messageComponent,
            recipient);
    }
}
