package com.massivecraft.factions.util;

import com.massivecraft.factions.teleport.DestinationWildtp;
import com.massivecraft.massivecore.ps.PS;

/**
 * Outcome of a wilderness destination search (random or predefined).
 */
public final class WildTpSearchResult
{
	/** Validated destination. */
	public final PS ps;
	/** True when the spot came from a predefined location (uses predefined revalidation). */
	public final boolean fromPredefined;

	private WildTpSearchResult(PS ps, boolean fromPredefined)
	{
		this.ps = ps;
		this.fromPredefined = fromPredefined;
	}

	/**
	 * @param ps random/claim-edge destination
	 * @return result using full random revalidation rules
	 */
	public static WildTpSearchResult random(PS ps)
	{
		return new WildTpSearchResult(ps, false);
	}

	/**
	 * @param status predefined inspect/select status (must be usable with non-null ps)
	 * @return result using predefined revalidation rules
	 */
	public static WildTpSearchResult predefined(WildTpPredefinedSelector.Status status)
	{
		return new WildTpSearchResult(status.ps, true);
	}

	/**
	 * Applies this result onto a destination handle for delayed teleport.
	 *
	 * @param destination session destination
	 */
	public void applyTo(DestinationWildtp destination)
	{
		destination.setResolved(this.ps, this.fromPredefined);
	}

}
