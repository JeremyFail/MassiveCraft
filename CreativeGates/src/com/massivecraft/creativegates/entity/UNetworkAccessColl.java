package com.massivecraft.creativegates.entity;

import com.massivecraft.massivecore.store.Coll;

/**
 * Persisted network use-access matrices for PLAYER and FACTION identities.
 */
public class UNetworkAccessColl extends Coll<UNetworkAccess>
{
	private static final UNetworkAccessColl i = new UNetworkAccessColl();
	public static UNetworkAccessColl get() { return i; }
	
	@Override
	public void onTick()
	{
		super.onTick();
	}
	
	/**
	 * Gets or creates access for a network identity (PLAYER/FACTION only).
	 *
	 * @param scope Scope type.
	 * @param ownerId Owner id.
	 * @param networkId Network name.
	 * @return Access entity, or null if scope does not use access lists.
	 */
	public UNetworkAccess getOrCreate(GateScopeType scope, String ownerId, String networkId)
	{
		String id = UNetworkAccess.identityId(scope, ownerId, networkId);
		if (id == null) return null;
		UNetworkAccess existing = this.get(id, false);
		if (existing != null) return existing;
		
		UNetworkAccess created = this.create(id);
		created.setScopeType(scope);
		created.setNetworkOwnerId(ownerId);
		created.setNetworkId(networkId);
		created.setUseRelations(UNetworkAccess.defaultRelationsFor(scope));
		return created;
	}
	
	/**
	 * Gets access if present without creating.
	 *
	 * @param scope Scope type.
	 * @param ownerId Owner id.
	 * @param networkId Network name.
	 * @return Access or null.
	 */
	public UNetworkAccess getIfExists(GateScopeType scope, String ownerId, String networkId)
	{
		String id = UNetworkAccess.identityId(scope, ownerId, networkId);
		if (id == null) return null;
		return this.get(id, false);
	}
	
	/**
	 * Detaches access for an identity when present.
	 *
	 * @param scope Scope type.
	 * @param ownerId Owner id.
	 * @param networkId Network name.
	 */
	public void detachIfPresent(GateScopeType scope, String ownerId, String networkId)
	{
		UNetworkAccess access = this.getIfExists(scope, ownerId, networkId);
		if (access != null) access.detach();
	}
	
	/**
	 * Moves or resets access when a gate identity changes.
	 * Keeps relations when owner id is unchanged; resets when owner changes.
	 *
	 * @param oldScope Old scope.
	 * @param oldOwner Old owner.
	 * @param oldNetwork Old network name.
	 * @param newScope New scope.
	 * @param newOwner New owner.
	 * @param newNetwork New network name.
	 */
	public void onIdentityChanged(
		GateScopeType oldScope, String oldOwner, String oldNetwork,
		GateScopeType newScope, String newOwner, String newNetwork
	)
	{
		UNetworkAccess oldAccess = this.getIfExists(oldScope, oldOwner, oldNetwork);
		boolean ownerSame = (oldOwner == null && newOwner == null)
			|| (oldOwner != null && oldOwner.equals(newOwner));
		boolean stillAccessScoped = newScope == GateScopeType.PLAYER || newScope == GateScopeType.FACTION;
		
		if (!stillAccessScoped)
		{
			cleanupIfOrphaned(oldScope, oldOwner, oldNetwork);
			return;
		}
		
		String newId = UNetworkAccess.identityId(newScope, newOwner, newNetwork);
		if (newId == null) return;
		
		if (oldAccess != null && ownerSame
			&& oldScope == newScope
			&& oldNetwork != null && newNetwork != null
			&& oldNetwork.equalsIgnoreCase(newNetwork)
			&& ((oldOwner == null && newOwner == null) || (oldOwner != null && oldOwner.equals(newOwner))))
		{
			return; // same identity
		}
		
		UNetworkAccess dest = this.getOrCreate(newScope, newOwner, newNetwork);
		if (dest != null && oldAccess != null && ownerSame)
		{
			dest.setUseRelations(oldAccess.getUseRelations());
		}
		
		cleanupIfOrphaned(oldScope, oldOwner, oldNetwork);
	}
	
	/**
	 * Detaches access when no gates remain on the identity.
	 *
	 * @param scope Scope type.
	 * @param ownerId Owner id.
	 * @param networkId Network name.
	 */
	public void cleanupIfOrphaned(GateScopeType scope, String ownerId, String networkId)
	{
		if (scope != GateScopeType.PLAYER && scope != GateScopeType.FACTION) return;
		if (UGateColl.get().getGateChain(scope, ownerId, networkId).isEmpty())
		{
			this.detachIfPresent(scope, ownerId, networkId);
		}
	}
	
	/**
	 * Removes all FACTION access entities for a team id.
	 *
	 * @param teamId Team id.
	 */
	public void destroyFactionAccessForTeam(String teamId)
	{
		if (teamId == null || teamId.isEmpty()) return;
		for (UNetworkAccess access : this.getAll())
		{
			if (access == null) continue;
			if (access.getScopeType() != GateScopeType.FACTION) continue;
			if (!teamId.equals(access.getNetworkOwnerId())) continue;
			access.detach();
		}
	}
}
