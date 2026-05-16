package com.failprooftech.factionschat.listeners;

import com.failprooftech.factionschat.config.Settings;

import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import org.bukkit.Bukkit;

/**
 * DiscordSRV integration on Paper: staff relay uses Adventure {@link org.bukkit.Server#broadcast(Component, String)}.
 *
 * <p>DiscordSRV supplies a shaded Adventure {@link github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component};
 * it is bridged through JSON to {@code net.kyori} for the Paper API.</p>
 */
public final class DiscordSRVPaperListener extends DiscordSRVListenerBase
{
    @Override
    protected void deliverStaffDiscordToMinecraft(DiscordGuildMessagePostProcessEvent event)
    {
        github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component shadedBody = event.getMinecraftMessage();
        Component bodyNetKyori;
        if (shadedBody == null)
        {
            bodyNetKyori = Component.empty();
        }
        else
        {
            String asJson = github.scarsz.discordsrv.dependencies.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                .serialize(shadedBody);
            bodyNetKyori = GsonComponentSerializer.gson().deserialize(asJson);
        }
        Component out = DiscordSRVChatRelayFormatter.trustedConfigSnippetToComponent(Settings.ChatPrefixes.STAFF).append(Component.text(" ")).append(bodyNetKyori);
        runSync(() -> Bukkit.getServer().broadcast(out, "factions.chat.staff"));
    }
}
