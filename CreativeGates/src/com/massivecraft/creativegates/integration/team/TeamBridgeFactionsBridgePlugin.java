package com.massivecraft.creativegates.integration.team;

import cc.javajobs.factionsbridge.FactionsBridge;
import cc.javajobs.factionsbridge.bridge.infrastructure.struct.FPlayer;
import cc.javajobs.factionsbridge.bridge.infrastructure.struct.Faction;
import cc.javajobs.factionsbridge.bridge.infrastructure.struct.FactionsAPI;
import cc.javajobs.factionsbridge.bridge.infrastructure.struct.Relationship;
import cc.javajobs.factionsbridge.bridge.infrastructure.struct.Role;
import com.massivecraft.creativegates.entity.GateTeamRank;
import com.massivecraft.creativegates.entity.GateTeamRelation;
import com.massivecraft.creativegates.util.GateFactionPermUtil;
import com.massivecraft.massivecore.collections.MassiveList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;

/**
 * Standalone FactionsBridge plugin implementation of {@link TeamBridge}.
 */
public final class TeamBridgeFactionsBridgePlugin implements TeamBridge
{
	private final FactionsAPI api;
	
	/**
	 * Creates a bridge wrapping the given FactionsBridge API.
	 *
	 * @param api Non-null FactionsAPI instance.
	 */
	private TeamBridgeFactionsBridgePlugin(FactionsAPI api)
	{
		this.api = api;
	}
	
	/**
	 * Attempts to create a bridge when the FactionsBridge plugin and API are ready.
	 *
	 * @return Bridge instance, or null if unavailable.
	 */
	public static TeamBridgeFactionsBridgePlugin tryCreate()
	{
		try
		{
			Plugin plugin = Bukkit.getPluginManager().getPlugin("FactionsBridge");
			if (plugin == null || !plugin.isEnabled()) return null;
			FactionsAPI api = FactionsBridge.getFactionsAPI();
			if (api == null) return null;
			return new TeamBridgeFactionsBridgePlugin(api);
		}
		catch (Throwable t)
		{
			return null;
		}
	}
	
	@Override
	public boolean isAvailable()
	{
		Plugin plugin = Bukkit.getPluginManager().getPlugin("FactionsBridge");
		return plugin != null && plugin.isEnabled() && this.api != null;
	}
	
	@Override
	public String getTeamId(Player player)
	{
		Faction faction = this.playerFaction(player);
		return faction == null ? null : faction.getId();
	}
	
	@Override
	public String getTeamDisplayName(String teamId)
	{
		if (teamId == null || this.api == null) return null;
		try
		{
			Faction faction = this.api.getFaction(teamId);
			if (faction == null || faction.isWilderness() || faction.isServerFaction()) return null;
			return faction.getName();
		}
		catch (Throwable t)
		{
			return null;
		}
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
		if (player == null || teamId == null || this.api == null) return false;
		if (!this.isMemberOf(player, teamId)) return false;
		try
		{
			FPlayer fp = this.api.getFPlayer(player);
			if (fp == null) return false;
			Role role = fp.getRole();
			return role == Role.LEADER || role == Role.getOwner();
		}
		catch (Throwable t)
		{
			return false;
		}
	}
	
