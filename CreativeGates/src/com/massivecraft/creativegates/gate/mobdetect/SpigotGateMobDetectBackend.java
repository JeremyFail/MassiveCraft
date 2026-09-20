package com.massivecraft.creativegates.gate.mobdetect;

import com.massivecraft.creativegates.engine.EngineGateMobs;
import com.massivecraft.creativegates.engine.EngineMain;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.util.GateEntityTeleport;
import com.massivecraft.massivecore.MassivePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spigot detection: poll intact gates whose content chunks are loaded.
 */
public final class SpigotGateMobDetectBackend implements GateMobDetectBackend
{
	private static final Set<UUID> ENTITIES_INSIDE_GATES = ConcurrentHashMap.newKeySet();

	private BukkitTask task;

	@Override
	public String getName()
	{
		return "Spigot";
	}

	@Override
	public void setActive(MassivePlugin plugin, boolean active)
	{
		if (this.task != null)
		{
			this.task.cancel();
			this.task = null;
		}
		ENTITIES_INSIDE_GATES.clear();

		if (!active || plugin == null) return;

		long period = Math.max(1L, MConf.get() != null ? MConf.get().getGatesAllowMobsScanTicks() : 10);
		this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
	}

	private void tick()
	{
		if (!MConf.get().isEnabled()) return;
		if (!MConf.get().isGatesAllowMobs()) return;

		Set<UUID> nowInside = new HashSet<>();

		for (UGate gate : UGateColl.get().getAll())
		{
			if (gate == null) continue;
			if (!gate.isAllowMobs()) continue;
			if (!gate.isEnterEnabled()) continue;
			if (!gate.isIntact()) continue;
			if (!isGateContentChunkLoaded(gate)) continue;

			BoundingBox box = boundingBoxOf(gate);
			if (box == null) continue;

			World world;
			try
			{
				world = gate.getExit().asBukkitWorld(true);
			}
			catch (IllegalStateException e)
			{
				continue;
			}
			if (world == null) continue;

			Location center = box.getCenter().toLocation(world);
			double radius = Math.max(box.getWidthX(), Math.max(box.getHeight(), box.getWidthZ())) / 2.0 + 1.0;

			for (Entity entity : world.getNearbyEntities(center, radius, radius, radius))
			{
				if (!(entity instanceof LivingEntity)) continue;
				LivingEntity living = (LivingEntity) entity;
				if (!GateEntityTeleport.isEligibleWanderingMob(living)) continue;
				if (!box.overlaps(living.getBoundingBox())) continue;
				if (EngineMain.getGateIntersectingEntity(living, living.getLocation()) != gate) continue;

				UUID id = living.getUniqueId();
				nowInside.add(id);

				if (ENTITIES_INSIDE_GATES.contains(id)) continue;

				EngineGateMobs.tryUseGate(living, gate);
			}
		}

		ENTITIES_INSIDE_GATES.clear();
		ENTITIES_INSIDE_GATES.addAll(nowInside);
	}

	private static boolean isGateContentChunkLoaded(UGate gate)
	{
		List<Block> blocks = gate.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return false;

		World world = null;
		for (Block block : blocks)
		{
			if (world == null) world = block.getWorld();
			if (world == null) return false;
			if (world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return true;
		}
		return false;
	}

	private static BoundingBox boundingBoxOf(UGate gate)
	{
		List<Block> blocks = gate.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return null;

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;

		for (Block block : blocks)
		{
			minX = Math.min(minX, block.getX());
			minY = Math.min(minY, block.getY());
			minZ = Math.min(minZ, block.getZ());
			maxX = Math.max(maxX, block.getX());
			maxY = Math.max(maxY, block.getY());
			maxZ = Math.max(maxZ, block.getZ());
		}

		return new BoundingBox(minX, minY - 0.25, minZ, maxX + 1.0, maxY + 1.25, maxZ + 1.0);
	}
}
