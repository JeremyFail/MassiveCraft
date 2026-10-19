package com.massivecraft.creativegates.integration.team;

import com.massivecraft.creativegates.entity.GateTeamRank;
import com.massivecraft.creativegates.entity.GateTeamRelation;
import com.massivecraft.creativegates.util.GateFactionPermUtil;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.collections.MassiveList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * MassiveCraft Factions implementation of {@link TeamBridge}.
 */
public final class TeamBridgeFactions implements TeamBridge
{
	private static final TeamBridgeFactions i = new TeamBridgeFactions();
	public static TeamBridgeFactions get() { return i; }
	
	private TeamBridgeFactions()
	{
	}
	
	@Override
	public boolean isAvailable()
	{
		return Bukkit.getPluginManager().isPluginEnabled("Factions");
	}
	
	@Override
	public String getTeamId(Player player)
	{
		if (player == null || !this.isAvailable()) return null;
		MPlayer mplayer = MPlayer.get(player);
		if (mplayer == null) return null;
		Faction faction = mplayer.getFaction();
		if (faction == null || faction.isNone()) return null;
		return faction.getId();
	}
	
	@Override
	public String getTeamDisplayName(String teamId)
	{
		if (teamId == null || !this.isAvailable()) return null;
		Faction faction = FactionColl.get().get(teamId);
		if (faction == null || faction.isNone()) return null;
		return faction.getName();
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
		if (!this.isMemberOf(player, teamId)) return false;
		MPlayer mplayer = MPlayer.get(player);
		return mplayer != null && mplayer.getRank() != null && mplayer.getRank().isLeader();
	}
	
	@Override
	public GateTeamRank getRank(Player player, String teamId)
	{
		if (player == null || teamId == null || !this.isAvailable()) return null;
		if (!this.isMemberOf(player, teamId)) return null;
		MPlayer mplayer = MPlayer.get(player);
		if (mplayer == null) return null;
		Rank rank = mplayer.getRank();
		if (rank == null) return GateTeamRank.MEMBER;
		if (rank.isLeader()) return GateTeamRank.LEADER;
		// Lowest rank in the faction -> MEMBER; everything between -> OFFICER.
		if (rank.getRankBelow() == null) return GateTeamRank.MEMBER;
		return GateTeamRank.OFFICER;
	}
	
	@Override
	public boolean canUse(Player player, String teamId)
	{
		return this.isMemberOf(player, teamId);
	}
	
	@Override
	public boolean teamExists(String teamId)
	{
		if (teamId == null || !this.isAvailable()) return false;
		Faction faction = FactionColl.get().get(teamId);
		return faction != null && !faction.isNone();
	}
	
	@Override
	public GateTeamRelation getRelation(String fromTeamId, String toTeamId)
	{
		if (fromTeamId == null || toTeamId == null) return GateTeamRelation.NEUTRAL;
		if (fromTeamId.equals(toTeamId)) return GateTeamRelation.MEMBER;
		if (!this.isAvailable()) return GateTeamRelation.NEUTRAL;
		Faction from = FactionColl.get().get(fromTeamId);
		Faction to = FactionColl.get().get(toTeamId);
		if (from == null || to == null || from.isNone() || to.isNone()) return GateTeamRelation.NEUTRAL;
		Rel rel = from.getRelationTo(to);
		return mapRel(rel);
	}
	
	@Override
	public String getTeamIdAt(Location location)
	{
		if (location == null || !this.isAvailable()) return null;
		Faction faction = BoardColl.get().getFactionAt(PS.valueOf(location));
		if (faction == null || faction.isNone()) return null;
		return faction.getId();
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
		Faction faction = FactionColl.get().getByName(nameOrId);
		if (faction == null || faction.isNone()) return null;
		return faction.getId();
	}
	
	@Override
	public Collection<String> getTeamDisplayNames()
	{
		MassiveList<String> ret = new MassiveList<>();
		if (!this.isAvailable()) return ret;
		for (Faction faction : FactionColl.get().getAll())
		{
			if (faction == null || faction.isNone()) continue;
			String name = faction.getName();
			if (name != null && !name.isEmpty()) ret.add(name);
		}
		return ret;
	}
	
	/**
	 * Checks if a player is a member of a team.
	 * 
	 * @param player The player to check.
	 * @param teamId The team id to check.
	 * @return True if the player is a member of the team, false otherwise.
	 */
	private boolean isMemberOf(Player player, String teamId)
	{
		if (player == null || teamId == null || !this.isAvailable()) return false;
		String id = this.getTeamId(player);
		return teamId.equals(id);
	}
	
	/**
	 * Maps a Factions {@code Rel} to a {@link GateTeamRelation}.
	 * 
	 * @param rel The relation to map.
	 * @return The mapped relation.
	 */
	private static GateTeamRelation mapRel(Rel rel)
	{
		if (rel == null) return GateTeamRelation.NEUTRAL;
		switch (rel)
		{
			case FACTION: return GateTeamRelation.MEMBER;
			case ALLY: return GateTeamRelation.ALLY;
			case TRUCE: return GateTeamRelation.TRUCE;
			case ENEMY: return GateTeamRelation.ENEMY;
			case NEUTRAL:
			default: return GateTeamRelation.NEUTRAL;
		}
	}
}
