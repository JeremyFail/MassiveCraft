package com.massivecraft.factionschat.chat;

/**
 * Permission flags for chat styling (legacy codes, URLs, MiniMessage tags).
 * See {@code plugin.yml} for {@code factions.chat.*} nodes.
 */
public final class ChatPermissions
{
    public final boolean allowColor;
    public final boolean allowFormat;
    public final boolean allowMagic;
    public final boolean allowRgb;
    public final boolean allowUrl;
    public final boolean underlineUrl;

    /** MiniMessage {@code <hover>} (show_text, show_item, show_entity, …). */
    public final boolean allowHover;
    /** MiniMessage {@code <click>} except {@code open_url} (that uses {@link #allowUrl}). */
    public final boolean allowClick;
    /** MiniMessage {@code <insert>} (shift-click paste into chat box). */
    public final boolean allowInsert;

    public final boolean allowKeybind;
    /** {@code <lang>}, {@code <tr>}, {@code <translate>}, {@code <lang_or>}, {@code <tr_or>}, {@code <translate_or>}. */
    public final boolean allowTranslatable;

    public final boolean allowRainbow;
    public final boolean allowGradient;
    public final boolean allowTransition;
    public final boolean allowFont;

    public final boolean allowSelector;
    public final boolean allowScore;
    public final boolean allowNbt;

    public final boolean allowPride;
    public final boolean allowSprite;
    public final boolean allowHead;

    public ChatPermissions(
        boolean allowColor,
        boolean allowFormat,
        boolean allowMagic,
        boolean allowRgb,
        boolean allowUrl,
        boolean underlineUrl,
        boolean allowHover,
        boolean allowClick,
        boolean allowInsert,
        boolean allowKeybind,
        boolean allowTranslatable,
        boolean allowRainbow,
        boolean allowGradient,
        boolean allowTransition,
        boolean allowFont,
        boolean allowSelector,
        boolean allowScore,
        boolean allowNbt,
        boolean allowPride,
        boolean allowSprite,
        boolean allowHead)
    {
        this.allowColor = allowColor;
        this.allowFormat = allowFormat;
        this.allowMagic = allowMagic;
        this.allowRgb = allowRgb;
        this.allowUrl = allowUrl;
        this.underlineUrl = underlineUrl;
        this.allowHover = allowHover;
        this.allowClick = allowClick;
        this.allowInsert = allowInsert;
        this.allowKeybind = allowKeybind;
        this.allowTranslatable = allowTranslatable;
        this.allowRainbow = allowRainbow;
        this.allowGradient = allowGradient;
        this.allowTransition = allowTransition;
        this.allowFont = allowFont;
        this.allowSelector = allowSelector;
        this.allowScore = allowScore;
        this.allowNbt = allowNbt;
        this.allowPride = allowPride;
        this.allowSprite = allowSprite;
        this.allowHead = allowHead;
    }
}
