package com.massivecraft.factions.integration.teamsapi;

import com.skyblockexp.teamsapi.model.TeamClaim;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable TeamsAPI {@link TeamClaim} row derived from faction board coordinates; timestamps default to epoch when faction data omits precise claim audit fields.
 */
public final class TeamsApiSnapshotTeamClaim implements TeamClaim
{
	private final UUID teamId;
	private final String worldName;
	private final int chunkX;
	private final int chunkZ;
	private final Instant claimedAt;

	/**
	 * Creates a new TeamsApiSnapshotTeamClaim instance.
	 * 
	 * @param teamId The team ID.
	 * @param worldName The world name.
	 * @param chunkX The chunk X.
	 * @param chunkZ The chunk Z.
	 * @param claimedAt The claimed at timestamp.
	 */
	public TeamsApiSnapshotTeamClaim(final UUID teamId, final String worldName, final int chunkX, final int chunkZ,
		final Instant claimedAt)
	{
		if (teamId == null) throw new NullPointerException("teamId");
		if (worldName == null) throw new NullPointerException("worldName");

		this.teamId = teamId;
		this.worldName = worldName;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.claimedAt = claimedAt != null ? claimedAt : Instant.EPOCH;
	}

	/** {@inheritDoc} */
	@Override
	public UUID getTeamId()
	{
		return this.teamId;
	}

	/** {@inheritDoc} */
	@Override
	public String getWorldName()
	{
		return this.worldName;
	}

	/** {@inheritDoc} */
	@Override
	public int getChunkX()
	{
		return this.chunkX;
	}

	/** {@inheritDoc} */
	@Override
	public int getChunkZ()
	{
		return this.chunkZ;
	}

	/** {@inheritDoc} */
	@Override
	public Instant getClaimedAt()
	{
		return this.claimedAt;
	}
}
