package com.massivecraft.creativegates.engine;

import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.gate.mobdetect.GateMobDetect;
import com.massivecraft.creativegates.util.GateEntityTeleport;
import com.massivecraft.massivecore.Engine;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared mob-gate use logic (debounce, transport) and lifecycle hook for {@link GateMobDetect}.
 * <p>
 * Wandering-mob <em>detection</em> is platform-specific (Paper event vs Spigot scan) and lives
 * in {@code gate.mobdetect} backends — not here.
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
	 * Attempts to send a wandering (non-player) living entity through a gate.
	 *
	 * @return {@code true} if the entity was transported.
	 */
	public static boolean tryUseGate(LivingEntity entity, UGate gate)
	{
		if (entity == null || gate == null) return false;
		if (!MConf.get().isEnabled()) return false;
		if (!gate.isAllowMobs()) return false;
		if (!gate.isEnterEnabled()) return false;
		if (!GateEntityTeleport.isEligibleWanderingMob(entity)) return false;

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

	@Override
	public void setActiveInner(boolean active)
	{
		GateMobDetect.setActive(active);
	}
}
