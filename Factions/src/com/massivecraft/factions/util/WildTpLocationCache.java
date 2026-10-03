package com.massivecraft.factions.util;

import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded per-world cache of previously validated wilderness locations (runtime only, not persisted).
 * Entries are revalidated on use and evicted when no longer valid.
 */
public final class WildTpLocationCache
{
	private static final WildTpLocationCache i = new WildTpLocationCache();

	/**
	 * @return singleton cache instance
	 */
	public static WildTpLocationCache get() { return i; }

	/** Per-world FIFO queues of cached PS values. */
	private final Map<String, Deque<PS>> byWorld = new ConcurrentHashMap<>();

	/**
	 * Private singleton constructor.
	 */
	private WildTpLocationCache()
	{
	}

	/**
	 * Tries cache entries first; revalidates and evicts invalid ones.
	 *
	 * @param world world whose cache to consult
	 * @return a still-valid cached PS, or null if none
	 */
	public PS tryTakeValid(World world)
	{
		Deque<PS> deque = this.byWorld.get(world.getName());
		if (deque == null || deque.isEmpty()) return null;

		synchronized (deque)
		{
			while (!deque.isEmpty())
			{
				PS ps = deque.pollFirst();
				if (ps == null) continue;
				if (WildTpDestinationValidator.isValid(ps, false))
				{
					return ps;
				}
			}
		}
		return null;
	}

	/**
	 * Adds a newly validated spot to the per-world cache (bounded by config).
	 *
	 * @param ps validated destination
	 */
	public void put(PS ps)
	{
		if (ps == null || ps.getWorld() == null) return;
		int max = MConf.get().wildTpLocationCacheSizePerWorld;
		if (max <= 0) return;

		Deque<PS> deque = this.byWorld.computeIfAbsent(ps.getWorld(), w -> new ArrayDeque<>());
		synchronized (deque)
		{
			deque.addLast(ps);
			while (deque.size() > max)
			{
				deque.pollFirst();
			}
		}
	}

	/**
	 * Convenience: cache a Bukkit location as a PS.
	 *
	 * @param location location to store
	 */
	public void putLocation(Location location)
	{
		if (location == null) return;
		this.put(PS.valueOf(location));
	}

}
