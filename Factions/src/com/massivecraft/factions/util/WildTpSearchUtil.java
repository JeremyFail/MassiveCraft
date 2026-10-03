package com.massivecraft.factions.util;

import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared helpers for random points inside the world border and chunk loading during search.
 */
public final class WildTpSearchUtil
{
	/**
	 * Prevents instantiation; all API methods are static.
	 */
	private WildTpSearchUtil()
	{
	}

	/**
	 * Picks a random block X/Z inside the world border with a small margin from the edge.
	 *
	 * @param world target world
	 * @return int array {@code {x, z}}
	 */
	public static int[] randomXZInsideBorder(World world)
	{
		WorldBorder border = world.getWorldBorder();
		double half = Math.max(16D, border.getSize() / 2D - 16D);
		double cx = border.getCenter().getX();
		double cz = border.getCenter().getZ();
		ThreadLocalRandom r = ThreadLocalRandom.current();
		int x = (int) Math.floor(cx + r.nextDouble(-half, half));
		int z = (int) Math.floor(cz + r.nextDouble(-half, half));
		return new int[]{x, z};
	}

	/**
	 * Ensures a chunk is loaded using the platform backend (Spigot sync / Paper probe).
	 *
	 * @param world target world
	 * @param blockX block X (converted to chunk X internally)
	 * @param blockZ block Z (converted to chunk Z internally)
	 */
	public static void ensureChunkLoaded(World world, int blockX, int blockZ)
	{
		WildTpChunkLoadBackend.get().ensureLoaded(world, blockX >> 4, blockZ >> 4);
	}

}
