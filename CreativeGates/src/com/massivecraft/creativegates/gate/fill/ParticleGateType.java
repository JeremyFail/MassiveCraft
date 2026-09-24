package com.massivecraft.creativegates.gate.fill;

import com.massivecraft.creativegates.gate.GateOrientation;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Gate interior that plays a Bukkit {@link Particle} throughout the fill instead of placing blocks.
 * <p>
 * Server fill is an invisible {@link Material#LIGHT} block at {@link GateType#DISPLAY_BLOCK_LIGHT_LEVEL}
 * so players walk through and the area is lit like BlockDisplay fills. Only particles that can be
 * spawned without extra data (no dust color, block data, item, vibration, …) are accepted - there
 * is no per-particle kit. Config ids are {@code PARTICLE_} plus the enum name so they never collide
 * with materials ({@link Particle#LAVA} vs {@link Material#LAVA}).
 * </p>
 */
public final class ParticleGateType implements GateType
{
	/** Prefix for persisted / config ids. */
	public static final String ID_PREFIX = "PARTICLE_";
	
	/**
	 * Default allow-list entries (unprefixed enum names). Unknown or data-requiring particles
	 * are dropped when sanitized.
	 */
	public static final Set<String> DEFAULT_RAW_IDS = Collections.unmodifiableSet(MUtil.set(
		"CHERRY_LEAVES",
		"CLOUD",
		"COPPER_FIRE_FLAME",
		"DRIPPING_LAVA",
		"DRIPPING_WATER",
		"ELECTRIC_SPARK",
		"ENCHANT",
		"END_ROD",
		"FLAME",
		"GLOW",
		"GLOW_SQUID_INK",
		"HAPPY_VILLAGER",
		"NAUTILUS",
		"OMINOUS_SPAWNING",
		"PORTAL",
		"REVERSE_PORTAL",
		"SCRAPE",
		"SMOKE",
		"SNOWFLAKE",
		"SOUL",
		"SOUL_FIRE_FLAME",
		"SPIT",
		"SQUID_INK",
		"SNEEZE",
		"TOTEM_OF_UNDYING",
		"TRIAL_SPAWNER_DETECTION",
		"TRIAL_SPAWNER_DETECTION_OMINOUS",
		"WAX_ON",
		"WAX_OFF",
		"WHITE_SMOKE"
	));
	
	private final Particle particle;
	
	private ParticleGateType(Particle particle)
	{
		this.particle = particle;
	}
	
	/**
	 * @param particle Simple (no-data) particle to play in the fill.
	 * @return Type instance.
	 */
	public static ParticleGateType of(Particle particle)
	{
		if (particle == null) throw new NullPointerException("particle");
		if (!isSimple(particle)) throw new IllegalArgumentException("particle requires extra data: " + particle);
		return new ParticleGateType(particle);
	}
	
	/**
	 * Parses a particle fill id: {@code PARTICLE_FLAME} or bare {@code FLAME}.
	 *
	 * @param id Config id; may be null/blank.
	 * @return Type, or null if unknown / not spawnable without data.
	 */
	public static ParticleGateType parse(String id)
	{
		if (id == null) return null;
		String key = id.trim().toUpperCase();
		if (key.isEmpty()) return null;
		if (key.startsWith(ID_PREFIX)) key = key.substring(ID_PREFIX.length());
		if (key.isEmpty()) return null;
		
		Particle particle;
		try
		{
			particle = Particle.valueOf(key);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
		
		if (!isSimple(particle)) return null;
		return of(particle);
	}
	
	/**
	 * Whether this particle can be spawned with the no-data {@code spawnParticle} overload.
	 *
	 * @param particle Particle to test; null is not simple.
	 * @return True when no extra particle data is required.
	 */
	public static boolean isSimple(Particle particle)
	{
		if (particle == null) return false;
		try
		{
			Class<?> dataType = particle.getDataType();
			if (dataType == null) return true;
			return dataType == Void.class || dataType == Void.TYPE;
		}
		catch (Throwable t)
		{
			return true;
		}
	}
	
	/**
	 * Sanitizes an allow-list of particle ids into canonical {@code PARTICLE_*} ids.
	 *
	 * @param ids Raw ids; null becomes empty.
	 * @return Ordered set of valid config ids.
	 */
	public static Set<String> sanitizeIds(Set<String> ids)
	{
		Set<String> sanitized = new LinkedHashSet<>();
		if (ids == null) return sanitized;
		for (String raw : ids)
		{
			ParticleGateType type = parse(raw);
			if (type == null) continue;
			sanitized.add(type.getConfigId());
		}
		return sanitized;
	}
	
	/**
	 * @return Default canonical config ids for new / migrated configs.
	 */
	public static Set<String> defaultConfigIds()
	{
		return sanitizeIds(DEFAULT_RAW_IDS);
	}
	
	@Override
	public boolean isParticleFill()
	{
		return true;
	}
	
	@Override
	public Particle getParticle()
	{
		return this.particle;
	}
	
	@Override
	public Material getBaseMaterial()
	{
		return Material.AIR;
	}
	
	@Override
	public Material getClientDisplayMaterial()
	{
		return Material.AIR;
	}
	
	@Override
	public int getEmittedBlockLightLevel(GateOrientation orientation)
	{
		return DISPLAY_BLOCK_LIGHT_LEVEL;
	}
	
	@Override
	public boolean usesBlockDisplay(GateOrientation orientation)
	{
		return false;
	}
	
	@Override
	public boolean isEnterable()
	{
		return true;
	}
	
	@Override
	public boolean shouldPreventDamage(DamageCause cause)
	{
		return false;
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
		return true;
	}
	
	@Override
	public String getConfigId()
	{
		return ID_PREFIX + this.particle.name();
	}
	
	/**
	 * @return Player-facing name (e.g. {@code Soul Fire Flame}).
	 */
	public String getDisplayName()
	{
		return Txt.getNicedEnum(this.particle, " ");
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!(obj instanceof ParticleGateType that)) return false;
		return this.particle == that.particle;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.particle);
	}
	
	@Override
	public String toString()
	{
		return "ParticleGateType[" + this.particle + "]";
	}
}
