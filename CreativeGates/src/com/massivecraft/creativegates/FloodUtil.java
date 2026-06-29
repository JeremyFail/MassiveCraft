package com.massivecraft.creativegates;

import com.massivecraft.creativegates.entity.MConf;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.Set;

/**
 * Flood-fill utilities for discovering gate interiors and frame boundaries during creation.
 */
public class FloodUtil
{
	// -------------------------------------------- //
	// GATE DISCOVERY
	// -------------------------------------------- //
	
	/**
	 * Attempts to find a valid gate frame around {@code startBlock} by flood-filling void (air)
	 * blocks along orientation-specific axes.
	 * <p>
	 * Vertical gates expand north/south or east/west and vertically; horizontal gates expand
	 * only on the XZ plane. When more than one orientation matches the same void region,
	 * {@code pitch} and {@code absYaw} are used to pick the intended type.
	 * </p>
	 *
	 * @param startBlock The void block inside the frame where the flood fill begins.
	 * @param absYaw The player's absolute yaw, used to disambiguate north/south vs east/west walls.
	 * @param pitch The player's pitch; values at or beyond 45 degrees favor horizontal gates.
	 * @return Flood-fill results including orientation, interior blocks, and frame shell,
	 *         or {@code null} if no valid gate region was found or the area exceeds {@link MConf#getMaxarea()}.
	 */
	public static GateFloodInfo getGateFloodInfo(Block startBlock, float absYaw, float pitch)
	{
		MConf mconf = MConf.get();
		
		// Try each orientation independently.
		Set<Block> blocksNS = getFloodBlocks(startBlock, new HashSet<>(), GateOrientation.NS.expandFaces, mconf.getMaxarea());
		Set<Block> blocksWE = getFloodBlocks(startBlock, new HashSet<>(), GateOrientation.WE.expandFaces, mconf.getMaxarea());
		Set<Block> blocksHorizontal = null;
		if (mconf.isHorizontalGatesEnabled())
		{
			blocksHorizontal = getFloodBlocks(startBlock, new HashSet<>(), GateOrientation.HORIZONTAL.expandFaces, mconf.getMaxarea());
			// Reject thin vertical shafts that happen to fit in one layer.
			if (blocksHorizontal != null && !isLikelyHorizontalGate(blocksHorizontal))
			{
				blocksHorizontal = null;
			}
		}
		
		// Looking steeply up or down suggests the player is framing a floor/ceiling gate.
		boolean preferHorizontal = Math.abs(pitch) >= 45f;
		
		// Disambiguate when multiple orientations match the same void region.
		if (blocksHorizontal != null && blocksNS != null && blocksWE != null)
		{
			if (preferHorizontal)
			{
				return buildGateFloodInfo(GateOrientation.HORIZONTAL, blocksHorizontal);
			}
			if (absYaw <= 135 && absYaw > 45)
			{
				return buildGateFloodInfo(GateOrientation.NS, blocksNS);
			}
			return buildGateFloodInfo(GateOrientation.WE, blocksWE);
		}
		
		if (blocksHorizontal != null && blocksNS != null)
		{
			if (preferHorizontal) return buildGateFloodInfo(GateOrientation.HORIZONTAL, blocksHorizontal);
			return buildGateFloodInfo(GateOrientation.NS, blocksNS);
		}
		
		if (blocksHorizontal != null && blocksWE != null)
		{
			if (preferHorizontal) return buildGateFloodInfo(GateOrientation.HORIZONTAL, blocksHorizontal);
			return buildGateFloodInfo(GateOrientation.WE, blocksWE);
		}
		
		if (blocksNS != null && blocksWE != null)
		{
			if (absYaw <= 135 && absYaw > 45)
			{
				return buildGateFloodInfo(GateOrientation.NS, blocksNS);
			}
			return buildGateFloodInfo(GateOrientation.WE, blocksWE);
		}
		
		if (blocksHorizontal != null) return buildGateFloodInfo(GateOrientation.HORIZONTAL, blocksHorizontal);
		if (blocksNS != null) return buildGateFloodInfo(GateOrientation.NS, blocksNS);
		if (blocksWE != null) return buildGateFloodInfo(GateOrientation.WE, blocksWE);
		
		return null;
	}
	
