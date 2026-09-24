package com.massivecraft.creativegates.engine;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.cmd.CmdCg;
import com.massivecraft.creativegates.engine.create.GateCreate;
import com.massivecraft.creativegates.engine.create.PendingGateCreate;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.MPlayer;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.gate.GateOrientation;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.creativegates.ui.GateFillPicker;
import com.massivecraft.creativegates.ui.GateManageUi;
import com.massivecraft.creativegates.util.FloodUtil;
import com.massivecraft.creativegates.util.GateFloodInfo;
import com.massivecraft.creativegates.util.GateInspectUtil;
import com.massivecraft.creativegates.util.GateLookUtil;
import com.massivecraft.creativegates.util.MaterialCountUtil;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EngineMain extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final EngineMain i = new EngineMain();
	public static EngineMain get() { return i; }
	
	// -------------------------------------------- //
	// IS X NEARBY (UTIL)
	// -------------------------------------------- //
	
	/**
	 * Check if a block is near a gate.
	 * 
	 * @param block The block to check.
	 * @return True if the block is near a gate, false otherwise.
	 */
	public static boolean isGateNearby(Block block)
	{
		if (!MConf.get().isEnabled()) return false;
		
		final int radius = 3;
		for (int dx = -radius; dx <= radius; dx++)
		{
			for (int dy = -radius; dy <= radius; dy++)
			{
				for (int dz = -radius; dz <= radius; dz++)
				{
					if (CreativeGates.get().getIndex().get(PS.valueOf(block.getRelative(dx, dy, dz))) != null) return true;
				}
			}
		}
		return false;
	}
	
	// -------------------------------------------- //
	// STABILIZE GATE CONTENT (FLUIDS)
	// -------------------------------------------- //
	
	/**
	 * Cancel fluid physics inside gate interiors.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(BlockPhysicsEvent event)
	{
		Block block = event.getBlock();
		Material type = block.getType();
		
		if (type == Material.LAVA || type == Material.WATER)
		{
			UGate gate = UGate.get(block);
			if (gate != null)
			{
				event.setCancelled(true);
			}
		}
	}
	
	// FLUID FLOW (WATER / LAVA)
	
	/**
	 * Handle block from to events. This is used to prevent fluid from flowing in/out of gates.
	 * 
	 * @param event The block from to event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(BlockFromToEvent event)
	{
		UGate fromGate = UGate.get(event.getBlock());
		UGate toGate = UGate.get(event.getToBlock());
		if (fromGate == null && toGate == null) return;
		
		UGate gate = fromGate != null ? fromGate : toGate;
		Material type = event.getBlock().getType();
		if (CreativeGates.isFluidFillMaterial(type) || gate.getOrientation().isHorizontal())
		{
			event.setCancelled(true);
		}
	}
	
	/**
	 * Stabilize the portal content block.
	 * 
	 * @param block The block to stabilize.
	 * @param cancellable The cancellable to set the cancelled state of.
	 */
	public static void stabilizePortalContentBlock(Block block, Cancellable cancellable)
	{
		if (UGate.get(block) == null) return;
		cancellable.setCancelled(true);
	}
	
	/**
	 * Handle block place events. This is used to prevent the portal from disappearing.
	 * 
	 * @param event The block place event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(BlockPlaceEvent event)
	{
		stabilizePortalContentBlock(event.getBlock(), event);
	}
	
	/**
	 * Handle player bucket fill events. This is used to prevent the portal from disappearing.
	 * 
	 * @param event The player bucket fill event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(PlayerBucketFillEvent event)
	{
		stabilizePortalContentBlock(event.getBlockClicked(), event);
	}
	
	/**
	 * Handle player bucket empty events. This is used to prevent the portal from disappearing.
	 * 
	 * @param event The player bucket empty event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(PlayerBucketEmptyEvent event)
	{
		stabilizePortalContentBlock(event.getBlockClicked(), event);
	}
	
	// -------------------------------------------- //
	// PREVENT GATE HARM (LAVA)
	// -------------------------------------------- //
	
	/**
	 * Cancel damage the gate fill asks to suppress (lava/fire for lava, drowning for water).
	 *
	 * @param event The entity damage event.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void preventGateDamage(EntityDamageEvent event)
	{
		if (!(event.getEntity() instanceof Player player)) return;
		
		DamageCause cause = event.getCause();
		if (cause == null) return;
		
		if (event instanceof EntityDamageByBlockEvent damageByBlock)
		{
			if (isProtectedGateDamageBlock(damageByBlock.getDamager(), cause))
			{
				clearGateDamageEffects(player, cause);
				event.setCancelled(true);
				return;
			}
		}
		
		if (isInIntactGatePreventingDamage(player, cause))
		{
			clearGateDamageEffects(player, cause);
			event.setCancelled(true);
		}
	}
	
	/**
	 * Handle entity combust events. This is used to prevent the player from taking damage from lava or fire in a gate.
	 * Primarily used for lava gates.
	 * 
	 * @param event The entity combust event.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void preventGateLavaCombust(EntityCombustEvent event)
	{
		if (!(event.getEntity() instanceof Player player)) return;
		
		if (event instanceof EntityCombustByBlockEvent combustByBlock)
		{
			if (isProtectedGateFluidBlock(combustByBlock.getCombuster()))
			{
				event.setCancelled(true);
				return;
			}
		}
		
		if (isInIntactGateFluid(player))
		{
			event.setCancelled(true);
		}
	}
	
	/**
	 * Handle player move events. This is used to clear the player's fire ticks in a lava gate.
	 * 
	 * @param event The player move event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void clearHazardTicksInGate(PlayerMoveEvent event)
	{
		if (MUtil.isSameBlock(event)) return;
		Player player = event.getPlayer();
		if (isInIntactGatePreventingDamage(player, DamageCause.FIRE) || isInIntactGateFluid(player))
		{
			player.setFireTicks(0);
		}
	}
	
	/**
	 * Handle block ignite events. This is used to prevent the fire from spreading in a gate.
	 * 
	 * @param event The block ignite event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void preventGateLavaFireSpread(BlockIgniteEvent event)
	{
		if (event.getCause() != BlockIgniteEvent.IgniteCause.LAVA) return;
		
		Block ignitingBlock = event.getIgnitingBlock();
		if (isProtectedGateFluidBlock(ignitingBlock))
		{
			event.setCancelled(true);
		}
	}
	
	private static void clearGateDamageEffects(Player player, DamageCause cause)
	{
		if (cause == DamageCause.LAVA || cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK)
		{
			player.setFireTicks(0);
		}
	}
	
	/**
	 * Check if a block is protected fluid gate content.
	 * 
	 * @param block The block to check.
	 * @return True if the block is protected by a gate, false otherwise.
	 */
	public static boolean isProtectedGateFluidBlock(Block block)
	{
		if (block == null) return false;
		if (!CreativeGates.isFluidFillMaterial(block.getType())) return false;
		
		UGate gate = UGate.get(block);
		if (gate == null) return false;
		return gate.isIntact();
	}
	
	/**
	 * Whether an intact gate fill at this block wants to suppress {@code cause}.
	 */
	public static boolean isProtectedGateDamageBlock(Block block, DamageCause cause)
	{
		if (block == null || cause == null) return false;
		
		UGate gate = UGate.get(block);
		if (gate == null || !gate.isInteriorBlock(block) || !gate.isIntact()) return false;
		
		GateType type = gate.getFillType();
		return type != null && type.shouldPreventDamage(cause);
	}
	
	/**
	 * Check if a player is in an intact fluid gate.
	 * 
	 * @param player The player to check.
	 * @return True if the player is in an intact gate, false otherwise.
	 */
	public static boolean isInIntactGateFluid(Player player)
	{
		return isInIntactGateMatching(player, true, null);
	}
	
	/**
	 * Whether the player is touching an intact gate that prevents {@code cause}.
	 */
	public static boolean isInIntactGatePreventingDamage(Player player, DamageCause cause)
	{
		return isInIntactGateMatching(player, false, cause);
	}
	
	private static boolean isInIntactGateMatching(Player player, boolean fluidOnly, DamageCause cause)
	{
		Location loc = player.getLocation();
		BoundingBox box = player.getBoundingBox();
		
		int minX = (int) Math.floor(box.getMinX());
		int maxX = (int) Math.floor(box.getMaxX());
		int minY = (int) Math.floor(box.getMinY()) - 1; // include stand-on / display fills
		int maxY = (int) Math.floor(box.getMaxY());
		int minZ = (int) Math.floor(box.getMinZ());
		int maxZ = (int) Math.floor(box.getMaxZ());
		
		World world = loc.getWorld();
		if (world == null) return false;
		
		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; y <= maxY; y++)
			{
				for (int z = minZ; z <= maxZ; z++)
				{
					Block block = world.getBlockAt(x, y, z);
					if (fluidOnly)
					{
						if (isProtectedGateFluidBlock(block)) return true;
					}
					else if (cause != null)
					{
						if (isProtectedGateDamageBlock(block, cause)) return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Get the gate at a location (includes frame blocks).
	 * 
	 * @param location The location to get the gate for.
	 * @return The gate at the location, or null if there is no gate.
	 */
	public static UGate getGateAt(Location location)
	{
		UGate gate = UGate.get(location.getBlock());
		if (gate != null) return gate;
		return UGate.get(location.clone().subtract(0, 1, 0).getBlock());
	}
	
	/**
	 * Get the gate at a location only when standing in portal content (water/lava/portal blocks).
	 * Frame blocks do not count.
	 * 
	 * @param location The location to get the gate for.
	 * @return The gate at the location, or null if there is no gate content here.
	 */
	public static UGate getGateAtContent(Location location)
	{
		Block block = location.getBlock();
		if (isGateContentBlock(block)) return UGate.get(block);
		
		block = location.clone().subtract(0, 1, 0).getBlock();
		if (isGateContentBlock(block)) return UGate.get(block);
		
		return null;
	}
	
	/**
	 * Check if a block is part of a gate's interior content (fluids, real portal, display fills, or particle fills).
	 * 
	 * @param block The block to check.
	 * @return True if the block is part of a gate's content, false otherwise.
	 */
	public static boolean isGateContentBlock(Block block)
	{
		UGate gate = UGate.get(block);
		if (gate == null) return false;
		if (!gate.isInteriorBlock(block)) return false;
		
		if (gate.usesBlockDisplayFill()) return true;
		if (gate.usesParticleFill()) return true;
		
		Material type = block.getType();
		return CreativeGates.isGateFillMaterial(type);
	}
	
	/**
	 * Get the gate at the player's location.
	 * 
	 * @param player The player to get the gate for.
	 * @return The gate at the player's location, or null if there is no gate.
	 */
	public static UGate getGateAtPlayer(Player player)
	{
		return getGateIntersectingPlayer(player, player.getLocation());
	}
	
	/**
	 * Finds a gate whose portal content intersects the player's hitbox at a specific location.
	 * Used for move events so entry from below (head first) matches detection at the feet block.
	 */
	public static UGate getGateIntersectingPlayer(Player player, Location location)
	{
		return getGateIntersectingEntity(player, location);
	}

	/**
	 * Finds a gate whose portal content intersects the entity's hitbox at a specific location.
	 */
	public static UGate getGateIntersectingEntity(org.bukkit.entity.Entity entity, Location location)
	{
		if (entity == null || location == null || location.getWorld() == null) return null;
		
		double halfWidth = entity.getWidth() / 2.0;
		double height = entity.getHeight();
		
		int minX = (int) Math.floor(location.getX() - halfWidth);
		int maxX = (int) Math.floor(location.getX() + halfWidth);
		// Include one block below feet so solid ice platforms (stand-on) still count.
		int minY = (int) Math.floor(location.getY()) - 1;
		int maxY = (int) Math.floor(location.getY() + height);
		int minZ = (int) Math.floor(location.getZ() - halfWidth);
		int maxZ = (int) Math.floor(location.getZ() + halfWidth);
		
		World world = location.getWorld();
		UGate found = null;
		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; y <= maxY; y++)
			{
				for (int z = minZ; z <= maxZ; z++)
				{
					Block block = world.getBlockAt(x, y, z);
					if (!isGateContentBlock(block)) continue;
					UGate gate = UGate.get(block);
					if (found != null && found != gate) continue;
					found = gate;
				}
			}
		}
		return found;
	}
	
	private static final Map<UUID, Long> RECENT_GATE_USE_BY_PLAYER = new ConcurrentHashMap<>();
	private static final Map<UUID, Vector> PEAK_HORIZONTAL_GATE_VELOCITY = new ConcurrentHashMap<>();
	private static final Set<UUID> HORIZONTAL_LAUNCH_AIRBORNE = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> HORIZONTAL_GATE_SUPPRESS_UNTIL_EXIT = ConcurrentHashMap.newKeySet();
	/** After leaving a horizontal portal, block re-entry briefly (avoids climb-out bobbing re-triggering). */
	private static final Map<UUID, Long> HORIZONTAL_GATE_EXIT_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
	private static final long GATE_USE_DEBOUNCE_MILLIS = 250L;
	private static final long POST_GATE_PORTAL_SUPPRESS_MILLIS = 1000L;
	private static final long HORIZONTAL_GATE_EXIT_COOLDOWN_MILLIS = 2500L;
	
	/** Minimum current speed (blocks/tick) required to launch; below this uses exit teleport. */
	private static final double MIN_LAUNCH_ENTRY_SPEED = 0.55;
	
	/** When current speed is below this, a high peak velocity still counts as a fast entry. */
	private static final double GROUND_STOMP_CURRENT_SPEED_MAX = 0.2;
	
	/**
	 * Velocity sample and whether a momentum launch is allowed (vs exit-marker teleport).
	 */
	public static final class HorizontalEntryContext
	{
		public final Vector velocity;
		public final boolean launchEligible;
		
		public HorizontalEntryContext(Vector velocity, boolean launchEligible)
		{
			this.velocity = velocity;
			this.launchEligible = launchEligible;
		}
	}
	
	/**
	 * Check if a player has recently used a creative gate.
	 * 
	 * @param player The player to check.
	 * @return True if the player has recently used a creative gate, false otherwise.
	 */
	private static boolean wasRecentCreativeGateTransport(Player player)
	{
		Long recent = RECENT_GATE_USE_BY_PLAYER.get(player.getUniqueId());
		if (recent == null) return false;
		return (System.currentTimeMillis() - recent) < POST_GATE_PORTAL_SUPPRESS_MILLIS;
	}
	
	/**
	 * Try to use a gate and teleport the player.
	 * 
	 * @param player The player to teleport.
	 * @param ugate The gate to use.
	 * @return True if the player was teleported, false otherwise.
	 */
	public static boolean tryUseGate(Player player, UGate ugate)
	{
		if (ugate == null) return false;
		
		UUID playerId = player.getUniqueId();
		if (HORIZONTAL_GATE_SUPPRESS_UNTIL_EXIT.contains(playerId))
		{
			UGate inGate = getGateAtPlayer(player);
			if (inGate != null && inGate.getOrientation().isHorizontal())
			{
				return false;
			}
			HORIZONTAL_GATE_SUPPRESS_UNTIL_EXIT.remove(playerId);
		}
		
		long now = System.currentTimeMillis();
		Long recent = RECENT_GATE_USE_BY_PLAYER.get(playerId);
		if (recent != null && (now - recent) < GATE_USE_DEBOUNCE_MILLIS) return false;
		
		if (!ugate.isIntact())
		{
			ugate.destroy();
			return false;
		}
		
		if (!MConf.get().isEnabled()) return false;
		if (!Perm.USE.has(player, MConf.get().verboseUsePermission)) return false;
		
		if (!ugate.isEnterEnabled())
		{
			String message = Txt.parse("<i>This gate has enter disabled.");
			MixinMessage.get().messageOne(player, message);
			return false;
		}
		
		if (!ugate.isAllowPlayers())
		{
			String message = Txt.parse("<i>This gate does not allow player travel.");
			MixinMessage.get().messageOne(player, message);
			return false;
		}
		
		HorizontalEntryContext entryContext = null;
		if (ugate.getOrientation().isHorizontal() && MConf.get().isHorizontalGatesPreserveVelocity())
		{
			entryContext = resolveHorizontalEntryContext(player);
		}
		Location sourceLocation = player.getLocation().clone();
		boolean transported = ugate.transport(player, entryContext, sourceLocation);
		if (transported)
		{
			RECENT_GATE_USE_BY_PLAYER.put(playerId, System.currentTimeMillis());
			player.setPortalCooldown(300);
			if (ugate.isAllowMobs() || ugate.isAllowVehicles())
			{
				EngineGateMobs.markRecentGateUse(player);
			}
			if (ugate.getOrientation().isHorizontal())
			{
				HORIZONTAL_GATE_SUPPRESS_UNTIL_EXIT.add(playerId);
			}
		}
		return transported;
	}
	
	/**
	 * Marks a player as airborne from a horizontal gate launch. Fall distance is reset each tick
	 * until they land, similar to bouncing off a slime block.
	 *
	 * @param player The launched player.
	 */
	public static void markHorizontalLaunchAirborne(Player player)
	{
		HORIZONTAL_LAUNCH_AIRBORNE.add(player.getUniqueId());
		player.setFallDistance(0);
	}
	
	private static boolean isHorizontalLaunchAirborne(Player player)
	{
		return HORIZONTAL_LAUNCH_AIRBORNE.contains(player.getUniqueId());
	}
	
	private static HorizontalEntryContext resolveHorizontalEntryContext(Player player)
	{
		Vector current = player.getVelocity().clone();
		Vector peak = PEAK_HORIZONTAL_GATE_VELOCITY.remove(player.getUniqueId());
		double currentSpeed = current.length();
		double peakSpeed = peak != null ? peak.length() : 0;
		
		boolean highSpeedGroundStomp = currentSpeed <= GROUND_STOMP_CURRENT_SPEED_MAX && peakSpeed >= MIN_LAUNCH_ENTRY_SPEED;
		boolean launchEligible = currentSpeed >= MIN_LAUNCH_ENTRY_SPEED || highSpeedGroundStomp;
		
		Vector velocity;
		if (highSpeedGroundStomp)
		{
			velocity = peak.clone();
		}
		else if (peak != null && peak.lengthSquared() > current.lengthSquared())
		{
			velocity = peak.clone();
		}
		else
		{
			velocity = current;
		}
		
		return new HorizontalEntryContext(velocity, launchEligible);
	}
	
	private static Vector sampleMoveVelocity(PlayerMoveEvent event)
	{
		Player player = event.getPlayer();
		Vector velocity = player.getVelocity().clone();
		
		Location to = event.getTo();
		if (to != null)
		{
			Vector delta = to.toVector().subtract(event.getFrom().toVector());
			if (delta.lengthSquared() > velocity.lengthSquared())
			{
				velocity = delta.clone();
			}
		}
		return velocity;
	}
	
	/**
	 * Finds a gate at the player's position, destination, or anywhere along the movement segment.
	 * High-speed movement can skip over portal blocks within a single tick.
	 */
	private static UGate getGateOnMovePath(PlayerMoveEvent event)
	{
		Player player = event.getPlayer();
		Location to = event.getTo();
		Location from = event.getFrom();
		
		if (to != null)
		{
			UGate gate = getGateIntersectingPlayer(player, to);
			if (gate != null) return gate;
		}
		
		UGate gate = getGateIntersectingPlayer(player, from);
		if (gate != null) return gate;
		
		if (to == null) return null;
		
		Vector delta = to.toVector().subtract(from.toVector());
		double length = delta.length();
		if (length < 0.001) return null;
		
		Vector step = delta.clone().normalize().multiply(0.25);
		Location cursor = from.clone();
		int steps = (int) Math.ceil(length / 0.25);
		for (int i = 0; i <= steps; i++)
		{
			gate = getGateIntersectingPlayer(player, cursor);
			if (gate != null) return gate;
			gate = getGateAtContent(cursor);
			if (gate != null) return gate;
			cursor.add(step);
		}
		return null;
	}
	
	// -------------------------------------------- //
	// USE GATE
	// -------------------------------------------- //
	
	/**
	 * Tracks peak velocity while a player is inside a horizontal gate so ground contact does not
	 * erase the momentum used for the launch calculation.
	 *
	 * @param event The player move event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void trackHorizontalGateVelocity(PlayerMoveEvent event)
	{
		if (!MConf.get().isHorizontalGatesPreserveVelocity()) return;
		
		Player player = event.getPlayer();
		UGate gate = getGateOnMovePath(event);
		if (gate == null || !gate.getOrientation().isHorizontal())
		{
			PEAK_HORIZONTAL_GATE_VELOCITY.remove(player.getUniqueId());
			return;
		}
		
		Vector sample = sampleMoveVelocity(event);
		UUID playerId = player.getUniqueId();
		Vector peak = PEAK_HORIZONTAL_GATE_VELOCITY.get(playerId);
		if (peak == null || sample.lengthSquared() > peak.lengthSquared())
		{
			PEAK_HORIZONTAL_GATE_VELOCITY.put(playerId, sample);
		}
	}
	
	/**
	 * Handle player move events. This is used to teleport the player when they move into a gate.
	 * 
	 * @param event The player move event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void useGate(PlayerMoveEvent event)
	{
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		
		UGate ugate = getGateOnMovePath(event);
		if (ugate == null) return;
		
		if (ugate.getOrientation().isHorizontal())
		{
			if (handleHorizontalGateMove(player, event, ugate)) return;
		}
		else if (MUtil.isSameBlock(event))
		{
			return;
		}
		
		tryUseGate(player, ugate);
	}
	
	/**
	 * Horizontal gate move handling: detects exit (to start cooldown) and only allows use when the
	 * player clearly moves from outside the portal into it (not when climbing out and dipping back).
	 *
	 * @return {@code true} if this move should not call {@link #tryUseGate}.
	 */
	private static boolean handleHorizontalGateMove(Player player, PlayerMoveEvent event, UGate ugate)
	{
		UUID playerId = player.getUniqueId();
		Location to = event.getTo();
		
		UGate fromGate = getGateIntersectingPlayer(player, event.getFrom());
		UGate toGate = to != null ? getGateIntersectingPlayer(player, to) : null;
		
		// Leaving portal fluid: start re-entry cooldown so swimming up/out does not immediately re-fire.
		if (fromGate == ugate && toGate != ugate)
		{
			HORIZONTAL_GATE_EXIT_COOLDOWN_UNTIL.put(playerId, System.currentTimeMillis() + HORIZONTAL_GATE_EXIT_COOLDOWN_MILLIS);
			return true;
		}
		
		if (isHorizontalGateExitCooldownActive(playerId)) return true;
		
		if (wasRecentCreativeGateTransport(player)) return true;
		
		// Still inside at the start of this move - not a fresh entry.
		if (fromGate == ugate) return true;
		
		// Must end the tick inside this gate (outside → inside), not a grazing path sample.
		if (toGate != ugate) return true;
		
		return false;
	}
	
	private static boolean isHorizontalGateExitCooldownActive(UUID playerId)
	{
		Long until = HORIZONTAL_GATE_EXIT_COOLDOWN_UNTIL.get(playerId);
		if (until == null) return false;
		if (System.currentTimeMillis() >= until)
		{
			HORIZONTAL_GATE_EXIT_COOLDOWN_UNTIL.remove(playerId);
			return false;
		}
		return true;
	}
	
	/**
	 * Backup for the landing tick: fall damage can be evaluated before fall distance is cleared.
	 * Only applies while the launch-airborne flag is still set (cleared on first ground contact).
	 *
	 * @param event The entity damage event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void preventHorizontalLaunchFallDamage(EntityDamageEvent event)
	{
		if (!(event.getEntity() instanceof Player player)) return;
		if (event.getCause() != DamageCause.FALL) return;
		if (!isHorizontalLaunchAirborne(player)) return;
		
		event.setCancelled(true);
		player.setFallDistance(0);
	}
	
	/**
	 * Resets fall distance while airborne from a horizontal launch, like landing on a slime block.
	 * Protection ends on first ground contact so later falls behave normally.
	 *
	 * @param event The player move event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void handleHorizontalLaunchAirborne(PlayerMoveEvent event)
	{
		Player player = event.getPlayer();
		UUID playerId = player.getUniqueId();
		if (!HORIZONTAL_LAUNCH_AIRBORNE.contains(playerId)) return;
		
		if (player.isOnGround())
		{
			HORIZONTAL_LAUNCH_AIRBORNE.remove(playerId);
			player.setFallDistance(0);
			return;
		}
		
		player.setFallDistance(0);
	}
	
	// -------------------------------------------- //
	// DESTROY GATE
	// -------------------------------------------- //
	
	/**
	 * Destroy a gate.
	 * 
	 * @param block The block to destroy the gate at.
	 */
	public static void destroyGate(Block block)
	{
		UGate ugate = UGate.get(block);
		if (ugate == null) return;
		ugate.destroy();
	}
	
	/**
	 * Handle block break events. This is used to destroy the gate when a block is broken.
	 * 
	 * @param event The block break event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockBreakEvent event)
	{
		destroyGate(event.getBlock());
	}
	
	/**
	 * Handle entity change block events. This is used to destroy the gate when an entity changes a block.
	 * 
	 * @param event The entity change block event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(EntityChangeBlockEvent event)
	{
		destroyGate(event.getBlock());
	}
	
	/**
	 * Handle entity explode events. This is used to destroy the gate when an entity explodes.
	 * 
	 * @param event The entity explode event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(EntityExplodeEvent event)
	{
		// Wind charge: do not treat explosion as gate-destroying (use vanilla behavior)
		if (event.getEntity() != null && event.getEntity().getType() == EntityType.WIND_CHARGE) return;

		for (Block block : event.blockList())
		{
			destroyGate(block);
		}
	}
	
	/**
	 * Handle block piston extend events. This is used to destroy the gate when a piston extends.
	 * 
	 * @param event The block piston extend event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockPistonExtendEvent event)
	{
		Set<Block> blocks = new HashSet<>();
		
		Block piston = event.getBlock();
		Block extension = piston.getRelative(event.getDirection());
		blocks.add(extension);
		
		for (Block block : event.getBlocks())
		{
			blocks.add(block);
			blocks.add(block.getRelative(event.getDirection()));
		}
		
		for (Block block : blocks)
		{
			destroyGate(block);
		}
	}
	
	/**
	 * Handle block piston retract events. This is used to destroy the gate when a piston retracts.
	 * 
	 * @param event The block piston retract event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockPistonRetractEvent event)
	{
		destroyGate(event.getBlock().getRelative(event.getDirection(), 1));
		if (event.isSticky())
		{
			destroyGate(event.getBlock().getRelative(event.getDirection(), 2));
		}
	}
	
	/**
	 * Handle block fade events. This is used to destroy the gate when a block fades.
	 * 
	 * @param event The block fade event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockFadeEvent event)
	{
		destroyGate(event.getBlock());
	}
	
	/**
	 * Handle block burn events. This is used to destroy the gate when a block burns.
	 * 
	 * @param event The block burn event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockBurnEvent event)
	{
		destroyGate(event.getBlock());
	}
	
	// -------------------------------------------- //
	// TOOLS
	// -------------------------------------------- //
	
	/**
	 * Handle player interact events. This is used to create or use a gate when a player interacts with a block.
	 * 
	 * @param event The player interact event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void tools(PlayerInteractEvent event)
	{
		// If a player ...
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		
		// ... is clicking a block (create) or looking at a gate (inspect/manage) ...
		final Block clickedBlock = event.getClickedBlock();
		
		// ... and gates are enabled here ...
		if (!MConf.get().isEnabled()) return;

		// ... and the item in hand ...
		final ItemStack currentItem = event.getItem();
		if (currentItem == null) return;
		final Material material = currentItem.getType();
		
		// ... is in any way an interesting material ...
		if
		(
			material != MConf.get().getMaterialInspect()
			&&
			material != MConf.get().getMaterialManage()
			&&
			material != MConf.get().getMaterialCreate()
		)
		{
			return;
		}
		
		// Create still requires a clicked block.
		if (material == MConf.get().getMaterialCreate() && clickedBlock == null) return;
		
		// ... then find the current gate ...
		final UGate currentGate = GateLookUtil.getGateFromClickOrLook(player, clickedBlock);
		
		String message = null;
		
		// ... and if ...
		if (material == MConf.get().getMaterialCreate())
		{
			// ... we are trying to create ...
			
			// ... check permission node ...
			if ( ! Perm.CREATE.has(player, MConf.get().verboseCreatePermission)) return;
			
			// ... check world blacklist ...
			if (MConf.get().isGateCreationDisabledIn(player.getWorld().getName())
				&& !EngineGateOverride.canBypassCreationWorldRestriction(player))
			{
				message = Txt.parse("<b>Gate creation is disabled in this world.");
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			// ... check if the place is occupied ...
			if (currentGate != null)
			{
				message = Txt.parse("<b>There is no room for a new gate since there is already one here.");
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			// ... check if the item is named ...
			ItemMeta currentItemMeta = InventoryUtil.createMeta(currentItem);
			if ( ! currentItemMeta.hasDisplayName())
			{
				message = Txt.parse("<b>You must name the %s before creating a gate with it.", Txt.getMaterialName(material));
				MixinMessage.get().messageOne(player, message);
				return;
			}
			String newNetworkId = ChatColor.stripColor(currentItemMeta.getDisplayName());
			
			// ... perform the flood fill ...
			Block startBlock = clickedBlock.getRelative(event.getBlockFace());
			float absYaw = Math.abs(Location.normalizeYaw(player.getLocation().getYaw()));
			float pitch = player.getLocation().getPitch();
			GateFloodInfo gateFloodInfo = FloodUtil.getGateFloodInfo(startBlock, absYaw, pitch);
			if (gateFloodInfo == null)
			{
				message = Txt.parse("<b>There is no frame for the gate, or it's too big.", Txt.getMaterialName(material));
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			if (gateFloodInfo.orientation.isHorizontal() && !MConf.get().isHorizontalGatesEnabled())
			{
				message = Txt.parse("<b>Horizontal gates are disabled on this server.");
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			if (!MConf.get().isHorizontalGatesEnabled() && gateFloodInfo.orientation.isVertical() && FloodUtil.isLikelyHorizontalGate(gateFloodInfo.interiorBlocks))
			{
				message = Txt.parse("<b>Horizontal gates are disabled on this server. Look horizontally to create a wall gate instead.");
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			GateOrientation gateOrientation = gateFloodInfo.orientation;
			Set<Block> blocks = gateFloodInfo.allBlocks;
			Set<Block> interiorBlocks = gateFloodInfo.interiorBlocks;
			
			// ... ensure the required blocks are present (unless bypassed) ...
			if (!EngineGateOverride.canBypassFrameRequirement(player))
			{
				Map<Material, Integer> materialCounts = MaterialCountUtil.count(blocks);
				if ( ! MaterialCountUtil.has(materialCounts, MConf.get().getBlocksrequired()))
				{
					message = Txt.parse("<b>The frame must contain %s<b>.", MaterialCountUtil.desc(MConf.get().getBlocksrequired()));
					MixinMessage.get().messageOne(player, message);
					return;
				}
			}
			
			// ... calculate the exit location ...
			PS exit = PS.valueOf(player.getLocation());
			exit = exit.withPitch(0F);
			exit = exit.withYaw(gateOrientation.getExitYaw(exit, PS.valueOf(blocks.iterator().next())));
			
			// ... calculate the coords ...
			Set<PS> coords = new HashSet<>();
			Set<PS> interiorCoords = new HashSet<>();
			for (Block block : blocks)
			{
				coords.add(PS.valueOf(block).withWorld(null));
			}
			for (Block block : interiorBlocks)
			{
				interiorCoords.add(PS.valueOf(block).withWorld(null));
			}
			
			List<GateType> selectable = MConf.get().getSelectableGateTypes(gateOrientation);
			if (selectable.isEmpty())
			{
				message = Txt.parse("<b>No allowed gate types are configured for this gate.");
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			PendingGateCreate pending = new PendingGateCreate(
				player.getUniqueId(),
				player.getWorld(),
				newNetworkId,
				exit,
				coords,
				interiorCoords,
				gateOrientation,
				event.getHand()
			);
			
			boolean canPickFill = Perm.SET_GATE_FILL.has(player, MConf.get().verboseSetGateFillPermission);
			if (canPickFill && selectable.size() > 1)
			{
				PendingGateCreates.get().put(pending);
				GateFillPicker.open(player, pending);
				return;
			}
			
			GateType gateType = canPickFill ? selectable.get(0) : MConf.get().resolveDefaultGateType(gateOrientation, player.getWorld());
			if (gateType == null)
			{
				message = Txt.parse("<b>No allowed gate types are configured for this gate.");
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			GateCreate.complete(player, pending, gateType);
		}
		else
		{
			// ... we are trying to inspect or manage ...
			
			// ... skip if the player disabled gate tools ...
			if (!MPlayer.get(player).isToolsEnabled()) return;
			
			// ... silent permission check for tools ...
			if (material == MConf.get().getMaterialInspect() && !Perm.CG_INSPECT.has(player)) return;
			if (material == MConf.get().getMaterialManage() && !Perm.CG_MANAGE.has(player)) return;
			
			// ... and there is a gate ...
			if (currentGate == null)
			{
				// ... and there is no gate ...
				if (clickedBlock != null && isGateNearby(clickedBlock))
				{
					// ... but there is portal nearby.
					
					// ... exit with a message.
					message = Txt.parse("<i>You use the %s on the %s but there seem to be no gate.", Txt.getMaterialName(material), Txt.getMaterialName(clickedBlock.getType()));
					MixinMessage.get().messageOne(player, message);
					return;
				}
				else
				{
					// ... and there is no portal nearby ...
					
					// ... exit quietly.
					return;
				}
			}
			
			// ... refresh BlockDisplay / client-overlay fills if needed ...
			if (currentGate.usesBlockDisplayFill())
			{
				currentGate.fill();
			}
			
			// ... send use action description ...
			message = Txt.parse("<i>You use the <v>%s <i>on the <v>Gate<i>...", Txt.getMaterialName(material));
			MixinMessage.get().messageOne(player, message);
			
			if (material == MConf.get().getMaterialInspect())
			{
				// ... we are trying to inspect ...
				if (currentGate.isRestricted() && EngineGateOverride.canReadSecret(player, currentGate) && currentGate.isCreator(player))
				{
					message = Txt.parse("<i>... the gate is restricted but you are the creator ...");
					MixinMessage.get().messageOne(player, message);
				}
				
				GateInspectUtil.show(player, currentGate, 1, CmdCg.get().cmdCgInspect);
			}
			else if (material == MConf.get().getMaterialManage())
			{
				// ... we are trying to manage ...
				if (!EngineGateOverride.canManage(player, currentGate))
				{
					message = Txt.parse("<b>... only the gate creator can manage this gate.");
					MixinMessage.get().messageOne(player, message);
					return;
				}
				
				GateManageUi.open(player, currentGate);
			}
			
		}
		
	}
	
}
