package com.massivecraft.factions.util;

import com.massivecraft.massivecore.util.ReflectionUtil;
import org.bukkit.World;

/**
 * Spigot/Paper dual-support chunk load helper used while searching for wildtp destinations.
 * <p>
 * Spigot uses sync {@link World#getChunkAt(int, int)}. Paper's async chunk API is detected via
 * reflection/probe only so Spigot classloading never hard-links Paper-only types.
 * Search validation still uses sync loads on the main thread so block reads are immediately valid;
 * the Paper probe records capability for optional future async pre-load strategies.
 * </p>
 */
public class WildTpChunkLoadBackend
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //

	private static final WildTpChunkLoadBackend i = create();

	/**
	 * @return singleton backend instance
	 */
	public static WildTpChunkLoadBackend get() { return i; }

	/** True when a Paper-style async chunk API was detected at construction time. */
	private final boolean paperAsyncAvailable;

	/**
	 * @param paperAsyncAvailable whether Paper async chunk loading was probed successfully
	 */
	private WildTpChunkLoadBackend(boolean paperAsyncAvailable)
	{
		this.paperAsyncAvailable = paperAsyncAvailable;
	}

	/**
	 * @return a backend instance after probing for Paper async APIs
	 */
	private static WildTpChunkLoadBackend create()
	{
		boolean paper = ReflectionUtil.classExists("io.papermc.paper.chunk.system.ChunkSystem")
			|| ReflectionUtil.classExists("org.bukkit.World") && hasGetChunkAtAsync();
		return new WildTpChunkLoadBackend(paper);
	}

	/**
	 * @return true if {@code World.getChunkAtAsync(int, int)} exists
	 */
	private static boolean hasGetChunkAtAsync()
	{
		try
		{
			World.class.getMethod("getChunkAtAsync", int.class, int.class);
			return true;
		}
		catch (NoSuchMethodException e)
		{
			return false;
		}
	}

	/**
	 * Ensures the chunk is available for block reads on the calling thread.
	 *
	 * @param world world
	 * @param chunkX chunk X
	 * @param chunkZ chunk Z
	 */
	public void ensureLoaded(World world, int chunkX, int chunkZ)
	{
		world.getChunkAt(chunkX, chunkZ);
	}

	/**
	 * @return true if Paper async chunk API was detected at probe time
	 */
	public boolean isPaperAsyncAvailable()
	{
		return this.paperAsyncAvailable;
	}

}
