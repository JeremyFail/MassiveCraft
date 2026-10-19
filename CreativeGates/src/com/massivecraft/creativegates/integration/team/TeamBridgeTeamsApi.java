package com.massivecraft.creativegates.integration.team;

import com.massivecraft.creativegates.entity.GateTeamRank;
import com.massivecraft.creativegates.entity.GateTeamRelation;
import com.massivecraft.creativegates.util.GateFactionPermUtil;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsClaimService;
import com.skyblockexp.teamsapi.api.TeamsRelationService;
import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamClaim;
import com.skyblockexp.teamsapi.model.TeamRelation;
import com.skyblockexp.teamsapi.model.TeamRole;
import com.massivecraft.massivecore.collections.MassiveList;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * TeamsAPI-backed {@link TeamBridge}.
 * Team ids are stored as UUID strings.
 */
public final class TeamBridgeTeamsApi implements TeamBridge
{
	private final TeamsService teamsService;
	
	/**
	 * Creates a bridge wrapping the given TeamsAPI service.
	 *
	 * @param teamsService Non-null TeamsService instance.
	 */
	private TeamBridgeTeamsApi(TeamsService teamsService)
	{
		this.teamsService = teamsService;
	}
	
	/**
	 * Attempts to create a TeamsAPI bridge when the service is available.
	 *
	 * @return Bridge instance, or null if unavailable.
	 */
	public static TeamBridgeTeamsApi tryCreate()
	{
		try
		{
			if (!TeamsAPI.isAvailable()) return null;
			TeamsService svc = TeamsAPI.getService();
			if (svc == null) return null;
			return new TeamBridgeTeamsApi(svc);
		}
		catch (Throwable t)
		{
			return null;
		}
	}
	
	@Override
	public boolean isAvailable()
	{
		try
		{
			return TeamsAPI.isAvailable() && this.teamsService != null;
		}
		catch (Throwable t)
		{
			return false;
		}
	}
	
	@Override
	public String getTeamId(Player player)
	{
		if (player == null || !this.isAvailable()) return null;
		Optional<Team> team = this.teamsService.getPlayerTeam(player.getUniqueId());
		return team.map(t -> t.getId().toString()).orElse(null);
	}
	
	@Override
	public String getTeamDisplayName(String teamId)
	{
		UUID uuid = parseUuid(teamId);
		if (uuid == null || !this.isAvailable()) return null;
		Optional<Team> team = this.teamsService.getTeam(uuid);
		return team.map(Team::getName).orElse(null);
	}
	
	@Override
	public boolean canCreate(Player player, String teamId)
	{
		return GateFactionPermUtil.canManageFactionGates(player, teamId);
	}
	
	@Override
	public boolean canManage(Player player, String teamId)
	{
		return GateFactionPermUtil.canManageFactionGates(player, teamId);
	}
	
	@Override
	public boolean isLeader(Player player, String teamId)
	{
		if (player == null || teamId == null || !this.isAvailable()) return false;
		UUID uuid = parseUuid(teamId);
		if (uuid == null) return false;
		try
		{
			Optional<TeamRole> role = this.teamsService.getMemberRole(uuid, player.getUniqueId());
			return role.isPresent() && role.get() == TeamRole.OWNER;
		}
		catch (Throwable t)
		{
			return false;
		}
	}
	
	@Override
	public GateTeamRank getRank(Player player, String teamId)
	{
		if (player == null || teamId == null || !this.isAvailable()) return null;
		if (!this.isMemberOf(player, teamId)) return null;
		UUID uuid = parseUuid(teamId);
		if (uuid == null) return null;
		try
		{
			Optional<TeamRole> roleOpt = this.teamsService.getMemberRole(uuid, player.getUniqueId());
			if (!roleOpt.isPresent()) return null;
			TeamRole role = roleOpt.get();
			if (role == TeamRole.OWNER) return GateTeamRank.LEADER;
			String name = role.name();
			if ("ADMIN".equals(name) || "MODERATOR".equals(name) || "OFFICER".equals(name)
				|| "COLEADER".equals(name) || "CO_LEADER".equals(name) || "CO-LEADER".equals(name))
			{
				return GateTeamRank.OFFICER;
			}
			return GateTeamRank.MEMBER;
		}
		catch (Throwable t)
		{
			return null;
		}
	}
	
	@Override
	public boolean canUse(Player player, String teamId)
	{
		return this.isMemberOf(player, teamId);
	}
	
