package com.massivecraft.creativegates.entity;

/**
 * Portable team-to-team relation tokens for network use access.
 * Maps onto MassiveCraft {@code Rel}, TeamsAPI {@code TeamRelation}, and
 * FactionsBridge {@code Relationship} (NONE -> NEUTRAL).
 */
public enum GateTeamRelation
{
	MEMBER,
	ALLY,
	TRUCE,
	NEUTRAL,
	ENEMY,
	;
	
	/**
	 * Parses a relation token (case-insensitive).
	 *
	 * @param raw Raw token.
	 * @return Relation or null if not a relation name.
	 */
	public static GateTeamRelation parse(String raw)
	{
		if (raw == null || raw.isEmpty()) return null;
		try
		{
			return GateTeamRelation.valueOf(raw.trim().toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
	
	/**
	 * True if the string is a relation token (not a player/team prefix).
	 *
	 * @param raw Relation token.
	 * @return True when it matches a relation name.
	 */
	public static boolean isRelationToken(String raw)
	{
		return parse(raw) != null;
	}
}
