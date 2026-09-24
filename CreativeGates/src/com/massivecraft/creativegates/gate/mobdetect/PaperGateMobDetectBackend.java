package com.massivecraft.creativegates.gate.mobdetect;

import com.massivecraft.creativegates.engine.EngineGateMobs;
import com.massivecraft.creativegates.engine.EngineMain;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.util.GateEntityTeleport;
import com.massivecraft.massivecore.MassivePlugin;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/**
 * Paper detection: {@link EntityMoveEvent} when a living mob changes block into a gate.
 * <p>
 * Loaded reflectively by {@link GateMobDetect} only when Paper APIs are present.
 * </p>
 */
public final class PaperGateMobDetectBackend implements GateMobDetectBackend, GateMobDetectBackend.CapabilityProbe, Listener
{
	@Override
	public boolean isAvailable()
	{
		try
		{
			Class.forName("io.papermc.paper.event.entity.EntityMoveEvent");
			return true;
		}
		catch (ClassNotFoundException e)
		{
			return false;
		}
	}

	@Override
	public String getName()
	{
		return "Paper";
	}

	@Override
	public void setActive(MassivePlugin plugin, boolean active)
	{
		HandlerList.unregisterAll(this);
		if (active && plugin != null)
		{
			Bukkit.getPluginManager().registerEvents(this, plugin);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityMove(EntityMoveEvent event)
	{
		if (!MConf.get().isEnabled()) return;
		if (!MConf.get().isGatesAllowMobs()) return;
		if (!(event.getEntity() instanceof LivingEntity)) return;
		LivingEntity living = (LivingEntity) event.getEntity();
		if (!GateEntityTeleport.isEligibleWanderingMob(living)) return;

		Location to = event.getTo();
		Location from = event.getFrom();
		if (to == null) return;

		if (from.getBlockX() == to.getBlockX()
			&& from.getBlockY() == to.getBlockY()
			&& from.getBlockZ() == to.getBlockZ())
		{
			return;
		}

		UGate gate = EngineMain.getGateIntersectingEntity(living, to);
		if (gate == null) return;

		EngineGateMobs.tryUseGate(living, gate);
	}
}
