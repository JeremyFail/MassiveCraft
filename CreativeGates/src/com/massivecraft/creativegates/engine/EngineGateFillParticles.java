package com.massivecraft.creativegates.engine;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.gate.GateOrientation;
import com.massivecraft.creativegates.gate.fill.GateFillParticleKit;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.massivecore.Engine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ambient fill-themed particles for intact gates near players.
 * <p>
 * Block fills use {@link GateFillParticleKit}. Particle fills spawn the selected particle
 * throughout the interior using {@link MConf#getGateFillParticleAmount()}.
 * </p>
 */
public class EngineGateFillParticles extends Engine
{
	private static final EngineGateFillParticles i = new EngineGateFillParticles();
	public static EngineGateFillParticles get() { return i; }
	
	private static final int VIEW_DISTANCE_BLOCKS = 48;
	private static final long TICK_PERIOD = 4L;
	
	private BukkitTask task;
	private int tick;
	
	@Override
	public void setActiveInner(boolean active)
	{
		if (this.task != null)
		{
			this.task.cancel();
			this.task = null;
		}
		this.tick = 0;
		if (active)
		{
			this.task = Bukkit.getScheduler().runTaskTimer(CreativeGates.get(), this::tickAmbient, TICK_PERIOD, TICK_PERIOD);
		}
	}
	
	/**
	 * Burst particles for all content blocks (create / use).
	 * 
	 * @param gate The gate to spawn the particles for.
	 */
	public void burstGate(UGate gate)
	{
		if (gate == null) return;
		List<Block> blocks = gate.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return;
		
		GateType type = gate.getFillType();
		if (type != null && type.isParticleFill())
		{
			this.spawnParticleFill(blocks, type.getParticle(), Math.max(8, MConf.get().getGateFillParticleAmount() * 2));
			return;
		}
		
		GateFillParticleKit kit = GateFillParticleKit.of(type);
		Material display = type != null ? type.getClientDisplayMaterial() : null;
		GateOrientation orientation = gate.getOrientation();
		
		for (Block block : blocks)
		{
			kit.spawnBurst(block, display, orientation);
		}
	}
	
	/**
	 * Tick the ambient particles for all gates.
	 */
	private void tickAmbient()
	{
		this.tick++;
		if (Bukkit.getOnlinePlayers().isEmpty()) return;
		
		boolean kitTick = this.tick % 3 == 0;
		for (UGate gate : UGateColl.get().getAll())
		{
			this.tickGate(gate, kitTick);
		}
	}
	
	/**
	 * Tick the ambient particles for a single gate.
	 * 
	 * @param gate The gate to spawn the particles for.
	 * @param kitTick Whether block-fill kits should spawn this tick.
	 */
	private void tickGate(UGate gate, boolean kitTick)
	{
		if (gate == null || !gate.isIntact()) return;
		
		List<Block> blocks = gate.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return;
		
		World world = blocks.get(0).getWorld();
		if (world == null) return;
		
		Location sample = blocks.get(0).getLocation().add(0.5, 0.5, 0.5);
		boolean nearPlayer = false;
		for (Player player : world.getPlayers())
		{
			if (player.getLocation().distanceSquared(sample) <= (double) VIEW_DISTANCE_BLOCKS * VIEW_DISTANCE_BLOCKS)
			{
				nearPlayer = true;
				break;
			}
		}
		if (!nearPlayer) return;
		
		GateType type = gate.getFillType();
		if (type != null && type.isParticleFill())
		{
			this.spawnParticleFill(blocks, type.getParticle(), MConf.get().getGateFillParticleAmount());
			return;
		}
		
		if (!kitTick) return;
		
		GateFillParticleKit kit = GateFillParticleKit.of(type);
		Material display = type != null ? type.getClientDisplayMaterial() : null;
		GateOrientation orientation = gate.getOrientation();
		
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		int samples = Math.min(4, Math.max(1, blocks.size() / 4));
		for (int n = 0; n < samples; n++)
		{
			Block block = blocks.get(rng.nextInt(blocks.size()));
			kit.spawnAmbient(block, display, orientation);
		}
	}
	
	/**
	 * Spawns {@code amount} of {@code particle} at random points inside the fill blocks.
	 *
	 * @param blocks Interior blocks.
	 * @param particle Particle to play; ignored if null.
	 * @param amount How many particles to spawn.
	 */
	private void spawnParticleFill(List<Block> blocks, Particle particle, int amount)
	{
		if (particle == null || blocks == null || blocks.isEmpty() || amount <= 0) return;
		World world = blocks.get(0).getWorld();
		if (world == null) return;
		
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		int count = Math.max(1, amount);
		for (int n = 0; n < count; n++)
		{
			Block block = blocks.get(rng.nextInt(blocks.size()));
			double x = block.getX() + rng.nextDouble();
			double y = block.getY() + rng.nextDouble();
			double z = block.getZ() + rng.nextDouble();
			try
			{
				world.spawnParticle(particle, x, y, z, 1, 0.12, 0.12, 0.12, 0.02);
			}
			catch (Exception ignored)
			{
				return;
			}
		}
	}
}
