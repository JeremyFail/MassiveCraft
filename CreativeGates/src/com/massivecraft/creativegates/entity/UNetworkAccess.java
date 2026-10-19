package com.massivecraft.creativegates.entity;

import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.store.Entity;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Network-scoped use-access relations for PLAYER and FACTION identities.
 * Entries may be {@link GateTeamRelation} names, {@code p:<playerId>}, or {@code t:<teamId>}.
 */
public class UNetworkAccess extends Entity<UNetworkAccess>
{
	/** Prefix for player-id relation tokens. */
	public static final String PLAYER_PREFIX = "p:";
	
	/** Prefix for team-id relation tokens. */
	public static final String TEAM_PREFIX = "t:";
	
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	/**
	 * Looks up network access by entity id.
	 *
	 * @param oid Entity id or other MassiveCore oid.
	 * @return Stored access, or null if absent.
	 */
	public static UNetworkAccess get(Object oid)
	{
		return UNetworkAccessColl.get().get(oid);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public UNetworkAccess load(UNetworkAccess that)
	{
		this.scopeType = that.scopeType;
		this.networkOwnerId = that.networkOwnerId;
		this.networkId = that.networkId;
		this.useRelations = that.useRelations != null ? new MassiveSet<>(that.useRelations) : new MassiveSet<>();
		return this;
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private GateScopeType scopeType = GateScopeType.PLAYER;
	
	/**
	 * Scope of the network identity this access belongs to.
	 *
	 * @return Scope type; never null (defaults to {@link GateScopeType#PLAYER}).
	 */
	public GateScopeType getScopeType()
	{
		return this.scopeType != null ? this.scopeType : GateScopeType.PLAYER;
	}
	
	/**
	 * Sets the scope of the network identity.
	 *
	 * @param scopeType Scope to store; null becomes {@link GateScopeType#PLAYER}.
	 */
	public void setScopeType(GateScopeType scopeType)
	{
		GateScopeType next = scopeType != null ? scopeType : GateScopeType.PLAYER;
		this.changed(this.scopeType, next);
		this.scopeType = next;
	}
	
	private String networkOwnerId = null;
	
	/**
	 * Network owner id (player UUID or team id), or null when unused.
	 *
	 * @return Owner id, or null.
	 */
	public String getNetworkOwnerId() { return this.networkOwnerId; }
	
	/**
	 * Sets the network owner id.
	 *
	 * @param networkOwnerId Owner id, or null.
	 */
	public void setNetworkOwnerId(String networkOwnerId)
	{
		this.changed(this.networkOwnerId, networkOwnerId);
		this.networkOwnerId = networkOwnerId;
	}
	
	private String networkId = null;
	
	/**
	 * Network display/name id for this identity.
	 *
	 * @return Network name, or null if unset.
	 */
	public String getNetworkId() { return this.networkId; }
	
	/**
	 * Sets the network display/name id.
	 *
	 * @param networkId Network name.
	 */
	public void setNetworkId(String networkId)
	{
		this.changed(this.networkId, networkId);
		this.networkId = networkId;
	}
	
	private Set<String> useRelations = new MassiveSet<>();
	
	/**
	 * Normalized relation tokens that may travel this network.
	 *
	 * @return Live set, or an empty set when null.
	 */
	public Set<String> getUseRelations()
	{
		return this.useRelations != null ? this.useRelations : Collections.emptySet();
	}
	
	/**
	 * Replaces the relation token list.
	 *
	 * @param useRelations Tokens to store; null becomes an empty set.
	 */
	public void setUseRelations(Set<String> useRelations)
	{
		Set<String> next = useRelations != null ? new MassiveSet<>(useRelations) : new MassiveSet<>();
		this.changed(this.useRelations, next);
		this.useRelations = next;
	}
	
	/**
	 * Adds a normalized relation token.
	 *
	 * @param token Raw or normalized token (relation name, {@code p:}, or {@code t:}).
	 * @return True if newly added; false if invalid or already present.
	 */
	public boolean addRelation(String token)
	{
		if (token == null || token.isEmpty()) return false;
		String normalized = normalizeToken(token);
		if (normalized == null) return false;
		Set<String> next = new MassiveSet<>(this.getUseRelations());
		if (!next.add(normalized)) return false;
		this.setUseRelations(next);
		return true;
	}
	
	/**
	 * Removes a normalized relation token.
	 *
	 * @param token Raw or normalized token.
	 * @return True if removed; false if invalid or not present.
	 */
	public boolean removeRelation(String token)
	{
		if (token == null || token.isEmpty()) return false;
		String normalized = normalizeToken(token);
		if (normalized == null) return false;
		Set<String> next = new MassiveSet<>(this.getUseRelations());
		if (!next.remove(normalized)) return false;
		this.setUseRelations(next);
		return true;
	}
	
	/**
	 * True if the normalized token is present.
	 *
	 * @param token Raw or normalized token.
	 * @return True when listed.
	 */
	public boolean hasRelation(String token)
	{
		String normalized = normalizeToken(token);
		return normalized != null && this.getUseRelations().contains(normalized);
	}
	
	/**
	 * Resets relations to {@link #defaultRelationsFor(GateScopeType)} for this entity's scope.
	 */
	public void resetToDefaults()
	{
		this.setUseRelations(defaultRelationsFor(this.getScopeType()));
	}
	
	// -------------------------------------------- //
	// IDENTITY HELPERS
	// -------------------------------------------- //
	
	/**
	 * Stable entity id for a network identity.
	 *
	 * @param scope Scope type.
	 * @param ownerId Owner id or null.
	 * @param networkId Network name.
	 * @return Coll id, or null if identity is invalid for access storage.
	 */
	public static String identityId(GateScopeType scope, String ownerId, String networkId)
	{
		if (scope == null || networkId == null || networkId.isEmpty()) return null;
		if (scope != GateScopeType.PLAYER && scope != GateScopeType.FACTION) return null;
		String owner = ownerId != null ? ownerId : "_";
		return scope.name() + "|" + owner + "|" + networkId.toLowerCase(Locale.ROOT);
	}
	
	/**
	 * Builds a player relation token.
	 *
	 * @param playerId Stable player id.
	 * @return {@code p:<playerId>}, or null if playerId is empty.
	 */
	public static String playerToken(String playerId)
	{
		if (playerId == null || playerId.isEmpty()) return null;
		return PLAYER_PREFIX + playerId;
	}
	
	/**
	 * Builds a team relation token.
	 *
	 * @param teamId Opaque team id.
	 * @return {@code t:<teamId>}, or null if teamId is empty.
	 */
	public static String teamToken(String teamId)
	{
		if (teamId == null || teamId.isEmpty()) return null;
		return TEAM_PREFIX + teamId;
	}
	
	/**
	 * Normalizes a raw grant/relation string into a stored token.
	 *
	 * @param raw Relation name, {@code p:} / {@code t:} token, or blank.
	 * @return Canonical token, or null if unrecognized.
	 */
	public static String normalizeToken(String raw)
	{
		if (raw == null || raw.isEmpty()) return null;
		String t = raw.trim();
		if (t.regionMatches(true, 0, PLAYER_PREFIX, 0, PLAYER_PREFIX.length()))
		{
			String id = t.substring(PLAYER_PREFIX.length()).trim();
			return id.isEmpty() ? null : PLAYER_PREFIX + id;
		}
		if (t.regionMatches(true, 0, TEAM_PREFIX, 0, TEAM_PREFIX.length()))
		{
			String id = t.substring(TEAM_PREFIX.length()).trim();
			return id.isEmpty() ? null : TEAM_PREFIX + id;
		}
		GateTeamRelation rel = GateTeamRelation.parse(t);
		return rel != null ? rel.name() : null;
	}
	
	/**
	 * Default relation tokens for a newly created access entity of the given scope.
	 *
	 * @param scope Network scope.
	 * @return New set of normalized tokens (may be empty for PLAYER).
	 */
	public static Set<String> defaultRelationsFor(GateScopeType scope)
	{
		MassiveSet<String> ret = new MassiveSet<>();
		if (scope == GateScopeType.FACTION)
		{
			for (String token : MConf.get().getDefaultFactionUseRelations())
			{
				String n = normalizeToken(token);
				if (n != null) ret.add(n);
			}
			if (ret.isEmpty()) ret.add(GateTeamRelation.MEMBER.name());
		}
		else if (scope == GateScopeType.PLAYER)
		{
			for (String token : MConf.get().getDefaultPlayerUseRelations())
			{
				String n = normalizeToken(token);
				if (n != null) ret.add(n);
			}
		}
		return ret;
	}
}
