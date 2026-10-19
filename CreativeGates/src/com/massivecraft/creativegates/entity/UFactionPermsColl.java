package com.massivecraft.creativegates.entity;

import com.massivecraft.massivecore.store.Coll;

/**
 * Persisted per-team FACTION gate create/manage ranks.
 */
public class UFactionPermsColl extends Coll<UFactionPerms>
{
	private static final UFactionPermsColl i = new UFactionPermsColl();
	public static UFactionPermsColl get() { return i; }
	
	@Override
	public void onTick()
	{
		super.onTick();
	}
	
	/**
	 * Gets or creates perms for a team id.
	 *
	 * @param teamId Opaque team id.
	 * @return Perms entity, or null if teamId is empty.
	 */
	public UFactionPerms getOrCreate(String teamId)
	{
		if (teamId == null || teamId.isEmpty()) return null;
		UFactionPerms existing = this.get(teamId, false);
		if (existing != null) return existing;
		
		UFactionPerms created = this.create(teamId);
		created.setTeamId(teamId);
		created.setManageRanks(UFactionPerms.defaultRanks());
		return created;
	}
	
	/**
	 * Gets perms if present without creating.
	 *
	 * @param teamId Opaque team id.
	 * @return Perms or null.
	 */
	public UFactionPerms getIfExists(String teamId)
	{
		if (teamId == null || teamId.isEmpty()) return null;
		return this.get(teamId, false);
	}
	
	/**
	 * Detaches perms for a disbanded team.
	 *
	 * @param teamId Opaque team id.
	 */
	public void destroyForTeam(String teamId)
	{
		UFactionPerms perms = this.getIfExists(teamId);
		if (perms != null) perms.detach();
	}
}
