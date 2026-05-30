package com.massivecraft.creativegates;

import org.bukkit.block.Block;

import java.util.Set;

/**
 * Information about a flood fill operation for a gate.
 */
public final class GateFloodInfo
{
	public final GateOrientation orientation;
	public final Set<Block> interiorBlocks;
	public final Set<Block> allBlocks;
	
	/**
	 * Creates a new GateFloodInfo.
	 * 
	 * @param orientation The orientation of the gate.
	 * @param interiorBlocks The blocks that are part of the interior of the gate.
	 * @param allBlocks The blocks that are part of the gate.
	 */
	public GateFloodInfo(GateOrientation orientation, Set<Block> interiorBlocks, Set<Block> allBlocks)
	{
		this.orientation = orientation;
		this.interiorBlocks = interiorBlocks;
		this.allBlocks = allBlocks;
	}
}
