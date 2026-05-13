package com.massivecraft.factionschat.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes a leading plain-text prefix from an Adventure tree by walking components in the same order as
 * {@link net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer}: each {@link TextComponent}'s content
 * string first, then its children, depth-first.
 *
 * <p>Used for colon-channel quick messages ({@code :mode text}): routing uses plain text, but the displayed body should
 * keep any rich structure on the suffix. The prefix to strip must match the plain-text ordering exactly or
 * {@link #stripPrefix(Component, String)} returns {@code null} and callers can fall back to a string-only pipeline.</p>
 */
public final class ComponentLeadingPlainStripper
{
    /**
     * Prevents instantiation; all entry points are static.
     */
    private ComponentLeadingPlainStripper()
    {
    }

    /**
     * Removes the first {@code prefix.length()} plain-text characters from {@code root}'s iteration order,
     * returning the remaining component subtree.
     *
     * @param root   full message component tree (e.g. {@code event.message()})
     * @param prefix plain UTF-16 code units to consume from the front; must equal the plain text of the stripped region
     * @return the tree after the prefix, {@code root} unchanged if {@code prefix} is null or empty,
     *         or {@code null} if the prefix could not be matched (character mismatch or not enough plain text)
     */
    public static Component stripPrefix(Component root, String prefix)
    {
        if (prefix == null || prefix.isEmpty())
        {
            return root;
        }
        // Single-element array so recursion can mutate the cursor through the prefix string.
        int[] pi = new int[1];
        Component out = stripComponent(root, prefix, pi);
        if (out == null)
        {
            return null;
        }
        // Partial match: plain text ended before the full prefix was consumed.
        if (pi[0] != prefix.length())
        {
            return null;
        }
        return out;
    }

    /**
     * Dispatches stripping for one node: if the prefix is already fully consumed, returns the subtree unchanged;
     * {@link TextComponent} nodes consume from their {@linkplain TextComponent#content() content} first;
     * other components recurse only through {@linkplain Component#children() children}.
     *
     * @param component root of the subtree being stripped
     * @param prefix    full prefix string (shared across recursion)
     * @param pi        {@code pi[0]} is the index into {@code prefix} (cursor); mutated as characters are matched
     * @return the transformed subtree, or {@code null} on mismatch
     */
    private static Component stripComponent(Component component, String prefix, int[] pi)
    {
        if (pi[0] >= prefix.length())
        {
            return component;
        }
        if (component instanceof TextComponent)
        {
            return stripTextComponent((TextComponent) component, prefix, pi);
        }
        return stripThroughChildren(component, prefix, pi);
    }

    /**
     * Strips prefix characters from this text node's content, then continues into children if the prefix continues.
     * When the prefix ends inside this node's content, the remainder of the content plus all original children are kept.
     *
     * @param tc     text component whose plain iteration is content then children
     * @param prefix full prefix string
     * @param pi     cursor into {@code prefix}
     * @return rebuilt text node / flattened equivalent, or {@code null} on mismatch
     */
    private static Component stripTextComponent(TextComponent tc, String prefix, int[] pi)
    {
        String content = tc.content();
        int ci = 0;
        final int clen = content.length();

        // Match prefix against this leaf's content, one code unit at a time.
        while (ci < clen && pi[0] < prefix.length())
        {
            if (content.charAt(ci) != prefix.charAt(pi[0]))
            {
                return null;
            }
            ci++;
            pi[0]++;
        }

        final boolean prefixDone = pi[0] >= prefix.length();

        if (!prefixDone)
        {
            // Prefix continues past this node's content; entire content must have been consumed.
            if (ci != clen)
            {
                return null;
            }
            List<Component> parts = new ArrayList<>(tc.children().size());
            for (Component child : tc.children())
            {
                Component stripped = stripComponent(child, prefix, pi);
                if (stripped == null)
                {
                    return null;
                }
                parts.add(stripped);
            }
            // Parent style preserved on an empty-text wrapper so sibling decorations survive.
            return flattenWithStyle(tc.style(), parts);
        }

        // Prefix fully consumed; keep trailing slice of content (if any) and all descendants unchanged.
        TextComponent.Builder b = Component.text().style(tc.style());
        if (ci < clen)
        {
            b.content(content.substring(ci));
        }
        b.append(tc.children());
        return b.build();
    }

    /**
     * Non-text wrappers (e.g. hover/click wrappers with no plain of their own): advance only through {@code children}.
     * Once the prefix cursor reaches the end, remaining children are appended without further stripping.
     *
     * @param wrapper non-text component
     * @param prefix  full prefix string
     * @param pi      cursor into {@code prefix}
     * @return wrapper with possibly stripped descendants, original reference if unchanged, or {@code null} on failure
     */
    private static Component stripThroughChildren(Component wrapper, String prefix, int[] pi)
    {
        if (pi[0] >= prefix.length())
        {
            return wrapper;
        }
        List<Component> out = new ArrayList<>(wrapper.children().size());
        for (Component child : wrapper.children())
        {
            if (pi[0] >= prefix.length())
            {
                // Prefix finished mid-list; tail children stay verbatim.
                out.add(child);
                continue;
            }
            Component stripped = stripComponent(child, prefix, pi);
            if (stripped == null)
            {
                return null;
            }
            out.add(stripped);
        }
        // Ran out of tree before consuming full prefix (e.g. empty children).
        if (pi[0] < prefix.length())
        {
            return null;
        }
        return rebuildWrapper(wrapper, out);
    }

    /**
     * Replaces {@code original}'s child list when stripping produced structural changes; avoids allocating when nothing moved.
     *
     * @param original    wrapper whose children were rewritten
     * @param newChildren replacement child list after stripping
     * @return {@code original} if child lists are identical, else {@code original.children(newChildren)}
     */
    private static Component rebuildWrapper(Component original, List<Component> newChildren)
    {
        if (newChildren.equals(original.children()))
        {
            return original;
        }
        return original.children(newChildren);
    }

    /**
     * Builds one {@link TextComponent} that carries {@code style} and appends all stripped child subtrees as siblings.
     * Used when the original text node's content was fully eaten by the prefix but descendants still contribute plain text.
     *
     * @param style inherited style from the stripped parent text node
     * @param parts processed children (already stripped)
     * @return a single text builder result containing those parts
     */
    private static Component flattenWithStyle(net.kyori.adventure.text.format.Style style, List<Component> parts)
    {
        TextComponent.Builder b = Component.text().style(style);
        for (Component p : parts)
        {
            b.append(p);
        }
        return b.build();
    }
}
