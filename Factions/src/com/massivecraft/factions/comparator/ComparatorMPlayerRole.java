package com.massivecraft.factions.comparator;

import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.Named;
import com.massivecraft.massivecore.comparator.ComparatorAbstract;

public class ComparatorMPlayerRole extends ComparatorAbstract<MPlayer> implements Named
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static ComparatorMPlayerRole i = new ComparatorMPlayerRole();
	public static ComparatorMPlayerRole get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String getName()
	{
		return "Rank";
	}
	
	@Override
	public int compareInner(MPlayer m1, MPlayer m2)
	{
		// Rank
		if (m1.getFaction() != m2.getFaction()) throw new IllegalArgumentException("Noncomparable players");
		Rank r1 = m1.getRank();
		Rank r2 = m2.getRank();
		return r2.getPriority() - r1.getPriority()	;
	}

}
