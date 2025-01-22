package com.massivecraft.factions.predicate;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;

import java.util.function.Predicate;

public class PredicateMPlayerRank implements Predicate<MPlayer>
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final Rank rank;
	public Rank getRank() { return this.rank; }
	
	// -------------------------------------------- //
	// INSTANCE AND CONTRUCT
	// -------------------------------------------- //
	
	public static PredicateMPlayerRank get(Rank rank) { return new PredicateMPlayerRank(rank); }
	public PredicateMPlayerRank(Rank rank)
	{
		this.rank = rank;
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public boolean test(MPlayer mplayer)
	{
		if (mplayer == null) return false;
		Faction faction = mplayer.getFaction();
		if (!faction.hasRank(this.getRank())) throw new IllegalStateException("rank: " + rank.getId() + " player:" + mplayer.getId());
		return mplayer.getRank() == this.rank;
	}
}
