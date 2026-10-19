package com.massivecraft.creativegates.comparator;

import com.massivecraft.creativegates.entity.UGate;

import java.util.Comparator;

/**
 * Orders gates by explicit {@code chainIndex} ascending, then by exit location.
 */
public class ChainIndexComparator implements Comparator<UGate>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static ChainIndexComparator i = new ChainIndexComparator();
	public static ChainIndexComparator get() { return i; }
	public ChainIndexComparator() {}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public int compare(UGate o1, UGate o2)
	{
		if (o1 == o2) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		
		int byIndex = Integer.compare(o1.getChainIndex(), o2.getChainIndex());
		if (byIndex != 0) return byIndex;
		
		return ExitComparator.get().compare(o1, o2);
	}
	
}
