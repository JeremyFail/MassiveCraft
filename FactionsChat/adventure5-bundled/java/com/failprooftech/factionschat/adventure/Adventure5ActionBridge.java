package com.failprooftech.factionschat.adventure;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Adventure 5 (Paper 26.x) action checks using typed {@link ClickEvent.Action} constants.
 *
 * <p>Compiled against Adventure 5 API in an isolated Maven step ({@code adventure5-bundled/java},
 * outside {@code src/} so the Adventure 4 main compile never sees it). Loaded only via
 * {@link AdventureActionCompat}.</p>
 *
 * <p>TODO(adventure4-drop): After dropping Adventure 4, move these checks into
 * {@link AdventureChatPermissionSanitizer} (or keep this class as the sole bridge) and delete
 * {@link Adventure4ActionBridge} / the dual-compile setup in {@code pom.xml}.</p>
 */
public final class Adventure5ActionBridge implements AdventureActionBridge
{
    @Override
    public boolean isOpenUrl(ClickEvent click)
    {
        // Adventure 5: OPEN_URL is ClickEvent.Action.OpenUrl (not an enum constant).
        return click.action() instanceof ClickEvent.Action.OpenUrl
            || click.action() == ClickEvent.Action.OPEN_URL;
    }

    @Override
    public boolean isShowText(HoverEvent<?> hover)
    {
        // HoverEvent.Action remains a named constant field (SHOW_TEXT) on Adventure 5.
        return hover.action() == HoverEvent.Action.SHOW_TEXT;
    }
}
