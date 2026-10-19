package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.GateTeamRank;
import com.massivecraft.creativegates.entity.UFactionPerms;
import com.massivecraft.creativegates.entity.UFactionPermsColl;
import com.massivecraft.creativegates.integration.team.TeamBridge;
import com.massivecraft.creativegates.integration.team.TeamBridgeRegistry;
import com.massivecraft.massivecore.util.IdUtil;
import org.bukkit.entity.Player;

/**
 * FACTION create/manage checks against per-team ranks and player allowlist ({@link UFactionPerms}).
 */
public final class GateFactionPermUtil
{
	private GateFactionPermUtil()
	{
	}
	
	/**
	 * True if the player may create or manage FACTION gates for the team
	 * (allowed ranks or listed player, and still a member), or has override/bypass.
	 *
	 * @param player Actor.
	 * @param teamId Team id.
	 * @return True when allowed.
	 */
	public static boolean canManageFactionGates(Player player, String teamId)
	{
		if (player == null || teamId == null || teamId.isEmpty()) return false;
		if (EngineGateOverride.canBypassOwnership(player)) return true;
		
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		if (bridge == null || !bridge.isAvailable()) return false;
		
		// Must be a current member — listed players who left the team do not keep access.
		GateTeamRank rank = bridge.getRank(player, teamId);
		if (rank == null) return false;
		
		UFactionPerms perms = UFactionPermsColl.get().getOrCreate(teamId);
		if (perms == null) return false;
		
		String playerId = IdUtil.getId(player);
		if (playerId != null && perms.hasPlayer(playerId)) return true;
		
		return perms.allowsRank(rank);
	}
	
	/**
	 * True if the player may edit {@code /cg perm} for their team (leader or override).
	 *
	 * @param player Actor.
	 * @param teamId Team id.
	 * @return True when allowed.
	 */
	public static boolean canEditFactionPerms(Player player, String teamId)
	{
		if (player == null || teamId == null || teamId.isEmpty()) return false;
		if (EngineGateOverride.canBypassOwnership(player)) return true;
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		if (bridge == null || !bridge.isAvailable()) return false;
		return bridge.isLeader(player, teamId);
	}
}
