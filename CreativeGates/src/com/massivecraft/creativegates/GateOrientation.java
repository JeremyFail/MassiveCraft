package com.massivecraft.creativegates;

import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.block.BlockFace;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Orientation of a creative gate, which controls flood-fill axes and exit facing.
 * <ul>
 *   <li>{@link #NS} and {@link #WE} — vertical wall gates (north/south or east/west plane).</li>
 *   <li>{@link #HORIZONTAL} — floor or ceiling gates (single Y layer, expands on XZ only).</li>
 * </ul>
 */
public enum GateOrientation
{
	
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //
	
	/** Vertical gate on the north/south plane; flood fill expands N, S, U, D. */
	NS(MUtil.set(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN)),
	
	/** Vertical gate on the east/west plane; flood fill expands E, W, U, D. */
	WE(MUtil.set(BlockFace.WEST, BlockFace.EAST, BlockFace.UP, BlockFace.DOWN)),
	
	/** Horizontal (floor/ceiling) gate; flood fill expands on the XZ plane only. */
	HORIZONTAL(MUtil.set(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)),
	
	// END OF LIST
	;
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	/**
	 * Directions used when flood-filling the gate interior and when expanding the frame by one block.
	 */
	public final Set<BlockFace> expandFaces;
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	/**
	 * @param expandFaces Directions along which void blocks are explored during gate discovery.
	 */
	GateOrientation(Collection<BlockFace> expandFaces)
	{
		Set<BlockFace> expandFacesTemp = new MassiveSet<>(expandFaces);
		this.expandFaces = Collections.unmodifiableSet(expandFacesTemp);
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	/**
	 * @return {@code true} if this orientation is {@link #HORIZONTAL}.
	 */
	public boolean isHorizontal()
	{
		return this == HORIZONTAL;
	}
	
	/**
	 * @return {@code true} if this orientation is {@link #NS} or {@link #WE}.
	 */
	public boolean isVertical()
	{
		return this != HORIZONTAL;
	}
	
	/**
	 * Resolves which horizontal block face the exit lies on relative to a reference gate block.
	 * <p>
	 * For {@link #HORIZONTAL} gates, uses the larger of |ΔX| and |ΔZ| between exit and gate.
	 * For vertical gates, the face is perpendicular to the gate plane (NS → E/W, WE → N/S).
	 * </p>
	 *
	 * @param exit Exit position (block coordinates used).
	 * @param gate A block position on the gate used as the reference point.
	 * @return The {@link BlockFace} from {@code gate} toward {@code exit}.
	 */
	public BlockFace getExitFace(PS exit, PS gate)
	{
		exit = exit.getBlockCoords(true);
		gate = gate.getBlockCoords(true);
		
		if (this == HORIZONTAL)
		{
			int dx = exit.getBlockX() - gate.getBlockX();
			int dz = exit.getBlockZ() - gate.getBlockZ();
			if (Math.abs(dx) >= Math.abs(dz))
			{
				return dx > 0 ? BlockFace.EAST : BlockFace.WEST;
			}
			return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
		}
		
		int mod;
		if (this == NS)
		{
			mod = exit.getBlockX() - gate.getBlockX();
			if (mod > 0)
			{
				return BlockFace.WEST;
			}
			else
			{
				return BlockFace.EAST;
			}
		}
		else
		{
			mod = exit.getBlockZ() - gate.getBlockZ();
			if (mod > 0)
			{
				return BlockFace.NORTH;
			}
			else
			{
				return BlockFace.SOUTH;
			}
		}
	}
	
	/**
	 * Yaw (degrees) the player should face when placed at the exit marker.
	 *
	 * @param exit Exit position.
	 * @param gate Reference gate block position.
	 * @return Yaw derived from {@link #getExitFace(PS, PS)}.
	 */
	public float getExitYaw(PS exit, PS gate)
	{
		return MUtil.getYaw(this.getExitFace(exit, gate));
	}
	
}
