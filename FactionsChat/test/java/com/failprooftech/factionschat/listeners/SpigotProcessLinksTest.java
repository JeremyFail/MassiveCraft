package com.failprooftech.factionschat.listeners;

import com.failprooftech.factionschat.chat.ChatPermissions;
import com.failprooftech.factionschat.testsupport.ChatPermissionsFixtures;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers Spigot's string-based URL processing and legacy style restoration.
 */
class SpigotProcessLinksTest
{
    /**
     * Verifies that disallowed URLs are neutralized without adding underline formatting.
     *
     * @throws Exception if reflective invocation of the link processor fails
     */
    @Test
    void disallowUrlBreaksPeriods() throws Exception
    {
        String out = processLinks(
            "see https://test.com now",
            ChatPermissionsFixtures.urlOnly(false, false),
            ChatColor.WHITE);

        assertEquals("see https://test com now", out);
        assertFalse(out.contains(ChatColor.UNDERLINE.toString()));
    }

    /**
     * Verifies that an underlined URL resets and then restores the active color.
     *
     * @throws Exception if reflective invocation of the link processor fails
     */
    @Test
    void underlineWrapsUrlThenRestoresColor() throws Exception
    {
        String out = processLinks(
            "§ago https://a.com end",
            ChatPermissionsFixtures.urlOnly(true, true),
            ChatColor.WHITE);

        String expectedUrl = ChatColor.UNDERLINE + "https://a.com" + ChatColor.RESET + "§a";
        assertTrue(out.contains(expectedUrl), out);
        assertTrue(out.startsWith("§ago "), out);
        assertTrue(out.endsWith(" end"), out);
    }

    /**
     * Verifies that URL processing restores color without emitting underline or reset codes.
     *
     * @throws Exception if reflective invocation of the link processor fails
     */
    @Test
    void withoutUnderlineStillAppendsRestoredColorAfterUrl() throws Exception
    {
        String out = processLinks(
            "§bgo https://a.com end",
            ChatPermissionsFixtures.urlOnly(true, false),
            ChatColor.WHITE);

        assertTrue(out.contains("https://a.com§b"), out);
        assertFalse(out.contains(ChatColor.UNDERLINE.toString()), out);
        assertFalse(out.contains(ChatColor.RESET.toString()), out);
    }

    /**
     * Verifies per-segment color restoration across multiple underlined URLs.
     *
     * @throws Exception if reflective invocation of the link processor fails
     */
    @Test
    void secondUrlFallsBackToBaseColorWhenSegmentHasNoCode() throws Exception
    {
        // First URL's "before" includes §a; second URL's "before" is only " y " → base WHITE.
        String out = processLinks(
            "§ax https://a.com y https://b.com",
            ChatPermissionsFixtures.urlOnly(true, true),
            ChatColor.WHITE);

        String afterFirst = ChatColor.UNDERLINE + "https://a.com" + ChatColor.RESET + "§a";
        String afterSecond = ChatColor.UNDERLINE + "https://b.com" + ChatColor.RESET + ChatColor.WHITE;
        assertTrue(out.contains(afterFirst), out);
        assertTrue(out.contains(afterSecond), out);
    }

    /**
     * Verifies base-color fallback for a later URL when underlining is disabled.
     *
     * @throws Exception if reflective invocation of the link processor fails
     */
    @Test
    void multiUrlWithoutUnderlineUsesBaseColorOnSecond() throws Exception
    {
        String out = processLinks(
            "§ax https://a.com y https://b.com",
            ChatPermissionsFixtures.urlOnly(true, false),
            ChatColor.GRAY);

        assertTrue(out.contains("https://a.com§a"), out);
        assertTrue(out.contains("https://b.com" + ChatColor.GRAY), out);
    }

    /**
     * Verifies that format codes following the active color are retained.
     *
     * @throws Exception if reflective invocation of the color helper fails
     */
    @Test
    void getLastColorCodeStringKeepsFormatAfterColor() throws Exception
    {
        assertEquals("§a§l", getLastColorCodeString("§a§lx", ChatColor.WHITE));
    }

    /**
     * Verifies that reset clears accumulated formats while preserving the color fallback.
     *
     * @throws Exception if reflective invocation of the color helper fails
     */
    @Test
    void getLastColorCodeStringResetClearsFormats() throws Exception
    {
        assertEquals("§a", getLastColorCodeString("§a§l§rx", ChatColor.WHITE));
    }

