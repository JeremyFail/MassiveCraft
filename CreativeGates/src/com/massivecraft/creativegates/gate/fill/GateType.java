package com.massivecraft.creativegates.gate.fill;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.gate.GateOrientation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Common API for gate interior fills: officially supported types and experimental custom materials.
 * <p>
 * Config stores type ids as strings; {@link GateTypeResolve} turns them into implementations of this
 * interface ({@link SupportedGateType} or {@link UnsupportedGateType}).
 * </p>
 */
public interface GateType
{
	/**
	 * Identity material for this type (UI icon / what players think of as the fill).
	 *
	 * @return Base / display material; never null.
	 */
	Material getBaseMaterial();
	
	/**
	 * Material the client should see for this type.
	 *
	 * @return Client display material; never null.
	 */
	Material getClientDisplayMaterial();
	
	/**
	 * Material actually placed in the world for this type.
	 *
	 * @param world World the gate is in; may be null.
	 * @return Server-side fill material; never null.
	 */
	Material getServerFillMaterial(World world);
	
	/**
	 * Whether this type keeps air on the server and shows {@link #getClientDisplayMaterial()} only to clients.
	 *
	 * @return True for client-visual fills.
	 */
	boolean usesClientVisual();
	
	/**
	 * Whether players are expected to occupy / move through the fill.
	 *
	 * @return True if walk-through (or sink-through).
	 */
	boolean isEnterable();
	
	/**
	 * Whether fade/melt of a real server fill should be cancelled.
	 *
	 * @return True if melt/fade should be blocked.
	 */
	boolean shouldPreventMelt();
	
	/**
	 * Whether damage of the given cause should be cancelled while the player is in this gate fill.
	 *
	 * @param cause Damage cause; null returns false.
	 * @return True if that damage should be blocked.
	 */
	boolean shouldPreventDamage(DamageCause cause);
	
	/**
	 * @return True for water/lava interiors (flow rules apply).
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
	 * Stable config / persistence id (enum name or material name).
	 *
	 * @return Non-null id string.
	 */
	String getConfigId();
	
	/**
	 * Whether {@code material} matches the expected server fill in {@code world}.
	 *
	 * @param material Block material currently in the world.
	 * @param world World for substitutions.
	 * @return True if the block matches {@link #getServerFillMaterial(World)}.
	 */
	default boolean isExpectedServerFill(Material material, World world)
	{
		Material expected = this.getServerFillMaterial(world);
		if (CreativeGates.isVoid(expected)) return CreativeGates.isVoid(material);
		return material == expected;
	}
}
