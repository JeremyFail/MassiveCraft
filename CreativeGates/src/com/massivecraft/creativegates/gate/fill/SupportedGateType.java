package com.massivecraft.creativegates.gate.fill;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.gate.GateOrientation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * First-class creative gate interior types with known, supported behaviors.
 * <p>
 * Admins choose which of these players may use via string ids in
 * {@link MConf#getAllowedGateTypes()} and {@link MConf#getAllowedHorizontalGateTypes()}
 * (resolved by {@link GateTypeResolve}). Experimental materials become
 * {@link UnsupportedGateType} instances instead.
 * </p>
 * <p>
 * <b>Server fill vs client look:</b> Some types place a real block
 * ({@link #WATER}, {@link #LAVA}, {@link #NETHER_PORTAL}, {@link #POWDER_SNOW}).
 * Others ({@link #END_GATEWAY}, ice variants) use
 * <em>client visuals</em>: the server keeps void/air so there is no collision, and
 * the look is (or will be) sent with {@code Player#sendBlockChange}. Collision is
 * always determined by the server block, not by what the client is shown.
 * </p>
 * <p>
 * Behavior flags on each constant ({@link #shouldPreventMelt()},
 * {@link #shouldPreventDamage(DamageCause)}, {@link #isEnterable()},
 * {@link #usesClientVisual()}) keep special-case logic out of event listeners.
 * </p>
 */
public enum SupportedGateType implements GateType
{
	/**
	 * Vanilla nether portal interior. Vertical gates only;
	 * not compatible with horizontal gates.
	 */
	NETHER_PORTAL(Material.NETHER_PORTAL, false, false, true, false),
	
	/**
	 * Water interior. In the nether, may become lava when
	 * {@link MConf#isReplaceWaterWithLavaInNether()} is true.
	 */
	WATER(Material.WATER, false, false, true, false),
	
	/**
	 * Lava interior (overworld or nether). Fire/lava damage
	 * is cancelled while inside the gate.
	 */
	LAVA(Material.LAVA, false, false, true, false),
	
	/**
	 * End-gateway look. Server fill is air; client overlay shows {@link Material#END_GATEWAY}.
	 * Walk-through because the server block has no collision.
	 */
	END_GATEWAY(Material.END_GATEWAY, false, false, true, true),
	
	/**
	 * Powder snow interior (real block). Players sink into it;
	 * melt and freeze damage are suppressed.
	 */
	POWDER_SNOW(Material.POWDER_SNOW, true, true, true, false),
	
	/**
	 * Ice look via client visual (server air). Walk-through; no real ice to melt or freeze from.
	 */
	ICE(Material.ICE, false, false, true, true),
	
	/**
	 * Packed ice look via client visual (server air). Walk-through.
	 */
	PACKED_ICE(Material.PACKED_ICE, false, false, true, true),
	
	/**
	 * Blue ice look via client visual (server air). Walk-through.
	 */
	BLUE_ICE(Material.BLUE_ICE, false, false, true, true),
	
	/**
	 * Frosted ice look via client visual (server air). Walk-through.
	 */
	FROSTED_ICE(Material.FROSTED_ICE, false, false, true, true),
	;
	
	/** Material used for placement and/or client display identity. */
	private final Material baseMaterial;
	
	/** When true, cancel {@code BlockFadeEvent} (and similar) on real server fills of this type. */
	private final boolean preventMelt;
	
	/** When true, cancel freeze damage / clear freeze ticks for real fills of this type. */
	private final boolean preventColdDamage;
	
	/**
	 * When true, players are expected to move through the fill.
	 * Client-visual types are always enterable because the server block is air.
	 */
	private final boolean enterable;
	
	/**
	 * When true, server places air and the look comes from {@link #getClientDisplayMaterial()}
	 * (via {@code sendBlockChange} when that layer is implemented).
	 */
	private final boolean clientVisual;
	
	SupportedGateType(Material baseMaterial, boolean preventMelt, boolean preventColdDamage, boolean enterable, boolean clientVisual)
	{
		this.baseMaterial = baseMaterial;
		this.preventMelt = preventMelt;
		this.preventColdDamage = preventColdDamage;
		this.enterable = enterable;
		this.clientVisual = clientVisual;
	}
	
	@Override
	public Material getBaseMaterial()
	{
		return this.baseMaterial;
	}
	
	@Override
	public Material getClientDisplayMaterial()
	{
		return this.baseMaterial;
	}
	
	@Override
	public boolean shouldPreventMelt()
	{
		return this.preventMelt;
	}
	
	@Override
	public boolean shouldPreventDamage(DamageCause cause)
	{
		if (cause == null) return false;
		if (this == LAVA)
		{
			return cause == DamageCause.LAVA || cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK;
		}
		if (this.preventColdDamage)
		{
			return cause == DamageCause.FREEZE;
		}
		return false;
	}
	
	@Override
	public boolean isEnterable()
	{
		return this.enterable;
	}
	
	@Override
	public boolean isCompatibleWith(GateOrientation orientation)
	{
		if (orientation != null && orientation.isHorizontal())
		{
			return this != NETHER_PORTAL;
		}
		return true;
	}
	
	@Override
	public boolean usesClientVisual()
	{
		return this.clientVisual;
	}
	
	@Override
	public boolean isFluid()
	{
		return this == WATER || this == LAVA;
	}
	
	@Override
	public boolean isSupported()
	{
		return true;
	}
	
	@Override
	public String getConfigId()
	{
		return this.name();
	}
	
	@Override
	public Material getServerFillMaterial(World world)
	{
		if (this.clientVisual) return Material.AIR;
		
		if (this == WATER
			&& world != null
			&& world.getEnvironment() == World.Environment.NETHER
			&& MConf.get().isReplaceWaterWithLavaInNether())
		{
			return Material.LAVA;
		}
		
		return this.baseMaterial;
	}
	
	/**
	 * Infer a supported gate type from an existing <em>server</em> fill block.
	 * Does not match client-visual types (those leave air in the world).
	 *
	 * @param material Current content material.
	 * @return Matching type, or null if unknown / void / client-visual-only.
	 */
	public static SupportedGateType fromServerMaterial(Material material)
	{
		if (material == null || CreativeGates.isVoid(material)) return null;
		for (SupportedGateType type : values())
		{
			if (type.clientVisual) continue;
			if (type.baseMaterial == material) return type;
		}
		return null;
	}
	
}
