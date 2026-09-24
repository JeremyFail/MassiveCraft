package com.massivecraft.massivecore.engine;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.entity.MassiveCoreMConf;
import com.massivecraft.massivecore.event.EventMassiveCoreAfterPlayerRespawn;
import com.massivecraft.massivecore.event.EventMassiveCoreAfterPlayerTeleport;
import com.massivecraft.massivecore.event.EventMassiveCorePermissionDeniedFormat;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.SmokeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EngineMassiveCoreMain extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static EngineMassiveCoreMain i = new EngineMassiveCoreMain();
	public static EngineMassiveCoreMain get() { return i; }
	
	// -------------------------------------------- //
	// PERMISSION DENIED FORMAT
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void permissionDeniedFormat(EventMassiveCorePermissionDeniedFormat event)
	{
		// If noone set a format already ...
		if (event.hasFormat()) return;
		
		// ... and we have a custom format in the config ...
		String customFormat = MassiveCoreMConf.get().getPermissionDeniedFormat(event.getPermissionId());
		if (customFormat == null) return;
		
		// ... then make use of that format.
		event.setFormat(customFormat);
	}
	
	// -------------------------------------------- //
	// EXPLOSION FX
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void explosionFx(EntityDamageByBlockEvent event)
	{
		// If an entity is taking damage from a block explosion ...
		DamageCause cause = event.getCause();
		if (cause != DamageCause.BLOCK_EXPLOSION) return;
		
		// ... and that explosion is a fake ...
		if (!SmokeUtil.fakeExplosion.booleanValue()) return;
		
		// ... then cancel the event and the damage.
		event.setCancelled(true);
	}
	
	// -------------------------------------------- //
	// AFTER EVENTS
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void after(PlayerTeleportEvent event)
	{
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(MassiveCore.get(), new EventMassiveCoreAfterPlayerTeleport(event), 0);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void after(PlayerRespawnEvent event)
	{
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(MassiveCore.get(), new EventMassiveCoreAfterPlayerRespawn(event, player.getLocation()), 0);
	}
	
	// -------------------------------------------- //
	// EVENT TOOL: causedByKick
	// -------------------------------------------- //
	
	public static Map<UUID, String> kickedPlayerReasons = new HashMap<>();
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void causedByKick(PlayerKickEvent event)
	{
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		final UUID uuid = player.getUniqueId();
		
		kickedPlayerReasons.put(uuid, event.getReason());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void causedByKick(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		final UUID uuid = player.getUniqueId();
		
		// We do the schedule in order for the set to be correct through out the whole MONITOR priority state.
		Bukkit.getScheduler().scheduleSyncDelayedTask(MassiveCore.get(), () -> kickedPlayerReasons.remove(uuid));
	}
	
}
