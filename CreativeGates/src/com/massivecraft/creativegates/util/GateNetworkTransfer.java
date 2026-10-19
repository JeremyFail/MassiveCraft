package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.entity.UNetworkAccessColl;
import com.massivecraft.creativegates.integration.team.TeamBridge;
import com.massivecraft.creativegates.integration.team.TeamBridgeRegistry;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Scope/owner transfer for gates while keeping the network display name.
 */
public final class GateNetworkTransfer
{
	private GateNetworkTransfer()
	{
	}
	
	/**
	 * Transfers one gate to a new scope/owner identity.
	 *
	 * @param actor Acting player.
	 * @param gate Gate to move.
	 * @param destScope Destination scope.
	 * @param destOwnerId Destination owner id (null for GLOBAL/SHARED).
	 * @param message Whether to send result messages.
	 * @return True on success.
	 */
	public static boolean transfer(Player actor, UGate gate, GateScopeType destScope, String destOwnerId, boolean message)
	{
		if (actor == null || gate == null || destScope == null) return false;
		
		if (!EngineGateOverride.canManage(actor, gate))
		{
			if (message) MixinMessage.get().messageOne(actor, Txt.parse("<b>You may not manage that gate."));
			return false;
		}
		
		if (!GateScopeUtil.isScopeOffered(actor, destScope, message)) return false;
		
		String ownerId = destOwnerId;
		if (!destScope.requiresOwnerId())
		{
			ownerId = null;
		}
		else if (ownerId == null || ownerId.isEmpty())
		{
			ownerId = GateScopeUtil.resolveOwnerIdForCreate(actor, destScope, message);
			if (ownerId != null && ownerId.isEmpty()) return false;
		}
		
		if (destScope == GateScopeType.FACTION)
		{
			TeamBridge bridge = TeamBridgeRegistry.getActive();
			if (bridge == null || !bridge.isAvailable() || !bridge.teamExists(ownerId))
			{
				if (message) MixinMessage.get().messageOne(actor, Txt.parse("<b>That %s is not available.", FactionTerm.get()));
				return false;
			}
			if (!bridge.canCreate(actor, ownerId) && !EngineGateOverride.canBypassOwnership(actor))
			{
				if (message) MixinMessage.get().messageOne(actor, Txt.parse("<b>You may not transfer into that %s.", FactionTerm.get()));
				return false;
			}
			Location landLoc = gateLocation(gate);
			if (!GateAccessUtil.isFactionLandOk(ownerId, landLoc))
			{
				if (message) MixinMessage.get().messageOne(actor, Txt.parse("<b>%s gates must be in that %s land.", FactionTerm.capital(), FactionTerm.possessive()));
				return false;
			}
		}
		
		String creatorId = GateLimitUtil.creatorIdOf(actor);
		if (!GateLimitUtil.checkCanAdd(actor, destScope, ownerId, gate.getNetworkId(), creatorId, gate, message))
		{
			return false;
		}
		
		GateScopeType oldScope = gate.getScopeType();
		String oldOwner = gate.getNetworkOwnerId();
		String networkId = gate.getNetworkId();
		
		gate.setScopeType(destScope);
		gate.setNetworkOwnerId(ownerId);
		gate.setChainIndex(UGateColl.get().nextChainIndex(destScope, ownerId, networkId));
		
		if (destScope == GateScopeType.PLAYER && ownerId != null
			&& (oldScope != GateScopeType.PLAYER || oldOwner == null || !ownerId.equalsIgnoreCase(oldOwner)))
		{
			gate.setCreatorId(ownerId);
		}
		
		UNetworkAccessColl.get().onIdentityChanged(oldScope, oldOwner, networkId, destScope, ownerId, networkId);
		GateAccessUtil.ensureAccess(gate);
		
		if (message)
		{
			MixinMessage.get().messageOne(actor, Txt.parse(
				"<g>Gate moved to <h>%s<g> (<h>%s<g>).",
				destScope.name(),
				GateScopeUtil.describeOwner(destScope, ownerId)
			));
		}
		return true;
	}
	
	/**
	 * Transfers every gate on the network to the destination identity (same network name).
	 *
	 * @param actor Admin/override actor.
	 * @param sample Gate defining the network and network name.
	 * @param destScope Destination scope.
	 * @param destOwnerId Destination owner or null.
	 * @param message Whether to message.
	 * @return Number of gates moved.
	 */
	public static int transferWholeNetwork(Player actor, UGate sample, GateScopeType destScope, String destOwnerId, boolean message)
	{
		if (actor == null || sample == null || destScope == null) return 0;
		if (!EngineGateOverride.canTransferWholeNetwork(actor))
		{
			if (message) MixinMessage.get().messageOne(actor, Txt.parse("<b>You need creativegates.cg.manage.transfernetwork or override."));
			return 0;
		}
		
		// SHARED/GLOBAL have no owner — whole-network transfer is not meaningful
		GateScopeType sourceScope = sample.getScopeType();
		if (sourceScope == GateScopeType.SHARED || sourceScope == GateScopeType.GLOBAL)
		{
			if (message)
			{
				MixinMessage.get().messageOne(actor, Txt.parse(
					"<b>%s networks cannot be transferred as a whole (no network owner). Convert individual gates with /cg manage scope.",
					sourceScope.name()
				));
			}
			return 0;
		}
		
		// FACTION source: only the team leader/owner (or staff override) may move the whole chain
		if (sourceScope == GateScopeType.FACTION && !EngineGateOverride.canBypassOwnership(actor))
		{
			String teamId = sample.getNetworkOwnerId();
			TeamBridge bridge = TeamBridgeRegistry.getActive();
			if (bridge == null || !bridge.isAvailable() || teamId == null || !bridge.isLeader(actor, teamId))
			{
				if (message)
				{
					MixinMessage.get().messageOne(actor, Txt.parse(
						"<b>Only the %s leader can transfer an entire %s network.",
						FactionTerm.get(),
						FactionTerm.get()
					));
				}
				return 0;
			}
		}
		
		List<UGate> chain = new ArrayList<>(UGateColl.get().getGateChain(sample));
		int moved = 0;
		for (UGate gate : chain)
		{
			if (transfer(actor, gate, destScope, destOwnerId, false)) moved++;
		}
		if (message)
		{
			MixinMessage.get().messageOne(actor, Txt.parse("<g>Moved <h>%d<g> gates to <h>%s<g> (<h>%s<g>).", moved, destScope.name(), GateScopeUtil.describeOwner(destScope, destOwnerId)));
		}
		return moved;
	}
	
	private static Location gateLocation(UGate gate)
	{
		if (gate == null) return null;
		PS exit = gate.getExit();
		if (exit == null) return null;
		try
		{
			return exit.asBukkitLocation();
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
