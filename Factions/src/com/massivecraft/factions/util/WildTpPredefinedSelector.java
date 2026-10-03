package com.massivecraft.factions.util;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.WildTpPredefinedLocation;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Selects and validates operator-defined predefined wilderness locations.
 * Honors {@link MConf#isWildTpPredefinedRadiusSoft()} vs strict radius enforcement.
 */
public final class WildTpPredefinedSelector
{
	/**
	 * Prevents instantiation; all API methods are static.
	 */
	private WildTpPredefinedSelector()
	{
	}

	/**
	 * Picks a random valid predefined location under current enforcement rules.
	 * Soft: only clear locations when any exist; otherwise may use non-clear.
	 * Strict: only clear locations.
	 *
	 * @return location PS or null if none are usable
	 */
	public static PS select()
	{
		return select(null);
	}

	/**
	 * @param excludePlayer ignored by nearby-player checks (usually the teleportee)
	 * @return location PS or null if none are usable
	 */
	public static PS select(Player excludePlayer)
	{
		Status status = selectStatus(excludePlayer);
		return status == null ? null : status.ps;
	}

	/**
	 * Same as {@link #select()} but returns the full status (for revalidation metadata).
	 *
	 * @return usable status, or null if none are usable
	 */
	public static Status selectStatus()
	{
		return selectStatus(null);
	}

	/**
	 * @param excludePlayer ignored by nearby-player checks (usually the teleportee)
	 * @return usable status, or null if none are usable
	 */
	public static Status selectStatus(Player excludePlayer)
	{
		MConf conf = MConf.get();
		List<WildTpPredefinedLocation> configured = conf.wildTpPredefinedLocations;
		if (configured == null || configured.isEmpty()) return null;

		List<Status> clear = new ArrayList<>();
		List<Status> softOnly = new ArrayList<>();

		for (WildTpPredefinedLocation loc : configured)
		{
			Status status = inspect(loc, excludePlayer);
			if (!status.usable) continue;
			if (status.radiusClear) clear.add(status);
			else softOnly.add(status);
		}

		List<Status> pool;
		if (!clear.isEmpty())
		{
			pool = clear;
		}
		else if (conf.isWildTpPredefinedRadiusSoft() && !softOnly.isEmpty())
		{
			pool = softOnly;
		}
		else
		{
			return null;
		}

		return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
	}

	/**
	 * Inspects a configured location for listing/diagnostics and selection.
	 *
	 * @param loc configured location
	 * @return never-null status describing usability and details
	 */
	public static Status inspect(WildTpPredefinedLocation loc)
	{
		return inspect(loc, null);
	}

	/**
	 * Inspects a configured location for listing/diagnostics and selection.
	 * Uses predefined rules: per-entry no-claim radius, players, surface/water;
	 * skips global claim/spawn/biome and war/safe restrictions.
	 *
	 * @param loc configured location
	 * @param excludePlayer ignored by nearby-player checks (list viewer / teleportee)
	 * @return never-null status describing usability and details
	 */
	public static Status inspect(WildTpPredefinedLocation loc, Player excludePlayer)
	{
		if (loc == null || loc.name == null || loc.name.isEmpty())
		{
			return Status.invalid(loc, "This location has no name");
		}
		if (loc.world == null || loc.world.isEmpty())
		{
			return Status.invalid(loc, "This location has no world set");
		}
		World world = Bukkit.getWorld(loc.world);
		if (world == null)
		{
			return Status.invalid(loc, "World \"" + loc.world + "\" is not loaded");
		}

		Location location = new Location(world, loc.x, loc.y, loc.z);
		PS ps = PS.valueOf(location);
		Faction at = BoardColl.get().getFactionAt(ps);
		String territory = at.getName();

		// No-claim radius only cares about normal faction claims (war/safe ignored)
		boolean clear = isRadiusClearOfNormalClaims(ps, Math.max(0, loc.radiusNoPlayerClaims));
		if (!clear && MConf.get().isWildTpPredefinedRadiusStrict())
		{
			return Status.invalid(loc, ps, territory, false,
				"Faction claims within the " + Math.max(0, loc.radiusNoPlayerClaims) + "-chunk no-claim radius");
		}

		String shared = WildTpDestinationValidator.failureReasonPredefined(ps, excludePlayer);
		if (shared != null)
		{
			return Status.invalid(loc, ps, territory, clear, shared);
		}

		return Status.valid(loc, ps, territory, clear);
	}

	/**
	 * Returns whether no normal faction claims exist within the chunk radius.
	 * Warzone/safezone inside the radius are ignored for predefined spots.
	 *
	 * @param center center chunk/position
	 * @param radiusChunks Chebyshev chunk radius (0 checks only the center chunk)
	 * @return true if the radius has no normal claims
	 */
	public static boolean isRadiusClearOfNormalClaims(PS center, int radiusChunks)
	{
		if (radiusChunks <= 0)
		{
			return !BoardColl.get().getFactionAt(center).isNormal();
		}

		Set<PS> nearby = BoardColl.getNearbyChunks(center.getChunk(true), radiusChunks);
		for (PS near : nearby)
		{
			if (BoardColl.get().getFactionAt(near).isNormal()) return false;
		}
		return true;
	}

	/**
	 * @deprecated use {@link #isRadiusClearOfNormalClaims(PS, int)}; war/safe are ignored for predefined
	 */
	@Deprecated
	public static boolean isRadiusClear(PS center, int radiusChunks, boolean allowWarzoneSafezone)
	{
		return isRadiusClearOfNormalClaims(center, radiusChunks);
	}

	/**
	 * Result of inspecting a predefined location for selection or listing.
	 */
	public static final class Status
	{
		/** Config entry inspected. */
		public final WildTpPredefinedLocation location;
		/** Resolved PS when coordinates could be built; may be null. */
		public final PS ps;
		/** Territory name at the spot when known. */
		public final String territory;
		/** True when the entry can be used by {@link #select()}. */
		public final boolean usable;
		/** True when the no-claims radius is clear of normal claims. */
		public final boolean radiusClear;
		/** Human-readable failure reason when not usable; null when usable. */
		public final String reason;

		private Status(WildTpPredefinedLocation location, PS ps, String territory, boolean usable, boolean radiusClear, String reason)
		{
			this.location = location;
			this.ps = ps;
			this.territory = territory;
			this.usable = usable;
			this.radiusClear = radiusClear;
			this.reason = reason;
		}

		private static Status valid(WildTpPredefinedLocation loc, PS ps, String territory, boolean clear)
		{
			return new Status(loc, ps, territory, true, clear, null);
		}

		private static Status invalid(WildTpPredefinedLocation loc, String reason)
		{
			return new Status(loc, null, null, false, false, reason);
		}

		private static Status invalid(WildTpPredefinedLocation loc, PS ps, String territory, boolean clear, String reason)
		{
			return new Status(loc, ps, territory, false, clear, reason);
		}
	}

}
