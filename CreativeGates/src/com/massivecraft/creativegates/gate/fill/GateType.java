package com.massivecraft.creativegates.gate.fill;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.gate.GateOrientation;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Common API for gate interior fills: officially supported types, experimental custom materials,
 * and {@link ParticleGateType particle fills}.
 * <p>
 * Config stores type ids as strings; {@link GateTypeResolve} turns them into implementations of this
 * interface ({@link SupportedGateType}, {@link UnsupportedGateType}, or {@link ParticleGateType}).
 * </p>
 */
public interface GateType
{
	/**
	 * Invisible {@link Material#LIGHT} level for BlockDisplay creative-gate fills.
	 */
	int DISPLAY_BLOCK_LIGHT_LEVEL = 11;
	
	/**
	 * Identity material for this type (UI icon / what players think of as the fill).
	 *
	 * @return Base / display material; never null.
	 */
	Material getBaseMaterial();
	
	/**
	 * Material the client should see for this type (BlockDisplay or legacy overlay).
	 *
	 * @return Client display material; never null.
	 */
	Material getClientDisplayMaterial();
	
	/**
	 * Material actually placed in the world for this type.
	 * Display fills place an invisible light block at {@link #DISPLAY_BLOCK_LIGHT_LEVEL}.
	 *
	 * @param world World the gate is in; may be null.
	 * @return Server-side fill material; never null.
	 */
	default Material getServerFillMaterial(World world)
	{
		return this.getServerFillMaterial(world, null);
	}
	
	/**
	 * Material actually placed for this type at the given gate orientation.
	 * Orientation matters for {@link SupportedGateType#NETHER_PORTAL}
	 * (vertical client overlay vs horizontal BlockDisplay).
	 *
	 * @param world World the gate is in; may be null.
	 * @param orientation Gate orientation; null is treated as vertical.
	 * @return Server-side fill material; never null.
	 */
	default Material getServerFillMaterial(World world, GateOrientation orientation)
	{
		return this.getEmittedBlockLightLevel(orientation) >= 0 ? Material.LIGHT : Material.AIR;
	}
	
	/**
	 * Block-light level emitted by the server fill (e.g. invisible {@link Material#LIGHT} for
	 * display and particle fills). Those fills use {@link #DISPLAY_BLOCK_LIGHT_LEVEL}.
	 *
	 * @return 0–15 to place a light block, or {@code -1} for no emission (air / fluids / real portal).
	 */
	default int getEmittedBlockLightLevel()
	{
		return this.getEmittedBlockLightLevel(null);
	}
	
	/**
	 * @param orientation Gate orientation; null is treated as vertical.
	 * @return 0–15 to place a light block, or {@code -1} for no emission.
	 */
	default int getEmittedBlockLightLevel(GateOrientation orientation)
	{
		return this.usesBlockDisplay(orientation) ? DISPLAY_BLOCK_LIGHT_LEVEL : -1;
	}
	
	/**
	 * Whether this type shows the look via BlockDisplay
	 * (or a temporary END_GATEWAY sendBlockChange fallback) for a typical / unknown orientation.
	 * Prefer {@link #usesBlockDisplay(GateOrientation)} when the gate orientation is known.
	 *
	 * @return True for display-based fills.
	 */
	default boolean usesBlockDisplay()
	{
		return this.usesBlockDisplay(null);
	}
	
	/**
	 * Whether this type shows the look via BlockDisplay for the given orientation.
	 * Server fill is then an invisible light block.
	 *
	 * @param orientation Gate orientation; null is treated as vertical.
	 * @return True for display-based fills.
	 */
	boolean usesBlockDisplay(GateOrientation orientation);
	
	/**
	 * Whether the look is sent with {@link org.bukkit.entity.Player#sendBlockChange} over the
	 * whole interior (server fill stays light/air) instead of BlockDisplays.
	 * Used for fire, soul fire, and vertical nether portal (see {@link #usesClientBlockChangeFill(GateOrientation)}).
	 *
	 * @return True for client block-change overlays at a typical / unknown orientation.
	 */
	default boolean usesClientBlockChangeFill()
	{
		return this.usesClientBlockChangeFill(null);
	}
	
	/**
	 * Whether the look is a client {@code sendBlockChange} overlay for the given orientation.
	 * <p>
	 * Fire and soul fire always use overlays (BlockDisplays cannot animate those atlases
	 * reliably). Vertical nether portal uses overlays so the client sees real portal blocks
	 * (correct animation) without server-side portal blocks (no piglin spawns / vanilla travel).
	 * Horizontal nether portal stays BlockDisplay - vanilla portal models cannot lie flat.
	 * </p>
	 *
	 * @param orientation Gate orientation; null is treated as vertical.
	 * @return True for client block-change overlays.
	 */
	default boolean usesClientBlockChangeFill(GateOrientation orientation)
	{
		return false;
	}
	
	/**
	 * Whether players are expected to occupy / move through the fill.
	 *
	 * @return True if walk-through (or sink-through for fluids).
	 */
	boolean isEnterable();
	
	/**
	 * Whether damage of the given cause should be cancelled while the player is in this gate fill.
	 *
	 * @param cause Damage cause; null returns false.
	 * @return True if that damage should be blocked.
	 */
	boolean shouldPreventDamage(DamageCause cause);
	
	/**
	 * @return True for water/lava interiors (flow rules apply; real server blocks).
	 */
	boolean isFluid();
	
	/**
	 * Whether this type can be used for the given orientation.
	 *
	 * @param orientation Gate orientation, or null to skip the check.
	 * @return True if compatible.
	 */
	boolean isCompatibleWith(GateOrientation orientation);
	
	/**
	 * @return True for officially supported built-in types; false for experimental custom materials.
	 */
	boolean isSupported();
	
	/**
	 * Whether this fill is a particle effect rather than a block / BlockDisplay interior.
	 *
	 * @return True for {@link ParticleGateType}.
	 */
	default boolean isParticleFill()
	{
		return false;
	}
	
	/**
	 * Particle played throughout the fill when {@link #isParticleFill()} is true.
	 *
	 * @return Particle, or null for block fills.
	 */
	default Particle getParticle()
	{
		return null;
	}
	
	/**
	 * Stable config / persistence id (enum name or material name).
	 *
	 * @return Non-null id string.
	 */
	String getConfigId();
	
	/**
	 * Whether {@code material} matches the expected server fill in {@code world}.
	 * Air and {@link Material#LIGHT} are interchangeable so display fills stay intact
	 * while upgrading from older air interiors.
	 *
	 * @param material Block material currently in the world.
	 * @param world World for substitutions.
	 * @return True if the block matches {@link #getServerFillMaterial(World)}.
	 */
	default boolean isExpectedServerFill(Material material, World world)
	{
		return this.isExpectedServerFill(material, world, null);
	}
	
	/**
	 * @param material Block material currently in the world.
	 * @param world World for substitutions.
	 * @param orientation Gate orientation; null is treated as vertical.
	 * @return True if the block matches {@link #getServerFillMaterial(World, GateOrientation)}.
	 */
	default boolean isExpectedServerFill(Material material, World world, GateOrientation orientation)
	{
		Material expected = this.getServerFillMaterial(world, orientation);
		if (CreativeGates.isVoid(expected) || expected == Material.LIGHT)
		{
			return CreativeGates.isVoid(material) || material == Material.LIGHT;
		}
		return material == expected;
	}
}
