package com.failprooftech.factionschat.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers RGB / legacy section handling in {@link LegacyRgbMessageCodec}, including typed {@code §x} forms
 * that must not be missed due to section-sign encoding issues in source.
 */
class LegacyRgbMessageCodecTest
{
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final LegacyRgbMessageCodec CODEC =
        new LegacyRgbMessageCodec(LegacyComponentSerializer.legacySection());

    /**
     * Modern {@code &#rrggbb} expands and colors following text.
     */
    @Test
    void modernAmpersandHexAppliesColor()
    {
        Component out = CODEC.toComponent("&#ff0000hi", null);
        assertEquals("hi", PLAIN.serialize(out));
        assertEquals(TextColor.color(0xFF0000), firstColor(out));
    }

    /**
     * Already-translated Bukkit typed hex ({@code §x§r§r…}) must take the RGB pipeline (not only {@code &#}).
     */
    @Test
    void typedSectionHexAppliesColor()
    {
        String typed = "\u00A7x\u00A7f\u00A7f\u00A70\u00A70\u00A70\u00A70hi";
        Component out = CODEC.toComponent(typed, null);
        assertEquals("hi", PLAIN.serialize(out));
        assertEquals(TextColor.color(0xFF0000), firstColor(out));
    }

    /**
     * Named legacy colors still deserialize when no RGB marker is present.
     */
    @Test
    void namedLegacyColorStillWorks()
    {
        Component out = CODEC.toComponent("\u00A7ahi", null);
        assertEquals("hi", PLAIN.serialize(out));
        assertEquals(TextColor.color(0x55FF55), firstColor(out));
    }

    private static TextColor firstColor(Component root)
    {
        if (root.color() != null)
        {
            return root.color();
        }
        if (root instanceof TextComponent text && text.color() != null)
        {
            return text.color();
        }
        for (Component child : root.children())
        {
            TextColor nested = firstColor(child);
            if (nested != null)
            {
                return nested;
            }
        }
        assertNotNull(null, "expected a color on component tree: " + root);
        return null;
    }
}
