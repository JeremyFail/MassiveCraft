package com.massivecraft.creativegates.entity;

import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.store.Entity;

import java.util.Collections;
import java.util.Set;

/**
 * Faction/team-scoped ranks and player allowlist for creating/managing FACTION gates.
 * Entity id is the opaque team id.
 * <p>
 * Player entries only apply while that player is still a member of the team.
 * </p>
 */
public class UFactionPerms extends Entity<UFactionPerms>
{
	/** Optional prefix when parsing player tokens from commands ({@code p:<id>}). */
	public static final String PLAYER_PREFIX = "p:";
	
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	/**
	 * Looks up faction gate perms by entity id (usually the team id).
	 *
	 * @param oid Entity id or other MassiveCore oid.
	 * @return Stored perms, or null if absent.
	 */
	public static UFactionPerms get(Object oid)
	{
		return UFactionPermsColl.get().get(oid);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public UFactionPerms load(UFactionPerms that)
	{
		this.teamId = that.teamId;
		this.manageRanks = that.manageRanks != null ? new MassiveSet<>(that.manageRanks) : new MassiveSet<>();
		this.managePlayers = that.managePlayers != null ? new MassiveSet<>(that.managePlayers) : new MassiveSet<>();
		return this;
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private String teamId = null;
	
	/**
	 * Opaque team/faction id this perms entity belongs to.
	 *
	 * @return Team id, or null if unset.
	 */
	public String getTeamId() { return this.teamId; }
	
	/**
	 * Sets the opaque team/faction id.
	 *
	 * @param teamId Team id to store.
	 */
	public void setTeamId(String teamId)
	{
		this.changed(this.teamId, teamId);
		this.teamId = teamId;
	}
	
	/**
	 * Portable ranks allowed to create and manage FACTION gates for this team.
	 * Matching uses at-least semantics ({@link GateTeamRank#meetsOrExceeds}).
	 */
	private Set<String> manageRanks = new MassiveSet<>();
	
	/**
	 * Rank tokens currently allowed ({@link GateTeamRank} names).
	 *
	 * @return Unmodifiable empty set when null; otherwise the live set of rank names.
	 */
	public Set<String> getManageRanks()
	{
		return this.manageRanks != null ? this.manageRanks : Collections.emptySet();
	}
	
	/**
	 * Replaces the allowed rank list.
	 *
	 * @param manageRanks Rank names to store; null becomes an empty set.
	 */
	public void setManageRanks(Set<String> manageRanks)
	{
		Set<String> next = manageRanks != null ? new MassiveSet<>(manageRanks) : new MassiveSet<>();
		this.changed(this.manageRanks, next);
		this.manageRanks = next;
	}
	
	/**
	 * Player ids (without prefix) allowed as one-offs while they remain team members.
	 */
	private Set<String> managePlayers = new MassiveSet<>();
	
	/**
	 * Player ids currently on the allowlist.
	 *
	 * @return Unmodifiable empty set when null; otherwise the live set of player ids.
	 */
	public Set<String> getManagePlayers()
	{
		return this.managePlayers != null ? this.managePlayers : Collections.emptySet();
	}
	
	/**
	 * Replaces the player allowlist.
	 *
	 * @param managePlayers Player ids to store; null becomes an empty set.
	 */
	public void setManagePlayers(Set<String> managePlayers)
	{
		Set<String> next = managePlayers != null ? new MassiveSet<>(managePlayers) : new MassiveSet<>();
		this.changed(this.managePlayers, next);
		this.managePlayers = next;
	}
	
	/**
	 * Adds a portable rank to the allowlist.
	 *
	 * @param rank Rank to add.
	 * @return True if newly added; false if null or already present.
	 */
	public boolean addRank(GateTeamRank rank)
	{
		if (rank == null) return false;
		Set<String> next = new MassiveSet<>(this.getManageRanks());
		if (!next.add(rank.name())) return false;
		this.setManageRanks(next);
		return true;
	}
	
	/**
	 * Removes a portable rank from the allowlist.
	 *
	 * @param rank Rank to remove.
	 * @return True if removed; false if null or not present.
	 */
	public boolean removeRank(GateTeamRank rank)
	{
		if (rank == null) return false;
		Set<String> next = new MassiveSet<>(this.getManageRanks());
		if (!next.remove(rank.name())) return false;
		this.setManageRanks(next);
		return true;
	}
	
	/**
	 * Adds a player id to the allowlist.
	 *
	 * @param playerId Stable player id (no {@link #PLAYER_PREFIX}).
	 * @return True if newly added; false if null/empty or already present.
	 */
	public boolean addPlayer(String playerId)
	{
		if (playerId == null || playerId.isEmpty()) return false;
		Set<String> next = new MassiveSet<>(this.getManagePlayers());
		if (!next.add(playerId)) return false;
		this.setManagePlayers(next);
		return true;
	}
	
	/**
	 * Removes a player id from the allowlist.
	 *
	 * @param playerId Stable player id.
	 * @return True if removed; false if null/empty or not present.
	 */
	public boolean removePlayer(String playerId)
	{
		if (playerId == null || playerId.isEmpty()) return false;
		Set<String> next = new MassiveSet<>(this.getManagePlayers());
		if (!next.remove(playerId)) return false;
		this.setManagePlayers(next);
		return true;
	}
	
	/**
	 * True if the player id is on the allowlist (membership is checked separately).
	 *
	 * @param playerId Stable player id.
	 * @return True when listed.
	 */
	public boolean hasPlayer(String playerId)
	{
		return playerId != null && this.getManagePlayers().contains(playerId);
	}
	
	/**
	 * True if the member's rank is allowed (at-least). Empty rank list means LEADER only.
	 *
	 * @param playerRank Player's rank in the team.
	 * @return True when allowed by rank.
	 */
	public boolean allowsRank(GateTeamRank playerRank)
	{
		if (playerRank == null) return false;
		Set<String> ranks = this.getManageRanks();
		if (ranks.isEmpty())
		{
			return playerRank.meetsOrExceeds(GateTeamRank.LEADER);
		}
		for (String token : ranks)
		{
			GateTeamRank required = GateTeamRank.parse(token);
			if (required != null && playerRank.meetsOrExceeds(required)) return true;
		}
		return false;
	}
	
	/**
	 * Resets ranks to {@link #defaultRanks()} and clears the player allowlist.
	 */
	public void resetToDefaults()
	{
		this.setManageRanks(defaultRanks());
		this.setManagePlayers(new MassiveSet<>());
	}
	
	/**
	 * Builds the default rank set from {@link MConf#getDefaultFactionManageRanks()}.
	 * Falls back to {@link GateTeamRank#LEADER} when the config list is empty or invalid.
	 *
	 * @return New set of rank name tokens.
	 */
	public static Set<String> defaultRanks()
	{
		MassiveSet<String> ret = new MassiveSet<>();
		for (String token : MConf.get().getDefaultFactionManageRanks())
		{
			GateTeamRank rank = GateTeamRank.parse(token);
			if (rank != null) ret.add(rank.name());
		}
		if (ret.isEmpty()) ret.add(GateTeamRank.LEADER.name());
		return ret;
	}
}
