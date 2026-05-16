package com.failprooftech.factionschat.adventure;

import com.failprooftech.factionschat.chat.ChatPermissions;
import com.failprooftech.factionschat.chat.PermissionAwareChatMessage;
import com.failprooftech.factionschat.listeners.FactionChatListenerBase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Walks an Adventure subtree and re-parses {@link TextComponent} leaves that still contain literal markup
 * ({@code &} / § color and format codes, hex, Bukkit typed {@code &x…} RGB, MiniMessage angle brackets).
 *
 * <p>Parsing uses {@link PermissionAwareChatMessage} so disallowed snippets stay literal and allowed runs use the same
 * legacy + MiniMessage pipeline as flat chat. Parsed content from a leaf is concatenated after expanded siblings so
 * plain-text order stays {@code content} then {@code children}, matching Kyori serializers.</p>
 */
public final class ChatMarkupLeafExpander
{
    /**
     * Same RGB patterns as chat validation ({@link FactionChatListenerBase#RGB_REGEX}): {@code &#RRGGBB}, {@code §x§…}, etc.
     */
    private static final Pattern RGB_PATTERN = Pattern.compile(FactionChatListenerBase.RGB_REGEX);

    /**
     * Bukkit-style typed hex ({@code &x&R&R&G&G&B&B} / § variant); checked at {@code &}/§ positions when followed by {@code x}.
     */
    private static final Pattern LEGACY_TYPED_HEX = Pattern.compile(
        "(?i)(?:&|§)x(?:[&§][A-Fa-f0-9]){6}");

    /**
     * Prevents instantiation; use {@link #expand(Component, TextColor, ChatPermissions, PaperAdventureChatCodec.LegacyRgbPipeline)}.
     */
    private ChatMarkupLeafExpander()
    {
    }

    /**
     * Depth-first expansion: each {@link TextComponent} is optionally replaced by a parsed variant; non-text nodes only
     * recurse into {@linkplain Component#children() children}.
     *
     * @param root          subtree rooted at {@code event.message()} or a stripped body
     * @param baseColor     channel default passed through to {@link PermissionAwareChatMessage}
     * @param permissions   sender caps for legacy / MiniMessage features
     * @param legacyRgb     legacy section + custom RGB codec (same instance as Paper listener)
     * @return new tree sharing unchanged subtrees where possible
     */
    public static Component expand(Component root, TextColor baseColor,
        ChatPermissions permissions, PaperAdventureChatCodec.LegacyRgbPipeline legacyRgb)
    {
        if (root instanceof TextComponent)
        {
            TextComponent tc = (TextComponent) root;
            List<Component> newChildren = mapChildren(tc.children(), baseColor, permissions, legacyRgb);
            String content = tc.content();

            if (!content.isEmpty() && mightContainParsableMarkup(content))
            {
                // Leaf still has typed codes or tags as raw characters - fold through the unified chat codec.
                Component parsed = PermissionAwareChatMessage.toAdventureComponent(content, baseColor, permissions, legacyRgb);
                Component merged = parsed;
                // Plain iteration order is text content then children; parsed replaces content only.
                for (Component ch : newChildren)
                {
                    merged = merged.append(ch);
                }
                return merged;
            }

            if (newChildren.equals(tc.children()))
            {
                return tc;
            }
            return Component.text(content).style(tc.style()).children(newChildren);
        }

        // Wrapper-only node: propagate into descendants.
        List<Component> mapped = mapChildren(root.children(), baseColor, permissions, legacyRgb);
        if (mapped.equals(root.children()))
        {
            return root;
        }
        return root.children(mapped);
    }

    /**
     * Applies {@link #expand(Component, TextColor, ChatPermissions, PaperAdventureChatCodec.LegacyRgbPipeline)} to each child.
     * Returns the original {@code children} list reference when nothing changed to avoid needless allocations.
     *
     * @param children    direct children of the node being processed
     * @param baseColor   forwarded to recursive {@link #expand}
     * @param permissions forwarded to recursive {@link #expand}
     * @param legacyRgb   forwarded to recursive {@link #expand}
     * @return either the original {@code children} list or a new list with one or more mapped instances
     */
    private static List<Component> mapChildren(List<Component> children, TextColor baseColor,
        ChatPermissions permissions, PaperAdventureChatCodec.LegacyRgbPipeline legacyRgb)
    {
        List<Component> out = new ArrayList<>(children.size());
        boolean changed = false;
        for (Component child : children)
        {
            Component mapped = expand(child, baseColor, permissions, legacyRgb);
            if (mapped != child)
            {
                changed = true;
            }
            out.add(mapped);
        }
        return changed ? out : children;
    }

    /**
     * Fast-negative filter before calling {@link PermissionAwareChatMessage}. Avoids running the full scanner/parser on
     * leaves that are obviously plain prose (no legacy introducers, no RGB substring, no MiniMessage opener).
     *
     * @param s non-null leaf {@linkplain TextComponent#content() content}
     * @return {@code true} if the leaf might contain markup worth parsing
     */
    static boolean mightContainParsableMarkup(String s)
    {
        if (s.isEmpty())
        {
            return false;
        }
        final int n = s.length();

        // Classic two-char legacy: & or § followed by digit/hex letter/format letter.
        for (int i = 0; i < n - 1; i++)
        {
            char c0 = s.charAt(i);
            char c1 = s.charAt(i + 1);
            if ((c0 == '&' || c0 == '§'))
            {
                if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c1) >= 0)
                {
                    return true;
                }
                if (c1 == '#')
                {
                    return true;
                }
                if ((c1 == 'x' || c1 == 'X') && LEGACY_TYPED_HEX.matcher(s).region(i, n).lookingAt())
                {
                    return true;
                }
            }
        }

        // &# / §# forms and §x spread form (may not start with &/§+x in edge cases).
        Matcher rgb = RGB_PATTERN.matcher(s);
        if (rgb.find())
        {
            return true;
        }

        // MiniMessage: share the same heuristic as the merger (escaped {@code \<} is ignored).
        for (int i = 0; i < n; i++)
        {
            if (LegacyMiniMessageMerger.isMiniMessageTagStart(s, i))
            {
                return true;
            }
        }
        return false;
    }
}
