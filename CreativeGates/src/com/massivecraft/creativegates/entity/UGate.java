package com.massivecraft.creativegates.entity;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.engine.EngineGateFillDisplay;
import com.massivecraft.creativegates.engine.EngineGateFillParticles;
import com.massivecraft.creativegates.engine.EngineMain;
import com.massivecraft.creativegates.engine.EngineMain.HorizontalEntryContext;
import com.massivecraft.creativegates.gate.GateOrientation;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.creativegates.gate.fill.GateTypeResolve;
import com.massivecraft.creativegates.gate.fill.SupportedGateType;
import com.massivecraft.creativegates.util.GateEntityTeleport;
import com.massivecraft.creativegates.util.GateTeleportSafety;
import com.massivecraft.creativegates.util.HorizontalGateLaunchUtil;
import com.massivecraft.creativegates.util.HorizontalGateLaunchUtil.LaunchPlan;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mixin.MixinTeleport;
import com.massivecraft.massivecore.mixin.MixinVisibility;
import com.massivecraft.massivecore.mixin.TeleporterException;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.teleport.Destination;
import com.massivecraft.massivecore.teleport.DestinationSimple;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.SmokeUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class UGate extends Entity<UGate>
{
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	/**
	 * Gets the gate for an object ID.
	 * 
	 * @param oid The object ID to get the gate for.
	 * @return The gate for the object ID.
	 */
	public static UGate get(Object oid)
	{
		if (oid == null) throw new NullPointerException("oid");

		String id = UGateColl.get().fixId(oid);
		if (id == null) return null;
		return UGateColl.get().getFixed(id);
	}
	
	// -------------------------------------------- //
	// OVERRIDE: ENTITY
	// -------------------------------------------- //
	
	@Override
	public UGate load(UGate that)
	{
		this.creatorId = that.creatorId;
		this.createdMillis = that.createdMillis;
		this.usedMillis = that.usedMillis;
		this.networkId = that.networkId;
		this.restricted = that.restricted;
		this.enterEnabled = that.enterEnabled;
		this.exitEnabled = that.exitEnabled;
		this.allowPlayers = that.allowPlayers;
		this.allowMobs = that.allowMobs;
		this.allowVehicles = that.allowVehicles;
		this.exit = that.exit;
		this.orientation = that.orientation;
		this.fillTypeId = that.fillTypeId;
		this.setCoordsNoChanged(that.coords);
		this.setInteriorCoordsNoChanged(that.interiorCoords);
		
		return this;
	}
	
	@Override
	public void postAttach(String id)
	{
		if (this.getExit() == null) return;
		CreativeGates.get().getIndex().add(this);
	}
	
	@Override
	public void postDetach(String id)
	{
		if (this.getExit() == null) return;
		CreativeGates.get().getIndex().remove(this);
	}
	
	@Override
	public UGateColl getColl()
	{
		return (UGateColl) super.getColl();
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private String creatorId = null;
	/**
	 * Gets the creator ID for the gate.
	 * 
	 * @return The creator ID for the gate.
	 */
	public String getCreatorId()
	{
		return this.creatorId;
	}
	/**
	 * Sets the creator ID for the gate.
	 * 
	 * @param creatorId The creator ID to set.
	 */
	public void setCreatorId(String creatorId)
	{
		this.changed(this.creatorId, creatorId);
		this.creatorId = creatorId;
	}
	
	private long createdMillis = System.currentTimeMillis();
	/**
	 * Gets the created millis for the gate.
	 * 
	 * @return The created millis for the gate.
	 */
	public long getCreatedMillis() { return this.createdMillis; }
	/**
	 * Sets the created millis for the gate.
	 * 
	 * @param createdMillis The created millis to set.
	 */
	public void setCreatedMillis(long createdMillis)
	{
		this.changed(this.createdMillis, createdMillis);
		this.createdMillis = createdMillis;
	}
	
	private long usedMillis = 0;
	/**
	 * Gets the used millis for the gate.
	 * 
	 * @return The used millis for the gate.
	 */
	public long getUsedMillis()
	{
		return this.usedMillis;
	}
	/**
	 * Sets the used millis for the gate.
	 * 
	 * @param usedMillis The used millis to set.
	 */
	public void setUsedMillis(long usedMillis)
	{
		this.changed(this.usedMillis, usedMillis);
		this.usedMillis = usedMillis;
	}
	
	private String networkId = null;
	/**
	 * Gets the network ID for the gate.
	 * 
	 * @return The network ID for the gate.
	 */
	public String getNetworkId()
	{
		return this.networkId;
	}
	/**
	 * Sets the network ID for the gate.
	 * 
	 * @param networkId The network ID to set.
	 */
	public void setNetworkId(String networkId)
	{
		this.changed(this.networkId, networkId);
		this.networkId = networkId;
	}
	
	private boolean restricted = false;
	/**
	 * Gets the restricted state for the gate.
	 * 
	 * @return The restricted state for the gate.
	 */
	public boolean isRestricted()
	{
		return this.restricted;
	}
	/**
	 * Sets the restricted state for the gate.
	 * 
	 * @param restricted The restricted state to set.
	 */
	public void setRestricted(boolean restricted)
	{
		this.changed(this.restricted, restricted);
		this.restricted = restricted;
	}
	
	private boolean enterEnabled = true;
	/**
	 * Gets the enter enabled state for the gate.
	 * 
	 * @return The enter enabled state for the gate.
	 */
	public boolean isEnterEnabled()
	{
		return this.enterEnabled;
	}
	/**
	 * Sets the enter enabled state for the gate.
	 * 
	 * @param enterEnabled The enter enabled state to set.
	 */
	public void setEnterEnabled(boolean enterEnabled)
	{
		this.changed(this.enterEnabled, enterEnabled);
		this.enterEnabled = enterEnabled;
	}
	
	private boolean exitEnabled = true;
	/**
	 * Gets the exit enabled state for the gate.
	 * 
	 * @return The exit enabled state for the gate.
	 */
	public boolean isExitEnabled()
	{
		return this.exitEnabled;
	}
	/**
	 * Sets the exit enabled state for the gate.
	 * 
	 * @param exitEnabled The exit enabled state to set.
	 */
	public void setExitEnabled(boolean exitEnabled)
	{
		this.changed(this.exitEnabled, exitEnabled);
		this.exitEnabled = exitEnabled;
	}

	/**
	 * Whether players may travel through this gate (when enter is also enabled).
	 */
	private boolean allowPlayers = true;

	/**
	 * Gets whether players may use this gate for travel.
	 * 
	 * @return True if players may use this gate for travel.
	 */
	public boolean isAllowPlayers()
	{
		return this.allowPlayers;
	}

	/**
	 * Sets whether players may travel through this gate.
	 *
	 * @param allowPlayers True to allow player travel.
	 */
	public void setAllowPlayers(boolean allowPlayers)
	{
		this.changed(this.allowPlayers, allowPlayers);
		this.allowPlayers = allowPlayers;
	}

	/**
	 * Per-gate mob teleport override. {@code null} means follow the server when mobs are allowed.
	 * Cannot enable mobs when {@link MConf#isGatesAllowMobs()} is false.
	 */
	private Boolean allowMobs = null;

	/**
	 * Effective whether mobs may use this gate.
	 * <p>
	 * Server {@link MConf#isGatesAllowMobs()} is a hard kill-switch: when false, every gate
	 * is denied regardless of any stored per-gate {@code true}. When the server allows mobs,
	 * a per-gate {@code false} can still disable them for that gate; {@code null} follows the server.
	 * </p>
	 */
	public boolean isAllowMobs()
	{
		if (!MConf.get().isGatesAllowMobs()) return false;
		if (this.allowMobs == null) return true;
		return this.allowMobs;
	}

	/**
	 * Raw per-gate override, or {@code null} when following server config.
	 */
	public Boolean getAllowMobsOverride()
	{
		return this.allowMobs;
	}

	/**
	 * Sets the per-gate mob teleport override. Pass {@code null} to follow server config.
	 * Values equal to the current server default are stored as {@code null}.
	 * 
	 * @param allowMobs The mob teleport override to set.
	 */
	public void setAllowMobs(Boolean allowMobs)
	{
		Boolean target = allowMobs;
		if (MUtil.equals(target, MConf.get().isGatesAllowMobs())) target = null;

		if (MUtil.equals(this.allowMobs, target)) return;

		this.changed(this.allowMobs, target);
		this.allowMobs = target;
	}

	/**
	 * Per-gate vehicle teleport override. {@code null} means follow the server when vehicles are allowed.
	 * Cannot enable vehicles when {@link MConf#isGatesAllowVehicles()} is false.
	 */
	private Boolean allowVehicles = null;

	/**
	 * Effective whether non-living vehicles may use this gate.
	 * <p>
	 * Server {@link MConf#isGatesAllowVehicles()} is a hard kill-switch. When the server allows
	 * vehicles, a per-gate {@code false} can still disable them; {@code null} follows the server.
	 * </p>
	 */
	public boolean isAllowVehicles()
	{
		if (!MConf.get().isGatesAllowVehicles()) return false;
		if (this.allowVehicles == null) return true;
		return this.allowVehicles;
	}

	/**
	 * Raw per-gate override, or {@code null} when following server config.
	 */
	public Boolean getAllowVehiclesOverride()
	{
		return this.allowVehicles;
	}

	/**
	 * Sets the per-gate vehicle teleport override. Pass {@code null} to follow server config.
	 * Values equal to the current server default are stored as {@code null}.
	 * 
	 * @param allowVehicles The vehicle teleport override to set.
	 */
	public void setAllowVehicles(Boolean allowVehicles)
	{
		Boolean target = allowVehicles;
		if (MUtil.equals(target, MConf.get().isGatesAllowVehicles())) target = null;

		if (MUtil.equals(this.allowVehicles, target)) return;

		this.changed(this.allowVehicles, target);
		this.allowVehicles = target;
	}
	
	private PS exit = null;
	/**
	 * Gets the exit location for the gate.
	 * 
	 * @return The exit location for the gate.
	 */
	public PS getExit()
	{
		return this.exit;
	}
	/**
	 * Sets the exit location for the gate.
	 * 
	 * @param exit The exit location to set.
	 */
	public void setExit(PS exit)
	{
		this.changed(this.exit, exit);
		this.exit = exit;
	}
	
	private Set<PS> coords = new TreeSet<>();

	/**
	 * Gets the coordinates for the gate.
	 * 
	 * @return The coordinates for the gate.
	 */
	public Set<PS> getCoords()
	{
		return Collections.unmodifiableSet(this.coords);
	}
	/**
	 * Sets the coordinates for the gate.
	 * 
	 * @param coords The coordinates to set.
	 */
	private void setCoordsNoChanged(Collection<PS> coords)
	{
		if (this.attached()) CreativeGates.get().getIndex().remove(this);
		
		this.coords = new TreeSet<>(coords);
		
		if (this.attached()) CreativeGates.get().getIndex().add(this);
	}
	/**
	 * Sets the coordinates for the gate.
	 * 
	 * @param coords The coordinates to set.
	 */
	public void setCoords(Collection<PS> coords)
	{
		this.changed(this.coords, coords);
		this.setCoordsNoChanged(coords);
	}
	
	// Portal interior only (air flood fill). Used for fill/empty; coords also includes the frame shell.
	private Set<PS> interiorCoords = new TreeSet<>();
	/**
	 * Gets the interior coordinates for the gate.
	 * 
	 * @return The interior coordinates for the gate.
	 */
	public Set<PS> getInteriorCoords()
	{
		return Collections.unmodifiableSet(this.interiorCoords);
	}
	/**
	 * Sets the interior coordinates for the gate.
	 * 
	 * @param interiorCoords The interior coordinates to set.
	 */
	private void setInteriorCoordsNoChanged(Collection<PS> interiorCoords)
	{
		this.interiorCoords = new TreeSet<>(interiorCoords);
	}
	/**
	 * Sets the interior coordinates for the gate.
	 * 
	 * @param interiorCoords The interior coordinates to set.
	 */
	public void setInteriorCoords(Collection<PS> interiorCoords)
	{
		this.changed(this.interiorCoords, interiorCoords);
		this.setInteriorCoordsNoChanged(interiorCoords);
	}

	private GateOrientation orientation = GateOrientation.NS;
	/**
	 * Gets the orientation for the gate.
	 * 
	 * @return The orientation for the gate.
	 */
	public GateOrientation getOrientation()
	{
		return this.orientation;
	}
	/**
	 * Sets the orientation for the gate.
	 * 
	 * @param orientation The orientation to set.
	 */
	public void setOrientation(GateOrientation orientation)
	{
		this.changed(this.orientation, orientation);
		this.orientation = orientation;
	}
	
	/**
	 * Config id of the fill type ({@link SupportedGateType} name, material name, or {@code PARTICLE_*}).
	 * Resolved via {@link GateTypeResolve}.
	 */
	private String fillTypeId = null;
	
	/**
	 * @return Stored fill type id, or null if unset.
	 */
	public String getFillTypeId()
	{
		return this.fillTypeId;
	}
	
	/**
	 * Sets the fill type by config id.
	 *
	 * @param fillTypeId Enum or material name; may be null.
	 */
	public void setFillTypeId(String fillTypeId)
	{
		String normalized = fillTypeId == null ? null : fillTypeId.trim().toUpperCase();
		if (normalized != null && normalized.isEmpty()) normalized = null;
		this.changed(this.fillTypeId, normalized);
		this.fillTypeId = normalized;
	}
	
	/**
	 * Sets the fill from a resolved {@link GateType}.
	 *
	 * @param gateType Type to store; null clears.
	 */
	public void setFillType(GateType gateType)
	{
		this.setFillTypeId(gateType == null ? null : gateType.getConfigId());
	}
	
	/**
	 * @return True when the look is BlockDisplay (or END_GATEWAY fallback) for this gate's orientation.
	 */
	public boolean usesBlockDisplayFill()
	{
		GateType type = this.getFillType();
		return type != null && type.usesBlockDisplay(this.orientation);
	}
	
	/**
	 * @return True when the interior is a particle fill rather than blocks.
	 */
	public boolean usesParticleFill()
	{
		GateType type = this.getFillType();
		return type != null && type.isParticleFill();
	}
	
	/**
	 * Material clients should see for this gate's interior.
	 *
	 * @return Display material, or null if unresolved.
	 */
	public Material getClientDisplayMaterial()
	{
		GateType type = this.getFillType();
		return type != null ? type.getClientDisplayMaterial() : null;
	}
	
	/**
	 * Resolves the fill type from {@link #fillTypeId}, inferring and persisting when missing.
	 *
	 * @return Effective type, or null if none can be resolved.
	 */
	public GateType getFillType()
	{
		if (this.fillTypeId != null)
		{
			GateType parsed = GateTypeResolve.parse(this.fillTypeId);
			if (parsed != null) return parsed;
		}
		
		GateType inferred = this.inferGateTypeFromContent();
		if (inferred == null)
		{
			inferred = MConf.get().resolveDefaultGateType(this.orientation, this.getWorld());
		}
		if (inferred != null)
		{
			this.setFillType(inferred);
		}
		return inferred;
	}
	
	/**
	 * Infers type from existing interior blocks without persisting.
	 *
	 * @return Inferred type, or null.
	 */
	private GateType inferGateTypeFromContent()
	{
		List<Block> blocks = this.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return null;
		
		for (Block block : blocks)
		{
			SupportedGateType type = SupportedGateType.fromServerMaterial(block.getType());
			if (type != null) return type;
		}
		return null;
	}
	
	/**
	 * Returns whether the block is part of this gate's portal interior (not frame).
	 *
	 * @param block Block to test.
	 * @return True if the block is interior content.
	 */
	public boolean isInteriorBlock(Block block)
	{
		if (block == null) return false;
		World world = this.getWorld();
		if (world == null || !world.equals(block.getWorld())) return false;
		
		PS ps = PS.valueOf(block).withWorld(null);
		return this.getContentCoordSet().contains(ps);
	}
	
	// -------------------------------------------- //
	// ASSORTED
	// -------------------------------------------- //
	
	/**
	 * Checks if a command sender is the creator of the gate.
	 * 
	 * @param sender The command sender to check.
	 * @return True if the command sender is the creator of the gate, false otherwise.
	 */
	public boolean isCreator(CommandSender sender)
	{
		String senderId = IdUtil.getId(sender);
		if (senderId == null) return false;
		return senderId.equalsIgnoreCase(this.creatorId);
	}
	
	/**
	 * Destroys the gate.
	 */
	public void destroy()
	{
		this.empty();
		this.detach();
		this.fxKitDestroy(null);
	}
	
	// -------------------------------------------- //
	// TRANSPORT
	// -------------------------------------------- //
	
	/**
	 * Transports a player through the gate chain.
	 * 
	 * @param player The player to transport.
	 */
	public boolean transport(Player player)
	{
		return this.transport(player, null, null);
	}
	
	/**
	 * Transports a player through the gate chain, optionally preserving entry momentum for horizontal gates.
	 * 
	 * @param player The player to transport.
	 * @param entryContext Velocity and launch eligibility for horizontal gates, or null.
	 * @param sourceLocation Where the player entered; used to return them if the exit is blocked.
	 */
	public boolean transport(Player player, HorizontalEntryContext entryContext, Location sourceLocation)
	{
		if (this.isAllowMobs() && GateEntityTeleport.hasMobEntourage(player)
			|| this.isAllowVehicles() && GateEntityTeleport.hasNonLivingVehicle(player))
		{
			return this.transportPlayerWithEntourage(player, sourceLocation);
		}

		List<UGate> gateChain = this.getGateChain();
		
		String message;
		String blockedMessage = Txt.parse("<b>The gate exit is blocked.");
		
		for (UGate ugate : gateChain)
		{
			if ( ! ugate.isExitEnabled()) continue;
			
			PS destinationPs = ugate.getExit();
			String destinationDesc = (MConf.get().teleportationMessageActive ? "the gate destination" : "");
			
			boolean tryLaunch = entryContext != null
				&& entryContext.launchEligible
				&& ugate.getOrientation().isHorizontal()
				&& MConf.get().isHorizontalGatesPreserveVelocity();
			
			LaunchPlan launchPlan = tryLaunch ? HorizontalGateLaunchUtil.tryPlan(ugate, entryContext.velocity) : null;
			
			if (launchPlan == null && !GateTeleportSafety.isDestinationSafe(player, destinationPs))
			{
				MixinMessage.get().messageOne(player, blockedMessage);
				continue;
			}
			
			Destination destination = new DestinationSimple(
				launchPlan != null ? launchPlan.getLaunchPs() : destinationPs,
				destinationDesc
			);
			
			try
			{
				MixinTeleport.get().teleport(player, destination, 0);
				
				if (launchPlan == null && !GateTeleportSafety.isDestinationSafe(player, destinationPs))
				{
					this.returnPlayerToSource(player, sourceLocation);
					MixinMessage.get().messageOne(player, blockedMessage);
					continue;
				}
				
				this.setUsedMillis(System.currentTimeMillis());
				this.fxKitUse(player);
				if (launchPlan != null)
				{
					this.applyLaunch(player, launchPlan);
				}
				return true;
			}
			catch (TeleporterException e)
			{
				message = e.getMessage();
				MixinMessage.get().messageOne(player, message);
			}
		}
		
		message = Txt.parse("<i>This gate does not seem to lead anywhere.");
		MixinMessage.get().messageOne(player, message);
		return false;
	}

	/**
	 * Transports a player together with their mount and/or leashed mobs, keeping mounts and leads.
	 * Momentum launch is skipped so the whole party can be moved as a unit.
	 * 
	 * @param player The player to transport.
	 * @param sourceLocation The location the player entered from.
	 * @return True if the player was teleported.
	 */
	private boolean transportPlayerWithEntourage(Player player, Location sourceLocation)
	{
		boolean bringMobs = this.isAllowMobs() && GateEntityTeleport.hasMobEntourage(player);
		boolean bringVehicles = this.isAllowVehicles() && GateEntityTeleport.hasNonLivingVehicle(player);

		List<UGate> gateChain = this.getGateChain();
		String blockedMessage = Txt.parse("<b>The gate exit is blocked.");

		for (UGate ugate : gateChain)
		{
			if (!ugate.isExitEnabled()) continue;
			if (bringMobs && !ugate.isAllowMobs()) continue;
			if (bringVehicles && !ugate.isAllowVehicles()) continue;

			PS destinationPs = ugate.getExit();
			if (!GateTeleportSafety.isDestinationSafe(player, destinationPs))
			{
				MixinMessage.get().messageOne(player, blockedMessage);
				continue;
			}

			Location destination;
			try
			{
				destination = destinationPs.asBukkitLocation(true);
			}
			catch (IllegalStateException e)
			{
				continue;
			}

			if (!GateEntityTeleport.teleportParty(player, destination)) continue;

			if (!GateTeleportSafety.isDestinationSafe(player, destinationPs))
			{
				this.returnPlayerToSource(player, sourceLocation);
				MixinMessage.get().messageOne(player, blockedMessage);
				continue;
			}

			this.setUsedMillis(System.currentTimeMillis());
			this.fxKitUse(player);
			return true;
		}

		MixinMessage.get().messageOne(player, Txt.parse("<i>This gate does not seem to lead anywhere."));
		return false;
	}

	/**
	 * Transports a non-player entity (living mob or vehicle) and its mount / passengers / leash party
	 * through the gate chain.
	 *
	 * @param entity The entity to transport.
	 * @return {@code true} if the entity was teleported.
	 */
	public boolean transportEntity(org.bukkit.entity.Entity entity)
	{
		if (entity == null || !entity.isValid()) return false;
		if (entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) return false;
		if (entity instanceof Player) return false;

		boolean livingTrigger = entity instanceof LivingEntity;
		if (livingTrigger)
		{
			if (!this.isAllowMobs()) return false;
		}
		else if (!this.isAllowVehicles())
		{
			return false;
		}
		if (!this.isEnterEnabled()) return false;

		// Leash/mount party that includes a player: use the player path (perms, debounce, FX).
		Player player = GateEntityTeleport.findPlayerInParty(entity);
		if (player != null)
		{
			return EngineMain.tryUseGate(player, this);
		}

		List<UGate> gateChain = this.getGateChain();
		for (UGate ugate : gateChain)
		{
			if (!ugate.isExitEnabled()) continue;
			if (livingTrigger)
			{
				if (!ugate.isAllowMobs()) continue;
			}
			else if (!ugate.isAllowVehicles())
			{
				continue;
			}

			PS destinationPs = ugate.getExit();
			if (!GateTeleportSafety.isDestinationSafe(entity, destinationPs)) continue;

			Location destination;
			try
			{
				destination = destinationPs.asBukkitLocation(true);
			}
			catch (IllegalStateException e)
			{
				continue;
			}

			if (!GateEntityTeleport.teleportParty(entity, destination)) continue;

			this.setUsedMillis(System.currentTimeMillis());
			this.fxKitUseEntity(entity);
			return true;
		}
		return false;
	}

	/**
	 * Plays use FX for an entity party (player sound when a player is present).
	 * 
	 * @param entity The entity to play the use FX for.
	 */
	private void fxKitUseEntity(org.bukkit.entity.Entity entity)
	{
		Player player = GateEntityTeleport.findPlayerInParty(entity);
		if (player != null)
		{
			this.fxKitUse(player);
			return;
		}
		EngineGateFillParticles.get().burstGate(this);
	}
	
	private void returnPlayerToSource(Player player, Location sourceLocation)
	{
		if (sourceLocation == null) return;
		try
		{
			Destination source = new DestinationSimple(PS.valueOf(sourceLocation), "");
			MixinTeleport.get().teleport(player, source, 0);
		}
		catch (TeleporterException ignored)
		{
		}
	}
	
	private void applyLaunch(Player player, LaunchPlan launchPlan)
	{
		Vector launchVelocity = launchPlan.getVelocity().clone();
		Location launchLoc;
		try
		{
			launchLoc = launchPlan.getLaunchPs().asBukkitLocation(true);
		}
		catch (IllegalStateException e)
		{
			return;
		}
		
		float lookYaw = launchLoc.getYaw();
		CreativeGates.get().getServer().getScheduler().runTask(CreativeGates.get(), () ->
		{
			launchLoc.setYaw(lookYaw);
			launchLoc.setPitch(0f);
			player.teleport(launchLoc);
			player.setVelocity(launchVelocity);
			player.setFallDistance(0);
			EngineMain.markHorizontalLaunchAirborne(player);
		});
	}
	
	/**
	 * Gets the gate chain for the gate.
	 * 
	 * @return The gate chain for the gate.
	 */
	public List<UGate> getGateChain()
	{
		List<UGate> ret = new ArrayList<>();
		
		List<UGate> rawchain = this.getColl().getGateChain(this.getNetworkId());
		int myIndex = rawchain.indexOf(this);
		
		// Add what is after me
		ret.addAll(rawchain.subList(myIndex+1, rawchain.size()));
		
		// Add what is before me
		ret.addAll(rawchain.subList(0, myIndex));
		
		return ret;
	}
	
	// -------------------------------------------- //
	// CONTENT
	// -------------------------------------------- //
	
	/**
	 * Gets the world for the gate.
	 * 
	 * @return The world for the gate.
	 * @throws IllegalStateException if the exit is not in a world.
	 */
	private World getWorld()
	{
		try
		{
			return this.getExit().asBukkitWorld(true);
		}
		catch (IllegalStateException e)
		{
			return null;
		}
	}
	
	/**
	 * Gets the blocks for a set of coordinates.
	 * 
	 * @param coords The coordinates to get the blocks for.
	 * @return The blocks for the coordinates.
	 */
	private List<Block> getBlocksForCoords(Set<PS> coords)
	{
		World world = this.getWorld();
		if (world == null) return null;
		
		List<Block> ret = new ArrayList<>(coords.size());
		for (PS coord : coords)
		{
			Block block = world.getBlockAt(coord.getBlockX(), coord.getBlockY(), coord.getBlockZ());
			ret.add(block);
		}
		
		return ret;
	}
	
	/**
	 * Gets the blocks for the gate.
	 * 
	 * @return The blocks for the gate.
	 */
	public List<Block> getBlocks()
	{
		return this.getBlocksForCoords(this.coords);
	}
	
	/**
	 * Gets the content blocks for the gate.
	 * 
	 * @return The content blocks for the gate.
	 */
	public List<Block> getContentBlocks()
	{
		return this.getBlocksForCoords(this.getContentCoordSet());
	}
	
	/**
	 * Gets the content coordinates for the gate.
	 * 
	 * @return The content coordinates for the gate.
	 */
	private Set<PS> getContentCoordSet()
	{
		if ( ! this.interiorCoords.isEmpty()) return this.interiorCoords;
		return this.inferLegacyContentCoords();
	}
	
	/**
	 * Gets the legacy content coordinates for the gate.
	 * 
	 * @return The legacy content coordinates for the gate.
	 */
	private Set<PS> inferLegacyContentCoords()
	{
		Set<PS> ret = new TreeSet<>();
		World world = this.getWorld();
		if (world == null) return ret;
		
		for (PS coord : this.coords)
		{
			Material material = world.getBlockAt(coord.getBlockX(), coord.getBlockY(), coord.getBlockZ()).getType();
			if (!CreativeGates.isGateFillOrVoid(material)) continue;
			ret.add(coord);
		}
		
		return ret;
	}
	
	/**
	 * Gets the center block for the gate.
	 * 
	 * @return The center block for the gate.
	 */
	public Block getCenterBlock()
	{
		List<Block> blocks = this.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return null;
		
		return blocks.get(blocks.size() / 2);
	}
	
	/**
	 * Checks if the gate is intact.
	 * 
	 * @return True if the gate is intact, false otherwise.
	 */
	public boolean isIntact()
	{
		List<Block> blocks = this.getContentBlocks();
		if (blocks == null) return true;
		
		GateType type = this.getFillType();
		if (type == null) return true;
		
		World world = blocks.get(0).getWorld();
		for (Block block : blocks)
		{
			if (!type.isExpectedServerFill(block.getType(), world, this.orientation)) return false;
		}
		return true;
	}
	
	/**
	 * Sets the content of the gate to a material.
	 * 
	 * @param material The material to set the content to.
	 */
	public void setContent(Material material)
	{
		this.setContent(material, true);
	}
	
	/**
	 * Sets the content of the gate to a material.
	 * 
	 * @param material The material to set the content to.
	 * @param applyPhysics Whether to apply physics to the blocks.
	 */
	public void setContent(Material material, boolean applyPhysics)
	{
		List<Block> blocks = this.getContentBlocks();
		if (blocks == null) return;
		
		GateType type = this.getFillType();
		int lightLevel = type != null ? type.getEmittedBlockLightLevel(this.orientation) : -1;
		
		for (Block block : blocks)
		{
			Material blockMaterial = block.getType();
			
			if (!CreativeGates.isGateFillOrVoid(blockMaterial)) continue;
			
			if (material == Material.LIGHT && lightLevel >= 0)
			{
				org.bukkit.block.data.type.Light light = (org.bukkit.block.data.type.Light) Material.LIGHT.createBlockData();
				light.setLevel(Math.min(15, lightLevel));
				block.setBlockData(light, applyPhysics);
			}
			else
			{
				block.setType(material, applyPhysics);
			}
		}
	}
	
	/**
	 * Fills the gate with the fill material and syncs BlockDisplay visuals.
	 */
	public void fill()
	{
		List<Block> blocks = this.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return;
		
		GateType type = this.getFillType();
		CreativeGates.get().setFilling(true);
		this.setContent(CreativeGates.getFillMaterial(type, blocks.get(0).getWorld(), this.orientation));
		CreativeGates.get().setFilling(false);
		
		if (this.usesBlockDisplayFill())
		{
			EngineGateFillDisplay.get().syncGate(this);
		}
		else
		{
			EngineGateFillDisplay.get().clearGate(this);
		}
	}
	
	/**
	 * Empties the gate.
	 */
	public void empty()
	{
		EngineGateFillDisplay.get().clearGate(this);
		this.setContent(Material.AIR, false);
	}
	
	// -------------------------------------------- //
	// FX KIT
	// -------------------------------------------- //

	/**
	 * Plays the create effect for the gate.
	 * 
	 * @param player The player to play the effect for.
	 */
	public void fxKitCreate(Player player)
	{
		playConfiguredTeleportSound(player, false);
		EngineGateFillParticles.get().burstGate(this);
	}
	
	/**
	 * Plays the use effect for the gate.
	 * 
	 * @param player The player to play the effect for.
	 */
	public void fxKitUse(Player player)
	{
		playConfiguredTeleportSound(player, true);
		EngineGateFillParticles.get().burstGate(this);
	}
	
	/**
	 * Plays the configured teleport sound for the gate.
	 * 
	 * @param player The player to play the effect for.
	 * @param requireActive Whether to require the sound to be active.
	 */
	private void playConfiguredTeleportSound(Player player, boolean requireActive)
	{
		MConf mconf = MConf.get();
		if (requireActive && !mconf.teleportationSoundActive) return;
		if (player.getGameMode() == GameMode.SPECTATOR) return;
		if (!MixinVisibility.get().isVisible(player)) return;
		
		Sound sound = mconf.resolveTeleportationSound();
		player.playSound(player.getLocation(), sound, mconf.teleportationSoundVolume, mconf.teleportationSoundPitch);
	}
	
	/**
	 * Plays the destroy effect for the gate.
	 * 
	 * @param player The player to play the effect for.
	 */
	public void fxKitDestroy(Player player)
	{
		this.fxExplode();
	}
	
	// -------------------------------------------- //
	// FX SINGLE
	// -------------------------------------------- //
	
	/**
	 * Plays the smoke effect for the gate.
	 * 
	 * @param player The player to play the effect for.
	 */
	public void fxSmoke()
	{
		List<Block> blocks = this.getContentBlocks();
		if (blocks == null) return;
		for (Block block : blocks)
		{
			SmokeUtil.spawnCloudSimple(block.getLocation());
		}
	}
	
	/**
	 * Plays the ender effect for the gate.
	 * 
	 * @param player The player to play the effect for.
	 */
	public void fxEnder()
	{
		List<Block> blocks = this.getContentBlocks();
		if (blocks == null) return;
		for (Block block : blocks)
		{
			Location location = block.getLocation();
			location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 0);
		}
	}
	
	/**
	 * Plays the explode effect for the gate.
	 */
	public void fxExplode()
	{
		Block block = this.getCenterBlock();
		if (block == null) return;
		
		Location location = block.getLocation().add(0.5, 0.5, 0.5);
		World world = location.getWorld();
		
		SmokeUtil.spawnCloudSimple(location);
		world.playEffect(location, Effect.ENDER_SIGNAL, 0);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
	}
	
}
