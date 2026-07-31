package com.failprooftech.factionschat.listeners;

import com.failprooftech.factionschat.adventure.AdventureActionCompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Paper component URL processing preserves styles inherited from parent components.
 */
class ProcessLinksStyleInheritanceTest
{
    /**
     * Verifies that splitting a URL child retains the parent's green color on preceding text.
     *
     * @throws Exception if reflective access to the link processor fails
     */
    @Test
    void preservesInheritedGreenOnTextBeforeUrl() throws Exception
    {
        // Color on parent, URL text on uncolored child — the structure that previously lost green.
        Component tree = Component.empty()
            .color(NamedTextColor.GREEN)
            .append(Component.text("test https://test.com"));

        Component processed = invokeProcessLinks(tree, true);

        boolean sawGreenBefore = false;
        boolean sawClickableUrl = false;
        for (Component node : flatten(processed))
        {
            if (!(node instanceof TextComponent text))
            {
                continue;
            }
            String content = text.content();
            if (content.contains("test") && !content.contains("http"))
            {
                assertEquals(NamedTextColor.GREEN, text.color());
                sawGreenBefore = true;
            }
            if (content.contains("https://test.com"))
            {
                assertNotNull(text.clickEvent());
                assertTrue(AdventureActionCompat.isOpenUrl(text.clickEvent()));
                assertEquals(TextDecoration.State.TRUE, text.decoration(TextDecoration.UNDERLINED));
                sawClickableUrl = true;
            }
        }
        assertTrue(sawGreenBefore, "expected green text before URL");
        assertTrue(sawClickableUrl, "expected clickable URL");
    }

    /**
     * Calls the listener's private component link processor for focused regression coverage.
     *
     * @param input component tree to process
     * @param underline whether detected URLs should be underlined
     * @return processed component tree
     * @throws Exception if the private method cannot be accessed or invoked
     */
    private static Component invokeProcessLinks(Component input, boolean underline) throws Exception
    {
        PaperFactionChatListener listener = new PaperFactionChatListener();
        // Reflection isolates the private tree transformation without exercising chat routing.
        Method m = PaperFactionChatListener.class.getDeclaredMethod(
            "processLinksInComponent", Component.class, boolean.class);
        m.setAccessible(true);
        return (Component) m.invoke(listener, input, underline);
    }

    /**
     * Flattens a component tree in pre-order for style and action assertions.
     *
     * @param root root component
     * @return mutable list containing the root and all descendants
     */
    private static List<Component> flatten(Component root)
    {
        List<Component> out = new ArrayList<>();
        walk(root, out);
        return out;
    }

    /**
     * Adds a component subtree to a pre-order output list.
     *
     * @param c current component
     * @param out destination list
     */
    private static void walk(Component c, List<Component> out)
    {
        out.add(c);
        for (Component child : c.children())
        {
            walk(child, out);
        }
    }
}
