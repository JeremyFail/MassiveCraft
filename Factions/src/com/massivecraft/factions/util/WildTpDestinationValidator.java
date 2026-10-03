package com.massivecraft.factions.util;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validates wilderness destination candidates against world border, claims, distances, biome, and surface rules.
 */
public final class WildTpDestinationValidator
{
	/**
	 * Prevents instantiation; all API methods are static.
	 */
	private WildTpDestinationValidator()
	{
	}

	/**
	 * Validates a candidate location under full random/hybrid search rules.
	 *
	 * @param ps candidate PS with world and block coords
	 * @param allowWarzoneSafezone when true, warzone/safezone at the spot is allowed (predefined opt-in)
	 * @return true if the candidate is acceptable under current {@link MConf} wildtp rules
	 */
	public static boolean isValid(PS ps, boolean allowWarzoneSafezone)
	{
		return isValid(ps, allowWarzoneSafezone, null);
	}

	/**
	 * Validates a candidate location under full random/hybrid search rules.
	 *
	 * @param ps candidate PS with world and block coords
	 * @param allowWarzoneSafezone when true, warzone/safezone at the spot is allowed (predefined opt-in)
	 * @param excludePlayer player ignored by the nearby-player check (usually the teleportee)
	 * @return true if the candidate is acceptable under current {@link MConf} wildtp rules
	 */
	public static boolean isValid(PS ps, boolean allowWarzoneSafezone, Player excludePlayer)
	{
		return failureReason(ps, allowWarzoneSafezone, excludePlayer) == null;
	}

	/**
	 * Validates a predefined destination at teleport/list time.
	 * Skips global claim distance, spawn, biomes, and war/safe restrictions (operator-chosen).
	 * Still enforces border, no normal claims at the spot, player distance, and surface/water.
	 *
	 * @param ps candidate PS
	 * @return true if acceptable for a predefined teleport
	 */
	public static boolean isValidPredefined(PS ps)
	{
		return isValidPredefined(ps, null);
	}

	/**
	 * Validates a predefined destination at teleport/list time.
	 *
	 * @param ps candidate PS
	 * @param excludePlayer player ignored by the nearby-player check (list viewer or teleportee)
	 * @return true if acceptable for a predefined teleport
	 */
	public static boolean isValidPredefined(PS ps, Player excludePlayer)
	{
		return failureReasonPredefined(ps, excludePlayer) == null;
	}

	/**
	 * Full random/hybrid failure reason (claims, players, spawn, biome, surface).
	 *
	 * @param ps candidate PS with world and block coords
	 * @param allowWarzoneSafezone when true, warzone/safezone at the spot is allowed (predefined opt-in)
	 * @return null if valid, otherwise a short failure reason
	 */
	public static String failureReason(PS ps, boolean allowWarzoneSafezone)
	{
		return failureReason(ps, allowWarzoneSafezone, null);
	}

	/**
	 * Full random/hybrid failure reason (claims, players, spawn, biome, surface).
	 *
	 * @param ps candidate PS with world and block coords
	 * @param allowWarzoneSafezone when true, warzone/safezone at the spot is allowed (predefined opt-in)
	 * @param excludePlayer player ignored by the nearby-player check
	 * @return null if valid, otherwise a short failure reason
	 */
	public static String failureReason(PS ps, boolean allowWarzoneSafezone, Player excludePlayer)
	{
		return failureReasonShared(ps, allowWarzoneSafezone, false, true, true, true, excludePlayer);
	}

	/**
	 * Predefined failure reason: border, no normal claim at feet, players, surface/water.
	 *
	 * @param ps candidate PS
	 * @return null if valid, otherwise a short failure reason
	 */
	public static String failureReasonPredefined(PS ps)
	{
		return failureReasonPredefined(ps, null);
	}

	/**
	 * Predefined failure reason: border, no normal claim at feet, players, surface/water.
	 *
	 * @param ps candidate PS
	 * @param excludePlayer player ignored by the nearby-player check
	 * @return null if valid, otherwise a short failure reason
	 */
	public static String failureReasonPredefined(PS ps, Player excludePlayer)
	{
		return failureReasonShared(ps, true, true, false, false, false, excludePlayer);
	}

