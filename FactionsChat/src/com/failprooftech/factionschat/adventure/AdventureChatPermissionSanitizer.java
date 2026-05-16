package com.failprooftech.factionschat.adventure;

import com.failprooftech.factionschat.chat.ChatPermissions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.Action;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Strips Adventure / MiniMessage features the sender is not allowed to use (hovers, clicks, RGB, etc.).
 * Hover {@link Action#SHOW_TEXT} contents are sanitized recursively so nested clicks/colors respect permissions.
 */
public final class AdventureChatPermissionSanitizer
{
    private AdventureChatPermissionSanitizer()
    {
    }

    /**
     * Sanitizes a component recursively.
     * 
     * @param root The root component to sanitize.
     * @param p The chat permissions to use.
     * @param fallbackColor The fallback color to use.
     * @return The sanitized component.
     */
    public static Component sanitize(Component root, ChatPermissions p, TextColor fallbackColor)
    {
        return sanitizeNode(root, p, fallbackColor);
    }

    /**
     * Sanitizes a node recursively.
     * 
     * @param c The component to sanitize.
     * @param p The chat permissions to use.
     * @param parentColor The parent color to use.
     * @return The sanitized component.
     */
    private static Component sanitizeNode(Component c, ChatPermissions p, TextColor parentColor)
    {
        TextColor here = c.style().color() != null ? c.style().color() : parentColor;

        // If the component is a text component, sanitize the style.
        if (c instanceof TextComponent)
        {
            TextComponent tc = (TextComponent) c;
            Style st = filterStyle(tc.style(), p, parentColor);
            TextComponent.Builder b = Component.text().content(tc.content()).style(st);
            if (p.allowInsert && tc.insertion() != null)
            {
                b.insertion(tc.insertion());
            }
            b.append(tc.children().stream().map(ch -> sanitizeNode(ch, p, here)).collect(Collectors.toList()));
            return b.build();
        }

        // If the component is not a text component, sanitize the children.
        List<Component> newChildren = c.children().stream()
            .map(ch -> sanitizeNode(ch, p, here))
            .collect(Collectors.toList());

        // If the component has the same children and style as the original, return the original.
        Style filtered = filterStyle(c.style(), p, parentColor);
        if (newChildren.equals(c.children()) && filtered.equals(c.style()))
        {
            return c;
        }
        return c.style(filtered).children(newChildren);
    }

    /**
     * Filters the style of a component.
     * 
     * @param s The style to filter.
     * @param p The chat permissions to use.
     * @param inherit The parent color to use.
     * @return The filtered style.
     */
    private static Style filterStyle(Style s, ChatPermissions p, TextColor inherit)
    {
        Style.Builder b = Style.style();

        // If the style has a color, filter it.
        TextColor col = s.color();
        if (col != null)
        {
            if (colorAllowed(col, p))
            {
                b.color(col);
            }
            else if (inherit != null)
            {
                b.color(inherit);
            }
        }

        // If the style has any decorations, filter them.
        for (TextDecoration d : TextDecoration.values())
        {
            TextDecoration.State state = s.decoration(d);
            if (state == TextDecoration.State.NOT_SET)
            {
                continue;
            }
            if (d == TextDecoration.OBFUSCATED && !p.allowMagic)
            {
                b.decoration(d, TextDecoration.State.FALSE);
                continue;
            }
            if ((d == TextDecoration.BOLD || d == TextDecoration.ITALIC
                || d == TextDecoration.STRIKETHROUGH || d == TextDecoration.UNDERLINED) && !p.allowFormat)
            {
                b.decoration(d, TextDecoration.State.FALSE);
                continue;
            }
            b.decoration(d, state);
        }

        // If the style has a click event, filter it.
        ClickEvent click = s.clickEvent();
        if (click != null)
        {
            if (click.action() == ClickEvent.Action.OPEN_URL)
            {
                if (p.allowUrl)
                {
                    b.clickEvent(click);
                }
            }
            else if (p.allowClick)
            {
                b.clickEvent(click);
            }
        }

        // If the style has a hover event, filter it.
        HoverEvent<?> hover = s.hoverEvent();
        if (hover != null && p.allowHover)
        {
            b.hoverEvent(sanitizeHoverEvent(hover, p, inherit));
        }

        // If the style has an insertion, filter it.
        if (p.allowInsert && s.insertion() != null)
        {
            b.insertion(s.insertion());
        }

        return b.build();
    }

    /**
     * Sanitizes a hover event.
     * 
     * @param hover The hover event to sanitize.
     * @param p The chat permissions to use.
     * @param inherit The parent color to use.
     * @return The sanitized hover event.
     */
    private static HoverEvent<?> sanitizeHoverEvent(HoverEvent<?> hover, ChatPermissions p, TextColor inherit)
    {
        if (hover.action() == Action.SHOW_TEXT)
        {
            Component value = (Component) hover.value();
            return HoverEvent.showText(sanitizeNode(value, p, inherit));
        }
        return hover;
    }

    /**
     * Checks if a color is allowed.
     * 
     * @param col The color to check.
     * @param p The chat permissions to use.
     * @return True if the color is allowed, false otherwise.
     */
    private static boolean colorAllowed(TextColor col, ChatPermissions p)
    {
        if (!p.allowColor)
        {
            return false;
        }
        if (p.allowRgb)
        {
            return true;
        }
        return col instanceof NamedTextColor;
    }
}
