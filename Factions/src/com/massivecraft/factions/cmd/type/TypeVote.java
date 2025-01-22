package com.massivecraft.factions.cmd.type;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Vote;

import java.util.Collection;

public class TypeVote extends TypeEntityInternalFaction<Vote>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static TypeVote i = new TypeVote();
	public static TypeVote get() { return i; }
	private TypeVote()
	{
		super(Vote.class);
	}

	public static TypeVote get(Faction faction) { return new TypeVote(faction); }
	public TypeVote(Faction faction)
	{
		super(Vote.class, faction);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Collection<Vote> getAll(Faction faction)
	{
		return faction.getVotes().getAll();
	}

}
