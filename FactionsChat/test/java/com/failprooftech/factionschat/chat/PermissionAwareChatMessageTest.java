package com.failprooftech.factionschat.chat;

import com.failprooftech.factionschat.adventure.LegacyRgbMessageCodec;
import com.failprooftech.factionschat.testsupport.ChatPermissionsFixtures;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Permission-aware chat body pipelines for Spigot legacy strings and Paper Adventure components.
 * <p>
 * Covers {@link BukkitLegacyPermissionChatMessage} and {@link PermissionAwareChatMessage}:
 * disallowed markup kept literal (with base color), allowed codes translated, and RGB expansion.
 */
class PermissionAwareChatMessageTest
{
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final LegacyRgbMessageCodec LEGACY_RGB =
        new LegacyRgbMessageCodec(LegacyComponentSerializer.legacySection());

    /**
     * Boots MockBukkit once so Adventure codec static init can read {@code Bukkit.getLogger()}.
     */
    @BeforeAll
    static void bootServerForAdventureCodecStaticInit()
    {
        // PaperAdventureChatCodec captures Bukkit.getLogger() in <clinit>.
        MockBukkit.mock();
    }

    /** Shuts down the shared MockBukkit server after the class finishes. */
    @org.junit.jupiter.api.AfterAll
    static void tearDownServer()
    {
        MockBukkit.unmock();
    }

    /**
     * Spigot: denied color codes stay as literal {@code &} text, prefixed by the base color.
     */
    @Test
    void spigotKeepsDisallowedColorCodesLiteral()
    {
        String out = BukkitLegacyPermissionChatMessage.toBukkitLegacyString(
            "&ahi",
            ChatPermissionsFixtures.urlOnly(false, false),
            "§7");

        assertEquals("§7&ahi", out);
    }

    /**
     * Spigot: allowed {@code &a} is translated to section-sign color and applied to following text.
     */
    @Test
    void spigotTranslatesAllowedColorCodes()
    {
        String out = BukkitLegacyPermissionChatMessage.toBukkitLegacyString(
            "&ahi",
            ChatPermissionsFixtures.colorOnly(),
            "§7");

        assertEquals("§ahi", out);
    }

    /**
     * Spigot: color+format allowed but magic denied — {@code &k} remains literal under base color;
     * allowed colors still translate.
     */
    @Test
    void spigotMixedAllowedColorAndDeniedMagic()
    {
        String out = BukkitLegacyPermissionChatMessage.toBukkitLegacyString(
            "&akeep &kblocked &bok",
            ChatPermissionsFixtures.colorFormatOnly(),
            "§f");

        assertTrue(out.contains("§akeep "), out);
        // Denied magic: base color + literal &k (not translated §k).
        assertTrue(out.contains("§f&k"), out);
        assertTrue(out.contains("§bok") || out.endsWith("ok"), out);
        assertFalse(out.contains("§kblocked"), out);
    }

    /**
     * Spigot: modern {@code &#rrggbb} expands to typed {@code §x§r§r...} when RGB is allowed.
     */
    @Test
    void spigotExpandsModernRgbWhenAllowed()
    {
        String out = BukkitLegacyPermissionChatMessage.toBukkitLegacyString(
            "&#ff00aahi",
            ChatPermissionsFixtures.allAllowed(),
            "");

        assertTrue(out.startsWith("§x§f§f§0§0§a§a"), out);
        assertTrue(out.endsWith("hi"), out);
    }

    /**
     * Spigot: null and empty raw input both yield an empty string (base color unused).
     */
    @Test
    void spigotNullAndEmpty()
    {
        assertEquals("", BukkitLegacyPermissionChatMessage.toBukkitLegacyString(
            null, ChatPermissionsFixtures.allAllowed(), "§a"));
        assertEquals("", BukkitLegacyPermissionChatMessage.toBukkitLegacyString(
            "", ChatPermissionsFixtures.allAllowed(), "§a"));
    }

    /**
     * Paper: denied {@code &a} stays in plain text with the supplied base {@link NamedTextColor}.
     */
    @Test
    void paperKeepsDisallowedColorAsLiteralWithBaseColor()
    {
        Component out = PermissionAwareChatMessage.toAdventureComponent(
            "&ahi",
            NamedTextColor.GRAY,
            ChatPermissionsFixtures.urlOnly(false, false),
            LEGACY_RGB);

        assertEquals("&ahi", PLAIN.serialize(out));
        assertEquals(NamedTextColor.GRAY, firstTextColor(out));
    }

    /**
     * Paper: allowed named color is applied to content; plain text omits the markup characters.
     */
    @Test
    void paperParsesAllowedNamedColor()
    {
        Component out = PermissionAwareChatMessage.toAdventureComponent(
            "&ahi",
            NamedTextColor.WHITE,
            ChatPermissionsFixtures.colorOnly(),
            LEGACY_RGB);

        assertEquals("hi", PLAIN.serialize(out));
        assertEquals(NamedTextColor.GREEN, firstTextColor(out));
    }

    /**
     * Paper: null and empty raw input both map to {@link Component#empty()}.
     */
    @Test
    void paperEmptyRawIsEmptyComponent()
    {
        assertEquals(Component.empty(), PermissionAwareChatMessage.toAdventureComponent(
            "", NamedTextColor.WHITE, ChatPermissionsFixtures.allAllowed(), LEGACY_RGB));
        assertEquals(Component.empty(), PermissionAwareChatMessage.toAdventureComponent(
            null, NamedTextColor.WHITE, ChatPermissionsFixtures.allAllowed(), LEGACY_RGB));
    }

    /**
     * Depth-first walk for the first {@link NamedTextColor} on a text component or its children.
     */
    private static NamedTextColor firstTextColor(Component root)
    {
        if (root instanceof TextComponent text && text.color() instanceof NamedTextColor named)
        {
            return named;
        }
        for (Component child : root.children())
        {
            NamedTextColor nested = firstTextColor(child);
            if (nested != null)
            {
                return nested;
            }
        }
        return root.color() instanceof NamedTextColor named ? named : null;
    }
}
