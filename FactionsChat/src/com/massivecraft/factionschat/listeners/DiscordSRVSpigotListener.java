package com.massivecraft.factionschat.listeners;

import com.massivecraft.factionschat.config.Settings;

import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;

import org.bukkit.Bukkit;

/**
 * DiscordSRV integration on Spigot (non-Paper): staff relay uses legacy {@link Bukkit#broadcast(String, String)}.
 *
 * <p>Uses {@link DiscordGuildMessagePostProcessEvent#getProcessedMessage()} (DiscordSRV deprecated string view of the
 * same in-game line as {@link DiscordGuildMessagePostProcessEvent#getMinecraftMessage()}) so we do not depend on
 * Paper's Adventure {@code Server} methods at runtime.</p>
 */
public final class DiscordSRVSpigotListener extends DiscordSRVListenerBase
{
    @Override
    @SuppressWarnings("deprecation")
    protected void deliverStaffDiscordToMinecraft(DiscordGuildMessagePostProcessEvent event)
    {
        String body = event.getProcessedMessage();
        if (body == null)
        {
            body = "";
        }
        String line = DiscordSRVChatRelayFormatter.trustedConfigSnippetToLegacy(Settings.ChatPrefixes.STAFF) + " " + body;
        runSync(() -> Bukkit.broadcast(line, "factions.chat.staff"));
    }
}
