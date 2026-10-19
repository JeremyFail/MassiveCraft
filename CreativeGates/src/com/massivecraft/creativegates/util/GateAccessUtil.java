package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.GateTeamRelation;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UNetworkAccess;
import com.massivecraft.creativegates.entity.UNetworkAccessColl;
import com.massivecraft.creativegates.integration.team.TeamBridge;
import com.massivecraft.creativegates.integration.team.TeamBridgeRegistry;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.IdUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Network use-access and FACTION land helpers.
 */
public final class GateAccessUtil
{
	private GateAccessUtil()
	{
	}
	
	/**
	 * True if the sender may travel through the entry gate (scope access matrix).
	 * Does not check Perm.USE, Entry, or Players toggles.
	 *
	 * @param sender Traveler.
	 * @param gate Entry gate.
	 * @return True if allowed.
	 */
	public static boolean canUse(CommandSender sender, UGate gate)
	{
		if (sender == null || gate == null) return false;
		if (EngineGateOverride.canBypassOwnership(sender)) return true;
		
		GateScopeType scope = gate.getScopeType();
		if (scope == GateScopeType.SHARED || scope == GateScopeType.GLOBAL) return true;
		
		if (!(sender instanceof Player)) return false;
		Player player = (Player) sender;
		String playerId = IdUtil.getId(player);
		
		if (scope == GateScopeType.PLAYER)
		{
			String ownerId = gate.getNetworkOwnerId();
			if (ownerId != null && ownerId.equalsIgnoreCase(playerId)) return true;
			return matchesRelations(player, gate);
		}
		
		if (scope == GateScopeType.FACTION)
		{
			return matchesRelations(player, gate);
		}
		
		return false;
	}
	
	/**
	 * True when FACTION land rules are disabled, or the gate sits in its owning team's land.
	 * Non-FACTION gates always return true.
	 *
	 * @param gate Gate to check.
	 * @return True if land is OK for use/create purposes.
	 */
	public static boolean isFactionLandOk(UGate gate)
	{
		if (gate == null) return false;
		if (gate.getScopeType() != GateScopeType.FACTION) return true;
		if (!MConf.get().isFactionGatesRequireOwnLand()) return true;
		
		String teamId = gate.getNetworkOwnerId();
		if (teamId == null || teamId.isEmpty()) return false;
		
		Location loc = gateLocation(gate);
		if (loc == null) return false;
		
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		if (bridge == null || !bridge.isAvailable()) return false;
		return bridge.isOwnedBy(teamId, loc);
	}
	
	/**
	 * True when a prospective FACTION gate at {@code location} would satisfy land rules.
	 *
	 * @param teamId Owning team id.
	 * @param location Gate location.
	 * @return True if allowed.
	 */
	public static boolean isFactionLandOk(String teamId, Location location)
	{
		if (!MConf.get().isFactionGatesRequireOwnLand()) return true;
		if (teamId == null || teamId.isEmpty() || location == null) return false;
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		if (bridge == null || !bridge.isAvailable()) return false;
		return bridge.isOwnedBy(teamId, location);
	}
	
	/**
	 * Ensures access exists for PLAYER/FACTION identities (create path).
	 *
	 * @param gate Gate just created or transferred onto an identity.
	 */
	public static void ensureAccess(UGate gate)
	{
		if (gate == null) return;
		GateScopeType scope = gate.getScopeType();
		if (scope != GateScopeType.PLAYER && scope != GateScopeType.FACTION) return;
		UNetworkAccessColl.get().getOrCreate(scope, gate.getNetworkOwnerId(), gate.getNetworkId());
	}
	
	/**
	 * Access entity for the gate's identity, creating defaults if needed.
	 *
	 * @param gate Gate.
	 * @return Access or null for SHARED/GLOBAL.
	 */
	public static UNetworkAccess getAccess(UGate gate)
	{
		if (gate == null) return null;
		return UNetworkAccessColl.get().getOrCreate(gate.getScopeType(), gate.getNetworkOwnerId(), gate.getNetworkId());
	}
	
	private static boolean matchesRelations(Player player, UGate gate)
	{
		UNetworkAccess access = UNetworkAccessColl.get().getOrCreate(
			gate.getScopeType(),
			gate.getNetworkOwnerId(),
			gate.getNetworkId()
		);
		if (access == null) return false;
		
		Set<String> relations = access.getUseRelations();
		if (relations.isEmpty() && gate.getScopeType() == GateScopeType.PLAYER) return false;
		
		String playerId = IdUtil.getId(player);
		if (playerId != null && relations.contains(UNetworkAccess.playerToken(playerId))) return true;
		
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		String travelerTeam = bridge != null && bridge.isAvailable() ? bridge.getTeamId(player) : null;
		if (travelerTeam != null && relations.contains(UNetworkAccess.teamToken(travelerTeam))) return true;
		
		String hostTeam = resolveHostTeamId(gate, bridge);
		if (hostTeam == null || bridge == null || !bridge.isAvailable()) return false;
		
		GateTeamRelation relation;
		if (travelerTeam != null && travelerTeam.equals(hostTeam))
		{
			relation = GateTeamRelation.MEMBER;
		}
		else if (travelerTeam == null)
		{
			relation = GateTeamRelation.NEUTRAL;
		}
		else
		{
			relation = bridge.getRelation(travelerTeam, hostTeam);
		}
		
		return relations.contains(relation.name());
	}
	
	/**
	 * Host team for relation checks: FACTION network owner, or PLAYER owner's current team.
	 */
	private static String resolveHostTeamId(UGate gate, TeamBridge bridge)
	{
		if (gate.getScopeType() == GateScopeType.FACTION)
		{
			return gate.getNetworkOwnerId();
		}
		if (gate.getScopeType() == GateScopeType.PLAYER)
		{
			if (bridge == null || !bridge.isAvailable()) return null;
			String ownerId = gate.getNetworkOwnerId();
			if (ownerId == null) return null;
			Player owner = IdUtil.getPlayer(ownerId);
			if (owner != null) return bridge.getTeamId(owner);
			// Offline owner: cannot resolve team for relations without player online.
			return null;
		}
		return null;
	}
	
	/**
	 * Gets the location of the gate.
	 * 
	 * @param gate The gate to get the location of.
	 * @return The location of the gate.
	 */
	private static Location gateLocation(UGate gate)
	{
		PS exit = gate.getExit();
		if (exit == null) return null;
		try
		{
			World world = exit.asBukkitWorld(false);
			if (world == null) return null;
			return exit.asBukkitLocation();
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
