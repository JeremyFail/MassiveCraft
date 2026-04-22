package com.massivecraft.factionschat.listeners;

import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.util.ColonChannelChatParser;
import com.massivecraft.factionschat.util.ColonChannelChatParser.ParseType;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;

/**
 * Shared DiscordSRV API subscription: game chat routing, staff channel registration, and staff Discord→Minecraft relay.
 * Subclasses implement {@link #deliverStaffDiscordToMinecraft} for Paper (Adventure) vs Spigot (legacy string) servers.
 */
public abstract class DiscordSRVListenerBase extends FactionChatListenerBase
{
    /**
     * Sends a staff-channel Discord message to Minecraft using the platform-appropriate pipeline.
     *
     * @param event Post-process event; already matched to the configured staff channel and cancelled for DiscordSRV.
     */
    protected abstract void deliverStaffDiscordToMinecraft(DiscordGuildMessagePostProcessEvent event);

    /**
     * Runs after the chat message is displayed to the player in-game.
     * @param event The event containing the chat message.
     */
    @Subscribe
    public void onGameChatMessage(GameChatMessagePreProcessEvent event)
    {
        ColonChannelChatParser.ParseResult colon = ColonChannelChatParser.parse(event.getPlayer(), event.getMessage());
        if (colon.getType() == ParseType.INVALID || colon.getType() == ParseType.TOGGLE)
        {
            event.setCancelled(true);
            return;
        }

        ChatMode cm;
        if (colon.getType() == ParseType.QUICK_MESSAGE)
        {
            cm = colon.getTargetMode();
        }
        else
        {
            cm = FactionsChat.instance.getPlayerChatModes().getOrDefault(event.getPlayer().getUniqueId(), ChatMode.GLOBAL);
        }

        // Strip all formatting from the message before we send it to Discord
        event.setMessage(DiscordSRVChatRelayFormatter.playerBodyToDiscordLegacy(event.getPlayer(), colon.getMessageBody(), this));

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

    @Subscribe
    public void onMessageReceive(DiscordGuildMessagePostProcessEvent event)
    {
        String channelId = event.getChannel().getId();
        String staffChannelId = DiscordSRV.getPlugin().getChannels().get("staff");
        if (staffChannelId == null || !staffChannelId.equals(channelId))
        {
            return;
        }

        event.setCancelled(true);
        deliverStaffDiscordToMinecraft(event);
    }

    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event)
    {
        String staffChannelId = FactionsChat.instance.getConfig().getString("DiscordSRV.StaffChannel", "000000000000000000");
        DiscordSRV.getPlugin().getChannels().put("staff", staffChannelId);
        FactionsChat.instance.getLogger().info("Registered channel ID " + DiscordSRV.getPlugin().getChannels().get("staff") + " for staff chat");
    }
}
