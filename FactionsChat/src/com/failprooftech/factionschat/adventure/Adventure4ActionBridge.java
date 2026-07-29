package com.failprooftech.factionschat.adventure;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Adventure 4 (Paper 1.21.x) action checks using enum constants.
 *
 * <p>Only loaded when Adventure 5's {@code ClickEvent.Action.OpenUrl} class is absent
 * ({@link AdventureActionCompat}). Linking this class on Adventure 5 would throw
 * {@link NoSuchFieldError} because {@code OPEN_URL}'s field descriptor changed.</p>
 *
 * <p>TODO(adventure4-drop): Delete this class when Adventure 4 support is removed.</p>
 */
public final class Adventure4ActionBridge implements AdventureActionBridge
{
    @Override
    public boolean isOpenUrl(ClickEvent click)
    {
        return click.action() == ClickEvent.Action.OPEN_URL;
    }

    @Override
    public boolean isShowText(HoverEvent<?> hover)
    {
        return hover.action() == HoverEvent.Action.SHOW_TEXT;
    }
}
