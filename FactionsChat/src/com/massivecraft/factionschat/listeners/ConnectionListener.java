package com.massivecraft.factionschat.listeners;

import com.massivecraft.factionschat.FactionsChat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join/quit events for ignore data management.
 */
public class ConnectionListener implements Listener
{
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        // Load ignore data for the joining player
        FactionsChat.instance.getIgnoreManager().loadPlayerIgnores(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        // Save and unload ignore data for the leaving player
        FactionsChat.instance.getIgnoreManager().saveAndUnloadPlayerIgnores(event.getPlayer().getUniqueId());
    }
}