package com.failprooftech.factionschat.adventure;

import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.chat.ChatPermissions;
import com.failprooftech.factionschat.util.ChatTxt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts expanded chat strings (after placeholders) into {@link Component}s using one pipeline:
 * legacy {@code §} / hex runs merged with MiniMessage {@code <tags>}, then lenient MiniMessage parse.
 */
public final class PaperAdventureChatCodec
{
    private static final MiniMessage LENIENT_MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger FALLBACK_LOGGER = Bukkit.getLogger();

    /**
     * Parses expanded format or message text into a component.
     *
     * @param expanded           string with PAPI/built-in placeholders already resolved; {@code &} is translated to {@code §}
     * @param rootDefaultColor   applied only to the deserialized root when it has no color (use for chat message
     *                           bodies so plain text picks up the color before {@code %MESSAGE%}; use {@code null}
     *                           for format strings so legacy {@code §r} resets to default white and {@code §} before
     *                           {@code %MESSAGE%} is not overridden)
     * @param legacyRgbPipeline  FactionsChat RGB + legacy segment builder (includes section deserialization)
     */
    public static Component toComponent(
        String expanded,
        TextColor rootDefaultColor,
        LegacyRgbPipeline legacyRgbPipeline)
    {
        return toComponent(expanded, rootDefaultColor, legacyRgbPipeline, null);
    }

    /**
     * Converts an expanded string to a component.
     * 
     * @param expanded The expanded string to convert to a component.
     * @param rootDefaultColor The default color to apply to the component.
     * @param legacyRgbPipeline The legacy RGB pipeline to use.
     * @param playerChatPermissions when non-null, MiniMessage tags the sender cannot use are appended as literal
     *                              text (no MiniMessage parse, no visible escape character)
     */
    public static Component toComponent(
        String expanded,
        TextColor rootDefaultColor,
        LegacyRgbPipeline legacyRgbPipeline,
        ChatPermissions playerChatPermissions)
    {
        if (expanded == null || expanded.isEmpty())
        {
            return Component.empty();
        }

        String normalized = ChatTxt.parseLegacy('&', expanded);
        if (playerChatPermissions != null)
        {
            return LegacyMiniMessageMerger.mergeAndDeserializeWithLiteralDisallowedTags(
                normalized,
                rootDefaultColor,
                legacyRgbPipeline,
                LENIENT_MINI_MESSAGE,
                playerChatPermissions);
        }
        // Format / trusted strings with literal "<§…>" brackets: legacy -> MiniMessage -> deserialize can drop §r
        // resets and recolor "<" ">" with the last active §. Legacy-only avoids that round-trip.
        if (!LegacyMiniMessageMerger.containsTrustedMiniMessageTag(normalized))
        {
            return applyBaseColor(legacyRgbPipeline.toComponent(normalized, rootDefaultColor), rootDefaultColor);
        }
        String merged = LegacyMiniMessageMerger.mergeLegacySegmentsIntoMiniMessage(
            normalized, legacyRgbPipeline, LENIENT_MINI_MESSAGE);
        try
        {
            return applyBaseColor(LENIENT_MINI_MESSAGE.deserialize(merged), rootDefaultColor);
        }
        catch (Exception e)
        {
            // A § in the MiniMessage buffer causes ParsingExceptionImpl ("Legacy formatting codes
            // detected"). Log both the merged MiniMessage and the original input so the merger
            // path that let § through can be identified and fixed.
            getLogger().log(Level.SEVERE,
                "An unexpected error occurred while parsing MiniMessage. Please file a bug report with this error log.");
            getLogger().log(Level.SEVERE, "MiniMessage deserialize failed. merged=" + merged);
            getLogger().log(Level.SEVERE, "Original expanded input=" + expanded, e);
            throw e;
        }
    }

    private static Logger getLogger()
    {
        if (FactionsChat.instance != null)
        {
            return FactionsChat.instance.getLogger();
        }
        return FALLBACK_LOGGER;
    }

    private static Component applyBaseColor(Component component, TextColor baseColor)
    {
        if (baseColor == null)
        {
            return component;
        }
        return component.colorIfAbsent(baseColor);
    }

    /**
     * Hook for the listener's legacy + custom RGB handling.
     */
    @FunctionalInterface
    public interface LegacyRgbPipeline
    {
        Component toComponent(String normalizedExpanded, TextColor baseColor);
    }
}
