package com.massivecraft.massivecore.command.type.enumeration;

import org.bukkit.Particle;

public class TypeParticle extends TypeEnum<Particle>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TypeParticle i = new TypeParticle();
	public static TypeParticle get() { return i; }
	public TypeParticle()
	{
		super(Particle.class);
	}

}
