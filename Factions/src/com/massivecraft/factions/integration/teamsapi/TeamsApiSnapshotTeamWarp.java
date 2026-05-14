package com.massivecraft.factions.integration.teamsapi;

import com.skyblockexp.teamsapi.model.TeamWarp;
import org.bukkit.Location;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable {@link TeamWarp} snapshot synthesized from persisted {@link com.massivecraft.factions.entity.Warp}s; filler ids/times compensate
 * for missing metadata in faction storage layers.
 */
public final class TeamsApiSnapshotTeamWarp implements TeamWarp
{
	/** Mirrors {@link TeamsApiWarpIds#UNKNOWN} when warp ownership metadata absent. */
	private static final UUID NIL_CREATOR = new UUID(0L, 0L);

	private final UUID teamId;
	private final String name;
	private final Location location;
	private final UUID creatorUUID;
	private final Instant createdAt;

	public TeamsApiSnapshotTeamWarp(final UUID teamId, final String name, final Location location, final UUID creatorUUID,
		final Instant createdAt)
	{
		if (teamId == null) throw new NullPointerException("teamId");
		if (name == null) throw new NullPointerException("name");
		if (location == null) throw new NullPointerException("location");

		this.teamId = teamId;
		this.name = name;
		this.location = location;
		this.creatorUUID = creatorUUID != null ? creatorUUID : NIL_CREATOR;
		this.createdAt = createdAt != null ? createdAt : Instant.EPOCH;
	}

	/** {@inheritDoc} */
	@Override
	public UUID getTeamId()
	{
		return this.teamId;
	}

	/** {@inheritDoc} */
	@Override
	public String getName()
	{
		return this.name;
	}

	/** {@inheritDoc} */
	@Override
	public Location getLocation()
	{
		return this.location;
	}

	/** {@inheritDoc} */
	@Override
	public UUID getCreatorUUID()
	{
		return this.creatorUUID;
	}

	/** {@inheritDoc} */
	@Override
	public Instant getCreatedAt()
	{
		return this.createdAt;
	}
}
