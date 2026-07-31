package com.failprooftech.factionschat.adventure;

import com.failprooftech.factionschat.chat.ChatPermissions;
import com.failprooftech.factionschat.testsupport.ChatPermissionsFixtures;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies permission-based Adventure component sanitization and action detection
 * across the supported Adventure 4 and 5 test profiles.
 */
class AdventureSanitizerAndActionCompatTest
{
    /**
     * Verifies that permitted URL actions and formatting survive sanitization.
     */
    @Test
    void keepsOpenUrlWhenAllowUrl()
    {
        Component input = Component.text("https://test.com")
            .clickEvent(ClickEvent.openUrl("https://test.com"))
            .decorate(TextDecoration.UNDERLINED);

        // URL underlining is configured separately, but explicit formatting still requires allowFormat.
        Component out = AdventureChatPermissionSanitizer.sanitize(
            input, ChatPermissionsFixtures.colorAndUrl(), NamedTextColor.WHITE);

        assertNotNull(out.clickEvent());
        assertTrue(AdventureActionCompat.isOpenUrl(out.clickEvent()));
        assertEquals(TextDecoration.State.TRUE, out.decoration(TextDecoration.UNDERLINED));
    }

    /**
     * Verifies that URL click actions are removed when URL permissions are absent.
     */
    @Test
    void stripsOpenUrlWhenDisallowed()
    {
        Component input = Component.text("https://test.com")
            .clickEvent(ClickEvent.openUrl("https://test.com"));

        Component out = AdventureChatPermissionSanitizer.sanitize(
            input, ChatPermissionsFixtures.urlOnly(false, false), NamedTextColor.WHITE);

        assertNull(out.clickEvent());
    }

    /**
     * Verifies that command click actions require the broader click permission.
     */
    @Test
    void stripsRunCommandUnlessAllowClick()
    {
        Component withClick = Component.text("x").clickEvent(ClickEvent.runCommand("/spawn"));

        assertNull(AdventureChatPermissionSanitizer.sanitize(
            withClick, ChatPermissionsFixtures.urlOnly(true, true), null).clickEvent());

        ChatPermissions allowClick = new ChatPermissions(
            false, false, false, false,
            false, false,
            false, true, false,
            false, false,
            false, false, false, false,
            false, false, false,
            false, false, false);
        assertNotNull(AdventureChatPermissionSanitizer.sanitize(
            withClick, allowClick, null).clickEvent());
    }

    /**
     * Verifies that disallowed named colors are replaced with the supplied fallback.
     */
    @Test
    void stripsNamedColorWithoutAllowColor()
    {
        Component green = Component.text("hi", NamedTextColor.GREEN);
        Component out = AdventureChatPermissionSanitizer.sanitize(
            green, ChatPermissionsFixtures.urlOnly(false, false), NamedTextColor.GRAY);
        assertEquals(NamedTextColor.GRAY, out.color());
    }

    /**
     * Verifies that permitted named colors remain unchanged.
     */
    @Test
    void keepsNamedColorWhenAllowed()
    {
        Component green = Component.text("hi", NamedTextColor.GREEN);
        Component out = AdventureChatPermissionSanitizer.sanitize(
            green, ChatPermissionsFixtures.colorAndUrl(), NamedTextColor.GRAY);
        assertEquals(NamedTextColor.GREEN, out.color());
    }

    /**
     * Verifies that retained hover text is recursively sanitized for disallowed actions.
     */
    @Test
    void sanitizesNestedHoverShowText()
    {
        Component hoverBody = Component.text("secret")
            .clickEvent(ClickEvent.openUrl("https://evil.example"));
        Component root = Component.text("hover me")
            .hoverEvent(HoverEvent.showText(hoverBody));

        // Keep the hover container while denying the nested URL action.
        ChatPermissions hoverNoUrl = new ChatPermissions(
            false, false, false, false,
            false, false,
            true, false, false,
            false, false,
            false, false, false, false,
            false, false, false,
            false, false, false);

        Component out = AdventureChatPermissionSanitizer.sanitize(root, hoverNoUrl, null);

        assertNotNull(out.hoverEvent());
        assertTrue(AdventureActionCompat.isShowText(out.hoverEvent()));
        Component nested = (Component) out.hoverEvent().value();
        assertNull(nested.clickEvent());
    }

    /**
     * Verifies compatible open-URL detection without misclassifying command actions.
     */
    @Test
    void openUrlCompatRecognizesFactoryClick()
    {
        ClickEvent click = ClickEvent.openUrl("https://test.com");
        assertTrue(AdventureActionCompat.isOpenUrl(click));
        assertFalse(AdventureActionCompat.isOpenUrl(ClickEvent.runCommand("/help")));
    }
}