	@Override
	public GateTeamRank getRank(Player player, String teamId)
	{
		if (player == null || teamId == null || this.api == null) return null;
		if (!this.isMemberOf(player, teamId)) return null;
		try
		{
			FPlayer fp = this.api.getFPlayer(player);
			if (fp == null) return null;
			Role role = fp.getRole();
			if (role == null) return GateTeamRank.MEMBER;
			if (role == Role.LEADER || role == Role.getOwner()) return GateTeamRank.LEADER;
			String name = role.name();
			if ("ADMIN".equals(name) || "COLEADER".equals(name) || "CO_LEADER".equals(name)
				|| "MODERATOR".equals(name) || "MOD".equals(name) || "OFFICER".equals(name))
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
		if (teamId == null || this.api == null) return false;
		try
		{
			Faction faction = this.api.getFaction(teamId);
			return faction != null && !faction.isWilderness() && !faction.isServerFaction();
		}
		catch (Throwable t)
		{
			return false;
		}
	}
	
	@Override
	public GateTeamRelation getRelation(String fromTeamId, String toTeamId)
	{
		if (fromTeamId == null || toTeamId == null) return GateTeamRelation.NEUTRAL;
		if (fromTeamId.equals(toTeamId)) return GateTeamRelation.MEMBER;
		if (this.api == null) return GateTeamRelation.NEUTRAL;
		try
		{
			Faction from = this.api.getFaction(fromTeamId);
			Faction to = this.api.getFaction(toTeamId);
			if (from == null || to == null) return GateTeamRelation.NEUTRAL;
			Relationship rel = from.getRelationshipTo(to);
			return mapRelationship(rel);
		}
		catch (Throwable t)
		{
			return GateTeamRelation.NEUTRAL;
		}
	}
	
	@Override
	public String getTeamIdAt(Location location)
	{
		if (location == null || this.api == null) return null;
		try
		{
			Faction faction = this.api.getFactionAt(location);
			if (faction == null || faction.isWilderness() || faction.isServerFaction()) return null;
			return faction.getId();
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
		if (nameOrId == null || nameOrId.isEmpty() || this.api == null) return null;
		if (this.teamExists(nameOrId)) return nameOrId;
		try
		{
			Faction faction = this.api.getFactionByName(nameOrId);
			if (faction == null || faction.isWilderness() || faction.isServerFaction()) return null;
			return faction.getId();
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
		if (this.api == null) return ret;
		try
		{
			List<Faction> factions = this.api.getFactions();
			if (factions == null) return ret;
			for (Faction faction : factions)
			{
				if (faction == null || faction.isWilderness() || faction.isServerFaction()) continue;
				String name = faction.getName();
				if (name == null || name.isEmpty()) name = faction.getTag();
				if (name != null && !name.isEmpty()) ret.add(name);
			}
		}
		catch (Throwable ignored)
		{
		}
		return ret;
	}
	
	/**
	 * True if the player is a member of the given faction id.
	 *
	 * @param player Actor.
	 * @param teamId Faction id.
	 * @return True when membership matches.
	 */
	private boolean isMemberOf(Player player, String teamId)
	{
		if (player == null || teamId == null) return false;
		String id = this.getTeamId(player);
		return teamId.equals(id);
	}
	
	/**
	 * Resolves the player's real (non-wilderness) faction.
	 *
	 * @param player Actor.
	 * @return Faction, or null.
	 */
	private Faction playerFaction(Player player)
	{
		if (player == null || this.api == null) return null;
		try
		{
			FPlayer fp = this.api.getFPlayer(player);
			if (fp == null || !fp.hasFaction()) return null;
			Faction faction = fp.getFaction();
			if (faction == null || faction.isWilderness() || faction.isServerFaction()) return null;
			return faction;
		}
		catch (Throwable t)
		{
			return null;
		}
	}
	
	/**
	 * Maps a FactionsBridge {@link Relationship} to {@link GateTeamRelation}.
	 *
	 * @param rel Provider relationship (may be null).
	 * @return Portable relation; never null.
	 */
	private static GateTeamRelation mapRelationship(Relationship rel)
	{
		if (rel == null || rel == Relationship.NONE || rel == Relationship.DEFAULT_RELATIONSHIP)
		{
			return GateTeamRelation.NEUTRAL;
		}
		switch (rel)
		{
			case MEMBER: return GateTeamRelation.MEMBER;
			case ALLY: return GateTeamRelation.ALLY;
			case TRUCE: return GateTeamRelation.TRUCE;
			case ENEMY: return GateTeamRelation.ENEMY;
			case NONE:
			default: return GateTeamRelation.NEUTRAL;
		}
	}
}
