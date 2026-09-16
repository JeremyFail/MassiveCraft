package com.massivecraft.creativegates.gate.fill;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.gate.GateOrientation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Objects;

/**
 * Experimental / custom gate fill keyed by an arbitrary block {@link Material}.
 * <p>
 * Always enterable and client-visual (server air). Attempts to prevent melt and
 * <em>any</em> damage while the player is in the gate. Not officially supported;
 * admins opt in via string ids in the MConf allow-lists.
 * </p>
 */
public final class UnsupportedGateType implements GateType
{
	private final Material material;
	
	private UnsupportedGateType(Material material)
	{
		this.material = material;
	}
	
	/**
	 * @param material Block material to show client-side; must be a non-void block.
	 * @return New unsupported type instance.
	 */
	public static UnsupportedGateType of(Material material)
	{
		if (material == null) throw new NullPointerException("material");
		if (CreativeGates.isVoid(material)) throw new IllegalArgumentException("material must not be void");
		if (!material.isBlock()) throw new IllegalArgumentException("material must be a block: " + material);
		return new UnsupportedGateType(material);
	}
	
	@Override
	public Material getBaseMaterial()
	{
		return this.material;
	}
	
	@Override
	public Material getClientDisplayMaterial()
	{
		return this.material;
	}
	
	@Override
	public Material getServerFillMaterial(World world)
	{
		return Material.AIR;
	}
	
	@Override
	public boolean usesClientVisual()
	{
		return true;
	}
	
	@Override
	public boolean isEnterable()
	{
		return true;
	}
	
	@Override
	public boolean shouldPreventMelt()
	{
		return true;
	}
	
	@Override
	public boolean shouldPreventDamage(DamageCause cause)
	{
		return cause != null;
	}
	
	@Override
	public boolean isFluid()
	{
		return false;
	}
	
	@Override
	public boolean isCompatibleWith(GateOrientation orientation)
	{
		return true;
	}
	
	@Override
	public boolean isSupported()
	{
		return false;
	}
	
	@Override
	public String getConfigId()
	{
		return this.material.name();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!(obj instanceof UnsupportedGateType that)) return false;
		return this.material == that.material;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.material);
	}
	
	@Override
	public String toString()
	{
		return "UnsupportedGateType[" + this.material + "]";
	}
}
