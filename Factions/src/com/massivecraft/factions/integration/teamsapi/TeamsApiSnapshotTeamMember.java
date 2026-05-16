package com.massivecraft.factions.integration.teamsapi;

import com.skyblockexp.teamsapi.model.TeamMember;
import com.skyblockexp.teamsapi.model.TeamRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable TeamsAPI {@link TeamMember} row derived from faction roster data; timestamps are synthesized when persistence lacks them.
 */
public final class TeamsApiSnapshotTeamMember implements TeamMember
{
	private final UUID playerUuid;
	private final TeamRole role;
	/** Factions historically does not store join instants separately for API export - callers should treat sentinel times carefully. */
	private final Instant joinedAt;

	/**
	 * @param playerUuid player owning this roster row - non-null enforced
	 * @param role       projected Teams coarse role mapped from faction rank ladders
	 * @param joinedAt   persisted join timestamp if known; otherwise {@link Instant#EPOCH} replacement
	 */
	public TeamsApiSnapshotTeamMember(final UUID playerUuid, final TeamRole role, final Instant joinedAt)
	{
		if (playerUuid == null) throw new NullPointerException("playerUuid");
		if (role == null) throw new NullPointerException("role");

		this.playerUuid = playerUuid;
		this.role = role;
		this.joinedAt = joinedAt != null ? joinedAt : Instant.EPOCH;
	}

	/** {@inheritDoc} */
	@Override
	public UUID getPlayerUUID()
	{
		return this.playerUuid;
	}

	/** {@inheritDoc} */
	@Override
	public TeamRole getRole()
	{
		return this.role;
	}

	/** {@inheritDoc} */
	@Override
	public Instant getJoinedAt()
	{
		return this.joinedAt;
	}
}
