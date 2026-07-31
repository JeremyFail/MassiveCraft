package com.failprooftech.factionschat.testsupport;

import com.failprooftech.factionschat.chat.ChatPermissions;

/**
 * Shared {@link ChatPermissions} builders for unit tests.
 *
 * <p>Constructor argument order matches {@link ChatPermissions}: color/format/magic/RGB,
 * URL (+ underline), hover/click/insert, keybind/translatable, rainbow/gradient/transition/font,
 * selector/score/nbt, pride/sprite/head.</p>
 */
public final class ChatPermissionsFixtures
{
    private ChatPermissionsFixtures()
    {
    }

    /**
     * All styling and interactivity flags enabled (including URL underline and MiniMessage actions).
     *
     * @return a fully permissive {@link ChatPermissions} instance
     */
    public static ChatPermissions allAllowed()
    {
        return new ChatPermissions(
            true, true, true, true,
            true, true,
            true, true, true,
            true, true,
            true, true, true, true,
            true, true, true,
            true, true, true);
    }

    /**
     * Denies every flag except optional auto-URL parsing and URL underline.
     *
     * @param allowUrl   whether clickable URL detection is allowed
     * @param underline  whether allowed URLs may be underlined
     * @return a mostly-denied {@link ChatPermissions} with only the URL pair configurable
     */
    public static ChatPermissions urlOnly(boolean allowUrl, boolean underline)
    {
        return new ChatPermissions(
            false, false, false, false,
            allowUrl, underline,
            false, false, false,
            false, false,
            false, false, false, false,
            false, false, false,
            false, false, false);
    }

    /**
     * Named colors (and format) plus URLs; denies magic, RGB, and MiniMessage hover/click/etc.
     *
     * @return permissions allowing color, format, URL, and URL underline only among the style flags
     */
    public static ChatPermissions colorAndUrl()
    {
        return new ChatPermissions(
            true, true, false, false,
            true, true,
            false, false, false,
            false, false,
            false, false, false, false,
            false, false, false,
            false, false, false);
    }

    /**
     * Named colors plus format codes; denies magic, RGB, URLs, and MiniMessage interactivity.
     *
     * @return permissions allowing color and format only
     */
    public static ChatPermissions colorFormatOnly()
    {
        return new ChatPermissions(
            true, true, false, false,
            false, false,
            false, false, false,
            false, false,
            false, false, false, false,
            false, false, false,
            false, false, false);
    }

    /**
     * Named colors only — format, magic, RGB, URLs, and MiniMessage features denied.
     *
     * @return permissions allowing color and nothing else
     */
    public static ChatPermissions colorOnly()
    {
        return new ChatPermissions(
            true, false, false, false,
            false, false,
            false, false, false,
            false, false,
            false, false, false, false,
            false, false, false,
            false, false, false);
    }
}