	/**
	 * Shared validation core.
	 *
	 * @param allowWarzoneSafezone when not {@code predefinedTerritory}, war/safe at spot allowed if true
	 * @param predefinedTerritory when true, only normal claims at the spot are rejected (war/safe always OK)
	 * @param checkClaims apply {@link MConf#wildTpMinChunksFromClaims}
	 * @param checkSpawn apply {@link MConf#wildTpMinChunksFromSpawn}
	 * @param checkBiome apply {@link MConf#wildTpDisallowedBiomes}
	 * @param excludePlayer player ignored by the nearby-player check (may be null)
	 */
	private static String failureReasonShared(PS ps, boolean allowWarzoneSafezone, boolean predefinedTerritory, boolean checkClaims, boolean checkSpawn, boolean checkBiome, Player excludePlayer)
	{
		if (ps == null) return "Location data is missing";
		World world;
		try
		{
			world = ps.asBukkitWorld();
		}
		catch (Exception e)
		{
			return "That world is not available";
		}
		if (world == null) return "That world is not available";

		Location loc = ps.asBukkitLocation();
		if (loc == null) return "Location data is missing";

		// Always respect world border (no config toggle)
		if (!world.getWorldBorder().isInside(loc)) return "Outside the world border";

		Faction faction = BoardColl.get().getFactionAt(ps);
		if (predefinedTerritory)
		{
			// Operator placed the spot — only block standing inside a normal faction claim
			if (faction.isNormal())
			{
				return "Inside " + faction.getName() + " territory";
			}
		}
		else if (faction.isNone())
		{
			// wilderness OK
		}
		else if (allowWarzoneSafezone && (faction.isWarZone() || faction.isSafeZone()))
		{
			// allowed only when caller opts in
		}
		else
		{
			return "Inside " + faction.getName() + " territory";
		}

		MConf conf = MConf.get();
		PS chunk = ps.getChunk(true);

		if (checkClaims)
		{
			String claimsFail = farEnoughFromClaimsReason(chunk, conf.wildTpMinChunksFromClaims);
			if (claimsFail != null) return claimsFail;
		}

		String playersFail = farEnoughFromPlayersReason(chunk, conf.wildTpMinChunksFromPlayers, excludePlayer);
		if (playersFail != null) return playersFail;

		if (checkSpawn && !isFarEnoughFromSpawn(chunk, world, conf.wildTpMinChunksFromSpawn))
		{
			return "Too close to world spawn";
		}

		if (checkBiome)
		{
			String biomeFail = biomeAllowedReason(world, loc, conf.wildTpDisallowedBiomes);
			if (biomeFail != null) return biomeFail;
		}

		if (conf.wildTpCheckSurfaceBlock || conf.wildTpAvoidWater)
		{
			String surfaceFail = surfaceSafeReason(world, loc, conf.wildTpCheckSurfaceBlock, conf.wildTpAvoidWater);
			if (surfaceFail != null) return surfaceFail;
		}

		return null;
	}

	/**
	 * Resolves a standable top location for block X/Z in the world.
	 * Uses {@link HeightMap#MOTION_BLOCKING_NO_LEAVES}, then finds solid ground with a 2-high clear gap
	 * so feet/head are not inside terrain, leaves, or snow.
	 *
	 * @param world target world
	 * @param blockX block X
	 * @param blockZ block Z
	 * @return location centered on the block (feet Y), or null if unsafe / outside border
	 */
	public static Location resolveTopLocation(World world, int blockX, int blockZ)
	{
		int mapY = world.getHighestBlockYAt(blockX, blockZ, HeightMap.MOTION_BLOCKING_NO_LEAVES);
		int minY = world.getMinHeight();
		int maxY = world.getMaxHeight() - 2;
		if (mapY <= minY + 1) return null;

		// Heightmap Y should be the highest motion-blocking block; if that cell is not solid (fluids), search down.
		int groundY = -1;
		int probeFrom = Math.min(mapY, maxY);
		for (int y = probeFrom; y >= minY; y--)
		{
			if (world.getBlockAt(blockX, y, blockZ).getType().isSolid())
			{
				groundY = y;
				break;
			}
		}
		if (groundY < 0) return null;

		// From just above solid ground, find the first 2-high non-solid gap (skips leaf/snow stacks above terrain).
		for (int feetY = groundY + 1; feetY <= maxY; feetY++)
		{
			Block ground = world.getBlockAt(blockX, feetY - 1, blockZ);
			Block feet = world.getBlockAt(blockX, feetY, blockZ);
			Block head = world.getBlockAt(blockX, feetY + 1, blockZ);

			if (!ground.getType().isSolid())
			{
				// Left the solid column (cave/gap); stop - no standable surface here
				break;
			}
			if (feet.getType().isSolid() || head.getType().isSolid())
			{
				continue;
			}

			Location loc = new Location(world, blockX + 0.5D, feetY, blockZ + 0.5D);
			if (!world.getWorldBorder().isInside(loc)) return null;
			return loc;
		}
		return null;
	}

	/**
	 * @param chunkOrPos chunk or block PS (chunk coords calculated)
	 * @return true if far enough from normal claims per {@link MConf#wildTpMinChunksFromClaims}
	 */
	public static boolean isFarEnoughFromClaims(PS chunkOrPos)
	{
		if (chunkOrPos == null) return false;
		return farEnoughFromClaimsReason(chunkOrPos.getChunk(true), MConf.get().wildTpMinChunksFromClaims) == null;
	}

