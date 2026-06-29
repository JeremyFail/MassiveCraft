package com.massivecraft.creativegates;

import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes ballistic launch plans for horizontal gates when velocity preservation is enabled.
 * <p>
 * Launches are restricted to the top or bottom face of the portal (never the sides). The
 * elevation angle is chosen by combining:
 * </p>
 * <ul>
 *   <li>A closed-form ballistic estimate (gravity only, no drag) toward the exit marker.</li>
 *   <li>Golden-section search on that angle using the same per-tick gravity and drag model as gameplay.</li>
 *   <li>Portal clearance penalties so the arc does not skim back through the fluid.</li>
 * </ul>
 * <p>
 * Minecraft drag has no simple exact formula; simulation-based refinement is the standard approach.
 * If no plan is viable, {@link UGate#transport} falls back to a normal exit-marker teleport.
 * </p>
 */
public class HorizontalGateLaunchUtil
{
	/** Ticks used to estimate minimum speed needed to clear the portal volume. */
	private static final double CLEARANCE_TICKS = 6.0;
	
	/** Minimum squared horizontal length before normalizing exit direction from XZ delta. */
	private static final double MIN_HORIZONTAL_LENGTH_SQ = 0.01;
	
	/** Extra blocks added beyond the portal extent when computing clearance distance. */
	private static final double PLAYER_CLEARANCE = 1.0;
	
	/**
	 * Vertical offset below/above the portal layer so a ~1.8-block-tall player fully clears the fluid.
	 * Flat horizontal portals have zero dot-product extent; this replaces the old 0.75-block nudge.
	 */
	private static final double LAUNCH_ORIGIN_BELOW_PORTAL = 2.0;
	private static final double LAUNCH_ORIGIN_ABOVE_PORTAL = 2.0;
	
	/** Downward acceleration per tick (Minecraft-like gravity). */
	private static final double GRAVITY_PER_TICK = 0.08;
	
	/** Horizontal velocity multiplier per tick. */
	private static final double DRAG_XZ = 0.91;
	
	/** Vertical velocity multiplier per tick. */
	private static final double DRAG_Y = 0.98;
	
	/** Maximum ticks simulated when scoring landing error. */
	private static final int MAX_SIM_TICKS = 160;
	
	/** Ticks simulated to verify the arc leaves the portal interior quickly (fast entries). */
	private static final int PORTAL_CLEARANCE_SIM_TICKS_FAST = 14;
	
	/** Longer window for slow entries so shallow arcs are not accepted too early. */
	private static final int PORTAL_CLEARANCE_SIM_TICKS_SLOW = 32;
	
	/** Full trajectory check for portal re-entry after leaving. */
	private static final int PORTAL_OCCUPANCY_SIM_TICKS = 48;
	
	/** Maximum distance (blocks) from exit for a launch angle to be accepted. */
	private static final double MAX_ACCEPTABLE_LANDING_ERROR = 2.5;
	
	/** Extra landing tolerance when entry speed is near the launch minimum. */
	private static final double SLOW_SPEED_LANDING_ERROR_BONUS = 4.0;
	
	/** Entry speeds at or above this use the fast clearance window and shallowest allowed angles. */
	private static final double REFERENCE_FAST_ENTRY_SPEED = 1.15;
	
	/** Entry speeds at or below this use the slow clearance window and steepest required angles. */
	private static final double REFERENCE_SLOW_ENTRY_SPEED = 0.55;
	
	/** Y offsets from feet used to approximate player height during clearance simulation. */
	private static final double[] CLEARANCE_BODY_Y_OFFSETS = { 0.0, 0.9, 1.62 };
	
	/** Golden-section search iterations when refining the launch angle (radians). */
	private static final int ANGLE_SEARCH_ITERATIONS = 28;
	
	/** Minimum horizontal distance (blocks) to the exit for the vacuum ballistic formula. */
	private static final double MIN_VACUUM_HORIZONTAL_DIST = 0.25;
	
	/**
	 * When the portal center is at least this many blocks above the exit, prefer launching
	 * downward first (ceiling-style gates with exits below).
	 */
	private static final double EXIT_BELOW_PORTAL_Y_THRESHOLD = 2.0;
	
	/** Base launch angle limits (degrees from horizontal); tightened further when entry speed is low. */
	private static final int MIN_UP_LAUNCH_ANGLE = 48;
	private static final int MAX_UP_LAUNCH_ANGLE = 85;
	private static final int MIN_DOWN_LAUNCH_ANGLE = -85;
	private static final int MAX_DOWN_LAUNCH_ANGLE = -40;
	
	/** At slow speed, shallowest allowed UP angle and shallowest allowed DOWN angle shift by this many degrees. */
	private static final int SLOW_SPEED_EXTRA_STEEPNESS_DEG = 22;
	
	private HorizontalGateLaunchUtil() { }
	
	/**
	 * A resolved launch: where to place the player and which velocity to apply on the next tick.
	 */
	public static final class LaunchPlan
	{
		private final PS launchPs;
		private final Vector velocity;
		
		/**
		 * @param launchPs Position and yaw for the player at launch (pitch is level).
		 * @param velocity Velocity vector in blocks per tick.
		 */
		public LaunchPlan(PS launchPs, Vector velocity)
		{
			this.launchPs = launchPs;
			this.velocity = velocity;
		}
		
		/**
		 * @return Launch position and facing.
		 */
		public PS getLaunchPs()
		{
			return this.launchPs;
		}
		
		/**
		 * @return Launch velocity in blocks per tick.
		 */
		public Vector getVelocity()
		{
			return this.velocity;
		}
	}
	
	/**
	 * Attempts to build a launch plan for the destination horizontal gate.
	 * <p>
	 * Returns {@code null} when the gate is not horizontal, content or exit is missing,
	 * both top and bottom faces are blocked, no ballistic angle lands within tolerance,
	 * or entry speed is below the minimum needed to clear the portal.
	 * </p>
	 *
	 * @param destGate Destination horizontal gate: ejection geometry and exit marker both come from this gate.
	 * @param entryVelocity Player velocity at source entry (magnitude used for launch speed).
	 * @return A launch plan, or {@code null} if launch should not be used.
	 */
	public static LaunchPlan tryPlan(UGate destGate, Vector entryVelocity)
	{
		if (destGate == null || entryVelocity == null) return null;
		if (!destGate.getOrientation().isHorizontal()) return null;
		
		List<Block> contentBlocks = destGate.getContentBlocks();
		if (contentBlocks == null || contentBlocks.isEmpty()) return null;
		
		PS exitPs = destGate.getExit();
		if (exitPs == null) return null;
		
		Location portalCenter = getPortalCenter(contentBlocks);
		if (portalCenter == null) return null;
		
		Location exitLoc;
		try
		{
			exitLoc = exitPs.asBukkitLocation(true);
		}
		catch (IllegalStateException e)
		{
			return null;
		}
		
		BlockFace exitFace = resolveExitFace(portalCenter, exitLoc, contentBlocks);
		if (exitFace == null) return null;
		
		Vector horizontalDir = getHorizontalDirectionForFace(exitFace, portalCenter, exitLoc, exitPs);
		if (horizontalDir == null) return null;
		
		Location launchOrigin = getLaunchOrigin(portalCenter, contentBlocks, exitFace);
		double entrySpeed = entryVelocity.length();
		Vector launchDirection = computeBallisticLaunchDirection(launchOrigin, exitLoc, horizontalDir, entrySpeed, exitFace, contentBlocks);
		if (launchDirection == null) return null;
		
		double clearDistance = getClearDistance(contentBlocks, launchOrigin, launchDirection);
		double minLaunchSpeed = clearDistance / CLEARANCE_TICKS;
		if (entrySpeed < minLaunchSpeed) return null;
		
		Location launchLoc = launchOrigin.clone();
		launchLoc.setYaw(getHorizontalYaw(horizontalDir));
		launchLoc.setPitch(0f);
		
		return new LaunchPlan(PS.valueOf(launchLoc), launchDirection.clone().multiply(entrySpeed));
	}
	
	/**
	 * Picks the top or bottom portal face to launch through, preferring the alternate face if the
	 * first choice is blocked by solid blocks.
	 *
	 * @param portalCenter Center of portal content blocks.
	 * @param exitLoc Exit marker location.
	 * @param contentBlocks Portal interior blocks.
	 * @return {@link BlockFace#UP} or {@link BlockFace#DOWN}, or {@code null} if both are blocked.
	 */
	private static BlockFace resolveExitFace(Location portalCenter, Location exitLoc, List<Block> contentBlocks)
	{
		List<BlockFace> ranked = rankExitFaces(portalCenter, exitLoc);
		
		for (BlockFace face : ranked)
		{
			if (!GateTeleportSafety.isFaceBlocked(contentBlocks, face)) return face;
		}
		
		return null;
	}
	
	/**
	 * Orders {@link BlockFace#UP} vs {@link BlockFace#DOWN} by where the exit marker sits vertically.
	 * <p>
	 * Ceiling-style gates (portal well above exit) try DOWN first; floor-style gates try UP first.
	 * </p>
	 *
	 * @param portalCenter Center of portal content.
	 * @param exitLoc Exit marker location.
	 * @return A list of length two: preferred face first, alternate second.
	 */
	private static List<BlockFace> rankExitFaces(Location portalCenter, Location exitLoc)
	{
		List<BlockFace> ranked = new ArrayList<>(2);
		if (portalCenter.getY() >= exitLoc.getY() + EXIT_BELOW_PORTAL_Y_THRESHOLD)
		{
			ranked.add(BlockFace.DOWN);
			ranked.add(BlockFace.UP);
		}
		else
		{
			ranked.add(BlockFace.UP);
			ranked.add(BlockFace.DOWN);
		}
		return ranked;
	}
	
	/**
	 * Horizontal unit vector toward the exit (XZ only). {@code face} is not used for direction;
	 * launch always uses horizontal aim toward the exit marker.
	 *
	 * @param face Resolved launch face (unused for direction).
	 * @param portalCenter Portal center.
	 * @param exitLoc Exit location.
	 * @param exitPs Exit PS (yaw fallback when exit is vertically aligned with portal).
	 * @return Normalized XZ direction, or {@code null} if direction cannot be resolved.
	 */
	private static Vector getHorizontalDirectionForFace(BlockFace face, Location portalCenter, Location exitLoc, PS exitPs)
	{
		return getHorizontalDirection(portalCenter, exitLoc, exitPs);
	}
	
	/**
	 * Places the launch origin just outside the portal on the chosen face.
	 *
	 * @param portalCenter Center of portal content.
	 * @param contentBlocks Portal interior blocks.
	 * @param face {@link BlockFace#UP} or {@link BlockFace#DOWN}.
	 * @return World location offset along {@code face} past the portal extent.
	 */
	private static Location getLaunchOrigin(Location portalCenter, List<Block> contentBlocks, BlockFace face)
	{
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (Block block : contentBlocks)
		{
			minY = Math.min(minY, block.getY());
			maxY = Math.max(maxY, block.getY());
		}
		
		Location origin = portalCenter.clone();
		
		if (face == BlockFace.DOWN)
		{
			// Single-layer portals have zero extent along DOWN; place feet well below the fluid layer.
			origin.setY(minY - LAUNCH_ORIGIN_BELOW_PORTAL);
		}
		else if (face == BlockFace.UP)
		{
			origin.setY(maxY + LAUNCH_ORIGIN_ABOVE_PORTAL);
		}
		else
		{
			Vector normal = face.getDirection();
			double extent = getExtentAlongDirection(contentBlocks, portalCenter, normal);
			origin.add(normal.clone().multiply(extent + 0.75));
		}
		
		return origin;
	}
	
	/**
	 * Maximum distance from {@code center} to any content block center along {@code direction}.
	 *
	 * @param contentBlocks Portal interior blocks.
	 * @param center Reference point (portal center).
	 * @param direction Unit or non-unit vector along the launch face normal.
	 * @return Maximum positive dot product along {@code direction}.
	 */
	private static double getExtentAlongDirection(List<Block> contentBlocks, Location center, Vector direction)
	{
		Vector centerVec = center.toVector();
		double maxAlong = 0;
		
		for (Block block : contentBlocks)
		{
			Vector blockCenter = new Vector(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
			double along = blockCenter.subtract(centerVec).dot(direction);
			if (along > maxAlong) maxAlong = along;
		}
		
		return maxAlong;
	}
	
	/**
	 * Solves for launch elevation angle toward {@code exitLoc} at fixed {@code speed}, then returns
	 * the unit direction (caller multiplies by speed for velocity).
	 * <p>
	 * Uses {@link #estimateVacuumLaunchAngle} for an initial guess (physics without drag), then
	 * minimizes a combined landing-and-clearance score with golden-section search under the game's
	 * per-tick gravity and drag model.
	 * </p>
	 *
	 * @param launchOrigin World position where the player is placed before velocity is applied.
	 * @param exitLoc Exit marker target.
	 * @param horizontalDir Unit vector in XZ toward the exit.
	 * @param speed Entry speed magnitude (blocks per tick).
	 * @param exitFace {@link BlockFace#UP} or {@link BlockFace#DOWN} launch face.
	 * @param contentBlocks Portal interior blocks for clearance simulation.
	 * @return Unit launch direction, or {@code null} if none qualify.
	 */
	private static Vector computeBallisticLaunchDirection(Location launchOrigin, Location exitLoc, Vector horizontalDir, double speed, BlockFace exitFace, List<Block> contentBlocks)
	{
		if (speed < 0.001) return null;
		
		double slowWeight = getSlowSpeedWeight(speed);
		int[] angleBounds = resolveAngleBounds(speed, exitFace);
		double minRad = Math.toRadians(angleBounds[0]);
		double maxRad = Math.toRadians(angleBounds[1]);
		double maxLandingError = MAX_ACCEPTABLE_LANDING_ERROR + slowWeight * SLOW_SPEED_LANDING_ERROR_BONUS;
		
		double horizontalDist = getHorizontalDistanceToExit(launchOrigin, exitLoc, horizontalDir);
		double deltaY = exitLoc.getY() - launchOrigin.getY();
		
		double bestAngle = minRad;
		double bestScore = Double.MAX_VALUE;
		double bestLandingError = Double.MAX_VALUE;
		
		// Seed from vacuum ballistics (low and high arc when both exist).
		double[] seed = considerAngleCandidate(launchOrigin, exitLoc, horizontalDir, speed, exitFace, contentBlocks, slowWeight,
			estimateVacuumLaunchAngle(horizontalDist, deltaY, speed, false), minRad, maxRad,
			bestAngle, bestScore, bestLandingError);
		bestAngle = seed[0];
		bestScore = seed[1];
		bestLandingError = seed[2];
		
		seed = considerAngleCandidate(launchOrigin, exitLoc, horizontalDir, speed, exitFace, contentBlocks, slowWeight,
			estimateVacuumLaunchAngle(horizontalDist, deltaY, speed, true), minRad, maxRad,
			bestAngle, bestScore, bestLandingError);
		bestAngle = seed[0];
		bestScore = seed[1];
		bestLandingError = seed[2];
		
		// Refine with golden-section search on the combined score (landing + clearance under drag).
		double[] refined = goldenSectionMinimizeAngle(launchOrigin, exitLoc, horizontalDir, speed, exitFace, contentBlocks, slowWeight, minRad, maxRad, bestAngle, bestScore, bestLandingError);
		bestAngle = refined[0];
		bestScore = refined[1];
		bestLandingError = refined[2];
		
		if (bestScore >= Double.MAX_VALUE / 2 || Double.isNaN(bestAngle)) return null;
		if (bestLandingError > maxLandingError) return null;
		
		double cos = Math.cos(bestAngle);
		double sin = Math.sin(bestAngle);
		return new Vector(horizontalDir.getX() * cos, sin, horizontalDir.getZ() * cos);
	}
	
	/**
	 * Horizontal distance from launch to exit along {@code horizontalDir} (signed; positive = exit ahead).
	 */
	private static double getHorizontalDistanceToExit(Location launchOrigin, Location exitLoc, Vector horizontalDir)
	{
		double dx = exitLoc.getX() - launchOrigin.getX();
		double dz = exitLoc.getZ() - launchOrigin.getZ();
		return dx * horizontalDir.getX() + dz * horizontalDir.getZ();
	}
	
	/**
	 * Ideal launch angle (radians from horizontal, positive = up) to reach a target in a vacuum with
	 * constant gravity {@link #GRAVITY_PER_TICK} per tick and no drag.
	 * <p>
	 * Uses the standard ballistic identity
	 * {@code tan(θ) = (v² ± sqrt(v⁴ - g(gx² + 2yv²))) / (gx)} with {@code x} = horizontal distance
	 * and {@code y} = vertical offset.
	 * </p>
	 *
	 * @param horizontalDist Signed distance to target along the launch heading.
	 * @param deltaY Target Y minus launch Y.
	 * @param speed Initial speed (blocks per tick).
	 * @param highArc If both roots exist, use the higher arc ({@code +} root).
	 * @return Angle in radians, or {@link Double#NaN} if the target is out of range.
	 */
	private static double estimateVacuumLaunchAngle(double horizontalDist, double deltaY, double speed, boolean highArc)
	{
		double x = Math.abs(horizontalDist);
		if (x < MIN_VACUUM_HORIZONTAL_DIST) return Double.NaN;
		
		double y = deltaY;
		double v = speed;
		double g = GRAVITY_PER_TICK;
		double v2 = v * v;
		double radicand = v2 * v2 - g * (g * x * x + 2 * y * v2);
		if (radicand < 0) return Double.NaN;
		
		double sqrt = Math.sqrt(radicand);
		double tanTheta = highArc ? (v2 + sqrt) / (g * x) : (v2 - sqrt) / (g * x);
		return Math.atan(tanTheta);
	}
	
	/**
	 * Updates best angle/score if {@code angleRad} is in range and beats the current best.
	 * Returns {@code double[3]}: angle, score, landingError (for chaining into search).
	 */
	private static double[] considerAngleCandidate(Location launchOrigin, Location exitLoc, Vector horizontalDir, double speed, BlockFace exitFace, List<Block> contentBlocks, double slowWeight, double angleRad, double minRad, double maxRad, double bestAngle, double bestScore, double bestLandingError)
	{
		if (Double.isNaN(angleRad)) return new double[] { bestAngle, bestScore, bestLandingError };
		
		angleRad = clamp(angleRad, minRad, maxRad);
		double landingError = simulateLandingError(launchOrigin, exitLoc, horizontalDir, speed, angleRad);
		double score = scoreLaunchAngle(launchOrigin, exitLoc, horizontalDir, speed, angleRad, exitFace, contentBlocks, slowWeight, landingError);
		
		if (score < bestScore)
		{
			return new double[] { angleRad, score, landingError };
		}
		return new double[] { bestAngle, bestScore, bestLandingError };
	}
	
	/**
	 * Minimizes {@link #scoreLaunchAngle} over elevation angle using golden-section search.
	 *
	 * @return {@code double[3]} — best angle (rad), score, landing error.
	 */
	private static double[] goldenSectionMinimizeAngle(Location launchOrigin, Location exitLoc, Vector horizontalDir, double speed, BlockFace exitFace, List<Block> contentBlocks, double slowWeight, double minRad, double maxRad, double bestAngle, double bestScore, double bestLandingError)
	{
		double[] fromMax = considerAngleCandidate(launchOrigin, exitLoc, horizontalDir, speed, exitFace, contentBlocks, slowWeight, maxRad, minRad, maxRad, bestAngle, bestScore, bestLandingError);
		bestAngle = fromMax[0];
		bestScore = fromMax[1];
		bestLandingError = fromMax[2];
		
		final double phi = (Math.sqrt(5) - 1) / 2;
		double lo = minRad;
		double hi = maxRad;
		double c = hi - phi * (hi - lo);
		double d = lo + phi * (hi - lo);
		
		double scoreC = evaluateLaunchScore(launchOrigin, exitLoc, horizontalDir, speed, exitFace, contentBlocks, slowWeight, c);
		double scoreD = evaluateLaunchScore(launchOrigin, exitLoc, horizontalDir, speed, exitFace, contentBlocks, slowWeight, d);
		
		for (int i = 0; i < ANGLE_SEARCH_ITERATIONS; i++)
		{
			if (scoreC < bestScore)
			{
				bestScore = scoreC;
				bestAngle = c;
				bestLandingError = simulateLandingError(launchOrigin, exitLoc, horizontalDir, speed, c);
			}
			if (scoreD < bestScore)
			{
				bestScore = scoreD;
				bestAngle = d;
				bestLandingError = simulateLandingError(launchOrigin, exitLoc, horizontalDir, speed, d);
			}
			
			if (scoreC < scoreD)
			{
				hi = d;
				d = c;
				scoreD = scoreC;
				c = hi - phi * (hi - lo);
				scoreC = evaluateLaunchScore(launchOrigin, exitLoc, horizontalDir, speed, exitFace, contentBlocks, slowWeight, c);
			}
			else
			{
				lo = c;
				c = d;
				scoreC = scoreD;
				d = lo + phi * (hi - lo);
				scoreD = evaluateLaunchScore(launchOrigin, exitLoc, horizontalDir, speed, exitFace, contentBlocks, slowWeight, d);
			}
		}
		
		return new double[] { bestAngle, bestScore, bestLandingError };
	}
	
	private static double evaluateLaunchScore(Location launchOrigin, Location exitLoc, Vector horizontalDir, double speed, BlockFace exitFace, List<Block> contentBlocks, double slowWeight, double angleRad)
	{
		double landingError = simulateLandingError(launchOrigin, exitLoc, horizontalDir, speed, angleRad);
		return scoreLaunchAngle(launchOrigin, exitLoc, horizontalDir, speed, angleRad, exitFace, contentBlocks, slowWeight, landingError);
	}
	
	/**
	 * Returns how much to bias toward steep angles and strict clearance (0 = fast entry, 1 = slow).
	 */
	private static double getSlowSpeedWeight(double speed)
	{
		double span = REFERENCE_FAST_ENTRY_SPEED - REFERENCE_SLOW_ENTRY_SPEED;
		if (span <= 0) return 0;
		return clamp((REFERENCE_FAST_ENTRY_SPEED - speed) / span, 0, 1);
	}
	
	private static double clamp(double value, double min, double max)
	{
		return Math.max(min, Math.min(max, value));
	}
	
	/**
	 * Speed-adaptive launch angle limits. Slower entries require steeper arcs (more vertical).
	 *
	 * @return {@code int[2]} — {@code [0]} minimum angle, {@code [1]} maximum angle (degrees from horizontal).
	 */
	private static int[] resolveAngleBounds(double speed, BlockFace exitFace)
	{
		double slowWeight = getSlowSpeedWeight(speed);
		int extra = (int) Math.round(slowWeight * SLOW_SPEED_EXTRA_STEEPNESS_DEG);
		
		if (exitFace == BlockFace.DOWN)
		{
			int minAngle = MIN_DOWN_LAUNCH_ANGLE - (extra / 2);
			int maxAngle = MAX_DOWN_LAUNCH_ANGLE - extra;
			return new int[] { Math.min(minAngle, maxAngle), maxAngle };
		}
		
		int minAngle = MIN_UP_LAUNCH_ANGLE + extra;
		int maxAngle = MAX_UP_LAUNCH_ANGLE;
		return new int[] { Math.min(minAngle, maxAngle), maxAngle };
	}
	
	/**
	 * Combined score for picking a launch angle. Heavily penalizes time inside portal fluid,
	 * especially at low entry speed, and lightly prefers steeper angles when slow.
	 */
	private static double scoreLaunchAngle(Location start, Location exitLoc, Vector horizontalDir, double speed, double angleRadians, BlockFace exitFace, List<Block> contentBlocks, double slowWeight, double landingError)
	{
		int clearanceTicks = (int) Math.round(lerp(PORTAL_CLEARANCE_SIM_TICKS_FAST, PORTAL_CLEARANCE_SIM_TICKS_SLOW, slowWeight));
		PortalOccupancy occupancy = simulatePortalOccupancy(start, horizontalDir, speed, angleRadians, contentBlocks, clearanceTicks, PORTAL_OCCUPANCY_SIM_TICKS);
		
		if (occupancy.ticksInsidePortal > 0 && occupancy.ticksUntilFirstClear < 0)
		{
			return Double.MAX_VALUE;
		}
		
		double score = landingError;
		
		// Any time inside the portal is bad; worse when entry speed is low.
		double insidePenalty = 12.0 + 20.0 * slowWeight;
		score += occupancy.ticksInsidePortal * insidePenalty;
		
		// Slow to leave the portal on the initial arc.
		if (occupancy.ticksUntilFirstClear >= 0)
		{
			score += occupancy.ticksUntilFirstClear * (2.0 + 5.0 * slowWeight);
		}
		
		// Re-entering portal fluid after clearing is very bad (common with shallow slow arcs).
		score += occupancy.ticksInsideAfterFirstClear * (25.0 + 35.0 * slowWeight);
		
		// At slow speed, prefer steeper launch angles (more vertical component).
		double angleDeg = Math.toDegrees(angleRadians);
		if (exitFace == BlockFace.UP)
		{
			score -= slowWeight * 0.12 * angleDeg;
		}
		else
		{
			score += slowWeight * 0.12 * angleDeg;
		}
		
		return score;
	}
	
	private static double lerp(double a, double b, double t)
	{
		return a + (b - a) * t;
	}
	
	private static final class PortalOccupancy
	{
		int ticksInsidePortal;
		int ticksUntilFirstClear;
		int ticksInsideAfterFirstClear;
	}
	
	/**
	 * Simulates movement and tracks how long a player-sized sample spends inside portal blocks.
	 */
	private static PortalOccupancy simulatePortalOccupancy(Location start, Vector horizontalDir, double speed, double angleRadians, List<Block> contentBlocks, int clearanceWindowTicks, int totalTicks)
	{
		PortalOccupancy result = new PortalOccupancy();
		result.ticksUntilFirstClear = -1;
		
		double x = start.getX();
		double y = start.getY();
		double z = start.getZ();
		
		double cos = Math.cos(angleRadians);
		double sin = Math.sin(angleRadians);
		double vx = horizontalDir.getX() * cos * speed;
		double vy = sin * speed;
		double vz = horizontalDir.getZ() * cos * speed;
		
		boolean startedInside = intersectsPortalVolume(x, y, z, contentBlocks);
		boolean hasCleared = !startedInside;
		
		for (int tick = 0; tick < totalTicks; tick++)
		{
			boolean inside = intersectsPortalVolume(x, y, z, contentBlocks);
			
			if (inside)
			{
				result.ticksInsidePortal++;
				if (hasCleared)
				{
					result.ticksInsideAfterFirstClear++;
				}
			}
			else if (!hasCleared)
			{
				hasCleared = true;
				result.ticksUntilFirstClear = tick;
			}
			
			// Must be outside for the full clearance window before we stop caring about re-entry.
			if (tick == clearanceWindowTicks - 1 && !hasCleared)
			{
				return result;
			}
			
			x += vx;
			y += vy;
			z += vz;
			vy -= GRAVITY_PER_TICK;
			vx *= DRAG_XZ;
			vy *= DRAG_Y;
			vz *= DRAG_XZ;
		}
		
		return result;
	}
	
	/**
	 * Returns whether any part of a standing player at {@code (x, y, z)} overlaps portal content.
	 */
	private static boolean intersectsPortalVolume(double x, double y, double z, List<Block> contentBlocks)
	{
		for (double yOffset : CLEARANCE_BODY_Y_OFFSETS)
		{
			if (isInsidePortalVolume(x, y + yOffset, z, contentBlocks)) return true;
		}
		return false;
	}
	
	/**
	 * Returns whether the given world position is inside a portal content block (block coords).
	 */
	private static boolean isInsidePortalVolume(double x, double y, double z, List<Block> contentBlocks)
	{
		int bx = (int) Math.floor(x);
		int by = (int) Math.floor(y);
		int bz = (int) Math.floor(z);
		
		for (Block block : contentBlocks)
		{
			if (block.getX() == bx && block.getY() == by && block.getZ() == bz) return true;
		}
		return false;
	}
	
	/**
	 * Simulates flight and estimates how close the arc gets to the exit marker.
	 * <p>
	 * Uses a simple per-tick gravity and drag model. When the trajectory crosses the target
	 * height while falling, returns an error weighted toward horizontal miss. Otherwise returns
	 * the best error seen after the first few ticks.
	 * </p>
	 *
	 * @param start Launch origin.
	 * @param exitLoc Exit marker.
	 * @param horizontalDir Unit XZ direction.
	 * @param speed Speed magnitude.
	 * @param angleRadians Launch angle from horizontal.
	 * @return Landing error in blocks (lower is better).
	 */
	private static double simulateLandingError(Location start, Location exitLoc, Vector horizontalDir, double speed, double angleRadians)
	{
		double x = start.getX();
		double y = start.getY();
		double z = start.getZ();
		
		double cos = Math.cos(angleRadians);
		double sin = Math.sin(angleRadians);
		double vx = horizontalDir.getX() * cos * speed;
		double vy = sin * speed;
		double vz = horizontalDir.getZ() * cos * speed;
		
		double targetX = exitLoc.getX();
		double targetY = exitLoc.getY();
		double targetZ = exitLoc.getZ();
		
		double bestError = Double.MAX_VALUE;
		boolean wasAboveTarget = y >= targetY;
		
		for (int tick = 0; tick < MAX_SIM_TICKS; tick++)
		{
			x += vx;
			y += vy;
			z += vz;
			
			double horizDist = Math.hypot(x - targetX, z - targetZ);
			double vertDist = Math.abs(y - targetY);
			double error = Math.sqrt(horizDist * horizDist + vertDist * vertDist);
			if (tick >= 2 && error < bestError)
			{
				bestError = error;
			}
			
			// Prefer the error at the moment we cross the exit height while descending.
			if (wasAboveTarget && y <= targetY && vy <= 0)
			{
				return Math.max(horizDist, vertDist * 0.5);
			}
			wasAboveTarget = y >= targetY;
			
			vy -= GRAVITY_PER_TICK;
			vx *= DRAG_XZ;
			vy *= DRAG_Y;
			vz *= DRAG_XZ;
			
			if (y < start.getY() - 64) break;
		}
		
		return bestError;
	}
	
	/**
	 * Converts a horizontal direction vector to a Bukkit yaw (degrees).
	 *
	 * @param horizontalDir XZ direction (not required to be unit length).
	 * @return Yaw in degrees.
	 */
	private static float getHorizontalYaw(Vector horizontalDir)
	{
		return (float) Math.toDegrees(Math.atan2(-horizontalDir.getX(), horizontalDir.getZ()));
	}
	
	/**
	 * Unit vector in XZ from portal center toward the exit, or from exit yaw if vertically aligned.
	 *
	 * @param portalCenter Portal center.
	 * @param exitLoc Exit location.
	 * @param exitPs Exit PS; yaw used when XZ delta is negligible.
	 * @return Normalized XZ vector, or {@code null} if yaw is missing when needed.
	 */
	private static Vector getHorizontalDirection(Location portalCenter, Location exitLoc, PS exitPs)
	{
		Vector horizontal = new Vector(
			exitLoc.getX() - portalCenter.getX(),
			0,
			exitLoc.getZ() - portalCenter.getZ()
		);
		
		if (horizontal.lengthSquared() >= MIN_HORIZONTAL_LENGTH_SQ)
		{
			return horizontal.normalize();
		}
		
		Float yaw = exitPs.getYaw(true);
		if (yaw == null) return null;
		
		double radians = Math.toRadians(yaw);
		return new Vector(-Math.sin(radians), 0, Math.cos(radians));
	}
	
	/**
	 * Arithmetic mean of content block centers.
	 *
	 * @param contentBlocks Portal interior blocks; must be non-empty.
	 * @return Center location in the blocks' world, or {@code null} if the list is empty.
	 */
	private static Location getPortalCenter(List<Block> contentBlocks)
	{
		World world = contentBlocks.get(0).getWorld();
		double x = 0;
		double y = 0;
		double z = 0;
		
		for (Block block : contentBlocks)
		{
			x += block.getX() + 0.5;
			y += block.getY() + 0.5;
			z += block.getZ() + 0.5;
		}
		
		int count = contentBlocks.size();
		return new Location(world, x / count, y / count, z / count);
	}
	
	/**
	 * Distance along {@code direction} from {@code origin} through the portal extent, plus clearance.
	 * Used to reject launches when entry speed is too low to clear the portal in time.
	 *
	 * @param contentBlocks Portal interior blocks.
	 * @param origin Launch origin.
	 * @param direction Launch direction (typically includes vertical component).
	 * @return Minimum speed scale factor along the portal (blocks).
	 */
	private static double getClearDistance(List<Block> contentBlocks, Location origin, Vector direction)
	{
		if (direction.lengthSquared() < MIN_HORIZONTAL_LENGTH_SQ) return PLAYER_CLEARANCE;
		
		Vector unit = direction.clone().normalize();
		Vector originVec = origin.toVector();
		double maxAlongDirection = 0;
		
		for (Block block : contentBlocks)
		{
			Vector blockCenter = new Vector(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
			double along = blockCenter.subtract(originVec).dot(unit);
			if (along > maxAlongDirection)
			{
				maxAlongDirection = along;
			}
		}
		
		return Math.max(maxAlongDirection, 0) + PLAYER_CLEARANCE;
	}
	
}
