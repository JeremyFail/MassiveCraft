package com.massivecraft.factionschat.listeners;

import com.massivecraft.factionschat.adventure.AdventureChatPermissionSanitizer;
import com.massivecraft.factionschat.adventure.LegacyRgbMessageCodec;
import com.massivecraft.factionschat.adventure.PaperAdventureChatCodec;
import com.massivecraft.factionschat.chat.ChatPermissions;
import com.massivecraft.factionschat.chat.PermissionAwareChatMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;

/**
 * Formats Minecraft chat for DiscordSRV using the same UNIFIED legacy + MiniMessage + RGB pipeline as Paper chat,
 * then serializes to a legacy § string for DiscordSRV's deprecated {@code GameChatMessagePreProcessEvent#setMessage(String)}.
 *
 * <p>DiscordSRV ships a shaded copy of Adventure; {@code setMessageComponent} with {@code net.kyori} types is not
 * binary-compatible with that JAR, so we pass legacy text and let DiscordSRV parse it with its own {@code MessageUtil}.</p>
 *
 * <p>Lives in this package to use {@link FactionChatListenerBase}'s protected permission/strip helpers.</p>
 */
final class DiscordSRVChatRelayFormatter
{
    private static final TextColor BASE = NamedTextColor.WHITE;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final LegacyRgbMessageCodec RGB_CODEC = new LegacyRgbMessageCodec(LEGACY);

    private DiscordSRVChatRelayFormatter()
    {
    }

    /**
     * Player chat body (e.g. colon-quick) -> legacy string for DiscordSRV, respecting permissions and sanitizer.
     */
    static String playerBodyToDiscordLegacy(Player player, String body, FactionChatListenerBase base)
    {
        ChatPermissions perms = base.getPlayerChatPermissions(player);
        Component c = PermissionAwareChatMessage.toAdventureComponent(body, BASE, perms, RGB_CODEC);
        c = AdventureChatPermissionSanitizer.sanitize(c, perms, BASE);
        return LEGACY.serialize(c);
    }

    /**
     * Trusted config snippet (e.g. {@link com.massivecraft.factionschat.config.Settings.ChatPrefixes}) -> legacy for {@code Bukkit.broadcast}.
     */
    static String trustedConfigSnippetToLegacy(String snippet)
    {
        if (snippet == null || snippet.isEmpty())
        {
            return "";
        }
        Component c = PaperAdventureChatCodec.toComponent(
            snippet,
            null,
            RGB_CODEC);
        return LEGACY.serialize(c);
    }
}