	/**
	 * Chunk Chebyshev distance between two chunk PS values.
	 *
	 * @param a first position
	 * @param b second position
	 * @return max of absolute chunk-X and chunk-Z deltas
	 */
	public static int chunkDistance(PS a, PS b)
	{
		a = a.getChunk(true);
		b = b.getChunk(true);
		return Math.max(Math.abs(a.getChunkX() - b.getChunkX()), Math.abs(a.getChunkZ() - b.getChunkZ()));
	}

	/**
	 * @param chunk candidate chunk
	 * @param minChunks minimum Chebyshev distance from normal claims (0 disables)
	 * @return failure reason, or null if far enough
	 */
	private static String farEnoughFromClaimsReason(PS chunk, int minChunks)
	{
		if (minChunks <= 0) return null;
		Set<PS> nearby = BoardColl.getNearbyChunks(chunk, minChunks);
		for (PS near : nearby)
		{
			if (near.equals(chunk)) continue;
			Faction f = BoardColl.get().getFactionAt(near);
			if (f.isNormal())
			{
				return "Too close to " + f.getName() + " territory";
			}
		}
		return null;
	}

	/**
	 * @param chunk candidate chunk
	 * @param minChunks minimum Chebyshev distance from online players (0 disables)
	 * @param excludePlayer player to ignore (list viewer / teleportee); may be null
	 * @return failure reason for the first too-close player, or null if far enough
	 */
	private static String farEnoughFromPlayersReason(PS chunk, int minChunks, Player excludePlayer)
	{
		if (minChunks <= 0) return null;
		for (Player player : Bukkit.getOnlinePlayers())
		{
			if (excludePlayer != null && player.equals(excludePlayer)) continue;
			if (player.getWorld() == null) continue;
			if (!player.getWorld().getName().equals(chunk.getWorld())) continue;
			PS playerChunk = PS.valueOf(player.getLocation()).getChunk(true);
			int dist = chunkDistance(chunk, playerChunk);
			if (dist < minChunks)
			{
				return "Too close to player " + player.getName();
			}
		}
		return null;
	}

	/**
	 * @param chunk candidate chunk
	 * @param world world for spawn lookup
	 * @param minChunks minimum Chebyshev distance from spawn (0 disables)
	 * @return true if far enough from world spawn
	 */
	private static boolean isFarEnoughFromSpawn(PS chunk, World world, int minChunks)
	{
		if (minChunks <= 0) return true;
		PS spawnChunk = PS.valueOf(world.getSpawnLocation()).getChunk(true);
		return chunkDistance(chunk, spawnChunk) >= minChunks;
	}

	/**
	 * @param world world
	 * @param loc location to sample biome at
	 * @param disallowed biome name list from config
	 * @return failure reason, or null if allowed
	 */
	private static String biomeAllowedReason(World world, Location loc, List<String> disallowed)
	{
		if (disallowed == null || disallowed.isEmpty()) return null;
		Biome biome = world.getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		String name = biome.name();
		for (String banned : disallowed)
		{
			if (banned == null) continue;
			if (name.equalsIgnoreCase(banned.trim())) return "Biome not allowed (" + name + ")";
		}
		return null;
	}

	/**
	 * @param world world
	 * @param loc feet location
	 * @param checkSurface require solid ground and clear feet/head
	 * @param avoidWater reject water-like materials
	 * @return failure reason, or null if surface rules pass
	 */
	private static String surfaceSafeReason(World world, Location loc, boolean checkSurface, boolean avoidWater)
	{
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		Block feet = world.getBlockAt(x, y, z);
		Block head = world.getBlockAt(x, y + 1, z);
		Block ground = world.getBlockAt(x, y - 1, z);

		if (checkSurface)
		{
			if (!ground.getType().isSolid()) return "Nothing solid to stand on (" + prettyMaterial(ground.getType()) + ")";
			if (feet.getType().isSolid()) return "Standing space is blocked (" + prettyMaterial(feet.getType()) + ")";
			if (head.getType().isSolid()) return "Head space is blocked (" + prettyMaterial(head.getType()) + ")";
		}

		if (avoidWater)
		{
			if (isWater(feet.getType()) || isWater(ground.getType()) || isWater(head.getType()))
			{
				return "Location is in or on water";
			}
		}

		return null;
	}

	/**
	 * @param material block material
	 * @return true if the material is treated as water for avoidance
	 */
	private static boolean isWater(Material material)
	{
		String name = material.name().toUpperCase(Locale.ROOT);
		return name.contains("WATER") || name.equals("KELP") || name.equals("KELP_PLANT") || name.equals("SEAGRASS") || name.equals("TALL_SEAGRASS");
	}

	/**
	 * @param material block material
	 * @return readable material name for player-facing messages
	 */
	private static String prettyMaterial(Material material)
	{
		String name = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
		if (name.isEmpty()) return material.name();
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

}