	@Override
	public boolean teamExists(String teamId)
	{
		UUID uuid = parseUuid(teamId);
		if (uuid == null || !this.isAvailable()) return false;
		return this.teamsService.getTeam(uuid).isPresent();
	}
	
	@Override
	public GateTeamRelation getRelation(String fromTeamId, String toTeamId)
	{
		if (fromTeamId == null || toTeamId == null) return GateTeamRelation.NEUTRAL;
		if (fromTeamId.equals(toTeamId)) return GateTeamRelation.MEMBER;
		UUID from = parseUuid(fromTeamId);
		UUID to = parseUuid(toTeamId);
		if (from == null || to == null) return GateTeamRelation.NEUTRAL;
		try
		{
			if (!TeamsAPI.isRelationAvailable()) return GateTeamRelation.NEUTRAL;
			TeamsRelationService relations = TeamsAPI.getRelationService();
			if (relations == null) return GateTeamRelation.NEUTRAL;
			TeamRelation rel = relations.getRelation(from, to);
			return mapRelation(rel);
		}
		catch (Throwable t)
		{
			return GateTeamRelation.NEUTRAL;
		}
	}
	
	@Override
	public String getTeamIdAt(Location location)
	{
		if (location == null || location.getWorld() == null) return null;
		try
		{
			if (!TeamsAPI.isClaimAvailable()) return null;
			TeamsClaimService claims = TeamsAPI.getClaimService();
			if (claims == null) return null;
			Optional<TeamClaim> claim = claims.getClaimAt(
				location.getWorld().getName(),
				location.getBlockX() >> 4,
				location.getBlockZ() >> 4
			);
			if (!claim.isPresent()) return null;
			UUID teamId = claim.get().getTeamId();
			return teamId != null ? teamId.toString() : null;
		}
		catch (Throwable t)
		{
			return null;
		}
	}
	
	@Override
	public boolean isOwnedBy(String teamId, Location location)
	{
		if (teamId == null || teamId.isEmpty()) return false;
		String at = this.getTeamIdAt(location);
		return teamId.equals(at);
	}
	
	@Override
	public String resolveTeamId(String nameOrId)
	{
		if (nameOrId == null || nameOrId.isEmpty() || !this.isAvailable()) return null;
		if (this.teamExists(nameOrId)) return nameOrId;
		try
		{
			Optional<Team> byName = this.teamsService.getTeamByName(nameOrId);
			return byName.map(t -> t.getId().toString()).orElse(null);
		}
		catch (Throwable t)
		{
			return null;
		}
	}
	
	@Override
	public Collection<String> getTeamDisplayNames()
	{
		MassiveList<String> ret = new MassiveList<>();
		if (!this.isAvailable()) return ret;
		try
		{
			for (Team team : this.teamsService.getAllTeams())
			{
				if (team == null) continue;
				String name = team.getName();
				if (name != null && !name.isEmpty()) ret.add(name);
			}
		}
		catch (Throwable ignored)
		{
		}
		return ret;
	}
	
	/**
	 * True if the player is a member of the given team id.
	 *
	 * @param player Actor.
	 * @param teamId Team id.
	 * @return True when membership matches.
	 */
	private boolean isMemberOf(Player player, String teamId)
	{
		if (player == null || teamId == null) return false;
		String id = this.getTeamId(player);
		return teamId.equals(id);
	}
	
	/**
	 * Maps a TeamsAPI {@link TeamRelation} to {@link GateTeamRelation}.
	 *
	 * @param rel Provider relation (may be null).
	 * @return Portable relation; never null.
	 */
	private static GateTeamRelation mapRelation(TeamRelation rel)
	{
		if (rel == null) return GateTeamRelation.NEUTRAL;
		switch (rel)
		{
			case MEMBER: return GateTeamRelation.MEMBER;
			case ALLY: return GateTeamRelation.ALLY;
			case TRUCE: return GateTeamRelation.TRUCE;
			case ENEMY: return GateTeamRelation.ENEMY;
			case NEUTRAL:
			default: return GateTeamRelation.NEUTRAL;
		}
	}
	
	/**
	 * Parses a team id string as a UUID.
	 *
	 * @param teamId Raw team id.
	 * @return UUID, or null if invalid.
	 */
	private static UUID parseUuid(String teamId)
	{
		if (teamId == null || teamId.isEmpty()) return null;
		try
		{
			return UUID.fromString(teamId);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
}
