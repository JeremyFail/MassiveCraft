package com.massivecraft.factionschat.listeners;

import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.ChatPrefixes;
import com.massivecraft.factionschat.FactionsChat;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DiscordSRVListener implements Listener
{
    @EventHandler
    public void onGameChatMessage(GameChatMessagePreProcessEvent event)
    {
        ChatMode cm = FactionChatListener.qmPlayers.containsKey(event.getPlayer().getUniqueId()) 
                ? FactionChatListener.qmPlayers.get(event.getPlayer().getUniqueId()) 
                : FactionsChat.instance.getPlayerChatModes().getOrDefault(event.getPlayer().getUniqueId(), ChatMode.GLOBAL);

        if (cm == ChatMode.GLOBAL)
        {
            return;
        } 
        else if (cm == ChatMode.STAFF)
        {
            event.setChannel("staff");
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onMessageReceive(DiscordGuildMessagePostProcessEvent event)
    {
        String channelId = event.getChannel().getId();
        if (!DiscordSRV.getPlugin().getChannels().get("staff").equals(channelId))
        {
            return;
        }

        // Cancel event
        event.setCancelled(true);

        // Broadcast message in staff channel
        Bukkit.broadcast(ChatPrefixes.STAFF + event.getProcessedMessage(), "factionschat.staff");
    }

    @EventHandler
    public void onDiscordReady(DiscordReadyEvent event)
    {
        String staffChannelId = FactionsChat.instance.getConfig().getString("DiscordStaffChannel", "000000000000000000");
        DiscordSRV.getPlugin().getChannels().put("staff", staffChannelId);
        FactionsChat.instance.getLogger().info("Registered channel ID " + DiscordSRV.getPlugin().getChannels().get("staff") + " for staff chat");
    }
}
