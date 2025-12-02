package com.massivecraft.factionschat.listeners;

import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.config.Settings;
import com.massivecraft.factionschat.FactionsChat;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.api.Subscribe;

import org.bukkit.Bukkit;

/**
 * An event listener that hooks into the DiscordSRV plugin to send staff
 * chats to a discord server channel. Requires the server have DiscordSRV
 * installed and the config file to be setup with the Discord channel ID.
 */
public class DiscordSRVListener
{
    /**
     * Handles game chat messages and redirects them to the appropriate Discord channel
     * based on the player's chat mode.
     *
     * @param event The game chat message pre-process event.
     */
    @Subscribe
    public void onGameChatMessage(GameChatMessagePreProcessEvent event)
    {
        ChatMode cm = FactionsChat.qmPlayers.containsKey(event.getPlayer().getUniqueId()) 
                ? FactionsChat.qmPlayers.get(event.getPlayer().getUniqueId()) 
                : FactionsChat.instance.getPlayerChatModes().getOrDefault(event.getPlayer().getUniqueId(), ChatMode.GLOBAL);

        // Send global messages to default Discord channel
        if (cm == ChatMode.GLOBAL)
        {
            return;
        } 
        // Send staff messages to the staff channel
        else if (cm == ChatMode.STAFF)
        {
            event.setChannel("staff");
            return;
        }

        // Ignore all other chat modes
        event.setCancelled(true);
    }

    // TODO: Look into deprecation of getProcessedMessage() in DiscordSRV
    /**
     * Handles messages sent in the Discord staff channel and broadcasts them
     * to the server's staff chat.
     *
     * @param event The Discord message post-process event.
     */
    @Subscribe
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
        Bukkit.broadcast(Settings.ChatPrefixes.STAFF + event.getProcessedMessage(), "factions.chat.staff");
    }

    /**
     * Handles the DiscordSRV plugin being ready and registers the staff channel ID.
     *
     * @param event The Discord ready event.
     */
    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event)
    {
        String staffChannelId = FactionsChat.instance.getConfig().getString("DiscordSRV.StaffChannel", "000000000000000000");
        DiscordSRV.getPlugin().getChannels().put("staff", staffChannelId);
        FactionsChat.instance.getLogger().info("Registered channel ID " + DiscordSRV.getPlugin().getChannels().get("staff") + " for staff chat");
    }
}
