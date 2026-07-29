package com.failprooftech.factionschat.adventure;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Facade that picks an {@link AdventureActionBridge} for the Adventure generation on the server.
 *
 * <p>Mirrors Paper vs Spigot listener selection: detect once, {@link Class#forName(String)} only the
 * matching implementation so the other is never linked (Adventure 4 and 5 {@code Action} constants are
 * binary-incompatible — unlike Paper/Spigot APIs, they cannot share one compile classpath).</p>
 *
 * <p>TODO(adventure4-drop): Replace this facade with direct Adventure 5 checks (or a single bridge)
 * once Paper 1.21.x support is dropped.</p>
 */
public final class AdventureActionCompat
{
    /**
     * Nested class present only on Adventure 5+ (Paper 26.x).
     */
    private static final String ADVENTURE5_OPEN_URL_CLASS =
        "net.kyori.adventure.text.event.ClickEvent$Action$OpenUrl";

    private static final String BRIDGE_ADVENTURE5 =
        "com.failprooftech.factionschat.adventure.Adventure5ActionBridge";

    private static final String BRIDGE_ADVENTURE4 =
        "com.failprooftech.factionschat.adventure.Adventure4ActionBridge";

    private static final AdventureActionBridge BRIDGE = loadBridge();

    private AdventureActionCompat()
    {
    }

    /**
     * @param click non-null click event
     * @return {@code true} if the action opens a URL
     */
    public static boolean isOpenUrl(ClickEvent click)
    {
        return BRIDGE.isOpenUrl(click);
    }

    /**
     * @param hover non-null hover event
     * @return {@code true} if the action shows text
     */
    public static boolean isShowText(HoverEvent<?> hover)
    {
        return BRIDGE.isShowText(hover);
    }

    private static AdventureActionBridge loadBridge()
    {
        final String className = isAdventure5() ? BRIDGE_ADVENTURE5 : BRIDGE_ADVENTURE4;
        try
        {
            return (AdventureActionBridge) Class.forName(className).getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Failed to load Adventure action bridge: " + className, e);
        }
    }

    /**
     * Same style of detection as {@code FactionsChat#isPaper()} — class presence only, no field access.
     */
    private static boolean isAdventure5()
    {
        try
        {
            Class.forName(ADVENTURE5_OPEN_URL_CLASS);
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }
}
