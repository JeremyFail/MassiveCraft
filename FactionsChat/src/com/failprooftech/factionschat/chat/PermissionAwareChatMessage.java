package com.failprooftech.factionschat.chat;

import com.failprooftech.factionschat.adventure.PaperAdventureChatCodec;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Splits player chat into literal spans (codes the sender is not allowed to use stay as plain
 * text) and parseable spans (passed through {@link PaperAdventureChatCodec} for Paper).
 * Spigot string output lives in {@link BukkitLegacyPermissionChatMessage} so the JVM need not
 * load Adventure on non-Paper servers.
 *
 * <p>Literal spans use the message base {@link TextColor} on Paper so they match the rest of the body; on Spigot
 * (see {@link BukkitLegacyPermissionChatMessage}) they are prefixed with the format base ChatColor.</p>
 */
public final class PermissionAwareChatMessage
{
    private PermissionAwareChatMessage()
    {
    }

    /**
     * Builds an Adventure {@link Component} for Paper chat: disallowed legacy/RGB snippets stay as
     * literal text; allowed runs use the unified legacy + MiniMessage pipeline.
     *
     * @param raw          the player's message as typed (may contain {@code &}, {@code §}, MiniMessage, etc.)
     * @param baseColor    default color for roots without an explicit color; applied to literal spans; may be {@code null}
     * @param permissions  what the sender may use (drives which prefixes count as literal)
     * @param legacyRgb    codec for translating a parseable substring into nested components before MiniMessage merge
     * @return a single flattened component (may be {@link Component#empty()} if {@code raw} is null or empty)
     */
    public static Component toAdventureComponent(
        String raw,
        TextColor baseColor,
        ChatPermissions permissions,
        PaperAdventureChatCodec.LegacyRgbPipeline legacyRgb)
    {
        if (raw == null || raw.isEmpty())
        {
            return Component.empty();
        }
        Component out = Component.empty();
        final int n = raw.length();
        int i = 0;
        // Scan left-to-right: either emit a blocked code as plain text, or hand a maximal "parseable" run to the codec.
        while (i < n)
        {
            int literalLen = ChatMessageDisallowedPrefix.disallowedPrefixLength(raw, i, permissions);
            if (literalLen > 0)
            {
                String lit = raw.substring(i, i + literalLen);
                out = out.append(baseColor != null ? Component.text(lit, baseColor) : Component.text(lit));
                i += literalLen;
                continue;
            }
            // Extend until the next index that starts a disallowed prefix (or end of string).
            int next = i;
            while (next < n && ChatMessageDisallowedPrefix.disallowedPrefixLength(raw, next, permissions) == 0)
            {
                next++;
            }
            if (next > i)
            {
                out = out.append(PaperAdventureChatCodec.toComponent(raw.substring(i, next), baseColor, legacyRgb, permissions));
            }
            i = next;
        }
        return out;
    }
}
