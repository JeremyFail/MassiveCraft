package com.failprooftech.factionschat.listeners;

import com.failprooftech.factionschat.chat.BukkitLegacyPermissionChatMessage;
import com.failprooftech.factionschat.chat.ChatPermissions;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.util.ChatTxt;

import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;

import net.md_5.bungee.api.chat.BaseComponent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * DiscordSRV integration on Spigot (non-Paper): staff relay uses Bungee legacy components
 * so {@code §} color codes render (plain {@link Bukkit#broadcast(String, String)} may not).
 *
 * <p>Uses {@link DiscordGuildMessagePostProcessEvent#getProcessedMessage()} (DiscordSRV deprecated string view of the
 * same in-game line as {@link DiscordGuildMessagePostProcessEvent#getMinecraftMessage()}) so we do not depend on
 * Paper's Adventure {@code Server} methods at runtime. Game-chat formatting likewise stays on
 * {@link BukkitLegacyPermissionChatMessage} / {@link ChatTxt} so Adventure is never class-loaded.</p>
 */
public final class DiscordSRVSpigotListener extends DiscordSRVListenerBase
{
    @Override
    protected String formatPlayerBodyForDiscord(Player player, String body)
    {
        ChatPermissions perms = getPlayerChatPermissions(player);
        return BukkitLegacyPermissionChatMessage.toBukkitLegacyString(body, perms, "");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void deliverStaffDiscordToMinecraft(DiscordGuildMessagePostProcessEvent event)
    {
        String body = event.getProcessedMessage();
        if (body == null)
        {
            body = "";
        }
        String line = trustedConfigSnippetToLegacy(Settings.ChatPrefixes.STAFF) + " " + body;
        BaseComponent[] components = SpigotFactionChatListener.toSendableComponents(line, false);
        final String consolePlain = ChatTxt.stripColorLegacy(line);
        runSync(() ->
        {
            for (Player player : Bukkit.getOnlinePlayers())
            {
                if (player.hasPermission("factions.chat.staff"))
                {
                    player.spigot().sendMessage(components);
                }
            }
            Bukkit.getConsoleSender().sendMessage(consolePlain);
        });
    }

    /** MassiveCore tags + {@code &} codes only — no Adventure / MiniMessage on Spigot. */
    private static String trustedConfigSnippetToLegacy(String snippet)
    {
        if (snippet == null || snippet.isEmpty())
        {
            return "";
        }
        return ChatTxt.parseLegacy('&', ChatTxt.parse(snippet));
    }
}
