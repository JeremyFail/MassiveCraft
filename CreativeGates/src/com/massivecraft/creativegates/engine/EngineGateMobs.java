package com.massivecraft.creativegates.engine;

import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.gate.mobdetect.GateMobDetect;
import com.massivecraft.creativegates.util.GateEntityTeleport;
import com.massivecraft.massivecore.Engine;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared mob/vehicle gate use logic (debounce, transport) and lifecycle hook for {@link GateMobDetect}.
 * <p>
 * Living-mob detection is platform-specific (Paper event vs Spigot scan). Non-living vehicles
 * (boats, minecarts) use Bukkit {@link VehicleMoveEvent}, available on both Spigot and Paper.
 * </p>
 */
public class EngineGateMobs extends Engine
{
	private static final EngineGateMobs i = new EngineGateMobs();
	public static EngineGateMobs get() { return i; }

	private static final long GATE_USE_DEBOUNCE_MILLIS = 1000L;
	private static final Map<UUID, Long> RECENT_GATE_USE_BY_ENTITY = new ConcurrentHashMap<>();

	/**
	 * Records that an entity (and optionally its party) just used a gate, for debounce.
	 */
	public static void markRecentGateUse(Entity trigger)
	{
		if (trigger == null) return;
		long now = System.currentTimeMillis();
		for (UUID id : GateEntityTeleport.partyIds(trigger))
		{
			RECENT_GATE_USE_BY_ENTITY.put(id, now);
		}
	}

	/**
	 * Attempts to send a non-player entity (wandering mob or eligible vehicle) through a gate.
	 *
	 * @param entity The entity to transport.
	 * @param gate The gate to transport the entity through.
	 * @return {@code true} if the entity was transported.
	 */
	public static boolean tryUseGate(Entity entity, UGate gate)
	{
		if (entity == null || gate == null) return false;
		if (!MConf.get().isEnabled()) return false;
		if (!gate.isEnterEnabled()) return false;
		if (!GateEntityTeleport.isEligibleNonPlayerTrigger(entity)) return false;

		if (entity instanceof LivingEntity)
		{
			if (!gate.isAllowMobs()) return false;
		}
		else if (!gate.isAllowVehicles())
		{
			return false;
		}

		UUID id = entity.getUniqueId();
		if (wasRecentGateUse(id)) return false;
		if (!gate.isIntact())
		{
			gate.destroy();
			return false;
		}

		if (!gate.transportEntity(entity)) return false;
		markRecentGateUse(entity);
		return true;
	}

	/**
	 * Checks if the entity has recently used a gate.
	 * 
	 * @param entityId The unique ID of the entity.
	 * @return True if the entity has recently used a gate.
	 */
	private static boolean wasRecentGateUse(UUID entityId)
	{
		Long recent = RECENT_GATE_USE_BY_ENTITY.get(entityId);
		if (recent == null) return false;
		if ((System.currentTimeMillis() - recent) >= GATE_USE_DEBOUNCE_MILLIS)
		{
			RECENT_GATE_USE_BY_ENTITY.remove(entityId);
			return false;
		}
		return true;
	}

	/**
	 * Boats / minecarts (and other non-living vehicles) on Spigot and Paper.
	 * Living vehicles are left to the living-mob backends to avoid duplicate work.
	 * 
	 * @param event The vehicle move event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onVehicleMove(VehicleMoveEvent event)
	{
		if (!MConf.get().isEnabled()) return;
		if (!MConf.get().isGatesAllowVehicles()) return;

		Entity vehicle = event.getVehicle();
		if (!GateEntityTeleport.isEligibleGateVehicle(vehicle)) return;

		Location to = event.getTo();
		Location from = event.getFrom();
		if (to == null) return;

		if (from.getBlockX() == to.getBlockX()
			&& from.getBlockY() == to.getBlockY()
			&& from.getBlockZ() == to.getBlockZ())
		{
			return;
		}

		UGate gate = EngineMain.getGateIntersectingEntity(vehicle, to);
		if (gate == null) return;

		tryUseGate(vehicle, gate);
	}

	@Override
	public void setActiveInner(boolean active)
	{
		GateMobDetect.setActive(active);
	}
}
