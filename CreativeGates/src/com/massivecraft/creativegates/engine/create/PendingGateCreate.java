package com.massivecraft.creativegates.engine.create;

import com.massivecraft.creativegates.gate.GateOrientation;

import com.massivecraft.massivecore.ps.PS;
import org.bukkit.World;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Snapshot of a gate create attempt waiting on fill-type selection.
 */
public final class PendingGateCreate
{
	private final UUID playerId;
	private final String worldName;
	private final String networkId;
	private final PS exit;
	private final Set<PS> coords;
	private final Set<PS> interiorCoords;
	private final GateOrientation orientation;
	private final EquipmentSlot hand;
	
	public PendingGateCreate(UUID playerId, World world, String networkId, PS exit, Set<PS> coords, Set<PS> interiorCoords, GateOrientation orientation, EquipmentSlot hand)
	{
		this.playerId = playerId;
		this.worldName = world.getName();
		this.networkId = networkId;
		this.exit = exit;
		this.coords = Collections.unmodifiableSet(new LinkedHashSet<>(coords));
		this.interiorCoords = Collections.unmodifiableSet(new LinkedHashSet<>(interiorCoords));
		this.orientation = orientation;
		this.hand = hand;
	}
	
	public UUID getPlayerId() { return this.playerId; }
	public String getWorldName() { return this.worldName; }
	public String getNetworkId() { return this.networkId; }
	public PS getExit() { return this.exit; }
	public Set<PS> getCoords() { return this.coords; }
	public Set<PS> getInteriorCoords() { return this.interiorCoords; }
	public GateOrientation getOrientation() { return this.orientation; }
	public EquipmentSlot getHand() { return this.hand; }
}
