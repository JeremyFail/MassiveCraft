package com.massivecraft.massivecore.engine;

import com.massivecraft.massivecore.gson.JsonObject;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.event.EventMassiveCorePlayerLeave;
import com.massivecraft.massivecore.event.EventMassiveCoreSenderRegister;
import com.massivecraft.massivecore.event.EventMassiveCoreSenderUnregister;
import com.massivecraft.massivecore.store.Coll;
import com.massivecraft.massivecore.store.SenderColl;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles automatic store sync on login/leave and maintains {@code SenderColl} sender references.
 * <p>
 * Platform-specific login timing (Spigot {@code PlayerLoginEvent} vs Paper {@code PlayerJoinEvent}) lives in
 * {@link com.massivecraft.massivecore.engine.loginpipeline.LoginPipeline}. This engine holds the shared logic
 * and events that are identical on both platforms ({@code AsyncPlayerPreLoginEvent}, quit, sender register).
 */
public class EngineMassiveCoreDatabase extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static EngineMassiveCoreDatabase i = new EngineMassiveCoreDatabase();
	public static EngineMassiveCoreDatabase get() { return i; }
	
	// -------------------------------------------- //
	// PLAYER AND SENDER REFERENCES
	// -------------------------------------------- //
	// Sender refs are wired during login/join by LoginPipeline*Listener.
	// Connection IP is cached separately because Player#getAddress() is often null during early connect.
	
	/** UUID string → client address, populated at async prelogin and cleared on deny/quit. */
	public static Map<String, InetAddress> idToConnectionAddress = new MassiveMap<>();
	
	/**
	 * Immediately sets the {@code SenderColl} sender reference for every coll instance.
	 * When {@code reference} is null, also clears the cached connection address for that id.
	 */
	public static void setSenderReferences(CommandSender sender, CommandSender reference)
	{
		// Check Sender 
		if (MUtil.isntSender(sender)) return;
		
		// Get and Check Id
		String id = IdUtil.getId(sender);
		if (id == null) return;
		
		// Avoid Race Condition
		// Explanation: Deferred removal could potentially happen after a new join.
		if (reference == null && sender instanceof Player && ((Player)sender).isOnline()) return;
		
		// Set References
		SenderColl.setSenderReferences(id, reference);
		
		if (reference == null)
		{
			idToConnectionAddress.remove(id);
		}
	}
	
	// Same as above but next tick.
	public static void setSenderReferencesSoon(final CommandSender sender, final CommandSender reference)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(MassiveCore.get(), () -> setSenderReferences(sender, reference));
	}
	
	/** Stores or removes the cached connection address for {@code MUtil#getIp} fallback lookup. */
	public static void cacheConnectionAddress(String id, InetAddress address)
	{
		if (id == null) return;
		
		if (address == null)
		{
			idToConnectionAddress.remove(id);
		}
		else
		{
			idToConnectionAddress.put(id, address);
		}
	}
	
	/** Clears sender refs and connection address immediately (no deferred online check). */
	public static void clearConnectionData(String id)
	{
		if (id == null) return;
		
		SenderColl.setSenderReferences(id, null);
		idToConnectionAddress.remove(id);
	}
	
	/** Clears sender refs and connection address on the next tick. Used when no {@code Player} exists yet. */
	public static void clearConnectionDataSoon(final String id)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(MassiveCore.get(), () -> clearConnectionData(id));
	}
	
	/** Returns the cached address for the given player id, or null if none is stored. */
	public static InetAddress getCachedConnectionAddress(String id)
	{
		if (id == null) return null;
		return idToConnectionAddress.get(id);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void setSenderReferencesQuitMonitor(PlayerQuitEvent event)
	{
		// PlayerQuitEvents are /probably/ trustworthy.
		// We check ourselves the next tick just to be on the safe side.
		setSenderReferencesSoon(event.getPlayer(), null);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void setSenderReferencesRegisterMonitor(EventMassiveCoreSenderRegister event)
	{
		// This one we can however trust.
		setSenderReferences(event.getSender(), event.getSender());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void setSenderReferencesUnregisterMonitor(EventMassiveCoreSenderUnregister event)
	{
		// This one we can however trust.
		setSenderReferences(event.getSender(), null);
	}
	
	// -------------------------------------------- //
	// SYNC: LOGIN
	// -------------------------------------------- //
	// Async prefetch in massiveStoreLoginSync(AsyncPlayerPreLoginEvent); synchronous hydration in
	// hydrateStoreOnJoin(Player), invoked by LoginPipeline*Listener at login/join LOWEST.
	
	protected Map<String, Map<SenderColl<?>, Entry<JsonObject, Long>>> idToRemoteEntries = new ConcurrentHashMap<>();
	
	// Intended to be ran asynchronously.
	public void storeRemoteEntries(final String playerId)
	{
		// Create remote entries ...
		Map<SenderColl<?>, Entry<JsonObject, Long>> remoteEntries = createRemoteEntries(playerId);
		
		// ... store them ...
		this.idToRemoteEntries.put(playerId, remoteEntries);
		
		// ... and make sure they are removed after 30 seconds.
		// Without this we might cause a memory leak.
		// Players might trigger AsyncPlayerPreLoginEvent but not complete join.
		// Using WeakHashMap is not an option since the player object does not exist at AsyncPlayerPreLoginEvent.
		Bukkit.getScheduler().runTaskLaterAsynchronously(this.getPlugin(), () -> idToRemoteEntries.remove(playerId), 20*30);
	}
	
	/** Whether async prelogin has prefetched DB rows not yet consumed by {@link #hydrateStoreOnJoin}. */
	public boolean hasPendingRemoteEntries(String playerId)
	{
		return this.idToRemoteEntries.containsKey(playerId);
	}
	
	// Intended to be ran synchronously.
	// It will use remoteEntries from AsyncPlayerPreLoginEvent if possible.
	// If no such remoteEntries are available it will create them and thus lock the main server thread a bit.
	public Map<SenderColl<?>, Entry<JsonObject, Long>> getRemoteEntries(String playerId)
	{
		// If there are stored remote entries we used those ...
		Map<SenderColl<?>, Entry<JsonObject, Long>> ret = idToRemoteEntries.remove(playerId);	
		if (ret != null) return ret;
		
		// ... otherwise we create brand new ones.
		return createRemoteEntries(playerId);
	}
	
	// Used by the two methods above.
	public Map<SenderColl<?>, Entry<JsonObject, Long>> createRemoteEntries(String playerId)
	{
		// Create Ret
		Map<SenderColl<?>, Entry<JsonObject, Long>> ret = new HashMap<>();
		
		// Fill Ret
		for (final SenderColl<?> coll : Coll.getSenderInstances())
		{
			Entry<JsonObject, Long> remoteEntry = coll.getDb().load(coll, playerId);
			ret.put(coll, remoteEntry);
		}
		
		// Return Ret
		return ret;
	}
	
	/**
	 * Syncs prefetched (or freshly loaded) remote store entries into memory for the joining player.
	 * Called from LoginPipeline at Spigot login LOWEST or Paper join LOWEST.
	 */
	public void hydrateStoreOnJoin(Player player)
	{
		if (MUtil.isntPlayer(player)) return;
		final String playerId = player.getUniqueId().toString();
		
		Map<SenderColl<?>, Entry<JsonObject, Long>> remoteEntries = getRemoteEntries(playerId);
		
		for (Entry<SenderColl<?>, Entry<JsonObject, Long>> entry : remoteEntries.entrySet())
		{
			SenderColl<?> coll = entry.getKey();
			Entry<JsonObject, Long> remoteEntry = entry.getValue();
			coll.syncId(playerId, null, remoteEntry);
		}
	}
	
	// Cache address early; MONITOR handler clears it if prelogin is denied.
	@EventHandler(priority = EventPriority.LOWEST)
	public void cacheConnectionAddressPreLoginLowest(AsyncPlayerPreLoginEvent event)
	{
		cacheConnectionAddress(event.getUniqueId().toString(), event.getAddress());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void massiveStoreLoginSync(AsyncPlayerPreLoginEvent event)
	{
		// If the login was allowed ...
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
		{
			// Prelogin LOWEST may have cached an address; remove refs and cache on deny.
			clearConnectionData(event.getUniqueId().toString());
			return;
		}
		
		// ... get player id ...
		final String playerId = event.getUniqueId().toString();
		
		// ... and store the remote entries.
		this.storeRemoteEntries(playerId);
	}
	
	// -------------------------------------------- //
	// SYNC: LEAVE
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void syncOnPlayerLeave(EventMassiveCorePlayerLeave event)
	{
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		final String id = player.getUniqueId().toString();
		for (SenderColl<?> coll : Coll.getSenderInstances())
		{
			coll.syncId(id);
		}
	}
	
}
