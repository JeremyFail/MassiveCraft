package com.massivecraft.factions.cmd.type;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Warp;

import java.util.Collection;

public class TypeWarp extends TypeEntityInternalFaction<Warp>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static TypeWarp i = new TypeWarp();
	public static TypeWarp get() { return i; }
	private TypeWarp()
	{
		super(Warp.class);
	}

	public static TypeWarp get(Faction faction) { return new TypeWarp(faction); }
	public TypeWarp(Faction faction)
	{
		super(Warp.class, faction);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Collection<Warp> getAll(Faction faction)
	{
		return faction.getWarps().getAll();
	}

}
