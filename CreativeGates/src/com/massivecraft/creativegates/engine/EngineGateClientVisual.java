package com.massivecraft.creativegates.engine;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.massivecore.Engine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps client-only gate fills visible via {@link Player#sendBlockChange}.
 */
public class EngineGateClientVisual extends Engine
{
	private static final EngineGateClientVisual i = new EngineGateClientVisual();
	public static EngineGateClientVisual get() { return i; }
	
	private static final int VIEW_DISTANCE_BLOCKS = 64;
	private static final long MOVE_SYNC_INTERVAL_MS = 750L;
	
	private final Map<UUID, Long> lastMoveSyncMillis = new HashMap<>();
	
	/**
	 * Sends the client display for a gate to all nearby players (or clears to real world state).
	 */
	public void syncGate(UGate gate)
	{
		if (gate == null) return;
		
		List<Block> blocks = gate.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return;
		World world = blocks.get(0).getWorld();
		if (world == null) return;
		
		boolean clientVisual = gate.usesClientVisualFill();
		GateType type = gate.getFillType();
		BlockData display = null;
		if (clientVisual && type != null)
		{
			Material material = type.getClientDisplayMaterial();
			if (material != null && material.isBlock())
			{
				display = material.createBlockData();
			}
		}
		
		for (Player player : world.getPlayers())
		{
			if (!isNear(player.getLocation(), blocks.get(0).getLocation())) continue;
			sendBlocks(player, blocks, display);
		}
	}
	
	/**
	 * Clears client overlays for a gate (show real server blocks / air).
	 */
	public void clearGate(UGate gate)
	{
		if (gate == null) return;
		
		List<Block> blocks = gate.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return;
		World world = blocks.get(0).getWorld();
		if (world == null) return;
		
		for (Player player : world.getPlayers())
		{
			if (!isNear(player.getLocation(), blocks.get(0).getLocation())) continue;
			sendBlocks(player, blocks, null);
		}
	}
	
	/**
	 * Syncs all client-visual gates near a player.
	 */
	public void syncPlayer(Player player)
	{
		if (player == null) return;
		World world = player.getWorld();
		
		for (UGate gate : UGateColl.get().getAll())
		{
			if (gate == null || !gate.usesClientVisualFill()) continue;
			
			List<Block> blocks = gate.getContentBlocks();
			if (blocks == null || blocks.isEmpty()) continue;
			if (blocks.get(0).getWorld() != world) continue;
			if (!isNear(player.getLocation(), blocks.get(0).getLocation())) continue;
			
			GateType type = gate.getFillType();
			if (type == null) continue;
			Material material = type.getClientDisplayMaterial();
			if (material == null || !material.isBlock()) continue;
			sendBlocks(player, blocks, material.createBlockData());
		}
	}
	
	private static void sendBlocks(Player player, List<Block> blocks, BlockData displayOrNull)
	{
		for (Block block : blocks)
		{
			Location loc = block.getLocation();
			if (displayOrNull != null)
			{
				player.sendBlockChange(loc, displayOrNull);
			}
			else
			{
				player.sendBlockChange(loc, block.getBlockData());
			}
		}
	}
	
	private static boolean isNear(Location playerLoc, Location gateLoc)
	{
		if (playerLoc == null || gateLoc == null) return false;
		if (playerLoc.getWorld() != gateLoc.getWorld()) return false;
		return playerLoc.distanceSquared(gateLoc) <= (double) VIEW_DISTANCE_BLOCKS * VIEW_DISTANCE_BLOCKS;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event)
	{
		Bukkit.getScheduler().runTaskLater(CreativeGates.get(), () -> syncPlayer(event.getPlayer()), 20L);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldChange(PlayerChangedWorldEvent event)
	{
		Bukkit.getScheduler().runTaskLater(CreativeGates.get(), () -> syncPlayer(event.getPlayer()), 5L);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event)
	{
		Bukkit.getScheduler().runTaskLater(CreativeGates.get(), () -> syncPlayer(event.getPlayer()), 5L);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onMove(PlayerMoveEvent event)
	{
		Location from = event.getFrom();
		Location to = event.getTo();
		if (to == null) return;
		if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
		
		Player player = event.getPlayer();
		long now = System.currentTimeMillis();
		Long last = this.lastMoveSyncMillis.get(player.getUniqueId());
		if (last != null && now - last < MOVE_SYNC_INTERVAL_MS) return;
		this.lastMoveSyncMillis.put(player.getUniqueId(), now);
		
		this.syncPlayer(player);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event)
	{
		this.lastMoveSyncMillis.remove(event.getPlayer().getUniqueId());
	}
}
