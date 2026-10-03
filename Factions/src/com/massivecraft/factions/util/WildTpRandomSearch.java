package com.massivecraft.factions.util;

import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pure-random wilderness candidate search within a shared attempt budget.
 */
public final class WildTpRandomSearch
{
	/**
	 * Prevents instantiation; all API methods are static.
	 */
	private WildTpRandomSearch()
	{
	}

	/**
	 * Attempts to find a random valid wilderness location.
	 *
	 * @param world target world
	 * @param attemptsRemaining shared attempt counter (decremented once per try)
	 * @return valid PS or null if the budget is exhausted without a hit
	 */
	public static PS find(World world, AtomicInteger attemptsRemaining)
	{
		while (attemptsRemaining.get() > 0)
		{
			attemptsRemaining.decrementAndGet();
			int[] xz = WildTpSearchUtil.randomXZInsideBorder(world);
			WildTpSearchUtil.ensureChunkLoaded(world, xz[0], xz[1]);
			Location loc = WildTpDestinationValidator.resolveTopLocation(world, xz[0], xz[1]);
			if (loc == null) continue;
			PS ps = PS.valueOf(loc);
			if (WildTpDestinationValidator.isValid(ps, false))
			{
				return ps;
			}
		}
		return null;
	}

}
