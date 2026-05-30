package com.massivecraft.creativegates.entity;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.GateOrientation;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mixin.MixinTeleport;
import com.massivecraft.massivecore.mixin.MixinVisibility;
import com.massivecraft.massivecore.mixin.TeleporterException;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.teleport.Destination;
import com.massivecraft.massivecore.teleport.DestinationSimple;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.SmokeUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Axis;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
		this.exit = that.exit;
		this.orientation = that.orientation;
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
	
	/**
	 * Toggles the mode of the gate.
	 */
	public void toggleMode()
	{
		boolean enter = this.isEnterEnabled();
		boolean exit = this.isExitEnabled();
		
		if (enter == false && exit == false)
		{
			this.setEnterEnabled(true);
			this.setExitEnabled(false);
		}
		else if (enter == true && exit == false)
		{
			this.setEnterEnabled(false);
			this.setExitEnabled(true);
		}
		else if (enter == false && exit == true)
		{
			this.setEnterEnabled(true);
			this.setExitEnabled(true);
		}
		else if (enter == true && exit == true)
		{
			this.setEnterEnabled(false);
			this.setExitEnabled(false);
		}
	}
	
	// -------------------------------------------- //
	// TRANSPORT
	// -------------------------------------------- //
	
	/**
	 * Transports a player through the gate chain.
	 * 
	 * @param player The player to transport.
	 */
	public void transport(Player player)
	{
		List<UGate> gateChain = this.getGateChain();
		
		String message;
		
		for (UGate ugate : gateChain)
		{
			if ( ! ugate.isExitEnabled()) continue;
			
			PS destinationPs = ugate.getExit();
			String destinationDesc = (MConf.get().teleportationMessageActive ? "the gate destination" : "");
			Destination destination = new DestinationSimple(destinationPs, destinationDesc);
			
			try
			{
				MixinTeleport.get().teleport(player, destination, 0);
				this.setUsedMillis(System.currentTimeMillis());
				this.fxKitUse(player);
				return;
			}
			catch (TeleporterException e)
			{
				message = e.getMessage();
				MixinMessage.get().messageOne(player, message);
			}
		}
		
		message = Txt.parse("<i>This gate does not seem to lead anywhere.");
		MixinMessage.get().messageOne(player, message);
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
			if (material != Material.NETHER_PORTAL && !CreativeGates.isFluidFillMaterial(material) && !CreativeGates.isVoid(material)) continue;
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
		
		for (Block block : blocks)
		{
			if (CreativeGates.isVoid(block))
			{
				return false;
			}
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
		Axis axis = orientation == GateOrientation.NS ? Axis.Z : Axis.X;
		
		for (Block block : blocks)
		{
			Material blockMaterial = block.getType();
			
			if (blockMaterial != Material.NETHER_PORTAL && !CreativeGates.isFluidFillMaterial(blockMaterial) && !CreativeGates.isVoid(blockMaterial)) continue;
			
			block.setType(material, applyPhysics);
			
			// Apply orientation
			if (material != Material.NETHER_PORTAL) continue;

			Orientable orientable = (Orientable) block.getBlockData();
			orientable.setAxis(axis);
			block.setBlockData(orientable);
		}
	}
	
	/**
	 * Fills the gate with the fill material.
	 */
	public void fill()
	{
		List<Block> blocks = this.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return;
		
		CreativeGates.get().setFilling(true);
		this.setContent(CreativeGates.getFillMaterial(blocks.get(0).getWorld()));
		CreativeGates.get().setFilling(false);
	}
	
	/**
	 * Empties the gate.
	 */
	public void empty()
	{
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
		//this.fxSmoke();
		playConfiguredTeleportSound(player, false);
	}
	
	/**
	 * Plays the use effect for the gate.
	 * 
	 * @param player The player to play the effect for.
	 */
	public void fxKitUse(Player player)
	{
		playConfiguredTeleportSound(player, true);
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
