package com.massivecraft.factions.integration.teamsapi;

import com.skyblockexp.teamsapi.model.TeamMember;
import com.skyblockexp.teamsapi.model.TeamRole;
import com.skyblockexp.teamsapi.model.TeamRoleDefinition;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable TeamsAPI {@link TeamMember} row derived from faction roster data; timestamps are synthesized when persistence lacks them.
 */
public final class TeamsApiSnapshotTeamMember implements TeamMember
{
	private final UUID playerUuid;
	private final TeamRole role;
	private final TeamRoleDefinition roleDefinition;
	/** Factions historically does not store join instants separately for API export - callers should treat sentinel times carefully. */
	private final Instant joinedAt;

	/**
	 * Creates a new TeamsApiSnapshotTeamMember instance.
	 * 
	 * @param playerUuid The player UUID.
	 * @param role The coarse TeamsAPI role.
	 * @param roleDefinition The member's faction rank definition (name + prefix).
	 * @param joinedAt The joined at timestamp.
	 */
	public TeamsApiSnapshotTeamMember(
		final UUID playerUuid,
		final TeamRole role,
		final TeamRoleDefinition roleDefinition,
		final Instant joinedAt
	)
	{
		if (playerUuid == null) throw new NullPointerException("playerUuid");
		if (role == null) throw new NullPointerException("role");
		if (roleDefinition == null) throw new NullPointerException("roleDefinition");

		this.playerUuid = playerUuid;
		this.role = role;
		this.roleDefinition = roleDefinition;
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
	public TeamRoleDefinition getRoleDefinition()
	{
		return this.roleDefinition;
	}

	/** {@inheritDoc} */
	@Override
	public Instant getJoinedAt()
	{
		return this.joinedAt;
	}
}
