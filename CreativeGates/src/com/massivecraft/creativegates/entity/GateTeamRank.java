package com.massivecraft.creativegates.entity;

/**
 * Portable intra-team rank tokens for FACTION gate create/manage permissions.
 * <p>
 * Mapped from MassiveCraft Factions ranks, TeamsAPI roles, and FactionsBridge roles.
 * Higher privilege has a lower ordinal: {@link #LEADER} &gt; {@link #OFFICER} &gt; {@link #MEMBER}.
 * </p>
 */
public enum GateTeamRank
{
	LEADER,
	OFFICER,
	MEMBER,
	;
	
	/**
	 * Parses a rank token (case-insensitive).
	 *
	 * @param raw Raw token.
	 * @return Rank or null.
	 */
	public static GateTeamRank parse(String raw)
	{
		if (raw == null || raw.isEmpty()) return null;
		try
		{
			return GateTeamRank.valueOf(raw.trim().toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
	
	/**
	 * True if this rank is at least as privileged as {@code required}
	 * (e.g. LEADER satisfies an OFFICER requirement).
	 *
	 * @param required Minimum required rank.
	 * @return True when this rank meets or exceeds the requirement.
	 */
	public boolean meetsOrExceeds(GateTeamRank required)
	{
		if (required == null) return false;
		return this.ordinal() <= required.ordinal();
	}
}
