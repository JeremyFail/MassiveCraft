package com.massivecraft.creativegates.integration.team;

import com.massivecraft.creativegates.entity.GateTeamRank;
import com.massivecraft.creativegates.entity.GateTeamRelation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;

/**
 * Soft facade for faction/team queries used by FACTION-scoped gates.
 * Implementations must not leak provider types into core gate identity code.
 */
public interface TeamBridge
{
	/**
	 * @return True when this provider is present and usable.
	 */
	boolean isAvailable();
	
	/**
	 * Stable team id for the player, or null if not in a team.
	 *
	 * @param player Player to resolve.
	 * @return Opaque team id or null.
	 */
	String getTeamId(Player player);
	
	/**
	 * Display name for a team id (inspect/manage).
	 *
	 * @param teamId Opaque team id.
	 * @return Display name, or null if unknown.
	 */
	String getTeamDisplayName(String teamId);
	
	/**
	 * True if the player may create FACTION gates under the team.
	 *
	 * @param player Actor.
	 * @param teamId Team id.
	 * @return True if create is allowed.
	 */
	boolean canCreate(Player player, String teamId);
	
	/**
	 * True if the player may manage FACTION gates under the team.
	 *
	 * @param player Actor.
	 * @param teamId Team id.
	 * @return True if manage is allowed.
	 */
	boolean canManage(Player player, String teamId);
	
	/**
	 * True if the player is the faction/team leader or owner (top rank only).
	 * Used for whole-network FACTION transfers; co-leaders/admins do not qualify.
	 *
	 * @param player Actor.
	 * @param teamId Team id.
	 * @return True if leader/owner of that team.
	 */
	boolean isLeader(Player player, String teamId);
	
	/**
	 * Portable rank of the player within the team, or null if not a member.
	 *
	 * @param player Actor.
	 * @param teamId Team id.
	 * @return {@link GateTeamRank} or null.
	 */
	GateTeamRank getRank(Player player, String teamId);
	
	/**
	 * True if the player may use FACTION gates under the team (membership default).
	 * Prefer CreativeGates network access matrix for final use decisions.
	 *
	 * @param player Actor.
	 * @param teamId Team id.
	 * @return True if use is allowed.
	 */
	boolean canUse(Player player, String teamId);
	
	/**
	 * True if the team id still exists.
	 *
	 * @param teamId Opaque team id.
	 * @return True if the team is present.
	 */
	boolean teamExists(String teamId);
	
	/**
	 * Relation of {@code fromTeamId} toward {@code toTeamId}.
	 * Same id yields {@link GateTeamRelation#MEMBER}. Missing relation services
	 * yield {@link GateTeamRelation#NEUTRAL} when teams differ.
	 *
	 * @param fromTeamId Observer / traveler team id.
	 * @param toTeamId Host / network owner team id.
	 * @return Normalized relation; never null.
	 */
	GateTeamRelation getRelation(String fromTeamId, String toTeamId);
	
	/**
	 * Stable team id that owns the chunk at the location, or null for wilderness / unknown.
	 *
	 * @param location World location.
	 * @return Opaque team id or null.
	 */
	String getTeamIdAt(Location location);
	
	/**
	 * True when {@code teamId} owns the land at {@code location}.
	 *
	 * @param teamId Expected owner team id.
	 * @param location Location to check.
	 * @return True if owned by that team.
	 */
	boolean isOwnedBy(String teamId, Location location);
	
	/**
	 * Resolves a team id from an id or display name when possible.
	 *
	 * @param nameOrId Team id or name.
	 * @return Stable team id, or null if unknown.
	 */
	default String resolveTeamId(String nameOrId)
	{
		if (nameOrId == null || nameOrId.isEmpty()) return null;
		if (this.teamExists(nameOrId)) return nameOrId;
		return null;
	}
	
	/**
	 * Display names of all real factions/teams (for tab-complete). Excludes wilderness/none.
	 *
	 * @return Mutable or immutable collection of names; never null.
	 */
	default Collection<String> getTeamDisplayNames()
	{
		return Collections.emptyList();
	}
}
