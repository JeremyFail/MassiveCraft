package com.massivecraft.creativegates.gate.fill;

import com.massivecraft.creativegates.gate.GateOrientation;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Ambient / burst particle style for a gate fill.
 */
public enum GateFillParticleKit
{
	NETHER_PORTAL,
	END_GATEWAY,
	WATER,
	LAVA,
	FIRE,
	SOUL_FIRE,
	SNOW,
	DUST,
	;
	
	/** Spawn just outside a full cube so lava/water particles are not buried in the block. */
	private static final double FACE_OUTSET = 0.08;
	
	/**
	 * Resolve a particle kit for a fill type.
	 *
	 * @param type The type to resolve the particle kit for.
	 * @return The particle kit for the type.
	 */
	public static GateFillParticleKit of(GateType type)
	{
		if (type == null || type.isParticleFill()) return DUST;
		if (type instanceof SupportedGateType supported)
		{
			return switch (supported)
			{
				case NETHER_PORTAL -> NETHER_PORTAL;
				case END_GATEWAY -> END_GATEWAY;
				case WATER -> WATER;
				case LAVA -> LAVA;
				case FIRE -> FIRE;
				case SOUL_FIRE -> SOUL_FIRE;
			};
		}
		return ofMaterial(type.getBaseMaterial());
	}
	
	/**
	 * Resolve a particle kit for a material. Cold blocks use snowflakes,
	 * other materials use tinted dust.
	 *
	 * @param material The material to resolve the particle kit for.
	 * @return The particle kit for the material.
	 */
	public static GateFillParticleKit ofMaterial(Material material)
	{
		if (material == null) return DUST;
		if (material == Material.FIRE || material == Material.CAMPFIRE) return FIRE;
		if (material == Material.SOUL_FIRE || material == Material.SOUL_CAMPFIRE) return SOUL_FIRE;
		if (material == Material.POWDER_SNOW
			|| material == Material.SNOW
			|| material == Material.SNOW_BLOCK
			|| material == Material.ICE
			|| material == Material.PACKED_ICE
			|| material == Material.BLUE_ICE
			|| material == Material.FROSTED_ICE
			|| Tag.SNOW.isTagged(material))
		{
			return SNOW;
		}
		return DUST;
	}
	
