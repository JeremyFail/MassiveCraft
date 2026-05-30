package com.massivecraft.creativegates;

import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Safety checks for gate teleports and horizontal gate launches.
 * <p>
 * Determines whether a destination has enough space for a player and whether a
 * horizontal portal face (top or bottom) is blocked by solid blocks.
 * </p>
 */
public final class GateTeleportSafety
{
	private GateTeleportSafety() { }
	
	/**
	 * Returns whether a player can pass through or stand inside the given block.
	 * <p>
	 * Air, gate fluids (water/lava), nether portal blocks, and non-solid blocks count as passable.
	 * </p>
	 *
	 * @param block The block to test; {@code null} is not passable.
	 * @return {@code true} if a player can occupy or move through the block.
	 */
	public static boolean isPassableForPlayer(Block block)
	{
		if (block == null) return false;
		Material material = block.getType();
		if (CreativeGates.isVoid(material)) return true;
		if (CreativeGates.isFluidFillMaterial(material)) return true;
		if (material == Material.NETHER_PORTAL) return true;
		return !material.isSolid();
	}
	
	/**
	 * Returns whether the destination position is safe for the player to stand in.
	 *
	 * @param player The player who would be teleported (reserved for future checks).
	 * @param destination The destination in MassiveCore {@link PS} form.
	 * @return {@code true} if both the feet and head blocks at the destination are passable;
	 *         {@code false} if {@code destination} is {@code null} or not in a loaded world.
	 */
	public static boolean isDestinationSafe(Player player, PS destination)
	{
		if (destination == null) return false;
		try
		{
			return isDestinationSafe(player, destination.asBukkitLocation(true));
		}
		catch (IllegalStateException e)
		{
			return false;
		}
	}
	
	/**
	 * Returns whether the destination location is safe for the player to stand in.
	 * <p>
	 * Requires the block at the location and the block directly above it to be passable
	 * (standard two-block-tall player clearance).
	 * </p>
	 *
	 * @param player The player who would be teleported (reserved for future checks).
	 * @param location The destination location.
	 * @return {@code true} if feet and head space are passable;
	 *         {@code false} if {@code player}, {@code location}, or its world is {@code null}.
	 */
	public static boolean isDestinationSafe(Player player, Location location)
	{
		if (player == null || location == null || location.getWorld() == null) return false;
		
		World world = location.getWorld();
		int baseX = location.getBlockX();
		int baseY = location.getBlockY();
		int baseZ = location.getBlockZ();
		
		if (!canOccupy(world, baseX, baseY, baseZ)) return false;
		if (!canOccupy(world, baseX, baseY + 1, baseZ)) return false;
		
		return true;
	}
	
	/**
	 * Returns whether the block at the given world coordinates is passable for a player.
	 *
	 * @param world The world containing the block.
	 * @param x Block X coordinate.
	 * @param y Block Y coordinate.
	 * @param z Block Z coordinate.
	 * @return {@code true} if the block is passable.
	 */
	private static boolean canOccupy(World world, int x, int y, int z)
	{
		return isPassableForPlayer(world.getBlockAt(x, y, z));
	}
	
	/**
	 * Returns whether the given face of the portal is fully blocked (no usable exit).
	 * <p>
	 * For each content block, the adjacent block on {@code face} is examined. If that
	 * neighbor is not part of the portal interior and is passable, the face is considered
	 * open. The face is blocked only when every content block has a solid or interior
	 * neighbor on that side.
	 * </p>
	 *
	 * @param contentBlocks Portal interior blocks (water/lava/portal).
	 * @param face The face to test, typically {@link BlockFace#UP} or {@link BlockFace#DOWN}.
	 * @return {@code true} if no passable space exists outside the portal on this face;
	 *         {@code false} if at least one content block borders open space on {@code face}.
	 */
	public static boolean isFaceBlocked(Collection<Block> contentBlocks, BlockFace face)
	{
		Set<Block> contentSet = new HashSet<>(contentBlocks);
		boolean foundOpen = false;
		
		for (Block block : contentBlocks)
		{
			Block outside = block.getRelative(face);
			// Another portal block on this face does not count as an exit.
			if (contentSet.contains(outside)) continue;
			if (isPassableForPlayer(outside))
			{
				foundOpen = true;
				break;
			}
		}
		
		return !foundOpen;
	}
	
}
