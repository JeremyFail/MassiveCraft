package com.massivecraft.factions.engine;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.teleport.DestinationWildtp;
import com.massivecraft.factions.util.WildTpSearchOrchestrator;
import com.massivecraft.factions.util.WildTpSearchResult;
import com.massivecraft.factions.WildTpTeleportMode;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.event.EventMassiveCorePlayerLeave;
import com.massivecraft.massivecore.event.EventMassiveCorePlayerPSTeleport;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Engine that owns in-flight {@code /f wildtp} sessions.
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 *   <li>Pace destination search during the stand-still delay (main-thread batches every 5 ticks)</li>
 *   <li>Clear sessions on move/damage/leave without applying cost or cooldown</li>
 *   <li>Apply economy cost and cooldown only after MassiveCore accepts the teleport</li>
 * </ul>
 */
public class EngineWildtp extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static final EngineWildtp i = new EngineWildtp();

	/**
	 * @return singleton engine instance
	 */
	public static EngineWildtp get() { return i; }

	/**
	 * Constructs the engine and sets a 5-tick period for paced search work.
	 */
	private EngineWildtp()
	{
		this.setPeriod(5L);
	}

	/** Active sessions keyed by player id. */
	private final Map<String, Session> sessions = new ConcurrentHashMap<>();

	// -------------------------------------------- //
	// SESSION API
	// -------------------------------------------- //

	/**
	 * Starts (or replaces) a wildtp session for the player.
	 *
	 * @param playerId MassiveCore/MPlayer id
	 * @param destination mutable destination used by MixinTeleport
	 * @param world world being searched
	 * @param mode teleport mode for this attempt
	 * @param chargeCost whether to charge after success
	 * @param applyCooldown whether to record last-teleport time for cooldown (false when bypass)
	 * @param dueMillis epoch millis when the stand-still delay ends
	 * @param resolveBeforeDelay true if destination was already found before scheduling
	 * @return the stored session
	 */
	public Session startSession(String playerId, DestinationWildtp destination, World world, WildTpTeleportMode mode, boolean chargeCost, boolean applyCooldown, long dueMillis, boolean resolveBeforeDelay)
	{
		Session session = new Session(playerId, destination, world.getName(), mode, chargeCost, applyCooldown, dueMillis, resolveBeforeDelay);
		this.sessions.put(playerId, session);
		return session;
	}

	/**
	 * @param playerId player id
	 * @return active session or null
	 */
	public Session getSession(String playerId)
	{
		return this.sessions.get(playerId);
	}

	/**
	 * Clears a session without charging or starting cooldown (cancel / fail).
	 *
	 * @param playerId player id
	 */
	public void clearSession(String playerId)
	{
		this.sessions.remove(playerId);
	}

	/**
	 * Marks search failed, optionally notifies the player, and clears the session without cost/cooldown.
	 *
	 * @param playerId player id
	 * @param message optional MassiveCore color message; null to skip messaging
	 */
	public void failSession(String playerId, String message)
	{
		Session session = this.sessions.remove(playerId);
		if (session != null && session.destination != null)
		{
			session.destination.setSearchFailed();
		}
		if (message != null)
		{
			MixinMessage.get().msgOne(playerId, message);
		}
	}

	/**
	 * Checks whether the player (or their faction bank, per {@code bankFactionPaysCosts}) can afford a cost.
	 *
	 * @param mplayer acting player
	 * @param cost amount to check
	 * @return true if economy is disabled, cost is zero, or the payer has enough
	 */
	public static boolean canAfford(MPlayer mplayer, double cost)
	{
		if (!Econ.isEnabled() || cost <= 0D) return true;
		Faction faction = mplayer.getFaction();
		if (MConf.get().bankEnabled && MConf.get().bankFactionPaysCosts && faction.isNormal())
		{
			return Econ.hasAtLeast(faction, cost, "to use wildtp");
		}
		return Econ.hasAtLeast(mplayer, cost, "to use wildtp");
	}

	// -------------------------------------------- //
	// ENGINE TICK â€” paced search during delay
	// -------------------------------------------- //

	/**
	 * Clears sessions when the engine is deactivated.
	 *
	 * @param active whether the engine is becoming active
	 */
	@Override
	public void setActiveInner(boolean active)
	{
		if (!active)
		{
			this.sessions.clear();
		}
	}

	/**
	 * Periodically advances search-during-delay sessions without extending the player-facing warmup.
	 */
	@Override
	public void run()
	{
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<String, Session>> it = this.sessions.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, Session> entry = it.next();
			Session session = entry.getValue();
			if (session.completed || session.resolveBeforeDelay)
			{
				continue;
			}

			if (session.destination.isResolved())
			{
				continue;
			}

			if (now >= session.dueMillis)
			{
				session.destination.setSearchFailed();
				it.remove();
				continue;
			}

			World world = Bukkit.getWorld(session.worldName);
			if (world == null)
			{
				session.destination.setSearchFailed();
				it.remove();
				continue;
			}

			if (session.attemptsRemaining.get() <= 0)
			{
				if (session.mode == WildTpTeleportMode.HYBRID)
				{
					Player teleportee = IdUtil.getPlayer(session.playerId);
					WildTpSearchResult predefined = WildTpSearchOrchestrator.searchStep(world, WildTpTeleportMode.PREDEFINED, session.attemptsRemaining, 1, teleportee);
					if (predefined != null)
					{
						predefined.applyTo(session.destination);
					}
					else
					{
						session.destination.setSearchFailed();
						it.remove();
					}
				}
				else
				{
					session.destination.setSearchFailed();
					it.remove();
				}
				continue;
			}

			// Small batch per period so Spigot main-thread chunk loads stay bounded
			Player teleportee = IdUtil.getPlayer(session.playerId);
			WildTpSearchResult found = WildTpSearchOrchestrator.searchStep(world, session.mode, session.attemptsRemaining, 8, teleportee);
			if (found != null)
			{
				found.applyTo(session.destination);
			}
		}
	}

	// -------------------------------------------- //
	// SUCCESS: cost + cooldown after teleport accepted
	// -------------------------------------------- //

	/**
	 * Applies cost and cooldown when MassiveCore accepts a wildtp destination teleport.
	 *
	 * @param event teleport event from MassiveCore
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTeleportSuccess(EventMassiveCorePlayerPSTeleport event)
	{
		if (!(event.getDestination() instanceof DestinationWildtp)) return;
		DestinationWildtp dest = (DestinationWildtp) event.getDestination();
		Session session = this.sessions.get(event.getTeleporteeId());
		if (session == null) return;
		if (!dest.isResolved()) return;

		session.completed = true;
		this.sessions.remove(event.getTeleporteeId());

		MPlayer mplayer = MPlayer.get(event.getTeleporteeId());
		if (mplayer == null) return;

		MConf conf = MConf.get();
		if (session.chargeCost && conf.wildTpCostEnabled && conf.wildTpCostAmount > 0D)
		{
			Econ.payForAction(conf.wildTpCostAmount, mplayer, "for wilderness teleport");
		}
		if (session.applyCooldown)
		{
			mplayer.setWildTpLastMillis(System.currentTimeMillis());
		}
	}

	// -------------------------------------------- //
	// CANCEL SESSION (no cost/cooldown)
	// -------------------------------------------- //

	/**
	 * Clears the session when the player changes blocks (mirrors MassiveCore teleport cancel).
	 *
	 * @param event move event
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onMove(PlayerMoveEvent event)
	{
		if (MUtil.isSameBlock(event)) return;
		MPlayer mplayer = MPlayer.get(event.getPlayer());
		if (mplayer != null) this.clearSession(mplayer.getId());
	}

	/**
	 * Clears the session when the player takes damage.
	 *
	 * @param event damage event
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamage(EntityDamageEvent event)
	{
		if (!(event.getEntity() instanceof Player)) return;
		MPlayer mplayer = MPlayer.get(event.getEntity());
		if (mplayer != null) this.clearSession(mplayer.getId());
	}

	/**
	 * Clears the session when the player leaves the server.
	 *
	 * @param event leave event
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onLeave(EventMassiveCorePlayerLeave event)
	{
		MPlayer mplayer = MPlayer.get(event.getPlayer());
		if (mplayer != null) this.clearSession(mplayer.getId());
	}

	// -------------------------------------------- //
	// SESSION TYPE
	// -------------------------------------------- //

	/**
	 * Per-player wildtp attempt state for one delayed teleport.
	 */
	public static class Session
	{
		/** Player id owning this session. */
		public final String playerId;
		/** Destination filled by search and read by MixinTeleport. */
		public final DestinationWildtp destination;
		/** World name being searched. */
		public final String worldName;
		/** Teleport mode for this attempt. */
		public final WildTpTeleportMode mode;
		/** Whether to charge economy after success. */
		public final boolean chargeCost;
		/** Whether to start cooldown after success. */
		public final boolean applyCooldown;
		/** Epoch millis when the stand-still delay expires. */
		public final long dueMillis;
		/** True if the destination was resolved before the delay started. */
		public final boolean resolveBeforeDelay;
		/** Remaining destination attempts for paced search. */
		public final AtomicInteger attemptsRemaining;
		/** Set when teleport success handlers have finished applying rewards. */
		public boolean completed;

		/**
		 * @param playerId player id
		 * @param destination destination handle
		 * @param worldName world name
		 * @param mode teleport mode
		 * @param chargeCost charge after success
		 * @param applyCooldown cooldown after success
		 * @param dueMillis delay expiry millis
		 * @param resolveBeforeDelay whether search already finished
		 */
		public Session(String playerId, DestinationWildtp destination, String worldName, WildTpTeleportMode mode, boolean chargeCost, boolean applyCooldown, long dueMillis, boolean resolveBeforeDelay)
		{
			this.playerId = playerId;
			this.destination = destination;
			this.worldName = worldName;
			this.mode = mode != null ? mode : MConf.get().getWildTpTeleportMode();
			this.chargeCost = chargeCost;
			this.applyCooldown = applyCooldown;
			this.dueMillis = dueMillis;
			this.resolveBeforeDelay = resolveBeforeDelay;
			this.attemptsRemaining = new AtomicInteger(Math.max(1, MConf.get().wildTpMaxAttempts));
		}
	}

}
