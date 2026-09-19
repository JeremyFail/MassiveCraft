package com.massivecraft.creativegates.gate.fill;

import com.massivecraft.creativegates.CreativeGates;
import org.bukkit.Material;

/**
 * Resolves config / persistence string ids into {@link GateType} instances.
 * <p>
 * Order: {@link ParticleGateType} ({@code PARTICLE_*} prefix), then {@link SupportedGateType}
 * enum name, then {@link Material} as {@link UnsupportedGateType}.
 * </p>
 */
public final class GateTypeResolve
{
	private GateTypeResolve() { }
	
	/**
	 * Parse a config id into a gate type.
	 *
	 * @param id Enum name, {@code PARTICLE_*} id, or material name; may be null/blank.
	 * @return Resolved type, or null if unrecognized / invalid.
	 */
	public static GateType parse(String id)
	{
		if (id == null) return null;
		String trimmed = id.trim();
		if (trimmed.isEmpty()) return null;
		
		String key = trimmed.toUpperCase();
		
		if (key.startsWith(ParticleGateType.ID_PREFIX))
		{
			return ParticleGateType.parse(key);
		}
		
		try
		{
			return SupportedGateType.valueOf(key);
		}
		catch (IllegalArgumentException ignored)
		{
			// fall through to material
		}
		
		Material material;
		try
		{
			material = Material.valueOf(key);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
		
		if (CreativeGates.isVoid(material) || !material.isBlock()) return null;
		
		// Prefer supported type when a material coincides with a supported base (e.g. WATER).
		SupportedGateType supported = SupportedGateType.fromServerMaterial(material);
		if (supported != null) return supported;
		for (SupportedGateType type : SupportedGateType.values())
		{
			if (type.getBaseMaterial() == material) return type;
		}
		
		return UnsupportedGateType.of(material);
	}
	
	/**
	 * @param id Config id.
	 * @return True if {@link #parse(String)} would succeed.
	 */
	public static boolean isResolvable(String id)
	{
		return parse(id) != null;
	}
}
