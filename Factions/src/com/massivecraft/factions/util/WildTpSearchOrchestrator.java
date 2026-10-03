package com.massivecraft.factions.util;

import com.massivecraft.factions.WildTpTeleportMode;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates wilderness destination search for the configured teleport mode.
 * Order for random/hybrid: location cache → pure random → claim-edge band → (hybrid only) predefined.
 */
public final class WildTpSearchOrchestrator
{
	/**
	 * Prevents instantiation; all API methods are static.
	 */
	private WildTpSearchOrchestrator()
	{
	}

	/**
	 * Runs one paced search step using a shared attempt counter.
	 *
	 * @param world target world to search in
	 * @param mode teleport mode
	 * @param attemptsRemaining shared budget; decremented by search tiers that consume attempts
	 * @param batchMax maximum attempts to spend in this call (for pacing); use a large value to exhaust
	 * @return search result, or null if not found yet / budget exhausted without hybrid fallback success
	 */
	public static WildTpSearchResult searchStep(World world, WildTpTeleportMode mode, AtomicInteger attemptsRemaining, int batchMax)
	{
		return searchStep(world, mode, attemptsRemaining, batchMax, null);
	}

	/**
	 * @param excludePlayer ignored by nearby-player checks when selecting predefined spots
	 */
	public static WildTpSearchResult searchStep(World world, WildTpTeleportMode mode, AtomicInteger attemptsRemaining, int batchMax, Player excludePlayer)
	{
		MConf conf = MConf.get();
		WildTpTeleportMode resolved = resolveMode(mode, conf);

		if (resolved == WildTpTeleportMode.PREDEFINED)
		{
			return predefinedOrNull(excludePlayer);
		}

		if (attemptsRemaining.get() <= 0)
		{
			if (resolved == WildTpTeleportMode.HYBRID)
			{
				return predefinedOrNull(excludePlayer);
			}
			return null;
		}

		// Cache first (does not consume attempt budget)
		PS cached = WildTpLocationCache.get().tryTakeValid(world);
		if (cached != null)
		{
			WildTpLocationCache.get().put(cached);
			return WildTpSearchResult.random(cached);
		}

		int before = attemptsRemaining.get();
		AtomicInteger batch = new AtomicInteger(Math.min(before, Math.max(1, batchMax)));

		PS random = WildTpRandomSearch.find(world, batch);
		int spent = Math.min(before, batchMax) - batch.get();
		attemptsRemaining.addAndGet(-spent);
		if (random != null)
		{
			WildTpLocationCache.get().put(random);
			return WildTpSearchResult.random(random);
		}

		if (attemptsRemaining.get() <= 0)
		{
			if (resolved == WildTpTeleportMode.HYBRID) return predefinedOrNull(excludePlayer);
			return null;
		}

		before = attemptsRemaining.get();
		batch = new AtomicInteger(Math.min(before, Math.max(1, batchMax)));
		PS edge = WildTpClaimEdgeSearch.find(world, batch);
		spent = Math.min(before, batchMax) - batch.get();
		attemptsRemaining.addAndGet(-spent);
		if (edge != null)
		{
			WildTpLocationCache.get().put(edge);
			return WildTpSearchResult.random(edge);
		}

		if (attemptsRemaining.get() <= 0 && resolved == WildTpTeleportMode.HYBRID)
		{
			return predefinedOrNull(excludePlayer);
		}

		return null;
	}

	/**
	 * Exhausts the configured attempt budget (resolve-before-delay path).
	 *
	 * @param world target world
	 * @param mode teleport mode
	 * @return search result or null if none found
	 */
	public static WildTpSearchResult search(World world, WildTpTeleportMode mode)
	{
		return search(world, mode, null);
	}

	/**
	 * @param excludePlayer ignored by nearby-player checks when selecting predefined spots
	 */
	public static WildTpSearchResult search(World world, WildTpTeleportMode mode, Player excludePlayer)
	{
		MConf conf = MConf.get();
		WildTpTeleportMode resolved = resolveMode(mode, conf);

		if (resolved == WildTpTeleportMode.PREDEFINED)
		{
			return predefinedOrNull(excludePlayer);
		}

		AtomicInteger attempts = new AtomicInteger(Math.max(1, conf.wildTpMaxAttempts));
		WildTpSearchResult found = null;
		while (attempts.get() > 0 && found == null)
		{
			found = searchStep(world, resolved, attempts, attempts.get(), excludePlayer);
			if (found != null) return found;
			if (attempts.get() <= 0) break;
			if (found == null && attempts.get() > 0)
			{
				PS random = WildTpRandomSearch.find(world, attempts);
				if (random != null)
				{
					WildTpLocationCache.get().put(random);
					return WildTpSearchResult.random(random);
				}
			}
		}
		if (resolved == WildTpTeleportMode.HYBRID)
		{
			return predefinedOrNull(excludePlayer);
		}
		return null;
	}

	private static WildTpSearchResult predefinedOrNull(Player excludePlayer)
	{
		WildTpPredefinedSelector.Status status = WildTpPredefinedSelector.selectStatus(excludePlayer);
		return status == null ? null : WildTpSearchResult.predefined(status);
	}

	/**
	 * Uses the given mode, or falls back to config when null.
	 *
	 * @param mode caller-supplied mode (may be null)
	 * @param conf active config
	 * @return never-null mode
	 */
	private static WildTpTeleportMode resolveMode(WildTpTeleportMode mode, MConf conf)
	{
		return mode != null ? mode : conf.getWildTpTeleportMode();
	}

}
