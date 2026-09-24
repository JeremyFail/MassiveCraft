package com.failprooftech.factionschat.adventure;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Adventure-version-specific click/hover action checks.
 *
 * <p>Paper 1.21.x ships Adventure 4 ({@link ClickEvent.Action} is an enum). Paper 26.x ships Adventure 5
 * ({@code Action} is a typed interface; {@code OPEN_URL} is no longer an enum field). Implementations are
 * compiled against the matching Adventure generation and loaded lazily via {@link AdventureActionCompat}
 * (same idea as Paper vs Spigot listeners).</p>
 *
 * <p>TODO(adventure4-drop): When Paper 1.21.x / Adventure 4 support is removed, delete
 * {@link Adventure4ActionBridge}, {@link Adventure5ActionBridge}, and {@link AdventureActionCompat},
 * and call {@code click.action() == ClickEvent.Action.OPEN_URL} (or
 * {@code instanceof ClickEvent.Action.OpenUrl}) directly from {@link AdventureChatPermissionSanitizer}.</p>
 */
public interface AdventureActionBridge
{
    /**
     * @param click non-null click event
     * @return {@code true} if the action opens a URL
     */
    boolean isOpenUrl(ClickEvent click);

    /**
     * @param hover non-null hover event
     * @return {@code true} if the action shows text
     */
    boolean isShowText(HoverEvent<?> hover);
}
