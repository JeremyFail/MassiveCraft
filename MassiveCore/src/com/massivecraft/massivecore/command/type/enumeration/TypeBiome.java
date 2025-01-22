package com.massivecraft.massivecore.command.type.enumeration;

import org.bukkit.block.Biome;

public class TypeBiome extends TypeOldEnum<Biome>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TypeBiome i = new TypeBiome();
	public static TypeBiome get() { return i; }
	public TypeBiome()
	{
		super(Biome.class);
	}

}
