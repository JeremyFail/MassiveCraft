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
 * {@link UnsupportedGateType} instances instead. Particle fills are configured separately
 * via {@link MConf#getAllowedGateParticleTypes()}.
 * </p>
 * <p>
 * <b>Server fill vs look:</b> {@link #WATER} and {@link #LAVA} place real fluid blocks.
 * All other supported types keep invisible {@link Material#LIGHT} on the server and show the
 * look via BlockDisplay and/or {@code Player#sendBlockChange} - never real portal/fire blocks
 * (so no piglin portal spawns, vanilla nether travel, or fire damage from the fill).
 * {@link #FIRE}, {@link #SOUL_FIRE}, and <em>vertical</em> {@link #NETHER_PORTAL} use
 * {@code sendBlockChange} (client sees real blocks → correct animation).
 * <em>Horizontal</em> {@link #NETHER_PORTAL} uses rotated BlockDisplays (vanilla portal models
 * cannot lie flat; BlockDisplay animation may freeze at some pitches).
 * {@link #END_GATEWAY} uses BlockDisplay (or {@code sendBlockChange} before MC 26.1).
 * </p>
 */
public enum SupportedGateType implements GateType
{
	/**
	 * Nether portal look. Vertical: client {@code sendBlockChange} (animates; no server portal
	 * block). Horizontal: rotated BlockDisplay (flat look; animation may freeze at some pitches).
	 */
	NETHER_PORTAL(Material.NETHER_PORTAL),
	
	/**
	 * Water interior. In the nether, may become lava when
	 * {@link MConf#isReplaceWaterWithLavaInNether()} is true.
	 */
	WATER(Material.WATER),
	
	/**
	 * Lava interior (overworld or nether). Fire/lava damage
	 * is cancelled while inside the gate.
	 */
	LAVA(Material.LAVA),
	
	/**
	 * End-gateway look. BlockDisplay when MC ≥ 26.1,
	 * otherwise temporary client block-change fallback.
	 */
	END_GATEWAY(Material.END_GATEWAY),
	
	/**
	 * Fire look via client {@code sendBlockChange} on the whole interior (server light).
	 * BlockDisplays cannot animate fire reliably from all view angles. Full cube model;
	 * walk-through may show the vanilla fire overlay. No server fire damage.
	 */
	FIRE(Material.FIRE),
	
	/**
	 * Soul fire look via client {@code sendBlockChange} on the whole interior (server light).
	 * BlockDisplays never start the soul-fire atlas. Full cube model; walk-through may show
	 * the vanilla fire overlay. No server fire damage.
	 */
	SOUL_FIRE(Material.SOUL_FIRE),
	;
	
	/** Material used for placement and/or client display identity. */
	private final Material baseMaterial;
	
	SupportedGateType(Material baseMaterial)
	{
		this.baseMaterial = baseMaterial;
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
	public boolean shouldPreventDamage(DamageCause cause)
	{
		if (cause == null) return false;

		// Only real lava fill deals contact damage; client-overlay fire does not.
		if (this == LAVA)
		{
			return cause == DamageCause.LAVA || cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK;
		}
		// Real water fill - prevent drowning if the player lingers in the column (unlikely, but just in case)
		if (this == WATER)
		{
			return cause == DamageCause.DROWNING;
		}
		return false;
	}
	
	@Override
	public boolean isEnterable()
	{
		return true;
	}
	
	@Override
	public boolean isCompatibleWith(GateOrientation orientation)
	{
		return true;
	}
	
	@Override
	public boolean usesBlockDisplay(GateOrientation orientation)
	{
		// LIGHT server fill + EngineGateFillDisplay (entities and/or sendBlockChange).
		return this == END_GATEWAY || this == FIRE || this == SOUL_FIRE || this == NETHER_PORTAL;
	}
	
	@Override
	public boolean usesClientBlockChangeFill(GateOrientation orientation)
	{
		if (this == FIRE || this == SOUL_FIRE) return true;
		if (this == NETHER_PORTAL)
		{
			// Horizontal keeps BlockDisplay (flat). Vertical uses client portal blocks.
			return orientation == null || orientation.isVertical();
		}
		return false;
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
	public Material getServerFillMaterial(World world, GateOrientation orientation)
	{
		if (this.usesBlockDisplay(orientation))
		{
			return Material.LIGHT;
		}
		
		if (this == WATER
			&& world != null
			&& world.getEnvironment() == World.Environment.NETHER
			&& MConf.get().isReplaceWaterWithLavaInNether())
		{
			return Material.LAVA;
		}
		
		return this.baseMaterial;
	}
	
	@Override
	public boolean isExpectedServerFill(Material material, World world, GateOrientation orientation)
	{
		// Legacy vertical gates may still have real portal blocks until the next fill().
		if (this == NETHER_PORTAL && material == Material.NETHER_PORTAL) return true;
		return GateType.super.isExpectedServerFill(material, world, orientation);
	}
	
	/**
	 * Infer a supported gate type from an existing <em>server</em> fill block.
	 * Matches real fluid fills and legacy real nether-portal interiors.
	 *
	 * @param material Current content material.
	 * @return Matching type, or null if unknown / void / display-only.
	 */
	public static SupportedGateType fromServerMaterial(Material material)
	{
		if (material == null || CreativeGates.isVoid(material)) return null;
		if (material == Material.WATER) return WATER;
		if (material == Material.LAVA) return LAVA;
		if (material == Material.NETHER_PORTAL) return NETHER_PORTAL;
		return null;
	}
	
}