    /**
     * Verifies that text without codes returns the supplied base color.
     *
     * @throws Exception if reflective invocation of the color helper fails
     */
    @Test
    void getLastColorCodeStringFallsBackToBase() throws Exception
    {
        assertEquals(ChatColor.GOLD.toString(), getLastColorCodeString("plain", ChatColor.GOLD));
    }

    /**
     * After legacy deserialization, URL spans receive {@link net.md_5.bungee.api.chat.ClickEvent.Action#OPEN_URL}.
     */
    @Test
    @SuppressWarnings("deprecation")
    void attachUrlClickEventsMakesUnderlinedUrlClickable()
    {
        String legacy = ChatColor.UNDERLINE + "https://a.com" + ChatColor.RESET + "§a";
        BaseComponent[] linked = SpigotFactionChatListener.toSendableComponents(legacy, true);

        boolean found = false;
        for (BaseComponent c : linked)
        {
            if (c instanceof TextComponent text
                && "https://a.com".equals(text.getText())
                && text.getClickEvent() != null
                && text.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL
                && "https://a.com".equals(text.getClickEvent().getValue()))
            {
                found = true;
                break;
            }
        }
        assertTrue(found, java.util.Arrays.toString(linked));
    }

    /**
     * URLs embedded in a larger legacy run are split so only the URL carries the click event.
     */
    @Test
    void attachUrlClickEventsSplitsUrlOutOfSurroundingText()
    {
        BaseComponent[] linked = SpigotFactionChatListener.toSendableComponents("§ago https://a.com end", true);

        StringBuilder plain = new StringBuilder();
        boolean foundClick = false;
        for (BaseComponent c : linked)
        {
            if (c instanceof TextComponent text)
            {
                plain.append(text.getText());
                if ("https://a.com".equals(text.getText())
                    && text.getClickEvent() != null
                    && text.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL)
                {
                    foundClick = true;
                }
            }
        }
        assertEquals("go https://a.com end", plain.toString());
        assertTrue(foundClick, plain.toString());
    }

    /**
     * Sendable components must not carry empty {@code extra} lists (Spigot chat codec rejects them).
     */
    @Test
    void toSendableComponentsLeavesNoEmptyExtras()
    {
        BaseComponent[] linked = SpigotFactionChatListener.toSendableComponents(
            "see https://a.com please", true);
        for (BaseComponent c : linked)
        {
            assertTrue(c.getExtra() == null || !c.getExtra().isEmpty(), c.toString());
        }
    }

    /**
     * Without linkify, colors still survive as styled text components with no extras.
     */
    @Test
    void toSendableComponentsWithoutLinkifyPreservesText()
    {
        BaseComponent[] comps = SpigotFactionChatListener.toSendableComponents("§etest", false);
        StringBuilder plain = new StringBuilder();
        for (BaseComponent c : comps)
        {
            if (c instanceof TextComponent text)
            {
                plain.append(text.getText());
                assertTrue(c.getExtra() == null || !c.getExtra().isEmpty());
            }
        }
        assertEquals("test", plain.toString());
    }

    /**
     * Invokes the private Spigot link processor with explicit permissions.
     *
     * @param message legacy-formatted message
     * @param permissions URL and underline permissions
     * @param baseColor fallback color for segments without color codes
     * @return processed legacy-formatted message
     * @throws Exception if the private method cannot be accessed or invoked
     */
    private static String processLinks(String message, ChatPermissions permissions, ChatColor baseColor)
        throws Exception
    {
        // Reflection keeps these assertions focused on the private string transformation.
        Method m = SpigotFactionChatListener.class.getDeclaredMethod(
            "processLinks", String.class, ChatPermissions.class, ChatColor.class);
        m.setAccessible(true);
        return (String) m.invoke(null, message, permissions, baseColor);
    }

    /**
     * Invokes the private helper that derives the legacy style active at the end of text.
     *
     * @param text legacy-formatted text to inspect
     * @param baseColor fallback when the text contains no active color
     * @return active color and formatting sequence
     * @throws Exception if the private method cannot be accessed or invoked
     */
    private static String getLastColorCodeString(String text, ChatColor baseColor) throws Exception
    {
        Method m = SpigotFactionChatListener.class.getDeclaredMethod(
            "getLastColorCodeString", String.class, ChatColor.class);
        m.setAccessible(true);
        return (String) m.invoke(null, text, baseColor);
    }
}