	/**
	 * Wraps an interior flood-fill result with the surrounding one-block frame shell.
	 *
	 * @param orientation The resolved gate orientation.
	 * @param interior The void blocks that form the portal interior.
	 * @return A {@link GateFloodInfo} containing orientation, interior, and full coords (interior + frame).
	 */
	private static GateFloodInfo buildGateFloodInfo(GateOrientation orientation, Set<Block> interior)
	{
		Set<Block> all = expandedByOne(interior, orientation.expandFaces);
		return new GateFloodInfo(orientation, interior, all);
	}
	
	/**
	 * Returns whether a flood-filled region looks like a horizontal (floor/ceiling) gate rather
	 * than a single-block pit or vertical shaft cross-section.
	 * <p>
	 * Horizontal gates must span at least two blocks and lie entirely on one Y layer.
	 * </p>
	 *
	 * @param interiorBlocks The interior blocks from a horizontal-axis flood fill.
	 * @return {@code true} if the region is a single Y layer with more than one block; {@code false} otherwise.
	 */
	public static boolean isLikelyHorizontalGate(Set<Block> interiorBlocks)
	{
		if (interiorBlocks == null || interiorBlocks.size() < 2) return false;
		
		int y = interiorBlocks.iterator().next().getY();
		for (Block block : interiorBlocks)
		{
			if (block.getY() != y) return false;
		}
		return true;
	}
	
	// -------------------------------------------- //
	// FLOOD FILL
	// -------------------------------------------- //
	
	/**
	 * Recursively flood-fills void blocks reachable from {@code startBlock} along {@code expandFaces}.
	 * <p>
	 * Non-void blocks act as frame boundaries and are not included in the result. The fill aborts
	 * and returns {@code null} when the collected set exceeds {@code maxarea}.
	 * </p>
	 *
	 * @param startBlock The block to visit next in the flood fill.
	 * @param foundBlocks Blocks collected so far; must be a mutable set (may be empty).
	 * @param expandFaces Directions along which void blocks are explored.
	 * @param maxarea Maximum allowed interior size before the fill is rejected.
	 * @return The collected void blocks, or {@code null} if {@code foundBlocks} is {@code null}
	 *         or the area limit was exceeded.
	 */
	public static Set<Block> getFloodBlocks(Block startBlock, Set<Block> foundBlocks, Set<BlockFace> expandFaces, int maxarea)
	{
		if (foundBlocks == null)
		{
			return null;
		}
		
		if (foundBlocks.size() > maxarea)
		{
			return null;
		}
		
		if (foundBlocks.contains(startBlock))
		{
			return foundBlocks;
		}
		
		if (CreativeGates.isVoid(startBlock))
		{
			foundBlocks.add(startBlock);
			
			for (BlockFace face : expandFaces)
			{
				Block potentialBlock = startBlock.getRelative(face);
				foundBlocks = getFloodBlocks(potentialBlock, foundBlocks, expandFaces, maxarea);
			}
		}
		
		return foundBlocks;
	}
	
	/**
	 * Expands a block set by one layer in each direction given by {@code expandFaces}.
	 * <p>
	 * Used to derive the frame shell surrounding a gate interior without including interior blocks
	 * that were already in the input set.
	 * </p>
	 *
	 * @param blocks The interior (or other base) blocks to expand around.
	 * @param expandFaces Directions in which adjacent blocks are added.
	 * @return A new set containing {@code blocks} plus all directly adjacent blocks along {@code expandFaces}.
	 */
	public static Set<Block> expandedByOne(Set<Block> blocks, Set<BlockFace> expandFaces)
	{
		Set<Block> ret = new HashSet<>(blocks);
		
		for (Block block : blocks)
		{
			for (BlockFace face : expandFaces)
			{
				Block potentialBlock = block.getRelative(face);
				if (ret.contains(potentialBlock)) continue;
				ret.add(potentialBlock);
			}
		}
		
		return ret;
	}
	
}