	/**
	 * Spawn a small ambient burst at the block.
	 *
	 * @param block The block to spawn the particles at.
	 * @param displayMaterial The material to use for the particles.
	 * @param orientation Gate plane; used to put fluid particles on the visible faces.
	 */
	public void spawnAmbient(Block block, Material displayMaterial, GateOrientation orientation)
	{
		if (block == null) return;
		World world = block.getWorld();
		if (world == null) return;
		
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		double[] at = new double[3];
		
		switch (this)
		{
			case NETHER_PORTAL ->
			{
				center(block, at);
				world.spawnParticle(Particle.PORTAL, at[0], at[1], at[2], 8, 0.25, 0.35, 0.25, 0.4);
			}
			case END_GATEWAY ->
			{
				center(block, at);
				world.spawnParticle(Particle.REVERSE_PORTAL, at[0], at[1], at[2], 6, 0.2, 0.3, 0.2, 0.02);
				world.spawnParticle(Particle.PORTAL, at[0], at[1], at[2], 3, 0.15, 0.25, 0.15, 0.2);
			}
			case WATER ->
			{
				randomFluidFace(block, orientation, rng, at, true);
				world.spawnParticle(Particle.DRIPPING_WATER, at[0], at[1], at[2], 2, 0.12, 0.05, 0.12, 0);
				if (rng.nextBoolean())
				{
					world.spawnParticle(Particle.SPLASH, at[0], at[1], at[2], 2, 0.1, 0.05, 0.1, 0);
				}
			}
			case LAVA ->
			{
				randomFluidFace(block, orientation, rng, at, true);
				world.spawnParticle(Particle.DRIPPING_LAVA, at[0], at[1], at[2], 2, 0.12, 0.05, 0.12, 0);
				if (rng.nextBoolean())
				{
					world.spawnParticle(Particle.FLAME, at[0], at[1], at[2], 1, 0.1, 0.1, 0.1, 0.01);
				}
			}
			case FIRE ->
			{
				center(block, at);
				offsetAlongThinAxis(at, orientation, rng, 0.12);
				world.spawnParticle(Particle.FLAME, at[0], at[1], at[2], 3, 0.12, 0.3, 0.12, 0.01);
				if (rng.nextInt(3) == 0)
				{
					world.spawnParticle(Particle.SMOKE, at[0], at[1] + 0.2, at[2], 1, 0.08, 0.15, 0.08, 0.01);
				}
			}
			case SOUL_FIRE ->
			{
				center(block, at);
				offsetAlongThinAxis(at, orientation, rng, 0.12);
				world.spawnParticle(Particle.SOUL_FIRE_FLAME, at[0], at[1], at[2], 3, 0.12, 0.3, 0.12, 0.01);
				if (rng.nextInt(3) == 0)
				{
					world.spawnParticle(Particle.SOUL, at[0], at[1] + 0.15, at[2], 1, 0.08, 0.12, 0.08, 0.01);
				}
			}
			case SNOW ->
			{
				center(block, at);
				Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 1.0f);
				world.spawnParticle(Particle.DUST, at[0], at[1], at[2], 4, 0.3, 0.3, 0.3, 0, white);
			}
			case DUST ->
			{
				center(block, at);
				Material mat = displayMaterial != null && displayMaterial.isBlock() ? displayMaterial : Material.STONE;
				Color color = colorFor(mat);
				Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
				world.spawnParticle(Particle.DUST, at[0], at[1], at[2], 3, 0.25, 0.25, 0.25, 0, dust);
				if (rng.nextInt(4) == 0)
				{
					BlockData data = mat.createBlockData();
					world.spawnParticle(Particle.BLOCK, at[0], at[1], at[2], 2, 0.2, 0.2, 0.2, 0.02, data);
				}
			}
		}
	}
	
	/**
	 * Stronger burst for create / use FX.
	 *
	 * @param block The block to spawn the particles at.
	 * @param displayMaterial The material to use for the particles.
	 * @param orientation Gate plane; used to put fluid particles on the visible faces.
	 */
	public void spawnBurst(Block block, Material displayMaterial, GateOrientation orientation)
	{
		if (block == null) return;
		World world = block.getWorld();
		if (world == null) return;
		
		double[] at = new double[3];
		
		switch (this)
		{
			case NETHER_PORTAL ->
			{
				center(block, at);
				world.spawnParticle(Particle.PORTAL, at[0], at[1], at[2], 40, 0.4, 0.5, 0.4, 0.6);
			}
			case END_GATEWAY ->
			{
				center(block, at);
				world.spawnParticle(Particle.REVERSE_PORTAL, at[0], at[1], at[2], 30, 0.35, 0.45, 0.35, 0.05);
				world.spawnParticle(Particle.PORTAL, at[0], at[1], at[2], 20, 0.3, 0.4, 0.3, 0.5);
			}
			case WATER ->
			{
				for (boolean positive : new boolean[]{false, true})
				{
					fluidFace(block, orientation, positive, at, false);
					world.spawnParticle(Particle.SPLASH, at[0], at[1], at[2], 12, 0.2, 0.2, 0.2, 0.08);
				}
			}
			case LAVA ->
			{
				for (boolean positive : new boolean[]{false, true})
				{
					fluidFace(block, orientation, positive, at, false);
					world.spawnParticle(Particle.LAVA, at[0], at[1], at[2], 4, 0.15, 0.15, 0.15, 0);
					world.spawnParticle(Particle.FLAME, at[0], at[1], at[2], 6, 0.18, 0.18, 0.18, 0.02);
				}
			}
			case FIRE ->
			{
				center(block, at);
				world.spawnParticle(Particle.FLAME, at[0], at[1], at[2], 16, 0.25, 0.4, 0.25, 0.02);
				world.spawnParticle(Particle.LAVA, at[0], at[1], at[2], 4, 0.15, 0.2, 0.15, 0);
				world.spawnParticle(Particle.SMOKE, at[0], at[1] + 0.3, at[2], 8, 0.2, 0.25, 0.2, 0.02);
			}
			case SOUL_FIRE ->
			{
				center(block, at);
				world.spawnParticle(Particle.SOUL_FIRE_FLAME, at[0], at[1], at[2], 16, 0.25, 0.4, 0.25, 0.02);
				world.spawnParticle(Particle.SOUL, at[0], at[1] + 0.2, at[2], 8, 0.2, 0.25, 0.2, 0.02);
			}
			case SNOW ->
			{
				center(block, at);
				Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 1.15f);
				world.spawnParticle(Particle.DUST, at[0], at[1], at[2], 25, 0.45, 0.45, 0.45, 0, white);
			}
			case DUST ->
			{
				center(block, at);
				Material mat = displayMaterial != null && displayMaterial.isBlock() ? displayMaterial : Material.STONE;
				Color color = colorFor(mat);
				Particle.DustOptions dust = new Particle.DustOptions(color, 1.2f);
				world.spawnParticle(Particle.DUST, at[0], at[1], at[2], 18, 0.4, 0.4, 0.4, 0, dust);
			}
		}
	}
	
	private static void center(Block block, double[] xyz)
	{
		xyz[0] = block.getX() + 0.5;
		xyz[1] = block.getY() + 0.5;
		xyz[2] = block.getZ() + 0.5;
	}
	
	/**
	 * Puffs particles off both sides of a paper-thin display plane.
	 */
	private static void offsetAlongThinAxis(double[] xyz, GateOrientation orientation, ThreadLocalRandom rng, double amount)
	{
		double delta = rng.nextBoolean() ? amount : -amount;
		if (orientation == GateOrientation.WE)
		{
			xyz[2] += delta;
		}
		else if (orientation != null && orientation.isHorizontal())
		{
			xyz[1] += delta;
		}
		else
		{
			xyz[0] += delta;
		}
	}
	
	private static void randomFluidFace(Block block, GateOrientation orientation, ThreadLocalRandom rng, double[] xyz, boolean dripFromTop)
	{
		fluidFace(block, orientation, rng.nextBoolean(), xyz, dripFromTop);
		if (orientation == GateOrientation.WE)
		{
			xyz[0] = block.getX() + rng.nextDouble(0.2, 0.8);
			if (!dripFromTop) xyz[1] = block.getY() + rng.nextDouble(0.15, 0.85);
		}
		else if (orientation != null && orientation.isHorizontal())
		{
			xyz[0] = block.getX() + rng.nextDouble(0.2, 0.8);
			xyz[2] = block.getZ() + rng.nextDouble(0.2, 0.8);
		}
		else
		{
			if (!dripFromTop) xyz[1] = block.getY() + rng.nextDouble(0.15, 0.85);
			xyz[2] = block.getZ() + rng.nextDouble(0.2, 0.8);
		}
	}
	
	/**
	 * Places {@code xyz} just outside a cube face that matches the gate plane.
	 *
	 * @param dripFromTop When true, start near the top of vertical faces so drips fall down the surface.
	 */
	private static void fluidFace(Block block, GateOrientation orientation, boolean positive, double[] xyz, boolean dripFromTop)
	{
		xyz[0] = block.getX() + 0.5;
		xyz[1] = dripFromTop ? block.getY() + 0.85 : block.getY() + 0.5;
		xyz[2] = block.getZ() + 0.5;
		if (orientation == GateOrientation.WE)
		{
			xyz[2] = positive ? block.getZ() + 1.0 + FACE_OUTSET : block.getZ() - FACE_OUTSET;
		}
		else if (orientation != null && orientation.isHorizontal())
		{
			xyz[1] = positive ? block.getY() + 1.0 + FACE_OUTSET : block.getY() - FACE_OUTSET;
		}
		else
		{
			xyz[0] = positive ? block.getX() + 1.0 + FACE_OUTSET : block.getX() - FACE_OUTSET;
		}
	}
	
	/**
	 * Resolve a color for a material.
	 *
	 * @param material The material to resolve the color for.
	 * @return The color for the material.
	 */
	private static Color colorFor(Material material)
	{
		int hash = material.name().hashCode();
		int r = 40 + ((hash >> 16) & 0x7F);
		int g = 40 + ((hash >> 8) & 0x7F);
		int b = 40 + (hash & 0x7F);
		return Color.fromRGB(r, g, b);
	}
}
