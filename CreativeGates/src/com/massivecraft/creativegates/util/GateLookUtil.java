package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.entity.UGate;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Resolves the gate a player is looking at, including through air/particle fill
 * (indexed interior coords) without requiring a solid block hit.
 */
public final class GateLookUtil
{
	/** Default max look distance in blocks. */
	public static final int DEFAULT_MAX_DISTANCE = 8;
	
	private GateLookUtil()
	{
	}
	
	/**
	 * Finds the first gate along the player's look ray within {@link #DEFAULT_MAX_DISTANCE}.
	 *
	 * @param player Looking player.
	 * @return Gate or null.
	 */
	public static UGate getLookedAtGate(Player player)
	{
		return getLookedAtGate(player, DEFAULT_MAX_DISTANCE);
	}
	
	/**
	 * Finds the first gate along the player's look ray.
	 * <p>
	 * Steps block-by-block so air/particle fills (still indexed) are detected.
	 * Also checks the solid ray-trace hit if present.
	 * </p>
	 *
	 * @param player Looking player.
	 * @param maxDistance Max distance in blocks.
	 * @return Gate or null.
	 */
	public static UGate getLookedAtGate(Player player, int maxDistance)
	{
		if (player == null || maxDistance < 1) return null;
		
		Location eye = player.getEyeLocation();
		Vector direction = eye.getDirection().normalize();
		
		// Prefer solid hit first (frame / solid fill), then walk for air interiors.
		RayTraceResult hit = player.getWorld().rayTraceBlocks(eye, direction, maxDistance, FluidCollisionMode.NEVER, true);
		if (hit != null && hit.getHitBlock() != null)
		{
			UGate gate = UGate.get(hit.getHitBlock());
			if (gate != null) return gate;
		}
		
		Block previous = null;
		for (double d = 0.0; d <= maxDistance; d += 0.25)
		{
			Location point = eye.clone().add(direction.clone().multiply(d));
			Block block = point.getBlock();
			if (block.equals(previous)) continue;
			previous = block;
			
			UGate gate = UGate.get(block);
			if (gate != null) return gate;
		}
		
		return null;
	}
	
	/**
	 * Resolves a gate from a clicked block, or from the look ray if the click missed a gate.
	 *
	 * @param player Interacting player.
	 * @param clickedBlock Clicked block, or null.
	 * @return Gate or null.
	 */
	public static UGate getGateFromClickOrLook(Player player, Block clickedBlock)
	{
		if (clickedBlock != null)
		{
			UGate gate = UGate.get(clickedBlock);
			if (gate != null) return gate;
		}
		return getLookedAtGate(player);
	}
	
}
