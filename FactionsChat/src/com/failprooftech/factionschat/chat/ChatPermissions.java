package com.failprooftech.factionschat.chat;

/**
 * Permission flags for chat styling (legacy codes, URLs, MiniMessage tags).
 * See {@code plugin.yml} for {@code factions.chat.*} nodes.
 */
public final class ChatPermissions
{
    /** Allow color codes in chat messages. Includes legacy color codes and MiniMessage color codes. */
    public final boolean allowColor;
    /** Allow formatting codes in chat messages. Includes legacy formatting codes and MiniMessage formatting codes. */
    public final boolean allowFormat;
    /** Allow magic codes in chat messages. Includes legacy magic codes and MiniMessage <obfuscated> tags. */
    public final boolean allowMagic;
    /** Allow RGB color codes in chat messages. Includes legacy RGB color codes and MiniMessage RGB <color> tags. */
    public final boolean allowRgb;
    /** Allow automatically parsed, clickable URLs in chat messages (without <click:open_url:…> tags - this is automatic handling). */
    public final boolean allowUrl;
    /** Allow underlining clickable URLs in chat messages. */
    public final boolean underlineUrl;

    /** MiniMessage {@code <hover>} (show_text, show_item, show_entity, …). */
    public final boolean allowHover;
    /** MiniMessage {@code <click>} except {@code open_url} (that uses {@link #allowUrl}). */
    public final boolean allowClick;
    /** MiniMessage {@code <insert>} (shift-click paste into chat box). */
    public final boolean allowInsert;

    /** MiniMessage {@code <key>} (key.jump, key.sneak, etc.). */
    public final boolean allowKeybind;
    /** MiniMessage {@code <lang>}, {@code <tr>}, {@code <translate>}, {@code <lang_or>}, {@code <tr_or>}, {@code <translate_or>}. */
    public final boolean allowTranslatable;

    /** MiniMessage {@code <rainbow>} (requires color permission in practice). */
    public final boolean allowRainbow;
    /** MiniMessage {@code <gradient>} (requires color permission in practice). */
    public final boolean allowGradient;
    /** MiniMessage {@code <transition>} (requires color permission in practice). */
    public final boolean allowTransition;
    /** MiniMessage {@code <font>} (requires color permission in practice). */
    public final boolean allowFont;

    /** MiniMessage {@code <selector>} (requires color permission in practice). */
    public final boolean allowSelector;
    /** MiniMessage {@code <score>} (requires color permission in practice). */
    public final boolean allowScore;
    /** MiniMessage {@code <nbt>} (requires color permission in practice). */
    public final boolean allowNbt;

    /** MiniMessage {@code <pride>} (requires color permission in practice). */
    public final boolean allowPride;
    /** MiniMessage {@code <sprite>} (requires color permission in practice). */
    public final boolean allowSprite;
    /** MiniMessage {@code <head>} (requires color permission in practice). */
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
