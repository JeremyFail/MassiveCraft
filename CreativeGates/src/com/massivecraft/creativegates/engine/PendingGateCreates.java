package com.massivecraft.creativegates.engine;

import com.massivecraft.creativegates.engine.create.PendingGateCreate;
import com.massivecraft.massivecore.Engine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One pending gate create per player. Cleared on quit or cancel; no timeout.
 */
public class PendingGateCreates extends Engine
{
	private static final PendingGateCreates i = new PendingGateCreates();
	public static PendingGateCreates get() { return i; }
	
	private final Map<UUID, PendingGateCreate> pendingByPlayer = new ConcurrentHashMap<>();
	
	public PendingGateCreate get(Player player)
	{
		if (player == null) return null;
		return this.pendingByPlayer.get(player.getUniqueId());
	}
	
	public PendingGateCreate get(UUID playerId)
	{
		if (playerId == null) return null;
		return this.pendingByPlayer.get(playerId);
	}
	
	public void put(PendingGateCreate pending)
	{
		if (pending == null) return;
		this.pendingByPlayer.put(pending.getPlayerId(), pending);
	}
	
	public PendingGateCreate remove(Player player)
	{
		if (player == null) return null;
		return this.pendingByPlayer.remove(player.getUniqueId());
	}
	
	public PendingGateCreate remove(UUID playerId)
	{
		if (playerId == null) return null;
		return this.pendingByPlayer.remove(playerId);
	}
	
	public boolean has(Player player)
	{
		return this.get(player) != null;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event)
	{
		this.remove(event.getPlayer());
	}
}
